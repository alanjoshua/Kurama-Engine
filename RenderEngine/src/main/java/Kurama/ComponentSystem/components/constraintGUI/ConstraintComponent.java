package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.PosXLeftAttachPercent;
import Kurama.ComponentSystem.automations.PosXRightAttachPercent;
import Kurama.ComponentSystem.automations.PosYBottomAttachPercent;
import Kurama.ComponentSystem.automations.PosYTopAttachPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public List<Boundary> boundaries = new ArrayList<>();

    public ConstraintComponent(Game game, Component parent, Vector radii, String identifier) {
        super(game, parent, radii, identifier);
        init();
    }

    public ConstraintComponent(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        init();
    }

    public ConstraintComponent addBoundary(Boundary bound) {
        boundaries.add(bound);
        this.children.add(bound);
        return this;
    }

    public void init() {
        // This is probably kinda temp

        var l = new VerticalBoundary(this.game, this, "leftB");
        var t = new HorizontalBoundary(this.game, this, "topB");

        var r = new VerticalBoundary(this.game, this, "rightB");
        var b = new HorizontalBoundary(this.game, this, "bottomB");

        r.addInitAutomation(new PosXRightAttachPercent(0f));
        b.addInitAutomation(new PosYBottomAttachPercent(0f));
        l.addInitAutomation(new PosXLeftAttachPercent(0f));
        t.addInitAutomation(new PosYTopAttachPercent(0f));

        addBoundary(l).addBoundary(r).addBoundary(t).addBoundary(b);

        l.addConnectedBoundary(t, 1, 0);
        l.addConnectedBoundary(b, 1, 0);
        t.addConnectedBoundary(r, 1, 0);
        b.addConnectedBoundary(r, 1, 0);
    }

}
