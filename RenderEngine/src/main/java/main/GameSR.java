package main;

import Kurama.GUI.Button;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.camera.Camera;
import Kurama.display.Display;
import Kurama.display.DisplaySR;
import Kurama.game.Game;
import Kurama.geometry.MeshBuilder;
import Kurama.geometry.MeshBuilderHints;
import Kurama.inputs.Input;
import Kurama.inputs.InputSR;
import Kurama.model.Model;
import Kurama.model.ModelBehaviourTickInput;
import Kurama.renderingEngine.RenderingEngine;
import Kurama.renderingEngine.RenderingEngine.ProjectionMode;
import Kurama.renderingEngine.RenderingEngineSR;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public class GameSR extends Game implements Runnable {

	protected DisplaySR display;
	protected Camera cam;
	protected InputSR input;
	protected RenderingEngineSR renderingEngine;
	protected List<Model> models;

	protected float mouseXSensitivity = 20f;
	protected float mouseYSensitivity = 20f;
	protected float speed = 15f;
	protected float speedMultiplier = 1;
	protected float speedIncreaseMultiplier = 2;

	protected int lookAtIndex = 0;
	protected boolean isGameRunning = true;

	protected List<Model> modelsOnlyOutline;
	protected List<Kurama.GUI.Button> pauseButtons;

	protected Button EXIT;
	protected Button FULLSCREEN;
	protected Button WINDOWED;

	public GameSR(String threadName) {
		super(threadName);
	}
	public GameSR(String threadName,boolean shouldBenchmark) {
		super(threadName,shouldBenchmark);
	}


	public void init() {
		display = new DisplaySR(this);
		input = new InputSR(this);

		display.setInput(input);
		display.addComponentListenerToFrame(
				new ComponentAdapter() {
					public void componentResized(ComponentEvent e) {
						try {
							getCamera().setShouldUpdateValues(true);
							renderingEngine.resetBuffers();
						} catch (Exception ex) {
						}
					}
				});
		display.startScreen();

		renderingEngine = new RenderingEngineSR(this);

		pauseButtons = new ArrayList<>();
		models = new ArrayList<>();
		modelsOnlyOutline = new ArrayList<>();

		cam = new Camera(this,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
				(int)display.windowResolution.get(0), (int)display.windowResolution.get(0));

		renderingEngine.resetBuffers();
		renderingEngine.projectionMode = ProjectionMode.PERSPECTIVE;

		cam.updateValues();

		initModels();
		initPauseScreen();

		refocusOnModel();

	}

	@Override
	public void cleanUp() {
		display.cleanUp();
		renderingEngine.cleanUp();
		for(Model m:models) {
			m.meshes.forEach( mesh -> mesh.cleanUp());
		}
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
//		MiniBehaviour tempRot = ((m, params) -> {
//			Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50* timeDelta);
//			Quaternion newQ = rot.multiply(m.getOrientation());
//			m.setOrientation(newQ);
//		});

		MeshBuilderHints hints = new MeshBuilderHints();

		Model deer = new Model(this, MeshBuilder.buildMesh("/Resources/deer.obj",hints),"deer");
		deer.setPos(new Vector(new float[] {-20,7,-20}));
		deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));

		Model mill = new Model(this, MeshBuilder.buildMesh("/Resources/low-poly-mill.obj",hints),"mill");
		mill.setPos(new Vector(new float[] {10,5,-10}));
		mill.setScale(new Vector(new float[] { 0.05f, 0.05f, 0.05f }));
//		mill.triangulate();

		Model grid = new Model(this, MeshBuilder.buildGridDeprecated(100, 100),"grid");
		grid.setPos(new Vector(new float[] {0,0,0}));

		Model pot = new Model(this, MeshBuilder.buildMesh("/Resources/TeapotHex3.obj",hints),"pot");
		pot.setPos(new Vector(new float[]{0,10,0}));
		pot.setScale(new Vector(new float[]{0.2f,0.2f,0.2f}));
//		pot.setMiniBehaviourObj(tempRot);
//		pot.triangulate();

//		ModelBuilder.ModelBuilderHints hints = new ModelBuilder.ModelBuilderHints();
//		hints.shouldTriangulate = true;
//		hints.convertToLines = true;
////		hints.shouldTriangulate = true;
////		hints.convertToLines = true;
//		hints.initLWJGLAttribs = false;
//
//		Model cube = new Model(this,ModelBuilder.buildModelFromFileGL("/Resources/cube.obj",meshInstances,hints),"cube");

//		models.add(deer);
		models.add(mill);
//		models.add(pot);
//		models.add(grid);

		modelsOnlyOutline.add(grid);
//		modelsOnlyOutline.add(mill);

	}

	public void initPauseScreen() {

		int width = 200;
		int height = 100;

//		Making Exit button
		EXIT = new Kurama.GUI.Button(this,new Vector(new float[]{0.05f,0.1f}),width,height);
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
		FULLSCREEN = new Kurama.GUI.Button(this,new Vector(new float[]{0.05f,0.25f}),width,height);
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
		WINDOWED = new Kurama.GUI.Button(this,new Vector(new float[]{0.05f,0.4f}),width,height);
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

		ModelBehaviourTickInput params = new ModelBehaviourTickInput(timeDelta, scene);

		models.forEach(m -> m.tick(params));
		modelsOnlyOutline.forEach(m -> m.tick(params));

		if(!isGameRunning) {
			pauseButtons.forEach((b) -> b.tick(((InputSR)input).getPosition(),((InputSR)input).buttonDown(1)));
		}
		else {
			inputTick();
			cam.tick(timeDelta);
		}
		
	}

	public void inputTick() {
		
		float cameraSpeed = speed * timeDelta * speedMultiplier;
		Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

		if (input.keyDownOnce(input.R)) {
			refocusOnModel();
//			cam.lookAtModel(models.get(lookAtIndex));
		}
		
		if (input.keyDownOnce(input.LEFT_CONTROL)) {
			if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
			else speedMultiplier = 1;
		}
		
		if (input.keyDownOnce(input.F)) {
			if(targetFPS == 165)
				targetFPS = 1000;
			else 		
				targetFPS = 165;
		}
		
		if (input.keyDown(input.W)) {
			Vector x = rotationMatrix[0];
			Vector y = new Vector(new float[] {0,1,0});
			Vector z = x.cross(y);
			cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(input.S)) {
			Vector x = rotationMatrix[0];
			Vector y = new Vector(new float[] {0,1,0});
			Vector z = x.cross(y);
			cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(input.A)) {
			Vector v = rotationMatrix[0];
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(input.D)) {
			Vector v = rotationMatrix[0];
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(input.SPACE)) {
			Vector v = new Vector(new float[] {0,1,0});
			cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
		}
		
		if (input.keyDown(input.LEFT_SHIFT)) {
			Vector v = new Vector(new float[] {0,1,0});
			cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
		}
		
//		if (input.keyDownOnce(input.Q)) {
//			if(((RenderingEngineSR)renderingEngine).getRenderPipeline() == RenderMultiplicationMode_Deprecated.Quat) ((RenderingEngineSR)renderingEngine).setRenderPipeline(RenderMultiplicationMode_Deprecated.Matrix);
//			else ((RenderingEngineSR)renderingEngine).setRenderPipeline(RenderMultiplicationMode_Deprecated.Quat);
//		}
		
		if (((InputSR)input).getPosition().getNorm() != 0 && isGameRunning) {

//			input.getPosition().display();

			float yawIncrease   = mouseXSensitivity * timeDelta * -((InputSR)input).getPosition().get(0);
			float pitchIncrease = mouseYSensitivity * timeDelta * ((InputSR)input).getPosition().get(1);
			
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
			g.fillRect(0, 0,(int)display.windowResolution.get(0), (int)display.windowResolution.get(1));
			g.setBackground(bg);

			g.setColor(Color.LIGHT_GRAY);

			if(models.size()>0) {
				((RenderingEngineSR)renderingEngine).render2(models,g);
			}
			((RenderingEngineSR)renderingEngine).render(modelsOnlyOutline, g,cam);

			g.setColor(Color.white);
			g.drawString(cam.getPos().toString(), 10, (int) ((int)display.windowResolution.get(1) * 0.9));
			g.drawString("FPS : " + this.displayFPS, 10, (int) ((int)display.windowResolution.get(1) * 0.1));
			g.setColor(Color.RED);
//			g.drawString("Rendering Pipeline : " + ((RenderingEngineSR)renderingEngine).getRenderPipeline(), (int) (display.getWidth() * 0.8), (int) (display.getHeight() * 0.1));

			g.setColor(Color.GREEN);
			g.drawString( "Render res: "+ (int)display.windowResolution.get(0) + " x " + (int)display.windowResolution.get(1), (int) ((int)display.windowResolution.get(0) * 0.8), (int) ((int)display.windowResolution.get(1) * 0.9));

			if(!this.isGameRunning) {
				for(Kurama.GUI.Button b:pauseButtons) {
					b.render(g);
				}
			}

			g.dispose();

		} while (bs.contentsLost());
		bs.show();
	}

	public RenderingEngine getRenderingEngine() {
		return renderingEngine;
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

	public List<Model> getModels() {
		return models;
	}

	public void setModels(List<Model> models) {
		this.models = models;
	}

}
