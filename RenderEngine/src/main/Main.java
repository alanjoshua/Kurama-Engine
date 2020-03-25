package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import Math.Vector;
import models.DataStructure.LinkedList.CircularDoublyLinkedList;
import models.DataStructure.LinkedList.DoublyLinkedList;

public class Main {
	
	Game game;
	
	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale","1"); //Important to make fullscreen and GUI scale properly irrespective of windows scaling
		Main m = new Main();
		m.start();
	}
	
	public void start() {
		game = new Game();
		game.init();
		game.run();
	}
}
