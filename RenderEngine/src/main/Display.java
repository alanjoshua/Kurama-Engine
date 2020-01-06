package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;

import inputs.Input;

public class Display extends Canvas {
	
	private JFrame frame;
	private Game game;
	private Input input;
	
	public Display(int w, int h,Game game) {
		this.setSize(w, h);
		this.setBackground(Color.black);
		frame = new JFrame();
		frame.add(this);
		frame.pack();
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(true);
		frame.setIgnoreRepaint( true );
		
		frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
            	try {
            	game.getCamera().setImageWidth(getWidth());
            	game.getCamera().setImageHeight(getHeight());
                game.getCamera().setShouldUpdateValues(true);
            	}catch(Exception ex) {}
            }
        });
		
//		disableCursor();
	}
	
public void setInput(Input input) {
		this.input = input;
		frame.addMouseListener(input);
		frame.addMouseMotionListener(input);
		frame.addMouseWheelListener(input);
		frame.addKeyListener(input);
		
		this.addMouseListener(input);
		this.addMouseMotionListener(input);
		this.addMouseWheelListener(input);
		this.addKeyListener(input);
	}

	public JFrame getFrame() {
		return frame;
	}
	
	public void disableCursor() {
	    Toolkit tk = Toolkit.getDefaultToolkit();
	    Image image = tk.createImage( "" );
	    Point point = new Point( 0, 0 );
	    String name = "CanBeAnything";
	    Cursor cursor = tk.createCustomCursor( image, point, name ); 
	    frame.setCursor( cursor );
	  }
	
	public void enableCursor() {
	    frame.setCursor(Cursor.getDefaultCursor());
	}
	
}
