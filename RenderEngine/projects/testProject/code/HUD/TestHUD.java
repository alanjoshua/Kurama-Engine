package HUD;

import engine.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import engine.DataStructure.Mesh.Mesh;
import engine.GUI.Text;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;
import engine.model.HUD;
import engine.model.Model;

import java.awt.*;
import java.util.Arrays;

public class TestHUD extends HUD {

    public Text demoText;
    public Model texquad;
    Font FONT = new Font("Arial", Font.PLAIN, 25);
    String CHARSET = "ISO-8859-1";

    public TestHUD(Game game) {
        super(game);

        Model texquad = game.scene.createModel(game.scene.loadMesh("res/misc/quad.obj",
                "HUD_texquad_mesh", null), "texQuad",
                Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));

        for (int i = 0; i < texquad.meshes.get(0).vertAttributes.get(Mesh.TEXTURE).size(); i++) {
            Vector v = texquad.meshes.get(0).vertAttributes.get(Mesh.TEXTURE).get(i);
            v = v.removeDimensionFromVec(2);
            texquad.meshes.get(0).vertAttributes.get(Mesh.TEXTURE).set(i, v);
        }

        for (int i = 0; i < texquad.meshes.get(0).vertAttributes.get(Mesh.POSITION).size(); i++) {
            Vector v = texquad.meshes.get(0).vertAttributes.get(Mesh.POSITION).get(i);
            texquad.meshes.get(0).vertAttributes.get(Mesh.POSITION).set(i, v);
        }

        texquad.setScale(300);
        texquad.meshes.forEach(Mesh::initOpenGLMeshData);
        texquad.shouldCastShadow =false;
        texquad.shouldGreyScale = true;
        texquad.shouldLinearizeDepthInHUD = true;
        texquad.shouldRender = false;
        hudElements.add(texquad);

        demoText = new Text(game, "Kurama Engine -alpha 2.1", new FontTexture(FONT,CHARSET), "HUD_text");
        demoText.meshes.get(0).meshIdentifier = "hud_text_mesh";
        game.scene.setUniqueMeshID(demoText.meshes.get(0));

        String temp = demoText.fontTexture.font.getAttributes().toString();
//        System.out.println(temp);
//        Font f = Font.decode(temp);
//        System.out.println(f.getFontName());
//        Font f1 = new Font();

        demoText.meshes.get(0).materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});

        demoText.shouldGreyScale = false;
        demoText.shouldLinearizeDepthInHUD = false;
        hudElements.add(demoText);

        game.scene.addModel(demoText, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));

        texquad.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), -180).multiply(texquad.getOrientation()));
        texquad.setPos(new Vector(new float[]{500,game.getDisplay().getHeight() - 500,0}));
        demoText.setPos(new Vector(new float[]{game.getDisplay().getWidth() - 400,game.getDisplay().getHeight() - 50,0}));

    }

    public void tick() {
//        demoText.setPos(demoText.getPos().sub(new Vector(new float[]{1, 0, 0})));
    }

}
