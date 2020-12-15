package main;

import HUD.TestHUD;
import ModelBehaviour.AttachToPlayer;
import ModelBehaviour.SunRevolve;
import engine.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.Effects.Fog;
import engine.Effects.Material;
import engine.Effects.ShadowMap;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Terrain.TerrainUtils;
import engine.camera.Camera;
import engine.display.Display;
import engine.display.DisplayLWJGL;
import engine.game.Game;
import engine.inputs.Input;
import engine.inputs.InputLWJGL;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.Mesh.MeshBuilderHints;
import engine.model.Model;
import engine.model.ModelBehaviourTickInput;
import engine.renderingEngine.RenderingEngineGL;
import engine.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import engine.scene.Scene;
import engine.scene.SceneUtils;
import engine.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class GameLWJGL extends Game implements Runnable {

    protected DisplayLWJGL display;
    protected InputLWJGL input;
    protected RenderingEngineGL renderingEngine;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    protected int lookAtIndex = 1;
    protected boolean isGameRunning = true;

    protected List<engine.GUI.Button> pauseButtons = new ArrayList<>();
    protected engine.GUI.Button EXIT;
    protected engine.GUI.Button FULLSCREEN;
    protected engine.GUI.Button WINDOWED;

    protected Vector mouseDelta;
    protected Vector mousePos;

    protected boolean prevGameState = false;

    private boolean shouldDayNight = false;

    public GameLWJGL(String threadName) {
        super(threadName);
    }

    public void init() {
        scene = new Scene(this);

        renderingEngine = new RenderingEngineGL(this);
        display = new DisplayLWJGL(this);
        display.startScreen();

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

        input = new InputLWJGL(this);

//        initScene();
        scene = SceneUtils.loadScene(this, "projects/testProject");
        initPauseScreen();

        renderingEngine.renderPipeline = scene.renderPipeline;
        renderingEngine.init(scene);

        display.setClearColor(0,0,0,1);
        scene.camera.updateValues();
        targetFPS = display.getRefreshRate();

    }

    public void writeSceneToFile() {
        SceneUtils.writeSceneToKE(scene, "projects", "testProject", null,
                null, "projects/testProject/code/HUD",
                "projects/testProject/code/ModelBehaviour", "Kurama Engine ver alpha-2.0");
    }

    public void initScene() {
        MeshBuilderHints hints = new MeshBuilderHints();

        scene.hud = new TestHUD(this);

        scene.camera = new Camera(this,null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
                display.getWidth(), display.getHeight());

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);

        scene.ambientLight = new Vector(0.3f,0.3f,0.3f);

        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),1f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                (Mesh) null, null, directionalLightOrthoProjection, "Sun");

        directionalLight.setPos(new Vector(0,30,0));
        directionalLight.lightPosScale = 500;
        directionalLight.shouldCastShadow = false;
        directionalLight.setScale(100);
        directionalLight.setBehaviour(new SunRevolve());
        directionalLight.meshes.add(scene.loadMesh("res/glassball/glassball.obj", "sun_mesh", hints));
        scene.addDirectionalLight(directionalLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        scene.fog = new Fog(true, new Vector(new float[]{0.5f, 0.5f, 0.5f}), 0.005f);
        scene.fog = Fog.NOFOG;
        shouldDayNight = false;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Vector lightPos = new Vector(new float[]{0,0,10});
        PointLight sl_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, 0f);
        sl_pointLight.attenuation = new PointLight.Attenuation(0f,0f, 0.01f);
        Quaternion coneOrientation = Quaternion.getQuaternionFromEuler(0,0,0);
        SpotLight spotLight = new SpotLight(this,sl_pointLight, coneOrientation, 45,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH*4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT*4),
                (Mesh) null, null, null,"spotlight 1");

        spotLight.generateShadowProjectionMatrix(0.1f , 100, 1, 1);

        spotLight.meshes.add(scene.loadMesh("res/torch/test/hand_light.obj", "torchlight_mesh", hints));
//        spotLight.meshes.add(scene.loadMesh("res/apricot/Apricot_02_hi_poly.obj", "apricot", hints));
        spotLight.setScale(0.05f);
        spotLight.setPos(new Vector(new float[]{20,45f,12f}));

//        spotLight.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{1, 0, 0}), -30).
//                multiply(Quaternion.getAxisAsQuat(new Vector(0, 0, 1), 90)));

        spotLight.shouldCastShadow = false;
        spotLight.setBehaviour(new AttachToPlayer());
        scene.addSplotLight(spotLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Material quadMat = new Material();
        quadMat.matName = "shadowMapVisualizer";
        quadMat.texture = spotLight.shadowMap.depthMap;
        scene.hud.hudElements.get(0).meshes.get(0).materials.set(0, quadMat);
//     -------------------------------------------------------------------------------------------------------------------
//                                                   Second Spot Light

//        lightPos = new Vector(new float[]{0,0,10});
//        PointLight s2_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, 1f);
//        s2_pointLight.attenuation = new PointLight.Attenuation(0f,0f, 0.01f);
//        Quaternion coneOrientation_2 = Quaternion.getQuaternionFromEuler(-65,0,0);
//        SpotLight spotLight_2 = new SpotLight(this,s2_pointLight, coneOrientation_2, 25,
//                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
//                null, null, null,"spotlight 1");
//
//        spotLight_2.generateShadowProjectionMatrix(0.1f , 100, 1, 1);
//
//        scene.spotLights.add(spotLight_2);
//
//        spotLight_2.setPos(new Vector(new float[]{72,-44.7f,78.5f}));
//        spotLight_2.isOpaque = false;
// ------------------------------------------------------------------------------------------------------------------------

//        MiniBehaviour tempRot = ((m, params) -> {
//            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50* timeDelta);
//            Quaternion newQ = rot.multiply(m.getOrientation());
//            m.setOrientation(newQ);
//        });

        Texture tex = null;
        try {
            tex = new Texture("res/misc/grassblock.png");
        }catch (Exception e) {
            e.printStackTrace();
        }

        Material cubeMat = new Material(tex, "minecraftCubeMat");
        cubeMat.diffuseMap = cubeMat.texture;
        cubeMat.specularMap = cubeMat.texture;
        cubeMat.specularPower = 10;
        cubeMat.reflectance = 0.5f;

        float skyBoxScale = 1000;
        float boxScale = 1f;
        int boxCount = 100;
        float yRange = 60;

        Model skybox = scene.createModel(scene.loadMesh("res/misc/skybox.obj",
                "skybox_mesh", hints), "skybox", Arrays.asList(new String[]{DefaultRenderPipeline.skyboxShaderBlockID}));
        skybox.setScale(skyBoxScale);

        Material skyMat = new Material(new Texture("res/misc/skybox.png"),1, "SkyBox");
        skyMat.ambientColor = new Vector(new float[]{1f,1f,1f,1});
        skyMat.ambientColor = scene.ambientLight;
        skybox.meshes.get(0).materials.set(0,skyMat);
        scene.skybox = skybox;

        Vector[] bounds = Model.getBounds(scene.skybox.meshes.get(0));

        long seed = Utils.generateSeed("UchihaConan");
        System.out.println("seed: "+seed);
        float[][] heightMap = TerrainUtils.generateRandomHeightMap(boxCount,boxCount,5,0.5f, 0.01f,seed);
        Mesh cubeMesh = scene.loadMesh("res/misc/cube.obj", "cube_mesh", hints);

        for(int i = 0;i < 20;i++) {
            for(int y = 0;y < 20;y++) {
                Vector pos = bounds[0].removeDimensionFromVec(3).add(new Vector(new float[]{i*boxScale*2,y*boxScale*2,0}));
                Model cube = new Model(this,cubeMesh , "cube");
                cube.setScale(boxScale);
                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
//                cube.behaviour = new rotate();
                cube.meshes.get(0).materials.set(0,cubeMat);
                scene.addModel(cube, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            }
        }

        Model plant = scene.createModel(scene.loadMesh("res/plant/01Alocasia_obj.obj",
                "plantMesh", hints), "plant", Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
        plant.setPos(new Vector(15, 45, 10));
        plant.setScale(0.005f);

        Model terrain = TerrainUtils.createTerrainFromHeightMap(heightMap,boxCount/1,this,"terrain");
        terrain.meshes.get(0).meshIdentifier = "Terrain_mesh";
        Material ter = new Material();
        ter.matName = "TERRAIN";
        ter.texture = new Texture("res/misc/crystalTexture.jpg");
        ter.diffuseMap = ter.texture;
        ter.normalMap = new Texture("res/misc/crystalNormalMap.jpg");
        ter.specularMap = new Texture("res/misc/crystalSpecularMap.jpg");
        ter.reflectance = 1f;
        terrain.meshes.get(0).materials.set(0,ter);

        terrain.meshes.get(0).initOpenGLMeshData();
        terrain.setScale(boxCount,yRange,boxCount);
        scene.addModel(terrain, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

        scene.renderPipeline = new DefaultRenderPipeline(this);

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
        scene.cleanUp();
    }

    public void tick() {
        tickInput();
        if(scene.hud != null) {
            scene.hud.tick();
        }

        if(glfwWindowShouldClose(display.getWindow())) {
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
            ModelBehaviourTickInput params = new ModelBehaviourTickInput(timeDelta, scene);
            scene.modelID_model_map.values().forEach(m -> m.tick(params));
        }

        if(!isGameRunning) {
            pauseButtons.forEach((b) -> b.tick(mousePos,input.isLeftMouseButtonPressed));
        }
        else {
            calculate3DCamMovement();
            scene.camera.tick();
        }

    }

    public void tickInput() {

        Vector posDelta = new Vector(3,0);

        if(input.keyDown(input.W)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            posDelta = posDelta.add((z.scalarMul(-cameraSpeed)));
//            cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(input.B)) {
            writeSceneToFile();
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            posDelta = posDelta.add(z.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            posDelta = posDelta.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            posDelta = posDelta.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            posDelta = posDelta.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            posDelta = posDelta.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(input.ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(input.keyDown(input.UP_ARROW)) {
            DirectionalLight light = scene.directionalLights.get(0);
            scene.directionalLights.get(0).setPos(light.getPos().add(new Vector(0,timeDelta* 3,0)));
        }
        if(input.keyDown(input.DOWN_ARROW)) {
            var light = scene.directionalLights.get(0);
            scene.directionalLights.get(0).setPos(light.getPos().sub(new Vector(0,timeDelta* 3,0)));
        }

        if(isGameRunning) {
//            if(input.keyDownOnce(input.R)) {
//                cam.lookAtModel( scene.models.get(lookAtIndex));
//                posDelta = new Vector(3,0);
//            }

            if(input.keyDownOnce(input.LEFT_CONTROL)) {
                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                else speedMultiplier = 1;
            }

            if(input.keyDownOnce(input.F)) {
                if(targetFPS == display.getRefreshRate()) {
                    targetFPS = 10000;
                }
                else {
                    targetFPS = display.getRefreshRate();
                }
                System.out.println("Changed target resolution"+targetFPS);
            }

            if(input.keyDownOnce(input.V)) {
                display.toggleWindowModes();
            }
        }

        Vector newPos = scene.camera.getPos().add(posDelta);
        scene.camera.setPos(newPos);
//        Terrain.TerrainMovementDataPack terrainCollisionData = terrain.isPositionValid(newPos);
//        if(terrainCollisionData.isValid) {
//            this.cam.setPos(terrainCollisionData.validPosition);
//        }

    }

    public void calculate3DCamMovement() {
        if (mouseDelta.getNorm() != 0 && isGameRunning) {

            float yawIncrease   = mouseXSensitivity * timeDelta * -mouseDelta.get(0);
            float pitchIncrease = mouseYSensitivity * timeDelta * -mouseDelta.get(1);

            Vector currentAngle = scene.camera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = scene.camera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            scene.camera.setOrientation(q);
        }

    }

    public void render() {
        renderingEngine.render(scene);
        glfwSwapBuffers(display.getWindow());
        glfwPollEvents();
        input.poll();
    }

    public RenderingEngineGL getRenderingEngine() {
        return renderingEngine;
    }

    public DisplayLWJGL getDisplay() {
        return display;
    }

    public Camera getCamera() {
        return scene.camera;
    }

    public Input getInput() {
        return input;
    }
}
