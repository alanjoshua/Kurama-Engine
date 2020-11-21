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

    public static void writeSceneToKE(Scene scene, String directory, String filePrefix, String engineVersion) throws IOException {

//      Create Save folder
        File folder = new File(directory+"/"+filePrefix);
        boolean folderCreationSuccess = folder.mkdir();
        if(folderCreationSuccess){
            Logger.log("Directory created successfully");
        }else{
            Logger.logError("Sorry couldn’t create save folder");
            Logger.logError("Save failed...");
            return;
        }

//        File shadersFolder = new File()

//      Write Material File
        writeMaterialFile(scene.meshID_mesh_map, directory, filePrefix, engineVersion);

//      Write .KE master file
        writeMasterKEFile(scene, directory, filePrefix, engineVersion);

    }

    public static void writeMasterKEFile(Scene scene, String directory, String filePrefix, String engineVersion)
            throws IOException {

//        Create new master KE file
        File materialFile = new File(directory+"/"+filePrefix+"/"+"master.ke");
        try {
            materialFile.createNewFile();
        }catch(IOException e) {
            throw new IOException("Unable to create new material file");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(materialFile))) {

//            Write file creation date and basic scene info
            writer.write("# Created by "+engineVersion+" on "+java.time.LocalDateTime.now()+"\n");
            writer.write("# Mesh Count: "+scene.meshID_mesh_map.size()+"\n");
            writer.write("# Model count: "+scene.modelID_model_map.size() + "\n\n");

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                          Write shader Info

            File shadersFolder = new File(directory+"/"+filePrefix+"/shaders");
            boolean folderCreationSuccess = shadersFolder.mkdir();
            if(folderCreationSuccess){
                Logger.log("Shaders folder created successfully");
            }else{
                Logger.logError("Sorry couldn’t create v folder");
                Logger.logError("Save failed...");
                return;
            }

            Map<String, String> shadersWrittenSoFar = new HashMap<>();

            writer.write("RENDER PIPELINE INFO\n\n");

            writer.write("renderPipeline_class:"+scene.renderPipeline.getClass().getName()+"\n\n");
//            for (RenderBlock shader: scene.renderPipeline.renderBlockID_renderBlock_map.values()) {
//
//                writer.write("start new renderblock\n");
//                writer.write("ID:"+shader.shaderIdentifier+"\n");
//
//                String vertShaderLoc;
//                String fragmentShaderLoc;
//
////----------------------------------------------------------------------------------------------------------------------
////                                                      Make copy of Shaders
//
////              Make copy of vertex shader in shaders folder
//                String newShaderLoc = shadersWrittenSoFar.get(shader.vertexShaderLocation);
//
////              If this shader hasn't already been copied
//                if (newShaderLoc == null) {
//                    String[] splits = shader.vertexShaderLocation.split("/");
//                    String saveTexName = directory + "/" + filePrefix + "/" + "shaders" + "/" + splits[splits.length - 1];
//
////                  Create copy of texture in current save directory
//                    File source = new File(shader.vertexShaderLocation);
//                    File dest = new File(saveTexName);
//                    try {
//                        Files.copy(source.toPath(), dest.toPath());
//                    } catch (Exception e) {
//                        Logger.logError("current Vert shader: " + shader.vertexShaderLocation + " Shader ID: " + shader.shaderIdentifier);
//                        e.printStackTrace();
//                        System.exit(1);
//                    }
//                    shadersWrittenSoFar.put(shader.vertexShaderLocation, directory+"/"+filePrefix+"/shaders/"+splits[splits.length - 1]);
//                }
//                vertShaderLoc = shadersWrittenSoFar.get(shader.vertexShaderLocation);
//
////                Make copy of fragment shader in shaders folder
//                newShaderLoc = shadersWrittenSoFar.get(shader.fragmentShaderLocation);
//
////              If this shader hasn't already been copied
//                if (newShaderLoc == null) {
//                    String[] splits = shader.fragmentShaderLocation.split("/");
//                    String saveTexName = directory + "/" + filePrefix + "/" + "shaders" + "/" + splits[splits.length - 1];
//
////                  Create copy of texture in current save directory
//                    File source = new File(shader.fragmentShaderLocation);
//                    File dest = new File(saveTexName);
//                    try {
//                        Files.copy(source.toPath(), dest.toPath());
//                    } catch (Exception e) {
//                        Logger.logError("current Frag shader: " + shader.fragmentShaderLocation + " Shader ID: " + shader.shaderIdentifier);
//                        e.printStackTrace();
//                        System.exit(1);
//                    }
//                    shadersWrittenSoFar.put(shader.fragmentShaderLocation, directory+"/"+filePrefix+"/shaders/"+splits[splits.length - 1]);
//                }
//                fragmentShaderLoc = shadersWrittenSoFar.get(shader.fragmentShaderLocation);
//
////----------------------------------------------------------------------------------------------------------------------
//                writer.write("vertexShader:"+vertShaderLoc+"\n");
//                writer.write("fragmentShader:"+fragmentShaderLoc+"\n");
//                writer.newLine();
//            }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                   First write mesh info to master file

            writer.write("MESH INFO\n\n");  //Heading indicating that mesh info is starting

            for (Mesh mesh: scene.meshID_mesh_map.values()) {
                writer.write("start new mesh\n");  //Indicates new mesh is starting

//                Write mesh info
                writer.write("ID:"+mesh.meshIdentifier+"\n");
                writer.write("Face count:"+mesh.faces.size()+"\n");
                writer.write("location:"+ directory+"/"+filePrefix+"/"+"meshes/"+mesh.meshIdentifier+".keObj\n");
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
    public static void writeMaterialFile(Map<String, Mesh> meshes, String directory, String filePrefix,
                                         String engineVersion) throws IOException {

//      Create new material file

        File materialFile = new File(directory+"/"+filePrefix+"/"+"matLibrary.mtl");
        try {
            materialFile.createNewFile();
        }catch(IOException e) {
            throw new IOException("Unable to create new material file");
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
                            String saveTexName = directory + "/" + filePrefix + "/" + "textures" + "/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            File source = new File(curTex.fileName);
                            File dest = new File(saveTexName);
                            try {
                                Files.copy(source.toPath(), dest.toPath());
                            }
                            catch(Exception e) {
                                Logger.logError("curTex: "+curTex.fileName + " MeshID: "+meshID);
                                e.printStackTrace();
                                System.exit(1);
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
                            String saveTexName = directory + "/" + filePrefix + "/" + "textures" + "/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            Files.copy(new File(curTex.fileName).toPath(), new File(saveTexName).toPath());

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
                            String saveTexName = directory + "/" + filePrefix + "/" + "textures" + "/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            Files.copy(new File(curTex.fileName).toPath(), new File(saveTexName).toPath());

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
                            String saveTexName = directory + "/" + filePrefix + "/" + "textures" + "/" + splits[splits.length - 1];

//                        Create copy of texture in current save directory
                            Files.copy(new File(curTex.fileName).toPath(), new File(saveTexName).toPath());

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
    }

}
