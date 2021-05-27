package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public List<Boundary> boundaries = new ArrayList<>();

    public ConstraintComponent(Game game, Component parent, Vector radii, String identifier) {
        super(game, parent, radii, identifier);
        init(null);
    }

    public ConstraintComponent(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        init(null);
    }

    public ConstraintComponent(Game game, Component parent, String identifier, BoundaryConfigurator configurator) {
        super(game, parent, identifier);
        init(configurator);
    }

    public ConstraintComponent addBoundary(Boundary bound) {
        boundaries.add(bound);
        this.children.add(bound);
        return this;
    }

    public void init(BoundaryConfigurator configurator) {
        // This is probably kinda temp

        var l = new Boundary(this.game, this, "leftB", Boundary.BoundaryOrient.Vertical, configurator);
        var t = new Boundary(this.game, this, "topB", Boundary.BoundaryOrient.Horizontal, configurator);

        var r = new Boundary(this.game, this, "rightB", Boundary.BoundaryOrient.Vertical, configurator);
        var b = new Boundary(this.game, this, "bottomB", Boundary.BoundaryOrient.Horizontal, configurator);

        r.initAutomations.add(new HeightPercent(0.5f));
        l.initAutomations.add(new HeightPercent(0.5f));
        t.initAutomations.add(new WidthPercent(0.5f));
        b.initAutomations.add(new WidthPercent(0.5f));

        r.addInitAutomation(new PosXYBottomRightAttachPercent(0.25f, 0.25f));
        t.addInitAutomation(new PosXYTopLeftAttachPercent(0.25f, 0.25f)).setColor(new Vector(1,0,0,1));
        l.addInitAutomation(new PosXYTopLeftAttachPercent(0.25f, 0.25f)).setColor(new Vector(0,1,0,1));
        b.addInitAutomation(new PosXYTopLeftAttachPercent(0.25f, 0.75f));

        addBoundary(l).addBoundary(r).addBoundary(t).addBoundary(b);

        l.addConnectedBoundary(t, 1, 0);
        l.addConnectedBoundary(b, 1, 0);
        r.addConnectedBoundary(t, 0, 1);
        r.addConnectedBoundary(b, 0, 1);
    }

}
