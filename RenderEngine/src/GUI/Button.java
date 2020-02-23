package GUI;

import Math.Vector;

import java.awt.*;

public class Button {

    public interface Behaviour {
        public void executeBehaviour(Button b, Vector mousePos, boolean isPressed);
    }

//    public interface Render {
//        public void render(Graphics g);
//    }

    public Vector position;
    public int width;
    public int height;
    public Behaviour behaviour;
//  Render renderObj;
    public String text;
    public Color textColor;
    public Color bgColor = Color.BLACK;
    public Font textFont;

    public Button(Vector position, int width, int height) {
        this.position = position;
        this.width = width;
        this.height = height;
    }

    public void render(Graphics g) {
//        this.renderObj.render(g);
        g.setColor(bgColor);
        g.fillRect((int)position.get(0),(int)position.get(1),width,height);
        g.setColor(textColor);
        g.setFont(textFont);
        int l = g.getFontMetrics().stringWidth(text);
        int dx = (width - l)/2;
        g.drawString(text,(int)position.get(0) + dx,(int)position.get(1) + height / 2);
    }

    public void setBehaviour(Behaviour t) {
        this.behaviour = t;
    }

    public void tick(Vector mousePos, boolean isPressed) {
        behaviour.executeBehaviour(this,mousePos, isPressed);
    }

    public boolean isMouseInside(Vector mp) {

        if(mp.get(0) >= position.get(0) && mp.get(0) <= position.get(0) + width && mp.get(1) >= position.get(1) && mp.get(1) <= position.get(1) + height) {
            return true;
        }
        else {
            return false;
        }
    }

}
