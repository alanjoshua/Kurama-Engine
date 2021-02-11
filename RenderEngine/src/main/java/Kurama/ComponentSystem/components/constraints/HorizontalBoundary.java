package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.ToggleKeyboardFocus;
import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;
import Kurama.inputs.Input;
import Kurama.utils.Logger;

public class HorizontalBoundary extends Boundary {

    public HorizontalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        height = 10;

        addOnClickAction(new ToggleKeyboardFocus());

        addOnKeyInputFocusedAction((Component current, Input input, float timeDelta) -> {
            pos.setDataElement(1, pos.get(1) + input.mouseDy);
            Logger.log(pos.toString());
        });

    }
}
