package main;

import engine.game.Game;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		Main m = new Main();
		m.start();
	}
	
	public void start() {
//		game = new GameSR("Software renderer");
//		game.start();

//		game = new GameSR_ESL("ESL quat-quat",true);
//		game.start();

		game = new GameLWJGL("OpenGL Renderer");
		game.start();
	}
}
