package engine.scene;

import engine.DataStructure.Mesh.Mesh;
import engine.Effects.Fog;
import engine.Math.Vector;
import engine.game.Game;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.HUD;
import engine.model.MeshBuilder;
import engine.model.Model;
import engine.shader.ShaderProgram;
import engine.utils.Utils;

import java.util.*;

import static engine.utils.Logger.log;
import static engine.utils.Logger.logError;

public class Scene {

    public Map<String, Mesh> meshID_mesh_map = new HashMap<>();
    public Map<String, HashMap<String, HashMap<String, Model>>> shader_mesh_model_map = new HashMap<>();
    public Map<String, Model> modelID_model_map = new HashMap<>();
    public Map<String, String> modelID_shaderID_map = new HashMap<>();
    public Map<String, ShaderProgram> shaderID_shader_map = new HashMap<>();

    public List<PointLight> pointLights = new ArrayList<>();
    public List<DirectionalLight> directionalLights = new ArrayList<>();
    public List<SpotLight> spotLights = new ArrayList<>();
    public Vector ambientLight = new Vector(new float[]{0.3f,0.3f,0.3f});
    public float specularPower = 2f;
    public Model skybox = null;
    public Fog fog = Fog.NOFOG;

    public HUD hud;

    private Game game;

    public Scene(Game game) {
        this.game = game;
    }

    public void addPointlight(PointLight pl) {
        pointLights.add(pl);
    }

    public void addSplotLight(SpotLight sl, String shaderID) {
        addModel(sl, shaderID);
        spotLights.add(sl);
    }

    public void addDirectionalLight(DirectionalLight dl, String shaderID) {
        addModel(dl, shaderID);
        directionalLights.add(dl);
    }

    public boolean isIDUniqueMeshID(String id) {
        if (id == null) {
            return false;
        }

        boolean idPresent = meshID_mesh_map.containsKey(id);
        if (idPresent) {
            return false;
        }
        else {
            return true;
        }
    }

    public void setUniqueMeshID(Mesh mesh) {
        if (!isIDUniqueMeshID(mesh.meshIdentifier)) {
            mesh.meshIdentifier = Utils.getUniqueID();
        }
    }

    public Mesh loadMesh(String location, String meshID, MeshBuilder.MeshBuilderHints hints) {

        log("Loading mesh "+meshID + " ...");
        Mesh newMesh = MeshBuilder.buildModelFromFileGL(location, hints);
        log("Finished loading mesh");

        log("Checking whether input meshID is unique...");
        boolean idPresent = meshID_mesh_map.containsKey(meshID);
        String id;

        if (idPresent) {
            log("ID not unique. Checking whether location already exists as an ID...");

            String[] splits = location.split("/");
            String fileName = splits[splits.length - 1].split(".")[0];
            boolean locPresent = meshID_mesh_map.containsKey(fileName);

            if (locPresent) {
                log("Location already being used as ID. Asigning random ID...");
                id = Utils.getUniqueID();
            }
            else {
                id = fileName;
            }
        }
        else {
             id = meshID;
        }

        log("Assigned id: "+id);
        newMesh.meshIdentifier = id;
        meshID_mesh_map.put(id, newMesh);
//        mesh_model_map.put(id, new HashMap<>());
        return newMesh;
    }

    public Model createModel(Mesh mesh, String modelID, String shaderID) {
        Model newModel;

        shader_mesh_model_map.putIfAbsent(shaderID, new HashMap<>());  // Enter a new hashmap if shaderID does not exist yet

//        Mesh does not exists in scene's database
        if (mesh != null && !meshID_mesh_map.containsKey(mesh.meshIdentifier)) {
            meshID_mesh_map.put(mesh.meshIdentifier, mesh);
            shader_mesh_model_map.get(shaderID).put(mesh.meshIdentifier, new HashMap<>());
        }

//        If mesh is already present, but no models so far have been added that use the specified shader
        if (!shader_mesh_model_map.get(shaderID).containsKey(mesh.meshIdentifier)) {
            shader_mesh_model_map.get(shaderID).put(mesh.meshIdentifier, new HashMap<>());
        }

//        Check whether modelID is unique. If not, assign a random ID
        if (modelID_model_map.containsKey(modelID)) {
            logError("Model ID "+ modelID + " not unique. Assigning random id...");
            String id = Utils.getUniqueID();
            newModel = new Model(game, mesh, id);
        }
        else {
            newModel = new Model(game, mesh, modelID);
        }

        modelID_model_map.put(newModel.identifier, newModel);
        modelID_shaderID_map.put(newModel.identifier, shaderID);

        if (mesh != null) {
            shader_mesh_model_map.get(shaderID).get(mesh.meshIdentifier).put(newModel.identifier, newModel);  // Insert new model into mesh_model map
        }

        return newModel;
    }

    public void addModel(Model newModel, String shaderID) {
//        If model does not have a mesh
        if (newModel.mesh == null) {

    //        Check whether modelID is unique. If not, assign a random ID
            if (modelID_model_map.containsKey(newModel.identifier)) {
                logError("Model ID "+ newModel.identifier + " not unique. Assigning random id...");
                newModel.identifier = UUID.randomUUID().toString();
            }

            modelID_model_map.put(newModel.identifier, newModel);
            modelID_shaderID_map.put(newModel.identifier, shaderID);
            return;
        }

        shader_mesh_model_map.putIfAbsent(shaderID, new HashMap<>());  // Enter a new hashmap if shaderID does not exist yet

//        Mesh does not exists in scene's database
        if (!meshID_mesh_map.containsKey(newModel.mesh.meshIdentifier)) {
            meshID_mesh_map.put(newModel.mesh.meshIdentifier, newModel.mesh);
            shader_mesh_model_map.get(shaderID).put(newModel.mesh.meshIdentifier, new HashMap<>());
        }

//        If mesh is already present, but no models so far have been added that use the specified shader
        if (!shader_mesh_model_map.get(shaderID).containsKey(newModel.mesh.meshIdentifier)) {
            shader_mesh_model_map.get(shaderID).put(newModel.mesh.meshIdentifier, new HashMap<>());
        }

//        Check whether modelID is unique. If not, assign a random ID
        if (modelID_model_map.containsKey(newModel.identifier)) {
            logError("Model ID "+ newModel.identifier + " not unique. Assigning random id...");
            newModel.identifier = Utils.getUniqueID();
        }

        modelID_model_map.put(newModel.identifier, newModel);
        modelID_shaderID_map.put(newModel.identifier, shaderID);
//        log("\n");
//        log("mesh id: "+ newModel.mesh.meshIdentifier);
//        log("model ID: "+newModel.identifier);
//        log("curr mesh is present in map: "+ shader_mesh_model_map.get(shaderID).containsKey(newModel.mesh.meshIdentifier));
        shader_mesh_model_map.get(shaderID).
                get(newModel.mesh.meshIdentifier).
                put(newModel.identifier, newModel);  // Insert new model into mesh_model map
    }

    public void addSkyBlock(Model skyblock, String shaderID) {
//        Check whether modelID is unique. If not, assign a random ID
        if (modelID_model_map.containsKey(skyblock.identifier)) {
            logError("Model ID "+ skyblock.identifier + " not unique. Assigning random id...");
            skyblock.identifier = Utils.getUniqueID();
        }

        setUniqueMeshID(skyblock.mesh);
        addModel(skyblock, shaderID);
        this.skybox = skyblock;
    }

    public void cleanUp() {
        for(Mesh m: meshID_mesh_map.values()) {
            m.cleanUp();
        }
    }

    public void updateAllModels(Model.ModelTickInput params) {
        modelID_model_map.values().forEach(m -> m.tick(params));
    }

    public Collection<Model> getModels() {
        return modelID_model_map.values();
    }

    public Collection<Mesh> getMeshes() {
        return meshID_mesh_map.values();
    }

}

