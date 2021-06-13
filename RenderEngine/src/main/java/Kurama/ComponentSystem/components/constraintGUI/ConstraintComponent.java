package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public List<Boundary> boundaries = new ArrayList<>();
    public BoundaryConfigurator configurator = null;
    public List<GridCell> gridCells = new ArrayList<>(); // GridCells would be considered as children

    public ConstraintComponent(Game game, Component parent, String identifier) {
        this(game, parent, identifier, null);
    }

    public ConstraintComponent(Game game, Component parent, String identifier, BoundaryConfigurator configurator) {
        super(game, parent, identifier);
        this.configurator = configurator;
        addInitAutomation((cur, in, t) -> init());

        addAutomationAfterChildTick((cur, in, t) -> {
            for(var b: boundaries) {
                b.shouldUpdateGridCell = false;
            }
        });
    }

    public ConstraintComponent addGridCell(GridCell g) {
        gridCells.add(g);

        if(children.size() == boundaries.size()) {
            children.add(g);
        }
        else {
            children.add(boundaries.size(), g);
        }

        if(g.attachedComp!=null) {
            addChild(g.attachedComp);
        }
        return this;
    }

    public ConstraintComponent addBoundary(Boundary bound) {
        boundaries.add(bound);
        this.children.add(0, bound);
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

    public Boundary createBoundary(String identifier, Boundary.BoundaryOrient orient, boolean userInteractable) {
        var b = new Boundary(game, this, identifier, orient, userInteractable, configurator);
        addBoundary(b);
        return b;
    }

    public GridCell createGridCell(String identifier) {
         var g = new GridCell(game, this, identifier);
         addGridCell(g);
         return g;
    }

    public ConstraintComponent setConfigurator(BoundaryConfigurator config) {
        this.configurator = config;
        return this;
    }

    public ConstraintComponent init() {

        // These are the default borders around the component.
        // Width and height (for vertical and horizontal boundaries respectively) are set to 0 by default

        var l = new Boundary(this.game, this, identifier+"_left", Boundary.BoundaryOrient.Vertical, false, configurator);
        var t = new Boundary(this.game, this, identifier+"_top", Boundary.BoundaryOrient.Horizontal, false, configurator);

        var r = new Boundary(this.game, this, identifier+"_right", Boundary.BoundaryOrient.Vertical, false, configurator);
        var b = new Boundary(this.game, this, identifier+"_bottom", Boundary.BoundaryOrient.Horizontal, false, configurator);

        r.addInitAutomation(new WidthPercent(0f));
        l.addInitAutomation(new WidthPercent(0f));
        t.addInitAutomation(new HeightPercent(0f));
        b.addInitAutomation(new HeightPercent(0f));

        r.addInitAutomation(new PosXYBottomRightAttachPercent(0f, 0f));
        t.addInitAutomation(new PosXYTopLeftAttachPercent(0f, 0f));
        l.addInitAutomation(new PosXYTopLeftAttachPercent(0f, 0f));
        b.addInitAutomation(new PosYBottomAttachPercent(0f)).addInitAutomation(new PosXLeftAttachPercent(0f));

        r.addInitAutomation(new HeightPercent(1f));
        l.addInitAutomation(new HeightPercent(1f));
        t.addInitAutomation(new WidthPercent(1f));
        b.addInitAutomation(new WidthPercent(1f));

        addBoundary(l).addBoundary(r).addBoundary(t).addBoundary(b);

        l.addConnectedBoundary(t, 1, 0);
        l.addConnectedBoundary(b, 1, 1);
        r.addConnectedBoundary(t, 0, 0);
        r.addConnectedBoundary(b, 0, 1);

        return this;
    }

}
