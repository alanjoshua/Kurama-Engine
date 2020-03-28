package rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Utils;
import Math.Quaternion;
import Math.Vector;
import jdk.swing.interop.SwingInterOpUtils;
import main.Game;
import models.DataStructure.LinkedList.CircularDoublyLinkedList;
import models.DataStructure.LinkedList.Node;
import models.DataStructure.Mesh.Face;
import models.DataStructure.Mesh.Vertex;
import models.Model;
import models.ModelBuilder;

public class RenderingEngine {

	Game game;
	private static float viewingTolerance = 1.5f;
	public float[][] depthBuffer;
	public Color[][] frameBuffer;

	public enum ProjectionMode {
		ORTHO, PERSPECTIVE
	}

	public enum RenderPipeline {
		Matrix, Quat
	}

	private ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;
	private RenderPipeline renderPipeline = RenderPipeline.Quat;

	public RenderingEngine(Game game) {
		int black = Color.BLACK.getRGB();
		int white = Color.WHITE.getRGB();
		this.game = game;
	}

	public void resetBuffers() {
		depthBuffer = new float[game.getCamera().getImageWidth()][game.getCamera().getImageHeight()];
		frameBuffer = new Color[game.getCamera().getImageWidth()][game.getCamera().getImageHeight()];
	}

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
			}

			else if (renderPipeline == RenderPipeline.Matrix) {
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
				float[] temp = new float[] { x / w, y / w, z / w};
//				Vector temp = new Vector(new float[] { x / w, y / w, z / w});

				projectedVectors.set(i,new Vector(new float[]{(int) ((temp[0] + 1) * 0.5 * cam.getImageWidth()),
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
			for(Face f : m.mesh.faces) {
				for (int i = 0; i < f.size(); i++) {
					if (i != f.size() - 1) {
//						if (isVisible[f.get(i)] && isVisible[f.get(i+1)]) {
//							drawLine(g, projectedVectors.get(f.get(i)), projectedVectors.get(f.get(i+1)));
//						}
						if (isVisible.get(f.get(i)) && isVisible.get(f.get(i+1))) {
							drawLine(g, projectedVectors.get(f.get(i)), projectedVectors.get(f.get(i+1)));
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

	public void render2(List<Model> models, BufferedImage frameBuffer) {

		Matrix worldToCam = game.getCamera().getWorldToCam();
		Quaternion camInverseQuat = game.getCamera().getOrientation().getInverse();

		float[][] depthBuffer = new float[game.getCamera().getImageHeight()][game.getCamera().getImageWidth()];
//		Color[][] frameBuffer = new Color[game.getCamera().getImageHeight()][game.getCamera().getImageWidth()];

//		Initialize the depth buffer
		for(int i = 0;i < depthBuffer.length;i++) {
			for(int j = 0;j < depthBuffer[0].length;j++) {
				depthBuffer[i][j] = Float.POSITIVE_INFINITY;
			}
		}

		for (Model m : models) {

			List<Vector> projectedVectors = getRasterizedVectors(m,worldToCam,camInverseQuat);

			for(Face f: m.mesh.faces) {
//				System.out.println("Inside face loop");
//				Calculate the bounding box of each polygon
				List<Vector> bounds = Utils.getBoundingBox(f,projectedVectors,game);
				int xMin = (int)bounds.get(0).get(0);
				int yMin = (int)bounds.get(0).get(1);
				int xMax = (int)bounds.get(1).get(0);
				int yMax = (int)bounds.get(1).get(1);

//				System.out.println("MIN:: " + xMin + " : " + yMin);
//				System.out.println("MAX::" + xMax + " : " + yMax);

//				System.out.println("Finished calculating bounds");
				float area = 0;

//				// Logic to precalculate area and vertices when polygon is a triangle (different algorithm for n-gons)
				Vector v0 = null,v1 = null,v2 = null;
				if(f.vertices.size() == 3) {
//					System.out.println("precalculating traingle area and vertices");
					v0 = projectedVectors.get(f.getVertex(0).getAttribute(Vertex.POSITION));
					v1 = projectedVectors.get(f.getVertex(1).getAttribute(Vertex.POSITION));
					v2 = projectedVectors.get(f.getVertex(2).getAttribute(Vertex.POSITION));
					area += Utils.edge(v0,v1,v2);
				}
//				System.out.println("Finished precalculations for triangle");

				for(int i = xMin; i <= xMax;i++) {
					for(int j = yMin;j <= yMax;j++) {
//						System.out.println("inside nested inner loop");

//						Calculate lambda values
						Vector p = new Vector(new float[]{i+0.5f,j+0.5f,0});
						float[] lambda = new float[f.vertices.size()];

//						Calculating lambda values for a triangle
						if(f.vertices.size() == 3) {
//							System.out.println("Calculating lambda for triangle");
							lambda[0] = Utils.edge(v1,v2,p) / area;
							lambda[1] = Utils.edge(v2,v0,p) / area;
							lambda[2] = Utils.edge(v0,v1,p) / area;
//							System.out.println("Finished calculating lambda for triangle");
						}
						else {   //	Calculating lambda values for n-gons (algorithm from the paper "Generalized Barycentric Coordinates on Irregular Polygons")
//							System.out.println("Calculating lambda values for other n-gons");
							for(int t = 0;t < lambda.length;t++) {

								int prev = (t + lambda.length - 1) % lambda.length;
								int next = (t + 1) % lambda.length;
								Vector qt = projectedVectors.get(f.getVertex(t).getAttribute(Vertex.POSITION));
								Vector qNext = projectedVectors.get(f.getVertex(next).getAttribute(Vertex.POSITION));
								Vector qPrev = projectedVectors.get(f.getVertex(prev).getAttribute(Vertex.POSITION));

								lambda[t] = (float) (Utils.cotangent(p,qt,qPrev) + Utils.cotangent(p,qt,qNext) / Math.pow(p.sub(qt).getNorm(),2));
								area += lambda[t];
							}
//							System.out.println("Finished calculating lambda values for n-gons ");

							// Normalize lambda values
							for(int t = 0;t < lambda.length;t++) {
								lambda[t] /= area;
							}
//							System.out.println("Finished normalizing lambda values for n-gons");

						}

//						Check whether point is inside polygon
						boolean isOverlap = true;
						for(float val: lambda) {
							if(val < 0 || val > 1) {
								isOverlap = false;
								break;
							}
						}

						if(isOverlap) {
//							System.out.println("Calculating z value");
//							Calculate z using perspective projection corrected interpolation
							float z = 0;
							for (int t = 0; t < lambda.length; t++) {
								float z_ = projectedVectors.get(f.getVertex(t).getAttribute(Vertex.POSITION)).get(2);
								z += ((1.0f / -z_) * lambda[t]);
							}
							z = 1f / z;
//						System.out.println("Finished calculating z value");

//							Update depth buffer
//							System.out.println(z);
							if (z < depthBuffer[j][i] && z >=1) {
								depthBuffer[j][i] = z;
//								int ind = (j * game.getCamera().getImageWidth()) + i;
								frameBuffer.setRGB(i,j,Color.LIGHT_GRAY.getRGB());
//								pixels[ind] = Color.LIGHT_GRAY.getRGB();
							}
//						System.out.println("Updated depth buffer");
						}
					}
				}
//				End of nested for loop

			}
//			End of polygon loop
//			System.out.println("Finished looping through all faces for given model");
		}
//		End of models list loop
//		System.out.println("Finished looping through all models");

		//	Render to screen
//		System.out.println("starting render");
//		for(int i = 0;i < frameBuffer.length;i++) {
//			for(int j = 0;j < frameBuffer[i].length;j++) {
//				if(frameBuffer[i][j] != null) {
//					g.setColor(frameBuffer[i][j]);
//					g.drawLine(i, j, i, j);
//				}
//			}
//		}
//		System.out.println("Finished render loop");
	}

	public List<Vector> getRasterizedVectors(Model m, Matrix worldToCam, Quaternion camInverseQuat) {

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
					(int) ((1 - (temp[1] + 1) * 0.5) * game.getCamera().getImageHeight()),camSpace.getColumn(i).get(2)}));
		}

		return projectedVectors;
	}

	public ProjectionMode getProjectionMode() {
		return projectionMode;
	}

	public void setProjectionMode(ProjectionMode projectionMode) {
		this.projectionMode = projectionMode;
	}

	public RenderPipeline getRenderPipeline() {
		return renderPipeline;
	}

	public void setRenderPipeline(RenderPipeline renderPipeline) {
		this.renderPipeline = renderPipeline;
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
