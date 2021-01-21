package Kurama.GUI.components;

import Kurama.GUI.automations.GrabKeyboardFocus;
import Kurama.GUI.automations.LoseKeyboardFocus;
import Kurama.GUI.constraints.Center;
import Kurama.font.FontTexture;
import Kurama.utils.Utils;

public class TextBox extends Rectangle {

    public Text text;

    public TextBox(Component parent, FontTexture fontTexture, String identifier) {
        super(parent, identifier);
        text = new Text(this, fontTexture, Utils.getUniqueID());
        children.add(text);
        text.addConstraint(new Center());
        addOnClickAction(new GrabKeyboardFocus());
        addOnClickOutsideAction(new LoseKeyboardFocus());
    }
}
