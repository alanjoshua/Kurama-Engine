package main;

import GUI.Button;
import inputs.Input;
import models.Model;
import models.ModelBuilder;
import rendering.Camera;
import rendering.RenderingEngine;
import Math.Vector;
import Math.Quaternion;

import java.util.ArrayList;
import java.util.Random;

public class ESL_Game extends Game {

    protected int gridWidth = 100;
    protected int gridDepth = 100;
    protected long seed = 123456789;
    protected Random rand;
    protected int N = 50;


    ESL_Game() {

    }

    ESL_Game(int N) {
        this.N = N;
    }

    public void init() {

        display = new Display(this);
        input = new Input(this);
        display.setInput(input);
        renderingEngine = new RenderingEngine(this);
        rand = new Random();
        rand.setSeed(seed);

        display.startScreen();

        pauseButtons = new ArrayList<>();
        models = new ArrayList<>(N + 1);

        cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 1f, 1000,
                display.getWidth(), display.getHeight());

        initModels();
        initPauseScreen();

        renderingEngine.setProjectionMode(RenderingEngine.ProjectionMode.PERSPECTIVE);
        renderingEngine.setRenderPipeline(RenderingEngine.RenderPipeline.Matrix);
        cam.lookAtModel(models.get(1));
        cam.updateValues();

    }

    public void initModels() {

        Model grid = ModelBuilder.buildGrid(gridWidth, gridDepth);
        grid.setPos(new Vector(new float[] {0,0,0}));

        models.add(grid);

        for(int i = 0;i < N;i++) {

            Model deer = ModelBuilder.buildModelFromFile("deer.obj");
            Vector pos = new Vector(new float[]{rand.nextInt(gridWidth) - gridWidth/2,7,rand.nextInt(gridDepth) - gridDepth/2});
            deer.setPos(pos);
            deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
//            deer.triangulate(true);
            int angle = (rand.nextInt(200) - 100);

            deer.setTickObj(
                    (m -> {
                        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), angle*speedConstant);
                        Quaternion newQ = rot.multiply(m.getOrientation());
                        m.setOrientation(newQ);
                    })
            );
            
            models.add(deer);
        }
    }

}
