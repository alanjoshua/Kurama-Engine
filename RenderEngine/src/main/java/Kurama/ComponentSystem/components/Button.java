package Kurama.ComponentSystem.components;

import Kurama.ComponentSystem.automations.Center;
import Kurama.font.FontTexture;
import Kurama.game.Game;

public class Button extends Rectangle {

    public Text buttonText;

    public Button(Game game, Component parent, FontTexture fontTexture, String buttonTextString, String identifier) {
        super(game, parent, identifier);
        buttonText = new Text(game, this, fontTexture, identifier);
        buttonText.setText(buttonTextString);
        buttonText.addOnResizeAction(new Center());
        children.add(buttonText);
    }
}
