package Kurama.ComponentSystem.components;

import Kurama.Annotations.Property;
import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.automations.Automation;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Texture;
import Kurama.game.Game;
import Kurama.inputs.Input;

import java.util.ArrayList;
import java.util.List;

public abstract class Component {

    public @Property Vector pos = new Vector(new float[]{0,0,0});
    public @Property Vector scale = new Vector(1,1,1);
    public @Property Quaternion orientation = Quaternion.getAxisAsQuat(1,0,0,0);
    public @Property int width;
    public @Property int height;
    public @Property Matrix objectToWorldMatrix = Matrix.getIdentityMatrix(4);
    public @Property Matrix objectToWorldNoScaleMatrix = Matrix.getIdentityMatrix(4);

    protected @Property Vector previousPos;
    protected @Property Quaternion previousOrient;
    protected @Property int previousWidth;
    protected @Property int previousHeight;
    protected @Property Vector previousScale;

    public @Property Vector color = new Vector(0,0,0,1);
    public @Property Vector overlayColor = null;
    public @Property Texture texture = null;
    public float alphaMask = 1;

    public String identifier;
    public boolean isContainerVisible = true;
    public boolean shouldTickRenderGroup = true;
    public boolean shouldTriggerOnClick = false;
    public boolean shouldTriggerOnMouseOver = false;
    public boolean shouldTriggerOnMouseLeave = false;

    public boolean isResizedOrMoved = true;
    protected boolean shouldResizeChildren = false;
    public boolean isClicked = false;
    public boolean isClickDragged = false;
    public boolean isClickedOutside = false;
    public boolean isMouseOver = false;
    public boolean isMouseLeft = false;
    protected boolean previousIsMouseOver = false;
    public boolean isKeyInputFocused = false;
    protected boolean shouldForceCheckKeyInputFocusUpdate = false;

    public boolean alwaysUpdateTransforms = false;

    public Component parent;
    public Game game;
    public List<Component> children = new ArrayList<>();

    // Constraints are updated only when components are resized.
    // WARNING: ALWAYS ADD SIZE CONSTRAINTS BEFORE POSITIONAL CONSTRAINTS
    public List<Automation> onResizeAutomations = new ArrayList<>();
    public List<Automation> globalResizeAutomations = new ArrayList<>();
    protected boolean doesChildHaveInputAccess = false;
    public boolean allowParentComponentsInputAccess = false;
    public boolean allowMultipleComponentsClickTriggers = false;

    protected boolean isFirstRun = true;
    public List<Kurama.ComponentSystem.automations.Automation> initAutomations = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> automations = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onClickActions = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onClickedOutsideActions = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onClickDraggedActions = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onClickDragEndActions = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onMouseOverActions = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onMouseLeaveActions = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onKeyInputFocused = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onKeyInputFocusedInit = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> onKeyInputFocusLossInit = new ArrayList<>();

    public List<Animation> animations = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> automationsBeforeChildTick = new ArrayList<>();
    public List<Kurama.ComponentSystem.automations.Automation> automationsAfterChildTick = new ArrayList<>();

    public Component(Game game, Component parent, String identifier) {
        this.identifier = identifier;
        this.parent = parent;
        this.game = game;
    }

    public Matrix getOrthoProjection() {
        return Matrix.buildOrtho2D(0, getWidth(), getHeight(), 0);
    }

    public Component findComponent(String id) {
        if(this.identifier.equals(id))
            return this;

        for(var child: children) {
            var res = child.findComponent(id);
            if(res != null)
                return res;
        }
        return null;
    }

    public Component getRoot() {
        if(this.parent == null)
            return this;
        this.parent.getRoot();
        return null; // this would theoretically never happen
    }

    public Component addOnClickAction(Kurama.ComponentSystem.automations.Automation action) {
        onClickActions.add(action);
        shouldTriggerOnClick = true;
        return this;
    }

    public Component addChild(Component child) {
        children.add(child);
        child.parent = this;
        return this;
    }

    public Component addOnClickOutsideAction(Kurama.ComponentSystem.automations.Automation action) {
        onClickedOutsideActions.add(action);
        shouldTriggerOnClick = true;
        return this;
    }

    public Component addOnKeyInputFocusedAction(Kurama.ComponentSystem.automations.Automation action) {
        onKeyInputFocused.add(action);
        return this;
    }

    public Component addOnClickDraggedAction(Kurama.ComponentSystem.automations.Automation action) {
        onClickDraggedActions.add(action);
        shouldTriggerOnClick = true;
        return this;
    }

    public Component addOnClickDragEndedAction(Kurama.ComponentSystem.automations.Automation action) {
        onClickDragEndActions.add(action);
        shouldTriggerOnClick = true;
        return this;
    }

    public Component addOnKeyInputFocusedInitAction(Kurama.ComponentSystem.automations.Automation action) {
        onKeyInputFocusedInit.add(action);
        return this;
    }

    public Component addOnKeyInputFocusLossInitAction(Kurama.ComponentSystem.automations.Automation action) {
        onKeyInputFocusLossInit.add(action);
        return this;
    }

    public Component addOnMouseOvertAction(Kurama.ComponentSystem.automations.Automation action) {
        this.shouldTriggerOnMouseOver = true;
        onMouseOverActions.add(action);
        return this;
    }

    public Component addOnMouseLeftAction(Kurama.ComponentSystem.automations.Automation action) {
        this.shouldTriggerOnMouseLeave = true;
        onMouseLeaveActions.add(action);
        return this;
    }

    public Component addAnimation(Animation animation) {
        animations.add(animation);
        return this;
    }

    public Component addOnResizeAction(Automation constraint) {
        this.onResizeAutomations.add(constraint);
        return this;
    }
    public Component addOnResizeAction(int index, Automation constraint) {
        this.onResizeAutomations.add(index, constraint);
        return this;
    }

    public Component addAutomation(Kurama.ComponentSystem.automations.Automation automation) {
        this.automations.add(automation);
        return this;
    }

    public Component addAutomation(int index, Kurama.ComponentSystem.automations.Automation automation) {
        this.automations.add(index, automation);
        return this;
    }

    public Component addInitAutomation(int index, Kurama.ComponentSystem.automations.Automation automation) {
        this.initAutomations.add(index, automation);
        return this;
    }

    public Component addInitAutomation(Kurama.ComponentSystem.automations.Automation automation) {
        this.initAutomations.add(automation);
        return this;
    }

    public Component addAutomationBeforeChildTick(Kurama.ComponentSystem.automations.Automation automation) {
        this.automationsBeforeChildTick.add(automation);
        return this;
    }

    public Component addAutomationBeforeChildTick(int index, Kurama.ComponentSystem.automations.Automation automation) {
        this.automationsBeforeChildTick.add(index, automation);
        return this;
    }

    public Component addAutomationAfterChildTick(Kurama.ComponentSystem.automations.Automation automation) {
        this.automationsBeforeChildTick.add(automation);
        return this;
    }

    public Component addAutomationAfterChildTick(int index, Kurama.ComponentSystem.automations.Automation automation) {
        this.automationsBeforeChildTick.add(index, automation);
        return this;
    }

    public Component setColor(Vector color) {
        this.color = color;
        return this;
    }

    public Component setWidth(int width) {
        this.width = width;
//        this.isResizedOrMoved = true;
        return this;
    }

    public Component setHeight(int height) {
        this.height = height;
//        this.isResizedOrMoved = true;
        return this;
    }

    public Component setAlphaMask(float alphaMask) {
        this.alphaMask = alphaMask;
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

    public Vector getScale() {
        return scale;
    }

    public Component setScale(Vector scale) {
        this.scale = scale;
        return this;
    }

    public Vector getTranslatedPos() {
        return objectToWorldMatrix.getColumn(3).removeDimensionFromVec(3);
    }

    public Component setShouldTriggerOnClick(boolean isClickable) {
        this.shouldTriggerOnClick = isClickable;
        return this;
    }

    public Component setKeyInputFocused(boolean isKeyboardInputFocusable) {
        shouldForceCheckKeyInputFocusUpdate = !(isKeyInputFocused == isKeyboardInputFocusable);
        this.isKeyInputFocused = isKeyboardInputFocusable;
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

    public Component setShouldAlwaysUpdateTransforms(boolean should) {
        this.alwaysUpdateTransforms = should;
        return this;
    }

    public Component setShouldTickRenderGroup(boolean shouldRender) {
        this.shouldTickRenderGroup = shouldRender;
        return this;
    }

    public void setupTransformationMatrices() {

        if(parent==null) {
            setPos(new Vector(new float[]{getWidth() /2f, getHeight() /2f, 0}));
        }

        Matrix rotationMatrix = getOrientation().getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(new Vector(getWidth(), getHeight(), 1));
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        objectToWorldNoScaleMatrix = rotationMatrix.addColumn(getPos());
        objectToWorldNoScaleMatrix = objectToWorldNoScaleMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        objectToWorldMatrix = rotScalMatrix.addColumn(getPos());
        objectToWorldMatrix = objectToWorldMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));


        if(parent!=null) {
            objectToWorldNoScaleMatrix = parent.objectToWorldNoScaleMatrix.matMul(objectToWorldNoScaleMatrix);
            objectToWorldMatrix = parent.objectToWorldNoScaleMatrix.matMul(objectToWorldMatrix);
        }

    }

    public Matrix getObjectToWorldMatrix() {
        return objectToWorldMatrix;
    }

    public void onClick(Input input, float timeDelta) {
        for(var actions: onClickActions) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onClickedOutside(Input input, float timeDelta) {
        for(var actions: onClickedOutsideActions) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onClickDragged(Input input, float timeDelta) {
        for(var actions: onClickDraggedActions) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onClickDragEnd(Input input, float timeDelta) {
        for(var actions: onClickDragEndActions) {
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

    public void onKeyFocus(Input input, float timeDelta) {
        for(var actions: onKeyInputFocused) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onKeyFocusInit(Input input, float timeDelta) {
        for(var actions: onKeyInputFocusedInit) {
            actions.run(this, input, timeDelta);
        }
    }

    public void onKeyFocusLossInit(Input input, float timeDelta) {
        for(var actions: onKeyInputFocusLossInit) {
            actions.run(this, input, timeDelta);
        }
    }

    public Component attachSelfToParent(Component parent) {
        parent.addChild(this);
        this.parent = parent;
        return this;
    }

    // A default component can't have mouse over
    public boolean isMouseOverComponent(Input input) {
        return false;
    }

    public boolean isClicked(Input input, boolean isMouseOver) {

        if(input == null) {
            return false;
        }

        if(input != null && input.isClickLocked() != null && input.isClickLocked() != this) {return false;}

        if(shouldTriggerOnClick && input.isCursorEnabled && input.isLeftMouseButtonPressed) {
            return isMouseOver;
        }
        return false;
    }

    public boolean isClickDragged(Input input, boolean isClicked, boolean isClickedOutside, boolean previousIsClickDragged) {

        if(isClicked) return true;
        else if(previousIsClickDragged && input.isLeftMouseButtonPressed()) return true;
        else return false;
    }

    public boolean isClickedOutside(Input input, boolean isMouseOver) {
        if(input == null) {
            return false;
        }

        if(shouldTriggerOnClick && input.isLeftMouseButtonPressed) {
            return !isMouseOver;
        }
        return false;
    }

    public boolean isMouseLeft(Input input, boolean isMouseOver) {
        if(previousIsMouseOver && !isMouseOver) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean resizeReposition(Vector newPos, int newWidth, int newHeight) {

        this.pos = newPos;
        this.width = newWidth;
        this.height = newHeight;

        return true;
    }

    public void tick(List<Automation> globalResizeAutomations, Input input, float timeDelta, boolean parentResized) {

        if(!shouldTickRenderGroup) {
            if(parentResized) this.isResizedOrMoved = true;
            return;
        }

        isResizedOrMoved = isResizedOrMoved || parentResized;

        if(isFirstRun) {

            for(var automation: onResizeAutomations) {
                automation.run(this, input, timeDelta);
            }

            if (globalResizeAutomations != null) {
                for(var automation: globalResizeAutomations) {
                    automation.run(this, input, timeDelta);
                }
            }

            for(var automation: initAutomations) {
                automation.run(this, input, timeDelta);
            }

            isResizedOrMoved = true;

            previousPos = getPos();
            previousHeight = getHeight();
            previousWidth = getWidth();
            previousOrient = getOrientation();
            previousScale = getScale();
        }

        for(var automation: automations) {
            automation.run(this, input, timeDelta);
        }

        List<Animation> toBeRemoved = new ArrayList<>();
        for(var anim: animations) {
            anim.run(this, input, timeDelta);
            if(anim.hasAnimEnded) {
                toBeRemoved.add(anim);
            }
        }
        animations.removeAll(toBeRemoved);

        // Mostly intended to be used internally, and not by user
        for(var automation: automationsBeforeChildTick) {
            automation.run(this, input, timeDelta);
        }

        isResizedOrMoved = isResizedOrMoved(isResizedOrMoved);
        if(isResizedOrMoved) {
//            Logger.log("resizing: "+identifier);

            for(var automation: onResizeAutomations) {
                automation.run(this, input, timeDelta);
            }

            if (globalResizeAutomations != null) {
                for(var automation: globalResizeAutomations) {
                    automation.run(this, input, timeDelta);
                }
            }
            setupTransformationMatrices();

            this.isResizedOrMoved = false;
            shouldResizeChildren = true;
        }
        previousPos = getPos();
        previousHeight = getHeight();
        previousWidth = getWidth();
        previousOrient = getOrientation();
        previousScale = getScale();

        boolean isChildMouseOver = false, isChildClicked = false;
        for(var child: children) {
            child.tick(this.globalResizeAutomations, input, timeDelta, shouldResizeChildren);
            isChildMouseOver = isChildMouseOver || child.isClicked;
            isChildClicked = isChildClicked || child.isMouseOver;
        }

        // Mostly intended to be used internally, and not by user
        for(var automation: automationsAfterChildTick) {
            automation.run(this, input, timeDelta);
        }

        shouldResizeChildren = false;
        isClicked = false; // Reset before processing inputs for current frame
        isMouseOver = false;
        isMouseLeft = false;
        isClickedOutside = false;
        boolean previousKeyFocus = isKeyInputFocused;
        boolean previousClickDragged = isClickDragged;

        if(!doesChildHaveInputAccess) {

            // If the child has triggered mouseover or click, then it means the parent also should trigger them. Don't need to check again

            // The below does the opposite; Checks only when we are sure none of the children triggered those events
            if(!isChildMouseOver && !isChildClicked) {
                isMouseOver = isMouseOverComponent(input);
                isClicked = isClicked(input, isMouseOver);
            }
            else {
                if(!shouldTriggerOnClick) {
                    isClicked = false;
                }
            }

            isMouseLeft = isMouseLeft(input, isMouseOver);
            isClickDragged = isClickDragged(input, isClicked, isClickedOutside, isClickDragged);
        }

        isClickedOutside = isClickedOutside(input, isMouseOver);

        if(!doesChildHaveInputAccess) {
            if(isClicked | isClickDragged | isMouseOver) {
                if(!allowParentComponentsInputAccess && parent != null) {
                    parent.doesChildHaveInputAccess = true;
                }
                if((isClicked || isClickDragged) && !allowMultipleComponentsClickTriggers) {
                    input.requestClickInputLock(this);
                }
            }
        }

        if(isClicked) {
            onClick(input, timeDelta);
        }

        if(isMouseOver) {
            onMouseOver(input, timeDelta);
            previousIsMouseOver = true;
        }

        if(isMouseLeft) {
            onMouseLeave(input, timeDelta);
            previousIsMouseOver = false;
        }

        if(isClickedOutside) {
            onClickedOutside(input, timeDelta);
        }

        // Called whenever component has keyboard focus
        if(isKeyInputFocused) {
            onKeyFocus(input, timeDelta);
        }

        if(isClickDragged) {

            onClickDragged(input, timeDelta);
        }

        if(previousClickDragged != isClickDragged) {
            onClickDragEnd(input, timeDelta);
        }

        // Called only once right after keyboard focus is lost or gained
        if(previousKeyFocus != isKeyInputFocused || shouldForceCheckKeyInputFocusUpdate) {
            if(!isKeyInputFocused) {
                onKeyFocusLossInit(input, timeDelta);
            }
            else {
                onKeyFocusInit(input, timeDelta);
            }
            shouldForceCheckKeyInputFocusUpdate = false;
        }

        doesChildHaveInputAccess = false;
        isFirstRun = false;

    }

    protected boolean isResizedOrMoved(boolean shouldUpdateSize) {
        if(shouldUpdateSize) {
            return true;
        }

        if(!previousPos.equals(getPos()) || getWidth() != previousWidth || getHeight() != previousHeight
                || !previousOrient.equals(getOrientation())) {
            return true;
        }
        else {
            return false;
        }
    }

    // Should probably be overwritten, or more control should be specified
    public boolean isValidLocation(Vector newPos, int width, int height) {
            return true;
    }

    public Vector getPos() {
        return pos;
    }

    public void setPos(Vector pos) {
        this.pos = pos;
//        this.isResizedOrMoved = true;
    }

    public Quaternion getOrientation() {
        return orientation;
    }

    public void setOrientation(Quaternion orientation) {
        this.orientation = orientation;
//        this.isResizedOrMoved = true;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
