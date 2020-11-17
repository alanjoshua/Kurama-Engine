package engine.scene;

import engine.DataStructure.Mesh.Mesh;
import engine.Effects.Fog;
import engine.Math.Vector;
import engine.game.Game;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.MeshBuilder;
import engine.model.Model;

import java.util.*;

import static engine.utils.Logger.log;
import static engine.utils.Logger.logError;

public class Scene {

    public Map<String, Mesh> meshID_mesh_map = new HashMap<>();
    public Map<String, HashMap<String, Model>> mesh_model_map = new HashMap<>();
    public Map<String, Model> modelID_model_map = new HashMap<>();

    public List<PointLight> pointLights = new ArrayList<>();
    public List<DirectionalLight> directionalLights = new ArrayList<>();
    public List<SpotLight> spotLights = new ArrayList<>();
    public Vector ambientLight = new Vector(new float[]{0.3f,0.3f,0.3f});
    public float specularPower = 2f;
    public Model skybox = null;
    public Fog fog = Fog.NOFOG;

    private Game game;

    public Scene(Game game) {
        this.game = game;
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
                id = UUID.randomUUID().toString();
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
        mesh_model_map.put(id, new HashMap<>());
        return newMesh;
    }

    public Model createModel(Mesh mesh, String modelID) {
        Model newModel;

//        Mesh does not exists in scene's database
        if (!meshID_mesh_map.containsKey(mesh.meshIdentifier)) {
            meshID_mesh_map.put(mesh.meshIdentifier, mesh);
            mesh_model_map.put(mesh.meshIdentifier, new HashMap<>());
        }

//        Check whether modelID is unique. If not, assign a random ID
        if (modelID_model_map.containsKey(modelID)) {
            logError("Model ID "+ modelID + " not unique. Assigning random id...");
            String id = UUID.randomUUID().toString();
            newModel = new Model(game, mesh, id);
        }
        else {
            newModel = new Model(game, mesh, modelID);
        }

        modelID_model_map.put(newModel.identifier, newModel);
        mesh_model_map.get(mesh.meshIdentifier).put(newModel.identifier, newModel);  // Insert new model into mesh_model map

        return newModel;
    }

    public void addModel(Model newModel) {
//        Mesh does not exists in scene's database
        if (!meshID_mesh_map.containsKey(newModel.mesh.meshIdentifier)) {
            meshID_mesh_map.put(newModel.mesh.meshIdentifier, newModel.mesh);
            mesh_model_map.put(newModel.mesh.meshIdentifier, new HashMap<>());
        }

//        Check whether modelID is unique. If not, assign a random ID
        if (modelID_model_map.containsKey(newModel.identifier)) {
            logError("Model ID "+ newModel.identifier + " not unique. Assigning random id...");
            newModel.identifier = UUID.randomUUID().toString();
        }

        modelID_model_map.put(newModel.identifier, newModel);
        mesh_model_map.get(newModel.mesh.meshIdentifier).put(newModel.identifier, newModel);  // Insert new model into mesh_model map
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

//    public void buildModelMap() {
//        for(Model m: models) {
//            Mesh mesh = m.mesh;
//            List<Model> list = mesh_model_map.get(mesh);
//            if(list == null) {
//                list = new ArrayList<>();
//                mesh_model_map.put(mesh,list);
//            }
//            list.add(m);
//        }
//    }

}

