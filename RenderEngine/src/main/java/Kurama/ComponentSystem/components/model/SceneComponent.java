package Kurama.ComponentSystem.components.model;

import Kurama.ComponentSystem.automations.DefaultPosVelAccelUpdate;
import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.game.Game;

public class SceneComponent extends Component {

   public Vector scale = new Vector(1,1,1);
    public Vector velocity = new Vector(0,0,0);
    public Vector acceleration = new Vector(0,0,0);
   public Matrix worldToObject = Matrix.getIdentityMatrix(4);

    public boolean shouldSelfCastShadow = true;
    public boolean shouldRender = true;
    public boolean isInsideFrustum = true;
    public boolean shouldBeConsideredForFrustumCulling = true;

    public boolean shouldRespectParentScaling = true; // If false, it doesn't take parent's scaling into consideration when calculating transformation matrices

    // An automation to run after all transformational changes occur. This is more of a convenience feature since
    // most SceneComponents require the final update of position, velocity and acceleration. This Automation would be
    // run after animations finish running.

    public SceneComponent(Game game, Kurama.ComponentSystem.components.Component parent, String identifier) {
        super(game, parent, identifier);
        this.identifier = identifier;
        this.parent = parent;
        this.game = game;
        automationsBeforeUpdatingTransforms.add(new DefaultPosVelAccelUpdate());
    }

    @Override
    public void setupTransformationMatrices() {

        Matrix rotationMatrix = orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(scale);
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);
//
        objectToWorldMatrix = rotScalMatrix.addColumn(pos).addRow(new Vector(new float[]{0, 0, 0, 1}));

        // This works different from the equivalent matrix in Component. This one only excludes the current scaling from calculation
        objectToWorldNoScaleMatrix = rotationMatrix.addColumn(pos).addRow(new Vector(new float[]{0, 0, 0, 1}));

        if(parent!=null) {
            objectToWorldNoScaleMatrix = parent.objectToWorldMatrix.matMul(objectToWorldNoScaleMatrix);

            if(shouldRespectParentScaling) {
                objectToWorldMatrix = parent.objectToWorldMatrix.matMul(objectToWorldMatrix);
            }
            else {
                objectToWorldMatrix = parent.objectToWorldNoScaleMatrix.matMul(objectToWorldMatrix);
            }
        }

        worldToObject = objectToWorldMatrix.getInverse();

    }
//
    public Matrix getWorldToObject() {
        return worldToObject;
    }

//    @Override
//    public void tick(List<Automation> parentGlobalConstraints, Input input, float timeDelta) {
//
//        if(!shouldRenderGroup) {
//            return;
//        }
//
//        if(isFirstRun) {
//            initAutomations.forEach(a -> a.run(this, input, timeDelta));
//            isFirstRun = false;
//            isResizedOrMoved = true;
//        }
//
//        isClicked = false; // Reset before processing inputs for current frame
//        currentIsMouseOver = false;
//        isMouseLeft = false;
//        isClickedOutside = false;
//        boolean previousKeyFocus = isKeyInputFocused;
//        boolean previousClickDragged = isClickDragged;
//
////        Vector previousPos = pos.getCopy();
////        int width = this.width;
////        int height = this.height;
//
//        currentIsMouseOver = isMouseOverComponent(input);  // This should always be first, since its result is used by isClicked
//        isClicked = isClicked(input, currentIsMouseOver);
//        isMouseLeft = isMouseLeft(input, currentIsMouseOver);
//        isClickedOutside = isClickedOutside(input, currentIsMouseOver);
//        isClickDragged = isClickDragged(input, isClicked, isClickedOutside, isClickDragged);
//
//        // Constraints are updated only when components are resized.
//        // WARNING: ALWAYS ADD SIZE CONSTRAINTS BEFORE POSITIONAL CONSTRAINTS
////        if(this.isResizedOrMoved || (parent != null && parent.isResizedOrMoved)) {
////            Logger.log("resize called: "+identifier);
////            for (var constraint : constraints) {
////                constraint.run(this, input, timeDelta);
////            }
////
////            if (parentGlobalConstraints != null) {
////                for (var globalConstraints : parentGlobalConstraints) {
////                    globalConstraints.run(this, input, timeDelta);
////                }
////            }
////        }
//
//        for(var automation: automations) {
//            automation.run(this, input, timeDelta);
//        }
//
//        List<Animation> toBeRemoved = new ArrayList<>();
//        for(var anim: animations) {
//            anim.run(this, input, timeDelta);
//            if(anim.hasAnimEnded) {
//                toBeRemoved.add(anim);
//            }
//        }
//        animations.removeAll(toBeRemoved);
//
//        finalAutomationsBeforePosConfirm.forEach(a -> a.run(this, input, timeDelta));
//        setupTransformationMatrices();  // This finalised transformation matrices, and other positional information. The mouse events should not directly change the positional information in this tick cycle
//
////        if(previousPos.sub(pos).sumSquared() != 0 || width != this.width | height != this.height) {
////            this.isResizedOrMoved = true;
////        }
////        if(this.isResizedOrMoved) {
////            Logger.log("resized: "+identifier);
////        }
//
//        for(var child: children) {
//            child.tick(globalChildrenConstraints, input, timeDelta);
//        }
//
//        automationsAfterChildTick.forEach(a -> a.run(this, input, timeDelta));
//
//        if(isClicked) {
//            onClick(input, timeDelta);
//        }
//
//        if(currentIsMouseOver) {
//            onMouseOver(input, timeDelta);
//            previousIsMouseOver = true;
//        }
//
//        if(isMouseLeft) {
//            onMouseLeave(input, timeDelta);
//            previousIsMouseOver = false;
//        }
//
//        if(isClickedOutside) {
//            onClickedOutside(input, timeDelta);
//        }
//
//        // Called whenever component has keyboard focus
//        if(isKeyInputFocused) {
//            onKeyFocus(input, timeDelta);
//        }
//
//        if(isClickDragged) {
//            onClickDragged(input, timeDelta);
//        }
//
//        if(previousClickDragged != isClickDragged) {
//            onClickDragEnd(input, timeDelta);
//        }
//
//        // Called only once right after keyboard focus is lost or gained
//        if(previousKeyFocus != isKeyInputFocused || shouldForceCheckKeyInputFocusUpdate) {
//            if(!isKeyInputFocused) {
//                onKeyFocusLossInit(input, timeDelta);
//            }
//            else {
//                onKeyFocusInit(input, timeDelta);
//            }
//            shouldForceCheckKeyInputFocusUpdate = false;
//        }
//
//        isResizedOrMoved = false;
//
//    }

}

