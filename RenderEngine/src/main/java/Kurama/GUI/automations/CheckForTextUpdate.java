package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.GUI.components.Text;
import Kurama.inputs.Input;

public class CheckForTextUpdate implements Automation {

    public Text text;
    public CheckForTextUpdate(Text text) {
        this.text = text;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(text.shouldUpdate) {
            text.updateText();
        }
    }
}
