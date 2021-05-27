package editor;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.MasterWindow;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintComponent;
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


//        BoundaryConfigurator customRigidConfig = (boundary) -> {
//            new RigidBodyConfigurator().configure(boundary);
//            boundary.interactionConstraints.add(0, new MaxXPos(0.95f));
//            return boundary;
//        };

        BoundaryConfigurator customRigidConfig = new StretchSystemConfigurator();

        hierarchyWindow =
                (ConstraintComponent) new ConstraintComponent(this, rootGuiComponent, "hierarchyWindow", customRigidConfig)
                .addConstraint(new WidthHeightPercent(1f, 1f))
                .setColor(new Vector(0,1,1,0.5f));
        rootGuiComponent.addChild(hierarchyWindow);

        var rr = hierarchyWindow.createBoundary(this, hierarchyWindow, "rr", Boundary.BoundaryOrient.Vertical);
        var bb = hierarchyWindow.createBoundary(this, hierarchyWindow, "bb", Boundary.BoundaryOrient.Horizontal);
        var tt = hierarchyWindow.createBoundary(this, hierarchyWindow, "tt", Boundary.BoundaryOrient.Horizontal);

        rr.addConnectedBoundary(tt, 0, 0);
        rr.addConnectedBoundary(bb, 0, 1);

        var right =  ((Boundary)hierarchyWindow.findComponent("rightB"));
       right.addConnectedBoundary(tt, 1, 0).addConnectedBoundary(bb, 1, 1);

        rr.addInitAutomation(new HeightPercent(0.5f)).addInitAutomation(new PosXRightAttachPercent(0.1f));
        tt.addInitAutomation(new WidthPercent(0.15f)).addInitAutomation(new PosXYTopLeftAttachPercent(0.75f, 0.25f));
        bb.addInitAutomation(new WidthPercent(0.15f)).addInitAutomation(new PosXYTopLeftAttachPercent(0.75f, 0.75f));
    }

    @Override
    public void cleanUp() {
        rootGuiComponent.cleanUp();
        renderingEngine.cleanUp();
        scene.cleanUp();
    }

    @Override
    public void tick() {
        rootGuiComponent.tick(null, rootGuiComponent.input, timeDelta);

        if(glfwWindowShouldClose(((DisplayLWJGL)display).getWindow())) {
            programRunning = false;
        }

        scene.rootSceneComp.children.forEach(m -> m.tick(null, input, timeDelta));

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
