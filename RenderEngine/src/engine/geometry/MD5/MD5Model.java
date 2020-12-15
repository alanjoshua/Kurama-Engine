package engine.geometry.MD5;

import engine.Math.Vector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MD5Model {

    public String version;
    public String commandLine;

    public int numJoints;
    public int numMeshes;

    public List<Joint> joints;
    public List<MD5Mesh> meshes;

    public MD5Model(String file) {

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while((line = reader.readLine()) != null) {
                var split = line.split("\\s+");

                switch (split[0]) {

//                    LOAD HEADER
                    case "MD5Version": {
                        version = split[1];
                        break;
                    }

                    case "commandline": {
                        commandLine = split[1];
                        break;
                    }

                    case "numJoints": {
                        numJoints = Integer.parseInt(split[1]);
                        joints = new ArrayList<>(numJoints);
                        break;
                    }

                    case "numMeshes": {
                        numMeshes = Integer.parseInt(split[1]);
                        meshes = new ArrayList<>(numMeshes);
                        break;
                    }

//                     START READING JOINTS
                    case "joints": {
                        while(!(line = reader.readLine()).equals("}")) {

                            var spaceSplit = line.split("\\s+");

                            var name = spaceSplit[1];
                            name = name.substring(1, name.length()-1);

                            var parent = Integer.parseInt(spaceSplit[2]);

                            var pos = new Vector(Float.parseFloat(spaceSplit[4]), Float.parseFloat(spaceSplit[5]),
                                    Float.parseFloat(spaceSplit[6]));

                            var orient = new Vector(Float.parseFloat(spaceSplit[9]), Float.parseFloat(spaceSplit[10]),
                                    Float.parseFloat(spaceSplit[11]));

                            var joint = new Joint(name, parent, pos, orient);
                            joints.add(joint);
                        }
                        break;
                    }

                    case "mesh": {
                        MD5Mesh mesh = new MD5Mesh(reader);
                        meshes.add(mesh);
                        break;
                    }

                    default:{
                        continue;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
