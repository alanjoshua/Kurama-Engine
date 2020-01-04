package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Vector;
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
	
	public Game(int width, int height) {
		display = new Display(width,height,this);
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
		 camData = new float[][] {
			{-0.95424f,0.0861242f,-0.28637f},
			{0,0.95763f,0.288002f},
			{0.299041f,0.274823f,-0.913809f}
		};
		
		cam = new Camera(this,camData,null,90,0.1f,100,display.getWidth(),display.getHeight());
		
		List<Vector> rot = new ArrayList<Vector>();
		rot.add(new Vector(new float[]{1,0}));
		
		Tick t = (Model m)->{
			List<Vector> temp = m.getRotation();
			for(Vector v : temp) {
				float val = (float) (v.getDataElement(1) + 1);
				v.setDataElement(1, val);
			}
			m.setRotation(temp);
		};
		
		Model cube = ModelBuilder.buildCube();
//		cube.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/2.0f,1}));
		cube.setPos(new Vector(new float[] {0,0,-3}));
		cube.setScale(new Vector(new float[] {1,1,1}));
		cube.setRotation(rot);
		cube.setTickObj(t);
		
		Model tree = ModelBuilder.buildTree();
//		tree.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/2.0f,1}));
//		tree.setPos(new Vector(new float[] {-14,777f,-29.3619f,-27.993464f}));
		tree.setScale(new Vector(new float[] {0.5f,0.5f,0.5f}));
		tree.setPos(new Vector(new float[] {0,0,0}));
		tree.setRotation(rot);
		tree.setTickObj(t);
		
		Model crate = ModelBuilder.buildModelFromFile("Crate.obj");
		crate.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/3.0f,1}));
		crate.setScale(new Vector(new float[] {100,100,100}));
		crate.setRotation(rot);
		crate.setTickObj(t);
		
		Model ironMan = ModelBuilder.buildModelFromFile("IronMan.obj");
		ironMan.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/0.7f,1}));
		ironMan.setScale(new Vector(new float[] {3,-3,3}));
		ironMan.setRotation(rot);
		ironMan.setTickObj(t);
		
		Model deer = ModelBuilder.buildModelFromFile("deer.obj");
//		deer.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/1.3f,0}));
		deer.setScale(new Vector(new float[] {0.01f,0.01f,0.01f}));
//		deer.setRotation(rot);
//		deer.setTickObj(t);
		
		Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj");
		mill.setPos(new Vector(new float[]{(float)display.getWidth()/2.0f,display.getHeight()/1.7f,2}));
		mill.setScale(new Vector(new float[] {2,2,2}));
		mill.setRotation(rot);
		mill.setTickObj(t);
		
		Model ship = ModelBuilder.buildShip();
//		Tick tShip = (m -> {
//			
//			m.setPos(new Vector(new float[] {0,0,(float) (m.getPos().getDataElement(2)+0.1)}));
//			
//		});
		ship.setTickObj(t);
		ship.setRotation(rot);
		
		Model teapot = ModelBuilder.buildModelFromFile("teapot.obj");
//		teapot.setPos(new Vector(new float[] {0,0,10}));
//		teapot.setScale(new Vector(new float[] {0.1f,0.1f,0.1f}));
		teapot.setRotation(rot);
		teapot.setTickObj(t);
		
		models.add(teapot);
//		models.add(ship);
//		models.add(cube);
//		models.add(tree);
//		models.add(crate);
//		models.add(ironMan);
//		models.add(deer);
//		models.add(mill);
		
		RenderingEngine.renderingMode = RenderingEngine.PERSPECTIVE;
		cam.updateValues();
		cam.lookAtModel(teapot);
	}
	
	public void run() {

		double dt = 0.0;
		double startTime = System.nanoTime();
		double currentTime = System.nanoTime();
		double startTime2 = System.nanoTime();
		double frameInterval = ((1.0 / targetFPS));
		double fps = 0.0;
		double delta = 0.0;

		while (running) {

			if (dt >= frameInterval) {
				tick();
				render();
				fps++;
				startTime = System.nanoTime();
			}

			if (delta >= 1) {
				if(shouldFPS) {
					System.out.println("FPS : " + fps);
				}
				fps = 0.0;
				delta = 0.0;
				startTime = System.nanoTime();
				startTime2 = System.nanoTime();
			}

			currentTime = System.nanoTime();
			dt = (currentTime - startTime) / 1000000000.0;
			delta = (currentTime - startTime2) / 1000000000.0;
		}
	}

	public void tick() {
//		Matrix mat = cam.getCamMat();
//		mat.getData()[2][3] = (float) (mat.getData()[2][3] - 0.1);
//		cam.setCamMat(mat);
		
		cam.tick();
		for(Model m : models) {
			m.tick();
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
			RenderingEngine.render(this,models, g, cam);
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
}
