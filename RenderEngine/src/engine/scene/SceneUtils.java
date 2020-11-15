package engine.scene;

import engine.DataStructure.Mesh.Mesh;
import engine.Effects.Material;
import engine.model.Model;
import org.lwjgl.system.CallbackI;

import java.util.*;

public class SceneUtils {


//    Sorting function from https://www.geeksforgeeks.org/sorting-a-hashmap-according-to-values/
    public static <T> Map<T, Integer> sortByValue(Map<T, Integer> hm) {

        // Create a list from elements of HashMap
        List<Map.Entry<T, Integer> > list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        Collections.sort(list, (o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        // put data from sorted list to hashmap
        HashMap<T, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<T, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static void writeSceneToKE(Scene scene, String fileName) {

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                              Get materials being used

        Map<Material, Integer> materials = new HashMap<>();

        for (Model model: scene.models) {
            for (Material mat: model.mesh.materials) {
                materials.computeIfPresent(mat, (k ,val) -> val + 1);
                materials.putIfAbsent(mat, 1);
            }
        }

        for (Model model: scene.spotLights) {
            for (Material mat: model.mesh.materials) {
                materials.computeIfPresent(mat, (k ,val) -> val + 1);
                materials.putIfAbsent(mat, 1);
            }
        }

        for (Model model: scene.directionalLights) {
            for (Material mat: model.mesh.materials) {
                materials.computeIfPresent(mat, (k ,val) -> val + 1);
                materials.putIfAbsent(mat, 1);
            }
        }

        for (Material mat: scene.skybox.mesh.materials) {
            materials.computeIfPresent(mat, (k ,val) -> val + 1);
            materials.putIfAbsent(mat, 1);
        }

        Map<Material, Integer> sortedMaterials = sortByValue(materials);

        System.out.println("\nMaterials used in Scene:");
        int curMatID = 0;
        for (Material key: sortedMaterials.keySet()) {
            System.out.println(key.matName + " Times: " + sortedMaterials.get(key));
            sortedMaterials.put(key, curMatID);
            curMatID++;

//            if (key.matName == Material.DEFAULT_MATERIAL_NAME)
//                System.out.println(key);
        }

        System.out.println("\nMaterials with ID");
        for (Material key: sortedMaterials.keySet()) {
            System.out.println(key.matName + " ID: " + sortedMaterials.get(key));
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                              Get meshes

        Map<Mesh, Integer> meshes = new HashMap<>();

        for (Model model: scene.models) {
            meshes.computeIfPresent(model.mesh, (k ,val) -> val + 1);
            meshes.putIfAbsent(model.mesh, 1);

        }

        for (Model model: scene.spotLights) {
            meshes.computeIfPresent(model.mesh, (k ,val) -> val + 1);
            meshes.putIfAbsent(model.mesh, 1);
        }

        for (Model model: scene.directionalLights) {
            meshes.computeIfPresent(model.mesh, (k ,val) -> val + 1);
            meshes.putIfAbsent(model.mesh, 1);
        }

        meshes.computeIfPresent(scene.skybox.mesh, (k ,val) -> val + 1);
        meshes.putIfAbsent(scene.skybox.mesh, 1);


        Map<Mesh, Integer> sortedMeshes = sortByValue(meshes);

        System.out.println("\nMeshes in scene:");
        int curMeshID = 0;
        for (Mesh key: sortedMeshes.keySet()) {
            System.out.println(key.meshLocation + " Times: " + sortedMeshes.get(key));
            sortedMeshes.put(key, curMeshID);
            curMeshID++;
        }

        System.out.println("\nMeshes with ID");
        for (Mesh key: sortedMeshes.keySet()) {
            System.out.println(key.meshLocation + " ID: " + sortedMeshes.get(key));
        }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    }

}
