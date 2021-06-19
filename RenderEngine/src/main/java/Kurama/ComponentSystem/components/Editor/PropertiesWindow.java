package Kurama.ComponentSystem.components.Editor;

import Kurama.Annotations.Property;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintComponent;
import Kurama.game.Game;
import Kurama.utils.Logger;
import Kurama.utils.Utils;

import java.lang.reflect.Field;
import java.util.List;

public class PropertiesWindow extends ConstraintComponent {

    public Component currentAttachedComponent;

    public PropertiesWindow(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
    }

    public void updateProperties(Component newComp) {

        List<Field> allFields = Utils.getAllDeclaredFieldsByAnnotation(newComp.getClass(), Property.class);
        for(var f: allFields) {
            try {
                Logger.log("field name: " + f.toString() + " value: "+ f.get(newComp));
            }
            catch (Exception e) {
                Logger.logError("couldnt get value from "+ f.toString());
            }
        }
    }

}
