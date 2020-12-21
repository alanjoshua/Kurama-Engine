package engine.GUI;

import engine.Mesh.Face;
import engine.Mesh.Mesh;
import engine.Mesh.Vertex;
import engine.Effects.Material;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;
import engine.geometry.MeshBuilder;
import engine.model.Model;
import engine.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Text extends Model {

    public static final float ZPOS = 0f;
    public static final int VERTICES_PER_QUAD = 4;
    public String text;
    public FontTexture fontTexture;
    public float width;

    public Text(Game game, String text, Mesh mesh, FontTexture fontTexture, float width, String modelIdentifier) {
        super(game, mesh, modelIdentifier);
        this.text = text;
        this.fontTexture = fontTexture;
        this.width = width;
    }

    public static List<Object> buildTextMesh(String text, FontTexture fontTexture) {
        List<Vector> positions = new ArrayList<>();
        List<Vector> textCoords = new ArrayList<>();
        List<Integer> indices= new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        char[] characters = text.toCharArray();
        int numChars = characters.length;

        float startx = 0;
        for(int i=0; i<numChars; i++) {
            FontTexture.CharInfo charInfo = fontTexture.getCharInfo(characters[i]);

            Vector pos;
            Vector tex;
            int index;
            Vertex tl = new Vertex();
            Vertex tr = new Vertex();
            Vertex bl = new Vertex();
            Vertex br = new Vertex();
            tl.setAttribute(0,Vertex.MATERIAL);
            tr.setAttribute(0,Vertex.MATERIAL);
            bl.setAttribute(0,Vertex.MATERIAL);
            br.setAttribute(0,Vertex.MATERIAL);
            Face f1 = new Face();

            // Build a character tile composed by two triangles

            // Left Top vertex
            pos = new Vector(new float[]{startx,0,ZPOS,1});
            tex = new Vector(new float[]{(float)charInfo.startX / (float)fontTexture.width,0});
            //tex = new Vector(new float[]{col * tileWidth/texture.width, row * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD;
            tl.setAttribute(index,Vertex.POSITION);
            tl.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);

            // Left Bottom vertex
            pos = new Vector(new float[]{startx,fontTexture.height,ZPOS,1});
            tex = new Vector(new float[]{(float)charInfo.startX / (float)fontTexture.width,1});
            //tex = new Vector(new float[]{col * tileWidth/texture.width, (row+1) * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD + 1;
            indices.add(index);
            bl.setAttribute(index, Vertex.POSITION);
            bl.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);

            // Right Bottom vertex
            pos = new Vector(new float[]{startx + charInfo.width, fontTexture.height,ZPOS,1});
            tex = new Vector(new float[]{((float)charInfo.startX + charInfo.width) / (float)fontTexture.width,1});
            //tex = new Vector(new float[]{(col+1) * tileWidth/texture.width, (row+1) * tileHeight/texture.height});
            index = i*VERTICES_PER_QUAD + 2;
            indices.add(index);
            br.setAttribute(index, Vertex.POSITION);
            br.setAttribute(index, Vertex.TEXTURE);
            positions.add(pos);
            textCoords.add(tex);
            indices.add(index);


            // Right Top vertex
            pos = new Vector(new float[]{startx + charInfo.width, 0,ZPOS,1});
            tex = new Vector(new float[]{((float)charInfo.startX + charInfo.width) / (float)fontTexture.width,0});
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
            startx += charInfo.width;
        }
        float width = startx;

        List<List<Vector>> vertAttribs = new ArrayList<>();
        vertAttribs.add(positions);
        vertAttribs.add(textCoords);

        Mesh res = new Mesh(null,faces,vertAttribs,null, null, null);
        res = MeshBuilder.triangulate(res,false, null);
        res = MeshBuilder.bakeMesh(res,null);

        Material textMat = new Material();
        textMat.texture = fontTexture.texture;
        textMat.matName = Utils.getUniqueID();
        res.materials.set(0, textMat);
        res.initOpenGLMeshData();

        res.meshIdentifier = Utils.getUniqueID();
        List<Object> results = new ArrayList<>();
        results.add(res);
        results.add(new Float(width));
        return results;
    }

//    public void setText(String text) {
//        this.text = text;
//        this.meshes.forEach(m -> m.deleteBuffers());
//        this.meshes = new ArrayList<>();
//        this.meshes.add(buildMesh());
//    }

}
