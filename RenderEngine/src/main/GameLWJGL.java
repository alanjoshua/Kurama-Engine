package main;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import engine.DataStructure.Mesh.Vertex;
import engine.DataStructure.Scene;
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
import engine.model.Terrain;
import engine.renderingEngine.RenderingEngine;
import engine.renderingEngine.RenderingEngineGL;
import engine.utils.Utils;
import static engine.model.MeshBuilder.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;

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

    Map<String, Mesh> meshInstances;
    Terrain terrain;
    private boolean shouldDayNight = false;

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

        meshInstances = new HashMap<>();

        renderingEngine = new RenderingEngineGL(this);

        display = new DisplayLWJGL(this);
        display.startScreen();

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);

        scene.ambientLight = new Vector(0.05f,0.05f,0.05f);
        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),1f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                null, null, directionalLightOrthoProjection, "light");

//        scene.directionalLights.add(directionalLight);
        directionalLight.setPos(new Vector(0,30,0));
        directionalLight.lightPosScale = 500;
        directionalLight.isOpaque = false;
//        scene.models.add(directionalLight);
        scene.fog = new Fog(true, new Vector(new float[]{0.5f, 0.5f, 0.5f}), 0.005f);
        scene.fog = Fog.NOFOG;
        shouldDayNight = false;

        DirectionalLight directionalLight2 = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{0,1,0}),-180),0f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                null, null, directionalLightOrthoProjection, "light2");

        //scene.directionalLights.add(directionalLight2);
        directionalLight2.setPos(new Vector(-0.2f,1.5f,-20.26f));
        directionalLight2.lightPosScale = 500;
        directionalLight2.isOpaque = false;
//        scene.models.add(directionalLight2);

        Vector lightColor = new Vector(new float[]{1,1,1});
        Vector lightPos = new Vector(new float[]{-1f,0f,0f});
        float lightIntensity = 0f;
        PointLight pointLight = new PointLight(lightColor,lightPos,lightIntensity);
        pointLight.attenuation = new PointLight.Attenuation(0,0f,0.01f);
        pointLight.pos = new Vector(-0.2f,30f,-10.26f);
//        scene.pointLights.add(pointLight);
////
        lightPos = new Vector(new float[]{0,0,10});
        PointLight sl_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, 5f);
        sl_pointLight.attenuation = new PointLight.Attenuation(0f,0f, 0.01f);
        Quaternion coneOrientation = Quaternion.getQuaternionFromEuler(0,0,0);
        SpotLight spotLight = new SpotLight(this,sl_pointLight, coneOrientation, 25,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                null, null, null,"spotlight 1");

//        spotLight.shadowProjectionMatrix = Matrix.buildOrthographicProjectionMatrix(1,700,10,-10,-10,10);
        spotLight.generateShadowProjectionMatrix(0.1f , 100, 1, 1);

        scene.spotLights.add(spotLight);
        spotLight.setPos(new Vector(new float[]{72,-44.7f,78.5f}));
        spotLight.isOpaque = false;

//     -------------------------------------------------------------------------------------------------------------------
//                                                   Second Spot Light

        lightPos = new Vector(new float[]{0,0,10});
        PointLight s2_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, 1f);
        s2_pointLight.attenuation = new PointLight.Attenuation(0f,0f, 0.01f);
        Quaternion coneOrientation_2 = Quaternion.getQuaternionFromEuler(-65,0,0);
        SpotLight spotLight_2 = new SpotLight(this,s2_pointLight, coneOrientation_2, 25,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                null, null, null,"spotlight 1");

        spotLight_2.generateShadowProjectionMatrix(0.1f , 100, 1, 1);

        scene.spotLights.add(spotLight_2);

        spotLight_2.setPos(new Vector(new float[]{72,-44.7f,78.5f}));
        spotLight_2.isOpaque = false;
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
        hud = new TestHUD(this);
        hud.hudElements.get(0).mesh.materials.get(0).texture = spotLight.shadowMap.depthMap;

        initModels();
        initPauseScreen();

        display.setClearColor(0,0,0,1);
        cam.updateValues();
        targetFPS = ((DisplayLWJGL)display).getRefreshRate();

    }

    public void initModels() {
        MiniBehaviour tempRot = ((m, params) -> {
            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50* timeDelta);
            Quaternion newQ = rot.multiply(m.getOrientation());
            m.setOrientation(newQ);
        });

        MeshBuilder.ModelBuilderHints hints = new MeshBuilder.ModelBuilderHints();
        hints.shouldSmartBakeVertexAttributes = true;
        hints.addRandomColor = false;
        hints.initLWJGLAttribs = true;

        Texture tex = null;
        try {
            tex = new Texture("res/misc/grassblock.png");
        }catch (Exception e) {
            e.printStackTrace();
        }

        Material cubeMat = new Material(tex);
        cubeMat.diffuseMap = cubeMat.texture;
        cubeMat.specularMap = cubeMat.texture;
        cubeMat.specularPower = 10;
        cubeMat.reflectance = 0.5f;

        float skyBoxScale = 1000;
        float boxScale = 1f;
        int boxCount = 100;
        float yRange = 60;

        hints.shouldSmartBakeVertexAttributes = false;
        hints.shouldGenerateTangentBiTangent = false;
        Mesh sun =buildModelFromFileGL("res/glassball/glassball.obj",meshInstances,hints);
//        scene.directionalLights.get(0).mesh = sun;
//        scene.directionalLights.get(0).calculateBoundingBox();
//        scene.directionalLights.get(0).setScale(100);
//        scene.directionalLights.get(1).mesh = sun;

//        scene.directionalLights.get(1).setScale(100);
//        scene.spotLights.get(0).mesh = sun;
//        scene.spotLights.get(0).calculateBoundingBox();

        hints.shouldGenerateTangentBiTangent = true;
        hints.shouldGenerateTangentBiTangent = false;
        hints.shouldSmartBakeVertexAttributes = true;
        scene.skybox = new Model(this, MeshBuilder.buildModelFromFileGL("res/misc/skybox.obj",meshInstances,hints),"skybox");
        scene.skybox.setScale(skyBoxScale);

        Material skyMat = new Material(new Texture("res/misc/skybox.png"),1);
        //skyMat.texture = scene.spotLights.get(0).shadowMap.depthMap;
        skyMat.ambientColor = new Vector(new float[]{1f,1f,1f,1});
        scene.skybox.mesh.materials.set(0,skyMat);
        Vector[] bounds = Model.getBounds(scene.skybox.mesh);
//        scene.skybox = null;

        hints.shouldSmartBakeVertexAttributes = true;
        hints.shouldGenerateTangentBiTangent = true;

        scene.spotLights.get(0).mesh =  MeshBuilder.buildModelFromFileGL("res/torch/test/hand_light.obj", meshInstances, hints);
        scene.spotLights.get(0).setScale(0.05f);
        scene.models.add(scene.spotLights.get(0));

//        Model torch = new Model(this,  MeshBuilder.buildModelFromFileGL("res/torch/test/hand_light.obj", meshInstances, hints), "torch");
        scene.spotLights.get(0).setPos(new Vector(7, 30, 20));
//        scene.models.add(torch);

//        hints.shouldRotate = 0;

//        Model testQuad = new Model(this,MeshBuilder.buildModelFromFileGL("res/misc/quad.obj",meshInstances,hints),"quad");
//        testQuad.setPos(new Vector(-0.2f,30f,-15.26f));
//        testQuad.setScale(5);
//        testQuad.isOpaque=false;
//        testQuad.setOrientation(Quaternion.getQuaternionFromEuler(0,0,0));
//        scene.models.add(testQuad);
//        testQuad.mesh.materials.get(0).texture = scene.spotLights.get(0).shadowMap.depthMap;

        long seed = Utils.generateSeed("UchihaConan");
        System.out.println("seed: "+seed);
        float[][] heightMap = TerrainUtils.generateRandomHeightMap(boxCount,boxCount,5,0.5f, 0.01f,seed);
        Mesh cubeMesh = MeshBuilder.buildModelFromFileGL("res/misc/cube.obj", meshInstances, hints);

//        for(int i = 0;i < heightMap.length;i++) {
//            for(int j = 0;j < heightMap[i].length;j++) {
//                float y = (int)(heightMap[i][j] * yRange * 2) * boxScale*2;
//                Vector pos = bounds[0].removeDimensionFromVec(3).add(new Vector(new float[]{i*boxScale*2,y,j*boxScale*2}));
//                Model cube = new Model(this,cubeMesh , "cube");
//                cube.setScale(boxScale);
//                cube.setPos(pos.sub(new Vector(new float[]{boxCount*boxScale,0,boxCount*boxScale})));
////                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
//                //cube.setMiniBehaviourObj(tempRot);
////               pos.sub(new Vector(new float[]{heightMap.length,0,heightMap[i].length}).scalarMul(boxScale))
//                cube.mesh.materials.set(0,cubeMat);
//                scene.models.add(cube);
//            }
//        }

//        for(int i = 0;i < 10;i++) {
//            for(int j = 0;j < 1;j++) {
//                float y = (int)(heightMap[i][j] * yRange * 2) * boxScale*2;
//                Vector pos = bounds[0].removeDimensionFromVec(3).add(new Vector(new float[]{i*boxScale*2,y,j*boxScale*2}));
//                Model cube = new Model(this,cubeMesh , "cube");
//                cube.setScale(boxScale);
////                cube.setPos(pos.sub(new Vector(new float[]{boxCount*boxScale,0,boxCount*boxScale})));
//                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
//                //cube.setMiniBehaviourObj(tempRot);
////               pos.sub(new Vector(new float[]{heightMap.length,0,heightMap[i].length}).scalarMul(boxScale))
//                cube.mesh.materials.set(0,cubeMat);
//                scene.models.add(cube);
//            }
//        }

        for(int i = 0;i < 10;i++) {
            for(int y = 0;y < 10;y++) {

                Vector pos = bounds[0].removeDimensionFromVec(3).add(new Vector(new float[]{i*boxScale*2,y*boxScale*2,0}));
                Model cube = new Model(this,cubeMesh , "cube");
                cube.setScale(boxScale);
//                cube.setPos(pos.sub(new Vector(new float[]{boxCount*boxScale,0,boxCount*boxScale})));
                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
                //cube.setMiniBehaviourObj(tempRot);
//               pos.sub(new Vector(new float[]{heightMap.length,0,heightMap[i].length}).scalarMul(boxScale))
                cube.mesh.materials.set(0,cubeMat);
                scene.models.add(cube);
            }
        }
//
//        Model apricot = new Model(this,buildModelFromFileGL("res/apricot/Apricot_02_hi_poly.obj",meshInstances,hints),"apricot2");
//        apricot.setPos(apricot.getPos().add(new Vector(7,30,10)));
//        apricot.setScale(0.5f);
//        scene.models.add(apricot);

        hints.shouldSmartBakeVertexAttributes = false;
        hints.shouldDumbBakeVertexAttributes = true;
        Model plant = new Model(this, buildModelFromFileGL("res/plant/01Alocasia_obj.obj", meshInstances, hints), "plant");
        plant.setPos(new Vector(7, 30, 10));
        plant.setScale(0.005f);
        scene.models.add(plant);
//
//        Model plane = new Model(this,buildModelFromFileGL("res/E-45-Aircraft/E 45 Aircraft_obj.obj",meshInstances,hints),"plane");
//        plane.setPos(plane.getPos().add(new Vector(0,5,0)));
//        scene.models.add(plane);
//
//        Model wolf = new Model(this,buildModelFromFileGL("res/wolf/Wolf_One_obj.obj",meshInstances,hints),"wolf");
//        scene.models.add(wolf);
//        wolf.setPos(wolf.getPos().add(new Vector(0,50,10)));
//        wolf.setScale(5);
//
//        Model spider = new Model(this,buildModelFromFileGL("res/spider/obj/Only_Spider_with_Animations_Export.obj",meshInstances,hints),"spider");
//        scene.models.add(spider);
//        spider.setPos(spider.getPos().add(new Vector(0,50,10)));
//        spider.setScale(0.1f);
//

        terrain = TerrainUtils.createTerrainFromHeightMap(heightMap,boxCount/1,this,"terrain");
        Material ter = terrain.mesh.materials.get(0);
        ter.texture = new Texture("res/misc/crystalTexture.jpg");
        ter.diffuseMap = ter.texture;
        ter.normalMap = new Texture("res/misc/crystalNormalMap.jpg");
        ter.specularMap = new Texture("res/misc/crystalSpecularMap.jpg");
        ter.reflectance = 1f;
        terrain.mesh.materials.set(0,ter);

        terrain.mesh.initOpenGLMeshData();
        terrain.setScale(boxCount,yRange,boxCount);
        scene.models.add(terrain);

//        Model livingRoom = new Model(this,buildModelFromFileGL("res/livingRoom/luxuryHouseInterior.obj",meshInstances,hints),"livingRoom");
//        livingRoom.setScale(0.1f);
//        scene.models.add(livingRoom);

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

            scene.models.forEach(m -> m.tick(params));

//            scene.pointLights.get(0).pos = cam.getPos();
//            scene.spotLights.get(0).setPos(cam.getPos().sub(cam.getOrientation().getRotationMatrix().getColumn(2).removeDimensionFromVec(3)));
//            scene.spotLights.get(0).setOrientation(cam.getOrientation());

            scene.spotLights.get(1).setPos(cam.getPos());
            scene.spotLights.get(1).setOrientation(cam.getOrientation());


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
            if(input.keyDownOnce(input.R)) {
                cam.lookAtModel( scene.models.get(lookAtIndex));
                posDelta = new Vector(3,0);
            }

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
        renderingEngine.render(scene,hud,cam);
        glfwSwapBuffers(display.getWindow());
        glfwPollEvents();
        input.poll();
    }

    public RenderingEngine getRenderingEngine() {
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

    public List<Model> getModels() {
        return  scene.models;
    }

    public void setModels(List<Model> models) {
        scene.models = models;
    }

}
