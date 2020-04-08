package main;

import inputs.InputSR;
import models.Model;
import models.ModelBuilder;
import Math.Vector;
import Math.Quaternion;
import rendering.Camera;
import rendering.RenderingEngine;
import rendering.RenderingEngineSR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class GameSR_ESL extends GameSR {

    public int MODELCOUNT = 50;
    public long seed = 123456789;
    public int gridWidth = 100;
    public int gridDepth = 100;
    public String testModel = "deer.obj";
    public int angleMax = 200;

    public GameSR_ESL(String threadName) {
        super(threadName);
    }

    public GameSR_ESL(String threadName,boolean shouldBenchmark) {
        super(threadName,shouldBenchmark);
    }

    public GameSR_ESL(String threadName, boolean shouldBenchmark, String testModel, int modelCount, int w, int d, int angleMax,long seed) {
        super(threadName,shouldBenchmark);
        this.testModel = testModel;
        this.MODELCOUNT = modelCount;
        this.gridWidth = w;
        this.gridDepth = d;
        this.seed = seed;
        this.angleMax = angleMax;
    }

    @Override
    public void initModels() {

        ((RenderingEngineSR)renderingEngine).setRenderPipeline(RenderingEngine.RenderPipeline.Matrix);

        Random random = new Random();
        random.setSeed(seed);

        Model grid = ModelBuilder.buildGrid(gridWidth, gridDepth);
        modelsOnlyOutline.add(grid);

        for(int i = 0;i < MODELCOUNT;i++) {

            int x = (int) (random.nextInt(gridWidth) - gridWidth / 2 + grid.getPos().get(0));
            int y = (int) (5 + grid.getPos().get(1));
            int z = (int) (random.nextInt(gridDepth) - gridDepth / 2 + grid.getPos().get(2));

            Model deer = ModelBuilder.buildModelFromFile(testModel,meshInstances);
            deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
            deer.setPos(x,y,z);

            float angle = (random.nextInt(angleMax) - angleMax/2 );

            Model.Tick tempRot = (m -> {
                Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), angle * speedConstant);
                Quaternion newQ = rot.multiply(m.getOrientation());
                m.setOrientation(newQ);
            });

            deer.setTickObj(tempRot);

            modelsOnlyOutline.add(deer);
        }
    }

}
