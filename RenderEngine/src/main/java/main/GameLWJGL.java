package main;

import HUD.TestHUD;
import ModelBehaviour.AttachToPlayer;
import ModelBehaviour.SunRevolve;
import engine.Effects.Fog;
import engine.Effects.Material;
import engine.Effects.ShadowMap;
import engine.Effects.Texture;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Mesh.Mesh;
import engine.Terrain.Terrain;
import engine.camera.Camera;
import engine.display.Display;
import engine.display.DisplayLWJGL;
import engine.game.Game;
import engine.geometry.MD5.MD5AnimModel;
import engine.geometry.MD5.MD5Model;
import engine.geometry.MD5.MD5Utils;
import engine.geometry.MeshBuilder;
import engine.geometry.MeshBuilderHints;
import engine.geometry.SceneUtils;
import engine.geometry.TerrainUtils;
import engine.inputs.Input;
import engine.inputs.InputLWJGL;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.AnimatedModel;
import engine.model.Model;
import engine.model.ModelBehaviourTickInput;
import engine.particle.FlowParticleGenerator;
import engine.particle.Particle;
import engine.particle.ParticleGeneratorTickInput;
import engine.renderingEngine.RenderingEngineGL;
import engine.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import engine.scene.Scene;
import engine.utils.Logger;
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
        display.displayMode = Display.DisplayMode.WINDOWED;
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

        initScene();
//        scene = SceneUtils.loadScene(this, "projects/testProject");

        initPauseScreen();

        renderingEngine.renderPipeline = scene.renderPipeline;
        renderingEngine.init(scene);

        display.setClearColor(0,0,0,1);
        scene.camera.updateValues();
        targetFPS = display.getRefreshRate();

        Logger.log("");
        for(var mat: scene.materialLibrary) {
            Logger.log(mat.matName);
        }
        Logger.log("");

    }

    public void writeSceneToFile() {
        SceneUtils.writeSceneToKE(scene, "projects", "testProject", null,
                null, "projects/testProject/code/HUD",
                "projects/testProject/code/ModelBehaviour", "Kurama Engine ver alpha-2.0");
    }

    public void initScene() {
        scene.renderPipeline = new DefaultRenderPipeline(this);

        MeshBuilderHints hints = new MeshBuilderHints();

        scene.hud = new TestHUD(this);

        scene.camera = new Camera(this,null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
                display.getWidth(), display.getHeight());

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);
//        hints.initLWJGLAttribs = false;
        scene.ambientLight = new Vector(0.3f,0.3f,0.3f);
        var sunMesh = scene.loadMesh("res/glassball/glassball.obj", "sun_mesh", hints);
        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),1f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 4),
                sunMesh, null, directionalLightOrthoProjection, "Sun");
        scene.renderPipeline.initializeMesh(sunMesh);
//        sunMesh.initOpenGLMeshData();
        directionalLight.setPos(new Vector(0,500,0));
        directionalLight.lightPosScale = 500;
        directionalLight.shouldCastShadow = false;
        directionalLight.setScale(100);
        directionalLight.setBehaviour(new SunRevolve());
        scene.addDirectionalLight(directionalLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        scene.fog = new Fog(true, new Vector(new float[]{0.5f, 0.5f, 0.5f}), 0.005f);
        scene.fog = Fog.NOFOG;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Vector lightPos = new Vector(new float[]{0,0,10});
        PointLight sl_pointLight = new PointLight(new Vector(new float[]{1, 1, 1}), lightPos, 1f);
        sl_pointLight.attenuation = new PointLight.Attenuation(0f,0.1f, 0f);
        Quaternion coneOrientation = Quaternion.getQuaternionFromEuler(0,0,0);
        SpotLight spotLight = new SpotLight(this,sl_pointLight, coneOrientation, 45,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH*4, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT*4),
                (Mesh) null, null, null,"spotlight 1");

        spotLight.generateShadowProjectionMatrix(0.1f , 100, 1, 1);

//        spotLight.addMesh(scene.loadMesh("res/torch/test/hand_light.obj", "torchlight_mesh", hints));
//        spotLight.meshes.add(scene.loadMesh("res/apricot/Apricot_02_hi_poly.obj", "apricot", hints));
        spotLight.setScale(0.05f);
        spotLight.setPos(new Vector(new float[]{20,45f,12f}));
        spotLight.shouldRender = false;

//        spotLight.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{1, 0, 0}), -30).
//                multiply(Quaternion.getAxisAsQuat(new Vector(0, 0, 1), 90)));

        spotLight.shouldCastShadow = false;
        spotLight.setBehaviour(new AttachToPlayer());
        scene.addSplotLight(spotLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

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

        float skyBoxScale = 1000;
        float boxScale = 1f;
        int boxCount = 100;
        float yRange = 60;

        var skybox_mesh = scene.loadMesh("res/misc/skybox.obj", "skybox_mesh", hints);
        scene.renderPipeline.initializeMesh(skybox_mesh);
        Material skyMat = new Material(new Texture("res/misc/skybox.png"),1, "SkyBox");
        skyMat.ambientColor = new Vector(new float[]{1f,1f,1f,1f});
        skybox_mesh.materials.set(0, skyMat);

        Model skybox = scene.createModel(skybox_mesh, "skybox", Arrays.asList(new String[]{DefaultRenderPipeline.skyboxShaderBlockID}));
        skybox.setScale(skyBoxScale);
        scene.skybox = skybox;

        Vector[] bounds = Model.getBounds(scene.skybox.meshes.get(0));

        long seed = Utils.generateSeed("UchihaConan");
        System.out.println("seed: "+seed);
        float[][] heightMap = engine.geometry.Utils.generateRandomHeightMap(boxCount,boxCount,5,0.5f, 0.01f,seed);
        hints.isInstanced = true;
        hints.numInstances = 600;
        Mesh cubeMesh = scene.loadMesh("res/misc/cube.obj", "cube_mesh", hints);
        scene.renderPipeline.initializeMesh(cubeMesh);
        hints.isInstanced = false;

        Texture cubeTexture = new Texture("res/misc/grassblock.png");
        Material cubeMat = new Material(cubeTexture, "minecraftCubeMat");
        cubeMat.diffuseMap = cubeMat.texture;
        cubeMat.specularMap = cubeMat.texture;
        cubeMat.specularPower = 10;
        cubeMat.reflectance = 0.5f;
        cubeMesh.materials.set(0, cubeMat);

        for(int i = 0;i < 30;i++) {
            for(int y = 0;y < 20;y++) {
                Vector pos = bounds[0].removeDimensionFromVec(3).add(new Vector(new float[]{i*boxScale*2,y*boxScale*2,0}));
                Model cube = new Model(this,cubeMesh , "cube");
                cube.setScale(boxScale);
                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
//                cube.behaviour = new rotate();
                scene.addModel(cube, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            }
        }

//        var plantMesh = scene.loadMesh("res/plant/01Alocasia_obj.obj", "plantMesh", hints);
//        plantMesh.initOpenGLMeshData();
//        Model plant = scene.createModel(plantMesh, "plant", Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
//        plant.setPos(new Vector(15, 45, 10));
//        plant.setScale(0.005f);

        Mesh terrain_mesh = TerrainUtils.createTerrainFromHeightMap(heightMap,boxCount/1,"Terrain_mesh");
        Material ter_mat = new Material();
        ter_mat.matName = "TERRAIN";
        ter_mat.texture = new Texture("res/misc/crystalTexture.jpg");
        ter_mat.diffuseMap = ter_mat.texture;
        ter_mat.normalMap = new Texture("res/misc/crystalNormalMap.jpg");
        ter_mat.specularMap = new Texture("res/misc/crystalSpecularMap.jpg");
        ter_mat.reflectance = 1f;
        terrain_mesh.materials.set(0,ter_mat);
        scene.renderPipeline.initializeMesh(terrain_mesh);
        var terrain = new Terrain(this, terrain_mesh, "Terrain", heightMap.length, heightMap[0].length, 2);
        terrain.setScale(boxCount,yRange,boxCount);
        scene.addModel(terrain, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

        hints.isInstanced = true;
        hints.numInstances = 2;
        MD5Model monster_md5 = new MD5Model("res/monster/monster.md5mesh");
        List<Mesh> monsterMeshes = MD5Utils.generateMeshes(monster_md5, new Vector(1f, 1f, 1f, 1f), hints);
        monsterMeshes.get(0).meshIdentifier = "monsterMesh1";
        monsterMeshes.forEach(m -> scene.renderPipeline.initializeMesh(m));
        hints.isInstanced = false;

        var monsterAnim = new MD5AnimModel("res/monster/monster.md5anim");
        var frames_inv = MD5Utils.generateAnimationFrames(monsterAnim, monster_md5);
        var frames = frames_inv.get(0);
        var invMats = frames_inv.get(1);

        for(int i = 0; i < 5;i++) {
            Model monster = scene.createAnimatedModel(monsterMeshes, frames, invMats, monsterAnim.frameRate, "monster"+i,
                    Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            monster.setScale(0.1f);
            monster.setPos(new Vector(10 + (i*10), 30, 10));
            monster.setOrientation(Quaternion.getAxisAsQuat(new Vector(1, 0, 0), -90).multiply(Quaternion.getAxisAsQuat(0, 0, 1, -90)));
        }
        var madara = new MD5Model("res/madara/madara.md5mesh");
        hints.isInstanced = true;
        hints.numInstances = 1;
        var madara_meshes = MD5Utils.generateMeshes(madara, new Vector(1f, 1f, 1f, 1f), hints);
        madara_meshes.get(0).meshIdentifier = "madara1";
        hints.isInstanced = false;
        madara_meshes.stream().forEach(m -> scene.renderPipeline.initializeMesh(m));

        var madara_model = scene.createModel(madara_meshes, "madara", Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
        madara_model.setScale(5f);
        madara_model.setPos(new Vector(10, 30, 30));
        madara_model.setOrientation(Quaternion.getAxisAsQuat(new Vector(1, 0,0), -90).multiply(Quaternion.getAxisAsQuat(0, 0, 1, -90)));
//
        var partHints = new MeshBuilderHints();
        partHints.isInstanced = true;
        partHints.shouldGenerateTangentBiTangent = false;
        partHints.numInstances = 100;
        Mesh partMesh = MeshBuilder.buildMesh("res/misc/particle.obj", partHints);
        scene.renderPipeline.initializeMesh(partMesh);
        Texture partTex = new Texture("res/misc/particle_anim.png", 4,4);
        partMesh.materials.get(0).texture = partTex;

        Particle particle = new Particle(this, partMesh, new Vector(-1f, 0f, 0), new Vector(-5f, 0f, 0),
                5,0.1f, "baseParticle");
        particle.scale = new Vector(3, 1f);
        particle.pos = new Vector(8.5f, 39, 30);
        var particleGenerator = new FlowParticleGenerator(particle, 100, 0.1f, "generator");
        particleGenerator.posRange = new Vector(0.1f, 0.1f, 0.2f);
        particleGenerator.velRange = new Vector(-0.2f, 1, 1f);
        particleGenerator.accelRange = new Vector(0,0.5f,0.2f);
        particleGenerator.animUpdateRange = 0.1f;
        scene.addParticleGenerator(particleGenerator, Arrays.asList(new String[]{DefaultRenderPipeline.particleShaderBlockID}));

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

            ParticleGeneratorTickInput param = new ParticleGeneratorTickInput(timeDelta);
            scene.particleGenerators.forEach(gen -> gen.tick(param));
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

        if(input.keyDown(input.UP_ARROW)) {
            int counter = 0;
            while(true) {
                AnimatedModel monster = (AnimatedModel) scene.modelID_model_map.get("monster"+counter);
                if(monster == null) {
                    break;
                }
                monster.cycleFrame(0.5f * (counter+1));
                monster.generateCurrentSkeleton(monster.currentFrame);
                counter++;
            }
        }

        if(input.keyDown(input.DOWN_ARROW)) {
            int counter = 0;
            while(true) {
                AnimatedModel monster = (AnimatedModel) scene.modelID_model_map.get("monster"+counter);
                if(monster == null) {
                    break;
                }
                monster.cycleFrame(-0.5f * (counter+1));
                monster.generateCurrentSkeleton(monster.currentFrame);
                counter++;
            }
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
        scene.hasMatLibraryUpdated = false;
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
