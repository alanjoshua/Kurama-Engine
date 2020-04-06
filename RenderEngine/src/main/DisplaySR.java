package main;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import inputs.Input;
import inputs.InputSR;
import rendering.RenderingEngineSR;

public class DisplaySR extends Display {

    private JFrame frame;
    private Canvas canvas;
    private GraphicsDevice screen;

    public DisplaySR(int defaultWindowedWidth, int defaultWindowedHeight, Game game) {
        super(defaultWindowedWidth,defaultWindowedHeight,game);
    }

    public DisplaySR(Game game) {
        super(game);
    }

    @Override
    public void init() {
        canvas = new Canvas();
        canvas.setIgnoreRepaint(true);
    }

    public void startScreen() {
        if (displayMode == DisplayMode.FULLSCREEN) setFullScreen();
        else setWindowedMode();
    }

    @Override
    public void toggleWindowModes() {

    }

    @Override
    public int getWidth() {
        return canvas.getWidth();
    }

    @Override
    public int getHeight() {
        return canvas.getHeight();
    }

    public void initFrame(boolean shouldBorder, boolean shouldResizable) {
        frame = new JFrame();
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);

        if(shouldResizable) {
            frame.setResizable(true);
        }
        else {
            frame.setResizable(false);
        }

        frame.setIgnoreRepaint(true);

        if(!shouldBorder) {
            frame.setUndecorated(true);
        }

        frame.setIgnoreRepaint(true);
        frame.setVisible(true);
        frame.add(canvas);
        frame.pack();

        canvas.requestFocus();

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                try {
                    game.getCamera().setImageWidth(getWidth());
                    game.getCamera().setImageHeight(getHeight());
                    game.getCamera().setShouldUpdateValues(true);
                    ((RenderingEngineSR)game.renderingEngine).resetBuffers();
                } catch (Exception ex) {
                }
            }
        });
    }

    public void setFullScreen() {
        removeWindow();
        screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!screen.isFullScreenSupported()) {
            System.out.println("Full screen mode not supported");
            System.exit(1);
        }

        double scale = getScalingRelativeToDPI();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        canvas.setSize(screenSize);
        canvas.setBackground(Color.black);
        canvas.requestFocus();
        initFrame(false,false);
        screen.setFullScreenWindow(frame);
    }

    public void setWindowedMode() {
        removeWindow();
        canvas.setSize(defaultWindowedWidth, defaultWindowedHeight);
        initFrame(true,true);
    }

    public void setWindowedMode(int width, int height) {
        canvas.setSize(width, height);
        initFrame(true,true);
    }

    protected void removeWindow() {
    	if(screen != null) {
			screen.setFullScreenWindow(null);
		}

    	if( frame != null) {
			frame.dispose();
		}
    }

    public void setInput(InputSR input) {
        this.input = input;
        canvas.addMouseListener(input);
        canvas.addMouseMotionListener(input);
        canvas.addMouseWheelListener(input);
        canvas.addKeyListener(input);
    }

    public JFrame getFrame() {
        return frame;
    }

    public void disableCursor() {
        if (frame != null) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Image image = tk.createImage("");
            Point point = new Point(0, 0);
            String name = "CanBeAnything";
            Cursor cursor = tk.createCustomCursor(image, point, name);
            frame.setCursor(cursor);
        }
    }

    public void enableCursor() {
        if (frame != null) {
            frame.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void cleanUp() {
        removeWindow();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public GraphicsDevice getScreen() {
        return screen;
    }

}
