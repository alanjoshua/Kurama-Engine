package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;
import Kurama.inputs.Input;

public class VerticalBoundary extends Boundary {

    public VerticalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        width = 10;

        addOnClickDraggedAction((Component current, Input input, float timeDelta) -> {
            current.pos.setDataElement(0, current.pos.get(0) + input.mouseDx);
        });

    }
}
