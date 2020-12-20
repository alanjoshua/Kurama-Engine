package ENED_Simulation;

public class Main {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale","1"); //Important to make fullscreen and engine.GUI scale properly irrespective of windows scaling
        System.setProperty("java.awt.headless", "true"); //To ensure fonttexture loading works properly in OSX

        Simulation simulation = new Simulation("ENED Simulation");
        simulation.start();
    }

}
