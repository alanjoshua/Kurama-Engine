package main;

import engine.Math.DataAnalysis;
import engine.Math.Matrix;

import java.util.List;

public class DataScienceMain {

    public static void main(String[] args) {
        float[][] data =   {{1,2,3,10,11,12},
                            {1,2,3,10,11,12},
                            {1,2,3,10,11,12}};
        List<Matrix> clusters = DataAnalysis.kMeanClustering(new Matrix(data),2);

        System.out.println("K-Mean cluster:");
        for(int i = 0;i < clusters.size();i++) {
            System.out.println("cluster "+(i+1) + ": ");
            clusters.get(i).display();
            System.out.println("------------------------------");
        }

        System.out.println("HAC:");
        List<Matrix> hacClusters =  DataAnalysis.HAC(new Matrix(data));
        for(int i = 0;i < hacClusters.size();i++) {
            System.out.println("cluster "+(i+1) + ": ");
            hacClusters.get(i).display();
            System.out.println("------------------------------");
        }

    }

}
