package engine.scene;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.Effects.Material;
import engine.model.Model;
import engine.utils.Logger;

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

    public static boolean writeSceneToKE(Scene scene, String directory, String filePrefix, String shadersDirectory,
                                      String RenderBlockDirectory, String hudDirectory, String modelBehaviourDirectory,
                                         String engineVersion) throws IOException {

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

        if(!writeMeshes(scene.meshID_mesh_map, directory, filePrefix)) {
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

    public static boolean writeMeshes(Map<String, Mesh> meshes, String directory, String filePrefix) {
        for (Mesh mesh: meshes.values()) {
            if (mesh.meshLocation != null) {
                File source = new File(mesh.meshLocation);
                File dest = new File(directory + "/" + filePrefix + "/models/meshes/" + mesh.meshIdentifier + ".keObj");

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

    public static boolean copyModelBehaviourFiles(String directory, String filePrefix, String modelBehaviourDir) {
        Logger.log("Copying model behaviour files...");
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

    public static boolean createProjectStructure(String directory, String filePrefix) throws IOException {

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
        if (masterFile.exists()) {
            Logger.log("Material file already exists. Deleting it..");
            masterFile.delete();
        }
        if(!masterFile.createNewFile()) {
            Logger.logError("Error while creating new master.ke file. Returning...");
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

    public static boolean writeMasterKEFile(Scene scene, String directory, String filePrefix, String engineVersion)
            throws IOException {

//        Create new master KE file
        File masterFile = new File(directory+"/"+filePrefix+"/KE_Files/master.ke");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(masterFile))) {

//            Write file creation date and basic scene info
            writer.write("# Created by "+engineVersion+" on "+java.time.LocalDateTime.now()+"\n");
            writer.write("# Mesh Count: "+scene.meshID_mesh_map.size()+"\n");
            writer.write("# Model count: "+scene.modelID_model_map.size() + "\n\n");

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                          Write RenderPipeline Info

            writer.write("renderPipeline_class:"+scene.renderPipeline.getClass().getName()+"\n\n");

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
                writer.write("ID:"+model.identifier+"\n");
                writer.write("type:"+model.getClass().getSimpleName()+"\n");
                writer.write("shader_ID:"+scene.modelID_shaderID_map.get(model.identifier)+"\n");

                if (model.mesh != null) {
                    writer.write("mesh_ID:"+model.mesh.meshIdentifier+"\n");
                }
                else {
                    writer.write("mesh_ID:null\n");
                }

                writer.write("scale:"+model.getScale().toString()+"\n");
                writer.write("pos:"+model.getPos().toString()+"\n");
                writer.write("orientation:"+model.getOrientation().toString()+"\n");
                writer.write("shouldCastShadow:"+model.shouldCastShadow+"\n");
                writer.write("shouldRender:"+model.shouldRender+"\n");
                writer.newLine();
            }

            return true;

        }

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
                                         String engineVersion) throws IOException {

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
                    String matName = meshID+"|"+mat.matName;
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
