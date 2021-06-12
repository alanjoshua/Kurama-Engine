package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.HeightPercent;
import Kurama.ComponentSystem.automations.PosXYBottomRightAttachPercent;
import Kurama.ComponentSystem.automations.PosXYTopLeftAttachPercent;
import Kurama.ComponentSystem.automations.WidthPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public List<Boundary> boundaries = new ArrayList<>();
    public BoundaryConfigurator configurator = null;
    public List<GridCell> gridCells = new ArrayList<>(); // GridCells would be considered as children

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
        this.configurator = configurator;
        init(configurator);
    }

    public ConstraintComponent addGridCell(GridCell g) {
        gridCells.add(g);
        addChild(g);
        if(g.attachedComp!=null) {
            addChild(g.attachedComp);
        }
        return this;
    }

    public ConstraintComponent addBoundary(Boundary bound) {
        boundaries.add(bound);
        this.children.add(bound);
        return this;
    }

    public Boundary getBoundary(String bName) {
        var o = boundaries.stream().filter(b -> b.identifier.equals(bName)).findFirst();
        if(o.isPresent()) {
            return o.get();
        }
        else {
            return null;
        }
    }

    public Boundary createBoundary(Game game, Component parent, String identifier, Boundary.BoundaryOrient orient) {
        var b = new Boundary(game, parent, identifier, orient, configurator);
        addBoundary(b);
        return b;
    }

    public Boundary createBoundary(Game game, Component parent, String identifier, Boundary.BoundaryOrient orient, BoundaryConfigurator configurator) {
        var b = new Boundary(game, parent, identifier, orient, configurator);
        addBoundary(b);
        return b;
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
        t.addInitAutomation(new PosXYTopLeftAttachPercent(0.25f, 0.25f));
        l.addInitAutomation(new PosXYTopLeftAttachPercent(0.25f, 0.25f));
        b.addInitAutomation(new PosXYTopLeftAttachPercent(0.25f, 0.75f));

        addBoundary(l).addBoundary(r).addBoundary(t).addBoundary(b);

        l.addConnectedBoundary(t, 1, 0);
        l.addConnectedBoundary(b, 1, 1);
        r.addConnectedBoundary(t, 0, 0);
        r.addConnectedBoundary(b, 0, 1);

    }

}
