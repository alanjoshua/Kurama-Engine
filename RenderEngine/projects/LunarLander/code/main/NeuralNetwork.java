package main;

import Kurama.Math.Matrix;
import Kurama.Math.Vector;

import static Kurama.utils.Logger.log;

public class NeuralNetwork {

    public int[] layers;
    public Matrix w1;
    public Matrix w2;

    public NeuralNetwork(int[] layers) {
        this.layers = layers;
        instantiateWeights();
    }

    public NeuralNetwork(int[] layers, float[] chromosome) {
        this.layers = layers;
        instantiateWeights(chromosome);
    }

    public void instantiateWeights() {
        w1 = Matrix.createRandomMatrix(layers[1], layers[0], null);
        w2 = Matrix.createRandomMatrix(layers[2], layers[1], null);
    }

    public void instantiateWeights(float[] chromosome) {

        float[][] w1Vals = new float[layers[1]][layers[0]];
        for(int i = 0; i < layers[1]; i++) {
            for(int j = 0; j < layers[0]; j++) {
                w1Vals[i][j] = chromosome[i * j];
            }
        }

        float[][] w2Vals = new float[layers[2]][layers[1]];
        for(int i = 0; i < layers[2]; i++) {
            for(int j = 0; j < layers[1]; j++) {
                w2Vals[i][j] = chromosome[(layers[1] * layers[0]) + (i * j)];
            }
        }

        w1 = new Matrix(w1Vals);
        w2 = new Matrix(w2Vals);
    }

    public Vector runBrain(Vector input) {
        var res = sigmoid(w2.matMul(relu(w1.matMul(input))));
        return res.getColumn(0);
    }

    public Vector relu(Vector in) {
        var res = new float[in.getNumberOfDimensions()];
        for(int i = 0; i < res.length; i++) {
            res[i] = Math.max(0, in.get(i));
        }
        return new Vector(res);
    }

    public Vector zeroOrOne(Vector in) {
        var res = new float[in.getNumberOfDimensions()];
        for(int i = 0; i < res.length; i++) {
            res[i] = in.get(i) <= 0.5?0f:1f;
        }
        return new Vector(res);
    }
    public Matrix zeroOrOne(Matrix in) {
        var res = new float[in.getRows()][in.getCols()];

        for(int i = 0; i < in.getRows(); i++) {
            for(int j = 0; j < in.getCols(); j++) {
                res[i][j] = in.get(i, j) <= 0.5?0f:1f;
            }
        }

        return new Matrix(res);
    }


    public Matrix relu(Matrix in) {
        var res = new float[in.getRows()][in.getCols()];

        for(int i = 0; i < in.getRows(); i++) {
            for(int j = 0; j < in.getCols(); j++) {
                res[i][j] = Math.max(0, in.get(i, j));
            }
        }

        return new Matrix(res);
    }

    public Matrix sigmoid(Matrix in) {
        var res = new float[in.getRows()][in.getCols()];

        for(int i = 0; i < in.getRows(); i++) {
            for(int j = 0; j < in.getCols(); j++) {
                res[i][j] = (float) (1.0f/(1+Math.exp(-in.get(i,j))));
            }
        }

        return new Matrix(res);
    }

}
