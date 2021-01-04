package Kurama.display;

import Kurama.Math.Vector;
import Kurama.inputs.Input;
import Kurama.game.Game;

import java.awt.*;

public abstract class Display {

    protected Game game;
    protected Input input;
    public static int defaultWindowedWidth = 1280;
    public static int defaultWindowedHeight = 720;
    public int resolutionScale = 1;

    public Vector windowResolution = new Vector(new float[]{defaultWindowedWidth, defaultWindowedHeight});
//    public Vector renderResolution = new Vector(new float[]{defaultWindowedWidth, defaultWindowedHeight});

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static final double winDPI = 96;
    public static final double macDPI = 72;

    public enum DisplayMode {
        FULLSCREEN, WINDOWED
    }

    public DisplayMode displayMode = DisplayMode.FULLSCREEN;

    public abstract void init();
    public abstract void startScreen();
    public abstract void toggleWindowModes();
//    public abstract int getWindowWidth();
//    public abstract int getWindowHeight();
    public abstract void setFullScreen();
    public abstract void setWindowedMode();
    public abstract void setWindowedMode(int width, int height);
    protected abstract void removeWindow();
    public abstract void disableCursor();
    public abstract void enableCursor();
    public abstract void cleanUp();

    public Display(Game game) {
        this.game = game;
        init();
    }

    public Display(int defaultWindowedWidth, int defaultWindowedHeight, Game game) {
        this.game = game;
        this.defaultWindowedWidth = defaultWindowedWidth;
        this.defaultWindowedHeight = defaultWindowedHeight;
        init();
    }

    public int getDPI() {
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }

    public float getScalingRelativeToDPI() {
        if(OS.indexOf("mac") > 0) {
            int dpi = getDPI();
            return (float)(dpi/macDPI);
        }

        else {
            int dpi = getDPI();
            return  (float)(dpi/winDPI);
        }
    }

}
