package engine.game;

import engine.DataStructure.GridNode;
import engine.DataStructure.Scene;
import engine.HUD;
import engine.Math.Vector;
import engine.display.Display;
import engine.inputs.Input;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.Model;
import engine.camera.Camera;
import engine.renderingEngine.RenderingEngine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public abstract class Game implements Runnable {

    protected float timeDelta;
    protected double targetFPS = 1000;
    protected boolean shouldDisplayFPS = false;
    protected boolean programRunning = true;
    protected float fps;
    protected float displayFPS;
    protected boolean shouldBenchMark = false;

    public Vector boundMin;
    public Vector boundMax;

    protected Thread gameLoopThread;
    protected BufferedWriter bw;

    public HUD hud;
    public Scene scene;

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

    public boolean isVectorInsideWorld(Vector newPos) {

        if(boundMax == null || boundMin == null) {
            return true;
        }

        if(boundMin!= null && boundMax!=null
                && newPos.get(0) >= boundMin.get(0) && newPos.get(0) < boundMax.get(0)
                && newPos.get(1) >= boundMin.get(1) && newPos.get(1) < boundMax.get(1)
                && newPos.get(2) >= boundMin.get(2) && newPos.get(2) < boundMax.get(2)) {
            return true;
        }
        return false;
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
                timeDelta = (float) (tickInterval /1000000000.0);
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

    public abstract RenderingEngine getRenderingEngine();

    public abstract Display getDisplay();

    public abstract Camera getCamera();

    public abstract Input getInput();

    public abstract List<Model> getModels();

    public float getMovementCost(GridNode current) {
        return 0f;
    }
}
