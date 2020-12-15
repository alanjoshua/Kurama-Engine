package engine.geometry.MD5;

import engine.Mesh.Face;

public class Triangle extends Face {

    public int index;

    public Triangle(int index, int v1, int v2, int v3) {
        this.index = index;

        var v11 = new engine.Mesh.Vertex();
        var v22 = new engine.Mesh.Vertex();
        var v33 = new engine.Mesh.Vertex();

        v11.setAttribute(engine.Mesh.Vertex.POSITION, v1);
        v11.setAttribute(engine.Mesh.Vertex.TEXTURE, v1);

        v22.setAttribute(engine.Mesh.Vertex.POSITION, v2);
        v22.setAttribute(engine.Mesh.Vertex.TEXTURE, v2);

        v33.setAttribute(engine.Mesh.Vertex.POSITION, v3);
        v33.setAttribute(engine.Mesh.Vertex.TEXTURE, v3);

        this.addVertex(v11);
        this.addVertex(v22);
        this.addVertex(v33);
    }

}
