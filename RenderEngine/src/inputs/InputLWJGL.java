package inputs;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;

import Math.Vector;

import java.util.ArrayList;
import java.util.List;

public class InputLWJGL extends Input {

    private long window;

    public InputLWJGL(long window) {
        super();
        this.window = window;
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
