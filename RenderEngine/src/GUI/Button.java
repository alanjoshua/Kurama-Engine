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

    public Vector position; // relative to display width and height
    public double width;  // relative to display width; value between 0 and 1 inclusive
    public double height; // relative to display height; value between 0 and 1 inclusive
    public Behaviour behaviour;
//  Render renderObj;
    public String text;
    public Color textColor;
    public Color bgColor = Color.BLACK;
    public Font textFont;
    public float scale = 1.0f;
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
        g.fillRect(getX(),getY(),getWidthInPixels(),getHeightInPixels());
        g.setColor(textColor);
        g.setFont(textFont);
        int len = g.getFontMetrics().stringWidth(text);
        int dx = (int)((getWidthInPixels() - len)/2);
        g.drawString(text,getX() + dx,getY() + getHeightInPixels() / 2);
    }

    public void setBehaviour(Behaviour t) {
        this.behaviour = t;
    }

    public void tick(Vector mousePos, boolean isPressed) {
        behaviour.executeBehaviour(this,mousePos, isPressed);
    }

    public boolean isMouseInside(Vector mp) {

        if(mp.get(0) >= getX() && mp.get(0) <= getX() + getWidthInPixels() && mp.get(1) >= getY() && mp.get(1) <= getY() + getHeightInPixels()) {
            return true;
        }
        else {
            return false;
        }
    }

    public int getX() {
        return (int)(position.get(0) * game.getDisplay().getWidth());
    }

    public int getY() {
        return (int)(position.get(1) * game.getDisplay().getHeight());
    }

    private int getWidthInPixels() {
//        return (int)(width * game.getDisplay().getWidth() * scale);
        return (int)(width * game.getDisplay().getScalingRelativeToDPI() * scale);

    }

    private int getHeightInPixels() {
//      return (int)(height * game.getDisplay().getHeight() * scale);
        return (int)(height * game.getDisplay().getScalingRelativeToDPI() * scale);

    }

}
