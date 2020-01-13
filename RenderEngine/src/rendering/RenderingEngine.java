package rendering;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Quaternion;
import Math.Vector;
import main.Game;
import models.Model;

public class RenderingEngine {

	private Game game;
	
	public enum RenderingMode {
		ORTHO, PERSPECTIVE
	}
	public enum RenderPipeline {
		Matrix, Quat
	}
	
	public static RenderingMode renderingMode = RenderingMode.PERSPECTIVE;
	public static RenderPipeline renderPipeline = RenderPipeline.Quat;

	public static void render(Game game, List<Model> models, Graphics2D g, Camera cam) {
		
		for (Model m : models) {
			
			Matrix projected = null;
			Matrix camSpace = null;
			
			if (renderPipeline == RenderPipeline.Quat) {
				Vector[] transformedV;
				
				if (m.isChanged()) {
					transformedV = new Vector[m.getVertices().length];
					transformedV = ((m.getOrientation().rotatePoints(
							(new Matrix(m.getVertices()).columnMul(m.getScale().addDimensionToVec(1))).convertToVectorArray())));
					for (int i = 0; i < transformedV.length; i++) {
						transformedV[i] = transformedV[i].add(m.getPos());
					}

					m.setTransformedVertices(Vector.addDimensionToVec(transformedV,1));
					m.setChanged(false);
				} else {
					transformedV = m.getTranformedVertices();
				}
				
				Vector[] camSpaceV = cam.getOrientation().getInverse().rotatePoints(transformedV);
				Vector pos_ = cam.getOrientation().getInverse().rotatePoint(cam.getPos());

				for (int i = 0; i < camSpaceV.length; i++) {
					camSpaceV[i] = camSpaceV[i].sub(pos_);
				}
				camSpace = new Matrix(Vector.addDimensionToVec(camSpaceV, 1));
			}
			
			else if(renderPipeline == RenderPipeline.Matrix) {
				Vector[] transformedV = null;
				
				if(m.isChanged()) {
					transformedV = (m.getObjectToWorldMatrix().matMul(new Matrix(m.getVertices()))).convertToVectorArray();
					m.setChanged(false);
					m.setTransformedVertices(transformedV);
				}
				else {
					transformedV = m.getTranformedVertices();
				}
				camSpace = cam.getWorldToCam().matMul(new Matrix(transformedV));
			}

			if (renderingMode == RenderingMode.PERSPECTIVE) {
				projected = cam.getPerspectiveProjectionMatrix().matMul(camSpace);
			} else if (renderingMode == RenderingMode.ORTHO) {
				projected = cam.getOrthographicProjectionMatrix().matMul(camSpace);
			}

			List<Boolean> isVisible = new ArrayList<Boolean>();
			List<Vector> normalisedVectors = new ArrayList<Vector>();
			projected.convertToVectorList().forEach((v) -> {
				float x = v.getDataElement(0);
				float y = v.getDataElement(1);
				float z = v.getDataElement(2);
				float w = v.getDataElement(3);
				Vector temp = new Vector(new float[] { x / w, y / w, z / w });
				normalisedVectors.add(temp);

				if ((-1 <= temp.getData()[0] && temp.getData()[0] <= 1)
						&& (-1 <= temp.getData()[1] && temp.getData()[1] <= 1)
						&& (-1 <= temp.getData()[2] && temp.getData()[2] <= 1)) {
					isVisible.add(new Boolean(true));
				} else {
					isVisible.add(new Boolean(false));
				}

			});

			List<Vector> rasterVectors = new ArrayList<Vector>();
			normalisedVectors.forEach((v) -> {
				rasterVectors
						.add(new Vector(new float[] { (int) ((v.getDataElement(0) + 1) * 0.5 * cam.getImageWidth()),
								(int) ((1 - (v.getDataElement(1) + 1) * 0.5) * cam.getImageHeight()) }));
			});

			for (int[] f : m.getFaces()) {
				for (int i = 0; i < f.length; i++) {

					if (i != f.length - 1) {
						if (isVisible.get(f[i]) || isVisible.get(f[i + 1])) {
							drawLine(g, rasterVectors.get(f[i]),
									rasterVectors.get(f[i + 1]));
						}
					} else {
						if (isVisible.get(f[i]) || isVisible.get(f[0])) {
							drawLine(g, rasterVectors.get(f[i]),
									rasterVectors.get(f[0]));
						}
					}
				}
			}

		}
	}

//	public static void renderGrid(Vector offset, Quaternion rotation, Camera cam, Graphics2D g) {
//		
//		List<Vector> vertices = new ArrayList<Vector>();
//		Vector totalOffset = new Vector(new float[] {cam.getPos().getDataElement(0), -cam.getPos().getDataElement(1), cam.getPos().getDataElement(2)}).add(offset);
//		
//		for(int i = 0;i < (cam.getFarClippingPlane() - cam.getNearClippingPlane());i++) {
//			Vector v = new Vector(new float[] {-cam.getLeft(),0,i}).add(totalOffset);
//			Vector u = new Vector(new float[] {cam.getRight(), 0, i}).add(totalOffset);
//			vertices.add(v);
//			vertices.add(u);
//		}
//		
//		for(int i = 0;i < cam.getCanvasWidth();i++) {
//			Vector v = new Vector(new float[] {i-cam.getLeft(),0,0}).add(totalOffset);
//			Vector u = new Vector(new float[] {i-cam.getLeft(), 0, (cam.getFarClippingPlane() - cam.getNearClippingPlane())}).add(totalOffset);
//			vertices.add(v);
//			vertices.add(u);
//		}
//		
//		Matrix camSpace = cam.getWorldToCam().matMul(Vector.addDimensionToVec(vertices, 1));
//		Matrix projected = cam.getPerspectiveProjectionMatrix().matMul(camSpace);
//		
//		List<Vector> normalisedVectors = new ArrayList<Vector>();
//		projected.convertToVectorList().forEach((v) -> {
//				float x = v.getDataElement(0);
//				float y = v.getDataElement(1);
//				float z = v.getDataElement(2);
//				float w = v.getDataElement(3);
//				Vector temp = new Vector(new float[] {
//						x/w,
//						y/w,
//						z/w
//				});
//				normalisedVectors.add(temp);
//		});
//		
//		List<Vector> rasterVectors = new ArrayList<Vector>();
//		normalisedVectors.forEach((v) -> {
//				rasterVectors.add(new Vector(new float[] { 
//						(int)((v.getDataElement(0)+1)*0.5*cam.getImageWidth()),
//						(int)((1 - (v.getDataElement(1) + 1) * 0.5)*cam.getImageHeight())
//				}));
//		});
//		
//		for(int i = 0; i < rasterVectors.size();i+=2) {
//			drawLine(g, rasterVectors.get(i), rasterVectors.get(i+1));
//		}
//		
//	}

//	public static void renderAxes(Graphics2D g, Camera cam) {
//		
//		Vector offset = new Vector(new float[] {cam.getImageWidth()/2,cam.getImageHeight()/2,0});
////		Vector[] axes = (new Matrix(cam.getData())).convertToVectorArray();
//		
////		for(int i =0; i < axes.length;i++) {
////			axes[i] = axes[i].add(offset);
////		}
//		
////		Vector tempPos = cam.getPos().add(offset);
//		Vector tempPos = offset;
//		
//		Vector[] axes = cam.getQuaternion().getRotationMatrix().convertToVectorArray();
//		
//		Matrix worldCoords = (((Vector.addDimensionToVec(axes, 0)).scalarMul(5)).AddVectorToColumns(new Vector(new float[] {0,0,cam.getNearClippingPlane(),1}))).addColumn(new Vector(new float[]{0,0,0,1}));
//		Matrix camSpace = cam.getWorldToCam().matMul(worldCoords);
//		
////		Matrix camSpace = (new Matrix(axes).scalarMul(5)).addColumn(new Vector(new float[] {0,0,cam.getNearClippingPlane()}));
////		camSpace = Vector.addDimensionToVec(camSpace.convertToVectorArray(),1);
//		Matrix projected = cam.getPerspectiveProjectionMatrix().matMul(camSpace);
//		
//		List<Vector> normalisedVectors = new ArrayList<Vector>();
//		projected.convertToVectorList().forEach((v) -> {
//				float x = v.getDataElement(0);
//				float y = v.getDataElement(1);
//				float z = v.getDataElement(2);
//				float w = v.getDataElement(3);
//				Vector temp = new Vector(new float[] {
//						x/w,
//						y/w,
//						z/w
//				});
//				normalisedVectors.add(temp);	
//		});
//		
//		List<Vector> rasterVectors = new ArrayList<Vector>();
//		normalisedVectors.forEach((v) -> {
//				rasterVectors.add(new Vector(new float[] { 
//						(int)((v.getDataElement(0)+1)*0.5*cam.getImageWidth()),
//						(int)((1 - (v.getDataElement(1) + 1) * 0.5)*cam.getImageHeight())
//				}));
//		});
//		
//		g.setColor(Color.green);
//		RenderingEngine.drawLine(g, rasterVectors.get(3), rasterVectors.get(0));
//		g.drawString("X", rasterVectors.get(0).getDataElement(0), rasterVectors.get(0).getDataElement(1));
//		
//		g.setColor(Color.blue);
//		RenderingEngine.drawLine(g, rasterVectors.get(3), rasterVectors.get(1));
//		g.drawString("Y", rasterVectors.get(1).getDataElement(0), rasterVectors.get(1).getDataElement(1));
//		
//		g.setColor(Color.red);
//		RenderingEngine.drawLine(g, rasterVectors.get(3), rasterVectors.get(2));
//		g.drawString("Z", rasterVectors.get(2).getDataElement(0), rasterVectors.get(2).getDataElement(1));
//		
//////		Vector[] axes = (camMatrix.matMul(dat)).convertToVectorArray();
////		
////		Vector offset = new Vector(new float[] {cam.getimageWidth/2,imageHeight/2,0});
////		Vector[] axes = (new Matrix(data).scalarMul(-100)).convertToVectorArray();
////		
////		for(int i =0; i < axes.length;i++) {
//////			axes[i].getData()[1] *= -1;;
////			axes[i] = axes[i].add(offset);
////		}
////		
////		Vector tempPos = pos.add(offset);
//		
//	}

	public static void renderVectors(Graphics2D g, Vector[] v) {
		for (Vector vv : v) {
			g.drawOval((int) vv.getData()[0], (int) vv.getData()[1], 1, 1);
		}
	}

	public static void renderVectors(Graphics2D g, List<Vector> v) {
		for (Vector vv : v) {
			g.drawOval((int) vv.getData()[0], (int) vv.getData()[1], 1, 1);
		}
	}

	public static void drawLine(Graphics2D g, Vector p1, Vector p2) {
		g.drawLine((int) p1.getData()[0], (int) p1.getData()[1], (int) p2.getData()[0], (int) p2.getData()[1]);
	}

	public static Vector[] lineVectorArrayFromVertixes(Vector p1, Vector p2) {

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
