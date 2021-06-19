package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.FixToBorder;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchMessage;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchSystemConfigurator;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public static BoundaryConfigurator DEFAULT_CONFIGURATOR = new StretchSystemConfigurator();

    public List<Boundary> boundaries = new ArrayList<>();
    public BoundaryConfigurator configurator;
    public List<GridCell> gridCells = new ArrayList<>(); // GridCells would be considered as children

    public Boundary left;
    public Boundary right;
    public Boundary top;
    public Boundary bottom;

    public ConstraintComponent(Game game, Component parent, String identifier) {
        this(game, parent, identifier, DEFAULT_CONFIGURATOR);
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

        onResizeAutomations.add((cur, in, t) -> {

            var dw = this.getWidth() - this.previousWidth;
            var dh = this.getHeight() - this.previousHeight;

            if(left != null) { // To make sure boundaries exist

                left.interact(new StretchMessage(-(dw/2f), 0, null, true), null, -1);
                right.interact(new StretchMessage((dw/2f), 0, null, true), null, -1);
                top.interact(new StretchMessage(0, -(dh/2f), null, true), null, -1);
                bottom.interact(new StretchMessage(0, (dh/2f), null, true), null, -1);
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

        return this;
    }

    public ConstraintComponent addBoundary(Boundary bound) {

        if(children.size() == boundaries.size()) {
            children.add(bound);
        }
        else {
            children.add(boundaries.size(), bound);
        }

        boundaries.add(bound);
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

    // Initialises surrounding boundaries
    public void init() {

        // These are the default borders around the component.
        // Width and height (for vertical and horizontal boundaries respectively) are set to 0 by default

        left = new Boundary(this.game, this, identifier+"_left", Boundary.BoundaryOrient.Vertical, false, configurator);
        top = new Boundary(this.game, this, identifier+"_top", Boundary.BoundaryOrient.Horizontal, false, configurator);

        right = new Boundary(this.game, this, identifier+"_right", Boundary.BoundaryOrient.Vertical, false, configurator);
        bottom = new Boundary(this.game, this, identifier+"_bottom", Boundary.BoundaryOrient.Horizontal, false, configurator);

//        r.addInitAutomation(new WidthPercent(0f));
//        l.addInitAutomation(new WidthPercent(0f));
//        t.addInitAutomation(new HeightPercent(0f));
//        b.addInitAutomation(new HeightPercent(0f));

        right.addInitAutomation(new HeightPercent(1f));
        left.addInitAutomation(new HeightPercent(1f));
        top.addInitAutomation(new WidthPercent(1f));
        bottom.addInitAutomation(new WidthPercent(1f));

        right.addInitAutomation(new PosXYBottomRightAttachPercent(0f, 0f));
        top.addInitAutomation(new PosXYTopLeftAttachPercent(0f, 0f));
        left.addInitAutomation(new PosXYTopLeftAttachPercent(0f, 0f));
        bottom.addInitAutomation(new PosYBottomAttachPercent(0f)).addInitAutomation(new PosXLeftAttachPercent(0f));

        addBoundary(left).addBoundary(right).addBoundary(top).addBoundary(bottom);

        left.addConnectedBoundary(top, 1, 0);
        left.addConnectedBoundary(bottom, 1, 1);
        right.addConnectedBoundary(top, 0, 0);
        right.addConnectedBoundary(bottom, 0, 1);

        left.addPreInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.left));
        right.addPreInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.right));
        top.addPreInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.top));
        bottom.addPreInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.bottom));

        left.addPostInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.left));
        right.addPostInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.right));
        top.addPostInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.top));
        bottom.addPostInteractionValidifier(new FixToBorder(FixToBorder.AttachPoint.bottom));

    }

}
