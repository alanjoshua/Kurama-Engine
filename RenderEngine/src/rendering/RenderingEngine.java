package rendering;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Vector;
import main.Display;
import main.Game;
import models.Model;

public class RenderingEngine {
	
	private Game game;
	public static final int ORTHO = 1;
	public static final int PERSPECTIVE = 0;
	public static int renderingMode = PERSPECTIVE;
	
	public static void render(Game game, List<Model> models, Graphics2D g, Camera cam) {

		for (Model m : models) {
			
			Matrix transformed = m.getObjectToWorldMatrix().matMul(Vector.addDimensionToVec(m.getVertices(), 1));
			Matrix camSpace = cam.getCamInverse().matMul(transformed);
			Matrix projected = null;
			
			if(renderingMode == PERSPECTIVE) {
				projected = cam.getPerspectiveProjectionMatrix().matMul(camSpace);
			}
			else if(renderingMode == ORTHO) {
				projected = cam.getOrthographicProjectionMatrix().matMul(camSpace);
			}
			
			List<Boolean> isVisible = new ArrayList<Boolean>();
			List<Vector> normalisedVectors = new ArrayList<Vector>();
			projected.convertToVectorList().forEach((v) -> {
					float x = v.getDataElement(0);
					float y = v.getDataElement(1);
					float z = v.getDataElement(2);
					float w = v.getDataElement(3);
					Vector temp = new Vector(new float[] {
							x/w,
							y/w,
							z/w
					});
					normalisedVectors.add(temp);
					
					if ((-1 <= temp.getData()[0] && temp.getData()[0] <= 1) && (-1 <= temp.getData()[1] && temp.getData()[1] <= 1) && (-1 <= temp.getData()[2] && temp.getData()[2] <= 1)) {
						isVisible.add(new Boolean(true));					
					}
					else {
						isVisible.add(new Boolean(false));
					}
					
			});
			
			List<Vector> rasterVectors = new ArrayList<Vector>();
			normalisedVectors.forEach((v) -> {
					rasterVectors.add(new Vector(new float[] { 
							(int)((v.getDataElement(0)+1)*0.5*cam.getImageWidth()),
							(int)((1 - (v.getDataElement(1) + 1) * 0.5)*cam.getImageHeight())
					}));
			});

			for (Vector con : m.getConnections()) {
				for (int i = 0; i < con.getNumberOfDimensions(); i++) {
					if (i != con.getNumberOfDimensions() - 1) {
						if(isVisible.get((int) con.getData()[i]) || isVisible.get((int) con.getData()[i + 1])) {
//							g.setColor(Color.green);
							drawLine(g, rasterVectors.get((int) con.getData()[i]), rasterVectors.get((int) con.getData()[i + 1]));
						}
						else {
//							g.setColor(Color.red);
						}
//						drawLine(g, rasterVectors.get((int) con.getData()[i]), rasterVectors.get((int) con.getData()[i + 1]));
					} else {
						if(isVisible.get((int) con.getData()[i]) || isVisible.get((int) con.getData()[0])) {
//							g.setColor(Color.green);
							drawLine(g, rasterVectors.get((int) con.getData()[i]), rasterVectors.get((int) con.getData()[0]));
						}
						else {
//							g.setColor(Color.red);
						}
//						drawLine(g, rasterVectors.get((int) con.getData()[i]), rasterVectors.get((int) con.getData()[0]));
					}
				}
			}
			

		}
	}

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
			double len = dir.getLength();
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

			double len = dir.getLength();

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
