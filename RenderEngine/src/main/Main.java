package main;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale","1"); //Important to make fullscreen and GUI scale properly irrespective of windows scaling
		Main m = new Main();
		m.start();
	}
	
	public void start() {
//		game = new Game();
//		game.init();
//		game.run();
		GameLWJGL g = null;
		DisplayLWJGL test = new DisplayLWJGL(g);
		test.startGLFW();
		test.startScreen();
		test.loop();
		test.removeWindow();
		test.removeGLFW();
	}
}
