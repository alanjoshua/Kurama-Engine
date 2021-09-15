package main;

import Kurama.game.Game;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		Main m = new Main();
		m.start();
	}
	
	public void start() {
		game = new MindPalace("Mind Palace");
//		game = new Editor("Editor");
		game.start();

	}
}
