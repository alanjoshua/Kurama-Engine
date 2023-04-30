package Kurama.Math;

import java.util.ArrayList;
import java.util.List;

public class Quaternion {

	private Vector coordinate;

	public Quaternion(Vector v) {
		try {
			if (v.getNumberOfDimensions() == 4) {
				this.coordinate = v;
			} else if (v.getNumberOfDimensions() == 3) {
				float[] dat = new float[4];
				dat[0] = 0f;
				dat[1] = v.get(0);
				dat[2] = v.get(1);
				dat[3] = v.get(2);
				this.coordinate = new Vector(dat);
			} else
				throw new Exception(
						"Input Vector should only have 3 or 4 elements, 3 for a pure vector and 4 for a full quaternion");
		} catch (Exception e) {
		}
	}

	// Algorithm from https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
	public Quaternion(Matrix rot) {
		float tr = rot.get(0,0) + rot.get(1,1) + rot.get(2,2);
		float qw, qx, qy, qz;

		if(tr > 0) {
			float S = (float) (Math.sqrt(tr + 1) * 2);
			qw = 0.25f * S;
			qx = (rot.get(2,1) - rot.get(1,2)) / S;
			qy = (rot.get(0,2) - rot.get(2,0)) / S;
			qz = (rot.get(1,0) - rot.get(0,1)) / S;
		}
		else if((rot.get(0,0) > rot.get(1,1)) && (rot.get(0,0) > rot.get(2,2))) {
			float S = (float) (Math.sqrt(1 + rot.get(0,0) - rot.get(1,1) - rot.get(2,2)) * 2);
			qw = rot.get(2,1) - rot.get(1,2);
			qx = 0.25f * S;
			qy = (rot.get(0,1) + rot.get(1,0)) / S;
			qz = (rot.get(0,2) + rot.get(2,0)) / S;
		}
		else if(rot.get(1,1) > rot.get(2,2)) {
			float S = (float) (Math.sqrt(1 + rot.get(1,1) - rot.get(0,0) - rot.get(2,2)) * 2);
			qw = rot.get(0,2) - rot.get(2,0);
			qx = (rot.get(0,1) + rot.get(1,0)) / S;
			qy = 0.25f * S;
			qz = (rot.get(1,2) + rot.get(2,1)) / S;
		}
		else {
			float S = (float) (Math.sqrt(1 + rot.get(2,2) - rot.get(0,0) - rot.get(1,1)) * 2);
			qw = rot.get(1,0) - rot.get(0,1);
			qx = (rot.get(0,2) + rot.get(2,0)) / S;
			qy = (rot.get(1,2) + rot.get(2,1)) / S;
			qz = 0.25f * S;
		}

		this.coordinate = new Vector(qw, qx, qy, qz);

	}

	public Quaternion(Quaternion copy) {
		coordinate = new Vector(copy.coordinate);
	}

    public Quaternion(float w, float x, float y, float z) {
		this.coordinate = new Vector(w, x, y, z);
    }

    public void normalise() {
		this.coordinate = this.coordinate.normalise();
	}

	public float getNorm() {
		return this.coordinate.getNorm();
	}

	public Quaternion getInverse() {
		return new Quaternion(getConjugate().getCoordinate().normalise());
	}
	
	public static Quaternion getAxisAsQuat(Vector v, float angle) {
		
		float a = (float) Math.toRadians(angle / 2f);
		float sin = (float)Math.sin(a);
		float[] data = new float[] { (float) Math.cos(a), sin * v.get(0), sin * v.get(1), sin * v.get(2) };
		return new Quaternion(new Vector(data));
	}

	public static Quaternion getAxisAsQuat(float x, float y, float z, float angle) {
		Vector v = new Vector(x, y, z);
		float a = (float) Math.toRadians(angle / 2f);
		float sin = (float)Math.sin(a);
		float[] data = new float[] { (float) Math.cos(a), sin * v.get(0), sin * v.get(1), sin * v.get(2) };
		return new Quaternion(new Vector(data));
	}


//	https://en.wikipedia.org/wiki/Slerp#:~:text=Writing%20a%20unit%20quaternion%20q,%CE%A9%20%2B%20v%20sin%20t%20%CE%A9.&text=Here%20are%20four%20equivalent%20quaternion%20expressions%20for%20Slerp.
	public static Quaternion slerp(Quaternion q1, Quaternion q2, float t) {
		q1.normalise();
		q2.normalise();

		double dot = q1.coordinate.dot(q2.coordinate);
		if(dot < 0f) {
			q1.coordinate = q1.coordinate.scalarMul(-1);
			dot = -dot;
		}

		double DOT_THRESHOLD = 0.9995;
		if (dot > DOT_THRESHOLD) {
			// If the inputs are too close for comfort, linearly interpolate
			// and normalize the result.

			var result = new Quaternion(q1.coordinate.add((q2.coordinate.sub(q1.coordinate)).scalarMul(t)));
			result.normalise();
			return result;
		}

		// Since dot is in range [0, DOT_THRESHOLD], acos is safe
		double theta_0 = Math.acos(dot);        // theta_0 = angle between input vectors
		double theta = theta_0*t;          // theta = angle between v0 and result
		double sin_theta = Math.sin(theta);     // compute this value only once
		double sin_theta_0 = Math.sin(theta_0); // compute this value only once

		double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;  // == sin(theta_0 - theta) / sin(theta_0)
		double s1 = sin_theta / sin_theta_0;

		return new Quaternion((q1.coordinate.scalarMul((float)s0)).add(q2.coordinate.scalarMul((float) s1)));

	}

	public static Quaternion getQuaternionFromEuler(float pitch, float yaw, float roll) {
		Quaternion p = Quaternion.getAxisAsQuat(new Vector(new float[]{1,0,0}),pitch);
		Quaternion y = Quaternion.getAxisAsQuat(new Vector(new float[]{0,1,0}),yaw);
		Quaternion r = Quaternion.getAxisAsQuat(new Vector(new float[]{0,0,1}),roll);
		return r.multiply(y.multiply(p));
	}

	public Quaternion multiply(Quaternion r) {

		Vector v = this.getPureVec();
		Vector w = r.getPureVec();
		float s = this.getCoordinate().get(0);
		float t = r.getCoordinate().get(0);

		float w_ = (s * t) - v.dot(w);
		Vector v_ = (w.scalarMul(s).add(v.scalarMul(t))).add(v.cross(w));

		float[] res = new float[] { w_, v_.get(0), v_.get(1), v_.get(2) };

		return new Quaternion(new Vector(res));

	}

	public Vector getPureVec() {
		return new Vector(new float[] { coordinate.get(1), coordinate.get(2),
				coordinate.get(3) });
	}


//	Calculate w component for quaternion
	public static Quaternion calculateWFromXYZ(Vector orient) {
		float t = 1f - (orient.get(0) * orient.get(0)) - (orient.get(1) * orient.get(1)) - (orient.get(2) * orient.get(2));
		float w;
		if (t < 0f) {
			w = 0f;
		} else {
			w = (float) -Math.sqrt(t);
		}

		var orient_quat = new Quaternion(new Vector(w, orient.get(0), orient.get(1), orient.get(2)));
		orient_quat.normalise();
		return orient_quat;
	}

	public Matrix getRotationMatrix() {

		float[] q = this.getCoordinate().getData();

		float[][] data = new float[][] {
				{ 1 - 2 * (q[2] * q[2] + q[3] * q[3]), 2 * (q[1] * q[2] - q[3] * q[0]),
						2 * (q[1] * q[3] + q[2] * q[0]) },
				{ 2 * (q[1] * q[2] + q[3] * q[0]), 1 - 2 * (q[1] * q[1] + q[3] * q[3]),
						2 * (q[2] * q[3] - q[1] * q[0]) },
				{ 2 * (q[1] * q[3] - q[2] * q[0]), 2 * (q[2] * q[3] + q[1] * q[0]),
						1 - 2 * (q[1] * q[1] + q[2] * q[2]) } };

		return new Matrix(data);
	}

	public Vector getAxisOfRotation() {
		float angle = getAngleOfRotation();

		Vector axis;

		if (angle != 0) {

			float temp = (float) Math.sqrt(1 - (coordinate.get(0) * coordinate.get(0)));

			float[] axisData = new float[] {coordinate.get(1) / temp,
					coordinate.get(2) / temp, coordinate.get(3) / temp};

			axis = new Vector(axisData).normalise();

		} else {
			float[] axisData = new float[] { 0, 0, 1 };
			axis = new Vector(axisData);
		}
		return axis;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) {return false;}

		Quaternion v = (Quaternion)o;
		if(this.coordinate.getNumberOfDimensions() != v.coordinate.getNumberOfDimensions()) return false;

		for(int i = 0; i < this.coordinate.getNumberOfDimensions(); i++) {
			if(v.coordinate.get(i) != this.coordinate.get(i)) {
				return false;
			}
		}
		return true;
	}

	public Vector rotatePoint(Vector p) {
		var p_ = new Quaternion(new Vector(0, p.get(0), p.get(1), p.get(2)));
		var res = this.multiply(p_).multiply(this.getInverse());
		return res.getPureVec();
	}

//	public Vector rotatePoint(Vector p) {
////		New Method
//		Vector w = this.getPureVec();
//		float w2 = (float)Math.pow(w.getNorm(),2);
//
//		return p.scalarMul((float)Math.pow(this.coordinate.get(0),2) - w2).add(w.scalarMul(2).scalarMul(p.dot(w))).add((w.cross(p)).scalarMul(2f * coordinate.get(0)));
//
////		Old method
////		Quaternion q_ = getInverse();
////		Quaternion res = (new Quaternion(new Vector(new float[] {0,v.get(0),v.get(1),v.get(2)}))).multiply(q_);
////		res = this.multiply(res);
////		return res.getPureVec();
//	}

	public Vector[] rotatePoints(Vector[] vList) {

//		Quaternion q_ = getInverse();
		Vector[] res = new Vector[vList.length];
//		Vector tempV = null;

		Vector w = this.getPureVec();
		float w2 = (float)Math.pow(w.getNorm(),2);
		
		int i = 0;
		for (Vector v : vList) {

//			Old method
//			Quaternion temp = (new Quaternion(new Vector(new float[] {0,v.get(0),v.get(1),v.get(2)}))).multiply(q_);
//			temp = this.multiply(temp);
//			res[i] = temp.getPureVec();
//			i++;

//			New method
			Vector p = new Vector(new float[]{v.get(0),v.get(1),v.get(2)});
			res[i] = p.scalarMul((float)Math.pow(this.coordinate.get(0),2) - w2).add(w.scalarMul(2).scalarMul(p.dot(w))).add((w.cross(p)).scalarMul(2f * coordinate.get(0)));
			i++;
		}

		return res;
	}

	public List<Vector> rotatePoints(List<Vector> vList) {

//		Quaternion q_ = getInverse();
		List<Vector> res = new ArrayList<>(vList.size());
//		Vector tempV = null;

		Vector w = this.getPureVec();
		float w2 = (float)Math.pow(w.getNorm(),2);

		for (Vector v : vList) {

//			Old method
//			Quaternion temp = (new Quaternion(new Vector(new float[] {0,v.get(0),v.get(1),v.get(2)}))).multiply(q_);
//			temp = this.multiply(temp);
//			res[i] = temp.getPureVec();
//			i++;

//			New method
			Vector p = new Vector(new float[]{v.get(0),v.get(1),v.get(2)});
			res.add(p.scalarMul((float)Math.pow(this.coordinate.get(0),2) - w2).add(w.scalarMul(2).scalarMul(p.dot(w))).add((w.cross(p)).scalarMul(2f * coordinate.get(0))));
		}

		return res;
	}

	public Vector[] rotatePoints(Matrix vList) {

//		Quaternion q_ = getInverse();
		Vector[] res = new Vector[vList.getCols()];
//		Vector tempV = null;

		Vector w = this.getPureVec();
		float w2 = (float)Math.pow(w.getNorm(),2);

		Vector v;
		for (int i = 0;i < vList.getCols();i++) {
//			Old method
//			v = vList.getColumn(i);
//			Quaternion temp = (new Quaternion(new Vector(new float[] {0,v.get(0),v.get(1),v.get(2)}))).multiply(q_);
//			temp = this.multiply(temp);
//			res[i] = temp.getPureVec();
//			i++;

//			New method
			v = vList.getColumn(i);

			Vector p = new Vector(new float[]{v.get(0),v.get(1),v.get(2)});
			res[i] = p.scalarMul((float)Math.pow(this.coordinate.get(0),2) - w2).add(w.scalarMul(2).scalarMul(p.dot(w))).add((w.cross(p)).scalarMul(2f * coordinate.get(0)));
		}

		return res;
	}

	public float getAngleOfRotation() {
		this.normalise();
		float angle = (float) (2 * Math.acos(this.coordinate.get(0)));
		angle = (float) Math.toDegrees(angle);
		return angle;
	}

	public Quaternion getConjugate() {
		float[] res = new float[] { coordinate.get(0), -1 * coordinate.get(1),
				-1 * coordinate.get(2), -1 * coordinate.get(3) };
		return new Quaternion(new Vector(res));
	}

	public Vector getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Vector v) {
		try {
			if (v.getNumberOfDimensions() == 4) {
				this.coordinate = v;
			} else if (v.getNumberOfDimensions() == 3) {
				float[] dat = new float[4];
				dat[0] = 0f;
				dat[1] = v.get(0);
				dat[2] = v.get(1);
				dat[3] = v.get(2);
				this.coordinate = new Vector(dat);
			} else
				throw new Exception(
						"Input Vector should only have 3 or 4 elements, 3 for a pure vector and 4 for a full quaternion");
		} catch (Exception e) {
		}
	}
	
	public Vector getPitchYawRoll() {
		float x = this.getCoordinate().get(1);
		float y = this.getCoordinate().get(2);
		float z = this.getCoordinate().get(3);
		float w = this.getCoordinate().get(0);

		float yaw = (float) Math.toDegrees(Math.atan2(2 * y * w - 2 * x * z, 1 - 2 * y * y - 2 * z * z));
		float pitch = (float) Math.toDegrees(Math.atan2(2 * x * w - 2 * y * z, 1 - 2 * x * x - 2 * z * z));
		float roll = (float) Math.toDegrees(Math.asin(2 * x * y + 2 * z * w));

		return new Vector(new float[] { pitch, yaw, roll });
	}

	public String toString() {
		return coordinate.toString();
	}

}
