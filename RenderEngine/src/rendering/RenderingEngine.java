package rendering;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Utils;
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

	private RenderingMode renderingMode = RenderingMode.PERSPECTIVE;
	private RenderPipeline renderPipeline = RenderPipeline.Quat;
	
	private int black;
	private int white;

	public RenderingEngine(Game game) {
		this.game = game;
		black = Color.BLACK.getRGB();
		white = Color.WHITE.getRGB();
	}

	public void render(List<Model> models, Graphics2D g, Camera cam) {

		for (Model m : models) {

			Matrix projectedMatrix = null;
			Matrix camSpace = null;
			List<Boolean> isVisible = new ArrayList<Boolean>();
			List<Vector> projectedVectors = null;
			List<Vector> rasterVectors = new ArrayList<Vector>();
			List<Vector> camSpaceNormals = new ArrayList<Vector>();
			
			List<Vector> camSpaceList = null;

			if (renderPipeline == RenderPipeline.Quat) {
				Vector[] transformedV;

				if (m.isChanged()) {
					transformedV = new Vector[m.getVertices().length];
					transformedV = ((m.getOrientation()
							.rotatePoints((new Matrix(m.getVertices()).columnMul(m.getScale().addDimensionToVec(1)))
									.convertToVectorArray())));
					for (int i = 0; i < transformedV.length; i++) {
						transformedV[i] = transformedV[i].add(m.getPos());
					}

					m.setTransformedVertices(Vector.addDimensionToVec(transformedV, 1));
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

			else if (renderPipeline == RenderPipeline.Matrix) {
				Vector[] transformedV = null;

				if (m.isChanged()) {
					transformedV = (m.getObjectToWorldMatrix().matMul(new Matrix(m.getVertices())))
							.convertToVectorArray();
					m.setChanged(false);
					m.setTransformedVertices(transformedV);
				} else {
					transformedV = m.getTranformedVertices();
				}
				camSpace = cam.getWorldToCam().matMul(new Matrix(transformedV));
			}
			
			camSpaceList = camSpace.convertToVectorList();
			
			if (renderingMode == RenderingMode.PERSPECTIVE) {
				projectedMatrix = cam.getPerspectiveProjectionMatrix().matMul(camSpace);
			} else if (renderingMode == RenderingMode.ORTHO) {
				projectedMatrix = cam.getOrthographicProjectionMatrix().matMul(camSpace);
			}

			projectedVectors = projectedMatrix.convertToVectorList();

			for (int i = 0; i < projectedVectors.size(); i++) {
				Vector v = projectedVectors.get(i);
				float x = v.get(0);
				float y = v.get(1);
				float z = v.get(2);
				float w = v.get(3);
				Vector temp = new Vector(new float[] { x / w, y / w, z / w});
				projectedVectors.set(i, temp);

				if ((-1 <= temp.getData()[0] && temp.getData()[0] <= 1)
						&& (-1 <= temp.getData()[1] && temp.getData()[1] <= 1)
						&& (-1 <= temp.getData()[2] && temp.getData()[2] <= 1)
//						&& (temp.getData()[2] >= 0)
						) {
					isVisible.add(Boolean.TRUE);
				} else {
					isVisible.add(Boolean.FALSE);
				}
			}
			
			for(int i = 0;i < projectedVectors.size();i++) {
				Vector v = projectedVectors.get(i);
				rasterVectors.add(new Vector(new float[] { (int) ((v.get(0) + 1) * 0.5 * cam.getImageWidth()),
						(int) ((1 - (v.get(1) + 1) * 0.5) * cam.getImageHeight()), v.get(2)}));
			}
			
//			rasterVectors.get(0).display();
			
//			float[] zBuffer = new float[cam.getImageWidth() * cam.getImageHeight()];
//			int[] frameBuffer = new int[zBuffer.length];
//			
//			for (int i = 0; i < zBuffer.length; i++) {
//				zBuffer[i] = cam.getFarClippingPlane();
//				frameBuffer[i] = black;
//			}
//			
//			for (int index = 0;index < m.getFaces().size();index++) {
//				
//				int[] f = m.getFaces().get(index);
//				
////				Get The Vertices and texture coords of polygon face
//
//				Vector[] verts = new Vector[f.length];
//				for (int i = 0; i < verts.length; i++) {
//					verts[i] = rasterVectors.get(f[i]);
//				}
//
////				int[] texFace = null;
////				Vector[] texCoords = null;
////				
////				if (m.getTextureFaces() != null) {
////					texFace = m.getTextureFaces().get(index);
////					if (texFace != null) {
////
////						try {
////							if (texFace.length != f.length) {
////								throw new Exception("Texture face size and vertices size do not match");
////							}
////						} catch (Exception e) {
////						}
////						;
////
////						texCoords = new Vector[texFace.length];
////						for (int i = 0; i < texCoords.length; i++) {
////							texCoords[i] = m.getTextureCoords()[texFace[i]];
////
////							for (int j = 0; j < texCoords[i].getNumberOfDimensions(); j++) {
////								texCoords[i].getData()[j] *= verts[i].get(2);
////							}
////						}
////					}
////				}
//
//				Vector bounds = Utils.getBoundingBox(verts);
//				int minX = (int) bounds.get(0);
//				int minY = (int) bounds.get(1);
//				int maxX = (int) bounds.get(2);
//				int maxY = (int) bounds.get(3);
//
//				int x0 = Math.max(0, minX);
//				int x1 = Math.min(maxX, cam.getImageWidth());
//				int y0 = Math.max(0, minY);
//				int y1 = Math.min(maxY, cam.getImageHeight());
//				
////				assuming polygon is a triangle
//				float area = Utils.edge(verts[0],verts[1],verts[2]);
//				
////				loop over pixels of the polygon bounding box
//				
//				for (int x = x0; x < x1; x++) {
//					for (int y = y0; y < y1; y++) {
//
//						Vector samplePixel = new Vector(new float[] { x + 0.5f, y + 0.5f,0 });
//						
//						float w0 = Utils.edge(verts[1],verts[2],samplePixel);
//		                float w1 = Utils.edge(verts[2],verts[0],samplePixel);
//		                float w2 = Utils.edge(verts[0],verts[1],samplePixel);
//		                
//		                if (w0 >= 0 && w1 >= 0 && w2 >= 0) { 
//		                    w0 /= area; 
//		                    w1 /= area; 
//		                    w2 /= area; 
//		                    float oneOverZ = verts[0].get(2) * w0 + verts[1].get(2) * w1 + verts[2].get(2) * w2; 
//		                    float z = 1 / oneOverZ; 
//
//		                    if (z < zBuffer[y * cam.getImageWidth() + x] && z > cam.getNearClippingPlane()) {
//		                    	g.drawLine(x,y,x,y);
//		                        zBuffer[y * cam.getImageWidth() + x] = z; 
//		                        frameBuffer[y * cam.getImageWidth() + x] = white;
//		                    }
//		                    
//		                }
//					}
//				}
//			}
//			BufferedImage img = new BufferedImage(cam.getImageWidth(), cam.getImageHeight(),BufferedImage.TYPE_INT_RGB);
			
//			for(int x = 0; x < cam.getImageWidth();x++) {
//				for(int y = 0;y < cam.getImageHeight();y++) {
////					img.setRGB(x, y, frameBuffer[y * cam.getImageWidth() + x]);
//					g.setColor(new Color(frameBuffer[y * cam.getImageWidth() + x]));
//					g.drawLine(x,y,x,y);
//				}
//			}
//			g.drawImage(img, null, 0, 0);
			
//			System.out.println(new Color(frameBuffer[500 * 500]));

//			BufferedImage img = new BufferedImage(cam.getImageWidth(), cam.getImageHeight(),BufferedImage.TYPE_INT_ARGB);
//			int color = Color.WHITE.getRGB();
//
//			for (int[] f : m.getFaces()) {
//				for (int i = 0; i < f.length; i++) {
//					if (isVisible.get(f[i])) {
//						img.setRGB((int) rasterVectors.get(f[i]).get(0), (int) rasterVectors.get(f[i]).get(1), color);
//					}
//				}
//			}
//
//			g.drawImage(img, null, 0, 0);
			

//			for (int i = 0; i < rasterVectors.size();i++) {
//					if(isVisible.get(i)) {
//						drawLine(g, rasterVectors.get(i),rasterVectors.get(i));
//					}
//			}
			
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

	public RenderingMode getRenderingMode() {
		return renderingMode;
	}

	public void setRenderingMode(RenderingMode renderingMode) {
		this.renderingMode = renderingMode;
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
