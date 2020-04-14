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

//    public final int Q = GLFW_KEY_Q;
//    public final int W = GLFW_KEY_W;
//    public final int E = GLFW_KEY_E;
//    public final int R = GLFW_KEY_R;
//    public final int T = GLFW_KEY_T;
//    public final int Y = GLFW_KEY_Y;
//    public final int U = GLFW_KEY_U;
//    public final int I = GLFW_KEY_I;
//    public final int O = GLFW_KEY_O;
//    public final int P = GLFW_KEY_P;
//    public final int A = GLFW_KEY_A;
//    public final int S = GLFW_KEY_S;
//    public final int D = GLFW_KEY_D;
//    public final int F = GLFW_KEY_F;
//    public final int G = GLFW_KEY_G;
//    public final int H = GLFW_KEY_H;
//    public final int J = GLFW_KEY_J;
//    public final int K = GLFW_KEY_K;
//    public final int L = GLFW_KEY_L;
//    public final int Z = GLFW_KEY_Z;
//    public final int X = GLFW_KEY_X;
//    public final int C = GLFW_KEY_C;
//    public final int V = GLFW_KEY_V;
//    public final int B = GLFW_KEY_B;
//    public final int N = GLFW_KEY_N;
//    public final int M = GLFW_KEY_M;
//
//    public final int SPACE = GLFW_KEY_SPACE;
//    public final int LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;
//    public final int LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;
//    public final int LEFT_ALT = GLFW_KEY_LEFT_ALT;
//    public final int TAB = GLFW_KEY_TAB;
//    public final int RIGHT_CONTROL = GLFW_KEY_RIGHT_CONTROL;
//    public final int RIGHT_SHIFT = GLFW_KEY_RIGHT_SHIFT;
//    public final int RIGHT_ALT = GLFW_KEY_RIGHT_ALT;

    public InputLWJGL(Game game) {
        super(game);
        this.window = ((DisplayLWJGL)game.getDisplay()).getWindow();
        init();
    }

    @Override
    public void init() {
        initKeyFields();
        KEY_COUNT = 349;
        keys = new ArrayList<>();
        currentKeys = new int[KEY_COUNT];
        initKeyCallBacks();
        initMouseCallBacks();
        System.out.println("input initted");
    }

    public void initKeyFields() {
        Q = GLFW_KEY_Q;
        W = GLFW_KEY_W;
        E = GLFW_KEY_E;
        R = GLFW_KEY_R;
        T = GLFW_KEY_T;
        Y = GLFW_KEY_Y;
        U = GLFW_KEY_U;
        I = GLFW_KEY_I;
        O = GLFW_KEY_O;
        P = GLFW_KEY_P;
        A = GLFW_KEY_A;
        S = GLFW_KEY_S;
        D = GLFW_KEY_D;
        F = GLFW_KEY_F;
        G = GLFW_KEY_G;
        H = GLFW_KEY_H;
        J = GLFW_KEY_J;
        K = GLFW_KEY_K;
        L = GLFW_KEY_L;
        Z = GLFW_KEY_Z;
        X = GLFW_KEY_X;
        C = GLFW_KEY_C;
        V = GLFW_KEY_V;
        B = GLFW_KEY_B;
        N = GLFW_KEY_N;
        M = GLFW_KEY_M;

        SPACE = GLFW_KEY_SPACE;
        LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;
        LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;
        LEFT_ALT = GLFW_KEY_LEFT_ALT;
        TAB = GLFW_KEY_TAB;
        RIGHT_CONTROL = GLFW_KEY_RIGHT_CONTROL;
        RIGHT_SHIFT = GLFW_KEY_RIGHT_SHIFT;
        RIGHT_ALT = GLFW_KEY_RIGHT_ALT;
        ESCAPE = GLFW_KEY_ESCAPE;
        ENTER = GLFW_KEY_ENTER;

        UP_ARROW = GLFW_KEY_UP;
        DOWN_ARROW = GLFW_KEY_DOWN;
        LEFT_ARROW = GLFW_KEY_LEFT;
        RIGHT_ARROW = GLFW_KEY_RIGHT;

        ONE = GLFW_KEY_1;
        TWO = GLFW_KEY_2;
        THREE = GLFW_KEY_3;
        FOUR = GLFW_KEY_4;
        FIVE = GLFW_KEY_5;
        SIX = GLFW_KEY_6;
        SEVEN = GLFW_KEY_7;
        EIGHT = GLFW_KEY_8;
        NINE = GLFW_KEY_9;
        ZERO = GLFW_KEY_0;
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
