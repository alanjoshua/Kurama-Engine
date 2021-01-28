package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;
import Kurama.utils.Logger;

public class Log implements Automation {

    public String text;
    public Log(String text) {
        this.text = text;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        Logger.log(text);
    }
}