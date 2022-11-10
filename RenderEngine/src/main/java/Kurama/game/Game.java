package Kurama.game;

import Kurama.ComponentSystem.components.MasterWindow;
import Kurama.Math.Vector;
import Kurama.display.Display;
import Kurama.inputs.Input;
import Kurama.misc_structures.GridNode;
import Kurama.renderingEngine.RenderingEngine;
import Kurama.scene.Scene;
import Kurama.utils.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public abstract class Game implements Runnable {

    public float timeDelta;  // In seconds
    public double targetFPS = 1000;
    protected boolean shouldDisplayFPS = false;
    protected boolean programRunning = true;
    public float fps;
    public String displayFPS;
    protected boolean shouldBenchMark = false;

    public Vector boundMin;
    public Vector boundMax;

    protected Thread gameLoopThread;
    protected BufferedWriter bw;

    public Scene scene;
    public MasterWindow rootGuiComponent;
    public RenderingEngine renderingEngine;
    public Display display;
    public Input input;

    // Convenient boolean that could be used by other game objects to conveniently print to console every second
    public boolean isOneSecond = false;

    public static GraphicsApi GRAPHICS_API;

    public enum GraphicsApi {CPU, OPENGL, VULKAN}

    public Game(String threadName) {
        gameLoopThread = new Thread(this,threadName);
    }

    public Game(String threadName, boolean shouldBenchMark) {
        gameLoopThread = new Thread(this,threadName);

        this.shouldBenchMark = shouldBenchMark;
        if(shouldBenchMark) {
            try {
                Date date = new Date();
                File temp = File.createTempFile("FPS LOG ",gameLoopThread.getName()+" Time-"+date.getTime()+".txt");
                bw = new BufferedWriter(new FileWriter(temp));
                System.out.println("opened FPS log file: "+temp.getAbsolutePath());
            }
            catch(Exception e) {
                System.err.println("Couldn't initialize FPS log file");
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if(gameLoopThread == null) {
            run();
            return;
        }

        String osName = System.getProperty("os.name");
        if ( osName.contains("Mac") ) {
            gameLoopThread.run();   //To make this program compatible with macs
        } else {
            System.out.println("start called");
            gameLoopThread.start();
        }
    }

    public void run() {
        runGame();
    }

    public abstract void init();
    public abstract void cleanUp();
    public abstract void tick();
    public abstract void render();

    public void finalCleanUp() {
        cleanUp();
        cleanBenchMark();
    }

    protected void cleanBenchMark() {
        if(bw != null) {
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runGame() {

        init();

        double dt = 0.0;
        double startTime = System.nanoTime();
        double currentTime = System.nanoTime();
        double timerStartTime = System.nanoTime();
        double timer = 0.0;
        double tempDt = 0;
        float tickInterval = 0;
        double timeU = 0;

        while (programRunning) {
            isOneSecond = false;
            timeU = ((1000000000.0 / targetFPS));
            currentTime = System.nanoTime();
            tempDt = (currentTime - startTime);
            dt += tempDt/timeU;
            tickInterval += tempDt;
            startTime = currentTime;
            timer = (currentTime - timerStartTime);

            if (timer >= 1000000000.0) {
                isOneSecond = true;
                displayFPS = getDisplayFPS();
                fps = 0;
                timerStartTime = System.nanoTime();

                if(shouldDisplayFPS) {
                    Logger.log("fps: "+ displayFPS);
                }

                if(shouldBenchMark) {
                    try {
                        bw.write(displayFPS+"\n");
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (dt >= 1) {
                timeDelta = (float) (tickInterval /1000000000.0);
                tickInterval = 0;
                tick();
                render();
                fps++;
                dt = 0;
            }
        }

        finalCleanUp();

    }

    public String getDisplayFPS() {
        return String.valueOf((int)fps);
    }

    public void setTargetFPS(double targetFPS) {
        this.targetFPS = targetFPS;
    }

    public void setProgramRunning(boolean programRunning) {
        this.programRunning = programRunning;
    }

    public MasterWindow getMasterWindow() {
        return rootGuiComponent;
    }

}
