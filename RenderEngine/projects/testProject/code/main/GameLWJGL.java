package main;

import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.*;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.SceneComponent;
import Kurama.Effects.Fog;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
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
import Kurama.shadow.ShadowMap;
import Kurama.utils.Logger;
import Kurama.utils.Utils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class GameLWJGL extends Game implements Runnable {

//    protected DisplayLWJGL display;
//    protected InputLWJGL input;
//    protected RenderingEngineGL renderingEngine;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    public boolean isGameRunning = true;  //temp. Replace with game state
    protected int lookAtIndex = 1;
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

        input = new InputLWJGL(this, (DisplayLWJGL) display);

        scene.renderPipeline = new DefaultRenderPipeline(this, null,"sceneRenderer");
        ((RenderingEngineGL)renderingEngine).sceneRenderPipeline = scene.renderPipeline;
        ((RenderingEngineGL)renderingEngine).guiRenderPipeline = new Gintoki(this, null,"Gintoki");
        renderingEngine.init(scene);

        playerCamera = new Camera(this,scene.rootSceneComp, null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
                Display.defaultWindowedWidth, Display.defaultWindowedHeight, "playerCam");
        playerCamera.shouldPerformFrustumCulling = true;
        scene.rootSceneComp.addChild(playerCamera);

//        glfwSetFramebufferSizeCallback(((DisplayLWJGL)display).getWindow(), (window, width, height) -> {
//            glViewport(0,0,width,height);
//            display.windowResolution = new Vector(new float[]{width, height});
//        });
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
                .setColor(new Vector(1,0,0,0.5f))
                .setContainerVisibility(false);

        var leftDivide =
                new Rectangle(this, rootGuiComponent, "gameScreen")
                        .setTexture(new Texture(playerCamera.renderBuffer.textureId))
                        .addOnResizeAction(new WidthHeightPercent(1f, 1f))
                        .addOnResizeAction(new PosXYTopLeftAttachPercent(0,0))
                        .addOnResizeAction(new ResizeCameraRenderResolution(playerCamera))
                        .setKeyInputFocused(true)
                        .addOnClickAction(new GrabKeyboardFocus());
        rootGuiComponent.addChild(leftDivide);

        fpsText =
                (Text)new Text(this, leftDivide, new FontTexture(new Font("Arial", Font.PLAIN, 20), FontTexture.defaultCharSet), "fps")
                        .addOnResizeAction(new PosXYTopLeftAttachPix(40, 20))
                        .addAutomation(new DisplayFPS(this, "FPS: "))
                        .setOverlayColor(new Vector(1,0,0,0.5f));
        leftDivide.addChild(fpsText);

        var guiSection =
                new Rectangle(this, leftDivide, "gui")
                        .setRadii(new Vector(0.8f,0.8f,0.8f,0.8f))
                        .setShouldTriggerOnClick(true)
                        .setColor(new Vector(0.5f, 0.4f, 0.9f, 0.3f))
                        .addOnResizeAction(new WidthHeightPercent(0.2f, 1f))
                        .addOnResizeAction(new PosXYBottomRightAttachPercent(0,0));
        leftDivide.addChild(guiSection);

        leftDivide
                .addOnKeyInputFocusedInitAction((Component current, Input input, float timeDelta) -> {
                    rootGuiComponent.input.disableCursor();
                    isGameRunning = true;
                    guiSection.shouldTickRenderGroup = false;
            })
                .addOnKeyInputFocusedAction(new SceneInputHandling(this))
                .addOnKeyInputFocusLossInitAction((Component current, Input input, float timeDelta) -> {
                    rootGuiComponent.input.enableCursor();
                    isGameRunning = false;
                    guiSection.shouldTickRenderGroup = true;
                });


//        var square1 =
//                new Rectangle(this, rightDivide, "shadowMap")
//                        .setColor(new Vector(0.9f, 0.5f, 0.4f, 0.5f))
//                        .addConstraint(new WidthHeightPercent(0.7f, 0.4f))
//                        .addConstraint(new PosXYTopLeftAttachPercent(0.1f, 0.1f))
//                        .addOnMouseOvertAction(new SetOverlayColor(new Vector(1,0,0,0.5f)))
//                        .addOnMouseLeftAction(new SetOverlayColor(new Vector(0,0,0,0f)))
//                        .addOnClickAction(new Log("Clicked button 1"));
//        rightDivide.addChild(square1);

        var sampleGUITitle =
                new Text(this, guiSection, new FontTexture(fpsText.fontTexture.font, FontTexture.defaultCharSet), "info text")
                .setText("Sample Menu")
//                        .setOverlayColor(new Vector(0.7f,0.7f,0.7f,1f))
                .addOnResizeAction(new WidthHeightPercent(0.75f, 0.1f))
                .addOnResizeAction(new PosYTopAttachPercent(0.06f));
        guiSection.addChild(sampleGUITitle);

        var enterName =
                (TextBox)new TextBox(this, guiSection, new FontTexture(new Font("Arial", Font.BOLD, 20), FontTexture.defaultCharSet), "enterName")
                        .setText("Enter name")
                        .setColor(new Vector(0.9f, 0.5f, 0.4f, 0.5f))
                        .addOnResizeAction(new WidthHeightPercent(0.75f, 0.1f))
                        .addOnResizeAction(new PosYTopAttachPercent(0.2f));
        guiSection.addChild(enterName);
        enterName.setRadii(new Vector(10,10,10,10));
//        enterName.text.setOverlayColor(new Vector(0.7f,0.7f,0.7f,1f));

        var saveMessage =
                new Text(this, guiSection, new FontTexture(new Font("Arial", Font.ITALIC, 12), FontTexture.defaultCharSet), "save message")
                        .setText("successfully Saved")
                        .addOnResizeAction(new WidthHeightPercent(0.75f, 0.1f))
                        .addOnResizeAction(new PosYTopAttachPercent(0.5f))
                        .setShouldTickRenderGroup(false);
        guiSection.addChild(saveMessage);

        var saveBox =
                (TextBox)new TextBox(this, guiSection, new FontTexture(new Font("Arial", Font.BOLD, 20), FontTexture.defaultCharSet), "save")
                        .setText("Save")
                        .setColor(new Vector(0.4f, 0.5f, 0.4f, 0.5f))
                        .addOnResizeAction(new WidthHeightPercent(0.75f, 0.1f))
                        .addOnResizeAction(new PosYTopAttachPercent(0.42f))
                        .addOnClickAction(new SetOverlayColor(new Vector(1,0.5f,0.9f,0.8f)))
                        .addOnMouseOvertAction(new SetOverlayColor(new Vector(1,0.5f,0.9f,0.4f)))
                        .addOnMouseLeftAction(new RemoveOverlayColor());
        saveBox
                        .addOnClickAction(new AddAnimationToComponent(saveBox,
                                new Animation(7,
                                        (comp, in, timeDelta) -> saveMessage.shouldTickRenderGroup = true,
                                        null,
                                        (comp, in, timeDelta) -> saveMessage.shouldTickRenderGroup = false)));

        guiSection.addChild(saveBox);
        saveBox.setRadii(new Vector(10,10,10,10));
//        saveBox.text.setOverlayColor(new Vector(0.7f,0.7f,0.7f,1f));


        var caret =
                new Rectangle(this, null, "caret")
                        .setColor(new Vector(1f, 1f, 1f, 0.7f))
                        .setShouldTickRenderGroup(false)
                        .setWidth(3)
                        .setHeight(20);

        var textBox =
                (TextBox)new TextBox(this, guiSection, new FontTexture(new Font("Arial", Font.ITALIC, 16),
                        FontTexture.defaultCharSet), caret,"textBox")
                        .setRadii(new Vector(1f,0.8f,0.8f,0.8f))
                        .setColor(new Vector(0.9f, 0.5f, 0.4f, 0.5f))
                        .addOnResizeAction(new WidthHeightPercent(0.75f, 0.1f))
                        .addOnResizeAction(new PosYTopAttachPercent(0.31f))

                        .addOnKeyInputFocusedInitAction(new AddAnimationToComponent(caret, new Animation(Float.POSITIVE_INFINITY, new Blink(0.4f))))
                        .addOnKeyInputFocusedInitAction((Component current, Input input, float timeDelta) -> caret.shouldTickRenderGroup = true)

                        .addOnKeyInputFocusLossInitAction((Component current, Input input, float timeDelta) -> caret.shouldTickRenderGroup = false)
                        .addOnKeyInputFocusLossInitAction(new RemoveAnimationsFromComponent(caret))

                        .addOnKeyInputFocusedAction(new InputHandling(0.1f) {
                            @Override
                            public void run(Component current, Input input, float timeDelta) {

                                Text t = ((TextBox)current).text;
                                timeFromLastDelete += timeDelta;
                                var s = new StringBuilder();
                                s.append(t.text);

                                for(int k: input.pressedChars) {
                                    if(input.keyDownOnce(Character.toUpperCase(k))) {
                                        s.append((char) k);
                                    }
                                }

                                if(input.keyDown(input.DELETE) || input.keyDown(input.BACKSPACE)) {
                                    if(s.length() > 0 && timeFromLastDelete > minDeleteTime) {
                                        timeFromLastDelete = 0;
                                        s.deleteCharAt(s.length() - 1);
                                    }
                                }

                                if(!t.text.equals(s)) {
                                    t.setText(s.toString());
                                }

                            }
                        });
        guiSection.addChild(textBox);
        textBox.setRadii(new Vector(10,10,10,10));
    }

    public void writeSceneToFile() {
//        SceneUtils.writeSceneToKE(scene, "projects", "testProject", null,
//                null, "projects/testProject/code/HUD",
//                "projects/testProject/code/ModelBehaviour", "Kurama Engine ver alpha-2.0");
    }

    public void initScene() {
        MeshBuilderHints hints = new MeshBuilderHints();

        Matrix directionalLightOrthoProjection = Matrix.buildOrthographicProjectionMatrix(1,-15,1,-1,-1,1);

        scene.ambientLight = new Vector(0.3f,0.3f,0.3f);
        var sunMesh = scene.loadMesh("res/glassball/glassball.obj", "sun_mesh", hints);
        DirectionalLight directionalLight = new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),10),1f,
                new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH, ShadowMap.DEFAULT_SHADOWMAP_HEIGHT),
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

        directionalLight.orientation = Quaternion.getQuaternionFromEuler(0,0,0);

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
        Material skyMat = new Material(new Texture("res/misc/skybox.png"),1, "SkyBox");
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

        Texture cubeTexture = new Texture("res/misc/grassblock.png");
        Material cubeMat = new Material(cubeTexture, "minecraftCubeMat");
        cubeMat.diffuseMap = cubeMat.texture;
        cubeMat.specularMap = cubeMat.texture;
        cubeMat.specularPower = 10;
        cubeMat.reflectance = 0.5f;
        cubeMesh.materials.set(0, cubeMat);

        SceneComponent minecraftWall = new SceneComponent(this, scene.rootSceneComp, "wall");
        minecraftWall.pos = new Vector(0,25,0);
        scene.rootSceneComp.addChild(minecraftWall);

        for(int i = 0;i < 30;i++) {
            for(int y = 0;y < 20;y++) {
                Vector pos = new Vector(0,0,0).add(new Vector(new float[]{i*boxScale*2,y*boxScale*2,0}));
                Model cube = new Model(this,cubeMesh , "cube"+Utils.getUniqueID());
                minecraftWall.addChild(cube);
                cube.pos = pos;
                cube.setScale(boxScale,boxScale,1);
                scene.addModel(cube, Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
            }
        }

//        minecraftWall.addAutomation((cur, in, timedelta) -> cur.pos = cur.pos.add(new Vector(timedelta * 5f, 0, 0)));

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
            minecraftWall.children.get(0).addChild(monster);
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
        Texture partTex = new Texture("res/misc/explosion2.png", 4,5);
        partMesh.materials.get(0).texture = partTex;

        Particle particle = new Particle(this, null, partMesh, new Vector(-1f, 0f, 0), new Vector(-5f, 0f, 0),
                5,0.1f, "baseParticle");
        particle.scale = new Vector(3, 1f);
        particle.shouldTickRenderGroup = false;

        var particleGenerator = new FlowParticleGenerator(this, madara_model, particle, 1000, 0.01f, "generator");
        particleGenerator.pos = new Vector(0,-1,9);
        particleGenerator.posRange = new Vector(0.2f, 0f, 0.2f);
        particleGenerator.velRange = new Vector(-0.2f, 1, 1f);
        particleGenerator.accelRange = new Vector(0,0.5f,0.2f);
        particleGenerator.animUpdateRange = 0.1f;

        particleGenerator.orientation = Quaternion.getQuaternionFromEuler(0, 0,90);
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
            model.pos = new Vector(30, 30, 50);
            model.scale = new Vector(20,20,20);
            model.orientation = Quaternion.getAxisAsQuat(1,0,0,-90);
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
            anim.pos = new Vector(30, 30, 30);
            anim.scale = new Vector(20,20,20);
            anim.orientation = Quaternion.getAxisAsQuat(1,0,0,-90);
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

        if(isGameRunning) {
            scene.rootSceneComp.tick(null, input, timeDelta, false);
            scene.soundManager.tick(playerCamera, timeDelta);
        }

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
