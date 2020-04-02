package inputs;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;

import Math.Vector;

public class InputLWJGL {

    public float mouseX,mouseY,mouseDx,mouseDy;
    private long window;
    public boolean isLeftMouseButtonPressed,isRightMouseButtonPressed;

    public InputLWJGL(long window) {
        this.window = window;
        init();
    }

    public void init() {
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

    public void poll() {
        mouseDx = 0;
        mouseDy = 0;
    }

    public Vector getPos() {
        Vector ret = new Vector(new float[]{mouseX,mouseY});
        return ret;
    }

    public Vector getDelta() {
        Vector ret = new Vector(new float[]{mouseDx,mouseDy});
        mouseDx = 0;
        mouseDy = 0;
        return ret;
    }

    public boolean isLeftMouseButtonPressed() {return isLeftMouseButtonPressed;}
    public boolean isRightMouseButtonPressed() {return isRightMouseButtonPressed;}

}
