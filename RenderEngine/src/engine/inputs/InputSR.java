package engine.inputs;

import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import engine.Math.Vector;
import engine.display.DisplaySR;
import engine.game.Game;

public class InputSR extends Input implements MouseInputListener, MouseWheelListener, KeyListener {

	// Current state of the keyboard
	private boolean[] currentKeys = null;

	// Polled keyboard state
	private KeyState[] keys = null;

	private static final int BUTTON_COUNT = 3;
	// Used for relative movement
	private int dx, dy;
	// Used to re-center the mouse
	private Robot robot = null;
	// Convert coordinates from component to screen
	private Component component;
	// The center of the component
	private Vector center;
	// Is this relative or absolute
	private boolean relative;
	// Polled position of the mouse cursor
	private Vector mousePos = null;
	// Current position of the mouse cursor
	private Vector currentPos = null;
	// Current state of mouse buttons
	private boolean[] state = null;
	// Colled mouse buttons
	private MouseState[] poll = null;
	
	private boolean isScrolled = false;
	private float scrollVal = 0;
	private boolean currentIsScrolled = false;
	private float currentScrollVal = 0;

	private enum MouseState {
		RELEASED, // Not down
		PRESSED, // Down, but not the first time
		ONCE // Down for the first time
	}

	public InputSR(Game game) {
		super(game);
		init();
	}

	public synchronized void poll() {

		for (int i = 0; i < KEY_COUNT; ++i) {
			// Set the key state
			if (currentKeys[i]) {
				// If the key is down now, but was not
				// down last frame, set it to ONCE,
				// otherwise, set it to PRESSED
				if (keys[i] == KeyState.RELEASED)
					keys[i] = KeyState.ONCE;
				else
					keys[i] = KeyState.PRESSED;
			} else {
				keys[i] = KeyState.RELEASED;
			}
		}

		// If relative, return only the delta movements,
		// otherwise return the current position...
		if (isRelative()) {
			mousePos = new Vector(new float[] { dx, dy});
		} else {
			mousePos = new Vector(currentPos);
		}

		// Since we have polled, need to reset the delta
		// so the values do not accumulate
		dx = dy = 0;
		int w = component.getBounds().width;
		int h = component.getBounds().height;

		center = new Vector(new float[] { w / 2, h / 2 });
		
		isScrolled = currentIsScrolled;
		scrollVal = currentScrollVal;
		
		currentIsScrolled = false;
		currentScrollVal = 0;

		// Check each mouse button
		for (int i = 0; i < BUTTON_COUNT; ++i) {
			// If the button is down for the first
			// time, it is ONCE, otherwise it is
			// PRESSED.
			if (state[i]) {
				if (poll[i] == MouseState.RELEASED)
					poll[i] = MouseState.ONCE;
				else
					poll[i] = MouseState.PRESSED;
			} else {
				// Button is not down
				poll[i] = MouseState.RELEASED;
			}
		}

	}

	@Override
	public void init() {
		KEY_COUNT = 256;

		currentKeys = new boolean[KEY_COUNT];
		keys = new KeyState[KEY_COUNT];
		for (int i = 0; i < KEY_COUNT; ++i) {
			keys[i] = KeyState.RELEASED;
		}

		// Need the component object to convert screen coordinates
		this.component = ((DisplaySR)game.getDisplay()).getCanvas();
		int w = component.getBounds().width;
		int h = component.getBounds().height;
		center = new Vector(new float[] { w / 2, h / 2 });
		// Calculate the component center

		try {
			robot = new Robot();
		} catch (Exception e) {
			// Handle exception [game specific]
		}

		// Create default mouse positions
		mousePos = new Vector(new float[] { 0, 0});
		currentPos = new Vector(new float[] { 0, 0});

		// Setup initial button states
		state = new boolean[BUTTON_COUNT];
		poll = new MouseState[BUTTON_COUNT];
		for (int i = 0; i < BUTTON_COUNT; ++i) {
			poll[i] = MouseState.RELEASED;
		}
	}

	public boolean keyDown(int keyCode) {
		return keys[keyCode] == KeyState.ONCE || keys[keyCode] == KeyState.PRESSED;
	}

	public boolean keyDownOnce(int keyCode) {
		return keys[keyCode] == KeyState.ONCE;
	}

	public synchronized void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode >= 0 && keyCode < KEY_COUNT) {
			currentKeys[keyCode] = true;
		}
	}

	public synchronized void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode >= 0 && keyCode < KEY_COUNT) {
			currentKeys[keyCode] = false;
		}
	}

	public void keyTyped(KeyEvent e) {
		// Not needed
	}

	public boolean isRelative() {
		return relative;
	}

	public void setRelative(boolean relative) {
		this.relative = relative;
		if (relative) {
			centerMouse();
		}
	}

	public Vector getPosition() {
		return mousePos;
	}

	public boolean buttonDownOnce(int button) {
		return poll[button - 1] == MouseState.ONCE;
	}

	public boolean buttonDown(int button) {
		return poll[button - 1] == MouseState.ONCE || poll[button - 1] == MouseState.PRESSED;
	}

	public void mousePressed(MouseEvent e) {
		state[e.getButton() - 1] = true;
	}

	public synchronized void mouseReleased(MouseEvent e) {
		state[e.getButton() - 1] = false;
	}

	public synchronized void mouseEntered(MouseEvent e) {
		mouseMoved(e);
	}

	public synchronized void mouseExited(MouseEvent e) {
		mouseMoved(e);
	}

	public synchronized void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	public synchronized void mouseMoved(MouseEvent e) {
//		System.out.println(e.getX() + " : " + e.getY());
		if (isRelative()) {
			Point p = e.getPoint();
			dx += p.x - center.get(0);
			dy += center.get(1) - p.y;
			centerMouse();
		} else {
			currentPos = new Vector(new float[] { e.getPoint().x, e.getPoint().y});
		}
	}

	public void mouseClicked(MouseEvent e) {
		// Not needed
	}

	public Vector getCenter() {return center;}

	public boolean isScrolled() {
		return isScrolled;
	}

	public float getScrollVal() {
		return scrollVal;
	}

	private void centerMouse() {
		if (robot != null && component.isShowing()) {
			// Because the convertPointToScreen method
			// changes the object, make a copy!

			Point copy = new Point((int) center.get(0), (int) center.get(1));
			SwingUtilities.convertPointToScreen(copy, component);
			robot.mouseMove(copy.x, copy.y);
		}
	}

	@Override
	public synchronized void mouseWheelMoved(MouseWheelEvent e) {
		currentIsScrolled = true;
		currentScrollVal = (float) e.getPreciseWheelRotation();
	}

}
