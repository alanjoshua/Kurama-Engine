package Kurama.particle;

import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.SceneComponent;
import Kurama.ComponentSystem.constraints.Constraint;
import Kurama.game.Game;
import Kurama.inputs.Input;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleGenerator extends SceneComponent {

    public List<Model> particles = new ArrayList<>();
    public Particle baseParticle = null;
    public boolean isInsideFrustum = true;

    public ParticleGenerator(Game game, Component parent, String id) {
        super(game, parent, id);
    }

    public abstract void cleanup();

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

        for(var part: particles) {
            part.tick(globalChildrenConstraints, input, timeDelta);
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
