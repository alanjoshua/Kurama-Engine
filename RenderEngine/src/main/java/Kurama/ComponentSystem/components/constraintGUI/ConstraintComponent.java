package Kurama.ComponentSystem.components.constraintGUI;

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

}
