package engine.Terrain;

import engine.Mesh.Face;
import engine.Mesh.Mesh;
import engine.Mesh.Vertex;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.geometry.TerrainUtils;
import engine.game.Game;
import engine.model.Model;

import java.util.ArrayList;
import java.util.List;

public class Terrain extends Model {

    public int width, height,facesPerCell;
    private List<Vector> lastKnownTrig = new ArrayList<>();
    private int searchMaxLimit = 40;
    public int minimumProximity = 2;

    public Terrain(Game game, Mesh mesh, String identifier,int width, int height, int facesPerCell) {
        super(game, mesh, identifier);
        this.width = width;
        this.height = height;
        this.facesPerCell = facesPerCell;
    }

    public class TerrainMovementDataPack {
        public boolean isValid;
        public Vector validPosition;
        TerrainMovementDataPack(boolean isValid, Vector mostRecentValidPosition) {
            this.isValid = isValid;
            this.validPosition = mostRecentValidPosition;
        }
    }

    public TerrainMovementDataPack isPositionValid(Vector newPos) {

        Vector updatedPos = null;
        boolean isValid = false;

        List<Vector> trig = this.getFaceAtPos(newPos);
        Vector avg = Vector.getAverage(trig);
        if(avg != null) {
            if(avg.getNumberOfDimensions() == 5) {
                updatedPos = newPos;
                isValid = true;
            }
            else {
                float interPolatedHeight = TerrainUtils.interpolateHeightFromTriangle(trig,newPos);
                if(newPos.get(1)-minimumProximity >= interPolatedHeight) {
                    updatedPos = newPos;
                    isValid = true;
                }
                else {
                    Vector tAvg = new Vector(avg);
                    tAvg.setDataElement(1, interPolatedHeight + minimumProximity + 0.2f);
                    updatedPos = tAvg;
                    isValid = true;
                }
            }
        }
        else {
           updatedPos = newPos;
            isValid = true;
        }

        return new TerrainMovementDataPack(isValid,updatedPos);

    }

    private List<Vector> getFaceAtPos(Vector reqPos) {
        Vector orig = new Vector(reqPos);

        Vector min = this.getBoundingBox().getVertices().get(0);
        Vector max = this.getBoundingBox().getVertices().get(this.getBoundingBox().getVertices().size() - 3);

        Matrix worldToModel = this.getWorldToObject();
        Matrix modelToWorld = this.getObjectToWorldMatrix();

        Vector min2 = worldToModel.matMul(modelToWorld.matMul(min)).getColumn(0).removeDimensionFromVec(3);
        Vector max2 = worldToModel.matMul(modelToWorld.matMul(max)).getColumn(0).removeDimensionFromVec(3);
        min2.setDataElement(1,0);
        max2.setDataElement(1,0);
        max2 = max2.sub(min2);

        reqPos = worldToModel.matMul(reqPos.append(1)).getColumn(0).removeDimensionFromVec(3);
        reqPos.setDataElement(1,0);
        reqPos = reqPos.sub(min2);

        if(reqPos.get(0) < 0 || reqPos.get(0) > max2.get(0) || reqPos.get(2) < 0 || reqPos.get(2) > max2.get(2)) {
            Vector temp = new Vector(5,0);
            lastKnownTrig = new ArrayList<>();
            lastKnownTrig.add(temp);
            lastKnownTrig.add(temp);
            lastKnownTrig.add(temp);
            return lastKnownTrig;
        }

            float xCoord,zCoord;
            xCoord = (reqPos.get(0)) / this.scale.get(0) * width;
            zCoord = (reqPos.get(2)) / this.scale.get(2) * height;
            Face selected = null;
            boolean isFound = false;
            List<Vector> vertices = this.meshes.get(0).getVertices();
            List<Vector> selectedTrigCoords = null;
            Vector avg = null;
            int count = 0;

            do {
                int faceCoord = (int) ((xCoord * height * facesPerCell) + (zCoord * facesPerCell));

                if (faceCoord >= this.meshes.get(0).faces.size() || faceCoord < 0) {
                    isFound = false;
                }

                else {
                    Face t1 = this.meshes.get(0).faces.get(faceCoord);
                    Face t2 = this.meshes.get(0).faces.get(faceCoord+1);
                    List<Vector> t1Coords = new ArrayList<>();
                    for (Vertex v : t1.vertices) {
                        Vector currVec = this.meshes.get(0).getVertices().get(v.getAttribute(Vertex.POSITION)).append(1);
                        Vector trans = modelToWorld.matMul(currVec).getColumn(0).removeDimensionFromVec(3);
                        t1Coords.add(trans);
                    }
                    List<Vector> t2Coords = new ArrayList<>();
                    for (Vertex v : t2.vertices) {
                        Vector currVec = this.meshes.get(0).getVertices().get(v.getAttribute(Vertex.POSITION)).append(1);
                        Vector trans = modelToWorld.matMul(currVec).getColumn(0).removeDimensionFromVec(3);
                        t2Coords.add(trans);
                    }

                    Vector v1 = t1Coords.get(1);
                    Vector v2 = t2Coords.get(0);
                    float m = (v2.get(2) - v1.get(2)) / (v2.get(0) - v1.get(0));
                    float diagZ = v1.get(2) + m*(orig.get(0) - v1.get(0));

                    if(orig.get(2) < diagZ) {
                       selectedTrigCoords = t1Coords;
                    }
                    else {
                        selectedTrigCoords = t2Coords;
                    }

//                    selected = this.mesh.faces.get(faceCoord);
//
//                    selectedTrigCoords = new ArrayList<>();
//                    for (Vertex v : selected.vertices) {
//                        Vector currVec = this.mesh.getVertices().get(v.getAttribute(Vertex.POSITION)).addDimensionToVec(1);
//                        Vector trans = modelToWorld.matMul(currVec).getColumn(0).removeDimensionFromVec(3);
//                        selectedTrigCoords.add(trans);
//                    }

                    avg = Vector.getAverage(selectedTrigCoords);
                    Vector p = new Vector(orig);
                    p.setDataElement(1,0);
//					System.out.print("avg: ");avg.display();
//					System.out.print("p: ");p.display();
                    if(TerrainUtils.areVectors2DApproximatelyEqual(avg,p)) {
                        lastKnownTrig = selectedTrigCoords;
                        return lastKnownTrig;
                    }
                }

                count++;

                if(count >= searchMaxLimit)  {
                    //System.out.println("returned null atr count nulkl");
                    if(lastKnownTrig == null) {
                        Vector temp = new Vector(5,0);
                        lastKnownTrig = new ArrayList<>();
                        lastKnownTrig.add(temp);
                        lastKnownTrig.add(temp);
                        lastKnownTrig.add(temp);

                    }
                    return lastKnownTrig;
                }

                float deltaX = 0, deltaZ = 0;

                if(avg != null) {
                    Vector diff = orig.sub(avg);
                    deltaX = diff.get(0) * 0.5f;
                    deltaZ = diff.get(2) * 0.5f;
                    //System.out.println("deltaZ: "+deltaZ);

                    xCoord = ((reqPos.get(0)) / this.scale.get(0) * width) + deltaX;
                    zCoord = ((reqPos.get(2) - deltaZ) / this.scale.get(2) * height);
                }

            }while(!isFound);

            return selectedTrigCoords;
    }
}
