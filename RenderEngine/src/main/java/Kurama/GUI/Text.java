package Kurama.GUI;

import Kurama.GUI.constraints.Constraint;
import Kurama.Math.Vector;
import Kurama.font.FontTexture;
import Kurama.inputs.Input;
import Kurama.utils.Logger;
import Kurama.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Text extends Component {

    public String text;
    public FontTexture fontTexture;
    protected boolean shouldUpdate = true;

    public Text(Component parent, FontTexture fontTexture, String identifier) {
        super(parent, identifier);
        this.fontTexture = fontTexture;
    }

    public Component setText(String text) {
        this.text = text;
        shouldUpdate = true;
        return this;
    }

    @Override
    public void tick(List<Constraint> parentGlobalConstraints, Input input, float timeDelta) {

        isClicked = false; // Reset before processing inputs for current frame
        currentIsMouseOver = false;
        isMouseLeft = false;

        currentIsMouseOver = isMouseOverComponent(input);  // This should always be first, since its result is used by isClicked
        isClicked = isClicked(input, currentIsMouseOver);
        isMouseLeft = isMouseLeft(input);

        for(var automation: automations) {
            automation.run(this, input, timeDelta);
        }

        for(var constraint: constraints) {
            constraint.solveConstraint(parent, this);
        }

        if(parentGlobalConstraints != null) {
            for (var globalConstraints : parentGlobalConstraints) {
                globalConstraints.solveConstraint(parent, this);
            }
        }

        if(shouldUpdate) {
            updateText();
        }

        if(parent != null) {
            globalOrientation = parent.globalOrientation.multiply(orientation);
        }

        for(var child: children) {
            child.tick(globalChildrenConstraints, input, timeDelta);
        }

        if(isClicked) {
            onClick(input, timeDelta);
        }

        if(currentIsMouseOver) {
            onMouseOver(input, timeDelta);
            previousIsMouseOver = true;
        }

        if(isMouseLeft) {
            onMouseLeave(input, timeDelta);
            previousIsMouseOver = false;
        }

    }

    protected void updateText() {
        children = new ArrayList<>();

        Vector curPos = new Vector(pos);
         this.width = 0;

        for(char c: text.toCharArray()) {
            var charInfo = fontTexture.charMap.get(c);
            var newComp = new Rectangle(this, Utils.getUniqueID());
            newComp.pos = curPos;
            newComp.texUL = charInfo.texUL;
            newComp.texBL = charInfo.texBL;
            newComp.texUR = charInfo.texUR;
            newComp.texBR = charInfo.texBR;
            newComp.width = charInfo.width;
            newComp.height = fontTexture.height;
            newComp.texture = fontTexture.texture;

            curPos = curPos.add(new Vector(new float[]{charInfo.width, 0, 0}));
            width += charInfo.width;

            children.add(newComp);
            Logger.log("adding child");
        }

        this.height = fontTexture.height;
        shouldUpdate = false;
    }

}
