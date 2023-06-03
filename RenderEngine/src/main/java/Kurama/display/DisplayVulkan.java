package Kurama.display;

import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GLCapabilities;

import java.awt.*;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class DisplayVulkan extends Display {

    public long window;

    public DisplayVulkan(int defaultWindowedWidth, int defaultWindowedHeight, Game game) {
        super(defaultWindowedWidth,defaultWindowedHeight,game);
    }

    public DisplayVulkan(Game game) {
        super(game);
    }

    @Override
    public void init() {
        startGLFW();
        initWindow();

        glfwSetFramebufferSizeCallback(this.getWindow(), (window, width, height) -> {
            this.windowResolution = new Vector(new float[]{width, height});
            resizeEvents.forEach(a -> a.run());
        });
    }

    public void initWindow() {
        System.out.println("initing window");
        // Configure GLFW
        //glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(defaultWindowedWidth, defaultWindowedHeight, game.name, NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        if(glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window,GLFW_RAW_MOUSE_MOTION,GLFW_TRUE);
        }
    }

    public long getWindow() {
        return window;
    }

    public void disableCursor() {
        glfwSetInputMode(window,GLFW_CURSOR,GLFW_CURSOR_DISABLED);
    }

    public void enableCursor() {
        glfwSetInputMode(window,GLFW_CURSOR,GLFW_CURSOR_NORMAL);
    }

    protected void startGLFW() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        Logger.log("started GLFW");
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

    }

    protected void removeGLFW() {
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
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

    public void setFullScreen() {

        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(),0,0,vid.width(),vid.height(),vid.refreshRate());
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwShowWindow(window);
//        WIDTH = vid.width();
//        HEIGHT = vid.height();

        windowResolution = new Vector(new float[]{vid.width(), vid.height()});
//        glViewport(0,0,vid.width(), vid.height());

//        renderResolution = new Vector(new float[]{vid.width(), vid.height()});
    }

    public void setWindowedMode() {
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowMonitor(window, NULL,0,0,defaultWindowedWidth,defaultWindowedHeight,vidmode.refreshRate());

        glfwSetWindowPos(
                window,
                (vidmode.width() - defaultWindowedWidth) / 2,
                (vidmode.height() - defaultWindowedHeight) / 2
        );
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwShowWindow(window);
    }

    public void setWindowedMode(int width, int height) {
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowMonitor(window, NULL,0,0,width,height,vidmode.refreshRate());

        glfwSetWindowPos(
                window,
                (vidmode.width() - width) / 2,
                (vidmode.height() - height) / 2
        );

        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwShowWindow(window);

//        windowResolution = new Vector(new float[]{width, height});
//        glViewport(0,0,width,height);
//        renderResolution = new Vector(new float[]{width, height});
    }

    protected void removeWindow() {
        if(window != NULL) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }

        windowResolution = new Vector(new float[]{defaultWindowedWidth, defaultWindowedHeight});
//        renderResolution = new Vector(new float[]{defaultWindowedWidth, defaultWindowedHeight});

    }

    public void toggleWindowModes() {
        if (displayMode == DisplayMode.FULLSCREEN) {
            setWindowedMode();
            displayMode = DisplayMode.WINDOWED;
        }
        else {
            setFullScreen();
            displayMode = DisplayMode.FULLSCREEN;
        }
    }

    public void cleanUp() {
        removeWindow();
        removeGLFW();
    }

    public int getRefreshRate() {
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        return vid.refreshRate();
    }

    public void setClearColor(float r, float g, float b, float a) {
        glClearColor(r,g,b,a);
    }

    @Override
    public void startScreen() {

    }
}
