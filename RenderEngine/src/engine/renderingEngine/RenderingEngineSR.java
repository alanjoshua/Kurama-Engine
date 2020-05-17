package engine.renderingEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import engine.Math.Matrix;
import engine.utils.Utils;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Vertex;
import engine.model.Model;
import engine.model.MeshBuilder;
import engine.camera.Camera;

public class RenderingEngineSR extends RenderingEngine {

    private static float viewingTolerance = 1.5f;
    public float[][] depthBuffer;
    public Color[][] frameBuffer;

    public RenderingEngineSR(Game game) {
        super(game);
    }

    @Override
    public void init() {
        resetBuffers();
    }

    @Override
    public void cleanUp() {

    }

    public void resetBuffers() {
        depthBuffer = new float[game.getCamera().getImageHeight()][game.getCamera().getImageWidth()];
        frameBuffer = new Color[game.getCamera().getImageHeight()][game.getCamera().getImageWidth()];
    }

//    Renders only the outlines of polygons
    public void render(List<Model> models, Graphics2D g, Camera cam) {

        Matrix worldToCam = cam.getWorldToCam();
        Quaternion camInverseQuat = cam.getOrientation().getInverse();

        for (Model m : models) {

            List<Vector> projectedVectors = null;
            Matrix camSpace = null;

//			Transform and convert 3D points to camera space according to rendering mode

            if (renderPipeline == RenderPipeline.Quat) {
                Matrix transformedV;

                if (m.isChanged()) { // Optimization to not calculate world coords repeatedly if model has not changed its position,rotation or scaling. This takes up more memory though
                    Vector[] temp = (m.getOrientation()
                            .rotatePoints((new Matrix(m.mesh.getVertices()).columnMul(m.getScale().addDimensionToVec(1)))));

                    for (int i = 0; i < temp.length; i++) {
                        temp[i] = temp[i].add(m.getPos());
                    }

                    m.setTransformedVertices(new Matrix(Vector.addDimensionToVec(temp, 1)));
                    m.setChanged(false);

                }
                transformedV = m.getTranformedVertices();

                Vector[] camSpaceV = camInverseQuat.rotatePoints(transformedV);
                Vector pos_ = camInverseQuat.rotatePoint(cam.getPos());

                for (int i = 0; i < camSpaceV.length; i++) {
                    camSpaceV[i] = camSpaceV[i].sub(pos_);
                }
                camSpace = new Matrix(Vector.addDimensionToVec(camSpaceV, 1));
            } else if (renderPipeline == RenderPipeline.Matrix) {
                Matrix transformedV = null;
                if (m.isChanged()) { // Optimization to not calculate world coords repeatedly if model has not changed its position,rotation or scaling. This takes up more memory though
                    transformedV = (m.getObjectToWorldMatrix().matMul(m.getMesh().getVertices()));
                    m.setChanged(false);
                    m.setTransformedVertices(transformedV);
                } else {
                    transformedV = m.getTranformedVertices();
                }

                camSpace = worldToCam.matMul(transformedV);

            }

//			Project model to the screen according to projection mode
            if (projectionMode == ProjectionMode.PERSPECTIVE) {
                projectedVectors = (cam.getPerspectiveProjectionMatrix().matMul(camSpace)).convertToColumnVectorList();
            } else if (projectionMode == ProjectionMode.ORTHO) {
                projectedVectors = (cam.getOrthographicProjectionMatrix().matMul(camSpace)).convertToColumnVectorList();
            }

//			initialise other variables
            List<Boolean> isVisible = new ArrayList<>(projectedVectors.size());
//			if(isVisible.length < projectedVectors.size()) {
//				isVisible = new boolean[projectedVectors.size()];
////				isVisible = new ArrayList<>(projectedVectors.size());
//			}

//			Normalise projected Vectors, rasterise them, calculate whether each point is visible or not
            for (int i = 0; i < projectedVectors.size(); i++) {

                Vector v = projectedVectors.get(i);
                float x = v.get(0);
                float y = v.get(1);
                float z = v.get(2);
                float w = v.get(3);
                float[] temp = new float[]{x / w, y / w, z / w};
//				Vector temp = new Vector(new float[] { x / w, y / w, z / w});

                projectedVectors.set(i, new Vector(new float[]{(int) ((temp[0] + 1) * 0.5 * cam.getImageWidth()),
                        (int) ((1 - (temp[1] + 1) * 0.5) * cam.getImageHeight()), temp[2]}));

                if ((-viewingTolerance <= temp[0] && temp[0] <= viewingTolerance)
                        && (-viewingTolerance <= temp[1] && temp[1] <= viewingTolerance)
                        && (0 <= temp[2] && temp[2] <= 1)
                ) {
                    isVisible.add(true);
//					isVisible[i] = true;
                } else {
                    isVisible.add(false);
//					isVisible[i] = false;
                }
            }

//			Render model using new Mesh model
            for (Face f : m.mesh.faces) {
                for (int i = 0; i < f.size(); i++) {
                    if (i != f.size() - 1) {
//						if (isVisible[f.get(i)] && isVisible[f.get(i+1)]) {
//							drawLine(g, projectedVectors.get(f.get(i)), projectedVectors.get(f.get(i+1)));
//						}
                        if (isVisible.get(f.get(i)) && isVisible.get(f.get(i + 1))) {
                            drawLine(g, projectedVectors.get(f.get(i)), projectedVectors.get(f.get(i + 1)));
                        }
                    } else {
//						if (isVisible[f.get(i)] && isVisible[f.get(0)]) {
//							drawLine(g, projectedVectors.get(f.get(i)), projectedVectors.get(f.get(0)));
//						}
                        if (isVisible.get(f.get(i)) && isVisible.get(f.get(0))) {
                            drawLine(g, projectedVectors.get(f.get(i)), projectedVectors.get(f.get(0)));
                        }
                    }
                }
            }

        }
    }

//    Render method which tries to shade triangles. Really stupid and slow because it individually checks every single pixel inside a triangle, therefore even if
//    only one triangle is being rendered, the program would run faster when the triangle is further away from the camera

    public void render2(List<Model> models, Graphics2D g) {

        Random rand = new Random();
        List<Vector> colors;
        Matrix worldToCam = game.getCamera().getWorldToCam();
        Quaternion camInverseQuat = game.getCamera().getOrientation().getInverse();

//		reset the depth buffer
        for (int i = 0; i < depthBuffer.length; i++) {
            for (int j = 0; j < depthBuffer[i].length; j++) {
                depthBuffer[i][j] = Float.POSITIVE_INFINITY;
            }
        }

//		reset the Color buffer
        for (int i = 0; i < frameBuffer.length; i++) {
            for (int j = 0; j < frameBuffer[i].length; j++) {
                frameBuffer[i][j] = null;
            }
        }

        for (Model m : models) {
            List<Vector> projectedVectors = getRasterizedVectors(m, worldToCam, camInverseQuat);

            for (Face f : m.mesh.faces) {

//				Skips face if n < 3
                if (!(f.vertices.size() < 3)) {

                    List<Face> tempFaces = new ArrayList<>();
                    tempFaces.addAll(MeshBuilder.triangulate(f,m.mesh.getVertices(),false));

                    for (Face currFace : tempFaces) {

//						Temp code: sets a random color for every vertex
                        colors = new ArrayList<>(currFace.vertices.size());
                        for (int t = 0; t < currFace.vertices.size(); t++) {
//							float[] vals = new float[]{rand.nextFloat(),rand.nextFloat(),rand.nextFloat()};
//							float[] vals = new float[]{1f,0f,0f};
                            Color c = Color.WHITE;
                            float[] vals = new float[]{c.getRed() / 255, c.getGreen() / 255, c.getBlue() / 255};

                            colors.add(new Vector(vals));
                        }
//						float[] c1 = new float[]{1,0,0,1};
//						float[] c2 = new float[]{0,1,0,1};
//						float[] c3 = new float[]{0,0,1,1};
//						colors.add(new Vector(c1));
//						colors.add(new Vector(c2));
//						colors.add(new Vector(c3));

//						Calculate the bounding box of each polygon
						List<Vector> bounds = Utils.getBoundingBox(currFace, projectedVectors, game);
                        int xMin = (int) bounds.get(0).get(0);
                        int yMin = (int) bounds.get(0).get(1);
                        int xMax = (int) bounds.get(1).get(0);
                        int yMax = (int) bounds.get(1).get(1);
                        float area = 0;

// 						Logic to precalculate area and vertices when polygon is a triangle
 						Vector v0 = null, v1 = null, v2 = null;
 						v0 = projectedVectors.get(currFace.getVertex(0).getAttribute(Vertex.POSITION));
 						v1 = projectedVectors.get(currFace.getVertex(1).getAttribute(Vertex.POSITION));
 						v2 = projectedVectors.get(currFace.getVertex(2).getAttribute(Vertex.POSITION));
 						area = Utils.edge(v0, v1, v2);

 						for (int i = yMin; i <= yMax; i++) {
 							for (int j = xMin; j <= xMax; j++) {

//								Calculate lambda values
								Vector p = new Vector(new float[]{j + 0.5f, i + 0.5f, 0});
								float[] lambda = new float[currFace.vertices.size()];

//								Calculating lambda values for a triangle
								lambda[0] = Utils.edge(v1, v2, p) / area;
								lambda[1] = Utils.edge(v2, v0, p) / area;
								lambda[2] = Utils.edge(v0, v1, p) / area;

//								Check whether point is inside polygon
								boolean isOverlap = true;
								for (float val : lambda) {
									if (val < 0) {
										isOverlap = false;
										break;
									}
								}

								if (isOverlap) {
//									Calculate z using perspective projection corrected interpolation
									float z = 0;
									Vector finColor = new Vector(new float[]{0, 0, 0});

									for (int t = 0; t < lambda.length; t++) {
										float z_ = 1.0f / projectedVectors.get(currFace.getVertex(t).getAttribute(Vertex.POSITION)).get(2);   // z is already reciprocated in getProjectedVectors()
										z += ((z_) * lambda[t]);

										Vector color = colors.get(t);
										for (int k = 0; k < color.getNumberOfDimensions(); k++) {
											finColor.getData()[k] += (color.get(k) * z_) * lambda[t];
										}
									}

									z = 1f / z;
									for (int k = 0; k < finColor.getNumberOfDimensions(); k++) {
										finColor.getData()[k] *= (z + 255);
									}

//									Update depth buffer
									if (z < depthBuffer[i][j] && z >= 1) {
										depthBuffer[i][j] = z;
										Color c = null;
										try {
											c = new Color((int) finColor.get(0), (int) finColor.get(1), (int) finColor.get(2), 200);
										} catch (Exception e) {
											finColor.display();
										}
										frameBuffer[i][j] = c;
									}
								}

 							}
 						}
//						End of nested for loop
                    }
                }

            }
//			End of polygon loop
        }
//		End of models list loop

        //	Render to screen
        for (int i = 0; i < frameBuffer.length; i++) {
            for (int j = 0; j < frameBuffer[i].length; j++) {
                if (frameBuffer[i][j] != null) {
                    g.setColor(frameBuffer[i][j]);
                    g.drawLine(j, i, j, i);
                }
            }
        }
//		End of render loop

    }

    public List<Vector> getRasterizedVectors(Model m, Matrix worldToCam, Quaternion camInverseQuat) {

        List<Vector> projectedVectors = null;
        Matrix camSpace = null;

//		Transform and convert 3D points to camera space according to rendering mode
        if (renderPipeline == RenderPipeline.Quat) {
            Matrix transformedV;

            if (m.isChanged()) { // Optimization to not calculate world coords repeatedly if model has not changed its position,rotation or scaling. This takes up more memory though
                Vector[] temp = (m.getOrientation()
                        .rotatePoints((new Matrix(m.mesh.getVertices()).columnMul(m.getScale().addDimensionToVec(1)))));

                for (int i = 0; i < temp.length; i++) {
                    temp[i] = temp[i].add(m.getPos());
                }

                m.setTransformedVertices(new Matrix(Vector.addDimensionToVec(temp, 1)));
                m.setChanged(false);

            }
            transformedV = m.getTranformedVertices();

            Vector[] camSpaceV = camInverseQuat.rotatePoints(transformedV);
            Vector pos_ = camInverseQuat.rotatePoint(game.getCamera().getPos());

            for (int i = 0; i < camSpaceV.length; i++) {
                camSpaceV[i] = camSpaceV[i].sub(pos_);
            }
            camSpace = new Matrix(Vector.addDimensionToVec(camSpaceV, 1));
        } else if (renderPipeline == RenderPipeline.Matrix) {
            Matrix transformedV = null;
            if (m.isChanged()) { // Optimization to not calculate world coords repeatedly if model has not changed its position,rotation or scaling. This takes up more memory though
                transformedV = (m.getObjectToWorldMatrix().matMul(m.getMesh().getVertices()));
                m.setChanged(false);
                m.setTransformedVertices(transformedV);
            } else {
                transformedV = m.getTranformedVertices();
            }

            camSpace = worldToCam.matMul(transformedV);

        }

//			Project model to the screen according to projection mode
        if (projectionMode == ProjectionMode.PERSPECTIVE) {
            projectedVectors = (game.getCamera().getPerspectiveProjectionMatrix().matMul(camSpace)).convertToColumnVectorList();
        } else if (projectionMode == ProjectionMode.ORTHO) {
            projectedVectors = (game.getCamera().getOrthographicProjectionMatrix().matMul(camSpace)).convertToColumnVectorList();
        }

//			Normalise and rasterize projected Vectors
        for (int i = 0; i < projectedVectors.size(); i++) {

            Vector v = projectedVectors.get(i);
            float x = v.get(0);
            float y = v.get(1);
            float z = v.get(2);
            float w = v.get(3);
            float[] temp = new float[]{x / w, y / w, z / w};

            projectedVectors.set(i, new Vector(new float[]{(int) ((temp[0] + 1) * 0.5 * game.getCamera().getImageWidth()),
                    (int) ((1 - (temp[1] + 1) * 0.5) * game.getCamera().getImageHeight()), -camSpace.getColumn(i).get(2)}));
        }

        return projectedVectors;
    }

    public void renderVectors(Graphics2D g, Vector[] v) {
        for (Vector vv : v) {
            g.drawOval((int) vv.getData()[0], (int) vv.getData()[1], 1, 1);
        }
    }

    public void renderVectors(Graphics2D g, List<Vector> v) {
        for (Vector vv : v) {
            g.drawOval((int) vv.getData()[0], (int) vv.getData()[1], 1, 1);
        }
    }

    public void drawLine(Graphics2D g, Vector p1, Vector p2) {
        g.drawLine((int) p1.getData()[0], (int) p1.getData()[1], (int) p2.getData()[0], (int) p2.getData()[1]);
    }

    public Vector[] lineVectorArrayFromVertixes(Vector p1, Vector p2) {

        Vector[] res = null;
        List<Vector> resList = new ArrayList<Vector>();
        resList.add(p1);

        try {

            Vector dir = Vector.sub(p2, p1);
            double len = dir.getNorm();
            dir = dir.normalise();

            for (int i = 1; i < len; i++) {
                resList.add(p1.add(dir.scalarMul(i)));
            }

            resList.add(p2);

            res = (Vector[]) resList.toArray();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public static List<Vector> lineVectorListFromVertices(Vector p1, Vector p2) {

        List<Vector> resList = new ArrayList<Vector>();
        resList.add(p1);

        try {

            Vector dir = Vector.sub(p2, p1);

//			System.out.println("Dir vector : ");
//			dir.display();

            double len = dir.getNorm();

//			System.out.println("lenght is : " + len);

            dir = dir.normalise();

//			System.out.println("normalised vector dir : ");
//			dir.display();

            for (int i = 1; i < len; i++) {
                resList.add(p1.add(dir.scalarMul(i)));
            }

            resList.add(p2);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resList;
    }

}
