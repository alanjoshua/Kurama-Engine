package main;

import Math.Utils;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale","1"); //Important to make fullscreen and GUI scale properly irrespective of windows scaling
		Main m = new Main();
		m.start();
	}
	
	public void start() {
//		game = new Game();
//		game.start();

		GameLWJGL g = new GameLWJGL();
		g.start();
	}
}
