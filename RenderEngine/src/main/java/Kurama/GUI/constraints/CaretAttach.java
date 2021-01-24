package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;
import Kurama.GUI.components.Text;
import Kurama.Math.Vector;

public class CaretAttach implements Constraint {

    public Text text;

    public CaretAttach(Text text) {
        this.text = text;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {

        if(text.children.size() > 0) {
            Component lastChar = text.children.get(text.children.size() - 1);
            current.pos = lastChar.pos.add(new Vector(lastChar.width, 0, 0));
        }
        else {
            current.pos = text.pos.getCopy();
        }

    }
}
