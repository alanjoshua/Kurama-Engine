package engine.scene;

import engine.Effects.Material;
import engine.model.Model;
import org.lwjgl.system.CallbackI;

import java.util.*;

public class SceneUtils {

    public static Map<Material, Integer> sortByValue(Map<Material, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<Material, Integer> > list =
                new LinkedList<Map.Entry<Material, Integer> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Material, Integer> >() {
            public int compare(Map.Entry<Material, Integer> o1,
                               Map.Entry<Material, Integer> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<Material, Integer> temp = new LinkedHashMap<Material, Integer>();
        for (Map.Entry<Material, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static void writeSceneToKE(Scene scene, String fileName) {
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

        Map<Material, Integer> sortedMaterials = sortByValue(materials);

        for (Map.Entry<Material, Integer> en : sortedMaterials.entrySet()) {
            System.out.println(en.getKey().matName + " Times: " + en.getValue());
        }

    }

}
