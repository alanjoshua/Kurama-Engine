package engine.scene;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.Effects.Material;
import engine.model.Model;
import engine.utils.Logger;
import org.lwjgl.system.CallbackI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
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

    public static void writeSceneToKE(Scene scene, String directory, String filePrefix, String engineVersion) {

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                              Get materials being used

        Map<Material, Integer> materials = new HashMap<>();

        for (Mesh mesh: scene.getMeshes()) {
            for (Material mat : mesh.materials) {
                materials.computeIfPresent(mat, (k, val) -> val + 1);
                materials.putIfAbsent(mat, 1);
            }
        }

        for (Material mat: scene.skybox.mesh.materials) {
            materials.computeIfPresent(mat, (k ,val) -> val + 1);
            materials.putIfAbsent(mat, 1);
        }

        Map<Material, Integer> sortedMaterials = sortByValue(materials);

        Logger.log("\nMaterials used in Scene:");
        int curMatID = 0;
        for (Material key: sortedMaterials.keySet()) {
            Logger.log(key.matName + " Times: " + sortedMaterials.get(key));
            sortedMaterials.put(key, curMatID);
            curMatID++;
        }

        Logger.log("\nMaterials with ID");
        for (Material key: sortedMaterials.keySet()) {
            Logger.log(key.matName + " ID: " + sortedMaterials.get(key));
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                              Get meshes

        Map<Mesh, Integer> meshes = new HashMap<>();

        for (Mesh mesh: scene.getMeshes()) {
            meshes.computeIfPresent(mesh, (k ,val) -> val + 1);
            meshes.putIfAbsent(mesh, 1);
        }

        meshes.computeIfPresent(scene.skybox.mesh, (k ,val) -> val + 1);
        meshes.putIfAbsent(scene.skybox.mesh, 1);


        Map<Mesh, Integer> sortedMeshes = sortByValue(meshes);

        Logger.log("\nMeshes in scene:");
        int curMeshID = 0;
        for (Mesh key: sortedMeshes.keySet()) {
            Logger.log(key.meshLocation + " Times: " + sortedMeshes.get(key));
            sortedMeshes.put(key, curMeshID);
            curMeshID++;
        }

        Logger.log("\nMeshes with ID");
        for (Mesh key: sortedMeshes.keySet()) {
            Logger.log(key.meshLocation + " ID: " + sortedMeshes.get(key));
        }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                              Create Save folder
        File folder = new File(directory+"/"+filePrefix);
        boolean folderCreationSuccess = folder.mkdir();
        if(folderCreationSuccess){
            Logger.log("Directory created successfully");
        }else{
            Logger.log("Sorry couldn’t create save folder");
            Logger.log("Save failed...");
            return;
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                           Write Material File
        writeMaterialFile(sortedMaterials, directory, filePrefix, engineVersion);


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                      Write .KE file

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public static void writeMaterialFile(Map<Material, Integer> sortedMaterials,
                                         String directory, String filePrefix, String engineVersion) {

//                                           Create new material file

        File materialFile = new File(directory+"/"+filePrefix+"/"+"matLibrary.mtl");
        try {
            materialFile.createNewFile();
        }catch(IOException e) {
            Logger.log("Could not create material file");
        }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                      Create folder for storing textures

        File textureFolder = new File(directory+"/"+filePrefix+"/"+"textures");
        boolean folderCreationSuccess = textureFolder.mkdir();
        if(folderCreationSuccess){
            Logger.log("Directory created successfully");
        }else{
            Logger.log("Sorry couldn’t create textures directory");
        }


        Map<String, Integer> matNamesSoFar = new HashMap<>();
        Map<String, String> texturesStoredSoFar = new HashMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(materialFile))) {

            writer.write("# Created by "+engineVersion+" on "+java.time.LocalDateTime.now()+"\n");
            writer.write("# Material Count: "+sortedMaterials.size()+"\n\n");

            for (Material mat: sortedMaterials.keySet()) {

//                Write material name
                String matName = mat.matName;
                Integer times = matNamesSoFar.get(matName);
                if (times == null) {
                    matNamesSoFar.put(matName, 1);
                }
                else {
                    matName += times;
                    matNamesSoFar.put(matName, times + 1);
                }

                writer.write("newmtl "+matName+"\n");

//                Write ambient Color
                writer.write("ka " + mat.ambientColor+'\n');

//                Write diffuse color
                writer.write("kd "+ mat.diffuseColor+"\n");

//                Write specular color
                writer.write("ks "+ mat.specularColor+"\n");

//                Write specular power
                writer.write("ns "+ mat.specularPower+"\n");

//                Write reflectance
                writer.write("reflectance "+mat.reflectance+"\n");

//                Write texture
                Texture curTex = mat.texture;
                if (curTex != null) {
                    String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                    if (newTextLoc == null) {
                        String[] splits = curTex.fileName.split("/");
                        String saveTexName = directory+"/"+filePrefix+"/"+"textures"+"/"+splits[splits.length-1];

//                        Create copy of texture in current save directory
                        File source = new File(curTex.fileName);
                        File dest = new File(saveTexName);
                        Files.copy(source.toPath(), dest.toPath());
                        texturesStoredSoFar.put(curTex.fileName, splits[splits.length-1]);
                    }
                    writer.write("map_ka "+texturesStoredSoFar.get(curTex.fileName)+"\n");
                }

//                Write diffuseMap
                curTex = mat.diffuseMap;
                if (curTex != null) {
                    String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                    if (newTextLoc == null) {
                        String[] splits = curTex.fileName.split("/");
                        String saveTexName = directory+"/"+filePrefix+"/"+"textures"+"/"+splits[splits.length-1];

//                        Create copy of texture in current save directory
                        Files.copy(new File(curTex.fileName).toPath(), new File(saveTexName).toPath());

                        texturesStoredSoFar.put(curTex.fileName, splits[splits.length-1]);
                    }
                    writer.write("map_kd "+texturesStoredSoFar.get(curTex.fileName)+"\n");
                }

//                Write specular Map
                curTex = mat.specularMap;
                if (curTex != null) {
                    String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                    if (newTextLoc == null) {
                        String[] splits = curTex.fileName.split("/");
                        String saveTexName = directory+"/"+filePrefix+"/"+"textures"+"/"+splits[splits.length-1];

//                        Create copy of texture in current save directory
                        Files.copy(new File(curTex.fileName).toPath(), new File(saveTexName).toPath());

                        texturesStoredSoFar.put(curTex.fileName, splits[splits.length-1]);
                    }
                    writer.write("map_ks "+texturesStoredSoFar.get(curTex.fileName)+"\n");
                }

//                Write bump map
                curTex = mat.normalMap;
                if (curTex != null) {
                    String newTextLoc = texturesStoredSoFar.get(curTex.fileName);

//                    If this texture hasn't already been copied
                    if (newTextLoc == null) {
                        String[] splits = curTex.fileName.split("/");
                        String saveTexName = directory+"/"+filePrefix+"/"+"textures"+"/"+splits[splits.length-1];

//                        Create copy of texture in current save directory
                        Files.copy(new File(curTex.fileName).toPath(), new File(saveTexName).toPath());

                        texturesStoredSoFar.put(curTex.fileName, splits[splits.length-1]);
                    }
                    writer.write("map_bump "+texturesStoredSoFar.get(curTex.fileName)+"\n");
                }

                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
