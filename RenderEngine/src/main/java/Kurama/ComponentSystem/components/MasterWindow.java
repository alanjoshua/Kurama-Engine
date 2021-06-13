package Kurama.ComponentSystem.components;

import Kurama.ComponentSystem.components.constraintGUI.ConstraintComponent;
import Kurama.display.Display;
import Kurama.game.Game;
import Kurama.inputs.Input;

public class MasterWindow extends ConstraintComponent {

    public Display display;
    public Input input;

    public MasterWindow(Game game, Display display, Input input, String identifier) {
        super(game, null, identifier);
        this.display = display;
        this.input = input;

        onResizeAutomations.add((cur, in, t) -> {

            var dw = display.windowResolution.geti(0) - this.getWidth();
            var dh = display.windowResolution.geti(1) - this.getHeight();

            // becoming bigger
            this.setWidth(display.windowResolution.geti(0));
            this.setHeight(display.windowResolution.geti(1));

            if(getBoundary(identifier+"_left") != null) {
                getBoundary(identifier + "_left").initialiseInteraction(-dw / 2f, 0);
                getBoundary(identifier + "_right").initialiseInteraction(dw / 2f, 0);

                getBoundary(identifier + "_top").initialiseInteraction(0, -dh / 2f);
                getBoundary(identifier + "_bottom").initialiseInteraction(0, dh / 2f);
            }
        });

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
