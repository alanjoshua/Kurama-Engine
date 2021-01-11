package Kurama.GUI;

import Kurama.GUI.constraints.Constraint;
import Kurama.font.FontTexture;
import Kurama.inputs.Input;

import java.util.ArrayList;
import java.util.List;

public class Text extends Component {

    public String text;
    public FontTexture fontTexture;
    protected boolean shouldUpdate;

    public Text(Component parent, FontTexture fontTexture, String identifier) {
        super(parent, identifier);
        this.fontTexture = fontTexture;
    }

    public Component setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public void tick(List<Constraint> parentGlobalConstraints, Input input) {
        if(shouldUpdate) {
            updateText();
        }
    }

    protected void updateText() {
        children = new ArrayList<>();

        for(char c: text.toCharArray()) {
            var charInfo = fontTexture.charMap.get(c);

        }

    }

}
