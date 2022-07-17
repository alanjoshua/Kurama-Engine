package main;

import Kurama.game.Game;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		Main m = new Main();
		m.start();
	}
	
	public void start() {
//		game = new GameLWJGL("OpenGL Renderer");
//		game = new Editor("Editor");
//		game.start();

		var app = new VulkanGame();
		app.run();

	}
}
