package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public List<Boundary> boundaries;
    public VerticalBoundary left;
    public VerticalBoundary right;
    public HorizontalBoundary top;
    public HorizontalBoundary bottom;

    public ConstraintComponent(Game game, Component parent, Vector radii, String identifier) {
        super(game, parent, radii, identifier);
        init();
    }

    public ConstraintComponent(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        init();
    }

    public void init() {
        boundaries = new ArrayList<>();

        left = new VerticalBoundary(game, this, identifier+"_left", 5,false);
        right = new VerticalBoundary(game, this, identifier+"_right", 5,false);
        top = new HorizontalBoundary(game, this, identifier+"_top",5, false);
        bottom = new HorizontalBoundary(game, this, identifier+"_bottom", 5,false);

        left.
                addConstraint(new HeightPercent(1)).
                addConstraint(new PosXLeftAttachPercent(0)).
                setContainerVisibility(true);
        right.
                addConstraint(new HeightPercent(1)).
                addConstraint(new PosXRightAttachPercent(0)).
                setContainerVisibility(true);

        top.
                addConstraint(new WidthPercent(1)).
                addConstraint(new PosYTopAttachPercent(0)).
                setContainerVisibility(true);
        bottom.
                addConstraint(new WidthPercent(1)).
                addConstraint(new PosYBottomAttachPercent(0)).
                setContainerVisibility(true);

        addBoundary(left);
        addBoundary(right);
        addBoundary(top);
        addBoundary(bottom);
    }

    public ConstraintComponent addBoundary(Boundary b) {
        boundaries.add(b);
        children.add(0, b);
        return this;
    }

}
