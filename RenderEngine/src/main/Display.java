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
    public static final int winDPI = 96;
    public static final int macDPI = 72;

    public static enum DisplayMode {
        FULLSCREEN, WINDOWED
    }

    public DisplayMode displayMode = DisplayMode.FULLSCREEN;

    public Display(int defaultWindowedWidth, int defaultWindowedHeight, Game game) {
        this.game = game;
        this.defaultWindowedWidth = defaultWindowedWidth;
        this.defaultWindowedHeight = defaultWindowedHeight;
    }

    public Display(Game game) {
        this.game = game;
    }

    public void startScreen() {
        if (displayMode == DisplayMode.FULLSCREEN) setFullScreen();
        else setWindowedMode();
    }

    public void initFrame(boolean shouldBorder) {
        System.out.println("init called");
        frame = new JFrame();
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.setIgnoreRepaint(true);

        if(!shouldBorder) {
            frame.setUndecorated(true);
        }

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
                } catch (Exception ex) {
                }
            }
        });
    }

    private void setFullScreen() {
        removeWindow();
        screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!screen.isFullScreenSupported()) {
            System.out.println("Full screen mode not supported");
            System.exit(1);
        }

        Dimension actual;

        if(OS.indexOf("mac") > 0) {
            System.out.println("Detected Mac");
            int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            double scale = dpi/macDPI;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            actual = new Dimension((int)(screenSize.getWidth()*scale),(int)(screenSize.getHeight()*scale));
        }

        else {
            System.out.println("Detected Windows (!Mac)");
            int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            double scale = dpi/winDPI;
            System.out.println(scale);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            System.out.println(screenSize);
            actual = new Dimension((int)(screenSize.getWidth()*scale),(int)(screenSize.getHeight()*scale));
            System.out.println(actual);
        }

        this.setSize(actual);
        this.setBackground(Color.black);
        this.requestFocus();

        initFrame(false);

        screen.setFullScreenWindow(frame);
    }

    private void setWindowedMode() {
        removeWindow();
        this.setSize(defaultWindowedWidth, defaultWindowedHeight);
        initFrame(true);
    }

    public void setWindowedMode(int width, int height) {
        this.setSize(width, height);
        initFrame(true);
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
