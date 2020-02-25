package GUI;

import Math.Vector;
import main.Game;

import java.awt.*;

public class Button {

    public interface Behaviour {
        public void executeBehaviour(Button b, Vector mousePos, boolean isPressed);
    }

//    public interface Render {
//        public void render(Graphics g);
//    }

    public Vector position;
    public double width;  // relative to display width; value between 0 and 1 inclusive
    public double height; // relative to display height; value between 0 and 1 inclusive
    public Behaviour behaviour;
//  Render renderObj;
    public String text;
    public Color textColor;
    public Color bgColor = Color.BLACK;
    public Font textFont;
    private Game game;

    public Button(Game game, Vector position, double width, double height) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.game = game;
    }

    public void render(Graphics g) {
//        this.renderObj.render(g);
        g.setColor(bgColor);
        g.fillRect((int)position.get(0),(int)position.get(1),getWidthInPixels(),getHeightInPixels());
        g.setColor(textColor);
        g.setFont(textFont);
        int len = g.getFontMetrics().stringWidth(text);
        int dx = (int)((getWidthInPixels() - len)/2);
        g.drawString(text,(int)position.get(0) + dx,(int)position.get(1) + getHeightInPixels() / 2);
    }

    public void setBehaviour(Behaviour t) {
        this.behaviour = t;
    }

    public void tick(Vector mousePos, boolean isPressed) {
        behaviour.executeBehaviour(this,mousePos, isPressed);
    }

    public boolean isMouseInside(Vector mp) {

        if(mp.get(0) >= position.get(0) && mp.get(0) <= position.get(0) + getWidthInPixels() && mp.get(1) >= position.get(1) && mp.get(1) <= position.get(1) + getHeightInPixels()) {
            return true;
        }
        else {
            return false;
        }
    }

    private int getWidthInPixels() {
        return (int)(width * game.getDisplay().getWidth());
    }

    private int getHeightInPixels() {
        return (int)(height * game.getDisplay().getHeight());
    }

}
