package Kurama.geometry.MD5;

import Kurama.Mesh.Face;
import Kurama.Mesh.Vertex;

public class Triangle extends Face {

    public int index;

    public Triangle(int index, int v1, int v2, int v3) {
        this.index = index;

        var v11 = new Kurama.Mesh.Vertex();
        var v22 = new Kurama.Mesh.Vertex();
        var v33 = new Kurama.Mesh.Vertex();

        v11.setAttribute(v1, Kurama.Mesh.Vertex.POSITION);
        v11.setAttribute(v1, Kurama.Mesh.Vertex.TEXTURE);
        v11.setAttribute(v1, Vertex.NORMAL);
        v11.setAttribute(v1, Vertex.WEIGHTBIASESPERVERT);
        v11.setAttribute(v1, Vertex.JOINTINDICESPERVERT);

        v22.setAttribute(v2, Kurama.Mesh.Vertex.POSITION);
        v22.setAttribute(v2, Kurama.Mesh.Vertex.TEXTURE);
        v22.setAttribute(v2, Vertex.NORMAL);
        v22.setAttribute(v2, Vertex.WEIGHTBIASESPERVERT);
        v22.setAttribute(v2, Vertex.JOINTINDICESPERVERT);

        v33.setAttribute(v3, Kurama.Mesh.Vertex.POSITION);
        v33.setAttribute(v3, Kurama.Mesh.Vertex.TEXTURE);
        v33.setAttribute(v3, Vertex.NORMAL);
        v33.setAttribute(v3, Vertex.WEIGHTBIASESPERVERT);
        v33.setAttribute(v3, Vertex.JOINTINDICESPERVERT);

        this.addVertex(v11);
        this.addVertex(v22);
        this.addVertex(v33);
    }

}
