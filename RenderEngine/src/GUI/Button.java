package GUI;

import Math.Vector;
import models.Model;

import java.awt.*;

public class Button {

    public interface Tick {
        public void tick(Vector mousePos,boolean isPressed);
    }

//    public interface Render {
//        public void render(Graphics g);
//    }

    public Vector position;
    public int width;
    public int height;
    public Tick tickObj;
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

    public void setTickObj(Tick t) {
        this.tickObj = t;
    }

//    public void setRenderObj(Render r) {
//        this.renderObj = r;
//    }

    public void tick(Vector mousePos, boolean isPressed) {
        tickObj.tick(mousePos, isPressed);
    }

}
