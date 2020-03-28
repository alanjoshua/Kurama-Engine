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
    private int defaultWindowedWidth = 1280;
    private int defaultWindowedHeight = 720;

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static final double winDPI = 96;
    public static final double macDPI = 72;

    public enum DisplayMode {
        FULLSCREEN, WINDOWED
    }

    public DisplayMode displayMode = DisplayMode.FULLSCREEN;

    public Display(int defaultWindowedWidth, int defaultWindowedHeight, Game game) {
        this.game = game;
        this.defaultWindowedWidth = defaultWindowedWidth;
        this.defaultWindowedHeight = defaultWindowedHeight;
        this.setIgnoreRepaint(true);
    }

    public Display(Game game) {
        this.game = game;
        this.setIgnoreRepaint(true);
    }

    public void startScreen() {
        if (displayMode == DisplayMode.FULLSCREEN) setFullScreen();
        else setWindowedMode();
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
        frame.add(this);
        frame.pack();

        this.requestFocus();

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                try {
                    game.getCamera().setImageWidth(getWidth());
                    game.getCamera().setImageHeight(getHeight());
                    game.getCamera().setShouldUpdateValues(true);
                    game.renderingEngine.resetBuffers();
                    game.resetBuffers();
                } catch (Exception ex) {
                }
            }
        });
    }

    public int getDPI() {
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }

    public float getScalingRelativeToDPI() {
        if(OS.indexOf("mac") > 0) {
            int dpi = getDPI();
            return (float)(dpi/macDPI);
        }

        else {
            int dpi = getDPI();
            return  (float)(dpi/winDPI);
        }
    }

    private void setFullScreen() {
        removeWindow();
        screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!screen.isFullScreenSupported()) {
            System.out.println("Full screen mode not supported");
            System.exit(1);
        }

        double scale = getScalingRelativeToDPI();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize(screenSize);
        this.setBackground(Color.black);
        this.requestFocus();
        initFrame(false,false);
        screen.setFullScreenWindow(frame);
    }

    private void setWindowedMode() {
        removeWindow();
        this.setSize(defaultWindowedWidth, defaultWindowedHeight);
        initFrame(true,true);
    }

    public void setWindowedMode(int width, int height) {
        this.setSize(width, height);
        initFrame(true,true);
    }

    public void removeWindow() {
    	if(screen != null) {
			screen.setFullScreenWindow(null);
		}

    	if( frame != null) {
			frame.dispose();
		}
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

    public GraphicsDevice getScreen() {
        return screen;
    }

}
