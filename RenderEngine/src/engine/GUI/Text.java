package engine.GUI;

import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;
import engine.DataStructure.Texture;
import engine.game.Game;
import engine.model.Model;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import engine.Math.Vector;
import engine.model.ModelBuilder;

public class Text extends Model {

    public static final float ZPOS = 0f;
    public static final int VERTICES_PER_QUAD = 4;
    protected String text;
    public final int numCols;
    public final int numRows;

    public Text(Game game, String text, String fontFileName, int numCols, int numRows, String identifier) {
        super(game,null,identifier);
        this.text = text;
        this.numCols = numCols;
        this.numRows = numRows;
        Texture texture = null;
        try {
            texture = new Texture(fontFileName);
        }catch(Exception e) {
            e.printStackTrace();
        }
        this.mesh = buildMesh(texture, numCols, numRows);
    }

    public Mesh buildMesh(Texture texture, int numCols, int numRows) {
        byte[] chars = text.getBytes(Charset.forName("ISO-8859-1"));
        int numChars = chars.length;

        List<Vector> positions = new ArrayList<>();
        List<Vector> textCoords = new ArrayList<>();
        List<Integer> indices= new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        float tileWidth = (float)texture.width / (float)numCols;
        float tileHeight = (float)texture.height / (float)numRows;

        for(int i=0; i<numChars; i++) {
            byte currChar = chars[i];

            int col = (currChar) % (numCols);
            int row = (currChar) / (numCols);

            Vector pos;
            Vector tex;
            int index;
            Vertex tl = new Vertex();
            Vertex tr = new Vertex();
            Vertex bl = new Vertex();
            Vertex br = new Vertex();
            Face f1 = new Face();

            // Build a character tile composed by two triangles

            // Left Top vertex
            pos = new Vector(new float[]{i*tileWidth,0,ZPOS,1});
            tex = new Vector(new float[]{(float)col / (float)numCols,(float)row / (float)numRows});
            //tex = new Vector(new float[]{col * tileWidth/texture.width, row * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD;
            tl.setAttribute(index,Vertex.POSITION);
            tl.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);

            // Left Bottom vertex
            pos = new Vector(new float[]{(float)i*tileWidth, tileHeight,ZPOS,1});
            tex = new Vector(new float[]{(float)col / (float)numCols,(float)(row + 1) / (float)numRows});
            //tex = new Vector(new float[]{col * tileWidth/texture.width, (row+1) * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD + 1;
            indices.add(index);
            bl.setAttribute(index, Vertex.POSITION);
            bl.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);

            // Right Bottom vertex
            pos = new Vector(new float[]{(float)i*tileWidth + tileWidth, tileHeight,ZPOS,1});
            tex = new Vector(new float[]{(float)(col + 1)/ (float)numCols,(float)(row + 1) / (float)numRows});
            //tex = new Vector(new float[]{(col+1) * tileWidth/texture.width, (row+1) * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD + 2;
            indices.add(index);
            br.setAttribute(index, Vertex.POSITION);
            br.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);


            // Right Top vertex
            pos = new Vector(new float[]{(float)i*tileWidth + tileWidth, 0,ZPOS,1});
            tex = new Vector(new float[]{(float)(col + 1)/ (float)numCols,(float)row / (float)numRows});
            //tex = new Vector(new float[]{(col+1) * tileWidth/texture.width, row * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD + 3;
            indices.add(index);
            tr.setAttribute(index, Vertex.POSITION);
            tr.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);

            f1.addVertex(tl);
            f1.addVertex(bl);
            f1.addVertex(br);
            f1.addVertex(tr);

            faces.add(f1);
        }

        List<List<Vector>> vertAttribs = new ArrayList<>();
        vertAttribs.add(positions);
        vertAttribs.add(textCoords);

        Mesh res = new Mesh(null,faces,vertAttribs);
        res = ModelBuilder.triangulate(res,false);
        res = ModelBuilder.bakeMesh(res,null);
        res.initOpenGLMeshData();
        res.material.texture = texture;
        return res;
    }

    public void setText(String text) {
        this.text = text;
        Texture texture = this.mesh.material.texture;
        this.mesh.deleteBuffers();
        this.mesh = buildMesh(texture, numCols, numRows);
    }

}
