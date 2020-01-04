package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;

import inputs.Mouse;

public class Display extends Canvas {
	
	private JFrame frame;
	private Game game;
	private Mouse mouseInput;
	
	public Display(int w, int h,Game game) {
		mouseInput = new Mouse(game);
		this.setSize(w, h);
		this.setBackground(Color.black);
		frame = new JFrame();
		frame.add(this);
		frame.pack();
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(true);
		
		frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
            	try {
            	game.getCamera().setImageWidth(getWidth());
            	game.getCamera().setImageHeight(getHeight());
                game.getCamera().setShouldUpdateValues(true);
            	}catch(Exception ex) {}
            }
        });
		
		this.addMouseListener(mouseInput);
		this.addMouseMotionListener(mouseInput);
		this.addMouseWheelListener(mouseInput);
	}

	public JFrame getFrame() {
		return frame;
	}
	
}
