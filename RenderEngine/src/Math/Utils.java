package Math;

public class Utils {
	
	public static float edge() {
		return 0;
	}
	
	public static Vector projectPointToPlane(Vector e1, Vector e2, Vector p) {
		Vector proj1 = e1.normalise().scalarMul(e1.normalise().dot(p));
		Vector proj2 = e2.normalise().scalarMul(e2.normalise().dot(p));
		Vector p_ = proj1.add(proj2);
		return p_;
	}
	
	public static float edge(Vector v1, Vector v2, Vector p) {
		Vector e = v2.sub(v1);
		Vector toPoint = p.sub(v1);
		return e.getDataElement(0)*toPoint.getDataElement(1) - e.getDataElement(1)*toPoint.getDataElement(0);
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
		
//		if(w1 >= 0 && w1 <= 1 && w2 >= 0 && w2 <= 1 && w3 >= 0 && w3 <= 1) {
		if(w1 >= 0 && w2 >= 0 && w3 >= 0) {
			res = true;
		}
		
		return res;
	}
	
}
