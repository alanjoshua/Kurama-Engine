package main;

import inputs.Input;
import inputs.InputSR;
import models.Model;
import rendering.Camera;
import rendering.RenderingEngine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public abstract class Game implements Runnable {

    protected Display display;
    protected Camera cam;
    protected Input input;
    protected RenderingEngine renderingEngine;
    protected List<Model> models;

    protected double targetFPS = 1000;
    protected boolean shouldDisplayFPS = false;
    protected boolean programRunning = true;
    protected boolean isGameRunning = true;
    protected float fps;
    protected float displayFPS;
    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;
    protected float speedConstant;
    protected int lookAtIndex = 0;

    protected boolean shouldBenchMark = false;

    protected Thread gameLoopThread;
    protected BufferedWriter bw;

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
        gameLoopThread.start();
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
        display.cleanUp();
        renderingEngine.cleanUp();
        for(Model m:models) {
            m.mesh.cleanUp();
        }
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

        while (programRunning) {

            double timeU = ((1000000000.0 / targetFPS));
            currentTime = System.nanoTime();
            tempDt = (currentTime - startTime);
            dt += tempDt/timeU;
            tickInterval += tempDt;
            startTime = currentTime;
            timer = (currentTime - timerStartTime);

            if (dt >= 1) {
                speedConstant = (float) (tickInterval /1000000000.0);
                tickInterval = 0;
                tick();
                render();
                fps++;
                dt = 0;
            }

            if (timer >= 1000000000.0) {
                displayFPS = fps;
                fps = 0;
                timer = 0;
                timerStartTime = System.nanoTime();

                if(shouldBenchMark) {
                    try {
                        bw.write(displayFPS+"\n");
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        finalCleanUp();

    }

    public RenderingEngine getRenderingEngine() {
        return renderingEngine;
    }

    public double getTargetFPS() {
        return targetFPS;
    }

    public void setTargetFPS(double targetFPS) {
        this.targetFPS = targetFPS;
    }

    public boolean isShouldFPS() {
        return shouldDisplayFPS;
    }

    public void setShouldFPS(boolean shouldFPS) {
        this.shouldDisplayFPS = shouldFPS;
    }

    public boolean isProgramRunning() {
        return programRunning;
    }

    public void setProgramRunning(boolean programRunning) {
        this.programRunning = programRunning;
    }

    public Display getDisplay() {
        return display;
    }

    public Camera getCamera() {
        return cam;
    }

    public Input getInput() {
        return input;
    }

    public List<Model> getModels() {
        return models;
    }

    public void setModels(List<Model> models) {
        this.models = models;
    }
}
