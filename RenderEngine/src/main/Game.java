package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import GUI.Button;
import Math.Quaternion;
import Math.Vector;
import Math.Utils;
import inputs.Input;
import models.DataStructure.Mesh.Face;
import models.DataStructure.Mesh.Vertex;
import models.Model;
import models.Model.Tick;
import models.ModelBuilder;
import rendering.Camera;
import rendering.RenderingEngine;
import rendering.RenderingEngine.RenderPipeline;
import rendering.RenderingEngine.ProjectionMode;

import javax.imageio.ImageIO;

public class Game {

	protected Display display;
	protected List<Model> models;
	protected List<GUI.Button> pauseButtons;
	protected double targetFPS = 1000;
	protected boolean shouldDisplayFPS = false;
	protected boolean programRunning = true;
	protected Camera cam;
	protected Input input;
	protected boolean isGameRunning = true;
	protected float fps;
	protected float displayFPS;
	protected float mouseXSensitivity = 20f;
	protected float mouseYSensitivity = 20f;
	protected float speed = 15f;
	protected float speedMultiplier = 1;
	protected float speedIncreaseMultiplier = 2;
	protected float speedConstant;

	protected Button EXIT;
	protected Button FULLSCREEN;
	protected Button WINDOWED;

	protected RenderingEngine renderingEngine;
	public BufferedImage frameBuffer;
	public int[] frameBufferPixels;
	
	public Game(int width, int height) {
		display = new Display(width, height, this);
		input = new Input(this);
		display.setInput(input);
		renderingEngine = new RenderingEngine(this);
	}

	public Game() {
		display = new Display(this);
		input = new Input(this);
		display.setInput(input);
		renderingEngine = new RenderingEngine(this);
	}

	public void init() {

		display.startScreen();

		pauseButtons = new ArrayList<>();
		models = new ArrayList<>();

		cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 1f, 100,
				display.getWidth(), display.getHeight());

		renderingEngine.resetBuffers();
		resetBuffers();
		initModels();
		initPauseScreen();

		renderingEngine.setProjectionMode(ProjectionMode.PERSPECTIVE);
		renderingEngine.setRenderPipeline(RenderPipeline.Matrix);

		cam.updateValues();
		cam.lookAtModel(models.get(0));

	}

	public void resetBuffers() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		frameBuffer = gc.createCompatibleImage(cam.getImageWidth(), cam.getImageHeight(),BufferedImage.TYPE_INT_ARGB);
		frameBuffer.setAccelerationPriority(1);

//		frameBufferPixels = ((DataBufferInt)frameBuffer.getRaster().getDataBuffer()).getData();
	}

	public void initModels() {
		Tick tempRot = (m -> {
			Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50*speedConstant);
			Quaternion newQ = rot.multiply(m.getOrientation());
			m.setOrientation(newQ);
		});

		Model deer = ModelBuilder.buildModelFromFile("deer.obj");
		deer.setPos(new Vector(new float[] {-20,7,-20}));
		deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));

		Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj");
		mill.setPos(new Vector(new float[] {10,5,-10}));
		mill.setScale(new Vector(new float[] { 0.5f, 0.5f, 0.5f }));

		Model grid = ModelBuilder.buildGrid(100, 100);
		grid.setPos(new Vector(new float[] {0,0,0}));

		Model pot = ModelBuilder.buildModelFromFile("TeapotHex.obj");
		pot.setPos(new Vector(new float[]{0,10,0}));
		pot.setTickObj(tempRot);

		models.add(deer);
//		models.add(grid);
//		models.add(mill);
//		models.add(pot);

	}

	public void initPauseScreen() {

		int width = 200;
		int height = 100;

		//		Making Exit button
		EXIT = new GUI.Button(this,new Vector(new float[]{0.05f,0.1f}),width,height);
		EXIT.text = "EXIT";

		Button.Behaviour exitButtonBehaviour = (b, mp, isPressed) -> {

			if(b.isMouseInside(mp)) {
				b.textColor = Color.RED;
				if(isPressed) {
					System.out.println("Exit pressed");
					programRunning = false;
				}
			}
			else {
				b.textColor = Color.LIGHT_GRAY;
			}
		};

		EXIT.bgColor = Color.DARK_GRAY;
		EXIT.behaviour = exitButtonBehaviour;

//		Font f = Font.getFont("Arial").deriveFont(Font.BOLD,24);
		EXIT.textFont = new Font("Sans-Serif",Font.BOLD,20);


//		Making FullScreen Toggle
		FULLSCREEN = new GUI.Button(this,new Vector(new float[]{0.05f,0.25f}),width,height);
		FULLSCREEN.text = "FULLSCREEN";

		Button.Behaviour fullscreenBehaviour = (b,mp,isPressed) -> {

			if(b.isMouseInside(mp)) {
				b.textColor = Color.RED;
				if(isPressed && getDisplay().displayMode != Display.DisplayMode.FULLSCREEN) {
					getDisplay().displayMode = Display.DisplayMode.FULLSCREEN;
					getDisplay().startScreen();
				}
			}
			else {
				b.textColor = Color.LIGHT_GRAY;
			}

		};

		FULLSCREEN.setBehaviour(fullscreenBehaviour);
		FULLSCREEN.bgColor = Color.DARK_GRAY;
		FULLSCREEN.textFont = new Font("Consolas", Font.BOLD,20);

//		Making WindowedMode Toggle
		WINDOWED = new GUI.Button(this,new Vector(new float[]{0.05f,0.4f}),width,height);
		WINDOWED.text = "WINDOWED MODE";

		Button.Behaviour windowedBehaviour = (b,mp,isPressed) -> {

			if(b.isMouseInside(mp)) {
				b.textColor = Color.RED;
				if(isPressed && getDisplay().displayMode != Display.DisplayMode.WINDOWED) {
					getDisplay().displayMode = Display.DisplayMode.WINDOWED;
					getDisplay().startScreen();
				}
			}
			else {
				b.textColor = Color.LIGHT_GRAY;
			}

		};

		WINDOWED.setBehaviour(windowedBehaviour);
		WINDOWED.bgColor = Color.DARK_GRAY;
		WINDOWED.textFont = new Font("Consolas", Font.BOLD,20);

		pauseButtons.add(EXIT);
		pauseButtons.add(FULLSCREEN);
		pauseButtons.add(WINDOWED);
	}

	public void run() {

		double dt = 0.0;
		double startTime = System.nanoTime();
		double currentTime = System.nanoTime();
		double timerStartTime = System.nanoTime();
		double timer = 0.0;
		double tempDt = 0;
		float tickInterval = 0;

		while (programRunning) {
			
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

		display.removeWindow();

	}

	public void tick() {
		
		if (input.keyDownOnce(KeyEvent.VK_ESCAPE)) {

			//				getDisplay().removeFullScreen();
			//				getDisplay().setFullScreen();
			isGameRunning = !isGameRunning;
		}

		input.setRelative(isGameRunning);
		input.poll();

		if (isGameRunning)
			display.disableCursor();
		else
			display.enableCursor();

		models.forEach(Model::tick);

		if(!isGameRunning) {
			pauseButtons.forEach((b) -> b.tick(input.getPosition(),input.buttonDown(1)));
		}
		else {
			inputTick();
			cam.tick();
		}
		
	}

	public void inputTick() {
		
		float cameraSpeed = speed * speedConstant * speedMultiplier;
		Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

		if (input.keyDownOnce(KeyEvent.VK_R)) {
			cam.lookAtModel(models.get(0));
		}
		
		if (input.keyDownOnce(KeyEvent.VK_CONTROL)) {
			if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
			else speedMultiplier = 1;
		}
		
		if (input.keyDownOnce(KeyEvent.VK_F)) {
			if(targetFPS == 165)
				targetFPS = 1000;
			else 		
				targetFPS = 165;
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
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
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
			if(renderingEngine.getRenderPipeline() == RenderPipeline.Quat) renderingEngine.setRenderPipeline(RenderPipeline.Matrix);
			else renderingEngine.setRenderPipeline(RenderPipeline.Quat);
		}
		
		if (input.getPosition().getNorm() != 0 && isGameRunning) {

//			input.getPosition().display();

			float yawIncrease   = mouseXSensitivity * speedConstant * -input.getPosition().get(0);
			float pitchIncrease = mouseYSensitivity * speedConstant * input.getPosition().get(1);
			
			Vector currentAngle = cam.getOrientation().getPitchYawRoll();
			float currentPitch = currentAngle.get(0) + pitchIncrease;
			
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

	public RenderingEngine getRenderingEngine() {
		return renderingEngine;
	}

	public void render() {
		BufferStrategy bs = display.getBufferStrategy();

		if (bs == null) {
			display.createBufferStrategy(2);
			return;
		}

		do {
			Graphics2D g = (Graphics2D) bs.getDrawGraphics();

			Graphics2D temp = frameBuffer.createGraphics();
			temp.clearRect(0,0,cam.getImageWidth(),cam.getImageHeight());
			temp.dispose();

			g.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

			Font f = new Font("Sans-Serif",Font.PLAIN,16);

			f = f.deriveFont(f.getSize() * display.getScalingRelativeToDPI());
			g.setFont(f);

			g.clearRect(0, 0,display.getWidth(), display.getHeight());
			g.setBackground(Color.BLACK);
			g.setColor(Color.WHITE);

//			long endTime = 0;
//			long startTime = System.nanoTime();

			renderingEngine.render(models, g,cam);
//			renderingEngine.render2(models,frameBuffer);

//			endTime = System.nanoTime();
//			System.out.println("Difference: " + (endTime - startTime) * 0.000000001 + "seconds");

			g.setColor(Color.white);
			g.drawString(cam.getPos().toString(), 10, (int) (display.getHeight() * 0.9));
			g.drawString("FPS : " + this.displayFPS, 10, (int) (display.getHeight() * 0.1));
			g.setColor(Color.RED);
			g.drawString("Rendering Pipeline : " + renderingEngine.getRenderPipeline(), (int) (display.getWidth() * 0.8), (int) (display.getHeight() * 0.1));

			g.setColor(Color.GREEN);
			g.drawString( "Render res: "+ display.getWidth() + " x " + display.getHeight(), (int) (display.getWidth() * 0.8), (int) (display.getHeight() * 0.9));

			if(!this.isGameRunning) {
				for(GUI.Button b:pauseButtons) {
					b.render(g);
				}
			}
//			g.drawImage(frameBuffer,cam.getImageWidth(),cam.getImageHeight(),null);
			g.dispose();

		} while (bs.contentsLost());
		bs.show();
//		try {
//			if(frameBuffer!=null) {
//				ImageIO.write(frameBuffer, "jpg", new File("temp.jpg"));
//				System.out.println("done");
//				System.exit(0);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
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

	public boolean isProgramRunning() {
		return programRunning;
	}

	public void setProgramRunning(boolean programRunning) {
		this.programRunning = programRunning;
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
