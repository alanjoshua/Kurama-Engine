package ENED_Simulation;

public class Main {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale","1"); //Important to make fullscreen and engine.GUI scale properly irrespective of windows scaling

        Simulation simulation = new Simulation("ENED Simulation");
        simulation.start();
    }

}
