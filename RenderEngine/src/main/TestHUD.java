package main;

import engine.DataStructure.Mesh.Mesh;
import engine.GUI.Text;
import engine.Math.Quaternion;
import engine.model.HUD;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;
import engine.model.MeshBuilder;
import engine.model.Model;

import java.awt.*;

public class TestHUD extends HUD {

    public Text demoText;
    public Model texquad;
    Font FONT = new Font("Arial", Font.PLAIN, 25);
    String CHARSET = "ISO-8859-1";

    public TestHUD(Game game) {
        super(game);

        texquad = new Model(game, MeshBuilder.buildModelFromFileGL("res/misc/quad.obj",null),"quad");
//        texquad.mesh.materials.get(0).ambientColor = new Vector(1,1,1,1);
        texquad.mesh = MeshBuilder.triangulate(texquad.mesh,false);
        texquad.mesh = MeshBuilder.bakeMesh(texquad.mesh,null);

        for (int i = 0; i < texquad.mesh.vertAttributes.get(Mesh.TEXTURE).size(); i++) {
            Vector v = texquad.mesh.vertAttributes.get(Mesh.TEXTURE).get(i);
            v = v.removeDimensionFromVec(2);
            texquad.mesh.vertAttributes.get(Mesh.TEXTURE).set(i, v);
        }

        for (int i = 0; i < texquad.mesh.vertAttributes.get(Mesh.POSITION).size(); i++) {
            Vector v = texquad.mesh.vertAttributes.get(Mesh.POSITION).get(i);
//            v = v.add(new Vector(new float[]{1f,1f,0,0}));
//            v = v.mul(new Vector(new float[]{500,500,1,1}));
            texquad.mesh.vertAttributes.get(Mesh.POSITION).set(i, v);
        }

        texquad.setScale(300);
        texquad.mesh.initOpenGLMeshData();
        texquad.isOpaque=false;
        texquad.shouldGreyScale = true;
        texquad.shouldLinearizeDepthInHUD = true;
        texquad.shouldRender = false;
        hudElements.add(texquad);

        demoText = new Text(game, "Kurama Engine -alpha 2.0", new FontTexture(FONT,CHARSET), "sample");
        demoText.mesh.materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        System.out.println("Demo tex id: "+demoText.fontTexture.texture.getId());
//        System.out.println("QUAD ID: "+texquad.mesh.materials.get(0).texture.getId());
        demoText.shouldGreyScale = false;
        demoText.shouldLinearizeDepthInHUD = false;
        hudElements.add(demoText);

        texquad.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), -180).multiply(texquad.getOrientation()));

        texquad.setPos(new Vector(new float[]{500,game.getDisplay().getHeight() - 500,0}));
        demoText.setPos(new Vector(new float[]{game.getDisplay().getWidth() - 400,game.getDisplay().getHeight() - 50,0}));
    }

    public void tick() {
//        demoText.setPos(demoText.getPos().sub(new Vector(new float[]{1, 0, 0})));
    }

}
