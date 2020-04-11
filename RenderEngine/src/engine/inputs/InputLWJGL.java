package engine.inputs;

import engine.display.DisplayLWJGL;
import engine.game.Game;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;

import java.util.ArrayList;

public class InputLWJGL extends Input {

    private long window;

//    public static final int Q = GLFW_KEY_Q;
//    public static final int W = GLFW_KEY_W;
//    public static final int E = GLFW_KEY_E;
//    public static final int R = GLFW_KEY_R;
//    public static final int T = GLFW_KEY_T;
//    public static final int Y = GLFW_KEY_Y;
//    public static final int U = GLFW_KEY_U;
//    public static final int I = GLFW_KEY_I;
//    public static final int O = GLFW_KEY_O;
//    public static final int P = GLFW_KEY_P;
//    public static final int A = GLFW_KEY_A;
//    public static final int S = GLFW_KEY_S;
//    public static final int D = GLFW_KEY_D;
//    public static final int F = GLFW_KEY_F;
//    public static final int G = GLFW_KEY_G;
//    public static final int H = GLFW_KEY_H;
//    public static final int J = GLFW_KEY_J;
//    public static final int K = GLFW_KEY_K;
//    public static final int L = GLFW_KEY_L;
//    public static final int Z = GLFW_KEY_Z;
//    public static final int X = GLFW_KEY_X;
//    public static final int C = GLFW_KEY_C;
//    public static final int V = GLFW_KEY_V;
//    public static final int B = GLFW_KEY_B;
//    public static final int N = GLFW_KEY_N;
//    public static final int M = GLFW_KEY_M;
//
//    public static final int SPACE = GLFW_KEY_SPACE;
//    public static final int LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;
//    public static final int LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;
//    public static final int LEFT_ALT = GLFW_KEY_LEFT_ALT;
//    public static final int TAB = GLFW_KEY_TAB;
//    public static final int RIGHT_CONTROL = GLFW_KEY_RIGHT_CONTROL;
//    public static final int RIGHT_SHIFT = GLFW_KEY_RIGHT_SHIFT;
//    public static final int RIGHT_ALT = GLFW_KEY_RIGHT_ALT;

    public InputLWJGL(Game game) {
        super(game);
        this.window = ((DisplayLWJGL)game.getDisplay()).getWindow();
        init();
    }

    @Override
    public void init() {
        KEY_COUNT = 349;
        keys = new ArrayList<>();
        currentKeys = new int[KEY_COUNT];
        initKeyCallBacks();
        initMouseCallBacks();
        System.out.println("input initted");
    }

    public void initKeyCallBacks() {

//      initing keys list
        for(int i = 0;i < KEY_COUNT;i++) {
            keys.add(null);
        }

        GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {

//                Making sure keys is within range
                if(key > 0 && key < keys.size()) {
                    currentKeys[key] = action;
                }

            }
        };

        glfwSetKeyCallback(window,keyCallback);
    }

    public void poll() {
        for (int i = 0; i < KEY_COUNT; ++i) {
            // Set the key state
            if (currentKeys[i] == GLFW_PRESS || currentKeys[i] == GLFW_REPEAT) {
                if (keys.get(i) == KeyState.RELEASED)
                    keys.set(i, KeyState.ONCE);
                else
                    keys.set(i, KeyState.PRESSED);
            } else {
                if(keys.get(i) == KeyState.PRESSED) {
                    keys.set(i, KeyState.JUST_RELEASED);
                }
                else {
                    keys.set(i, KeyState.RELEASED);
                }
            }
        }

    }

    public void initMouseCallBacks() {
        GLFWMouseButtonCallback mouseCallBack = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {

                if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                    isLeftMouseButtonPressed = true;
                }

                if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE) {
                    isLeftMouseButtonPressed = false;
                }

                if(button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
                    isRightMouseButtonPressed = true;
                }

                if(button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_RELEASE) {
                    isRightMouseButtonPressed = false;
                }

            }
        };

        glfwSetMouseButtonCallback(window,mouseCallBack);

        GLFWCursorPosCallback cursor = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseDx += (xpos - mouseX);
                mouseDy += (ypos - mouseY);

                mouseX = (float)xpos;
                mouseY = (float)ypos;

            }
        };

        glfwSetCursorPosCallback(window,cursor);
    }

}
