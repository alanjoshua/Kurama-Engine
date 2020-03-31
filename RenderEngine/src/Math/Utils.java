package Math;

import main.Game;
import main.GameLWJGL;
import models.DataStructure.LinkedList.DoublyLinkedList;
import models.DataStructure.Mesh.Face;
import models.DataStructure.Mesh.Vertex;

import java.util.ArrayList;
import java.util.List;

public class Utils {
	
	public static Vector projectPointToPlane(Vector e1, Vector e2, Vector p) {
		Vector proj1 = e1.normalise().scalarMul(e1.normalise().dot(p));
		Vector proj2 = e2.normalise().scalarMul(e2.normalise().dot(p));
		return proj1.add(proj2);
	}
	
	public static float edge(Vector v1, Vector v2, Vector p) {
		return (p.get(0) - v1.get(0)) * (v2.get(1) - v1.get(1)) - (p.get(1) - v1.get(1)) * (v2.get(0) - v1.get(0));
	}

	public static boolean isEar(Vertex v0, Vertex v1, Vertex v2, DoublyLinkedList<Vertex> reflex, List<Vector> vertList) {
		boolean isEar = true;
		reflex.resetLoc();
		for(int j = 0;j < reflex.getSize();j++) {
			Vertex p = reflex.peekNext();
			if(isPointInsideTriangle(v0,v1,v2,p,vertList)) {
				isEar = false;
				break;
			}
		}
		return isEar;
	}

	public static boolean isVertexConvex(Vertex v0, Vertex v1, Vertex v2, List<Vector> vertList) {
		Vector vert0 = vertList.get(v0.getAttribute(Vertex.POSITION));
		Vector vert1 = vertList.get(v1.getAttribute(Vertex.POSITION));
		Vector vert2 = vertList.get(v2.getAttribute(Vertex.POSITION));

		float angle = (vert0.sub(vert1)).getAngleBetweenVectors(vert2.sub(vert1));
		if (angle < 0) {
			angle = 180 - angle;
		}

		return angle <= 180;
	}

	public static boolean isPointInsideTriangle(Vertex v00, Vertex v11, Vertex v22, Vertex pp,List<Vector> vertices) {
		Vector v0 = vertices.get(v00.getAttribute(Vertex.POSITION));
		Vector v1 = vertices.get(v11.getAttribute(Vertex.POSITION));
		Vector v2 = vertices.get(v22.getAttribute(Vertex.POSITION));
		Vector p = vertices.get(pp.getAttribute(Vertex.POSITION));

		Vector e1 = v0.sub(v1);
		Vector e2 = v2.sub(v1);

		Vector proj1 = e1.normalise().scalarMul(e1.normalise().dot(p));
		Vector proj2 = e2.normalise().scalarMul(e2.normalise().dot(p));
		Vector p_ = proj1.add(proj2);

		float pa = (v0.sub(p_)).getNorm();
		float pb = (v1.sub(p_)).getNorm();
		float pc = (v2.sub(p_)).getNorm();
		float totalArea = e1.getNorm() * e2.getNorm() / 2.0f;

		float alpha = pa * pb / (2.0f * totalArea);
		float beta = pb * pc / (2.0f * totalArea);
		float gamma = 1 - alpha - beta;

		return alpha >= 0 && alpha <= 1 && beta >= 0 && beta <= 1 && alpha + beta + gamma == 1;

	}
	
	public static boolean isPointInsideTriangle(Vector v1 ,Vector v2, Vector v3, Vector p, boolean shouldProject) {
		boolean res = false;
		Vector point = p;
	
		if(shouldProject) {
			point = projectPointToPlane(v2.sub(v1),v2.sub(v3),p);
		}
		
		float w1 = edge(v2, v3, point);
		float w2 = edge(v1, v3, point);
		float w3 = edge(v1, v2, point);

		if(w1 >= 0 && w2 >= 0 && w3 >= 0) {
			res = true;
		}
		
		return res;
	}

//	from the paper "Generalized Barycentric Coordinates on Irregular Polygons"
	public static float cotangent(Vector a, Vector b, Vector c) {
		Vector ba = a.sub(b);
		Vector bc = c.sub(b);
		return (bc.dot(ba) / (bc.cross(ba)).getNorm());
	}
	
	public static List<Vector> getBoundingBox(Face f, List<Vector> projectedVectors, Game game) {

		Vector bbMax = new Vector(new float[]{Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});
		Vector bbMin = new Vector(new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY});

		for(Vertex v: f.vertices) {
			Vector curr = projectedVectors.get(v.getAttribute(Vertex.POSITION));
			if(curr.get(2) > 0) {
				if (curr.get(0) < bbMin.get(0)) {
					bbMin.setDataElement(0, curr.get(0));
				}
				if (curr.get(1) < bbMin.get(1)) {
					bbMin.setDataElement(1, curr.get(1));
				}
				if (curr.get(0) > bbMax.get(0)) {
					bbMax.setDataElement(0, curr.get(0));
				}
				if (curr.get(1) > bbMax.get(1)) {
					bbMax.setDataElement(1, curr.get(1));
				}
			}
		}

		int xMin = (int) Math.max(0, Math.min(game.getCamera().getImageWidth() - 1, Math.floor(bbMin.get(0))));
		int yMin = (int) Math.max(0, Math.min(game.getCamera().getImageHeight() - 1, Math.floor(bbMin.get(1))));
		int xMax = (int) Math.max(0, Math.min(game.getCamera().getImageWidth() - 1, Math.floor(bbMax.get(0))));
		int yMax = (int) Math.max(0, Math.min(game.getCamera().getImageHeight() - 1, Math.floor(bbMax.get(1))));

		Vector min = new Vector(new float[]{xMin,yMin});
		Vector max = new Vector(new float[]{xMax,yMax});
		List<Vector> res = new ArrayList<>();
		res.add(min);
		res.add(max);

		return res;

	}

	public static List<Vector> getBoundingBox(Face f, List<Vector> projectedVectors, GameLWJGL game) {

		Vector bbMax = new Vector(new float[]{Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});
		Vector bbMin = new Vector(new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY});

		for(Vertex v: f.vertices) {
			Vector curr = projectedVectors.get(v.getAttribute(Vertex.POSITION));
			if(curr.get(2) > 0) {
				if (curr.get(0) < bbMin.get(0)) {
					bbMin.setDataElement(0, curr.get(0));
				}
				if (curr.get(1) < bbMin.get(1)) {
					bbMin.setDataElement(1, curr.get(1));
				}
				if (curr.get(0) > bbMax.get(0)) {
					bbMax.setDataElement(0, curr.get(0));
				}
				if (curr.get(1) > bbMax.get(1)) {
					bbMax.setDataElement(1, curr.get(1));
				}
			}
		}

		int xMin = (int) Math.max(0, Math.min(game.getCamera().getImageWidth() - 1, Math.floor(bbMin.get(0))));
		int yMin = (int) Math.max(0, Math.min(game.getCamera().getImageHeight() - 1, Math.floor(bbMin.get(1))));
		int xMax = (int) Math.max(0, Math.min(game.getCamera().getImageWidth() - 1, Math.floor(bbMax.get(0))));
		int yMax = (int) Math.max(0, Math.min(game.getCamera().getImageHeight() - 1, Math.floor(bbMax.get(1))));

		Vector min = new Vector(new float[]{xMin,yMin});
		Vector max = new Vector(new float[]{xMax,yMax});
		List<Vector> res = new ArrayList<>();
		res.add(min);
		res.add(max);

		return res;

	}
	
}
