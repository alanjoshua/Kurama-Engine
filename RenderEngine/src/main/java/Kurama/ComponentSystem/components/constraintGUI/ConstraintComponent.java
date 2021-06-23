package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.*;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.FixToBorder;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchMessage;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchSystemConfigurator;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;

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

    // Variables required to manage the MasterBoundarySystem Integration
    public boolean isIntegratedWithParentBoundaries = false;
    protected ConstraintComponent master;
    protected List<Boundary> integratedBounds = new ArrayList<>();
    protected List<GridCell> integratedGridCells = new ArrayList<>();
    protected Boundary left_archive;
    protected Boundary right_archive;
    protected Boundary top_archive;
    protected Boundary bottom_archive;

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
    }

    public void integrateWithMasterConstraintSystem(GridCell g, ConstraintComponent master) {
        this.master = master;
        this.isIntegratedWithParentBoundaries = true;

        // attaching comp.top's attachments to the parent bound
        {
            for (var b : this.top.positiveAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;

                master.addBoundary(b);
                g.top.addConnectedBoundary(b, 1);
                b.replaceConnection(this.top, g.top);
            }
            for (var b : this.top.negativeAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.top.addConnectedBoundary(b, 0);
                b.replaceConnection(this.top, g.top);
            }
            for (var b : this.top.neutralAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.top.addConnectedBoundary(b, -1);
                b.replaceConnection(this.top, g.top);
            }
        }

        // attaching comp.bottom's attachments to the parent bound
        {
            for (var b : this.bottom.positiveAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.bottom.addConnectedBoundary(b, 1);
                b.replaceConnection(this.bottom,g. bottom);
            }
            for (var b : this.bottom.negativeAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.bottom.addConnectedBoundary(b, 0);
                b.replaceConnection(this.bottom, g.bottom);
            }
            for (var b : this.bottom.neutralAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.bottom.addConnectedBoundary(b, -1);
                b.replaceConnection(this.bottom, g.bottom);
            }
        }

        // attaching comp.right's attachments to the parent bound
        {
            for (var b : this.right.positiveAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.right.addConnectedBoundary(b, 1);
                b.replaceConnection(this.right, g.right);
            }
            for (var b : this.right.negativeAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.right.addConnectedBoundary(b, 0);
                b.replaceConnection(this.right, g.right);
            }
            for (var b : this.right.neutralAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.right.addConnectedBoundary(b, -1);
                b.replaceConnection(this.right, g.right);
            }
        }

        // attaching comp.left's attachments to the parent bound
        {
            for (var b : this.left.positiveAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.left.addConnectedBoundary(b, 1);
                b.replaceConnection(this.left, g.left);
            }
            for (var b : this.left.negativeAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.left.addConnectedBoundary(b, 0);
                b.replaceConnection(this.left, g.left);
            }
            for (var b : this.left.neutralAttachments) {
                if(b == this.top || b == this.bottom || b == this.right || b == this.left) continue;
                master.addBoundary(b);
                g.left.addConnectedBoundary(b, -1);
                b.replaceConnection(this.left, g.left);
            }
        }

        this.top_archive = this.top;
        this.bottom_archive = this.bottom;
        this.right_archive = this.right;
        this.left_archive = this.left;

        this.top.shouldTickRenderGroup = false;
        this.bottom.shouldTickRenderGroup = false;
        this.right.shouldTickRenderGroup = false;
        this.left.shouldTickRenderGroup = false;

//        this.removeBoundary(this.top)
//                .removeBoundary(this.bottom)
//                .removeBoundary(this.right)
//                .removeBoundary(this.left);

        this.top = g.top;
        this.bottom = g.bottom;
        this.right = g.right;
        this.left = g.left;
    }

    @Override
    public boolean resizeReposition(Vector newPos, int newWidth, int newHeight) {

        var dw = newWidth - this.getWidth();
        var dh = newHeight - this.getHeight();

        if(this.left != null) { // To make sure boundaries exist

            this.pos = newPos;
            this.width = newWidth;
            this.height = newHeight;

            if(!isIntegratedWithParentBoundaries) {
                Logger.logError("ARTIFICALLY MOVING BOUNDARIES");
                if (dw != 0) {
                    if (!this.left.interact(new StretchMessage(-(dw / 2f), 0, null, true), null, -1)) return false;
                    if (!this.right.interact(new StretchMessage((dw / 2f), 0, null, true), null, -1)) return false;
                }
                if (dh != 0) {
                    if (!this.top.interact(new StretchMessage(0, -(dh / 2f), null, true), null, -1)) return false;
                    if (!this.bottom.interact(new StretchMessage(0, (dh / 2f), null, true), null, -1)) return false;
                }
            }
        }
        else {
            return false;
        }

        return true;
    }

    public ConstraintComponent addGridCell(GridCell g) {
        if(!isIntegratedWithParentBoundaries) {

            g.parent = this;
            gridCells.add(g);

            if (children.size() == boundaries.size()) {
                children.add(g);
            } else {
                children.add(boundaries.size(), g);
            }
        }
        else {
            master.addGridCell(g);
            integratedGridCells.add(g);
        }

        return this;
    }

    public ConstraintComponent removeBoundary(Boundary bound) {
        boundaries.remove(bound);
        children.remove(bound);
        return this;
    }

    public ConstraintComponent addBoundary(Boundary bound) {

        if(!isIntegratedWithParentBoundaries) {

            bound.parent = this;

            if (children.size() == boundaries.size()) {
                children.add(bound);
            } else {
                children.add(boundaries.size(), bound);
            }

            boundaries.add(bound);
        }
        else {
            master.addBoundary(bound);
            integratedBounds.add(bound);
        }
        return this;
    }

    public Boundary getBoundary(String bName) {
        var o = boundaries.stream().filter(b -> b.identifier.equals(bName)).findFirst();
        if(o.isPresent()) {
            return o.get();
        }
        else {
            if(isIntegratedWithParentBoundaries) {
                return master.getBoundary(bName);
            }
            return null;
        }
    }

    public Boundary createBoundary(String identifier, Boundary.BoundaryOrient orient, boolean userInteractable, BoundaryConfigurator configurator) {
        var b = new Boundary(game, this, identifier, orient, userInteractable, configurator);
        addBoundary(b);
        return b;
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

//        right.addInitAutomation(new HeightPercent(1f));
//        left.addInitAutomation(new HeightPercent(1f));
//        top.addInitAutomation(new WidthPercent(1f));
//        bottom.addInitAutomation(new WidthPercent(1f));
//
//        right.addInitAutomation(new PosXYBottomRightAttachPercent(0f, 0f));
//        top.addInitAutomation(new PosXYTopLeftAttachPercent(0f, 0f));
//        left.addInitAutomation(new PosXYTopLeftAttachPercent(0f, 0f));
//        bottom.addInitAutomation(new PosYBottomAttachPercent(0f)).addInitAutomation(new PosXLeftAttachPercent(0f));

        right.addOnResizeAction(new HeightPercent(1f));
        left.addOnResizeAction(new HeightPercent(1f));
        top.addOnResizeAction(new WidthPercent(1f));
        bottom.addOnResizeAction(new WidthPercent(1f));

        right.addOnResizeAction(new PosXYBottomRightAttachPercent(0f, 0f));
        top.addOnResizeAction(new PosXYTopLeftAttachPercent(0f, 0f));
        left.addOnResizeAction(new PosXYTopLeftAttachPercent(0f, 0f));
        bottom.addOnResizeAction(new PosYBottomAttachPercent(0f)).addOnResizeAction(new PosXLeftAttachPercent(0f));

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
