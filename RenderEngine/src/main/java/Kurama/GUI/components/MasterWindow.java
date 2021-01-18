package Kurama.GUI.components;

import Kurama.GUI.constraints.DisplayAttach;
import Kurama.Math.Vector;
import Kurama.display.Display;
import Kurama.inputs.Input;

public class MasterWindow extends Rectangle {

    public Display display;
    public Input input;

    public MasterWindow(Display display, Input input, String identifier) {
        super(null, new Vector(0,0,0,0), identifier);
        this.display = display;
        this.input = input;
        this.constraints.add(new DisplayAttach(display));
    }

    public void cleanUp() {
        display.cleanUp();
    }

//    Provides the same API as Display
    public int getDPI() { return display.getDPI(); }
    public float getScalingRelativeToDPI() { return display.getScalingRelativeToDPI();}
    public void toggleWindowModes() {display.toggleWindowModes();}
    public void setFullScreen() {display.setFullScreen();}
    public void setWindowedMode() {display.setWindowedMode();}
    public void setWindowedMode(int width, int height) {display.setWindowedMode(width, height);}
    public void disableCursor() {display.disableCursor();}
    public void enableCursor() {display.enableCursor();}
    public float getRefreshRate() {return display.getRefreshRate();}
}
