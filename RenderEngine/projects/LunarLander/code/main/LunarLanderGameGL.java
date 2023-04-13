package main;

import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Editor.HierarchyWindow;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.*;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem.RigidBodyConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchSystemConfigurator;
import Kurama.ComponentSystem.components.model.AnimatedModel;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.SceneComponent;
import Kurama.Effects.Fog;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.OpenGL.TextureGL;
import Kurama.Terrain.Terrain;
import Kurama.audio.SoundBuffer;
import Kurama.audio.SoundManager;
import Kurama.audio.SoundSource;
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
import Kurama.particle.FlowParticleGenerator;
import Kurama.particle.Particle;
import Kurama.renderingEngine.RenderingEngineGL;
import Kurama.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import Kurama.renderingEngine.ginchan.Gintoki;
import Kurama.scene.Scene;
import Kurama.shadow.ShadowMapGL;
import Kurama.utils.Logger;
import Kurama.utils.Utils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class LunarLanderGameGL extends Game implements Runnable {

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    public boolean isGameRunning = true;  //temp. Replace with game state

    public Camera playerCamera;
    public Text fpsText;

    public LunarLanderGameGL(String threadName) {
        super(threadName);
        GRAPHICS_API = GraphicsApi.OPENGL;
    }

    public void init() {
        scene = new Scene(this);

        renderingEngine = new RenderingEngineGL(this);

        display = new DisplayLWJGL(this);
        display.displayMode = Display.DisplayMode.WINDOWED;
        display.startScreen();

        input = new InputLWJGL(this, (DisplayLWJGL) display);

        scene.renderPipeline = new DefaultRenderPipeline(this, null,"sceneRenderer");
        ((RenderingEngineGL)renderingEngine).sceneRenderPipeline = scene.renderPipeline;
        ((RenderingEngineGL)renderingEngine).guiRenderPipeline = new Gintoki(this, null,"Gintoki");
        renderingEngine.init(scene);

        playerCamera = new Camera(this,scene.rootSceneComp, null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
                Display.defaultWindowedWidth, Display.defaultWindowedHeight, true,"playerCam");
        playerCamera.shouldPerformFrustumCulling = true;
        scene.rootSceneComp.addChild(playerCamera);

        scene.cameras.add(playerCamera);

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

        initGUI();
        initScene();

        ((DisplayLWJGL)display).setClearColor(0,0,0,1);
        scene.cameras.forEach(Camera::updateValues);
        targetFPS = display.getRefreshRate();

        Logger.log("");
        for(var mat: scene.materialLibrary) {
            Logger.log(mat.matName);
        }
        Logger.log("");

    }

    public void initGUI() {

        rootGuiComponent = new MasterWindow(this, display, input,"masterWindow");
        rootGuiComponent
                .setConfigurator(new RigidBodyConfigurator())
                .setColor(new Vector(1,0,0,0.5f))
                .setContainerVisibility(false)
                .addOnResizeAction((comp, in, time) -> Logger.log("resizing to size: "+ comp.getWidth() +":"+ comp.getHeight()));

        var gameScreen =
                new Rectangle(this, rootGuiComponent, "gameScreen")
                        .attachSelfToParent(rootGuiComponent)
                        .setTexture(Texture.createTexture(playerCamera.renderBuffer.textureId))
                        .addOnResizeAction(new ResizeCameraRenderResolution(playerCamera))
                        .setKeyInputFocused(true)
                        .addOnClickAction(new GrabKeyboardFocus())
                        .addOnClickAction((cur, in, td) -> System.out.println("Clicking"))
                        .addOnKeyInputFocusedInitAction((Component current, Input input, float timeDelta) -> {
                            rootGuiComponent.input.disableCursor();
                            isGameRunning = true;

                        })
                        .addOnKeyInputFocusedAction((Component current, Input input, float timeDelta) -> {
                            Vector velocity = new Vector(3,0);

                            if(input.keyDown(input.W)) {
                                float cameraSpeed = speed * speedMultiplier;
                                Vector[] rotationMatrix = playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

                                Vector x = rotationMatrix[0];
                                Vector y = new Vector(new float[] {0,1,0});
                                Vector z = x.cross(y);
                                velocity = velocity.add((z.scalarMul(-cameraSpeed)));
                            }

                            if(input.keyDownOnce(input.ESCAPE)) {
                                current.isKeyInputFocused = false;
                            }

                            if (input.keyDownOnce(input.F)) {
                                if (targetFPS == rootGuiComponent.getRefreshRate()) {
                                    targetFPS = 10000;
                                } else {
                                    targetFPS = rootGuiComponent.getRefreshRate();
                                }
                                Logger.log("Changed target resolution" + targetFPS);
                            }

                            if (input.keyDownOnce(input.V)) {
                                rootGuiComponent.toggleWindowModes();
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

                            if(input.keyDownOnce(input.LEFT_CONTROL)) {
                                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                                else speedMultiplier = 1;
                            }

                            playerCamera.velocity = velocity;

                            calculate3DCamMovement();

                        })
                        .addOnKeyInputFocusLossInitAction((Component current, Input input, float timeDelta) -> {
                            rootGuiComponent.input.enableCursor();
                            isGameRunning = false;
                        });
        rootGuiComponent.addOnClickAction((cur, in, td) -> {
            rootGuiComponent.input.disableCursor();
            gameScreen.isClicked = true;
            isGameRunning = true;
            gameScreen.isKeyInputFocused = true;
        });

        fpsText =
                (Text)new Text(this, gameScreen, new FontTexture(new Font("Arial", Font.PLAIN, 20), FontTexture.defaultCharSet), "fps")
                        .attachSelfToParent(gameScreen)
                        .addOnResizeAction(new PosXYTopLeftAttachPix(40, 20))
                        .addAutomation(new DisplayFPS(this, "FPS: "))
                        .setOverlayColor(new Vector(1,0,0,0.5f));

    }

    private void calculate3DCamMovement() {
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
            System.out.println("Moving mouse");
        }
    }

    public void initScene() {
        MeshBuilderHints hints = new MeshBuilderHints();

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-15,1,-1,-1,1);

        scene.ambientLight = new Vector(0.3f,0.3f,0.3f);
        var sunMesh = scene.loadMesh("res/glassball/glassball.obj", "sun_mesh", hints);
        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),1f,
                new ShadowMapGL(ShadowMapGL.DEFAULT_SHADOWMAP_WIDTH, ShadowMapGL.DEFAULT_SHADOWMAP_HEIGHT),
                sunMesh, directionalLightOrthoProjection, "Sun");

//        rootGuiComponent.findComponent("shadowMap").setTexture(directionalLight.shadowMap.depthMap);
        scene.renderPipeline.initializeMesh(sunMesh);
//        sunMesh.initOpenGLMeshData();
        directionalLight.setPos(new Vector(0,500,0));
        directionalLight.shouldSelfCastShadow = false;
        directionalLight.doesProduceShadow = true;
        directionalLight.setScale(100);
        scene.addDirectionalLight(directionalLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
        scene.rootSceneComp.addChild(directionalLight);
//        directionalLight.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);

//        directionalLight.pos = new Vector(0, 60, 100);
//        directionalLight.orientation = new Quaternion(0, 1, 0, 0);

        directionalLight.setOrientation(Quaternion.getQuaternionFromEuler(0,0,0));

        directionalLight.addAutomation((current, input, timeDelta) -> {
            Scene scene = current.game.scene;

            float lightPosScale = 500;

            DirectionalLight light = (DirectionalLight) current;
            light.setPos(light.getOrientation().getRotationMatrix().getColumn(2).scalarMul(-lightPosScale));

            float delta = (5f * timeDelta);
            float currentPitch = light.getOrientation().getPitchYawRoll().get(0);

            float lightAngle = currentPitch + delta;

            if (lightAngle > 180 || lightAngle < 0) {
                light.intensity = 0;

            } else if (lightAngle <= 10 || lightAngle >= 170) {
                float factor = (lightAngle > 10?180-lightAngle:lightAngle)/20f;
                light.intensity = factor;

            } else {
                light.intensity = 1;
                light.color = new Vector(3, 1);
            }

            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}), delta);
            light.setOrientation(rot.multiply(light.getOrientation()));

//            Logger.log("pos: "+light.pos.toString());
//            Logger.log("orient: "+light.orientation.toString());
//            Logger.log();

            scene.skybox.meshes.get(0).materials.get(0).ambientColor = new Vector(4, light.intensity);
        });

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        scene.fog = new Fog(true, new Vector(new float[]{0.5f, 0.5f, 0.5f}), 0.005f);
        scene.fog = Fog.NOFOG;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//        Vector lightPos = new Vector(new float[]{0,0,10});
//        PointLight sl_pointLight = new PointLight(this, scene.rootSceneComp, "pointlight", new Vector(new float[]{1, 1, 1}), lightPos, 1f);
//        sl_pointLight.attenuation = new PointLight.Attenuation(0f,0f, 0f);
//        Quaternion coneOrientation = Quaternion.getQuaternionFromEuler(0,0,0);
//        SpotLight spotLight = new SpotLight(this,sl_pointLight, coneOrientation, 45,
//                null, (Mesh) null, null,"spotlight 1");
//
////        spotLight.generateShadowProjectionMatrix(0.1f , 100, 1, 1);
//        spotLight.shadowProjectionMatrix = Matrix.getIdentityMatrix(4);
//        spotLight.doesProduceShadow = false;
//
////        spotLight.addMesh(scene.loadMesh("res/torch/test/hand_light.obj", "torchlight_mesh", hints));
////        spotLight.meshes.add(scene.loadMesh("res/apricot/Apricot_02_hi_poly.obj", "apricot", hints));
//        spotLight.setScale(1f);
//        spotLight.setPos(new Vector(new float[]{0,0f,5f}));
//        spotLight.shouldRender = true;
//
////        spotLight.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{1, 0, 0}), -30).
////                multiply(Quaternion.getAxisAsQuat(new Vector(0, 0, 1), 90)));
//
//        spotLight.shouldSelfCastShadow = false;
//        scene.addSplotLight(spotLight, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
//
//        playerCamera.addChild(spotLight);
//        spotLight.shouldBeConsideredForFrustumCulling = false;
//        spotLight.parent = playerCamera;

//     -------------------------------------------------------------------------------------------------------------------

        float skyBoxScale = 1000;
        float boxScale = 1f;
        int boxCount = 100;
        float yRange = 60;

        var skybox_mesh = scene.loadMesh("res/misc/skybox.obj", "skybox_mesh", hints);
        scene.renderPipeline.initializeMesh(skybox_mesh);
        Material skyMat = new Material(Texture.createTexture("res/misc/skybox.png"),1, "SkyBox");
        skyMat.ambientColor = new Vector(new float[]{1f,1f,1f,1f});
        skybox_mesh.materials.set(0, skyMat);

        Model skybox = scene.createModel(skybox_mesh, "skybox", Arrays.asList(new String[]{DefaultRenderPipeline.skyboxShaderBlockID}));
        skybox.setScale(skyBoxScale);
        scene.skybox = skybox;
        scene.skybox.parent = scene.rootSceneComp;
        scene.rootSceneComp.addChild(scene.skybox);

        long seed = Utils.generateSeed("UchihaConan");
        System.out.println("seed: "+seed);
        float[][] heightMap = Kurama.geometry.Utils.generateRandomHeightMap(boxCount,boxCount,5,0.5f, 0.01f,seed);
        hints.isInstanced = true;
        hints.numInstances = 600;
        Mesh cubeMesh = scene.loadMesh("res/misc/cube.obj", "cube_mesh", hints);
        scene.renderPipeline.initializeMesh(cubeMesh);
        hints.isInstanced = false;

        Texture cubeTexture = Texture.createTexture("res/misc/grassblock.png");
        Material cubeMat = new Material(cubeTexture, "cubeMat");
        cubeMat.diffuseMap = cubeMat.texture;
        cubeMat.specularMap = cubeMat.texture;
        cubeMat.specularPower = 10;
        cubeMat.reflectance = 0.5f;
        cubeMesh.materials.set(0, cubeMat);

        SceneComponent minecraftWall = new SceneComponent(this, scene.rootSceneComp, "wall");
        minecraftWall.setPos(new Vector(0,25,0));
        scene.rootSceneComp.addChild(minecraftWall);

        for(int i = 0;i < 30;i++) {
            for(int y = 0;y < 20;y++) {
                Vector pos = new Vector(0,0,0).add(new Vector(new float[]{i*boxScale*2,y*boxScale*2,0}));
                Model cube = new Model(this,cubeMesh , "cube"+Utils.getUniqueID());
                minecraftWall.addChild(cube);
                cube.setPos(pos);
//                cube.setOrientation(Quaternion.getAxisAsQuat(1,0,0,90));
                cube.setScale(boxScale,boxScale,1);
                scene.addModel(cube, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            }
        }

//        minecraftWall.addAutomation((cur, in, timedelta) -> cur.pos = cur.pos.add(new Vector(timedelta * 5f, 0, 0)));

        Mesh terrain_mesh = TerrainUtils.createTerrainFromHeightMap(heightMap,boxCount/1,"Terrain_mesh");
        Material ter_mat = new Material();
        ter_mat.matName = "TERRAIN";
        ter_mat.texture = Texture.createTexture("res/misc/crystalTexture.jpg");
        ter_mat.diffuseMap = ter_mat.texture;
        ter_mat.normalMap = Texture.createTexture("res/misc/crystalNormalMap.jpg");
        ter_mat.specularMap = Texture.createTexture("res/misc/crystalSpecularMap.jpg");
        ter_mat.reflectance = 1f;
        terrain_mesh.materials.set(0,ter_mat);
        scene.renderPipeline.initializeMesh(terrain_mesh);
        var terrain = new Terrain(this, terrain_mesh, "Terrain", heightMap.length, heightMap[0].length, 2);
        terrain.setScale(boxCount,yRange,boxCount);
        scene.rootSceneComp.addChild(terrain);
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
            monster.setPos(new Vector(10 + (i*10), 5, 10));
            monster.setOrientation(Quaternion.getAxisAsQuat(new Vector(1, 0, 0), -90).multiply(Quaternion.getAxisAsQuat(0, 0, 1, -90)));
            minecraftWall.getChild(0).addChild(monster);
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
        scene.rootSceneComp.addChild(madara_model);

        SoundSource madaraSource = new SoundSource("madara", true, false);
        madaraSource.setBuffer(scene.soundManager.soundBufferMap.get("madara"));
        madaraSource.setGain(10);
        madaraSource.attachToModel(madara_model);
        madaraSource.play();
        scene.soundManager.addSoundSource(madaraSource);

        var partHints = new MeshBuilderHints();
        partHints.isInstanced = true;
        partHints.shouldGenerateTangentBiTangent = false;
        partHints.shouldTriangulate = true;
        partHints.numInstances = 1000;
        partHints.shouldReverseWindingOrder = true;
        Mesh partMesh = MeshBuilder.buildMesh("res/misc/particle.obj", partHints);
        scene.renderPipeline.initializeMesh(partMesh);
        Texture partTex = Texture.createTexture("res/misc/explosion2.png", 4,5);
        partMesh.materials.get(0).texture = partTex;

        Particle particle = new Particle(this, null, partMesh, new Vector(-1f, 0f, 0), new Vector(-5f, 0f, 0),
                5,0.1f, "baseParticle");
        particle.setScale(new Vector(3, 1f));
        particle.shouldTickRenderGroup = false;

        var particleGenerator = new FlowParticleGenerator(this, madara_model, particle, 1000, 0.01f, "generator");
        particleGenerator.setPos(new Vector(0,-1,9));
        particleGenerator.posRange = new Vector(0.2f, 0f, 0.2f);
        particleGenerator.velRange = new Vector(-0.2f, 1, 1f);
        particleGenerator.accelRange = new Vector(0,0.5f,0.2f);
        particleGenerator.animUpdateRange = 0.1f;

        particleGenerator.setOrientation(Quaternion.getQuaternionFromEuler(0, 0,90));
        particleGenerator.shouldRespectParentScaling = false;
        particle.parent = particleGenerator;

        madara_model.addChild(particleGenerator);

        scene.addParticleGenerator(particleGenerator, Arrays.asList(new String[]{DefaultRenderPipeline.particleShaderBlockID}));

        Logger.log("loading assimp model");
        try {
            MeshBuilderHints houseHints = new MeshBuilderHints();
            houseHints.isInstanced = false;
            houseHints.numInstances = 1;
            var meshes = scene.loadMeshesAssimp("res/wolf/Wolf_dae.dae", "res/wolf/textures",
                    houseHints);
            meshes.forEach(m -> scene.renderPipeline.initializeMesh(m));
            var model = new Model(this, meshes, "wolf_static");
            model.setPos(new Vector(30, 30, 50));
            model.setScale(new Vector(20,20,20));
            model.setOrientation(Quaternion.getAxisAsQuat(1,0,0,-90));
            scene.rootSceneComp.addChild(model);
            model.meshes.forEach(m -> m.shouldCull = false);
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
            anim.setPos(new Vector(30, 30, 30));
            anim.setScale(new Vector(20,20,20));
            anim.setOrientation(Quaternion.getAxisAsQuat(1,0,0,-90));
            anim.meshes.forEach(m -> {
                m.shouldCull = false;
            });
            scene.rootSceneComp.addChild(anim);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void cleanUp() {
        rootGuiComponent.cleanUp();
        renderingEngine.cleanUp();
        scene.cleanUp();
    }

    public void tick() {

        rootGuiComponent.tick(null, rootGuiComponent.input, timeDelta, false);

        if(glfwWindowShouldClose(((DisplayLWJGL)display).getWindow())) {
            programRunning = false;
        }

//        if(isGameRunning) {
            scene.rootSceneComp.tick(null, input, timeDelta, false);
            scene.soundManager.tick(playerCamera, timeDelta);
//        }

        input.reset();

    }

    public void render() {
        ((RenderingEngineGL)renderingEngine).render(scene, rootGuiComponent);
        glfwSwapBuffers(((DisplayLWJGL)display).getWindow());
        glfwPollEvents();
        input.poll();
        scene.hasMatLibraryUpdated = false;
    }

}
