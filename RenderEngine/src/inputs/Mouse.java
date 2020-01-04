package inputs;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

import main.Game;

public class Mouse implements MouseInputListener, MouseWheelListener {
	
	Game game;
	
	public Mouse(Game game) {
		this.game = game;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		game.getCamera().mouseDragInput(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		game.getCamera().mouseMoveInput(e);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		game.getCamera().mouseScrollInput(e);
	}
	
	
	
}
