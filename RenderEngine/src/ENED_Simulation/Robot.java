package ENED_Simulation;

import engine.DataStructure.GridNode;
import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.inputs.Input;
import engine.model.Model;
import engine.model.ModelBuilder;
import engine.model.Movable;
import jdk.swing.interop.SwingInterOpUtils;

import java.util.*;

import static org.lwjgl.opengl.GL11C.GL_LINES;

public class Robot extends Movable {

    private Simulation game;

    private Input input;
    public float scanRadius = 5;
    private Box boxPicked;
    public Box boxBeingPathFounded;
    public Vector home;
    public Vector barcodeBeingSearched;
    public float pathFindResolution = 1f;
    public boolean shouldLockSelectedBox = true;
    public int maxPathFindingCount = 1000; //-1 = uncapped

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
            shouldDropBox();
        }

        if(!isManualControl) {
            autoMove();
        }

        if(boxPicked==null) {
            if(shouldLockSelectedBox) {
                if(boxBeingPathFounded == null) {
                    boxBeingPathFounded = selectBox(game.boxes);
                }
            }
            else {
                boxBeingPathFounded = selectBox(game.boxes);
            }

            List<Model> modelsToAvoid = new ArrayList<>();
            modelsToAvoid.add(this);
            modelsToAvoid.add(boxBeingPathFounded);

           int[][] collisionArray = game.createCollisionArray(modelsToAvoid);
            if(!isPathValid(collisionArray)) {
                pathFind(boxBeingPathFounded.getPos(), boxBeingPathFounded,collisionArray);
            }
        }

        finalUpdate();

    }

    public void shouldDropBox() {
        if(boxPicked != null) {
            boxPicked.setPos(new Vector(new float[]{this.pos.get(0), 0, this.getPos().get(2)}));
            game.addBoxToDropped(boxPicked);
            boxPicked = null;
            boxBeingPathFounded = null;
            barcodeBeingSearched = null;
        }
    }

    public void finalUpdate() {
        translationDirection = translationDirection.normalise();

        if(boxPicked == null) {
            checkShouldPickBox();
        }
        else {   //Already picked a box
            updatePickedBox();
        }

        if(barcodeBeingSearched == null) {
            Box search = game.requestNextBarcode();
            System.out.println("box being searched: "+search + " barcode: "+search.barCode);
            barcodeBeingSearched = search.barCode;
        }
    }

    public void updatePickedBox() {
        boxPicked.setPos(this.pos.add(new Vector(new float[]{0,2,0})));
        boxPicked.setOrientation(this.getOrientation());
    }

    public void checkShouldPickBox() {
        Matrix robotMatrix = this.getOrientation().getRotationMatrix();
        Optional<Box> optional = game.boxes
                .stream()
                .filter(b -> b.isRobotInCorrectPositionToScan(this, robotMatrix))
                .findFirst();
        if (optional.isPresent()) {

            Box currBox = optional.get();
            System.out.println("Box scanned is: " + currBox.identifier);
            System.out.println("barcode: "+currBox.barCode);

//                Box picked only if correct barcode
            if(currBox.barCode.equals(barcodeBeingSearched)) {
                System.out.println("Required box was found");
                boxPicked = currBox;
                boxPicked.barCode.display();
                pathModel = null;
            }
            else {
                System.out.println("Barcodes did not match. This is not the required box");
            }

        }
    }

    public void autoMove() {

    }

    public boolean isLineOfSight(Vector from,Vector to,int[][] collisionArray) {
        Vector dir = to.sub(from);
        float dist = dir.getNorm();
        dir = dir.normalise();

        for(int i = 0;i < dist;i++) {
            Vector p = dir.scalarMul(i).add(from);
            int x = (int)p.get(0);
            int z = -(int)p.get(2);
            if(collisionArray[x][z] == 1) {
                return false;
            }
        }

        return true;
    }

    public boolean isPathValid(int[][] collisionArray) {
        if(pathModel == null) {
            return false;
        }

        Vector start = pathModel.mesh.getVertices().get(0);
        if(!isCollidingModel(start,this)) {
            return false;
        }

        Vector goal = pathModel.mesh.getVertices().get(pathModel.mesh.getVertices().size() - 1);
        if(!isCollidingModel(goal,boxBeingPathFounded)) {
            return false;
        }

        for(Vector v: pathModel.mesh.getVertices()) {
            int i = (int)v.get(0);
            int j = -(int)v.get(2);
            if(collisionArray[i][j] == 1) {
                return false;
            }
        }

        return true;

    }

//    Implements theta A*.
//    Algorithm inspired from:
//    https://www.redblobgames.com/pathfinding/a-star/introduction.html
//    https://en.wikipedia.org/wiki/Theta*
    public void pathFind(Vector target,Model targetModel,int[][] collisionArray) {

        boolean surr = isCompletelySurrounded(targetModel,collisionArray);
//            boolean surr = false;
            if (target != null && !surr) {

                boolean hasReachedEnd = false;
                int count = 0;

                PriorityQueue<GridNode> frontier = new PriorityQueue<>();

                Vector pos = new Vector(new float[]{this.pos.get(0),this.pos.get(1),this.pos.get(2)});

                GridNode start = new GridNode(pos, 0);
                frontier.add(start);

                HashMap<Vector, Vector> cameFrom = new HashMap<>();
                HashMap<Vector, Float> costSoFar = new HashMap<>();

                cameFrom.put(start.pos, null);
                costSoFar.put(start.pos, 0f);

                costSoFar.put(null, 0f);

                Vector tempGoal = new Vector(new float[]{target.get(0),this.getPos().get(1), target.get(2)});
                GridNode goal = new GridNode(tempGoal, 0);

                GridNode current = null;

                while (!frontier.isEmpty()) {
                    current = frontier.poll();
                    count++;

                    if (current.equals(goal) || (targetModel!=null && isCollidingModel(current.pos,targetModel))) {
                        hasReachedEnd = true;
                        goal = current;
                        break; //Reached end point
                    }

                    if(maxPathFindingCount != -1 && count >= maxPathFindingCount) {
                        goal = current;
                        break;
                    }

                    List<GridNode> neighbours = getNeighbours(current,collisionArray);
                    for (GridNode next : neighbours) {
//                        Float tempCost = costSoFar.get(current.pos);
//                        float newCost = (tempCost == null ? 0 : tempCost) + getMovementCost(current.pos, next.pos);

                        if (!costSoFar.containsKey(next.pos)) {
                            costSoFar.put(next.pos,Float.POSITIVE_INFINITY);
                            cameFrom.put(next.pos,null);
                        }

//                       Logic to update vertex
                        Vector parent = cameFrom.get(current.pos);

                        if(parent != null && isLineOfSight(parent,next.pos,collisionArray)) {
                            if(costSoFar.get(parent)+
                                    getMovementCost(parent,next.pos)
                                    < costSoFar.get(next.pos)) {
                                costSoFar.put(next.pos,costSoFar.get(parent)+getMovementCost(parent,next.pos));
                                cameFrom.put(next.pos,parent);
//                                Might have the remove below if statement
                                if(frontier.contains(next)) {
                                    frontier.remove(next);
                                }
                                next.priority = costSoFar.get(next.pos)+heuristic(goal.pos,next.pos);
                                frontier.add(next);

                            }
                        }
                        else {
                            if(costSoFar.get(current.pos) + getMovementCost(current.pos,next.pos) < costSoFar.get(next.pos)) {
                                costSoFar.put(next.pos,costSoFar.get(current.pos)+getMovementCost(current.pos,goal.pos));
                                cameFrom.put(next.pos,current.pos);
                                if(frontier.contains(next)) {
                                    frontier.remove(next);
                                }
                                next.priority = costSoFar.get(next.pos)+heuristic(goal.pos,next.pos);
                                frontier.add(next);
                            }
                        }

                    }

                }

                if(!hasReachedEnd) goal = current;

                if(targetModel != null) {
                    if(!isCollidingModel(goal.pos,targetModel)) {
                        pathModel = null;
                        return;
                    }
                }

                if(cameFrom.size() > 1) {
                    List<Vector> path = new ArrayList<>();
                    Vector curr = goal.pos;
                    while (!curr.equals(start.pos)) {
                        path.add(curr);
                        curr = cameFrom.get(curr);
                    }
                    path.add(start.pos);

                    List<Vector> finalPath = new ArrayList<>();
                    for (int i = path.size() - 1; i >= 0; i--) {
                        finalPath.add(path.get(i));
                    }

                    Mesh pathMesh = createMeshFromPath(finalPath);
                    pathMesh.drawMode = GL_LINES;
                    ModelBuilder.addColor(pathMesh, new Vector(new float[]{0f, 1f, 0f, 1f}));
                    pathMesh.initOpenGLMeshData();

                    if(pathModel == null) {
                        pathModel = new Model(game, pathMesh, identifier + "-path", false);
                    }
                    else {
                        pathModel.mesh = pathMesh;
                    }
                }
                else {
                    pathModel = null;
                }

            } else {
                pathModel = null;  //If target Pos is null
            }
    }

    public boolean isCompletelySurrounded(Model m,int[][] collisionArray) {

        List<Vector> boundData = new ArrayList<>();

        List<Vector> vertices = new ArrayList<>();
        vertices.add(m.boundingbox.getVertices().get(0));
        vertices.add(m.boundingbox.getVertices().get(2));
        vertices.add(m.boundingbox.getVertices().get(4));
        vertices.add(m.boundingbox.getVertices().get(6));

        vertices = m.getObjectToWorldMatrix().matMul(vertices).convertToColumnVectorList();

        Vector v1 = vertices.get(0);
        Vector v2 = vertices.get(1);
        Vector v3 = vertices.get(2);
        Vector v4 = vertices.get(3);

        Vector edge1 = v2.sub(v1);
        Vector edge2 = v4.sub(v1);

        int dist1 = (int)edge1.getNorm();
        int dist2 = (int)edge2.getNorm();

        edge1 = edge1.normalise();
        edge2 = edge2.normalise();

        for (int t1 = 0; t1 <= dist1; t1++) {
            Vector p1 = edge1.scalarMul(t1).add(v1);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < game.simWidth && j >= 0 && j < game.simDepth) {
                boundData.add(p1);
            }
        }

        for (int t1 = 0; t1 <= dist1; t1++) {
            Vector p1 = edge1.scalarMul(t1).add(v4);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < game.simWidth && j >= 0 && j < game.simDepth) {
                boundData.add(p1);
            }
        }

        for (int t1 = 0; t1 <= dist2; t1++) {
            Vector p1 = edge2.scalarMul(t1).add(v1);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < game.simWidth && j >= 0 && j < game.simDepth) {
                boundData.add(p1);
            }
        }

        for (int t1 = 0; t1 <= dist2; t1++) {
            Vector p1 = edge2.scalarMul(t1).add(v2);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < game.simWidth && j >= 0 && j < game.simDepth) {
                boundData.add(p1);
            }
        }

        for(Vector v:boundData) {
            if(!isCollidingWithAnyModel(v,collisionArray)) {
                return false;
            }
        }

//        if(!isCollidingWithAnyModel(m.getPos(),collisionArray)) {
//            return false;
//        }

        return true;
    }

    public List<GridNode> getNeighbours(GridNode current,int[][] collisionArray) {
        List<GridNode> neighbours = new ArrayList<>();

        Vector n1 = current.pos.add(new Vector(new float[]{pathFindResolution,0,0}));
        Vector n2 = current.pos.add(new Vector(new float[]{-pathFindResolution,0,0}));
        Vector n3 = current.pos.add(new Vector(new float[]{0,0,pathFindResolution}));
        Vector n4 = current.pos.add(new Vector(new float[]{0,0,-pathFindResolution}));


        if(game.isVectorInsideWorld(n1) && !isCollidingWithAnyModel(n1,collisionArray)) neighbours.add(new GridNode(n1,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n2)  && !isCollidingWithAnyModel(n2,collisionArray)) neighbours.add(new GridNode(n2,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n3)  && !isCollidingWithAnyModel(n3,collisionArray)) neighbours.add(new GridNode(n3,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n4)  && !isCollidingWithAnyModel(n4,collisionArray)) neighbours.add(new GridNode(n4,Float.POSITIVE_INFINITY));

        return neighbours;
    }

    public boolean isCollidingWithAnyModel(Vector v, int[][] collisionArray) {

        if(v.get(0) < 0 || v.get(0) >= collisionArray.length || -v.get(2) < 0 || -v.get(2) >= collisionArray[0].length) {
            return true;
        }

        if(collisionArray[(int)v.get(0)][-(int)v.get(2)] == 1) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isCollidingModel(Vector v, Model m) {

        Vector[] bounds = Model.getBounds(m.getObjectToWorldMatrix().matMul(m.boundingbox.getVertices()).convertToColumnVectorList());
        Vector boundMin = bounds[0];
        Vector boundMax = bounds[1];

        Vector pos = v;

        if (boundMin != null && boundMax != null
                && pos.get(0) >= boundMin.get(0) && pos.get(0) <= boundMax.get(0)
//                       && pos.get(1) > boundMin.get(1) - 1 && pos.get(1) < boundMax.get(1) + 1
                && pos.get(2) >= boundMin.get(2) && pos.get(2) <= boundMax.get(2)) {
            return true;
        }

        return false;
    }

    public float getMovementCost(Vector current,Vector next) {
        return current.sub(next).getNorm();
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

    public float heuristic(Vector goal, Vector p) {
        return goal.sub(p).getNorm();
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
