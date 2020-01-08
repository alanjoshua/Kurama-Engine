package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Quaternion;
import Math.Vector;
import inputs.Input;
import models.Model;
import models.Model.Tick;
import models.ModelBuilder;
import rendering.Camera;
import rendering.RenderingEngine;

public class Game {

	private Display display;
	private List<Model> models;
	private double targetFPS = 60;
	private boolean shouldFPS = false;
	private boolean running = true;
	private Camera cam;
	private Input input;
	private boolean shouldCursorCenter = true;
	private float fps;
	private float displayFPS;
	private float mouseXSensitivity = 10f;
	private float mouseYSensitivity = 10f;
	private double dt = 0.0;
	private float speed = 10f;
	private float speedMultiplier = 1;
	
	public Game(int width, int height) {

		display = new Display(width, height, this);
		input = new Input(this);
		display.setInput(input);
	}

	public void init() {
		models = new ArrayList<Model>();
		cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.01f, 1000,
				display.getWidth(), display.getHeight());

		Tick tQuat = (m -> {
			Quaternion rot2 = Quaternion.eulerToQuaternion(new Vector(new float[] { 0, 1, 0 }));
			Quaternion newR2 = rot2.multiply(m.getOrientation());
			m.setOrientation(newR2);
//			m.setPos(m.getPos().add(new Vector(new float[] {0.1f,0,0})));

		});

		Model cube = ModelBuilder.buildCube();
//		cube.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/2.0f,1}));
		cube.setPos(new Vector(new float[] { 0, 0, -3 }));
		cube.setScale(new Vector(new float[] { 1, 1, 1 }));

		Model tree = ModelBuilder.buildTree();
//		tree.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/2.0f,1}));
//		tree.setPos(new Vector(new float[] {-14,777f,-29.3619f,-27.993464f}));
		tree.setScale(new Vector(new float[] { 0.5f, 0.5f, 0.5f }));
		tree.setPos(new Vector(new float[] { 0, 0, 0 }));

		Model crate = ModelBuilder.buildModelFromFile("Crate.obj");
		crate.setPos(new Vector(new float[] { (float) display.getWidth() / 2.0f, display.getHeight() / 3.0f, 1 }));
		crate.setScale(new Vector(new float[] { 100, 100, 100 }));

		Model ironMan = ModelBuilder.buildModelFromFile("IronMan.obj");
//		ironMan.setPos(new Vector(new float[] { (float) display.getWidth() / 2.0f, display.getHeight() / 0.7f, 1 }));
//		ironMan.setScale(new Vector(new float[] { 3, -3, 3 }));
		ironMan.setTickObj(tQuat);

		Model deer = ModelBuilder.buildModelFromFile("deer.obj");
//		deer.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/1.3f,0}));
		deer.setPos(new Vector(new float[] {-20,7,7}));
		deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
//		deer.setRotation(rot);
		deer.setOrientation(Quaternion.eulerToQuaternion(new Vector(new float[] {0,0,0})));
		deer.setTickObj(tQuat);

		Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj");
		mill.setPos(new Vector(new float[] {10,5,-10}));
		mill.setScale(new Vector(new float[] { 0.5f, 0.5f, 0.5f }));

		Model ship = ModelBuilder.buildShip();

		Model teapot = ModelBuilder.buildModelFromFile("teapot.obj");
		teapot.setPos(new Vector(new float[] { 0, 0, 0}));
		teapot.setScale(new Vector(new float[] {0.1f,0.1f,0.1f}));
//		teapot.setRotation(rot);
//		teapot.setQuaternion(new Quaternion(new Vector(new float[] {0,0,-1}),0));
//		teapot.setTickObj(tQuat);
		
		Model grid = ModelBuilder.buildGrid(100, 100);

//		models.add(teapot);
//		models.add(ship);
//		models.add(cube);
//		models.add(tree);
//		models.add(crate);
//		models.add(ironMan);
		models.add(deer);
		models.add(grid);
		models.add(mill);

		RenderingEngine.renderingMode = RenderingEngine.PERSPECTIVE;
		cam.lookAtModel(models.get(0));
		cam.updateValues();
	}

	public void run() {

		dt = 0.0;
		double startTime = System.nanoTime();
		double currentTime = System.nanoTime();
		double startTime2 = System.nanoTime();
		double frameInterval = ((1.0 / targetFPS));
		double delta = 0.0;

		while (running) {

			if (dt >= frameInterval) {
				tick();
				render();
				fps++;
				startTime = System.nanoTime();
			}

			if (delta >= 1) {
				if (shouldFPS) {
					System.out.println("FPS : " + fps);
				}
				displayFPS = fps;
				fps = 0.0f;
				delta = 0.0;
				startTime = System.nanoTime();
				startTime2 = System.nanoTime();
			}

			currentTime = System.nanoTime();
			dt = (currentTime - startTime) / 1000000000.0;
			delta = (currentTime - startTime2) / 1000000000.0;
		}

		display.getFrame().dispose();

	}

	public void tick() {
//		cam.getQuaternion().getAxis().display();
//		cam.getForward().getCoordinate().display();
		
		if (input.keyDownOnce(KeyEvent.VK_ESCAPE)) {
			if (shouldCursorCenter)
				shouldCursorCenter = false;
			else
				shouldCursorCenter = true;
		}

		input.setRelative(shouldCursorCenter);

		if (shouldCursorCenter)
			display.disableCursor();
		else
			display.enableCursor();

		input.poll();
		cameraTick();
		cam.tick();
		
		for (Model m : models) {
			m.tick();
		}
		
//		cam.getQuaternion().getRotationMatrix().convertToVectorArray()[2].display();
		
	}

	public void cameraTick() {
		
		float cameraSpeed = (float) (speed * dt * speedMultiplier);
		Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToVectorArray();

		if (input.keyDownOnce(KeyEvent.VK_R)) {
			cam.setOrientation(new Quaternion(new Vector(new float[] {0, 0, 0}),0));
			cam.lookAtModel(models.get(0));
		}
		
		if (input.keyDownOnce(KeyEvent.VK_CONTROL)) {
			if(speedMultiplier == 1) speedMultiplier = 2;
			else speedMultiplier = 1;
		}
		
		if (input.keyDown(KeyEvent.VK_W)) {
			Vector x = rotationMatrix[0];
			Vector y = new Vector(new float[] {0,1,0});
			Vector z = x.cross(y);
			cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_S)) {
			Vector x = rotationMatrix[0];
			Vector y = new Vector(new float[] {0,1,0});
			Vector z = x.cross(y);
			cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_A)) {
			Vector v = rotationMatrix[0];
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));;
		}
		
		if (input.keyDown(KeyEvent.VK_D)) {
			Vector v = rotationMatrix[0];
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_SPACE)) {
			Vector v = new Vector(new float[] {0,1,0});
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_SHIFT)) {
			Vector v = new Vector(new float[] {0,1,0});
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
		}

		if (input.isScrolled()) {
			Vector v = null;
			Vector dir = cam.getOrientation().getRotationMatrix().convertToVectorList().get(2);

			if (input.getScrollVal() < 0) {
				v = cam.getPos().sub(dir.scalarMul(1f));
			} else {
				v = cam.getPos().add(dir.scalarMul(1f));
			}

			cam.setPos(v);
		}
		
		if (input.getPosition().getNorm() != 0 && shouldCursorCenter) {
		
			float yawIncrease   = (float) (mouseXSensitivity * dt * -input.getPosition().getDataElement(0));
			float pitchIncrease = (float) (mouseYSensitivity * dt * input.getPosition().getDataElement(1));
			float rollIncrease = 0;
			
			Vector currentAngle = cam.getOrientation().getPitchYawRoll();
//			Vector currentAngle = m.getQuaternion().getPitchYawRoll();
			float currentPitch = currentAngle.getDataElement(0) + pitchIncrease;
			
			if(currentPitch >= 0 && currentPitch > 60) {
				pitchIncrease = 0;
			}
			else if(currentPitch < 0 && currentPitch < -60) {
				pitchIncrease = 0;
			}
			
			Quaternion pitch = new Quaternion(new Vector(new float[] {1,0,0}),pitchIncrease);
			Quaternion yaw = new Quaternion(new Vector(new float[] {0,1,0}),yawIncrease);
			
			Quaternion q = cam.getOrientation();
			
			q = q.multiply(pitch);
			q = yaw.multiply(q);
			cam.setOrientation(q);
			
//			Quaternion temp = Quaternion.eulerToQuaternion(new Vector(new float[] {pitchIncrease,yawIncrease,rollIncrease}));
//			m.setQuaternion(new Quaternion(temp.rotatePoint(m.getQuaternion().getPureVec())));
//			cam.rotate(temp);
//			cam.setQuaternion(temp.multiply(cam.getQuaternion()));
//			m.rotate(temp);
		}

	}

	public void render() {
		BufferStrategy bs = display.getBufferStrategy();

		if (bs == null) {
			display.createBufferStrategy(3);
			return;
		}

		do {
			Graphics2D g = (Graphics2D) bs.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, display.getWidth(), display.getHeight());
			g.setColor(Color.white);
			g.drawString(cam.getPos().toString(), 10, (int) (display.getHeight() * 0.9));
			g.drawString("FPS : " + this.displayFPS, 10, (int) (display.getHeight() * 0.1));

			RenderingEngine.render(this, models, g, cam);
			g.dispose();
		} while (bs.contentsLost());

		bs.show();
	}

	public List<Model> getModels() {
		return models;
	}

	public void setModels(List<Model> models) {
		this.models = models;
	}

	public double getTargetFPS() {
		return targetFPS;
	}

	public void setTargetFPS(double targetFPS) {
		this.targetFPS = targetFPS;
	}

	public boolean isShouldFPS() {
		return shouldFPS;
	}

	public void setShouldFPS(boolean shouldFPS) {
		this.shouldFPS = shouldFPS;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public Display getDisplay() {
		return display;
	}

	public Camera getCamera() {
		return cam;
	}

	public Input getInput() {
		return input;
	}
}
