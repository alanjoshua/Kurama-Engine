package engine.geometry.MD5;

import engine.Math.Vector;

import java.util.ArrayList;
import java.util.List;

public class Frame {

    public Vector minBound;
    public Vector maxBound;
    public List<Float> components;

    public Frame(int numAnimatedComponents) {
        components = new ArrayList<>(numAnimatedComponents);
    }

}
