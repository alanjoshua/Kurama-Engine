package Kurama.GUI.components;

import Kurama.GUI.constraints.Center;
import Kurama.font.FontTexture;

public class Button extends Rectangle {

    public Text buttonText;

    public Button(Component parent, FontTexture fontTexture, String buttonTextString, String identifier) {
        super(parent, identifier);
        buttonText = new Text(this, fontTexture, identifier);
        buttonText.setText(buttonTextString);
        buttonText.addConstraint(new Center());
        children.add(buttonText);
    }
}
