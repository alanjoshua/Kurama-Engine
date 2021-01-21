package Kurama.GUI.components;

import Kurama.GUI.automations.Automation;
import Kurama.GUI.constraints.Constraint;
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
    public Matrix objectToWorldMatrix = Matrix.getIdentityMatrix(4);
    public Matrix objectToWorldNoScaleMatrix = Matrix.getIdentityMatrix(4);

    public Vector color = new Vector(0,0,0,1);
    public Vector overlayColor = null;
    public Texture texture = null;

    public String identifier;
    public boolean isContainerVisible = true;
    public boolean shouldRenderGroup = true;
    public boolean shouldTriggerOnClick = false;
    public boolean shouldTriggerOnMouseOver = false;
    public boolean shouldTriggerOnMouseLeave = false;

    public boolean isClicked = false;
    public boolean currentIsMouseOver = false;
    public boolean isMouseLeft = false;
    protected boolean previousIsMouseOver = false;

    public Component parent;
    public List<Component> children = new ArrayList<>();
    public List<Constraint> constraints = new ArrayList<>();
    public List<Constraint> globalChildrenConstraints = new ArrayList<>();
    public List<Automation> automations = new ArrayList<>();
//    public List<Automation> nonTransformationalAutomations = new ArrayList<>();  //Automations that promise not to make any positional changes.
    public List<Automation> onClickActions = new ArrayList<>();
    public List<Automation> onMouseOverActions = new ArrayList<>();
    public List<Automation> onMouseLeaveActions = new ArrayList<>();

    public Component(Component parent, String identifier) {
        this.identifier = identifier;
        this.parent = parent;
    }

    public Matrix getOrthoProjection() {
        return Matrix.buildOrtho2D(0, width, height, 0);
    }

    public Component addOnClickAction(Automation action) {
        onClickActions.add(action);
        shouldTriggerOnClick = true;
        return this;
    }

    public Component addOnMouseOvertAction(Automation action) {
        this.shouldTriggerOnMouseOver = true;
        onMouseOverActions.add(action);
        return this;
    }

    public Component addOnMouseLeftAction(Automation action) {
        this.shouldTriggerOnMouseLeave = true;
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

    public Component setOverlayColor(Vector color) {
        this.overlayColor = color;
        return this;
    }

    public Component setShouldTriggerOnClick(boolean isClickable) {
        this.shouldTriggerOnClick = isClickable;
        return this;
    }

    public Component setShouldTriggerOnMouseOver(boolean shouldTriggerOnMouseOver) {
        this.shouldTriggerOnMouseOver = shouldTriggerOnMouseOver;
        return this;
    }

    public Component setShouldTriggerOnMouseLeave(boolean shouldTriggerOnMouseLeave) {
        this.shouldTriggerOnMouseLeave = shouldTriggerOnMouseLeave;
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

    public void setupTransformationMatrices() {
        Matrix rotationMatrix = orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(new Vector(width, height, 1));
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        objectToWorldNoScaleMatrix = rotationMatrix.addColumn(pos);
        objectToWorldNoScaleMatrix = objectToWorldNoScaleMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        objectToWorldMatrix = rotScalMatrix.addColumn(pos);
        objectToWorldMatrix = objectToWorldMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));


        if(parent!=null) {
            objectToWorldNoScaleMatrix = parent.objectToWorldNoScaleMatrix.matMul(objectToWorldNoScaleMatrix);
            objectToWorldMatrix = parent.objectToWorldNoScaleMatrix.matMul(objectToWorldMatrix);
        }
        else {
            pos = new Vector(new float[]{width/2f, height/2f, 0});
        }

    }

    public void onClick(Input input, float timeDelta) {
        for(var actions: onClickActions) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onMouseOver(Input input, float timeDelta) {
        for(var actions: onMouseOverActions) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onMouseLeave(Input input, float timeDelta) {
        for(var actions: onMouseLeaveActions) {
            actions.run(this, input, timeDelta);
        }
    }

    // A default component can't have mouse over
    public boolean isMouseOverComponent(Input input) {
        return false;
    }

    public boolean isClicked(Input input, boolean isMouseOver) {
        if(shouldTriggerOnClick && input != null && input.isCursorEnabled && input.isLeftMouseButtonPressed) {
            return isMouseOver;
        }
        return false;
    }

    public boolean isMouseLeft(Input input, boolean isMouseOver) {
        if(previousIsMouseOver && input.isCursorEnabled && !isMouseOver) {
            return true;
        }
        else if(!input.isCursorEnabled) {
            return true;
        }
        else {
            return false;
        }
    }

    public void tick(List<Constraint> parentGlobalConstraints, Input input, float timeDelta) {

        isClicked = false; // Reset before processing inputs for current frame
        currentIsMouseOver = false;
        isMouseLeft = false;

        currentIsMouseOver = isMouseOverComponent(input);  // This should always be first, since its result is used by isClicked
        isClicked = isClicked(input, currentIsMouseOver);
        isMouseLeft = isMouseLeft(input, currentIsMouseOver);

        for(var constraint: constraints) {
            constraint.solveConstraint(parent, this);
        }

        if(parentGlobalConstraints != null) {
            for (var globalConstraints : parentGlobalConstraints) {
                globalConstraints.solveConstraint(parent, this);
            }
        }

        for(var automation: automations) {
            automation.run(this, input, timeDelta);
        }

        setupTransformationMatrices();  // This finalised transformation matrices, and other positional information. The mouse events should not directly change the positional information in this tick cycle

        for(var child: children) {
            child.tick(globalChildrenConstraints, input, timeDelta);
        }

        if(isClicked) {
            onClick(input, timeDelta);
        }

        if(currentIsMouseOver) {
            onMouseOver(input, timeDelta);
            previousIsMouseOver = true;
        }

        if(isMouseLeft) {
            onMouseLeave(input, timeDelta);
            previousIsMouseOver = false;
        }

    }

}
