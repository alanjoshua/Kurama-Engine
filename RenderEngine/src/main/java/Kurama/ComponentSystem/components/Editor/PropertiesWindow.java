package Kurama.ComponentSystem.components.Editor;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintComponent;
import Kurama.game.Game;

import java.lang.reflect.Field;

public class PropertiesWindow extends ConstraintComponent {

    public Component currentAttachedComponent;

    public PropertiesWindow(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
    }

    public void updateProperties(Component newComp) {

        Field[] allFields = newComp.getClass().getDeclaredFields();

    }

}
