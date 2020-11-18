package main;

import java.awt.*;
import java.util.*;
import java.util.List;

import engine.scene.Scene;
import engine.Effects.Fog;
import engine.Effects.ShadowMap;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Terrain.TerrainUtils;
import engine.display.Display;
import engine.display.DisplayLWJGL;
import engine.game.Game;
import engine.inputs.Input;
import engine.inputs.InputLWJGL;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.lighting.DirectionalLight;
import engine.Effects.Material;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.Model;
import engine.model.Model.MiniBehaviour;
import engine.model.MeshBuilder;
import engine.camera.Camera;
import engine.renderingEngine.RenderingEngine;
import engine.renderingEngine.RenderingEngineGL;
import engine.scene.SceneUtils;
import engine.utils.Logger;
import engine.utils.Utils;
import static engine.model.MeshBuilder.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class GameLWJGL extends Game implements Runnable {

    protected DisplayLWJGL display;
    protected Camera cam;
    protected InputLWJGL input;
    protected RenderingEngineGL renderingEngine;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    protected int lookAtIndex = 1;
    protected boolean isGameRunning = true;

    protected List<engine.GUI.Button> pauseButtons;
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);

        scene.ambientLight = new Vector(0.1f,0.1f,0.1f);
        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),0f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                null, null, directionalLightOrthoProjection, "light");

        directionalLight.setPos(new Vector(0,30,0));
        directionalLight.lightPosScale = 500;
        directionalLight.shouldCastShadow = false;
        scene.addDirectionalLight(directionalLight, renderingEngine.sceneShaderID);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        scene.fog = new Fog(true, new Vector(new float[]{0.5f, 0.5f, 0.5f}), 0.005f);
        scene.fog = Fog.NOFOG;
        shouldDayNight = false;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Vector lightPos = new Vector(new float[]{0,0,10});
        PointLight sl_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, 5f);
        sl_pointLight.attenuation = new PointLight.Attenuation(0f,0f, 0.01f);
        Quaternion coneOrientation = Quaternion.getQuaternionFromEuler(0,0,0);
        SpotLight spotLight = new SpotLight(this,sl_pointLight, coneOrientation, 45,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                null, null, null,"spotlight 1");

        spotLight.generateShadowProjectionMatrix(0.1f , 100, 1, 1);

        spotLight.mesh =  MeshBuilder.buildModelFromFileGL("res/torch/test/hand_light.obj", new MeshBuilderHints());
        spotLight.setScale(0.05f);
        spotLight.setPos(new Vector(new float[]{72,-44.7f,78.5f}));
        spotLight.shouldCastShadow = false;

        scene.addSplotLight(spotLight, renderingEngine.sceneShaderID);


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

        cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
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
        scene.hud = new TestHUD(this);

        Material quadMat = new Material();
        quadMat.matName = "shadowMapVisualizer";
        quadMat.texture = spotLight.shadowMap.depthMap;
        scene.hud.hudElements.get(0).mesh.materials.set(0, quadMat);

        initModels();
        initPauseScreen();

        display.setClearColor(0,0,0,1);
        cam.updateValues();
        targetFPS = display.getRefreshRate();

        try {
            SceneUtils.writeSceneToKE(scene, "res", "test", "Kurama Engine ver alpha-2.0");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initModels() {
        MiniBehaviour tempRot = ((m, params) -> {
            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50* timeDelta);
            Quaternion newQ = rot.multiply(m.getOrientation());
            m.setOrientation(newQ);
        });

        MeshBuilderHints hints = new MeshBuilderHints();

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

        Model skybox = new Model(this, MeshBuilder.buildModelFromFileGL("res/misc/skybox.obj",hints),"skybox");
        skybox.setScale(skyBoxScale);

        Material skyMat = new Material(new Texture("res/misc/skybox.png"),1, "SkyBox");
        skyMat.ambientColor = new Vector(new float[]{1f,1f,1f,1});
        skybox.mesh.materials.set(0,skyMat);
        scene.addSkyBlock(skybox, renderingEngine.skyboxShaderID);

        Vector[] bounds = Model.getBounds(scene.skybox.mesh);

        long seed = Utils.generateSeed("UchihaConan");
        System.out.println("seed: "+seed);
        float[][] heightMap = TerrainUtils.generateRandomHeightMap(boxCount,boxCount,5,0.5f, 0.01f,seed);
        Mesh cubeMesh = MeshBuilder.buildModelFromFileGL("res/misc/cube.obj", hints);

        for(int i = 0;i < 20;i++) {
            for(int y = 0;y < 20;y++) {
                Vector pos = bounds[0].removeDimensionFromVec(3).add(new Vector(new float[]{i*boxScale*2,y*boxScale*2,0}));
                Model cube = new Model(this,cubeMesh , "cube");
                cube.setScale(boxScale);
                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
                cube.mesh.materials.set(0,cubeMat);
                scene.addModel(cube, renderingEngine.sceneShaderID);
            }
        }

        Model plant = scene.createModel(scene.loadMesh("res/plant/01Alocasia_obj.obj", "plantMesh", hints), "plant", renderingEngine.sceneShaderID);
        plant.setPos(new Vector(15, 30, 5));
        plant.setScale(0.005f);

        Model terrain = TerrainUtils.createTerrainFromHeightMap(heightMap,boxCount/1,this,"terrain");
        scene.setUniqueMeshID(terrain.mesh);
        Material ter = new Material();
        ter.matName = "TERRAIN";
        ter.texture = new Texture("res/misc/crystalTexture.jpg");
        ter.diffuseMap = ter.texture;
        ter.normalMap = new Texture("res/misc/crystalNormalMap.jpg");
        ter.specularMap = new Texture("res/misc/crystalSpecularMap.jpg");
        ter.reflectance = 1f;
        terrain.mesh.materials.set(0,ter);

        terrain.mesh.initOpenGLMeshData();
        terrain.setScale(boxCount,yRange,boxCount);
        scene.addModel(terrain, renderingEngine.sceneShaderID);

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
        scene.hud.tick();

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
            Model.ModelTickInput params = new Model.ModelTickInput();
            params.timeDelta = timeDelta;

            scene.updateAllModels(params);

//            scene.pointLights.get(0).pos = cam.getPos();
//            scene.spotLights.get(0).setPos(cam.getPos());
            scene.spotLights.get(0).setPos(cam.getPos().sub(cam.getOrientation().getRotationMatrix().getColumn(2).removeDimensionFromVec(3)));
            scene.spotLights.get(0).setOrientation(cam.getOrientation());

//            scene.spotLights.get(1).setPos(cam.getPos());
//            scene.spotLights.get(1).setOrientation(cam.getOrientation());


            if(shouldDayNight) {
                DirectionalLight directionalLight = scene.directionalLights.get(0);
                SpotLight spotLight = scene.spotLights.get(0);
                float delta = (10f * timeDelta);
                float currentPitch = directionalLight.getOrientation().getPitchYawRoll().get(0);

                float lightAngle = currentPitch + delta;
                if (lightAngle > 180 || lightAngle < 0) {
                    directionalLight.intensity = 0;
                    //spotLight.pointLight.intensity = 0;
//                    if (lightAngle >= 360) {
//                        lightAngle = -90;
//                    }
                } else if (lightAngle <= 10 || lightAngle >= 170) {
                    float factor = (lightAngle > 10?180-lightAngle:lightAngle)/20f;
                    directionalLight.intensity = factor;
                    //spotLight.pointLight.intensity = 1;
//                    directionalLight.color.setDataElement(1, Math.min(factor, 0.9f));
//                    directionalLight.color.setDataElement(2, Math.max(factor, 0.5f));
                } else {
                    directionalLight.intensity = 1;
                    //spotLight.pointLight.intensity = 1;
                    directionalLight.color = new Vector(3, 1);
                }
                double angRad = Math.toRadians(lightAngle);
//                directionalLight.direction.setDataElement(0, (float) Math.sin(angRad));
//                directionalLight.direction.setDataElement(1, (float) Math.cos(angRad));
                Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}), delta);
                directionalLight.setOrientation(rot.multiply(directionalLight.getOrientation()));
//                spotLight.setOrientation(rot.multiply(spotLight.getOrientation()));
//                spotLight.setPos(spotLight.getOrientation().getRotationMatrix().getColumn(2).scalarMul(-10).add(new Vector(0,50,0)));

                scene.skybox.mesh.materials.get(0).ambientColor = new Vector(4, directionalLight.intensity);

//                System.out.println(currentPitch);
//                directionalLight.direction = cam.getOrientation().getRotationMatrix().getColumn(2);

            }

        }

        if(!isGameRunning) {
            System.out.println("paused");
            pauseButtons.forEach((b) -> b.tick(mousePos,input.isLeftMouseButtonPressed));
        }
        else {
            calculate3DCamMovement();
            cam.tick();
        }

    }

    public void tickInput() {

        Vector posDelta = new Vector(3,0);

        if(input.keyDown(input.W)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            posDelta = posDelta.add((z.scalarMul(-cameraSpeed)));
//            cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            posDelta = posDelta.add(z.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            posDelta = posDelta.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

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

        Vector newPos = cam.getPos().add(posDelta);
        cam.setPos(newPos);
//        Terrain.TerrainMovementDataPack terrainCollisionData = terrain.isPositionValid(newPos);
//        if(terrainCollisionData.isValid) {
//            this.cam.setPos(terrainCollisionData.validPosition);
//        }

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
        renderingEngine.render(scene,cam);
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
        return cam;
    }

    public Input getInput() {
        return input;
    }

}
