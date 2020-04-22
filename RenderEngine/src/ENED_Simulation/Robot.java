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

//    Path finding and following fine tuning parameters
    public int maxPathFindingCount = 5000; //-1 = uncapped
    public float turnOnlyThreshhold = 0.90f;
    public float verticalOnlyThreshhold = 0.1f;

    public float movementCost = 1f;
    public float nearbyCollisionCost = 50f; //Right now, code for calculcating this is commented off in movementCost()
    public int nearbyCollisionRange = 3;
    public float heuristicWeight = 1f;

    public float berzierResolution = 50;
    public float pathFindDistanceFromModel = 1.5f;

    public float minimumPathFindTimeInterval = 0.1f;
    public float timePassedSinceLastPathFind = 0;

    public float minimumIsStuckInterval = 0.1f;
    public float timePassedSinceLastStuckCheck = 0;
    public boolean isStuck = false;

    protected int[][] collisionMask;  //This mask includes everything except robot and models marked as isCollidable = false
    public List<Vector> boundData;

    public Robot(Simulation game,Mesh mesh, String identifier) {
        super(game,mesh, identifier);
        this.game = game;
        input = game.getInput();
        home = new Vector(new float[]{5,1,-5});
    }

    @Override
    public void tick(ModelTickInput params) {
        timePassedSinceLastPathFind += params.timeDelta;
        timePassedSinceLastStuckCheck += params.timeDelta;
        translationDirection = new Vector(3,0);
        rotationDirection = new Vector(3,0);
        finalMovement = new Vector(3,0);

        List<Model> doNotMask = new ArrayList<>();
        doNotMask.add(this);
        doNotMask.add(boxPicked);

        collisionMask = game.createCollisionArray(doNotMask);
        boundData = game.getModelFillCollisionData(this);

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
        checkChanges(params);
        finalMovement = rotationDirection.add(translationDirection);

        if(timePassedSinceLastPathFind >= minimumPathFindTimeInterval) {
            timePassedSinceLastPathFind = 0;
        }
        if(timePassedSinceLastStuckCheck >= minimumIsStuckInterval) {
            if(finalMovement.getNorm() == 0) {
                isStuck = true;
            }
            timePassedSinceLastStuckCheck = 0;
        }

    }

    @Override
    public boolean isOkayToUpdatePosition(Vector newPos) {
        //return !isCollidingWithAnyModel(boundData,newPos.sub(pos).addDimensionToVec(1),collisionMask);

        int i = (int)newPos.get(0);
        int j = -(int)newPos.get(2);
        return game.isVectorInsideWorld(newPos) && !(collisionMask[i][j] == 1);
    }

//    @Override
//    public boolean isOkayToUpdateRotation(Matrix rot) {
//        return !isCollidingWithAnyModel(boundData,rot,collisionMask);
//    }

    public void dropBox() {
        if(boxPicked != null) {
            boxPicked.setPos(new Vector(new float[]{this.pos.get(0), 0, this.getPos().get(2)}));
            game.addBoxToAtDestination(boxPicked);
            boxPicked = null;
            boxBeingPathFounded = null;
            barcodeBeingSearched = null;
        }
    }

    public void checkChanges(ModelTickInput params) {
        translationDirection = translationDirection.normalise();

        if(boxPicked == null) {
            orientTowardsBoxIfNear(params);
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
            if(search == null) {
                System.out.println("picked all boxes");
            }
            else {
                System.out.println("box being searched: " + search + " barcode: " + search.barCode);
                barcodeBeingSearched = search.barCode;
            }
        }
    }

//    Returns true if actually orienting towards the box
    public boolean orientTowardsBoxIfNear(ModelTickInput params) {

        if(pathModel != null && pathModel.mesh != null && pathModel.mesh.getVertices().size() >= 2) {
            return false;
        }

        Vector directionFromCentre = getDirectionToFrontFromCentre(boxBeingPathFounded);

        Matrix robotMatrix = this.getOrientation().getRotationMatrix();
        Matrix robotInBoxView = boxBeingPathFounded.getWorldToObject().matMul(pos.addDimensionToVec(1));

        float robotZ = robotInBoxView.getColumn(0).add(directionFromCentre.addDimensionToVec(1)).get(2);  // z position of robot from box's perspective
        float robotX = robotInBoxView.getColumn(0).sub(directionFromCentre.addDimensionToVec(1)).get(0);;
        float dist = new Vector(new float[]{robotX,robotZ}).getNorm();

        if (dist <= scanRadius && robotZ >= 0  && robotX <= boxBeingPathFounded.scanXProximity && robotX >= -boxBeingPathFounded.scanXProximity) {

            Matrix boxMatrix = boxBeingPathFounded.getOrientation().getRotationMatrix();
            Vector cross = boxMatrix.getColumn(2).cross(robotMatrix.getColumn(0));
            Vector temp = new Vector(new float[]{1, 1, 1});
            float rotation = cross.dot(temp);
            float verticalDirection = boxMatrix.getColumn(2).dot(robotMatrix.getColumn(0));

            if (verticalDirection > boxBeingPathFounded.scanDirSensitivity
                    && robotZ >= 0 && robotZ <= scanRadius
                    && Math.abs(rotation) < boxBeingPathFounded.scanRotationRange
                    && robotX <= boxBeingPathFounded.scanXProximity && robotX >= 0) {

               //Reached correct position
                move(params,-movementSpeed*1);
                return true;
            }

//            If robot is too far away in the z direction (relative to box) from the box but everything else is fine or if it is perpendicular to the box
            if (verticalDirection > boxBeingPathFounded.scanDirSensitivity
                    && robotZ > scanRadius
                                ||
                    (!(verticalDirection > boxBeingPathFounded.scanDirSensitivity)
                    && (Math.abs(rotation) < boxBeingPathFounded.scanRotationRange)
                    && !(robotX <= boxBeingPathFounded.scanXProximity && robotX >= 0))
                                ||
                    ((verticalDirection > boxBeingPathFounded.scanDirSensitivity)
                    && robotX < 0
                    && (Math.abs(rotation) < boxBeingPathFounded.scanRotationRange)))  {

                move(params,movementSpeed);
                turn(params,-rotationSpeed);

                return true;
            }

            turn(params,rotationSpeed * Math.signum(-rotation));
            return true;
        }

        return false;
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
                boxPicked.isCollidable = false;
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
                boxBeingPathFounded = null;
                game.addBoxToSearched(currBox);
            }

        }
    }

    public void AI(ModelTickInput params) {

        updatePathFinding();

        boolean isOrientingTowardsBox = false;
        if(!isManualControl && boxPicked == null && boxBeingPathFounded != null && orientTowardsBoxIfNear(params)) {
            isOrientingTowardsBox = true;
        }

        if(!isManualControl && !isOrientingTowardsBox) {
            followPath(params);
        }

        if(isStuck) {
            isStuck = false;

            if(!isManualControl) {
                if (!moveForward(params)) {
                    moveBackward(params);
                }

                if (!turnLeft(params)) {
                    turnRight(params);
                }
            }
        }

//      Logic to refactor path structure
        refactorPath();

    }

    public void updatePathFinding() {

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

            if(boxBeingPathFounded != null) {
                Vector destination = getModelSearchDestination(boxBeingPathFounded);
                if (!isPathToVectorValid(collisionMask, destination)) {
                    pathFind(destination, null, collisionMask);
                }
            }

        }
//        Logic if moving towards home/destination with box
        else {
            if(!isPathToVectorValid(collisionMask,home)) {
                pathFind(home, null,collisionMask);
            }
        }
    }

    //        Logic to update path model
    public void refactorPath() {
        if(pathModel != null && pathModel.mesh.getVertices().size() > 2) {
            Vector oldV = pathModel.mesh.getVertices().get(0);
            Vector next = pathModel.mesh.getVertices().get(1);
            float diff = next.sub(this.pos).getNorm();

//            if(diff < next.sub(oldV).getNorm()) {
            pathModel.mesh.getVertices().set(0,this.pos);
            if(diff <= 2) {
                pathModel.mesh.getVertices().remove(0);
            }
            pathModel.mesh =  createMeshFromPath(pathModel.mesh.getVertices());
//            }
        }
    }

    public void followPath(ModelTickInput params) {
        Vector dir = getMovementFromPath(params);
        if (dir != null) {

            Matrix rotMatrix = this.getOrientation().getRotationMatrix();
            Vector robotX = rotMatrix.getColumn(0);
            Vector robotZ = rotMatrix.getColumn(2);
            float angle = -dir.getAngleBetweenVectors(robotX);

            Vector verticalAmount = robotX.cross(dir);
            Vector temp = new Vector(3, 1);
            float pointerDir = -verticalAmount.dot(temp);

//             Logic to move a certain amount
            Vector delta = robotZ.scalarMul(movementSpeed * params.timeDelta * Math.signum(pointerDir) * verticalAmount.getNorm());
            Vector newPos = getPos().add(delta);

            if (isOkayToUpdatePosition(newPos)) {
                this.pos = newPos;
                translationDirection = translationDirection.add(delta);
            }
//
////            Logic to rotate
            float angleFinal = rotationSpeed * params.timeDelta * Math.signum(angle) * (float) Math.cos(Math.toRadians(Math.abs(angle)));
            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[]{0, 1, 0}), angleFinal);
            if (isOkayToUpdateRotation(rot.getRotationMatrix())) {
                Vector tempRot = new Vector(new float[]{(float)Math.sin(angleFinal),0,(float)Math.cos(angleFinal)});
                rotationDirection = rotationDirection.add(tempRot);
                Quaternion newQ = rot.multiply(getOrientation());
                setOrientation(newQ);
            }
        }
    }

    public Vector getModelSearchDestination(Model search) {
        Vector dir = getDirectionToFrontFromCentre(search);
        Vector norm = dir.normalise();
        Vector offset = norm.scalarMul(pathFindDistanceFromModel);
        Vector res = search.getPos().add(dir).add(offset);
        return res;
    }

    public Vector getDirectionToFrontFromCentre(Model search) {
        Vector[] bounds = Model.getBounds(search.boundingbox);
        float deltaZ = (bounds[1].get(2) - bounds[0].get(2)) / 2f;

        Vector z = search.getOrientation().getRotationMatrix().getColumn(2);
        Vector res = z.scalarMul(deltaZ);
        return res;
    }

    public Vector getMovementFromPath(ModelTickInput params) {
        if(pathModel == null) {
            return null;
        }

        List<Vector> vertices = pathModel.mesh.getVertices();
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

        if(timePassedSinceLastPathFind < minimumPathFindTimeInterval) {
            return;
        }

        //boolean surr = isCompletelySurrounded(targetModel,collisionArray);
        boolean surr = false;

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
                    pathModel = null;
                }

            } else {
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

    public static boolean isCollidingWithAnyModel(List<Vector> vList, Vector offset, int[][] collisionArray) {
        for(Vector v: vList) {
           Vector t = v.add(offset);
            if (t.get(0) < 0 || t.get(0) >= collisionArray.length || -t.get(2) < 0 || -t.get(2) >= collisionArray[0].length) {
               return true;
            }
            if(collisionArray[(int) t.get(0)][-(int) t.get(2)] == 1) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCollidingWithAnyModel(List<Vector> vList, Matrix rot, int[][] collisionArray) {
        for(Vector v: vList) {
            Vector t = rot.matMul(v.removeDimensionFromVec(3)).getColumn(0);
            if (t.get(0) < 0 || t.get(0) >= collisionArray.length || -t.get(2) < 0 || -t.get(2) >= collisionArray[0].length) {
                return true;
            }
            if(collisionArray[(int) t.get(0)][-(int) t.get(2)] == 1) {
                return true;
            }
        }

        return false;
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

        List<Vector> vecs = new ArrayList<>();

        vecs.add(next.add(new Vector(new float[]{1, 0, 0})));
        vecs.add(next.add(new Vector(new float[]{-1, 0, 0})));
        vecs.add(next.add(new Vector(new float[]{0, 0, 1})));
        vecs.add(next.add(new Vector(new float[]{0, 0, -1})));
        vecs.add(next.add(new Vector(new float[]{1,0,1})));
        vecs.add(next.add(new Vector(new float[]{-1,0,1})));
        vecs.add(next.add(new Vector(new float[]{1,0,-1})));
        vecs.add(next.add(new Vector(new float[]{-1,0,-1})));

        for(Vector v:vecs) {
            int i = (int)v.get(0);
            int j = -(int)v.get(2);

            if(i >= 0 && i < collisionMask.length && j >= 0 && j < collisionMask[0].length) {
                if (collisionMask[i][j] == 1) {
                    collisionCost += 1;
                }
            }
        }

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
