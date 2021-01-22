package Kurama.GUI.components;

import Kurama.GUI.automations.GrabKeyboardFocus;
import Kurama.GUI.automations.LoseKeyboardFocus;
import Kurama.GUI.constraints.Center;
import Kurama.font.FontTexture;
import Kurama.game.Game;
import Kurama.utils.Utils;

public class TextBox extends Rectangle {

    public Text text;

    public TextBox(Game game, Component parent, FontTexture fontTexture, String identifier) {
        super(game, parent, identifier);
        text = new Text(game, this, fontTexture, Utils.getUniqueID());
        children.add(text);
        text.addConstraint(new Center());
        addOnClickAction(new GrabKeyboardFocus());
        addOnClickOutsideAction(new LoseKeyboardFocus());
    }
}
