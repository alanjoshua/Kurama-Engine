package Kurama.GUI;

import Kurama.Math.Vector;
import Kurama.display.Display;

public class MasterWindow extends Rectangle {

    public Display display;
    public MasterWindow(Display display, String identifier) {
        super(null, new Vector(0,0,0,0), identifier);
        this.display = display;
    }

    @Override
    public void resolveConstraints() {

        for(var constraint: constraints) {
            constraint.solveConstraint(parent, this);
        }

        this.width = display.windowResolution.geti(0);
        this.height = display.windowResolution.geti(1);

        for(var child: children) {
            child.resolveConstraints();
        }
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
