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
import rendering.RenderingEngine.RenderPipeline;
import rendering.RenderingEngine.RenderingMode;

public class Game {

	private Display display;
	private List<Model> models;
	private double targetFPS = 120;
	private boolean shouldDisplayFPS = false;
	private boolean running = true;
	private Camera cam;
	private Input input;
	private boolean shouldCursorCenter = true;
	private float fps;
	private float displayFPS;
	private float mouseXSensitivity = 20f;
	private float mouseYSensitivity = 20f;
	private float speed = 15f;
	private float speedMultiplier = 1;
	private float speedIncreaseMultiplier = 2;
	private float speedConstant;
	
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
			Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50*speedConstant);
			Quaternion newQ = rot.multiply(m.getOrientation());
			m.setOrientation(newQ);
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
		ironMan.setTickObj(tQuat);

		Model deer = ModelBuilder.buildModelFromFile("deer.obj");
		deer.setPos(new Vector(new float[] {-20,7,7}));
		deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
		deer.setTickObj(tQuat);
//		deer.triangulate();

		Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj");
		mill.setPos(new Vector(new float[] {10,5,-10}));
		mill.setScale(new Vector(new float[] { 0.5f, 0.5f, 0.5f }));
//		mill.triangulate();

		Model ship = ModelBuilder.buildShip();

		Model teapot = ModelBuilder.buildModelFromFile("teapot.obj");
		teapot.setPos(new Vector(new float[] { 20, 10, 10}));
		teapot.setScale(new Vector(new float[] {1f,1f,1f}));
//		teapot.triangulate();
		
		Model grid = ModelBuilder.buildGrid(100, 100);
//		grid.triangulate();

		models.add(deer);
		models.add(grid);
		models.add(mill);
		models.add(teapot);

		RenderingEngine.renderingMode = RenderingMode.PERSPECTIVE;
		RenderingEngine.renderPipeline = RenderPipeline.Matrix;
		cam.lookAtModel(models.get(0));
		cam.updateValues();
		
//		Vector v1 = new Vector(new float[] {-1,0,0});
//		Vector v3 = new Vector(new float[] {1,0,0});
//		Vector v2= new Vector(new float[] {0,1,0});
//		Vector p = new Vector(new float[] {0,0.7f,1});
//		
//		System.out.println(ModelBuilder.isVertexInsideTriangle(v1, v2, v3, p));
	
	}

	public void run() {

		double dt = 0.0;
		double startTime = System.nanoTime();
		double currentTime = System.nanoTime();
		double timerStartTime = System.nanoTime();
		double timer = 0.0;
		double tempDt = 0;
		float tickInterval = 0;

		while (running) {
			
			double timeU = ((1000000000.0 / targetFPS));
			currentTime = System.nanoTime();
			tempDt = (currentTime - startTime);
			dt += tempDt/timeU;
			tickInterval += tempDt;
			startTime = currentTime;
			timer = (currentTime - timerStartTime);
			
			if (dt >= 1) {
				speedConstant = (float) (tickInterval /1000000000.0);
				tickInterval = 0;
				tick();
				render();
				fps++;
				dt = 0;
			}

			if (timer >= 1000000000.0) {
				displayFPS = fps;
				fps = 0;
				timer = 0;
				timerStartTime = System.nanoTime();
			}
		}

		display.getFrame().dispose();

	}

	public void tick() {
//		cam.getQuaternion().getAxis().display();
//		cam.getForward().getCoordinate().display();
//		System.out.println(speedConstant);
		
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
		inputTick();
		cam.tick();
		
		for (Model m : models) {
			m.tick();
		}
		
//		cam.getQuaternion().getRotationMatrix().convertToVectorArray()[2].display();
		
	}

	public void inputTick() {
		
		float cameraSpeed = (float) (speed * speedConstant * speedMultiplier);
		Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToVectorArray();

		if (input.keyDownOnce(KeyEvent.VK_R)) {
			cam.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[] {0, 0, 0}),0));
			cam.lookAtModel(models.get(0));
		}
		
		if (input.keyDownOnce(KeyEvent.VK_CONTROL)) {
			if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
			else speedMultiplier = 1;
		}
		
		if (input.keyDownOnce(KeyEvent.VK_F)) {
			if(targetFPS == 120)
				targetFPS = 1000;
			else 		
				targetFPS = 120;
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
		
		if (input.keyDownOnce(KeyEvent.VK_Q)) {
			if(RenderingEngine.renderPipeline == RenderPipeline.Quat) RenderingEngine.renderPipeline = RenderPipeline.Matrix;
			else RenderingEngine.renderPipeline = RenderPipeline.Quat;
		}
		
		if (input.getPosition().getNorm() != 0 && shouldCursorCenter) {
		
			float yawIncrease   = (float) (mouseXSensitivity * speedConstant * -input.getPosition().getDataElement(0));
			float pitchIncrease = (float) (mouseYSensitivity * speedConstant * input.getPosition().getDataElement(1));
			
			Vector currentAngle = cam.getOrientation().getPitchYawRoll();
			float currentPitch = currentAngle.getDataElement(0) + pitchIncrease;
			
			if(currentPitch >= 0 && currentPitch > 60) {
				pitchIncrease = 0;
			}
			else if(currentPitch < 0 && currentPitch < -60) {
				pitchIncrease = 0;
			}
			
			Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
			Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);
			
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
			g.setColor(Color.RED);;
			g.drawString("Rendering Pipeline : " + RenderingEngine.renderPipeline, (int) (display.getWidth() * 0.8), (int) (display.getHeight() * 0.1));
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
		return shouldDisplayFPS;
	}

	public void setShouldFPS(boolean shouldFPS) {
		this.shouldDisplayFPS = shouldFPS;
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
