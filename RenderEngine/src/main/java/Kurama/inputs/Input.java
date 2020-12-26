package Kurama.inputs;

import java.util.List;
import Kurama.Math.Vector;
import Kurama.game.Game;

public abstract class Input {

    public int Q;
    public int W;
    public int E;
    public int R;
    public int T;
    public int Y;
    public int U;
    public int I;
    public int O;
    public int P;
    public int A;
    public int S;
    public int D;
    public int F;
    public int G;
    public int H;
    public int J;
    public int K;
    public int L;
    public int Z;
    public int X;
    public int C;
    public int V;
    public int B;
    public int N;
    public int M;

    public int SPACE;
    public int LEFT_CONTROL;
    public int LEFT_SHIFT;
    public int LEFT_ALT;
    public int TAB;
    public int RIGHT_CONTROL;
    public int RIGHT_SHIFT;
    public int RIGHT_ALT;
    public int ESCAPE;

    public int UP_ARROW;
    public int DOWN_ARROW;
    public int LEFT_ARROW;
    public int RIGHT_ARROW;
    public int ENTER;

    public int ONE;
    public int TWO;
    public int THREE;
    public int FOUR;
    public int FIVE;
    public int SIX;
    public int SEVEN;
    public int EIGHT;
    public int NINE;
    public int ZERO;

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
