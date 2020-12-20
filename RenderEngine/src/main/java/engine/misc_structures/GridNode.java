package engine.misc_structures;

import engine.Math.Vector;

public class GridNode implements Comparable<GridNode> {

    public Vector pos;
    public float priority;

    public GridNode(Vector pos, float priority) {
        this.pos = pos;
        this.priority = priority;
    }

    @Override
    public int compareTo(GridNode o) {
        if(priority < o.priority) {
            return -1;
        }
        else if(priority == o.priority) {
            return 0;
        }
        else {
            return 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }

        GridNode g = (GridNode)o;
        return pos.sub(g.pos).getNorm() == 0;
    }

    @Override
    public String toString() {
        return pos.toString()+"::"+priority;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for(float val:pos.getData()) {
            hash+=val;
        }
        return (hash);
    }
}
