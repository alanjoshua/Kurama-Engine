package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.GUI.components.Text;
import Kurama.game.Game;
import Kurama.inputs.Input;

public class DisplayFPS implements Automation {

    public Game game;
    public String prefix = "";

    public DisplayFPS(Game game, String prefix) {
        this.game = game;
        this.prefix = prefix;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        ((Text)current).setText(prefix+game.displayFPS);
    }
}
