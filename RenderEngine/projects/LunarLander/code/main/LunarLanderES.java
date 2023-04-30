package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static Kurama.utils.Logger.log;

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
    public int currentGen = 0;
    public List<float[]> population = new ArrayList<>();
    Random random;
    public LunarLanderGame game;
    List<Float> scores = new ArrayList<>();
    float curHighestFitness = Float.NEGATIVE_INFINITY;
    float[] curBest = null;
    float averageScore = 0;
    float totalFitness;
    int cumCandidateEval = 0;

    public boolean hasRunEnded = false;

    public static class RepresentationSortable implements Comparable<RepresentationSortable> {

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

            else if (score > o.score) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }

    public LunarLanderES(LunarLanderGame game, int lambda, int mu, int maxIterations,
                         float mutationRate, float recombRate, float T, float epsilon, float alpha) {
        this.lambda = lambda;
        this.mu = mu;
        this.T = T;
        this.epsilon = epsilon;
        this.numIterations = maxIterations;
        this.alpha = alpha;
        this.mutationRate = mutationRate;
        this.crossOverRate = recombRate;
        this.game = game;

        this.random = new Random();

        initialisePopulation();
    }

    public List<float[]> iterate(List<RepresentationSortable> rankedChromosomes) {

        if(currentGen >= numIterations) {
            hasRunEnded = true;
            return population;
        }

        //reset values;
        scores.clear();
        averageScore = 0;
        totalFitness = 0;
        curHighestFitness = Float.NEGATIVE_INFINITY;
        curBest = null;

        for(var chromosome: rankedChromosomes) {

            scores.add(chromosome.score);
            totalFitness += chromosome.score;

            if(chromosome.score > curHighestFitness) {
                curHighestFitness = chromosome.score;
                curBest = chromosome.representation;
            }
        }

        cumCandidateEval += population.size();
        averageScore = totalFitness/(float)population.size();

        // Select MU parents
        var muParents = new ArrayList<float[]>();
        for(int j = 0; j < mu; j++) {
            muParents.add(rankedChromosomes.get(rankedChromosomes.size()-1-j).representation);
        }

        // Create children by first mutating parents
        int numChildPerParent = lambda/mu;

        var children = new ArrayList<float[]>();

        for(var p: muParents) {
            for(int __ = 0; __ < numChildPerParent; __++) {
                children.add(mutate(p));
            }
        }

        // Recomb children among themselves
        Collections.shuffle(children); // To randomize pairing
        var recombChildren = new ArrayList<float[]>();
        for(int k = 0; k < children.size()/2; k++) {
            var p1 = children.get(k * 2 + 0);
            var p2 = children.get(k * 2 + 1);

            recombChildren.addAll(recombination(p1, p2));
        }

        population = recombChildren;
        currentGen++;

        return population;
    }

    public void initialisePopulation() {
        int numWeights = (game.layers[0] * game.layers[1]) + (game.layers[1] * game.layers[2]);

        for(int i = 0; i < lambda; i++) {

            float[] res = new float[numWeights * 2];
            for(int j = 0; j < numWeights; j++) {
                res[j] = random.nextFloat();

                // Randomly initialize sigma to any value between epsilon to 5X of epsilon
                res[res.length/2 + j] =  random.nextFloat(epsilon, epsilon*5);
            }

            population.add(res);
        }
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

        // Calculate new weight values
        for(int i = 0; i < gene.length/2; i++) {
            res[i] = (float) random.nextGaussian(gene[i], res[gene.length/2 + i]);
        }

        return res;
    }

}
