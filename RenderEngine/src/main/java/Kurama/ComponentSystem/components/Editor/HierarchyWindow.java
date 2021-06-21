package Kurama.ComponentSystem.components.Editor;

import Kurama.ComponentSystem.automations.PosXLeftAttachPix;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.Text;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintComponent;
import Kurama.ComponentSystem.components.constraintGUI.GridCell;
import Kurama.Math.Vector;
import Kurama.font.FontTexture;
import Kurama.game.Game;
import Kurama.misc_structures.LinkedList.DoublyLinkedList;
import Kurama.utils.Logger;

import java.awt.*;
import java.util.List;

public class HierarchyWindow extends ConstraintComponent {

//     class Pair {
//
//         public Component comp;
//         public int level = 0;
//         public Pair(Component comp, int level) {this.comp = comp;this.level=level};
//
//     }

    DoublyLinkedList<GridCell> contents = new DoublyLinkedList<>();
    public int initOffsetX = 30;
    public int gridHeight = 100;
    public int stepOffset = 40;
    public FontTexture fontTexture = new FontTexture(new Font("Arial", Font.ITALIC, 17), FontTexture.defaultCharSet);
    private boolean alreadyCreated = false;

    public HierarchyWindow(Game game, Component parent, String identifier) {
        super(game, parent, identifier);

        addInitAutomation((c,i,t) -> {
            top.setColor(new Vector(0,1,1,1));
            bottom.setColor(new Vector(0,1,1,1));
        });

    }

    public void createHierarchy(List<Component> roots, int currentStep, boolean isMasterCall) {

        if(alreadyCreated) return;

        for(var root: roots) {

            GridCell lastGridCell = null;
            if(contents.getSize() > 0) lastGridCell = contents.peekTail();

            if(root == this) continue;

            Logger.log("Hierarchy: creating element for "+ root.identifier + " at level: "+currentStep);

            var grid = createGridCellForUnit(root, createUnit(root, currentStep, root.identifier), lastGridCell, currentStep);
            contents.pushTail(grid);
            addGridCell(grid);

            alreadyCreated = true;
//            return;

//            if(currentStep < 2) {
//                createHierarchy(root.children, currentStep + 1, false);
//            }
//            else {
//                grid = createGridCellForUnit(root, createUnit(root, currentStep + 1, "..."), grid, currentStep + 1);
//                contents.pushTail(grid);
//                addGridCell(grid);
//            }
        }

        if(isMasterCall) {
            alreadyCreated = true;

            var lastGridCell = contents.peekTail();

            var bottomMostCenter = new Boundary(game, this, identifier+"_hw_bottommostCenter", Boundary.BoundaryOrient.Vertical, false, configurator);

            bottomMostCenter.isContainerVisible = true;
            bottomMostCenter.minHeight = 30;
//            bottomMostCenter.maxHeight = 200;
            bottomMostCenter.width = 6;
            bottomMostCenter.setColor(new Vector(0,1,0,0.6f));

            bottomMostCenter.addOnResizeAction((c,i,t) -> c.pos.setDataElement(0, lastGridCell.attachedComp.children.get(0).getPos().geti(0) + 200));

            addBoundary(bottomMostCenter);
            lastGridCell.bottom.addConnectedBoundary(bottomMostCenter, 0, -1);
            bottom.addConnectedBoundary(bottomMostCenter, 1, -1);


        }
//        return this;
    }

    public Component createUnit(Component comp, int step, String textString) {
        var rec = new Rectangle(game, this, comp.identifier+"_hw")
                .setColor(new Vector(0.5f, 0.4f, 0.9f, 0.1f))
                .setContainerVisibility(false);

        var text = new Text(game, rec, fontTexture, comp.identifier+"_hw_text");
        text.setText(textString);
        text.addOnResizeAction(new PosXLeftAttachPix(initOffsetX + step*stepOffset));
        text.attachSelfToParent(rec);

        rec.attachSelfToParent(this);

        return rec;
    }

    public GridCell createGridCellForUnit(Component initComp, Component unit, GridCell lastGridCell, int step) {
        var g = new GridCell(game, this, initComp.identifier+"_hw_gc");
        g.attachedComp = unit;
        g.right = right;
        g.left = left;

        if(lastGridCell == null) {
            g.top = top;

        }
        else {
            g.top = lastGridCell.bottom;
        }

        var bottom_bound = new Boundary(game, this, initComp.identifier+"_hw_bottom", Boundary.BoundaryOrient.Horizontal, true, configurator);
        bottom_bound.addInitAutomation((c,i,t) -> c.pos = g.top.pos.add(new Vector(0,gridHeight,0)));
        bottom_bound.height = 5;

//        bottom_bound.isContainerVisible = false;

        g.bottom = bottom_bound;

        var centre_bound = new Boundary(game, this, initComp.identifier+"_hw_center", Boundary.BoundaryOrient.Vertical, true, configurator);

        centre_bound.addInitAutomation((c,i,t) -> {
            c.height = (int) ((g.bottom.pos.get(1)-g.bottom.height/2f) - (g.top.pos.get(1)+g.top.height/2f));
            c.pos = new Vector(0 ,(g.top.pos.get(1)+g.top.height/2f) + c.height/2f, 0);
        });

        centre_bound.isContainerVisible = true;
        centre_bound.minHeight = 50;
        centre_bound.maxHeight = 200;
        centre_bound.width = 6;
        centre_bound.setColor(new Vector(1,0,0,0.6f));

        centre_bound.addOnResizeAction((c,i,t) -> c.pos.setDataElement(0, g.attachedComp.children.get(0).getPos().geti(0) + 50));
        addBoundary(bottom_bound).addBoundary(centre_bound);

        g.top.addConnectedBoundary(centre_bound, 0, -1);
        g.bottom.addConnectedBoundary(centre_bound, 1, -1);

        g.left.addConnectedBoundary(g.top, 1, -1);
        g.left.addConnectedBoundary(g.bottom, 1, -1);
        g.right.addConnectedBoundary(g.top, 0, -1);
        g.right.addConnectedBoundary(g.bottom, 0, -1);

        return g;
    }

}
