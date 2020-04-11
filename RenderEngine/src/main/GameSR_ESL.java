package main;

import engine.model.Model;
import engine.model.ModelBuilder;
import engine.Math.Vector;
import engine.Math.Quaternion;
import engine.renderingEngine.RenderingEngine;

import java.util.Random;

public class GameSR_ESL extends GameSR {

    public int MODELCOUNT = 0;
    public long seed = 123456789;
    public int gridWidth = 100;
    public int gridDepth = 100;
    public String testModel = "/Resources/deer.obj";
    public int angleMax = 200;

    public GameSR_ESL(String threadName) {
        super(threadName);
    }

    public GameSR_ESL(String threadName,boolean shouldBenchmark) {
        super(threadName,shouldBenchmark);
    }

    @Override
    public void initModels() {

        renderingEngine.setRenderPipeline(RenderingEngine.RenderPipeline.Quat);

        Random random = new Random();
        random.setSeed(seed);

        Model grid = new Model(ModelBuilder.buildGridDeprecated(gridWidth, gridDepth),"grid");
        modelsOnlyOutline.add(grid);
        grid.displayMeshInformation();

        for(int i = 0;i < MODELCOUNT;i++) {

            int x = (int) (random.nextInt(gridWidth) - gridWidth / 2 + grid.getPos().get(0));
            int y = (int) (5 + grid.getPos().get(1));
            int z = (int) (random.nextInt(gridDepth) - gridDepth / 2 + grid.getPos().get(2));

            Model deer = new Model(ModelBuilder.buildModelFromFile(testModel,meshInstances),"deer: +i");
            deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
            deer.setPos(x,y,z);

            float angle = (random.nextInt(angleMax) - angleMax/2 );

            Model.MiniBehaviour tempRot = ((m, params )-> {
                Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), angle * timeDelta);
                Quaternion newQ = rot.multiply(m.getOrientation());
                m.setOrientation(newQ);
            });

            deer.setMiniBehaviourObj(tempRot);

            modelsOnlyOutline.add(deer);
        }

//        meshInstances.get("/Resources/deer.obj").displayMeshInformation();

    }

}
