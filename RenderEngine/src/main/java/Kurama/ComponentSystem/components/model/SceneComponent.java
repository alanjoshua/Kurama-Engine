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

    // An automation to run after all transformational changes occur. This is more of a convenience feature since
    // most SceneComponents require the final update of position, velocity and acceleration. This Automation would be
    // run after animations finish running.

    public SceneComponent(Game game, Kurama.ComponentSystem.components.Component parent, String identifier) {
        super(game, parent, identifier);
        this.identifier = identifier;
        this.parent = parent;
        this.game = game;
        finalAutomationsBeforePosConfirm.add(new DefaultPosVelAccelUpdate());
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

}

