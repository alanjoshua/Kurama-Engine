package Kurama.ComponentSystem.components.model;

import Kurama.ComponentSystem.automations.DefaultPosVelAccelUpdate;
import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.game.Game;

public class SceneComponent extends Component {

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
        addAutomationBeforeChildTick(new DefaultPosVelAccelUpdate());
    }

    @Override
    public void setupTransformationMatrices() {

        Matrix rotationMatrix = getOrientation().getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(getScale());
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);
//
        objectToWorldMatrix = rotScalMatrix.addColumn(getPos()).addRow(new Vector(new float[]{0, 0, 0, 1}));

        // This works different from the equivalent matrix in Component. This one only excludes the current scaling from calculation
        objectToWorldNoScaleMatrix = rotationMatrix.addColumn(getPos()).addRow(new Vector(new float[]{0, 0, 0, 1}));

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

    @Override
    protected boolean isResizedOrMoved(boolean shouldUpdateSize) {
        if(shouldUpdateSize) {
            return true;
        }

        if(!previousPos.equals(getPos()) || getWidth() != previousWidth || getHeight() != previousHeight
                || !previousOrient.equals(getOrientation())
                || !previousScale.equals(scale)) {
            return true;
        }
        else {
            return false;
        }
    }
//
    public Matrix getWorldToObject() {
        return worldToObject;
    }
}

