package ENED_Simulation;

import engine.DataStructure.GridNode;
import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
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
    public Box boxBeingPathFounded;
    public Vector home;
    public Vector barcodeBeingSearched;
    public float pathFindResolution = 1f;
    public boolean shouldLockSelectedBox = false;

//    Path finding and following fine tuning parameters
    public int maxPathFindingCount = 5000; //-1 = uncapped
    public float turnOnlyThreshhold = 0.90f;
    public float verticalOnlyThreshhold = 0.1f;

    public float movementCost = 1f;
    public float nearbyCollisionCost = 0f; //Right now, code for calculcating this is commented off in movementCost()
    public int nearbyCollisionRange = 3;
    public float heuristicWeight = 1f;

    public float berzierResolution = 50;
    public boolean hasPathFindingFailed = false;

    protected int[][] collisionMask;  //This mask includes everything except robot. This is only to be used for collision detection, not pathfinding

    public Robot(Simulation game,Mesh mesh, String identifier) {
        super(game,mesh, identifier);
        this.game = game;
        input = game.getInput();
        translationDirection = new Vector(3,0);
        home = new Vector(new float[]{3,1,-3,1});
    }

    @Override
    public void tick(ModelTickInput params) {

        translationDirection = new Vector(3,0);

        List<Model> doNotMask = new ArrayList<>();
        doNotMask.add(this);
        collisionMask = game.createCollisionArray(doNotMask);

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
            dropBox();
        }

        if(input.keyDownOnce(input.TWO)) {
            isManualControl = !isManualControl;
        }

        if(input.keyDownOnce(input.THREE)) {
            shouldShowPath = !shouldShowPath;
        }

        AI(params);

    }

    @Override
    public boolean isOkayToUpdatePosition(Vector newPos) {
        int i = (int)newPos.get(0);
        int j = -(int)newPos.get(2);

        return game.isVectorInsideWorld(newPos) && !(collisionMask[i][j] == 1);
    }

    public void dropBox() {
        if(boxPicked != null) {
            boxPicked.setPos(new Vector(new float[]{this.pos.get(0), 0, this.getPos().get(2)}));
            game.addBoxToAtDestination(boxPicked);
            boxPicked = null;
            boxBeingPathFounded = null;
            barcodeBeingSearched = null;
        }
    }

    public void checkChanges() {
        translationDirection = translationDirection.normalise();

        if(boxPicked == null) {
            checkShouldPickBox();
        }
        else {   //Already picked a box
            updatePickedBox();
            if(areVectorsApproximatelyEqual(home,this.pos)) { //Successfully reached home/destination. Dropping box
                dropBox();
            }
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
        Optional<Box> optional = game.boxesToBeSearched
                .stream()
                .filter(b -> b.isRobotInCorrectPositionToScan(this))
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
                boxPicked.setRandomColorToBoundingBox();
                boxPicked.shouldShowCollisionBox = true;
                boxPicked.isCollidable = false;
                boxBeingPathFounded = null;
            }
            else {
                System.out.println("Barcodes did not match. This is not the required box");
                currBox.setBoundingBoxColor(new Vector(new float[]{1,0,0,1}));
                currBox.shouldShowCollisionBox = true;
                game.addBoxToSearched(currBox);
            }

        }
    }

    public void AI(ModelTickInput params) {

        // Logic if still searching for box
        if(boxPicked==null) {
            if(shouldLockSelectedBox) {
                if(boxBeingPathFounded == null) {
                    boxBeingPathFounded = selectBox(game.boxesToBeSearched);
                }
            }
            else {
                boxBeingPathFounded = selectBox(game.boxesToBeSearched);
            }

            List<Model> modelsToAvoidCreatingCollisionMasks = new ArrayList<>();
            modelsToAvoidCreatingCollisionMasks.add(this);
            modelsToAvoidCreatingCollisionMasks.add(boxBeingPathFounded);

            int[][] collisionArray = game.createCollisionArray(modelsToAvoidCreatingCollisionMasks);
            if(!isPathToModelValid(collisionArray,boxBeingPathFounded)) {
                System.out.println("creating new path");
                pathFind(boxBeingPathFounded.getPos(), boxBeingPathFounded,collisionArray);
            }
        }

//        Logic if moving towards home/destination with box
        else {
            List<Model> modelsToAvoidCreatingCollisionMasks = new ArrayList<>();
            modelsToAvoidCreatingCollisionMasks.add(this);
            modelsToAvoidCreatingCollisionMasks.add(boxPicked);

            int[][] collisionArray = game.createCollisionArray(modelsToAvoidCreatingCollisionMasks);
            if(!isPathToVectorValid(collisionArray,home)) {
                System.out.println("creating new path");
                pathFind(home, null,collisionArray);
            }
        }

//        List<MOVEMENT> movements = getMovementFromPath(params);
//        if(!isManualControl) {
//            for (MOVEMENT m : movements) {
//                if (m == MOVEMENT.FORWARD) {
//                    moveForward(params);
//                }
//                if (m == MOVEMENT.BACKWARD) {
//                    moveBackward(params);
//                }
//                if (m == MOVEMENT.LEFT) {
//                    turnLeft(params);
//                }
//                if (m == MOVEMENT.RIGHT) {
//                    turnRight(params);
//                }
//            }
//        }

        if(!isManualControl) {
            Vector dir = getMovementFromPath(params);
            if(dir != null) {

                Matrix rotMatrix = this.getOrientation().getRotationMatrix();
                Vector robotX = rotMatrix.getColumn(0);
                Vector robotZ = rotMatrix.getColumn(2);
                float angle = -dir.getAngleBetweenVectors(robotX);

                Vector verticalAmount = robotX.cross(dir);
                Vector temp = new Vector(3, 1);
                float pointerDir = -verticalAmount.dot(temp);

//        Logic to move a certain amount
                Vector delta = robotZ.scalarMul(movementSpeed * params.timeDelta * Math.signum(pointerDir) * verticalAmount.getNorm());
                Vector newPos = getPos().add(delta);

                if (isOkayToUpdatePosition(newPos)) {
                    this.pos = newPos;
                    translationDirection = translationDirection.add(delta);
                }
//
////        Logic to rotate
                Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[]{0, 1, 0}), rotationSpeed * params.timeDelta * Math.signum(angle) * (float) Math.cos(Math.toRadians(Math.abs(angle))));
                Quaternion newQ = rot.multiply(getOrientation());
                setOrientation(newQ);
            }
        }

//        Logic to update path model                                  //Not implemented because this would mess with pathfind model
        if(pathModel != null && pathModel.mesh.getVertices().size() > 2) {
            Vector oldV = pathModel.mesh.getVertices().get(0);
            Vector next = pathModel.mesh.getVertices().get(1);
            float diff = next.sub(this.pos).getNorm();

            if(diff < next.sub(oldV).getNorm()) {
                pathModel.mesh.getVertices().set(0,this.pos);
                if(diff <= 2) {
                    pathModel.mesh.getVertices().remove(0);
                }
                pathModel.mesh =  createMeshFromPath(pathModel.mesh.getVertices());
            }
        }

        checkChanges();

    }

    public Vector getMovementFromPath(ModelTickInput params) {
        if(pathModel == null) {
//            return new ArrayList<>();
            return null;
        }

        List<MOVEMENT> movements = new ArrayList<>();
        List<Vector> vertices = pathModel.mesh.getVertices();

        Vector robotZ = this.getOrientation().getRotationMatrix().getColumn(2);
        Vector deltaZMove = new Vector(new float[]{0,0,1}).scalarMul(movementSpeed * params.timeDelta);
        Vector deltaZTurn = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), rotationSpeed * params.timeDelta).getRotationMatrix().getColumn(2);
        Vector avgMove = deltaZMove.add(deltaZTurn);
        float avgMoveSize = avgMove.getNorm();
        float moveSizeSoFar = 0;
        int counter = 0;
        Vector dir = null;


        while (moveSizeSoFar < avgMoveSize) {
            dir = vertices.get(counter + 1).sub(vertices.get(counter));
            moveSizeSoFar += dir.getNorm();
            counter++;
            if (counter == vertices.size()-1) {
                break;
            }
        }

        dir = dir.normalise();
        return dir;

//        Vector cross = robotZ.cross(dir);
//        Vector temp = new Vector(3,1);
//        float dist = cross.dot(temp);
//
//        float pointerDir = robotZ.dot(dir);
//
//        MOVEMENT verticalDirection;
//        if(pointerDir < 0) {
//            verticalDirection = MOVEMENT.FORWARD;
//        }
//        else {
//            verticalDirection = MOVEMENT.BACKWARD;
//        }
//
//        //Turn right
//        if(dist < 0) {
//            if(Math.abs(dist) <= verticalOnlyThreshhold) {
//                movements.add(verticalDirection);
//            }
//            else if(Math.abs(dist) > verticalOnlyThreshhold && Math.abs(dist) <= turnOnlyThreshhold) {
//                movements.add(verticalDirection);
//                movements.add(MOVEMENT.LEFT);
//            }
//            else {
//                movements.add(MOVEMENT.LEFT);
//            }
//        }
//        else {
//            if(Math.abs(dist) <= verticalOnlyThreshhold) {
//                movements.add(verticalDirection);
//            }
//            else if(Math.abs(dist) > verticalOnlyThreshhold && Math.abs(dist) <= turnOnlyThreshhold) {
//                movements.add(verticalDirection);
//                movements.add(MOVEMENT.RIGHT);
//            }
//            else {
//                movements.add(MOVEMENT.RIGHT);
//            }
//        }
//
//        return movements;
    }

    public boolean isLineOfSight(Vector from,Vector to,int[][] collisionArray) {

        Vector dir = to.sub(from);
        float dist = dir.getNorm();
        dir = dir.normalise();

        for(int i = 0;i < dist;i++) {
            Vector p = dir.scalarMul(i).add(from);

            int x = (int)p.get(0);
            int z = -(int)p.get(2);
            if(isWithingRangeOfCollision(p,collisionArray,nearbyCollisionRange) || collisionArray[x][z] == 1) {
                return false;
            }
        }

        return true;
    }

    public boolean isPathToModelValid(int[][] collisionArray,Model m) {
        if(pathModel == null) {
            return false;
        }

        Vector start = pathModel.mesh.getVertices().get(0);
//        if(!isCollidingModel(start,this)) {
//            return false;
//        }

        if(!areVectorsApproximatelyEqual(start,this.pos)) {
            return false;
        }

        Vector goal = pathModel.mesh.getVertices().get(pathModel.mesh.getVertices().size() - 1);
        if(!isCollidingModel(goal,m)) {
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

    public boolean isPathToVectorValid(int[][] collisionArray,Vector vecGoal) {
        if(pathModel == null) {
            return false;
        }

        Vector start = pathModel.mesh.getVertices().get(0);
//        if(!isCollidingModel(start,this)) {
//            return false;
//        }

        if(!areVectorsApproximatelyEqual(start,this.pos)) {
            return false;
        }

        Vector goal = pathModel.mesh.getVertices().get(pathModel.mesh.getVertices().size() - 1);
        if(!areVectorsApproximatelyEqual(goal,vecGoal)) {
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

    public boolean areVectorsApproximatelyEqual(Vector v1, Vector v2) {
        Vector a = new Vector(new float[]{(int)v1.get(0),(int)v1.get(2)});
        Vector b = new Vector(new float[]{(int)v2.get(0),(int)v2.get(2)});
        return a.equals(b);
    }

//    Implements theta A*.
//    Algorithm inspired from:
//    https://www.redblobgames.com/pathfinding/a-star/introduction.html
//    https://en.wikipedia.org/wiki/Theta*
    public void pathFind(Vector target,Model targetModel,int[][] collisionArray) {

        hasPathFindingFailed = false;
        boolean surr = isCompletelySurrounded(targetModel,collisionArray);

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

                    if (areVectorsApproximatelyEqual(current.pos,goal.pos) || (targetModel!=null && isCollidingModel(current.pos,targetModel))) {
                        hasReachedEnd = true;
                        goal = current;
                        break; //Reached end point
                    }

                    if(maxPathFindingCount != -1 && count >= maxPathFindingCount) {
                        goal = current;
                        hasPathFindingFailed = true;
                        break;
                    }

                    List<GridNode> neighbours = getNeighbours(current,collisionArray);
                    for (GridNode next : neighbours) {

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
                                frontier.remove(next);
                                next.priority = costSoFar.get(next.pos)+heuristic(goal.pos,next.pos);
                                frontier.add(next);

                            }
                        }
                        else {
                            if(costSoFar.get(current.pos) + getMovementCost(current.pos,next.pos) < costSoFar.get(next.pos)) {
                                costSoFar.put(next.pos,costSoFar.get(current.pos)+getMovementCost(current.pos,goal.pos));
                                cameFrom.put(next.pos,current.pos);
                                frontier.remove(next);
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
                        hasPathFindingFailed = true;
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

                    Mesh pathMesh = createMeshFromPath(smoothenPath(finalPath));

                    if(pathModel == null) {
                        pathModel = new Model(game, pathMesh, identifier + "-path", false);
                    }
                    else {
                        pathModel.mesh = pathMesh;
                    }
                }
                else {
                    hasPathFindingFailed = true;
                    pathModel = null;
                }

            } else {
                hasPathFindingFailed = true;
                pathModel = null;  //If target Pos is null
            }
    }

    public boolean isCompletelySurrounded(Model m,int[][] collisionArray) {
        if(m == null) {
            return false;
        }

        for(Vector v:game.getModelOutlineCollisionData(m)) {
            if(!isCollidingWithAnyModel(v,collisionArray)) {
                return false;
            }
        }

        return true;
    }

    public List<GridNode> getNeighbours(GridNode current,int[][] collisionArray) {
        List<GridNode> neighbours = new ArrayList<>();

        Vector n1 = current.pos.add(new Vector(new float[]{pathFindResolution,0,0}));
        Vector n2 = current.pos.add(new Vector(new float[]{-pathFindResolution,0,0}));
        Vector n3 = current.pos.add(new Vector(new float[]{0,0,pathFindResolution}));
        Vector n4 = current.pos.add(new Vector(new float[]{0,0,-pathFindResolution}));
        Vector n5 = current.pos.add(new Vector(new float[]{pathFindResolution,0,pathFindResolution}));
        Vector n6 = current.pos.add(new Vector(new float[]{-pathFindResolution,0,pathFindResolution}));
        Vector n7 = current.pos.add(new Vector(new float[]{pathFindResolution,0,-pathFindResolution}));
        Vector n8 = current.pos.add(new Vector(new float[]{-pathFindResolution,0,-pathFindResolution}));


        if(game.isVectorInsideWorld(n1) && !isWithingRangeOfCollision(n1,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n1,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n2) && !isWithingRangeOfCollision(n2,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n2,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n3) && !isWithingRangeOfCollision(n3,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n3,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n4) && !isWithingRangeOfCollision(n4,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n4,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n5) && !isWithingRangeOfCollision(n1,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n5,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n6) && !isWithingRangeOfCollision(n2,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n6,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n7) && !isWithingRangeOfCollision(n3,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n7,Float.POSITIVE_INFINITY));
        if(game.isVectorInsideWorld(n8) && !isWithingRangeOfCollision(n4,collisionArray,nearbyCollisionRange)) neighbours.add(new GridNode(n8,Float.POSITIVE_INFINITY));

        return neighbours;
    }

    public static boolean isWithingRangeOfCollision(Vector v,int[][] collisionArray, int range) {
        List<Vector> vecs = new ArrayList<>();
        vecs.add(v);
        for(int j = 0;j < range;j++) {
            vecs.add(v.add(new Vector(new float[]{j, 0, 0})));
            vecs.add(v.add(new Vector(new float[]{-j, 0, 0})));
            vecs.add(v.add(new Vector(new float[]{0, 0, j})));
            vecs.add(v.add(new Vector(new float[]{0, 0, -j})));
        }

        for(Vector vert: vecs) {
            if(isCollidingWithAnyModel(vert,collisionArray)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCollidingWithAnyModel(Vector v, int[][] collisionArray) {
        if(v.get(0) < 0 || v.get(0) >= collisionArray.length || -v.get(2) < 0 || -v.get(2) >= collisionArray[0].length) {
            return false;
        }

        return collisionArray[(int) v.get(0)][-(int) v.get(2)] == 1;
    }

    public boolean isCollidingModel(Vector v, Model m) {

        Vector[] bounds = Model.getBounds(m.getObjectToWorldMatrix().matMul(m.boundingbox.getVertices()).convertToColumnVectorList());
        Vector boundMin = bounds[0];
        Vector boundMax = bounds[1];

        return boundMin != null && boundMax != null
                && v.get(0) >= boundMin.get(0) && v.get(0) <= boundMax.get(0)
                && v.get(2) >= boundMin.get(2) && v.get(2) <= boundMax.get(2);
    }

    public float getMovementCost(Vector current, Vector next) {
        float collisionCost = 0;

//        List<Vector> vecs = new ArrayList<>();
//
//        vecs.add(next.add(new Vector(new float[]{1, 0, 0})));
//        vecs.add(next.add(new Vector(new float[]{-1, 0, 0})));
//        vecs.add(next.add(new Vector(new float[]{0, 0, 1})));
//        vecs.add(next.add(new Vector(new float[]{0, 0, -1})));
//        vecs.add(next.add(new Vector(new float[]{1,0,1})));
//        vecs.add(next.add(new Vector(new float[]{-1,0,1})));
//        vecs.add(next.add(new Vector(new float[]{1,0,-1})));
//        vecs.add(next.add(new Vector(new float[]{-1,0,-1})));
//
//        for(Vector v:vecs) {
//            int i = (int)v.get(0);
//            int j = -(int)v.get(2);
//
//            if(i >= 0 && i < collisionArray.length && j >= 0 && j < collisionArray[0].length) {
//                if (collisionArray[i][j] == 1) {
//                    collisionCost += 1;
//                }
//            }
//        }

        float movementCost = current.sub(next).getNorm();
        return movementCost* this.movementCost + collisionCost* nearbyCollisionCost;
    }

    public List<Vector> smoothenPath(List<Vector> path) {

        if(path.size() < 3) {
            return path;
        }

        List<Vector> newPath = new ArrayList<>();

        List<Vector> tempVerts = new ArrayList<>();
        tempVerts.add(path.get(0));

        for(int i = 1;i < path.size();i+=1) {
            tempVerts.add(path.get(i-1).add(path.get(i)).scalarMul(0.5f));  //Add point to middle
            tempVerts.add(path.get(i));
        }

        newPath.add(tempVerts.get(0));
        for(int i = 2;i < tempVerts.size();i+=2) {

            if(i != tempVerts.size()-1) {
                Vector control = tempVerts.get(i);
                Vector start = tempVerts.get(i - 1).add(control).scalarMul(0.5f);
                Vector end = tempVerts.get(i + 1).add(control).scalarMul(0.5f);
//            Vector start = tempVerts.get(i-1);
//            Vector end = tempVerts.get(i+1);


                if (Math.abs(control.sub(tempVerts.get(i - 1)).normalise().dot(tempVerts.get(i + 1).sub(control).normalise())) < 0.9) {  //Find additional points only if not straight
                    newPath.add(start);
                    for (int k = 0; k < berzierResolution; k++) {
                        Vector v = q(k / berzierResolution, start, control, end);
                        newPath.add(v);
                    }
                    newPath.add(end);
                }

                newPath.add(tempVerts.get(i + 1));
            }
            else {
                newPath.add(tempVerts.get(i));
            }
        }

        System.out.println("path size: "+path.size());
        System.out.println("temp verts: "+tempVerts.size());
        System.out.println("new path: "+ newPath.size());

        return newPath;
    }

    public Vector q(float t,Vector start, Vector control, Vector end) {
        return ((start.scalarMul(1-t).add(control.scalarMul(t))).scalarMul(1-t)).add((control.scalarMul(1-t).add(end.scalarMul(t))).scalarMul(t));
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
        pathMesh.drawMode = GL_LINES;
        ModelBuilder.addColor(pathMesh, new Vector(new float[]{0f, 1f, 0f, 1f}));
        pathMesh.initOpenGLMeshData();
        return pathMesh;
    }

    public float heuristic(Vector goal, Vector p) {
        return goal.sub(p).getNorm() * heuristicWeight;
        //return 0;
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
