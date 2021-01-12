package main;

import Kurama.Effects.Fog;
import Kurama.GUI.MasterWindow;
import Kurama.GUI.Rectangle;
import Kurama.GUI.Text;
import Kurama.GUI.automations.*;
import Kurama.GUI.constraints.MaxHeight;
import Kurama.GUI.constraints.PosXYTopLeftAttachPercent;
import Kurama.GUI.constraints.PosXYTopLeftAttachPix;
import Kurama.GUI.constraints.WidthHeightPercent;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.Terrain.Terrain;
import Kurama.audio.SoundBuffer;
import Kurama.audio.SoundManager;
import Kurama.camera.Camera;
import Kurama.display.Display;
import Kurama.display.DisplayLWJGL;
import Kurama.font.FontTexture;
import Kurama.game.Game;
import Kurama.geometry.MD5.MD5AnimModel;
import Kurama.geometry.MD5.MD5Model;
import Kurama.geometry.MD5.MD5Utils;
import Kurama.geometry.MeshBuilder;
import Kurama.geometry.MeshBuilderHints;
import Kurama.geometry.TerrainUtils;
import Kurama.inputs.Input;
import Kurama.inputs.InputLWJGL;
import Kurama.lighting.DirectionalLight;
import Kurama.lighting.PointLight;
import Kurama.lighting.SpotLight;
import Kurama.model.AnimatedModel;
import Kurama.model.Model;
import Kurama.model.modelBehaviour.AttachToCamera;
import Kurama.model.modelBehaviour.BehaviourTickInput;
import Kurama.particle.FlowParticleGenerator;
import Kurama.particle.Particle;
import Kurama.particle.ParticleGeneratorTickInput;
import Kurama.renderingEngine.RenderingEngineGL;
import Kurama.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import Kurama.renderingEngine.ginchan.Gintoki;
import Kurama.scene.Scene;
import Kurama.shadow.ShadowMap;
import Kurama.utils.Logger;
import Kurama.utils.Utils;
import ModelBehaviour.SunRevolve;

import java.awt.*;
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
    protected boolean prevGameState = false;
//    protected Vector mouseDelta;
//    protected Vector mousePos;

    public Camera playerCamera;
    public Text fpsText;

    public GameLWJGL(String threadName) {
        super(threadName);
    }

    public void init() {
        scene = new Scene(this);

        renderingEngine = new RenderingEngineGL(this);

        display = new DisplayLWJGL(this);
        display.displayMode = Display.DisplayMode.WINDOWED;
        display.startScreen();

        input = new InputLWJGL(this, display);

        scene.renderPipeline = new DefaultRenderPipeline(this);
        renderingEngine.sceneRenderPipeline = scene.renderPipeline;
        renderingEngine.guiRenderPipeline = new Gintoki(this);
        renderingEngine.init(scene);

        playerCamera = new Camera(this,null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
                Display.defaultWindowedWidth, Display.defaultWindowedHeight);
        playerCamera.shouldPerformFrustumCulling = true;

        glfwSetFramebufferSizeCallback(display.getWindow(), (window, width, height) -> {
            glViewport(0,0,width,height);
            display.windowResolution = new Vector(new float[]{width, height});
        });
        scene.cameras.add(playerCamera);

        masterComponent = new MasterWindow(display, input,"masterWindow");
        masterComponent
                .setColor(new Vector(1,0,0,0.5f))
                .setContainerVisibility(false);

        var leftDivide =
                new Rectangle(masterComponent, "leftHalf")
                .setTexture(new Texture(playerCamera.renderBuffer.textureId))
                .addConstraint(new WidthHeightPercent(0.75f, 1f))
                .addAutomation(new ResizeCameraRenderResolution(playerCamera))
                .addOnMouseOvertAction(new SetOverlayColor(new Vector(1,0.5f,0.9f,0.1f)))
                .addOnMouseLeftAction(new RemoveOverlayColor());
        masterComponent.children.add(leftDivide);

        fpsText =
            (Text)new Text(masterComponent, new FontTexture(new Font("Arial", Font.PLAIN, 14), FontTexture.defaultCharSet), "fps")
            .addConstraint(new PosXYTopLeftAttachPix(20, 20))
            .addAutomation(new DisplayFPS(this, "fps: "))
            .setOverlayColor(new Vector(1,0,0,0.5f));
        masterComponent.children.add(fpsText);

        var rightDivide =
                new Rectangle(masterComponent, "rightHalf")
                .setColor(new Vector(0.5f, 0.4f, 0.9f, 1f))
                .addConstraint(new WidthHeightPercent(0.25f, 1f))
                .addConstraint(new PosXYTopLeftAttachPercent(0.75f,0));
        masterComponent.children.add(rightDivide);

        var square1 =
                new Rectangle(rightDivide, "s1")
                .setColor(new Vector(0.9f, 0.5f, 0.4f, 0.5f))
                .addConstraint(new WidthHeightPercent(0.5f, 0.2f))
                .addConstraint(new MaxHeight(100))
                .addConstraint(new PosXYTopLeftAttachPercent(0.1f, 0.1f))
                .addOnMouseOvertAction(new SetOverlayColor(new Vector(1,0,0,1)))
                .addOnMouseLeftAction(new RemoveOverlayColor())
                .addAutomation(new Rotate(new Vector(0,0,1), 50))
                .addOnClickAction(new Log("Clicked button 1"));
        rightDivide.children.add(square1);

        var squareIn =
                new Rectangle(square1, "ss1")
                .setColor(new Vector(0.1f, 0.3f, 0.7f, 0.5f))
                .addConstraint(new WidthHeightPercent(0.5f, 0.5f))
                .addConstraint(new PosXYTopLeftAttachPercent(0.1f, 0.1f))
                .addOnClickAction(new Log("Clicked button2"))
                .addAutomation(new OnlyChildClicked());
        square1.children.add(squareIn);

        var square2 =
                new Rectangle(rightDivide, "s2")
                .setColor(new Vector(0.9f, 0.5f, 0.4f, 0.5f))
                .addConstraint(new WidthHeightPercent(0.5f, 0.2f))
                .addConstraint(new MaxHeight(100))
                .addConstraint(new PosXYTopLeftAttachPercent(0.1f, 0.5f))
                .addOnMouseOvertAction(new SetOverlayColor(new Vector(1,0,0,0.5f)))
                .addOnMouseLeftAction(new RemoveOverlayColor());
        rightDivide.children.add(square2);

        var text =
                new Text(square2, new FontTexture(new Font("Arial", Font.PLAIN, 20), FontTexture.defaultCharSet), "text")
                .setText("Alan")
                .addConstraint(new PosXYTopLeftAttachPercent(0.1f, 0.1f));
        square2.children.add(text);

        try {
            var soundManager = new SoundManager();
            soundManager.init();
            scene.soundManager = soundManager;
            SoundBuffer madaraSound = new SoundBuffer("madara", "res/sampleAudio/madaraFirestyle.ogg");
            soundManager.addSoundBuffer(madaraSound);
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        initScene();

        display.setClearColor(0,0,0,1);
        scene.cameras.forEach(Camera::updateValues);
        targetFPS = display.getRefreshRate();

        Logger.log("");
        for(var mat: scene.materialLibrary) {
            Logger.log(mat.matName);
        }
        Logger.log("");

    }

    public void writeSceneToFile() {
//        SceneUtils.writeSceneToKE(scene, "projects", "testProject", null,
//                null, "projects/testProject/code/HUD",
//                "projects/testProject/code/ModelBehaviour", "Kurama Engine ver alpha-2.0");
    }

    public void initScene() {
        MeshBuilderHints hints = new MeshBuilderHints();

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);
//        hints.initLWJGLAttribs = false;
        scene.ambientLight = new Vector(0.3f,0.3f,0.3f);
        var sunMesh = scene.loadMesh("res/glassball/glassball.obj", "sun_mesh", hints);
        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),1f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT),
                sunMesh, null, directionalLightOrthoProjection, "Sun");
        scene.renderPipeline.initializeMesh(sunMesh);
//        sunMesh.initOpenGLMeshData();
        directionalLight.setPos(new Vector(0,500,0));
        directionalLight.lightPosScale = 500;
        directionalLight.shouldSelfCastShadow = false;
        directionalLight.doesProduceShadow = true;
        directionalLight.setScale(100);
        directionalLight.addBehavior(new SunRevolve());
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
                null,
                (Mesh) null, null, null,"spotlight 1");

//        spotLight.generateShadowProjectionMatrix(0.1f , 100, 1, 1);
        spotLight.shadowProjectionMatrix = Matrix.getIdentityMatrix(4);
        spotLight.doesProduceShadow = false;

//        spotLight.addMesh(scene.loadMesh("res/torch/test/hand_light.obj", "torchlight_mesh", hints));
//        spotLight.meshes.add(scene.loadMesh("res/apricot/Apricot_02_hi_poly.obj", "apricot", hints));
        spotLight.setScale(0.05f);
        spotLight.setPos(new Vector(new float[]{20,45f,12f}));
        spotLight.shouldRender = false;

//        spotLight.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{1, 0, 0}), -30).
//                multiply(Quaternion.getAxisAsQuat(new Vector(0, 0, 1), 90)));

        spotLight.shouldSelfCastShadow = false;
        spotLight.addBehavior(new AttachToCamera(playerCamera));
        scene.addSplotLight(spotLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));

//     -------------------------------------------------------------------------------------------------------------------

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
        float[][] heightMap = Kurama.geometry.Utils.generateRandomHeightMap(boxCount,boxCount,5,0.5f, 0.01f,seed);
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
                cube.setScale(boxScale,boxScale,1);
                cube.setPos(pos.add(new Vector(new float[]{0,25,0})));
//                cube.behaviour = new rotate();
                scene.addModel(cube, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            }
        }

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
        hints.numInstances = 5;
        MD5Model monster_md5 = new MD5Model("res/monster/monster.md5mesh");
        List<Mesh> monsterMeshes = MD5Utils.generateMeshes(monster_md5, new Vector(1f, 1f, 1f, 1f), hints);
        monsterMeshes.forEach(m -> {
            m.boundingRadius = 3;
            scene.renderPipeline.initializeMesh(m);
        });
        hints.isInstanced = false;

        var monsterAnim = new MD5AnimModel("res/monster/monster.md5anim");
        var frames = MD5Utils.generateAnimationFrames(monsterAnim, monster_md5);

        for(int i = 0; i < 5;i++) {
            Model monster = scene.createAnimatedModel(monsterMeshes, frames, monsterAnim.frameRate, "monster"+i,
                    Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            monster.setScale(0.1f);
            monster.setPos(new Vector(10 + (i*10), 30, 10));
            monster.setOrientation(Quaternion.getAxisAsQuat(new Vector(1, 0, 0), -90).multiply(Quaternion.getAxisAsQuat(0, 0, 1, -90)));

//            SoundSource monsterSource = new SoundSource("monster_"+i, true, false);
//            monsterSource.setBuffer(scene.soundManager.soundBufferMap.get("madara"));
//            monsterSource.setGain(10);
//            monsterSource.attachToModel(monster);
//            monsterSource.play();
//            scene.soundManager.addSoundSource(monsterSource);

        }
        var madara = new MD5Model("res/madara/madara.md5mesh");
        hints.isInstanced = false;
        hints.numInstances = 1;
        var madara_meshes = MD5Utils.generateMeshes(madara, new Vector(1f, 1f, 1f, 1f), hints);
        madara_meshes.get(0).meshIdentifier = "madara1";
        hints.isInstanced = false;
        madara_meshes.stream().forEach(m -> scene.renderPipeline.initializeMesh(m));

        var madara_model = scene.createModel(madara_meshes, "madara", Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
        madara_model.setScale(5f);
        madara_model.setPos(new Vector(10, 30, 30));
        madara_model.setOrientation(Quaternion.getAxisAsQuat(new Vector(1, 0,0), -90).multiply(Quaternion.getAxisAsQuat(0, 0, 1, -90)));

//        SoundSource madaraSource = new SoundSource("madara", true, false);
//        madaraSource.setBuffer(scene.soundManager.soundBufferMap.get("madara"));
//        madaraSource.setGain(10);
//        madaraSource.attachToModel(madara_model);
//        madaraSource.play();
//        scene.soundManager.addSoundSource(madaraSource);

        var partHints = new MeshBuilderHints();
        partHints.isInstanced = true;
        partHints.shouldGenerateTangentBiTangent = false;
        partHints.shouldTriangulate = true;
        partHints.numInstances = 1000;
        partHints.shouldReverseWindingOrder = true;
        Mesh partMesh = MeshBuilder.buildMesh("res/misc/particle.obj", partHints);
        scene.renderPipeline.initializeMesh(partMesh);
        Texture partTex = new Texture("res/misc/explosion2.png", 4,5);
        partMesh.materials.get(0).texture = partTex;

        Particle particle = new Particle(this, partMesh, new Vector(-1f, 0f, 0), new Vector(-5f, 0f, 0),
                5,0.1f, "baseParticle");
        particle.scale = new Vector(3, 1f);
        particle.pos = new Vector(8.5f, 39, 30);
        var particleGenerator = new FlowParticleGenerator(particle, 1000, 0.01f, "generator");
        particleGenerator.posRange = new Vector(0.1f, 0.1f, 0.2f);
        particleGenerator.velRange = new Vector(-0.2f, 1, 1f);
        particleGenerator.accelRange = new Vector(0,0.5f,0.2f);
        particleGenerator.animUpdateRange = 0.1f;
        scene.addParticleGenerator(particleGenerator, Arrays.asList(new String[]{DefaultRenderPipeline.particleShaderBlockID}));

        Logger.log("loading assimp model");
        try {
            MeshBuilderHints houseHints = new MeshBuilderHints();
            houseHints.isInstanced = false;
            houseHints.numInstances = 1;
            var meshes = scene.loadMeshesAssimp("res/wolf/Wolf_dae.dae", "res/wolf/textures",
                    houseHints);
            meshes.forEach(m -> scene.renderPipeline.initializeMesh(m));
            var model = new Model(this, meshes, "woflf_static");
            model.pos = new Vector(30, 30, 50);
            model.scale = new Vector(20,20,20);
            model.orientation = Quaternion.getAxisAsQuat(1,0,0,-90);
            model.meshes.forEach(m -> {
                m.shouldCull = false;
            });
            scene.addModel(model, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            MeshBuilderHints houseHints = new MeshBuilderHints();
            houseHints.isInstanced = false;
            houseHints.numInstances = 1;
            var anim = scene.createAnimatedModelAssimp("res/wolf/Wolf_dae.dae", "res/wolf/textures",
                    "wolf", houseHints, new String[]{DefaultRenderPipeline.sceneShaderBlockID});
            anim.meshes.forEach(m -> scene.renderPipeline.initializeMesh(m));
            anim.pos = new Vector(30, 30, 30);
            anim.scale = new Vector(20,20,20);
            anim.orientation = Quaternion.getAxisAsQuat(1,0,0,-90);
            anim.meshes.forEach(m -> {
                m.shouldCull = false;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void cleanUp() {
        masterComponent.cleanUp();
        renderingEngine.cleanUp();
        scene.cleanUp();
    }

    public void tickGUI() {

    }

    public void tick() {

        tickGUI();
        masterComponent.tick(null, masterComponent.input, timeDelta);
        scene.cameras.forEach(c -> c.tick(timeDelta));

        tickInput();

        if(glfwWindowShouldClose(display.getWindow())) {
            programRunning = false;
        }

        if(isGameRunning != prevGameState) {
            if (isGameRunning)
                masterComponent.input.disableCursor();
//                masterComponent.disableCursor();
            else
                masterComponent.input.enableCursor();
//                masterComponent.enableCursor();
            prevGameState = isGameRunning;
        }

        if(isGameRunning) {
            BehaviourTickInput params = new BehaviourTickInput(timeDelta, scene);
            scene.modelID_model_map.values().forEach(m -> m.tick(params));

            ParticleGeneratorTickInput param = new ParticleGeneratorTickInput(timeDelta);
            scene.particleGenerators.forEach(gen -> gen.tick(param));

            scene.soundManager.tick(playerCamera, timeDelta);

        }

        if(isGameRunning) {
            calculate3DCamMovement();
        }

        input.reset();

    }

    public void tickInput() {

        Vector velocity = new Vector(3,0);

        if(input.keyDown(input.W)) {
            float cameraSpeed = speed * speedMultiplier;
            Vector[] rotationMatrix = playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add((z.scalarMul(-cameraSpeed)));
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
                monster.cycleFrame(20f * (counter+1) * timeDelta);
                monster.generateCurrentSkeleton(monster.currentAnimation.currentFrame);
                counter++;
            }
            var wolf = (AnimatedModel) scene.modelID_model_map.get("wolf");
            wolf.cycleFrame(20f * timeDelta);
//            wolf.cycleFrame(1);
            wolf.generateCurrentSkeleton(wolf.currentAnimation.currentFrame);
        }

        if(input.keyDown(input.DOWN_ARROW)) {
            int counter = 0;
            while(true) {
                AnimatedModel monster = (AnimatedModel) scene.modelID_model_map.get("monster"+counter);
                if(monster == null) {
                    break;
                }
                monster.cycleFrame(-20f * (counter+1) * timeDelta);
                monster.generateCurrentSkeleton(monster.currentAnimation.currentFrame);
                counter++;
            }
            var wolf = (AnimatedModel) scene.modelID_model_map.get("wolf");
            wolf.cycleFrame(-20f * timeDelta);
//            wolf.cycleFrame(-1);
            wolf.generateCurrentSkeleton(wolf.currentAnimation.currentFrame);
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = speed * speedMultiplier;
            Vector[] rotationMatrix = playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add(z.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = speed * speedMultiplier;
            Vector[] rotationMatrix = playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = speed * speedMultiplier;
            Vector[] rotationMatrix = playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = speed * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = speed * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(input.ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(isGameRunning) {
            if(input.keyDownOnce(input.LEFT_CONTROL)) {
                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                else speedMultiplier = 1;
            }

            if(input.keyDownOnce(input.F)) {
                if(targetFPS == masterComponent.getRefreshRate()) {
                    targetFPS = 10000;
                }
                else {
                    targetFPS = masterComponent.getRefreshRate();
                }
                System.out.println("Changed target resolution"+targetFPS);
            }

            if(input.keyDownOnce(input.V)) {
                masterComponent.toggleWindowModes();
            }
        }

//        Vector newPos = scene.camera.getPos().add(posDelta);
//        scene.camera.setPos(newPos);
        playerCamera.velocity = velocity;

//        Terrain.TerrainMovementDataPack terrainCollisionData = terrain.isPositionValid(newPos);
//        if(terrainCollisionData.isValid) {
//            this.cam.setPos(terrainCollisionData.validPosition);
//        }

    }

    public void calculate3DCamMovement() {
        if (input.getDelta().getNorm() != 0 && isGameRunning) {

            float yawIncrease   = mouseXSensitivity * timeDelta * -input.getDelta().get(0);
            float pitchIncrease = mouseYSensitivity * timeDelta * -input.getDelta().get(1);

            Vector currentAngle = playerCamera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = playerCamera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            playerCamera.setOrientation(q);
        }

    }

    public void render() {
        renderingEngine.render(scene, masterComponent);
        glfwSwapBuffers(display.getWindow());
        glfwPollEvents();
        input.poll();
        scene.hasMatLibraryUpdated = false;
    }

    public RenderingEngineGL getRenderingEngine() {
        return renderingEngine;
    }

//    public DisplayLWJGL getDisplay() {
//        return display;
//    }

    public Camera getCamera() {
        return playerCamera;
    }

    public Input getInput() {
        return input;
    }
}
