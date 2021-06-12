package Kurama.ComponentSystem.components;

import Kurama.ComponentSystem.automations.CheckForTextUpdate;
import Kurama.ComponentSystem.automations.PosXYTopLeftAttachPix;
import Kurama.font.FontTexture;
import Kurama.game.Game;
import Kurama.utils.Utils;

import java.util.ArrayList;

public class Text extends Rectangle {

    public String text = "";
    public FontTexture fontTexture;
    public boolean shouldUpdate = true;

    public Text(Game game, Component parent, FontTexture fontTexture, String identifier) {
        super(game, parent, identifier);
        this.fontTexture = fontTexture;
        isContainerVisible = false;
        addAutomation(new CheckForTextUpdate(this));
    }

    public Component setFontTexture(FontTexture fontTexture) {
        this.fontTexture = fontTexture;
        shouldUpdate = true;
        return this;
    }

    public Text setText(String text) {
        this.text = text;
        shouldUpdate = true;
        return this;
    }

    public void updateText() {

        children = new ArrayList<>();

        if(text == null || text.length() == 0) {
            return;
        }

        int curPos = 0;
        this.setWidth(0);
        this.setHeight(fontTexture.height);

        for(char c: text.toCharArray()) {
            var charInfo = fontTexture.charMap.get(c);
            var newComp = new Rectangle(game, this, Utils.getUniqueID());

            newComp.onResizeAutomations.add(new PosXYTopLeftAttachPix(curPos, 0));

            if(charInfo != null) {
                newComp.texUL = charInfo.texUL;
                newComp.texBL = charInfo.texBL;
                newComp.texUR = charInfo.texUR;
                newComp.texBR = charInfo.texBR;
                newComp.setWidth(charInfo.width);
                newComp.setHeight(fontTexture.height);
                newComp.texture = fontTexture.texture;

                newComp.setOverlayColor(this.overlayColor);
                curPos += newComp.getWidth();
                setWidth(getWidth() + newComp.getWidth());
                children.add(newComp);
            }
            // Check for special characters
            else if((int)c == 32) {
                newComp.setWidth(fontTexture.charMap.get('O').width);
                newComp.setHeight(fontTexture.height);
                newComp.isContainerVisible = false;

                newComp.setOverlayColor(this.overlayColor);
                curPos += newComp.getWidth();
                setWidth(getWidth() + newComp.getWidth());
                children.add(newComp);
            }

        }

        this.setHeight(fontTexture.height);
        shouldUpdate = false;
    }

}
