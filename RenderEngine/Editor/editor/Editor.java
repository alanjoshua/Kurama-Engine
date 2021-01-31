package editor;

import Kurama.display.Display;
import Kurama.display.DisplayLWJGL;
import Kurama.game.Game;
import Kurama.renderingEngine.RenderingEngineGL;
import Kurama.scene.Scene;

public class Editor extends Game {

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

        initGUI();
    }

    public void initGUI() {

    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {

    }

}
