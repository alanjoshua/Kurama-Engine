package main;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Vector;
import models.Cube;
import models.Model;
import models.Model.Tick;
import models.ModelBuilder;
import rendering.RenderingEngine;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale","1"); //Important to make fullscreen and GUI scale properly irrespective of windows scaling
		Main m = new Main();
		m.start();
	}
	
	public void start() {

		game = new Game();
//		game = new ESL_Game(200);

		game.init();
		game.run();
	}
}
