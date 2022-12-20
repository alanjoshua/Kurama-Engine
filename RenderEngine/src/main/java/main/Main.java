package main;

import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.Arrays;

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

		var v1 = new Vector(0,0,0);
		var v2 = new Vector(-1,-10,0);
		var v3 = new Vector(100,100,100);
		var v4 = new Vector(5,5,5);
		var v5 = new Vector(20,-20,-1000);
		var tempVerts = new ArrayList<>(Arrays.asList(v1, v2, v3, v4, v5));
//
//		var res = generateMeshlets(tempVerts, 0, 0,0);
//		res.forEach(val -> log(val.v()));


		game = new ActiveShutterGame("Active shutter Renderer");
		game.start();

//		game = new PointCloudController("Point Cloud Renderer");
//		game.start();

//		var app = new VulkanGame();
//		app.run();

	}
}
