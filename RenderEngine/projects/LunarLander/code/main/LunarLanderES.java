package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LunarLanderES {

    public int lambda;
    public int mu;

    // Controls the learning rate of the sigma values
    // I chose to go with uncorrelated self adaptation with n step sizes
    public float T;

    // min sigma value
    public float epsilon;

    // Controls the recombination behaviour (BLX − α)
    public float alpha;
    public int numIterations;
    public float mutationRate, crossOverRate;
    public float convergenceTolerance = 0.01f;
    public int convergenceCheckRate = 10;
    float runningFitnessDiff;
    float previousBestXY;
    public int currentGen = 0;
    Random random;

    public class RepresentationSortable implements Comparable<RepresentationSortable> {

        public float[] representation;
        public float score;
        public RepresentationSortable(float[] representation, float score) {
            this.representation = representation;
            this.score = score;
        }

        @Override
        public int compareTo(RepresentationSortable o) {
            if(o.score == score) {
                return 0;
            }

            else if (score < o.score) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }

    public LunarLanderES(int lambda, int mu, int maxIterations, int convergenceRateCheck, float convergenceTolerance,
                         float mutationRate, float recombRate, float T, float epsilon, float alpha) {
        this.lambda = lambda;
        this.mu = mu;
        this.convergenceTolerance = convergenceTolerance;
        this.convergenceCheckRate = convergenceRateCheck;
        this.T = T;
        this.epsilon = epsilon;
        this.numIterations = maxIterations;
        this.alpha = alpha;
        this.mutationRate = mutationRate;
        this.crossOverRate = recombRate;

        this.random = new Random();
    }

    // BLX-α
    // I selected this because it allows values from both outside and inside the parents' range of values,
    // so it is a good balance between exploration and exploitation

    // Returns two children
    public List<float[]> recombination(float[] p1, float[] p2) {

        if(random.nextFloat() > crossOverRate) {
            var res = new ArrayList<float[]>();
            res.add(p1);
            res.add(p2);
            return res;
        }

        float u = random.nextFloat();
        float y = (1 - 2*alpha) * u - alpha;
        float[] child1 = new float[p1.length];
        float[] child2 = new float[p1.length];

        for(int i = 0; i < p1.length; i++) {
            child1[i] = (1-y)*p1[i] + y*p2[i];
            child2[i] = (y)*p1[i] + (1-y)*p2[i];
        }

        var res = new ArrayList<float[]>();
        res.add(child1);
        res.add(child2);
        return res;
    }

    public float[] mutate(float[] gene) {
        if(random.nextFloat() > mutationRate) {
            return gene;
        }

        float[] res = new float[gene.length];

        // calculate new Sigma values
        for(int i = gene.length/2; i < gene.length; i++) {
            res[i] = (float) (gene[i] * Math.exp(random.nextGaussian(0, T)));
            if(res[i] < epsilon) {
                res[i] = epsilon;
            }
        }



        float x = gene[0];
        float y = gene[1];
        float sigma1 = gene[2];
        float sigma2 = gene[3];

        float sigma1P = (float) (sigma1 * Math.exp(random.nextGaussian(0, T)));
        float sigma2P = (float) (sigma2 * Math.exp(random.nextGaussian(0, T)));

        if(sigma1P < epsilon)
            sigma1P = epsilon;

        if(sigma2P < epsilon) {
            sigma2P = epsilon;
        }

        float xp = (float) random.nextGaussian(x, sigma1P);
        float yp = (float) random.nextGaussian(y, sigma2P);

        return new float[]{xp, yp, sigma1P, sigma2P};
    }

}
