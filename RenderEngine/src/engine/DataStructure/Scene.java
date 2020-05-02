package engine.DataStructure;

import engine.DataStructure.Mesh.Mesh;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {

    public List<Model> models = new ArrayList<>();
    public Map<Mesh, List<Model>> modelMap = new HashMap<>();
    public List<PointLight> pointLights = new ArrayList<>();
    public List<DirectionalLight> directionalLights = new ArrayList<>();
    public List<SpotLight> spotLights = new ArrayList<>();
    public Model skybox = null;

    public void buildModelMap() {
        for(Model m: models) {
            Mesh mesh = m.mesh;
            List<Model> list = modelMap.get(mesh);
            if(list == null) {
                list = new ArrayList<>();
                modelMap.put(mesh,list);
            }
            list.add(m);
        }
    }

}

