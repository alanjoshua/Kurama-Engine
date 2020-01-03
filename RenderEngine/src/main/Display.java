package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;

public class Display extends Canvas {
	
	private JFrame frame;
	private Game game;
	
	public Display(int w, int h,Game game) {
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
                // This is only called when the user releases the mouse button.
//            	if(getWidth() && getHeight() != null) {
            	try {
            	game.getCamera().setImageWidth(getWidth());
            	game.getCamera().setImageHeight(getHeight());
                game.getCamera().updateValues();
            	}catch(Exception ex) {
            		
            	}
            }
        });
		
	}

	public JFrame getFrame() {
		return frame;
	}
	
}
