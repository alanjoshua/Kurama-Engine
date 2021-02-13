package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;
import Kurama.inputs.Input;

public class HorizontalBoundary extends Boundary {

    public HorizontalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        height = 10;

        addOnClickDraggedAction((Component current, Input input, float timeDelta) -> {
            current.pos.setDataElement(1, current.pos.get(1) + input.mouseDy);
        });

    }
}
