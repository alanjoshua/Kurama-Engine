package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Text;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class CaretAttach implements Automation {

    public Text text;

    public CaretAttach(Text text) {
        this.text = text;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {

        if(text.children.size() > 0) {
            Component lastChar = text.children.get(text.children.size() - 1);
            current.setPos(lastChar.getPos().add(new Vector(lastChar.getWidth(), 0, 0)));
        }
        else {
            current.setPos(text.getPos().getCopy());
        }

    }
}
