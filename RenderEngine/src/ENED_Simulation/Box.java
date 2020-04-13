package ENED_Simulation;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Vector;
import engine.model.Model;

public class Box extends Model {

    public int layout;  //0 - bottom, 1 - top
    public Vector barCode; // 4 digit barcode

    public Box(Mesh mesh, String identifier,int layout, Vector barCode) {
        super(mesh, identifier);
        this.layout = layout;
        this.barCode = barCode;
    }

}
