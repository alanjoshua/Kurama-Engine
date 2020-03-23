package main;

import java.util.LinkedList;
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

		DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
		l.pushHead(1);
		l.pushTail(2);
		l.pushTail(-1);
		l.pushHead(-100);
//		System.out.println(l.peek());
		l.push(3,0);
		l.display();
		System.out.println(l.pop(1));
		l.display();
		System.out.println(l.pop(3));
		l.display();

		l.resetLoc();
		System.out.println(l.peekNext());
		System.out.println(l.peekNext());
		System.out.println(l.peekNext());
		System.out.println(l.peekPrevious());
		System.out.println(l.peekPrevious());
		System.out.println(l.peekPrevious());
		System.out.println(l.peekPrevious());

//		game = new Game();
//		game.init();
//		game.run();
	}
}
