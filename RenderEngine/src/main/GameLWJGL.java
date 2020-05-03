package main;

import java.awt.*;
import java.util.*;
import java.util.List;

import engine.DataStructure.Scene;
import engine.GUI.Text;
import engine.HUD;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.display.Display;
import engine.display.DisplayLWJGL;
import engine.font.FontTexture;
import engine.game.Game;
import engine.inputs.Input;
import engine.inputs.InputLWJGL;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.lighting.DirectionalLight;
import engine.lighting.Material;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.Model;
import engine.model.Model.MiniBehaviour;
import engine.model.ModelBuilder;
import engine.camera.Camera;
import engine.renderingEngine.RenderingEngine;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class GameLWJGL extends Game implements Runnable {

    protected DisplayLWJGL display;
    protected Camera cam;
    protected InputLWJGL input;
    protected RenderingEngineLWJGL renderingEngine;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    protected int lookAtIndex = 0;
    protected boolean isGameRunning = true;

    protected List<engine.GUI.Button> pauseButtons;
    protected engine.GUI.Button EXIT;
    protected engine.GUI.Button FULLSCREEN;
    protected engine.GUI.Button WINDOWED;

    protected Vector mouseDelta;
    protected Vector mousePos;

    protected boolean prevGameState = false;

    Map<String, Mesh> meshInstances;
    float lightAngle = 0;

    public GameLWJGL(String threadName) {
        super(threadName);
    }

    @Override
    public void start() {
        String osName = System.getProperty("os.name");
        if ( osName.contains("Mac") ) {
            gameLoopThread.run();   //To make this program compatible with macs
        } else {
            System.out.println("start called");
            gameLoopThread.start();
        }
    }

    public void init() {
        scene = new Scene();

        Vector lightColor = new Vector(new float[]{1f,1f,1f});
        Vector lightPos = new Vector(new float[]{-1f,0f,0f});
        float lightIntensity = 1f;
        PointLight pointLight = new PointLight(lightColor,lightPos,lightIntensity);
        pointLight.attenuation = new PointLight.Attenuation(0f,0f,1f);
        scene.pointLights.add(pointLight);

        lightPos = new Vector(new float[]{0,0,10});
        PointLight sl_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, lightIntensity);
        sl_pointLight.attenuation = new PointLight.Attenuation(0.0f, 0.0f, 0.02f);
        Vector coneDir = new Vector(new float[]{0, 0, -1});
        float cutoff = (float) Math.cos(Math.toRadians(140));
        SpotLight spotLight = new SpotLight(sl_pointLight, coneDir, cutoff);
        scene.spotLights.add(spotLight);

        scene.directionalLights.add(new DirectionalLight(new Vector(new float[]{1,1,1}),new Vector(new float[]{0,1,0}),1));

        meshInstances = new HashMap<>();

        renderingEngine = new RenderingEngineLWJGL(this);

        display = new DisplayLWJGL(this);
        display.startScreen();

        cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
                display.getWidth(), display.getHeight());

        glfwSetFramebufferSizeCallback(display.getWindow(), (window, width, height) -> {
            glViewport(0,0,width,height);
                if(getCamera() != null) {
                    display.setWIDTH(width);
                    display.setHEIGHT(height);
                    getCamera().setImageWidth(width);
                    getCamera().setImageHeight(height);
                    getCamera().setShouldUpdateValues(true);
                }
        });

        renderingEngine.init();

        input = new InputLWJGL(this);

        pauseButtons = new ArrayList<>();

        initModels();
        initPauseScreen();

        cam.updateValues();
        targetFPS = ((DisplayLWJGL)display).getRefreshRate();
        hud = new TestHUD(this);

    }

    public void initModels() {
        MiniBehaviour tempRot = ((m, params) -> {
            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50* timeDelta);
            Quaternion newQ = rot.multiply(m.getOrientation());
            m.setOrientation(newQ);
        });

        ModelBuilder.ModelBuilderHints hints = new ModelBuilder.ModelBuilderHints();
        hints.shouldBakeVertexAttributes = false;
        hints.addRandomColor = false;
        hints.initLWJGLAttribs = true;

        Texture tex = null;
        try {
            tex = new Texture("textures/grassblock.png");
        }catch (Exception e) {
            e.printStackTrace();
        }

        float reflectance = 1f;
        Material cubeMat = new Material(tex,reflectance);

        float skyBoxScale = 50;
        float boxScale = 1f;

        hints.shouldBakeVertexAttributes = true;
        scene.skybox = new Model(this,ModelBuilder.buildModelFromFileGL("/Resources/skybox.obj",meshInstances,hints),"skybox");
        scene.skybox.setScale(skyBoxScale);
        try {
            tex = new Texture("textures/skybox.png");
        }catch (Exception e) {
            System.out.println("Couldn't load skybox texture");
        }
        Material skyMat = new Material(tex,1);
        skyMat.ambientColor = new Vector(new float[]{1,1,1,1});
        scene.skybox.mesh.material = skyMat;

        hints.shouldBakeVertexAttributes = false;
        int cubeCount = 0;
        Vector[] bounds = Model.getBounds(scene.skybox.mesh);
        Random random = new Random();

        Model[][] cubes = new Model[(int)(skyBoxScale*2/(boxScale*2))][(int)(skyBoxScale*2/(boxScale*2))];
        float y;
        float prevXY;
        float prevZY;

        for(int i = 0;i < skyBoxScale*2/(boxScale*2);i++) {
            for(int j = 0;j < skyBoxScale*2/(boxScale*2);j++) {

                //y = random.nextFloat() > 0.7f ? boxScale*2:0f;
                //y = random.nextFloat() < 0.8f ? 0f:(random.nextFloat() < 0.9f ? boxScale*-2 : boxScale*2);

                Vector pos = bounds[0].add(new Vector(new float[]{i*boxScale*2,0,j*boxScale*2}));
                Model cube = new Model(this, ModelBuilder.buildModelFromFileGL("/Resources/cube.obj", meshInstances, hints), "cube");
                cube.setScale(boxScale);
                cube.setPos(pos);
                cube.mesh.material = cubeMat;
                scene.models.add(cube);
                cubes[i][j] = cube;
                cubeCount++;
            }
        }

        scene.buildModelMap();

    }

    public void initPauseScreen() {

        int width = 200;
        int height = 100;

        //		Making Exit button
        EXIT = new engine.GUI.Button(this,new Vector(new float[]{0.05f,0.1f}),width,height);
        EXIT.text = "EXIT";

        engine.GUI.Button.Behaviour exitButtonBehaviour = (b, mp, isPressed) -> {

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
        FULLSCREEN = new engine.GUI.Button(this,new Vector(new float[]{0.05f,0.25f}),width,height);
        FULLSCREEN.text = "FULLSCREEN";

        engine.GUI.Button.Behaviour fullscreenBehaviour = (b, mp, isPressed) -> {

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
        WINDOWED = new engine.GUI.Button(this,new Vector(new float[]{0.05f,0.4f}),width,height);
        WINDOWED.text = "WINDOWED MODE";

        engine.GUI.Button.Behaviour windowedBehaviour = (b, mp, isPressed) -> {

            if(b.isMouseInside(mp)) {
                b.textColor = Color.RED;
                if(isPressed && getDisplay().displayMode != DisplayLWJGL.DisplayMode.WINDOWED) {
                    getDisplay().displayMode = DisplayLWJGL.DisplayMode.WINDOWED;
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

    public void cleanUp() {
        display.cleanUp();
        renderingEngine.cleanUp();
        for(Model m: scene.models) {
            m.mesh.cleanUp();
        }
    }

    public void tick() {
        tickInput();
        hud.tick();

        if(glfwWindowShouldClose(((DisplayLWJGL)display).getWindow())) {
            programRunning = false;
        }

        mouseDelta = input.getDelta();
        mousePos = input.getPos();

        if(isGameRunning != prevGameState) {
            if (isGameRunning)
                display.disableCursor();
            else
                display.enableCursor();
            prevGameState = isGameRunning;
        }

        if(isGameRunning) {
            Model.ModelTickInput params = new Model.ModelTickInput();
            params.timeDelta = timeDelta;

            scene.models.forEach(m -> m.tick(params));

            //DirectionalLight directionalLight = directionalLights.get(0);
//            lightAngle += 10f * timeDelta;
//            if (lightAngle > 90) {
//                directionalLight.intensity = 0;
//                if (lightAngle >= 360) {
//                    lightAngle = -90;
//                }
//            } else if (lightAngle <= -80 || lightAngle >= 80) {
//                float factor = 1 - (float)(Math.abs(lightAngle) - 80)/ 10.0f;
//                directionalLight.intensity = factor;
//                directionalLight.color.setDataElement(1,Math.max(factor, 0.9f));
//                directionalLight.color.setDataElement(2,Math.max(factor, 0.5f));
//            } else {
//                directionalLight.intensity = 1;
//                directionalLight.color = new Vector(3,1);
//            }
//            double angRad = Math.toRadians(lightAngle);
//            directionalLight.direction.setDataElement(0, (float) Math.sin(angRad));
//            directionalLight.direction.setDataElement(1, (float) Math.cos(angRad));

        }

        if(!isGameRunning) {
            pauseButtons.forEach((b) -> b.tick(mousePos,((InputLWJGL)input).isLeftMouseButtonPressed));
        }
        else {
            calculate3DCamMovement();
            cam.tick();
        }

    }

    public void tickInput() {

        if(input.keyDown(input.W)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(input.ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(isGameRunning) {
            if(input.keyDownOnce(input.R)) {
                cam.lookAtModel( scene.models.get(lookAtIndex));
            }

            if(input.keyDownOnce(input.LEFT_CONTROL)) {
                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                else speedMultiplier = 1;
            }

            if(input.keyDownOnce(input.F)) {
                if(targetFPS == ((DisplayLWJGL)display).getRefreshRate()) {
                    targetFPS = 10000;
                }
                else {
                    targetFPS = ((DisplayLWJGL)display).getRefreshRate();
                }
            }

//            if(input.keyDownOnce(GLFW_KEY_Q)) {
//                if(renderingEngine.getRenderPipeline() == RenderingEngineLWJGL.RenderPipeline.Quat) renderingEngine.setRenderPipeline(RenderingEngineLWJGL.RenderPipeline.Matrix);
//                else renderingEngine.setRenderPipeline(RenderingEngineLWJGL.RenderPipeline.Quat);
//            }

            if(input.keyDownOnce(input.V)) {
                display.toggleWindowModes();
            }
        }
    }

    public void calculate3DCamMovement() {
        if (mouseDelta.getNorm() != 0 && isGameRunning) {

            float yawIncrease   = mouseXSensitivity * timeDelta * -mouseDelta.get(0);
            float pitchIncrease = mouseYSensitivity * timeDelta * -mouseDelta.get(1);

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
        ((RenderingEngineLWJGL)renderingEngine).render(scene,hud);

        glfwSwapBuffers(((DisplayLWJGL)display).getWindow());
        glfwPollEvents();
        input.poll();
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
        return  scene.models;
    }

    public void setModels(List<Model> models) {
        scene.models = models;
    }

}
