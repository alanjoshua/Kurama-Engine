package main;

import java.awt.*;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import rendering.CameraLWJGL;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class DisplayLWJGL {

    private long window;
    private GameLWJGL game;
    private int defaultWindowedWidth = 1280;
    private int defaultWindowedHeight = 720;

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static final double winDPI = 96;
    public static final double macDPI = 72;
    public int maxFPS = 120;

    public enum DisplayMode {
        FULLSCREEN, WINDOWED
    }

    public DisplayMode displayMode = DisplayMode.WINDOWED;

    public DisplayLWJGL(int defaultWindowedWidth, int defaultWindowedHeight, GameLWJGL game) {
        this.game = game;
        this.defaultWindowedWidth = defaultWindowedWidth;
        this.defaultWindowedHeight = defaultWindowedHeight;
        startGLFW();
    }

    public DisplayLWJGL(GameLWJGL game) {
        this.game = game;
        startGLFW();
    }

    public void startScreen() {
        initWindow();
        if (displayMode == DisplayMode.FULLSCREEN) setFullScreen();
        else setWindowedMode();
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

    public int getWidth() {
        GLFWVidMode vid = glfwGetVideoMode(window);
        return vid.width();
    }

    public int getHeight() {
        GLFWVidMode vid = glfwGetVideoMode(window);
        return vid.height();
    }

    public void initWindow() {

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation

        // Create the window
        window = glfwCreateWindow(defaultWindowedWidth, defaultWindowedHeight, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
//        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
//            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE ) {
//                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
//            }
//
//            if(key == GLFW_KEY_T && action == GLFW_RELEASE) {
//                toggleWindowModes();
//            }
//        });

        if(glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window,GLFW_RAW_MOUSE_MOTION,GLFW_TRUE);
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        // Enable v-sync
        glfwSwapInterval(1);

//        glfwSetFramebufferSizeCallback(window, (window,w,h) -> {
//            glViewport(0,0,w,h);
//        });

        glfwSetWindowSizeCallback(window,(window,w,h) -> {
            if(game !=null) {
                CameraLWJGL cam = game.getCamera();
                if(cam != null) {
                    game.getCamera().setImageWidth(w);
                    game.getCamera().setImageHeight(h);
                    game.getCamera().setShouldUpdateValues(true);
                }
                if(game.renderingEngine != null) {
                    game.renderingEngine.resetBuffers();
                }
            }
        });

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

    private void setFullScreen() {
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(),0,0,vid.width(),vid.height(),vid.refreshRate());
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwShowWindow(window);
    }

    private void setWindowedMode() {
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
    }

    public void removeWindow() {
        if(window != NULL) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
    }

    public void startGLFW() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

    }

    public void removeGLFW() {
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
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

}
