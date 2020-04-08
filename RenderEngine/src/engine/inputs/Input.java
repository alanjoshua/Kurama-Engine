package engine.inputs;

import java.util.List;
import engine.Math.Vector;
import engine.game.Game;

public abstract class Input {

    protected enum KeyState {
        JUST_RELEASED, // was just released
        RELEASED, // Not pressed
        ONCE,
        PRESSED, // key pressed
    }

    public float mouseX,mouseY,mouseDx,mouseDy;
    public boolean isLeftMouseButtonPressed,isRightMouseButtonPressed;
    protected List<Input.KeyState> keys;
    protected int[] currentKeys;

    protected int KEY_COUNT;

    Game game;

    public Input(Game game) {
        this.game = game;
    }

    public abstract void init();

    public boolean keyDown(int keyCode) {
        return keys.get(keyCode) == KeyState.PRESSED;
    }

    public boolean keyJustReleased(int key) {
        return keys.get(key) == KeyState.JUST_RELEASED;
    }

    public boolean keyDownOnce(int keyCode) {
        return keys.get(keyCode) == KeyState.ONCE;
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

    public abstract void poll();

    public boolean isLeftMouseButtonPressed() {return isLeftMouseButtonPressed;}
    public boolean isRightMouseButtonPressed() {return isRightMouseButtonPressed;}
}
