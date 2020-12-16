package engine.geometry.MD5;

import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.utils.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MD5AnimModel {

    public String version;
    public String commandLine;
    public int numJoints;
    public float frameRate;
    public int numFrames;
    public int numAnimatedComponents;

    public List<JointAnim> joints;
    public List<Frame> frames;

    public MD5AnimModel(String file) {

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

                    case "frameRate": {
                        frameRate = Float.parseFloat(split[1]);
                        break;
                    }

                    case "numFrames": {
                        numFrames = Integer.parseInt(split[1]);
                        frames = new ArrayList<>(numFrames);
                        break;
                    }

                    case "numAnimatedComponents": {
                        numAnimatedComponents = Integer.parseInt(split[1]);
                        break;
                    }

                    case "hierarchy": {
                        while(!(line = reader.readLine()).equals("}")) {

                            var spaceSplit = line.split("\\s+");

                            var name = spaceSplit[1];
                            name = name.substring(1, name.length()-1);

                            var parent = Integer.parseInt(spaceSplit[2]);
                            var flags = Integer.parseInt(spaceSplit[3]);
                            var startIndex = Integer.parseInt(spaceSplit[4]);

                            joints.add(new JointAnim(name, parent, flags, startIndex));
                        }
                        break;
                    }

                    case "bounds": {
                        while(!(line = reader.readLine()).equals("}")) {
                            var spaceSplit = line.split("\\s+");
                            var min = new Vector(Float.parseFloat(spaceSplit[2]), Float.parseFloat(spaceSplit[3]), Float.parseFloat(spaceSplit[4]));
                            var max = new Vector(Float.parseFloat(spaceSplit[7]), Float.parseFloat(spaceSplit[8]), Float.parseFloat(spaceSplit[9]));
                            Frame frame = new Frame(numAnimatedComponents);
                            frame.minBound = min;
                            frame.maxBound = max;
                            frames.add(frame);
                        }
                        break;
                    }

                    case "baseframe": {
                        int jointCounter = 0;
                        while(!(line = reader.readLine()).equals("}")) {
                            var spaceSplit = line.split("\\s+");
                            var pos = new Vector(Float.parseFloat(spaceSplit[2]), Float.parseFloat(spaceSplit[3]), Float.parseFloat(spaceSplit[4]));
                            var orient = new Vector(Float.parseFloat(spaceSplit[7]), Float.parseFloat(spaceSplit[8]), Float.parseFloat(spaceSplit[9]));

//                        Calculate w component for quqaternion
                            float t = 1f - (orient.get(0) * orient.get(0)) - (orient.get(1) * orient.get(1)) - (orient.get(2) * orient.get(2));
                            float w;
                            if (t < 0f) {
                                w = 0f;
                            } else {
                                w = (float) -Math.sqrt(t);
                            }

                            var orient_quat = new Quaternion(new Vector(w, orient.get(0), orient.get(1), orient.get(2)));
                            orient_quat.normalise();
                            joints.get(jointCounter).base_pos = pos;
                            joints.get(jointCounter).base_orient = orient_quat;
                            jointCounter++;
                        }
                        break;
                    }

                    case "frame": {
                        var frameIndex = Integer.parseInt(split[1]);
                        var frame = frames.get(frameIndex);

                        while(!(line = reader.readLine()).equals("}")) {
                            Logger.log(line);
                            var spaceSplit = line.split("\\s+");
                            for(var val: spaceSplit) {
                                try {
                                    frame.components.add(Float.parseFloat(val));
                                }
                                catch (NumberFormatException e) {
                                    // Do nothing, since this exception means it tried to convert an empty string
                                }
                            }
                        }
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
