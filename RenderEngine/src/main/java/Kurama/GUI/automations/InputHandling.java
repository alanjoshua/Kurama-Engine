package Kurama.GUI.automations;

public abstract class InputHandling implements Automation {

    public float timeFromLastDelete =0;
    public float minDeleteTime=0.1f;

    public InputHandling() {

    }

    public InputHandling(float minDeleteTime) {
        this.minDeleteTime = minDeleteTime;
    }

}
