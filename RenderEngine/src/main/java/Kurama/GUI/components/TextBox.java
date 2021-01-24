package Kurama.GUI.components;

import Kurama.GUI.animations.Animation;
import Kurama.GUI.automations.Blink;
import Kurama.GUI.automations.GrabKeyboardFocus;
import Kurama.GUI.automations.LoseKeyboardFocus;
import Kurama.GUI.constraints.CaretAttach;
import Kurama.GUI.constraints.Center;
import Kurama.Math.Vector;
import Kurama.font.FontTexture;
import Kurama.game.Game;
import Kurama.utils.Utils;

public class TextBox extends Rectangle {

    public Text text;
    public Component caret;

    public TextBox(Game game, Component parent, FontTexture fontTexture, String identifier) {
        super(game, parent, identifier);
        text = new Text(game, this, fontTexture, Utils.getUniqueID());
        children.add(text);
        text.addConstraint(new Center());
        addOnClickAction(new GrabKeyboardFocus());
        addOnClickOutsideAction(new LoseKeyboardFocus());

        caret = new Rectangle(game, this, Utils.getUniqueID());

        caret.color = new Vector(1,1,1,0.8f);
        caret.addAnimation(new Animation(Float.POSITIVE_INFINITY, new Blink(0.3f)));
        caret.addConstraint(new CaretAttach(text));
        caret.height = text.fontTexture.height;
        caret.width = 3;
        children.add(caret);
    }

    public TextBox(Game game, Component parent, FontTexture fontTexture, Component caret, String identifier) {
        super(game, parent, identifier);
        text = new Text(game, this, fontTexture, Utils.getUniqueID());
        children.add(text);
        text.addConstraint(new Center());
        addOnClickAction(new GrabKeyboardFocus());
        addOnClickOutsideAction(new LoseKeyboardFocus());

        this.caret = caret;
        this.caret.parent = this;
        this.caret.addConstraint(new CaretAttach(text));
        children.add(caret);
    }

}
