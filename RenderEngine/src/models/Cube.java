package models;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Vector;
import rendering.RenderingEngine;

public class Cube {

	private Matrix vertexMatrix;
	private Matrix scaledVertexMatrix;
	private Matrix dataMatrix;
	private double scale;

	public Cube(float scale) {
		init(scale);
	}

	public Cube() {
		init(1.0f);
	}
	
	public void init(float scale) {
		
		this.scale = scale;
		
		Vector[] vertexO = new Vector[8];
		vertexO[0] = new Vector(new float[] { 0, 0, 0 });
		vertexO[1] = new Vector(new float[] { 1, 0, 0 });
		vertexO[2] = new Vector(new float[] { 0, 1, 0 });
		vertexO[3] = new Vector(new float[] { 1, 1, 0 });
		vertexO[4] = new Vector(new float[] { 0, 0, 1 });
		vertexO[5] = new Vector(new float[] { 1, 0, 1 });
		vertexO[6] = new Vector(new float[] { 0, 1, 1 });
		vertexO[7] = new Vector(new float[] { 1, 1, 1 });

		try {
			vertexMatrix = new Matrix(vertexO);
			scaledVertexMatrix = vertexMatrix.scalarMul(scale);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		List<List<Vector>> temp = new ArrayList<List<Vector>>();
		Vector[] vertex = scaledVertexMatrix.convertToVectorArray();

		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[0], vertex[1]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[1], vertex[3]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[3], vertex[2]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[0], vertex[2]));

		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[0], vertex[4]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[2], vertex[6]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[4], vertex[6]));

		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[1], vertex[5]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[3], vertex[7]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[5], vertex[7]));

		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[6], vertex[7]));
		temp.add(RenderingEngine.lineVectorListFromVertices(vertex[4], vertex[5]));

		List<Vector> temp2 = new ArrayList<Vector>();

		for (List<Vector> vList : temp) {
			for (Vector v : vList) {
				temp2.add(v);
			}
		}

		try {
			dataMatrix = new Matrix(temp2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Matrix getScaledVertexMatrix() {
		return scaledVertexMatrix;
	}

	public Matrix getDataMatrix() {
		return dataMatrix;
	}

	public Matrix getVertexMatrix() {
		return vertexMatrix;
	}

	public void scale(float scale) {
		this.init(scale);
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public void render(Graphics g) {

		List<Vector> vertices = dataMatrix.convertToVectorList();

		for (Vector v : vertices) {
			g.drawOval((int) v.getData()[0], (int) v.getData()[1], 1, 1);
		}
		
//		List<Vector> vertices = scaledVertexMatrix.convertToVectorList();
//
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(0), vertices.get(1)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(1), vertices.get(3)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(3), vertices.get(2)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(0), vertices.get(2)));
//		
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(0), vertices.get(4)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(2), vertices.get(6)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(4), vertices.get(6)));
//		
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(1), vertices.get(5)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(3), vertices.get(7)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(5), vertices.get(7)));
//		
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(6), vertices.get(7)));
//		RenderingUtils.renderVectors(g, RenderingUtils.lineVectorListFromVertices(vertices.get(4), vertices.get(5)));


	}

}
