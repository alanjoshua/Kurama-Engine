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

        left = new VerticalBoundary(game, this, identifier+"_left", true);
        right = new VerticalBoundary(game, this, identifier+"_right", true);
        top = new HorizontalBoundary(game, this, identifier+"_top", true);
        bottom = new HorizontalBoundary(game, this, identifier+"_bottom", true);

        left.
                addConstraint(new HeightPercent(1)).
                addConstraint(new PosXLeftAttachPercent(0));
        right.
                addConstraint(new HeightPercent(1)).
                addConstraint(new PosXRightAttachPercent(0));

        top.
                addConstraint(new WidthPercent(1)).
                addConstraint(new PosYTopAttachPercent(0));
        bottom.
                addConstraint(new WidthPercent(1)).
                addConstraint(new PosYBottomAttachPercent(0));

        addBoundary(left);
        addBoundary(right);
        addBoundary(top);
        addBoundary(bottom);
    }

    public ConstraintComponent addBoundary(Boundary b) {
        boundaries.add(b);
        children.add(b);
        return this;
    }

}
