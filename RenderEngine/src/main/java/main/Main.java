package main;

import Kurama.game.Game;

import static Kurama.utils.Logger.log;

public class Main {

	Game game;

	public static void main(String[] args) {
		Main m = new Main();
		m.start();
	}

	public void start() {
//		game = new GameLWJGL("OpenGL Renderer");
//		game.start();
//		game = new Editor("Editor");

//		game = new GameVulkan("Vulkan");
//		game.start();

//		game = new AnaglyphGame("Anaglyph Renderer");
//		game.start();
//
//		var res = generateMeshlets(tempVerts, 0, 0,0);
//		res.forEach(val -> log(val.v()));


		game = new LunarLanderGame("Lunar Lander!");
		game.start();
//
//		game = new PointCloudController("Point Cloud Renderer (work in progress)");
//		game.start();

//		var app = new VulkanGame();
//		app.run();

	}
}
