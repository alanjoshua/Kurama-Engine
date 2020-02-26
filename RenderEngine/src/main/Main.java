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
		Main m = new Main();
		m.start();
	}
	
	public void start() {
		game = new Game(1000,1000);
		game.init();
		System.out.println(game.getDisplay().getScalingRelativeToDPI());
		System.out.println(game.getDisplay().getHeight());
		game.run();
	}
}
