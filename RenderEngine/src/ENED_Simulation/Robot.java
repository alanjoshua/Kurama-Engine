package ENED_Simulation;

import engine.DataStructure.GridNode;
import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.inputs.Input;
import engine.model.Model;
import engine.model.ModelBuilder;
import engine.model.Movable;
import org.lwjgl.system.CallbackI;

import java.util.*;

import static org.lwjgl.opengl.GL11C.GL_LINES;

public class Robot extends Movable {

    private Simulation game;

    private Input input;
    public float scanRadius = 5;
    private Box boxPicked;

    public Robot(Simulation game,Mesh mesh, String identifier) {
        super(game,mesh, identifier);
        this.game = game;
        input = game.getInput();
        translationDirection = new Vector(3,0);
    }

    @Override
    public void tick(ModelTickInput params) {
        translationDirection = new Vector(3,0);

        if(input.keyDown(input.UP_ARROW)) {
            moveForward(params);
            isManualControl = true;
        }

        if(input.keyDown(input.DOWN_ARROW)) {
            moveBackward(params);
            isManualControl = true;
        }

        if(input.keyDown(input.LEFT_ARROW)) {
           turnLeft(params);
            isManualControl = true;
        }

        if(input.keyDown(input.RIGHT_ARROW)) {
            turnRight(params);
            isManualControl = true;
        }

        if(input.keyDown(input.ONE)) {
            if(boxPicked != null) {
                boxPicked.setPos(new Vector(new float[]{this.pos.get(0), 0, this.getPos().get(2)}));
                game.addBoxToDropped(boxPicked);
                boxPicked = null;
            }
        }

        if(!isManualControl) {
            autoMove();
        }
       
       translationDirection = translationDirection.normalise();
        pathFind();

        if(boxPicked == null) {
            Matrix robotMatrix = this.getOrientation().getRotationMatrix();
            Optional<Box> optional = game.boxes
                    .stream()
                    .filter(b -> b.isRobotInCorrectPositionToScan(this, robotMatrix))
                    .findFirst();
            if (optional.isPresent()) {
                boxPicked = optional.get();
                System.out.println("box to be scanned is: " + boxPicked.identifier);
                System.out.print("barcode: ");
                boxPicked.barCode.display();
            }
        }
        else {   //Already picked a box
            boxPicked.setPos(this.pos.add(new Vector(new float[]{0,2,0})));
            boxPicked.setOrientation(this.getOrientation());

        }

    }

    public void autoMove() {

    }

    public void pathFind() {

        Box target = selectBox(game.boxes);

        try {
            if (target != null) {

                PriorityQueue<GridNode> frontier = new PriorityQueue<>();

                Vector pos = new Vector(new float[]{(int) this.pos.get(0), (int) this.pos.get(1), (int) this.pos.get(2)});

                GridNode start = new GridNode(pos, 0);
                frontier.add(start);
//        System.out.println(start);

                HashMap<Vector, Vector> cameFrom = new HashMap<>();
                HashMap<Vector, Float> costSoFar = new HashMap<>();

                cameFrom.put(start.pos, null);
                costSoFar.put(start.pos, 0f);

                costSoFar.put(null, 0f);

                Vector tempGoal = new Vector(new float[]{(int) target.getPos().get(0), (int) this.getPos().get(1), (int) target.getPos().get(2)});
                GridNode goal = new GridNode(tempGoal, 0);
                //GridNode goal = new GridNode(new Vector(new float[]{50,pos.get(1),-50}),0);
                GridNode current = null;

                while (!frontier.isEmpty()) {
                    current = frontier.poll();

                    if (current.equals(goal)) {
                        break; //Reached end point
                    }

                    List<GridNode> neighbours = game.getNeighbours(current, this, target);
                    //System.out.println(neighbours.size());
                    for (GridNode next : neighbours) {
                        Float tempCost = costSoFar.get(current.pos);
                        float newCost = (tempCost == null ? 0 : tempCost) + game.getMovementCost(current, next);

                        if (!costSoFar.containsKey(next.pos) || newCost < costSoFar.get(next.pos)) {
                            costSoFar.put(next.pos, newCost);
                            float priority = newCost + heuristic(goal, next);
                            next.priority = priority;
                            frontier.add(next);
                            cameFrom.put(next.pos, current.pos);
                            //System.out.println("next: "+next + " current: "+ current);
                        }
                    }

                }

                if(cameFrom.size() != 0) {

                    List<Vector> path = new ArrayList<>();

                    Vector curr = goal.pos;

                    while (!curr.equals(start.pos)) {
                        path.add(curr);
                        curr = cameFrom.get(curr);
//                        System.out.println("here");
                    }
//                    System.out.println("here");
                    path.add(start.pos);
//                    System.out.println("here");
                    List<Vector> finalPath = new ArrayList<>();
                    for (int i = path.size() - 1; i >= 0; i--) {
                        finalPath.add(path.get(i));
                    }

                    Mesh pathMesh = createMeshFromPath(finalPath);
                    pathMesh.drawMode = GL_LINES;
                    ModelBuilder.addColor(pathMesh, new Vector(new float[]{0f, 1f, 0f, 1f}));
                    pathMesh.initOpenGLMeshData();

                    pathModel = new Model(game, pathMesh, identifier + "-path", false);
                }
                else {
                    System.out.println("could not find path");
                }

            } else {
                pathModel = null;
            }
        }catch (Exception e) {
//            e.printStackTrace();
            System.out.println("error while creating path");
        }

    }

    public Mesh createMeshFromPath(List<Vector> path) {
        List<Integer> indices = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        for(int i = 0;i < path.size()-1;i++) {
            indices.add(i);
            indices.add(i+1);

            Face tempFace = new Face();
            Vertex v1 = new Vertex();
            Vertex v2 = new Vertex();

            v1.setAttribute(i,Vertex.POSITION);
            v2.setAttribute(i+1,Vertex.POSITION);

            tempFace.addVertex(v1);
            tempFace.addVertex(v2);
            faces.add(tempFace);
        }
        List<List<Vector>> vertAttribs = new ArrayList<>();
        vertAttribs.add(path);
        Mesh pathMesh = new Mesh(indices,faces,vertAttribs);
        return pathMesh;
    }

    public float heuristic(GridNode a, GridNode b) {
        return a.pos.sub(b.pos).getNorm();
    }

    public Box selectBox(List<Box> boxes) {
        Box selected = null;
        float closestDist = Float.POSITIVE_INFINITY;

        for(Box b:boxes) {
            float dist = this.getPos().sub(b.getPos()).getNorm();
            if(dist < closestDist) {
                selected = b;
                closestDist = dist;
            }
        }
        return selected;
    }

    public void IGPS(String text) {
        String[] split = text.trim().split("\\s+");

        float[] radVals = new float[3];
        for(int i = 0;i < radVals.length;i++) {
            radVals[i] = Float.parseFloat(split[i]);
        }

        // Hidden method to call teleport option
        if(split.length > 3) {
            System.out.println("called teleport command. Teleporting to specified location...");
            radVals[2] *= -1;
            this.setPos(new Vector(radVals));
        }

        else if(split.length == 3) {

//            Commented below is a method to calculate coords only from A and C. Works because we know for a fact that
//            in this simulation the robot will never go out of bounds

//            float a = radVals[0];
//            float c = radVals[1];
//            float d = radVals[2];
//            Vector C = new Vector(new float[]{boundMin.get(0),boundMax.get(1)});
//            float ac = C.sub(boundMin).getNorm();
//
//            float angleA = (float) Math.cos((-(c*c) + (ac*ac) + (a*a)) / (2*ac*a));
//            float angleAComp = (float) ((Math.PI/2) - angleA);
//
//            Vector preFinal = new Vector(new float[]{(float) Math.cos(angleAComp), (float) Math.sin(angleAComp)}).scalarMul(a);
//            System.out.println("Calculated coordinates are: ");
//            Vector res = new Vector(new float[]{preFinal.get(0),pos.get(1),preFinal.get(1)});
//            res.display();
//            System.out.println("Actual coordinates: ");
//            Vector tempPos = new Vector(new float[]{pos.get(0),pos.get(1),pos.get(2)*-1});
//            tempPos.display();

            float x1,x2,x3,y1,y2,y3,r1,r2,r3,x,y;
            x1 = boundMin.get(0);
            y1 = boundMin.get(1);
            r1 = radVals[0];

            x2 = boundMin.get(0);
            y2 = boundMax.get(1);
            r2 = radVals[1];

            x3 = boundMax.get(0);
            y3 = boundMax.get(1);
            r3 = radVals[2];

//            Triangulation formula from ENED community site
            x = (-(y2 - y3)*(((y2*y2)-(y1*y1))+((x2*x2)-(x1*x1))+((r1*r1)-(r2*r2))) + (y1-y2)*(((y3*y3)-(y2*y2))+((x3*x3)-(x2*x2))+((r2*r2)-(r3*r3))))/(2*((x1-x2)*(y2-y3) - (x2-x3)*(y1-y2)));
            y = (-(x2 - x3)*(((x2*x2) - (x1*x1)) + ((y2*y2) - (y1*y1)) + ((r1*r1) - (r2*r2)))+((x1-x2)*(((x3*x3)-(x2*x2))+((y3*y3)-(y2*y2))+((r2*r2)-(r3*r3)))))/(2*((y1-y2)*(x2-x3) - (y2-y3)*(x1-x2)));

            System.out.println("Calculated coordinates are: ");
            Vector res = new Vector(new float[]{x,pos.get(1),y});
            res.display();
            System.out.println("Actual coordinates: ");
            pos.display();

        }

    }

}
