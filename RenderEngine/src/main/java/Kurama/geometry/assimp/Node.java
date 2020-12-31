package Kurama.geometry.assimp;

import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

import java.util.ArrayList;
import java.util.List;

public class Node {

    public List<Node> children = new ArrayList<>();
    public String name;
    public Node parent;
    public Vector pos;
    public Quaternion orientation;
    public Vector scale;

    public Matrix transformation;

    public Node(String name, Node parent, Vector pos, Quaternion orientation) {
        this.name = name;
        this.parent = parent;
        this.pos = pos;
        this.orientation = orientation;
        this.scale = new Vector(1,1,1);
    }

    public Node(String name, Node parent, Matrix transformation) {
        this.name = name;
        this.parent = parent;
        this.transformation = transformation;
    }

    public Node(String name, Node parent, Vector pos, Quaternion orientation, Vector scale) {
        this.name = name;
        this.parent = parent;
        this.pos = pos;
        this.orientation = orientation;
        this.scale = scale;
    }

}
