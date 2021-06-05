package Kurama.ComponentSystem.components;

import Kurama.ComponentSystem.automations.DisplayAttach;
import Kurama.Math.Vector;
import Kurama.display.Display;
import Kurama.game.Game;
import Kurama.inputs.Input;

public class MasterWindow extends Rectangle {

    public Display display;
    public Input input;

    public MasterWindow(Game game, Display display, Input input, String identifier) {
        super(game, null, new Vector(0,0,0,0), identifier);
        this.display = display;
        this.input = input;
        this.positionalAutomations.add(new DisplayAttach(display));
        display.resizeEvents.add( () -> this.isResizedOrMoved = true);

    }

    public void cleanUp() {
        display.cleanUp();
    }

//    Provides the same API as Display
    public int getDPI() { return display.getDPI(); }
    public float getScalingRelativeToDPI() { return display.getScalingRelativeToDPI();}
    public void toggleWindowModes() {
//            isResizedOrMoved=true;
//            Logger.log();
            display.toggleWindowModes();
    }
    public void setFullScreen() {display.setFullScreen();}
    public void setWindowedMode() {display.setWindowedMode();}
    public void setWindowedMode(int width, int height) {display.setWindowedMode(width, height);}
    public void disableCursor() {display.disableCursor();}
    public void enableCursor() {display.enableCursor();}
    public float getRefreshRate() {return display.getRefreshRate();}
}
