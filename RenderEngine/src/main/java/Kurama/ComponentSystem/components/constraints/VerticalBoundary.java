package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.ToggleKeyboardFocus;
import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;
import Kurama.inputs.Input;
import Kurama.utils.Logger;

public class VerticalBoundary extends Boundary {

    public VerticalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        width = 10;

        addOnClickAction(new ToggleKeyboardFocus());

        addOnKeyInputFocusedAction((Component current, Input input, float timeDelta) -> {
            pos.setDataElement(0, pos.get(0) + input.mouseDx);
            Logger.log(pos.toString());
        });

    }
}
