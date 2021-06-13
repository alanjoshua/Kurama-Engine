package editor;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.MasterWindow;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintComponent;
import Kurama.ComponentSystem.components.constraintGUI.GridCell;
import Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem.RigidBodyConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchSystemConfigurator;
import Kurama.Math.Vector;
import Kurama.display.Display;
import Kurama.display.DisplayLWJGL;
import Kurama.game.Game;
import Kurama.inputs.InputLWJGL;
import Kurama.renderingEngine.RenderingEngineGL;
import Kurama.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import Kurama.renderingEngine.ginchan.Gintoki;
import Kurama.scene.Scene;

import static org.lwjgl.glfw.GLFW.*;

public class Editor extends Game {

    ConstraintComponent hierarchyWindow;
    Component sceneWindow;

    public Editor(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {

        scene = new Scene(this);
        renderingEngine = new RenderingEngineGL(this);

        display = new DisplayLWJGL(this);
        display.displayMode = Display.DisplayMode.WINDOWED;
        display.startScreen();
        display.setWindowedMode(1920, 1080);

        input = new InputLWJGL(this, (DisplayLWJGL) display);

        scene.renderPipeline = new DefaultRenderPipeline(this, null,"sceneRenderer");
        ((RenderingEngineGL)renderingEngine).sceneRenderPipeline = scene.renderPipeline;
        ((RenderingEngineGL)renderingEngine).guiRenderPipeline = new Gintoki(this, null,"Gintoki");
        renderingEngine.init(scene);

        initGUI();
    }

    public void initGUI() {

        rootGuiComponent = new MasterWindow(this, display, input,"masterWindow");
        rootGuiComponent
                .setContainerVisibility(false);


//        BoundaryConfigurator customRigidConfig2 = (boundary) -> {
//            new RigidBodyConfigurator().configure(boundary);
//            boundary.interactionConstraints.add(0, new MaxXPos(0.95f));
//            return boundary;
//        };

        BoundaryConfigurator rigidBodyConfig = new RigidBodyConfigurator();
        BoundaryConfigurator stretchConfig = new StretchSystemConfigurator();

        hierarchyWindow =
                (ConstraintComponent) new ConstraintComponent(this, rootGuiComponent, "hierarchyWindow", stretchConfig)
                .addOnResizeAction(new WidthHeightPercent(1f, 1f))
                .setColor(new Vector(0,1,1,0.5f));
        rootGuiComponent.addChild(hierarchyWindow);

        var rr = hierarchyWindow.createBoundary("rr", Boundary.BoundaryOrient.Vertical, true);
        var bb = hierarchyWindow.createBoundary("bb", Boundary.BoundaryOrient.Horizontal, true);
        var tt = hierarchyWindow.createBoundary("tt", Boundary.BoundaryOrient.Horizontal, true);

        bb.setColor(new Vector(0,1,1,1));

        rr.addConnectedBoundary(tt, 0, 0);
        rr.addConnectedBoundary(bb, 0, 1);

        var right =  hierarchyWindow.getBoundary(hierarchyWindow.identifier+"_right");
       right.addConnectedBoundary(tt, 1, 0).addConnectedBoundary(bb, 1, 1);

        rr.addInitAutomation(new HeightPercent(0.5f)).addInitAutomation(new PosXRightAttachPercent(0.1f));
        tt.addInitAutomation(new WidthPercent(0.15f)).addInitAutomation(new PosXYTopLeftAttachPercent(0.75f, 0.25f));
        bb.addInitAutomation(new WidthPercent(0.15f)).addInitAutomation(new PosXYTopLeftAttachPercent(0.75f, 0.75f));

        GridCell t1 = new GridCell(this, hierarchyWindow, "t1");
        t1.top = hierarchyWindow.getBoundary(hierarchyWindow.identifier+"_top");
        t1.bottom = hierarchyWindow.getBoundary(hierarchyWindow.identifier+"_bottom");
        t1.left = hierarchyWindow.getBoundary(hierarchyWindow.identifier+"_left");
        t1.right = hierarchyWindow.getBoundary(hierarchyWindow.identifier+"_right");
        t1.attachedComp = new Rectangle(this, hierarchyWindow, "ge1")
                            .setColor(new Vector(1,0,0,1));

        hierarchyWindow.addGridCell(t1);

        GridCell t2 = new GridCell(this, hierarchyWindow, "t2");
        t2.top = hierarchyWindow.getBoundary("tt");
        t2.bottom = hierarchyWindow.getBoundary("bb");
        t2.left = hierarchyWindow.getBoundary(hierarchyWindow.identifier+"_right");
        t2.right = hierarchyWindow.getBoundary("rr");
        t2.attachedComp = new Rectangle(this, hierarchyWindow, "ge2")
                .setColor(new Vector(0,1,0,1));

        hierarchyWindow.addGridCell(t2);
    }

    @Override
    public void cleanUp() {
        rootGuiComponent.cleanUp();
        renderingEngine.cleanUp();
        scene.cleanUp();
    }

    @Override
    public void tick() {
        rootGuiComponent.tick(null, rootGuiComponent.input, timeDelta, false);

        if(glfwWindowShouldClose(((DisplayLWJGL)display).getWindow())) {
            programRunning = false;
        }

        scene.rootSceneComp.children.forEach(m -> m.tick(null, input, timeDelta, false));
//        Logger.log("FPS: "+displayFPS);
        input.reset();
    }

    @Override
    public void render() {
        ((RenderingEngineGL)renderingEngine).render(scene, rootGuiComponent);
        glfwSwapBuffers(((DisplayLWJGL)display).getWindow());
        glfwPollEvents();
        input.poll();
        scene.hasMatLibraryUpdated = false;
    }

}
