package main;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import inputs.Input;

public class Display extends Canvas {

	private JFrame frame;
	private GraphicsDevice screen;
	private Game game;
	private Input input;
	private int w;
	private int h;
	
	public Display(int w, int h,Game game) {

		this.w = w;
		this.h = h;

		screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		if (!screen.isFullScreenSupported()) {
			System.out.println("Full screen mode not supported");
			System.exit(1);
		}

		this.setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize());

		this.setBackground(Color.black);
		frame = new JFrame();

		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setResizable(true);
		frame.setIgnoreRepaint( true );
		frame.setUndecorated(true);
		frame.setVisible(true);

//		if (!frame.isUndecorated()) {
//			frame.setUndecorated(true);
//		}
//		if(!frame.isVisible()){
//			frame.setVisible(true);
//		}

		frame.add(this);
		frame.pack();

		this.requestFocus();

		screen.setFullScreenWindow(frame);
		
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

	public void setFullScreen() {
		screen.setFullScreenWindow(frame);
	}

	public void removeFullScreen() {
		screen.setFullScreenWindow(null);
//		input.recalibrateCentre();
//		frame.pack();
	}
	
	public void setInput(Input input) {
		this.input = input;
		
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

	public GraphicsDevice getScreen() {
		return screen;
	}
	
}
