package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import GUI.Button;
import Math.Quaternion;
import Math.Vector;
import inputs.InputSR;
import models.DataStructure.Mesh.Mesh;
import models.Model;
import models.Model.Tick;
import models.ModelBuilder;
import rendering.Camera;
import rendering.RenderingEngineSR;
import rendering.RenderingEngine.RenderPipeline;
import rendering.RenderingEngine.ProjectionMode;

public class GameSR extends Game implements Runnable {

	protected List<Model> modelsOnlyOutline;
	protected List<GUI.Button> pauseButtons;

	protected Button EXIT;
	protected Button FULLSCREEN;
	protected Button WINDOWED;

	Map<String, Mesh> meshInstances;

	public GameSR(String threadName) {
		super(threadName);
	}
	public GameSR(String threadName,boolean shouldBenchmark) {
		super(threadName,shouldBenchmark);
	}


	public void init() {
		meshInstances = new HashMap<>();
		display = new DisplaySR(this);
		input = new InputSR(this);
		((DisplaySR)(display)).setInput((InputSR) input);
		renderingEngine = new RenderingEngineSR(this);
		display.startScreen();

		pauseButtons = new ArrayList<>();
		models = new ArrayList<>();
		modelsOnlyOutline = new ArrayList<>();

		cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
				display.getWidth(), display.getHeight());

		((RenderingEngineSR)renderingEngine).resetBuffers();

		((RenderingEngineSR)renderingEngine).setProjectionMode(ProjectionMode.PERSPECTIVE);
		((RenderingEngineSR)renderingEngine).setRenderPipeline(RenderPipeline.Matrix);

		cam.updateValues();

		initModels();
		initPauseScreen();

		refocusOnModel();

	}

	@Override
	public void cleanUp() {

	}

	public void refocusOnModel() {
		try {
			if (models.size() > 0) {
				cam.lookAtModel(models.get(lookAtIndex));
			} else {
				if (modelsOnlyOutline.size() > 0) {
					cam.lookAtModel(modelsOnlyOutline.get(lookAtIndex));
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initModels() {
		Tick tempRot = (m -> {
			Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50*speedConstant);
			Quaternion newQ = rot.multiply(m.getOrientation());
			m.setOrientation(newQ);
		});

		Model deer = ModelBuilder.buildModelFromFile("deer.obj",meshInstances);
		deer.setPos(new Vector(new float[] {-20,7,-20}));
		deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));

		Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj",meshInstances);
		mill.setPos(new Vector(new float[] {10,5,-10}));
		mill.setScale(new Vector(new float[] { 0.05f, 0.05f, 0.05f }));
//		mill.triangulate();

		Model grid = ModelBuilder.buildGrid(100, 100);
		grid.setPos(new Vector(new float[] {0,0,0}));

		Model pot = ModelBuilder.buildModelFromFile("TeapotHex3.obj",meshInstances);
		pot.setPos(new Vector(new float[]{0,10,0}));
		pot.setScale(new Vector(new float[]{0.2f,0.2f,0.2f}));
		pot.setTickObj(tempRot);
//		pot.triangulate();

//		models.add(deer);
		models.add(mill);
//		models.add(pot);
//		models.add(grid);

		modelsOnlyOutline.add(grid);
//		modelsOldRenderMethod.add(pot);

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
		EXIT.textFont = new Font("Sans-Serif",Font.BOLD,20);


//		Making FullScreen Toggle
		FULLSCREEN = new GUI.Button(this,new Vector(new float[]{0.05f,0.25f}),width,height);
		FULLSCREEN.text = "FULLSCREEN";

		Button.Behaviour fullscreenBehaviour = (b,mp,isPressed) -> {

			if(b.isMouseInside(mp)) {
				b.textColor = Color.RED;
				if(isPressed && getDisplay().displayMode != DisplaySR.DisplayMode.FULLSCREEN) {
					getDisplay().displayMode = DisplaySR.DisplayMode.FULLSCREEN;
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
				if(isPressed && getDisplay().displayMode != DisplaySR.DisplayMode.WINDOWED) {
					getDisplay().displayMode = DisplaySR.DisplayMode.WINDOWED;
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

	public void tick() {
		
		if (input.keyDownOnce(KeyEvent.VK_ESCAPE)) {
			isGameRunning = !isGameRunning;
		}

		((InputSR)input).setRelative(isGameRunning);
		input.poll();

		if (isGameRunning)
			display.disableCursor();
		else
			display.enableCursor();

		models.forEach(Model::tick);
		modelsOnlyOutline.forEach(Model::tick);

		if(!isGameRunning) {
			pauseButtons.forEach((b) -> b.tick(((InputSR)input).getPosition(),((InputSR)input).buttonDown(1)));
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
			refocusOnModel();
//			cam.lookAtModel(models.get(lookAtIndex));
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
			if(((RenderingEngineSR)renderingEngine).getRenderPipeline() == RenderPipeline.Quat) ((RenderingEngineSR)renderingEngine).setRenderPipeline(RenderPipeline.Matrix);
			else ((RenderingEngineSR)renderingEngine).setRenderPipeline(RenderPipeline.Quat);
		}
		
		if (((InputSR)input).getPosition().getNorm() != 0 && isGameRunning) {

//			input.getPosition().display();

			float yawIncrease   = mouseXSensitivity * speedConstant * -((InputSR)input).getPosition().get(0);
			float pitchIncrease = mouseYSensitivity * speedConstant * ((InputSR)input).getPosition().get(1);
			
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

		}

	}

	public void render() {
		BufferStrategy bs = ((DisplaySR)display).getCanvas().getBufferStrategy();

		if (bs == null) {
			((DisplaySR)display).getCanvas().createBufferStrategy(2);
			return;
		}

		do {
			Graphics2D g = (Graphics2D) bs.getDrawGraphics();

			g.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

			Font f = new Font("Sans-Serif",Font.PLAIN,16);

			f = f.deriveFont(f.getSize() * display.getScalingRelativeToDPI());
			g.setFont(f);

//			Color bg = new Color(100,100,100);
			Color bg = new Color(10,10,10);
			g.setColor(bg);
			g.fillRect(0, 0,display.getWidth(), display.getHeight());
			g.setBackground(bg);

			g.setColor(Color.LIGHT_GRAY);

			if(models.size()>0) {
				((RenderingEngineSR)renderingEngine).render2(models,g);
			}
			((RenderingEngineSR)renderingEngine).render(modelsOnlyOutline, g,cam);

			g.setColor(Color.white);
			g.drawString(cam.getPos().toString(), 10, (int) (display.getHeight() * 0.9));
			g.drawString("FPS : " + this.displayFPS, 10, (int) (display.getHeight() * 0.1));
			g.setColor(Color.RED);
			g.drawString("Rendering Pipeline : " + ((RenderingEngineSR)renderingEngine).getRenderPipeline(), (int) (display.getWidth() * 0.8), (int) (display.getHeight() * 0.1));

			g.setColor(Color.GREEN);
			g.drawString( "Render res: "+ display.getWidth() + " x " + display.getHeight(), (int) (display.getWidth() * 0.8), (int) (display.getHeight() * 0.9));

			if(!this.isGameRunning) {
				for(GUI.Button b:pauseButtons) {
					b.render(g);
				}
			}

			g.dispose();

		} while (bs.contentsLost());
		bs.show();
	}

}
