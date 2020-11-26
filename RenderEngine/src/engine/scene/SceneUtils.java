package engine.scene;

import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;
import engine.DataStructure.Texture;
import engine.Effects.Fog;
import engine.Effects.Material;
import engine.Effects.ShadowMap;
import engine.GUI.Text;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.camera.Camera;
import engine.font.FontTexture;
import engine.game.Game;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.HUD;
import engine.model.MeshBuilder;
import engine.model.ModelBehaviour;
import engine.model.Model;
import engine.renderingEngine.RenderPipeline;
import engine.utils.Logger;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class SceneUtils {

    public static RenderPipeline getRenderPipeline(String  renderPipelineClass_name, Game game) {
        Logger.log("Retrieving rendering pipeline...");
        try {
            Class renderPipeline_class = Class.forName(renderPipelineClass_name);
            Constructor  constructor = renderPipeline_class.getConstructor(new Class[]{Game.class});
            RenderPipeline renderPipeline = (RenderPipeline) constructor.newInstance(new Object[]{game});
            return renderPipeline;

        }  catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HUD getHUD(String  HUD_class_name, Game game) {
        Logger.log("Retrieving HUD...");
        try {
            Class hud_class = Class.forName(HUD_class_name);
            Constructor  constructor = hud_class.getConstructor(new Class[]{Game.class});
            HUD hud = (HUD) constructor.newInstance(new Object[]{game});
            return hud;

        }  catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ModelBehaviour getMiniBehaviour(String behaviour_classname, Game game) {
        Logger.log("Retrieving model behaviour...");
        try {
            Class behaviour_class = Class.forName(behaviour_classname);
            Constructor  constructor = behaviour_class.getConstructor();
            ModelBehaviour beh = (ModelBehaviour) constructor.newInstance();
            return beh;

        }  catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Scene loadScene(Game game, String directory) {
        Logger.log("Verifying project structure...");
        if(!verifyProjectStructure(directory)) {
            return null;
        }
        Logger.log("Project structure is valid");

        Logger.log("Parsing material library...");
        Map<String, Material> materials_map;
        try {
            materials_map = MeshBuilder.parseMaterialLibrary(directory + "/KE_Files/matLibrary.mtl", directory + "/models/textures/");
        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (materials_map == null) {
            return null;
        }
        for(String m: materials_map.keySet()) {
            Logger.log("matname: "+m);
        }
        Logger.log("Successfully parsed library");

        Scene scene = new Scene(game);
        boolean isLoadingMeshes = false;
        boolean isLoadingModels = false;
        boolean isLoadingCamera = false;
        boolean isLoadingFog = false;
        boolean isLoadingPointLights = false;

        String skyBoxID = null;
        Vector ambientColor = new Vector(new float[]{1,1,1,1});

        try(BufferedReader reader = new BufferedReader(new FileReader(new File(directory+"/KE_Files/master.ke")))) {
            String line;
            while((line=reader.readLine()) != null) {

                String[] tokens = line.split(":");

                if(tokens[0].equalsIgnoreCase("renderPipeline_class")) {
                    scene.renderPipeline = getRenderPipeline(tokens[1], game);
                    if(scene.renderPipeline == null)  {
                        Logger.logError("Was not able to load RenderPipeline class. Returning  null...");
                        return null;
                    }
                    Logger.log("Successfully loaded renderPipeline");
                }

                if(tokens[0].equalsIgnoreCase("HUD_class")) {
                    scene.hud = getHUD(tokens[1], game);
                    if(scene.hud == null)  {
                        Logger.logError("Was not able to load HUD class. Setting as null...");
                    }
                    Logger.log("Successfully loaded HUD");
                }

                if(tokens[0].equalsIgnoreCase("skybox_id")) {
                    skyBoxID = tokens[1];
                }

                if(tokens[0].equalsIgnoreCase("ambientColor")) {
                    List<Float> val = new ArrayList<>();
                    String[] token3 = tokens[1].split(" ");

                    for(int i = 0; i < token3.length; i++) {
                        val.add(Float.parseFloat(token3[i]));
                    }
                    ambientColor = new Vector(val);
                }


                if(line.equalsIgnoreCase("MESH INFO")) {
                    Logger.log("Loading meshes now");
                    isLoadingMeshes = true;
                    isLoadingModels = false;
                    isLoadingCamera = false;
                    isLoadingFog = false;
                    isLoadingPointLights = false;
                }
                else if (line.equalsIgnoreCase("MODEL INFO")) {
                    Logger.log("Loading models now");
                    isLoadingMeshes = false;
                    isLoadingModels = true;
                    isLoadingCamera = false;
                    isLoadingFog = false;
                    isLoadingPointLights = false;
                }

                else if (line.equalsIgnoreCase("CAMERA INFO")) {
                    Logger.log("Loading camera now");
                    isLoadingMeshes = false;
                    isLoadingModels = false;
                    isLoadingCamera = true;
                    isLoadingFog = false;
                    isLoadingPointLights = false;
                }

                else if (line.equalsIgnoreCase("FOG INFO")) {
                    Logger.log("Loading fog now");
                    isLoadingMeshes = false;
                    isLoadingModels = false;
                    isLoadingCamera = false;
                    isLoadingFog = true;
                    isLoadingPointLights = false;
                }

                else if (line.equalsIgnoreCase("POINTLIGHTS INFO")) {
                    Logger.log("Loading pointlights now");
                    isLoadingMeshes = false;
                    isLoadingModels = false;
                    isLoadingCamera = false;
                    isLoadingFog = false;
                    isLoadingPointLights = true;
                }

                if(isLoadingMeshes) {
                    if(line.equalsIgnoreCase("start new mesh")) {
                        String line2;
                        String id = null;
                        String location = null;

                        while(!(line2 = reader.readLine()).equals("")) {
//                            Logger.log("line2: "+line2);
                            String[] tokens2 = line2.split(":");

                            switch (tokens2[0]) {
                                case "ID" :
                                    id = tokens2[1];
                                    break;
                                case "location":
                                    location = tokens2[1];
                                    break;
                            }
                        }
                        if(!location.equals("null")) {
                            try {
                                Mesh newMesh = loadKEOBJ(directory + "/models/meshes/" + location, id, materials_map);
                                if(newMesh == null) {
                                    Logger.logError("Mesh ID:"+id+" returned as null");
                                }
                                else {
                                    scene.addMesh(newMesh);
                                }
                            }catch (Exception e) {
                                e.printStackTrace();
                                Logger.logError(directory + "/models/meshes/" + location);
                            }
                        }
                    }
                }

                if(isLoadingFog) {
                    Vector color = null;
                    float density = 0;
                    boolean active = false;

                    String line2;
                    while(!(line2 = reader.readLine()).equals("")) {
                        String[] tokens2 = line2.split(":");
                        switch (tokens2[0]) {
                            case "density:":
                                density = Float.parseFloat(tokens2[1]);
                                break;
                            case "active":
                                active = Boolean.parseBoolean(tokens2[1]);
                                break;
                            case "color":
                                List<Float> val = new ArrayList<>();
                                String[] token3 = tokens2[1].split(" ");

                                for(int i = 0; i < token3.length; i++) {
                                    val.add(Float.parseFloat(token3[i]));
                                }
                                color = new Vector(val);
                                break;
                        }
                    }
                    scene.fog = new Fog(active, color, density);
                }

                if(isLoadingCamera) {
                    Vector pos = null;
                    Quaternion orientation = null;
                    float fovX = 0;
                    float near = 0;
                    float far = 1000;
                    int imageWidth=100, imageHeight = 100;

                    String line2;
                    while(!(line2 = reader.readLine()).equals("")) {
                        String[] tokens2 = line2.split(":");

                        switch (tokens2[0]) {
                            case "fovX":
                                fovX = Float.parseFloat(tokens2[1]);
                                break;
                            case "near":
                                near = Float.parseFloat(tokens2[1]);
                                break;
                            case "far":
                                far = Float.parseFloat(tokens2[1]);
                                break;
                            case "imageWidth":
                                imageWidth = Integer.parseInt(tokens2[1]);
                                break;
                            case "imageHeight":
                                imageHeight = Integer.parseInt(tokens2[1]);
                                break;
                            case "pos":
                                List<Float> val = new ArrayList<>();
                                String[] token3 = tokens2[1].split(" ");

                                for(int i = 0; i < token3.length; i++) {
                                    val.add(Float.parseFloat(token3[i]));
                                }
                                pos = new Vector(val);
                                break;

                            case "orientation":
                                val = new ArrayList<>();
                                token3 = tokens2[1].split(" ");

                                for(int i = 0; i < token3.length; i++) {
                                    val.add(Float.parseFloat(token3[i]));
                                }
                                orientation = new Quaternion(new Vector(val));
                                break;
                        }
                    }
                    scene.camera = new Camera(game, orientation, pos, fovX, near, far, imageWidth, imageHeight);
                }

                if(isLoadingPointLights) {
                    if (line.equalsIgnoreCase("start new model")) {
                        Vector pos = null;
                        Vector color = null;
                        float att_constant=1;
                        float att_linear=1;
                        float att_exp=1;
                        float intensity = 1;

                        String line2;
                        while(!(line2 = reader.readLine()).equals("")) {
                            String[] tokens2 = line2.split(":");

                            switch (tokens2[0]) {
                                case "intensity":
                                    intensity = Float.parseFloat(tokens2[1]);
                                    break;
                                case "att_constant":
                                    att_constant = Float.parseFloat(tokens2[1]);
                                    break;
                                case "att_linear":
                                    att_linear = Float.parseFloat(tokens2[1]);
                                    break;
                                case "att_exp":
                                    att_exp = Float.parseFloat(tokens2[1]);
                                    break;
                                case "color":
                                    List<Float> val = new ArrayList<>();
                                    String[] token3 = tokens2[1].split(" ");

                                    for(int i = 0; i < token3.length; i++) {
                                        val.add(Float.parseFloat(token3[i]));
                                    }
                                    color = new Vector(val);
                                    break;

                                case "pos":
                                    val = new ArrayList<>();
                                    token3 = tokens2[1].split(" ");

                                    for(int i = 0; i < token3.length; i++) {
                                        val.add(Float.parseFloat(token3[i]));
                                    }
                                    pos = new Vector(val);
                                    break;
                            }

                            PointLight p = new PointLight(color, pos, intensity,
                                    new PointLight.Attenuation(att_constant, att_linear, att_exp));
                            scene.addPointlight(p);
                        }
                    }
                }

                if(isLoadingModels) {
                    if (line.equalsIgnoreCase("start new model")) {
                        String line2;
                        String id = null;
                        String location = null;
                        List<String> meshIDs = new ArrayList<>();
                        String type = null;
                        Vector scale = null;
                        Vector pos = null;
                        Quaternion orientation = null;
                        List<String> shaderIds = new ArrayList<>();
                        boolean shouldCastShadow = true;
                        boolean shouldRender = true;

                        Vector color = null;
                        float intensity = 1;
                        float lightPosScale = 100;
                        int shadowMapWidth = ShadowMap.DEFAULT_SHADOWMAP_WIDTH;
                        int shadowMapHeight = ShadowMap.DEFAULT_SHADOWMAP_HEIGHT;
                        Matrix shadowProjectMatrix = null;
                        float angle=45;
                        float att_constant=1;
                        float att_linear=1;
                        float att_exp=1;
                        
                        String font_name = null;
                        int font_size = 0;
                        int font_style = 0;

                        String text=null;

                        ModelBehaviour behaviour = null;

                        while(!(line2 = reader.readLine()).equals("")) {
                            String[] tokens2 = line2.split(":");
//                            Logger.log(line2);
//                            Logger.log(tokens2[0]);
                            switch (tokens2[0]) {
                                case "ID":
                                    id = tokens2[1];
                                    break;
                                case "type":
                                    type = tokens2[1];
                                    break;
                                case "mesh_ID":
                                    meshIDs = Arrays.asList(tokens2[1].split(","));
                                    break;
                                case "shouldCastShadow":
                                    shouldCastShadow = Boolean.parseBoolean(tokens2[1]);
                                    break;
                                case "shouldRender":
                                    shouldRender = Boolean.parseBoolean(tokens2[1]);
                                    break;
                                case "angle":
                                    angle = Float.parseFloat(tokens2[1]);
                                    break;
                                case "intensity":
                                    intensity = Float.parseFloat(tokens2[1]);
                                    break;
                                case "att_constant":
                                    att_constant = Float.parseFloat(tokens2[1]);
                                    break;
                                case "att_linear":
                                    att_linear = Float.parseFloat(tokens2[1]);
                                    break;
                                case "att_exp":
                                    att_exp = Float.parseFloat(tokens2[1]);
                                    break;
                                case "shadowMap_width":
                                    shadowMapWidth = Integer.parseInt(tokens2[1]);
                                    break;
                                case "shadowMap_height":
                                    shadowMapHeight = Integer.parseInt(tokens2[1]);
                                    break;
                                case "text":
                                    text = tokens2[1];
                                    break;
                                case "font_name":
                                    font_name = tokens2[1];
                                    break;
                                case "font_size":
                                    font_size = Integer.parseInt(tokens2[1]);
                                    break;
                                case "font_style":
                                    font_style = Integer.parseInt(tokens2[1]);
                                    break;

                                case "modelBehaviour":
                                    if(!tokens2[1].equals("null")) {
                                        behaviour = getMiniBehaviour(tokens2[1], game);
                                        if (behaviour == null) {
                                            Logger.logError("Was not able to load Model behaviour class. Returning  null...");
                                            return null;
                                        }
                                        Logger.log("Successfully loaded model behaviour");
                                    }
                                    break;

                                case "shader_ID":
                                    String withoutBrackets = tokens2[1].substring(1, tokens2[1].length()-1);
                                    String[] shaders = withoutBrackets.split(",");
                                    for(int i = 0; i < shaders.length; i++) {
                                        String shader_rev = shaders[i].strip();
                                        shaderIds.add(shader_rev);
                                    }
                                    break;

                                case "scale":
                                    List<Float> val = new ArrayList<>();
                                    String[] token3 = tokens2[1].split(" ");

                                    for(int i = 0; i < token3.length; i++) {
                                        val.add(Float.parseFloat(token3[i]));
                                    }
                                    scale = new Vector(val);
                                    break;

                                case "color":
                                    val = new ArrayList<>();
                                    token3 = tokens2[1].split(" ");

                                    for(int i = 0; i < token3.length; i++) {
                                        val.add(Float.parseFloat(token3[i]));
                                    }
                                    color = new Vector(val);
                                    break;

                                case "pos":
                                    val = new ArrayList<>();
                                    token3 = tokens2[1].split(" ");

                                    for(int i = 0; i < token3.length; i++) {
                                        val.add(Float.parseFloat(token3[i]));
                                    }
                                    pos = new Vector(val);
                                    break;

                                case "orientation":
                                    val = new ArrayList<>();
                                    token3 = tokens2[1].split(" ");

                                    for(int i = 0; i < token3.length; i++) {
                                        val.add(Float.parseFloat(token3[i]));
                                    }
                                    orientation = new Quaternion(new Vector(val));
                                    break;

                                case "shadowProjectionMatrix":
                                    // Assume matrix is 4 x 4
                                    String[] rows = tokens2[1].split(",");

                                    if(rows.length != 4) {
                                        Logger.logError("Number of rows of shadow Project matrix is not 4. Exiting...");
                                        System.exit(1);
                                    }

                                    float[][] vals = new float[4][4];

                                    for(int i = 0;i < rows.length; i++) {
                                        String[] cols = rows[i].split(" ");

                                        if(cols.length != 4) {
                                            Logger.logError("Number of cols of shadow Project matrix is not 4. Exiting...");
                                            System.exit(1);
                                        }

                                        for(int j = 0;j < cols.length;j++) {
                                            vals[i][j] = Float.parseFloat(cols[j]);
                                        }
                                    }
                                    shadowProjectMatrix = new Matrix(vals);
                                    break;
                            }

                        }

                        List<Mesh> meshes = new ArrayList<>();
                        for(String meshId: meshIDs) {
                            meshes.add(scene.meshID_mesh_map.get(meshId));
                        }

                        if(type.equals("DirectionalLight")) {
                            DirectionalLight l;

                            l = new DirectionalLight(game, color, orientation, intensity,
                                    new ShadowMap(shadowMapWidth, shadowMapHeight), meshes,
                                    null, shadowProjectMatrix, id);

                            l.setPos(pos);
                            l.setScale(scale);
                            l.shouldRender = shouldRender;
                            l.shouldCastShadow = shouldCastShadow;
                            l.lightPosScale = lightPosScale;
                            l.setBehaviour(behaviour);
                            scene.addDirectionalLight(l, shaderIds);
                        }

                        else if(type.equals("SpotLight")) {
                            SpotLight s;

                            s = new SpotLight(game, new PointLight(color, pos, intensity, new PointLight.Attenuation(att_constant, att_linear, att_exp)),
                                    orientation, angle, new ShadowMap(shadowMapWidth, shadowMapHeight),
                                    meshes, null, shadowProjectMatrix, id);
                            s.setPos(pos);
                            s.setScale(scale);
                            s.shouldRender = shouldRender;
                            s.shouldCastShadow = shouldCastShadow;
                            s.setBehaviour(behaviour);
                            scene.addSplotLight(s, shaderIds);
                        }

                        else if(type.equals("Text")) {
                            Text s;
                            Mesh mesh;

                            Font font = new Font(font_name, font_style, font_size);
                            s = new Text(game, text, new FontTexture(font, "ISO-8859-1"), id);

                            if(meshes.size() > 0) {
                                mesh = meshes.get(0); //Assumes Text will only have one mesh
                                s.meshes.get(0).meshIdentifier = mesh.meshIdentifier;

                                // Do this only if material list size are the same
                                if(mesh.materials.size() == s.meshes.get(0).materials.size()) {
                                    for (int i =0;i < mesh.materials.size();i++) {
                                        mesh.materials.get(i).texture = s.meshes.get(0).materials.get(i).texture;
                                        s.meshes.get(0).materials.set(i, mesh.materials.get(i));
                                    }
                                }

                            }

                            s.setPos(pos);
                            s.setScale(scale);
                            s.shouldRender = shouldRender;
                            s.shouldCastShadow = shouldCastShadow;
                            s.setBehaviour(behaviour);
                            scene.addModel(s, shaderIds);
                        }

                        else {
                            Model m;
                            m = new Model(game, meshes, id);
                            m.setPos(pos);
                            m.setScale(scale);
                            m.shouldRender = shouldRender;
                            m.shouldCastShadow = shouldCastShadow;
                            m.setBehaviour(behaviour);
                            scene.addModel(m, shaderIds);
                        }

                    }

                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        scene.skybox = scene.modelID_model_map.get(skyBoxID);
        scene.ambientLight = ambientColor;
        return scene;
    }

    public static Mesh loadKEOBJ(String file, String id, Map<String, Material> materialsMap) {
        Mesh ret = null;

        try(BufferedReader reader = new BufferedReader(new FileReader(new File(file)))) {
            String line;

            List<Vector> pos = new ArrayList<>();
            List<Vector> tex = new ArrayList<>();
            List<Vector> normals = new ArrayList<>();
            List<Vector> colors = new ArrayList<>();
            List<Vector> tangents = new ArrayList<>();
            List<Vector> bitangents = new ArrayList<>();
            List<Vector> materialInds = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();

            Map<Integer, String> matIndexMap = new HashMap<>();

            String meshID = null;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(":");

                if(tokens[0].equals("MESH_ID")) {
                    meshID = tokens[1];
                }

                if(line.equals("MATERIALS_MAP")) {
                    String line2;
                    while(!(line2 = reader.readLine()).equals("")) {
                        String[] tokens2 = line2.split(":");
                        matIndexMap.put(Integer.parseInt(tokens2[0]), tokens2[1]);
                    }
                }

                if(line.equals("VERTEX POSITIONS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        String[] tokens2 = line2.split(" ");
                        List<Float> val = new ArrayList<>();

                        for(int i = 0; i < tokens2.length; i++) {
                            val.add(Float.parseFloat(tokens2[i]));
                        }
                       pos.add(new Vector(val));
                    }
                }

                if(line.equals("TEXTURE POSITIONS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        if(!line2.equals("null")) {
                            String[] tokens2 = line2.split(" ");
                            List<Float> val = new ArrayList<>();

                            for (int i = 0; i < tokens2.length; i++) {
                                val.add(Float.parseFloat(tokens2[i]));
                            }
                            tex.add(new Vector(val));
                        }
                    }
                }

                if(line.equals("NORMAL POSITIONS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        if(!line2.equals("null")) {
                            String[] tokens2 = line2.split(" ");
                            List<Float> val = new ArrayList<>();

                            for (int i = 0; i < tokens2.length; i++) {
                                val.add(Float.parseFloat(tokens2[i]));
                            }
                            normals.add(new Vector(val));
                        }
                    }
                }

                if(line.equals("COLORS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        if(!line2.equals("null")) {
                            String[] tokens2 = line2.split(" ");
                            List<Float> val = new ArrayList<>();

                            for (int i = 0; i < tokens2.length; i++) {
                                val.add(Float.parseFloat(tokens2[i]));
                            }
                            colors.add(new Vector(val));
                        }
                    }
                }

                if(line.equals("TANGENTS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        if(!line2.equals("null")) {
                            String[] tokens2 = line2.split(" ");
                            List<Float> val = new ArrayList<>();

                            for (int i = 0; i < tokens2.length; i++) {
                                val.add(Float.parseFloat(tokens2[i]));
                            }
                            tangents.add(new Vector(val));
                        }
                    }
                }

                if(line.equals("BI-TANGENTS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        if(!line2.equals("null")) {
                            String[] tokens2 = line2.split(" ");
                            List<Float> val = new ArrayList<>();

                            for (int i = 0; i < tokens2.length; i++) {
                                val.add(Float.parseFloat(tokens2[i]));
                            }
                            bitangents.add(new Vector(val));
                        }
                    }
                }

                if(line.equals("MATERIALS")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        String[] tokens2 = line2.split(" ");
                        List<Float> val = new ArrayList<>();

                        for(int i = 0; i < tokens2.length; i++) {
                            val.add(Float.parseFloat(tokens2[i]));
                        }
                        materialInds.add(new Vector(val));
                    }
                }

                if(line.equals("INDICES")) {
                    String line2;

                    while(!(line2 = reader.readLine()).equals("")) {
                        indices.add(Integer.parseInt(line2));
                    }
                }

                List<Material> materials = new ArrayList<>();
                for(int i = 0;i < matIndexMap.size();i++) {
                    String matKey = meshID+"|"+matIndexMap.get(new Integer(i));
                    Material m = materialsMap.get(matKey);
                    materials.add(m);
                }

                List<List<Vector>> vertAttributes = new ArrayList<>();
                for(int i = 0;i < 7; i++) {
                    vertAttributes.add(null);
                }
                vertAttributes.set(Mesh.POSITION, pos);
                vertAttributes.set(Mesh.TEXTURE, tex);
                vertAttributes.set(Mesh.NORMAL, normals);
                vertAttributes.set(Mesh.COLOR, colors);
                vertAttributes.set(Mesh.TANGENT, tangents);
                vertAttributes.set(Mesh.BITANGENT, bitangents);
                vertAttributes.set(Mesh.MATERIAL, materialInds);

                List<Face> newFaces = new ArrayList<>();

        //		Create new vertices and faces using new index list
                for(int i = 0;i < indices.size();i+=3) {
                    Face temp = new Face();
                    for(int k = 0;k < 3;k++) {
                        Vertex v = new Vertex();
                        for (int j = 0; j < vertAttributes.size(); j++) {
                            v.setAttribute(indices.get(i + k), j);
                        }
                        temp.addVertex(v);
                    }
                    newFaces.add(temp);
                }

                ret = new Mesh(indices, newFaces, vertAttributes, materials, file, null);
                ret.meshIdentifier = meshID;
                ret.initOpenGLMeshData();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static boolean verifyProjectStructure(String directory) {
        File saveFolder = new File(directory);
        if (!saveFolder.exists()) {
            Logger.logError("Save directory does not exist. Please provide the correct folder to load from...");
            return false;
        }

        if(!new File(directory+"/code").exists()) {
            Logger.logError("Code directory does not exist.");
            return false;
        }

        if(!new File(directory+"/code/HUD").exists()) {
            Logger.logError("HUD directory does not exist.");
            return false;
        }

        if(!new File(directory+"/code/ModelBehaviour").exists()) {
            Logger.logError("ModelBehaviour directory does not exist.");
            return false;
        }

        if(!new File(directory+"/code/RenderPipeline").exists()) {
            Logger.logError("RenderPipeline directory does not exist.");
            return false;
        }

        if(!new File(directory+"/KE_Files").exists()) {
            Logger.logError("KE_Files directory does not exist.");
            return false;
        }

        if(!new File(directory+"/KE_Files/master.ke").exists()) {
            Logger.logError("master.ke does not exist.");
            return false;
        }

        if(!new File(directory+"/KE_Files/matLibrary.mtl").exists()) {
            Logger.logError("matLibrary.mtl does not exist.");
            return false;
        }

        if(!new File(directory+"/models").exists()) {
            Logger.logError("Models directory does not exist.");
            return false;
        }

        if(!new File(directory+"/models/meshes").exists()) {
            Logger.logError("meshes directory does not exist.");
            return false;
        }

        if(!new File(directory+"/models/textures").exists()) {
            Logger.logError("textures directory does not exist.");
            return false;
        }

        if(!new File(directory+"/Shaders").exists()) {
            Logger.logError("Shaders directory does not exist.");
            return false;
        }

        return true;
    }

    public static boolean writeSceneToKE(Scene scene, String directory, String filePrefix, String shadersDirectory,
                                      String RenderBlockDirectory, String hudDirectory, String modelBehaviourDirectory,
                                         String engineVersion) {

        if(!createProjectStructure(directory, filePrefix)) {
            return false;
        }

        if(!copyShaders(directory, filePrefix, shadersDirectory)) {
            return false;
        }

        if(!copyRenderBlocks(directory, filePrefix, RenderBlockDirectory)) {
            return false;
        }

        if(!copyHUDFiles(directory, filePrefix, hudDirectory)) {
            return false;
        }

        if(!copyModelBehaviourFiles(directory, filePrefix, modelBehaviourDirectory)) {
            return false;
        }

        if(!write_keOBJ_meshes(scene.meshID_mesh_map, directory, filePrefix)) {
            return false;
        }

        if(!writeMaterialFile(scene.meshID_mesh_map, directory, filePrefix, engineVersion)) {
            return false;
        }

        if(!writeMasterKEFile(scene, directory, filePrefix, engineVersion)) {
            return false;
        }

        return true;
    }

    public static boolean copyShaders(String directory, String filePrefix, String shadersDirectory) {
        Logger.log("Copying shaders...");

        if(shadersDirectory == null) {
            Logger.log("Shaders directory is null. Not copying anything...");
            return true;
        }

        File shadersFolder = new File(shadersDirectory);

        if(shadersFolder.isDirectory()) {
            for (File source: shadersFolder.listFiles()) {
                File dest = new File(directory+"/"+filePrefix+"/Shaders/"+source.getName());

                if (!source.equals(dest)) {  // Copy files only they are not the same file

                    if (dest.exists()) {
                        Logger.logError("Resource file already exists at destination. Deleting it...");
                        dest.delete();
                    }
                    try {
                        Files.copy(source.toPath(), dest.toPath());
                    } catch (Exception e) {
                        Logger.logError("Error while copying shader "+source.getName());
                        e.printStackTrace();
                        return false;
                    }
                }
                else {
                    Logger.log(" source and destination shaders are the same. Not overwriting files... "+source.getName());
                }
            }
        }
        return true;
    }

    public static boolean copyRenderBlocks(String directory, String filePrefix, String RenderBlockDirectory) {
        Logger.log("Copying renderBlocks...");

        if(RenderBlockDirectory == null) {
            Logger.log("RenderBlock directory is null. Not copying anything...");
            return true;
        }

        File shadersFolder = new File(RenderBlockDirectory);

        if(shadersFolder.isDirectory()) {
            for (File source: shadersFolder.listFiles()) {
                File dest = new File(directory+"/"+filePrefix+"/code/RenderPipeline/"+source.getName());

                if (!source.equals(dest)) {  // Copy files only they are not the same file

                    if (dest.exists()) {
                        Logger.logError("Resource file already exists at destination. Deleting it...");
                        dest.delete();
                    }
                    try {
                        Files.copy(source.toPath(), dest.toPath());
                    } catch (Exception e) {
                        Logger.logError("Error while copying file "+source.getName());
                        e.printStackTrace();
                        return false;
                    }
                }
                else {
                    Logger.log(" source and destination files are the same. Not overwriting files... "+source.getName());
                }
            }
        }

        return true;
    }

    public static boolean copyHUDFiles(String directory, String filePrefix, String HUDDirectory) {
        Logger.log("Copying HUD files...");

        if(HUDDirectory == null) {
            Logger.log("HUD directory is null. Not copying anything...");
            return true;
        }

        File shadersFolder = new File(HUDDirectory);

        if(shadersFolder.isDirectory()) {
            for (File source: shadersFolder.listFiles()) {
                File dest = new File(directory+"/"+filePrefix+"/code/HUD/"+source.getName());

                if (!source.equals(dest)) {  // Copy files only they are not the same file

                    if (dest.exists()) {
                        Logger.logError("Resource file already exists at destination. Deleting it...");
                        dest.delete();
                    }
                    try {
                        Files.copy(source.toPath(), dest.toPath());
                    } catch (Exception e) {
                        Logger.logError("Error while copying file "+source.getName());
                        e.printStackTrace();
                        return false;
                    }
                }
                else {
                    Logger.log(" source and destination files are the same. Not overwriting files... "+source.getName());
                }
            }
        }

        return true;
    }

    public static boolean copyObjMeshes(Map<String, Mesh> meshes, String directory, String filePrefix) {
        for (Mesh mesh: meshes.values()) {
            if (mesh.meshLocation != null) {
                File source = new File(mesh.meshLocation);
                File dest = new File(directory + "/" + filePrefix + "/models/meshes/" + mesh.meshIdentifier + ".obj");

                if (!source.equals(dest)) {  // Copy files only they are not the same file
                    if (dest.exists()) {
                        Logger.logError("Resource file already exists at destination. Deleting it...");
                        dest.delete();
                    }
                    try {
                        Files.copy(source.toPath(), dest.toPath());
                    } catch (Exception e) {
                        Logger.logError("Error while copying file " + source.getName());
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    Logger.log(" source and destination files are the same. Not overwriting files... " + source.getName());
                }
            }
            else {
                Logger.log("Mesh location is null for meshID: "+mesh.meshIdentifier);
            }
        }

        return true;
    }

    public static boolean write_keOBJ_meshes(Map<String, Mesh> meshes, String directory, String filePrefix) {
        for (Mesh mesh: meshes.values()) {

            File dest = new File(directory + "/" + filePrefix + "/models/meshes/" + mesh.meshIdentifier + ".keObj");
            Logger.log("Current Mesh: "+mesh.meshIdentifier);

            if (mesh.meshLocation != null) {
                File source = new File(mesh.meshLocation);

                if(!source.equals(dest)) {  // Not the same files, so .keObj doesn't yet exist
                    if(dest.exists()) {
                        Logger.log("Loaded from a different source, but prvious version exists, so deleting it...");
                        dest.delete();
                    }
                    if(!write_mesh_as_keObj(mesh, dest)) {
                        return false;
                    }
                }
                else {  //Some previous keObj version exists, but save only if mesh has been modified after loading
                    if(mesh.isModified) {
                        Logger.log("Loaded from previous version, but mesh has been modified, so overwriting file...");
                        dest.delete();
                        if(!write_mesh_as_keObj(mesh, dest)) {
                            return false;
                        }
                    }
                    else {
                        Logger.log("Previous version already exists, but mesh has not yet been modified, so not being saved");
                    }
                }
            }
            else {
                Logger.log("Mesh location is null for meshID: "+mesh.meshIdentifier+". Saving mesh to file...");
                if(!write_mesh_as_keObj(mesh, dest)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean write_mesh_as_keObj(Mesh mesh, File dest) {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(dest))) {

            writer.write("MESH_ID:"+mesh.meshIdentifier+"\n\n");

            writer.write("MATERIALS_MAP\n");
            for(int i = 0; i < mesh.materials.size();i++) {
                writer.write(i+":"+mesh.materials.get(i).matName+"\n");
            }
            writer.newLine();

            writer.write("VERTEX POSITIONS\n");
            for(engine.Math.Vector val: mesh.getAttributeList(Mesh.POSITION)) {
                if (val != null) {
                    writer.write(val.toString());
                }
                else {
                    writer.write("null");
                }
                writer.newLine();
            }
            writer.newLine();

            writer.write("TEXTURE POSITIONS\n");
            for(engine.Math.Vector val: mesh.getAttributeList(Mesh.TEXTURE)) {
                if (val != null) {
                    writer.write(val.toString());
                }
                else {
                    writer.write("null");
                }
                writer.newLine();
            }
            writer.newLine();

            writer.write("NORMAL POSITIONS\n");
            for(engine.Math.Vector val: mesh.getAttributeList(Mesh.NORMAL)) {
                if (val != null) {
                    writer.write(val.toString());
                }
                else {
                    writer.write("null");
                }
                writer.newLine();
            }
            writer.newLine();

            writer.write("COLORS\n");
            if(mesh.getAttributeList(Mesh.COLOR) != null) {
                for (engine.Math.Vector val : mesh.getAttributeList(Mesh.COLOR)) {
                    if (val != null) {
                        writer.write(val.toString());
                    } else {
                        writer.write("null");
                    }
                    writer.newLine();
                }
            }

            writer.newLine();

            writer.write("TANGENTS\n");
            if(mesh.getAttributeList(Mesh.TANGENT) != null) {
                for (engine.Math.Vector val : mesh.getAttributeList(Mesh.TANGENT)) {
                    if (val != null) {
                        writer.write(val.toString());
                    } else {
                        writer.write("null");
                    }
                    writer.newLine();
                }
            }
            writer.newLine();

            writer.write("BI-TANGENTS\n");
            if(mesh.getAttributeList(Mesh.BITANGENT) != null) {
                for (engine.Math.Vector val : mesh.getAttributeList(Mesh.BITANGENT)) {
                    if (val != null) {
                        writer.write(val.toString());
                    } else {
                        writer.write("null");
                    }
                    writer.newLine();
                }
            }
            writer.newLine();

            writer.write("MATERIALS\n");
            for(engine.Math.Vector val: mesh.getAttributeList(Mesh.MATERIAL)) {
                if (val != null) {
                    writer.write(val.toString());
                }
                else {
                    writer.write("null");
                }
                writer.newLine();
            }
            writer.newLine();

            writer.write("INDICES\n");
            for(Integer index: mesh.indices) {
                writer.write(index.intValue()+"");
                writer.newLine();
            }
            writer.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean copyModelBehaviourFiles(String directory, String filePrefix, String modelBehaviourDir) {
        Logger.log("Copying model behaviour files...");

        if(modelBehaviourDir == null) {
            Logger.log("modelBehaviour directory is null. Not copying anything...");
            return true;
        }

        File shadersFolder = new File(modelBehaviourDir);

        if(shadersFolder.isDirectory()) {
            for (File source: shadersFolder.listFiles()) {
                File dest = new File(directory+"/"+filePrefix+"/code/ModelBehaviour/"+source.getName());

                if (!source.equals(dest)) {  // Copy files only they are not the same file

                    if (dest.exists()) {
                        Logger.logError("Resource file already exists at destination. Deleting it...");
                        dest.delete();
                    }
                    try {
                        Files.copy(source.toPath(), dest.toPath());
                    } catch (Exception e) {
                        Logger.logError("Error while copying file "+source.getName());
                        e.printStackTrace();
                        return false;
                    }
                }
                else {
                    Logger.log(" source and destination files are the same. Not overwriting files... "+source.getName());
                }
            }
        }

        return true;
    }

    public static boolean createProjectStructure(String directory, String filePrefix) {

//        Create save folder

        File saveFolder = new File(directory + "/" +filePrefix);
        if(!saveFolder.exists()) {
            Logger.log("Save folder not found. Creating new save folder...");
            if(!saveFolder.mkdir()) {
                Logger.logError("Error while creating Save folder. Returning...");
                return false;
            }
        }

//        Create KE_Files folder
        File MasterFolder = new File(directory + "/" +filePrefix + "/KE_Files");
        if(!MasterFolder.exists()) {
            Logger.log("KE_Files folder does not exist. Creating folder...");
            if(!MasterFolder.mkdir()) {
                Logger.logError("Error while creating KE_Files folder. Returning...");
                return false;
            }
        }

//        Create new master KE file
        File masterFile = new File(directory+"/"+filePrefix+"/KE_Files/master.ke");
        try {
            if (masterFile.exists()) {
                Logger.log("Material file already exists. Deleting it..");
                masterFile.delete();
            }
            if (!masterFile.createNewFile()) {
                Logger.logError("Error while creating new master.ke file. Returning...");
                return false;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

//        Create Shaders folder
        File shadersFolder = new File(directory+"/"+filePrefix+"/Shaders");

        if (!shadersFolder.exists()) {
            boolean folderCreationSuccess = shadersFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("Shaders folder created successfully");
            } else {
                Logger.logError("Sorry couldn’t create v folder");
                Logger.logError("Save failed...");
                return false;
            }
        }
        else {
            Logger.log("Shaders folder already exists.");
        }

//      Create new material file
        File materialFile = new File(directory+"/"+filePrefix+"/KE_Files/matLibrary.mtl");
        try {
            if (materialFile.exists()) {
                Logger.logError("Material file already exists. Deleting current file...");
                materialFile.delete();
            }
            materialFile.createNewFile();
        }catch(IOException e) {
            Logger.logError("Error while creating material file");
            e.printStackTrace();
            return false;
        }

//        Create models folder
        File modelsFolder = new File(directory+"/"+filePrefix+"/models");
        if (modelsFolder.exists()) {
            Logger.log("Models folder already exists.");
        }
        else {
            boolean folderCreationSuccess = modelsFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("Models Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create Models directory");
                return false;
            }
        }

//        Create textures folder
        File textureFolder = new File(directory+"/"+filePrefix+"/models/"+"textures");
        if (textureFolder.exists()) {
            Logger.log("Textures folder already exists.");
        }
        else {
            boolean folderCreationSuccess = textureFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("Textures Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create textures directory");
                return false;
            }
        }

//        Create textures folder
        File meshesFolder = new File(directory+"/"+filePrefix+"/models/"+"meshes");
        if (meshesFolder.exists()) {
            Logger.log("Meshes folder already exists.");
        }
        else {
            boolean folderCreationSuccess = meshesFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("Textures Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create textures directory");
                return false;
            }
        }

//        Create code folder
        File codeFolder = new File(directory+"/"+filePrefix+"/code");
        if (codeFolder.exists()) {
            Logger.log("Code folder already exists.");
        }
        else {
            boolean folderCreationSuccess = codeFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("Code Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create Code directory");
                return false;
            }
        }

//        Create Render Pipeline folder
        File renderPipelineFolder = new File(directory+"/"+filePrefix+"/code/RenderPipeline");
        if (renderPipelineFolder.exists()) {
            Logger.log("renderPipeline folder already exists.");
        }
        else {
            boolean folderCreationSuccess = renderPipelineFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("renderPipeline Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create renderPipeline directory");
                return false;
            }
        }

//        Create Model Behaviour folder
        File modelBehaviourFolder = new File(directory+"/"+filePrefix+"/code/ModelBehaviour");
        if (modelBehaviourFolder.exists()) {
            Logger.log("modelBehaviour folder already exists.");
        }
        else {
            boolean folderCreationSuccess = modelBehaviourFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("modelBehaviour Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create modelBehaviour directory");
                return false;
            }
        }

//        Create HUD folder
        File HUDFolder = new File(directory+"/"+filePrefix+"/code/HUD");
        if (HUDFolder.exists()) {
            Logger.log("modelBehaviour folder already exists.");
        }
        else {
            boolean folderCreationSuccess = HUDFolder.mkdir();
            if (folderCreationSuccess) {
                Logger.log("HUD Directory created successfully");
            } else {
                Logger.logError("Sorry couldn’t create HUD directory");
                return false;
            }
        }

        return true;
    }

    public static boolean writeMasterKEFile(Scene scene, String directory, String filePrefix, String engineVersion) {

//        Create new master KE file
        File masterFile = new File(directory+"/"+filePrefix+"/KE_Files/master.ke");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(masterFile))) {

//            Write file creation date and basic scene info
            writer.write("# Created by "+engineVersion+" on "+java.time.LocalDateTime.now()+"\n");
            writer.write("# Mesh Count: "+scene.meshID_mesh_map.size()+"\n");
            writer.write("# Model count: "+scene.modelID_model_map.size() + "\n\n");

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                          Write RenderPipeline, HUD, skybox, ambientColor

            writer.write("renderPipeline_class:"+scene.renderPipeline.getClass().getName()+"\n\n");

            if(scene.hud != null) {
                writer.write("HUD_class:" + scene.hud.getClass().getName() + "\n\n");
            }
            else {
                writer.write("HUD_class:" + null + "\n\n");
            }

            if(scene.skybox != null) {
                writer.write("skybox_id:" + scene.skybox.identifier + "\n\n");
            }
            else {
                writer.write("skybox_id:" + null + "\n\n");
            }

            if(scene.ambientLight != null) {
                writer.write("ambientColor:" + scene.ambientLight.toString() + "\n\n");
            }
            else {
                writer.write("ambientColor:" + null + "\n\n");
            }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                   First write mesh info to master file

            writer.write("MESH INFO\n\n");  //Heading indicating that mesh info is starting

            for (Mesh mesh: scene.meshID_mesh_map.values()) {
                writer.write("start new mesh\n");  //Indicates new mesh is starting

//                Write mesh info
                writer.write("ID:"+mesh.meshIdentifier+"\n");
                writer.write("Face count:"+mesh.faces.size()+"\n");
                writer.write("location:"+ mesh.meshIdentifier+".keObj\n");
                writer.write("hints:"+mesh.hints+"\n");
                writer.newLine();
            }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                          Write model info to file

            writer.write("MODEL INFO\n\n");  //Heading indicating that mesh info is starting

            for (Model model: scene.modelID_model_map.values()) {
                writer.write("start new model\n");

//                Write model info
                String type = model.getClass().getSimpleName();
                writer.write("ID:"+model.identifier+"\n");
                writer.write("type:"+type+"\n");
                writer.write("shader_ID:"+scene.modelID_shaderID_map.get(model.identifier)+"\n");

                if(model.meshes == null) {
                    writer.write("mesh_ID:null\n");
                }
                else {
                    writer.write("mesh_ID:");
                    StringBuilder meshIds = new StringBuilder();
                    for(Mesh m: model.meshes) {
                        if(m != null) {
                            meshIds.append(m.meshIdentifier+",");
                        }
                    }
//                    System.out.println(meshIds.toString());
//                    if(meshIds.length() <= 1) {
//                        System.out.println("Model id with empty mesh IDs: "+model.identifier + "  #meshes="+model.meshes.size() + " type: "+model.getClass().getSimpleName());
//                    }
                    if(model.meshes.size() > 0) {
                        writer.write(meshIds.substring(0, meshIds.length() - 1) + "\n");
                    }
                    else {
                        writer.write("null\n");
                    }
                }

                writer.write("scale:"+model.getScale().toString()+"\n");
                writer.write("pos:"+model.getPos().toString()+"\n");
                writer.write("orientation:"+model.getOrientation().toString()+"\n");
                writer.write("shouldCastShadow:"+model.shouldCastShadow+"\n");
                writer.write("shouldRender:"+model.shouldRender+"\n");

                if(model.getBehaviour() == null) {
                    writer.write("modelBehaviour:null\n");
                }
                else{
                    writer.write("modelBehaviour:"+model.getBehaviour().getClass().getCanonicalName()+"\n");
                }

                if(type.equals("Text")) {
                    Text t = (Text)model;
                    writer.write("font_name:"+ t.fontTexture.font.getFontName()+"\n");
                    writer.write("font_style:"+ t.fontTexture.font.getStyle()+"\n");
                    writer.write("font_size:"+ t.fontTexture.font.getSize()+"\n");
                    writer.write("text:"+t.text+"\n");
                }

                if(type.equals("DirectionalLight")) {
                    DirectionalLight l = (DirectionalLight)model;
                    writer.write("color:"+l.color.toString()+"\n");
                    writer.write("intensity:"+l.intensity+"\n");
                    writer.write("lightPosScale:"+l.lightPosScale+"\n");
                    writer.write("shadowMap_width:"+l.shadowMap.shadowMapWidth+"\n");
                    writer.write("shadowMap_height:"+l.shadowMap.shadowMapHeight+"\n");
                    writer.write("shadowProjectionMatrix:"+l.shadowProjectionMatrix.toString()+"\n");
                }

                if(type.equals("SpotLight")) {
                    SpotLight s = (SpotLight)model;
                    writer.write("angle:"+s.angle+"\n");
                    writer.write("angle:"+s.angle+"\n");
                    writer.write("shadowMap_width:"+s.shadowMap.shadowMapWidth+"\n");
                    writer.write("shadowMap_height:"+s.shadowMap.shadowMapHeight+"\n");
                    writer.write("shadowProjectionMatrix:"+s.shadowProjectionMatrix.toString()+"\n");
                    writer.write("color:"+ s.pointLight.color.toString()+"\n");
                    writer.write("intensity:"+ s.pointLight.intensity+"\n");
                    writer.write("att_constant:"+s.pointLight.attenuation.constant+"\n");
                    writer.write("att_linear:"+s.pointLight.attenuation.linear+"\n");
                    writer.write("att_exp:"+s.pointLight.attenuation.exponent+"\n");
                }
                writer.newLine();
            }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                              Write Pointlights
            writer.write("POINTLIGHTS INFO\n");

            for(PointLight s: scene.pointLights) {
                writer.write("start new pointLight\n");
                writer.write("");
                writer.write("color:"+ s.color.toString()+"\n");
                writer.write("intensity:"+ s.intensity+"\n");
                writer.write("att_constant:"+s.attenuation.constant+"\n");
                writer.write("att_linear:"+s.attenuation.linear+"\n");
                writer.write("att_exp:"+s.attenuation.exponent+"\n");
                writer.write("pos:"+s.pos.toString()+"\n");
                writer.newLine();
            }
            writer.newLine();

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                          Write camera and Fog

            writer.write("CAMERA INFO\n");
            writer.write("pos:"+scene.camera.getPos().toString()+"\n");
            writer.write("orientation:"+scene.camera.getOrientation().toString()+"\n");
            writer.write("fovX:"+scene.camera.getFovX()+"\n");
            writer.write("near:"+scene.camera.getNearClippingPlane()+"\n");
            writer.write("far:"+scene.camera.getFarClippingPlane()+"\n");
            writer.write("imageWidth:"+scene.camera.getImageWidth()+"\n");
            writer.write("imageHeight:"+scene.camera.getImageHeight()+"\n");
            writer.newLine();

            writer.write("FOG INFO\n");
            writer.write("active:"+scene.fog.active+"\n");
            writer.write("color:"+scene.fog.color.toString()+"\n");
            writer.write("density:"+scene.fog.density+"\n");
            writer.newLine();

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Writes the materials being used by the list of meshes passed in to file.
     * Creates a texture folder to copy textures being used by the materials.
     * <p>
     * If a material contains a texture but does not have an original source, meaning they were generated by code as
     * seen in font textures, are not stored.
     *
     * @param meshes List to meshes to write materials from
     * @param directory Directory to save files
     * @param filePrefix File name of the save folder
     * @param engineVersion Engine version string to write to material file
     */
    public static boolean writeMaterialFile(Map<String, Mesh> meshes, String directory, String filePrefix,
                                         String engineVersion) {

//      Open Material File
        File materialFile = new File(directory+"/"+filePrefix+"/KE_Files/matLibrary.mtl");

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                             Start writing material file

        Map<String, Integer> matNamesSoFar = new HashMap<>();
        Map<String, String> texturesStoredSoFar = new HashMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(materialFile))) {

            int matCount = 0;
            for (Mesh m: meshes.values()) {
                matCount += m.materials.size();
            }

            writer.write("# Created by "+engineVersion+" on "+java.time.LocalDateTime.now()+"\n");
            writer.write("# Material Count: "+matCount+"\n\n");

            for (String meshID: meshes.keySet()) {
                for (Material mat : meshes.get(meshID).materials) {

//                Write material name
                    String matName;
                    if(!mat.matName.contains("|")) {
                        matName = meshID + "|" + mat.matName;
                    }
                    else {
                        matName = mat.matName;
                    }
//                    Integer times = matNamesSoFar.get(matName);
//                    if (times == null) {
//                        matNamesSoFar.put(matName, 1);
//                    } else {
//                        matName += times;
//                        matNamesSoFar.put(matName, times + 1);
//                    }

                    writer.write("newmtl " + matName + "\n");

//                Write ambient Color
                    writer.write("ka " + mat.ambientColor + '\n');

//                Write diffuse color
                    writer.write("kd " + mat.diffuseColor + "\n");

//                Write specular color
                    writer.write("ks " + mat.specularColor + "\n");

//                Write specular power
                    writer.write("ns " + mat.specularPower + "\n");

//                Write reflectance
                    writer.write("reflectance " + mat.reflectance + "\n");

//                Write texture
                    Texture curTex = mat.texture;
                    if (curTex != null && curTex.fileName != null) {
                        String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                        if (newTextLoc == null) {
                            String[] splits = curTex.fileName.split("/");
                            String saveTexName = directory + "/" + filePrefix + "/models/textures/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            File source = new File(curTex.fileName);
                            File dest = new File(saveTexName);

                            if (!source.equals(dest)) {  // Copy files only they are not the same file
                                if (dest.exists()) {
                                    Logger.logError("Resource file already exists at destination. Deleting it...");
                                    dest.delete();
                                }
                                try {
                                    Files.copy(source.toPath(), dest.toPath());
                                } catch (Exception e) {
                                    Logger.logError("curTex: " + curTex.fileName + " MeshID: " + meshID);
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                            else {
                                Logger.log(curTex.fileName + " source and destination are the same. Not overwriting files...");
                            }
                            texturesStoredSoFar.put(curTex.fileName, splits[splits.length - 1]);
                        }
                        writer.write("map_ka " + texturesStoredSoFar.get(curTex.fileName) + "\n");
                    }

//                Write diffuseMap
                    curTex = mat.diffuseMap;
                    if (curTex != null && curTex.fileName != null) {
                        String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                        if (newTextLoc == null) {
                            String[] splits = curTex.fileName.split("/");
                            String saveTexName = directory + "/" + filePrefix + "/models/textures/" + splits[splits.length - 1];

//                       Create copy of texture in current save directory
                            File source = new File(curTex.fileName);
                            File dest = new File(saveTexName);

                            if (!source.equals(dest)) {  // Copy files only they are not the same file
                                if (dest.exists()) {
                                    Logger.logError("Resource file already exists at destination. Deleting it...");
                                    dest.delete();
                                }
                                try {
                                    Files.copy(source.toPath(), dest.toPath());
                                } catch (Exception e) {
                                    Logger.logError("curTex: " + curTex.fileName + " MeshID: " + meshID);
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                            else {
                                Logger.log(curTex.fileName + " source and destination are the same. Not overwriting files...");
                            }
                            texturesStoredSoFar.put(curTex.fileName, splits[splits.length - 1]);
                        }
                        writer.write("map_kd " + texturesStoredSoFar.get(curTex.fileName) + "\n");
                    }

//                Write specular Map
                    curTex = mat.specularMap;
                    if (curTex != null && curTex.fileName != null) {
                        String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                        if (newTextLoc == null) {
                            String[] splits = curTex.fileName.split("/");
                            String saveTexName = directory + "/" + filePrefix + "/models/textures/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            File source = new File(curTex.fileName);
                            File dest = new File(saveTexName);

                            if (!source.equals(dest)) {  // Copy files only they are not the same file
                                if (dest.exists()) {
                                    Logger.logError("Resource file already exists at destination. Deleting it...");
                                    dest.delete();
                                }
                                try {
                                    Files.copy(source.toPath(), dest.toPath());
                                } catch (Exception e) {
                                    Logger.logError("curTex: " + curTex.fileName + " MeshID: " + meshID);
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                            else {
                                Logger.log(curTex.fileName + " source and destination are the same. Not overwriting files...");
                            }
                            texturesStoredSoFar.put(curTex.fileName, splits[splits.length - 1]);
                        }
                        writer.write("map_ks " + texturesStoredSoFar.get(curTex.fileName) + "\n");
                    }

//                Write bump map
                    curTex = mat.normalMap;
                    if (curTex != null && curTex.fileName != null) {
                        String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                        if (newTextLoc == null) {
                            String[] splits = curTex.fileName.split("/");
                            String saveTexName = directory + "/" + filePrefix + "/models/textures/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            File source = new File(curTex.fileName);
                            File dest = new File(saveTexName);

                            if (!source.equals(dest)) {  // Copy files only they are not the same file
                                if (dest.exists()) {
                                    Logger.logError("Resource file already exists at destination. Deleting it...");
                                    dest.delete();
                                }
                                try {
                                    Files.copy(source.toPath(), dest.toPath());
                                } catch (Exception e) {
                                    Logger.logError("curTex: " + curTex.fileName + " MeshID: " + meshID);
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                            else {
                                Logger.log(curTex.fileName + " source and destination are the same. Not overwriting files...");
                            }

                            texturesStoredSoFar.put(curTex.fileName, splits[splits.length - 1]);
                        }
                        writer.write("map_bump " + texturesStoredSoFar.get(curTex.fileName) + "\n");
                    }

                    writer.newLine();
                }
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;

    }

}
