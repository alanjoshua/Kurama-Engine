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
	private float mouseXSensitivity = 0.1f;
	private float mouseYSensitivity = 0.1f;
	private double dt = 0.0;
	private float speed = 20f;
	
	public Game(int width, int height) {

		display = new Display(width, height, this);
		input = new Input(this);
		display.setInput(input);
	}

	public void init() {
		models = new ArrayList<Model>();

//		cameraPos = new Vector(new float[] {0.5f,0.5f,0.5f});

		float[][] camData = null;

//		camData = new float[][] {
//			{0.871214f,-0.192902f,0.451415f,14.777467f},
//			{0,0.919559f,0.392953f,29.361945f},
//			{-0.490904f,-0.342346f,0.801132f,27.993464f},
//			{0,0,0,1}
//		};
//		
		camData = new float[][] { { -0.95424f, 0.0861242f, -0.28637f }, { 0, 0.95763f, 0.288002f },
				{ 0.299041f, 0.274823f, -0.913809f } };

		camData = new float[][] { { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, -1 } };

		cam = new Camera(this,null,null,90, 0.1f, 100,
				display.getWidth(), display.getHeight());

		Tick tQuat = (m -> {
//			Quaternion rotation = new Quaternion(new Vector(new float[] {0,1,0}),1f);
//			Quaternion newR = rotation.multiply(m.getQuaternion());

			Quaternion rot2 = Quaternion.eulerToQuaternion(new Vector(new float[] { 0, 1, 0 }));
			Quaternion newR2 = rot2.multiply(m.getQuaternion());
			m.setQuaternion(newR2);

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
		deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
//		deer.setRotation(rot);
		deer.setTickObj(tQuat);

		Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj");
		mill.setPos(new Vector(new float[] { (float) display.getWidth() / 2.0f, display.getHeight() / 1.7f, 2 }));
		mill.setScale(new Vector(new float[] { 2, 2, 2 }));

		Model ship = ModelBuilder.buildShip();

		Model teapot = ModelBuilder.buildModelFromFile("teapot.obj");
		teapot.setPos(new Vector(new float[] { 0, 0, 50 }));
//		teapot.setScale(new Vector(new float[] {0.1f,0.1f,0.1f}));
//		teapot.setRotation(rot);
//		teapot.setQuaternion(new Quaternion(new Vector(new float[] {0,0,-1}),0));
//		teapot.setTickObj(tQuat);

//		models.add(teapot);
//		models.add(ship);
//		models.add(cube);
//		models.add(tree);
//		models.add(crate);
//		models.add(ironMan);
		models.add(deer);
//		models.add(mill);

		RenderingEngine.renderingMode = RenderingEngine.PERSPECTIVE;
		cam.updateValues();
		cam.lookAtModel(deer);

		cam.getCamToWorld().display();
		System.out.println();
		cam.getWorldToCam().display();
		System.out.println();
		(cam.getWorldToCam().matMul(cam.getCamToWorld())).display();

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
	}

	public void cameraTick() {
		
		float cameraSpeed = (float) (speed * dt);

		if (input.keyDownOnce(KeyEvent.VK_CONTROL)) {
			cam.setQuaternion(new Quaternion(new Vector(new float[] {0, 0, 0}),0));
		}
		
		if (input.keyDown(KeyEvent.VK_W)) {
			Vector v = cam.getQuaternion().getRotationMatrix().convertToVectorArray()[2];
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_S)) {
			Vector v = cam.getQuaternion().getRotationMatrix().convertToVectorArray()[2];
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_A)) {
			Vector v = cam.getQuaternion().getRotationMatrix().convertToVectorArray()[0];
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));;
		}
		
		if (input.keyDown(KeyEvent.VK_D)) {
			Vector v = cam.getQuaternion().getRotationMatrix().convertToVectorArray()[0];
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_SPACE)) {
			Vector v = cam.getQuaternion().getRotationMatrix().convertToVectorArray()[1];
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(KeyEvent.VK_SHIFT)) {
			Vector v = cam.getQuaternion().getRotationMatrix().convertToVectorArray()[1];
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
		}

		if (input.isScrolled()) {
			Vector v = null;
			Vector dir = cam.getQuaternion().getRotationMatrix().convertToVectorList().get(2);

			if (input.getScrollVal() < 0) {
				v = cam.getPos().sub(dir.scalarMul(1f));
			} else {
				v = cam.getPos().add(dir.scalarMul(1f));
			}

			cam.setPos(v);
			v.display();
		}
		
		if (input.getPosition().getNorm() != 0 && shouldCursorCenter) {
			
			float[] rot = new float[] { input.getPosition().getDataElement(1) * mouseXSensitivity, -input.getPosition().getDataElement(0) * mouseYSensitivity, 0};
			Quaternion localRotate = Quaternion.eulerToQuaternion(new Vector(rot));
			cam.rotate(localRotate);
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
