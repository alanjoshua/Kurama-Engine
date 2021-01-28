package Kurama.ComponentSystem.components.model;

import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.automations.DefaultPosUpdate;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.constraints.Constraint;
import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.inputs.Input;

import java.util.ArrayList;
import java.util.List;

public abstract class SceneComponent extends Component {

   public Vector scale = new Vector(1,1,1);
    public Vector velocity = new Vector(0,0,0);
    public Vector acceleration = new Vector(0,0,0);
   public Matrix worldToObject = Matrix.getIdentityMatrix(4);

    public boolean shouldSelfCastShadow = true;
    public boolean shouldRender = true;
    public boolean isInsideFrustum = true;
    public boolean shouldBeConsideredForFrustumCulling = true;

    // An automation to run after all transformational changes occur. This is more of a convenience feature since
    // most SceneComponents require the final update of position, velocity and acceleration. This Automation would be
    // run after animations finish running.
   public Automation finalComponentUpdate = new DefaultPosUpdate();

    public SceneComponent(Game game, Kurama.ComponentSystem.components.Component parent, String identifier) {
        super(game, parent, identifier);
        this.identifier = identifier;
        this.parent = parent;
        this.game = game;
    }

    public void setupTransformationMatrices() {
        Matrix rotationMatrix = orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(scale);
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        objectToWorldMatrix = rotScalMatrix.addColumn(pos);
        objectToWorldMatrix = objectToWorldMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        if(parent!=null) {
            objectToWorldMatrix = parent.objectToWorldMatrix.matMul(objectToWorldMatrix);
        }

        worldToObject = objectToWorldMatrix.getInverse();
    }

    public Matrix getWorldToObject() {
        return worldToObject;
    }

    public void tick(List<Constraint> parentGlobalConstraints, Input input, float timeDelta) {

        if(!shouldRenderGroup) {
            return;
        }

        isClicked = false; // Reset before processing inputs for current frame
        currentIsMouseOver = false;
        isMouseLeft = false;
        isClickedOutside = false;
        boolean previousKeyFocus = isKeyInputFocused;

        currentIsMouseOver = isMouseOverComponent(input);  // This should always be first, since its result is used by isClicked
        isClicked = isClicked(input, currentIsMouseOver);
        isMouseLeft = isMouseLeft(input, currentIsMouseOver);
        isClickedOutside = isClickedOutside(input, currentIsMouseOver);

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

        List<Animation> toBeRemoved = new ArrayList<>();
        for(var anim: animations) {
            anim.run(this, input, timeDelta);
            if(anim.hasAnimEnded) {
                toBeRemoved.add(anim);
            }
        }
        animations.removeAll(toBeRemoved);

        finalComponentUpdate.run(this, input, timeDelta);
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

        if(isClickedOutside) {
            onClickedOutside(input, timeDelta);
        }

        // Called whenever component has keyboard focus
        if(isKeyInputFocused) {
            onKeyFocus(input, timeDelta);
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

    }

}

