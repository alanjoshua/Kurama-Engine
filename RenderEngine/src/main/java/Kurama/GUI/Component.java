package Kurama.GUI;

import Kurama.GUI.automations.Automation;
import Kurama.GUI.constraints.Constraint;
import Kurama.GUI.inputHandling.InputAction;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Texture;
import Kurama.inputs.Input;

import java.util.ArrayList;
import java.util.List;

public abstract class Component {

    public Vector pos = new Vector(new float[]{0,0,0});
    public Quaternion orientation = Quaternion.getAxisAsQuat(1,0,0,0);
    public int width;
    public int height;

    public Vector color = new Vector(0,0,0,1);
    public boolean isMouseOver = false;
    public Vector overlayColor = null;
    public Texture texture = null;

    public String identifier;
    public boolean isContainerVisible = true;
    public boolean shouldRenderGroup = true;

    public Component parent;
    public List<Component> children = new ArrayList<>();
    public List<Constraint> constraints = new ArrayList<>();
    public List<Constraint> globalChildrenConstraints = new ArrayList<>();
    public List<Automation> automations = new ArrayList<>();
    public List<InputAction> onClickActions = new ArrayList<>();
    public List<InputAction> onMouseOverActions = new ArrayList<>();
    public List<InputAction> onMouseLeaveActions = new ArrayList<>();

    public Component(Component parent, String identifier) {
        this.identifier = identifier;
        this.parent = parent;
    }

    public Component addOnClickAction(InputAction action) {
        onClickActions.add(action);
        return this;
    }

    public Component addOnMouseOvertAction(InputAction action) {
        onMouseOverActions.add(action);
        return this;
    }

    public Component addOnMouseLeftAction(InputAction action) {
        onMouseLeaveActions.add(action);
        return this;
    }

    public Component addConstraint(Constraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    public Component addAutomation(Automation automation) {
        this.automations.add(automation);
        return this;
    }

    public Component setColor(Vector color) {
        this.color = color;
        return this;
    }

    public Component setTexture(Texture tex) {
        this.texture = tex;
        return this;
    }

    public Component setContainerVisibility(boolean isVisible) {
        this.isContainerVisible = isVisible;
        return this;
    }

    public Component setShouldRenderGroup(boolean shouldRender) {
        this.shouldRenderGroup = shouldRender;
        return this;
    }

    public Matrix getObjectToWorldMatrix() {

        Matrix rotationMatrix = this.orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(new Vector(width, height, 1));
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        Matrix transformationMatrix = rotScalMatrix.addColumn(pos.add(new Vector(width/2, height/2, 0)));
        transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        return transformationMatrix;
    }

    public void onClick(Input input) {
        for(var actions: onClickActions) {
            actions.run(this, input);
        }
    }

    public void onMouseOver(Input input) {
        isMouseOver = true;
        for(var actions: onMouseOverActions) {
            actions.run(this, input);
        }
    }

    public void onMouseLeave(Input input) {
        isMouseOver = false;
        for(var actions: onMouseLeaveActions) {
            actions.run(this, input);
        }
    }

    public boolean isMouseOverComponent(Input input) {
        if(input != null && input.isCursorEnabled) {
            var mp = input.getPos();

            return
                    mp.get(0) >= pos.get(0)
                            && mp.get(0) <= (pos.get(0) + width)
                            && mp.get(1) >= pos.get(1)
                            && mp.get(1) <= (pos.get(1)  + height);
        }
        else {
            return false;
        }
    }

    public boolean isClicked(Input input) {
        if(input != null && input.isCursorEnabled && input.isLeftMouseButtonPressed) {
            return isMouseOverComponent(input);
        }
        return false;
    }

    public boolean isMouseLeft(Input input) {
        if(isMouseOver && input.isCursorEnabled && !isMouseOverComponent(input)) {
            return true;
        }
        else if(!input.isCursorEnabled) {
            return true;
        }
        else {
            return false;
        }
    }

    public void tick(List<Constraint> parentGlobalConstraints, Input input) {

        if(this instanceof MasterWindow) {
            input = ((MasterWindow) this).input;
        }

        for(var automation: automations) {
            automation.runAutomation(this);
        }

        for(var constraint: constraints) {
            constraint.solveConstraint(parent, this);
        }

        if(parentGlobalConstraints != null) {
            for (var globalConstraints : parentGlobalConstraints) {
                globalConstraints.solveConstraint(parent, this);
            }
        }

        if(isClicked(input)) {
            onClick(input);
        }

        if(isMouseOverComponent(input)) {
            onMouseOver(input);
        }

        if(isMouseLeft(input)) {
            onMouseLeave(input);
        }

        for(var child: children) {
            child.tick(globalChildrenConstraints, input);
        }

    }

}
