package Math;

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
		float[] data = new float[] { (float) Math.cos(a), (float) (Math.sin(a) * v.get(0)),
				(float) (Math.sin(a) * v.get(1)), (float) (Math.sin(a) * v.get(2)) };
		
		return new Quaternion(new Vector(data));
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

			float[] axisData = new float[] { (float) (coordinate.get(1) / temp),
					(float) (coordinate.get(2) / temp), (float) (coordinate.get(3) / temp) };

			axis = new Vector(axisData).normalise();

		} else {
			float[] axisData = new float[] { 0, 0, 1 };
			axis = new Vector(axisData);
		}
		return axis;
	}

	public Vector rotatePoint(Vector v) {
		Quaternion q_ = getInverse();		
		
		Quaternion res = (new Quaternion(new Vector(new float[] {0,v.get(0),v.get(1),v.get(2)}))).multiply(q_);
		res = this.multiply(res);
		return res.getPureVec();
	}

	public Vector[] rotatePoints(Vector[] vList) {

		Quaternion q_ = getInverse();
		Vector[] res = new Vector[vList.length];
		Vector tempV = null;
		
		int i = 0;
		for (Vector v : vList) {
			Quaternion temp = (new Quaternion(new Vector(new float[] {0,v.get(0),v.get(1),v.get(2)}))).multiply(q_);
			temp = this.multiply(temp);
			res[i] = temp.getPureVec();
			i++;
		}

		return res;
	}

	public float getAngleOfRotation() {
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

//	private Vector axis;
//	private float angle;
//	private Vector coordinate;
//
//	public Quaternion(Vector axis, float angle) {
//		try {
//			if (axis.getNumberOfDimensions() != 3) {
//				throw new IllegalArgumentException("The axis of a quaternion can only have 3 elements");
//			}
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		}
//		this.axis = axis.normalise();
//		this.setAngle(angle);
//		recalculateCoordinate();
//	}
//
//	public Quaternion(Vector v) {
//
//		try {
//			if (v.getNumberOfDimensions() == 4) {
//				this.coordinate = v;
//			} else if (v.getNumberOfDimensions() == 3) {
//				float[] dat = new float[4];
//				dat[0] = 0f;
//				dat[1] = v.getDataElement(0);
//				dat[2] = v.getDataElement(1);
//				dat[3] = v.getDataElement(2);
//				this.coordinate = new Vector(dat);
////				this.coordinate = new Vector(dat).normalise();
//			} else
//				throw new Exception("Coordinate of a quaternion should only have 3 or 4 elements ");
//		} catch (Exception e) {
//		}
//
////		reculateAxisAngle();
//	}
//
//	public Quaternion multiply(Quaternion r) {
////		float[] r = x.getCoordinate().getData();
////		float[] q = this.getCoordinate().getData();
////		float[] res = new float[4];
////
////		res[0] = r[0] * q[0] - r[1] * q[1] - r[2] * q[2] - r[3] * q[3];
////		res[1] = r[0] * q[1] + r[1] * q[0] - r[2] * q[3] - r[3] * q[2];
////		res[2] = r[0] * q[2] + r[1] * q[3] + r[2] * q[0] - r[3] * q[1];
////		res[3] = r[0] * q[3] - r[1] * q[2] + r[2] * q[1] - r[3] * q[0];
////
////		return new Quaternion(new Vector(res));
//
//		Vector v = this.getPureVec();
//		Vector w = r.getPureVec();
//		float s = this.getCoordinate().getDataElement(0);
//		float t = r.getCoordinate().getDataElement(0);
//
//		float w_ = (s * t) - v.dot(w);
//		Vector v_ = (w.scalarMul(s).add(v.scalarMul(t))).add(v.cross(w));
//
//		float[] res = new float[] { w_, v_.getDataElement(0), v_.getDataElement(1), v_.getDataElement(2) };
//
//		return new Quaternion(new Vector(res));
//
//	}
//	public static Quaternion eulerToQuaternion(Vector v) {
//
//		Quaternion temp = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
//		Quaternion pitch = new Quaternion(new Vector(new float[] { 1, 0, 0 }), v.getDataElement(0));
//		Quaternion yaw = new Quaternion(new Vector(new float[] { 0, 1, 0 }), v.getDataElement(1));
//		Quaternion roll = new Quaternion(new Vector(new float[] { 0, 0, 1 }), v.getDataElement(2));
//
//		temp = pitch.multiply(yaw);
//		temp = temp.multiply(roll);
//		return temp;
//	}
//
//	public Quaternion getInverse() {
//		Vector q = getConjugate().getCoordinate();
//		Vector temp = q.scalarMul(1 / (q.getNorm()));
//		return new Quaternion(temp);
//	}
//
//	public Quaternion getConjugate() {
//		float[] res = new float[] { coordinate.getDataElement(0), -1 * coordinate.getDataElement(1),
//				-1 * coordinate.getDataElement(2), -1 * coordinate.getDataElement(3) };
//
//		return new Quaternion(new Vector(res));
//	}
//
//	public Vector rotatePoint(Vector v) {
//		Quaternion q_ = getInverse();
//		Quaternion res = (new Quaternion(v)).multiply(q_);
//		res = this.multiply(res);
//		return res.getPureVec();
//	}
//
//	public Vector[] rotatePoints(Vector[] vList) {
//		
//		Quaternion q_ = getInverse();
//		Vector[] res = new Vector[vList.length];
//		
//		int i = 0;
//		for (Vector v : vList) {
//			Quaternion temp = (new Quaternion(v)).multiply(q_);
//			temp = this.multiply(temp);
//			res[i] = temp.getPureVec();
//			i++;
//		}
//		
//		return res;
//	}
//
//	public Matrix getRotationMatrix() {
//
//		float[] q = this.getCoordinate().getData();
//
//		float[][] data = new float[][] {
//				{ 1 - 2 * (q[2] * q[2] + q[3] * q[3]), 2 * (q[1] * q[2] - q[3] * q[0]),
//						2 * (q[1] * q[3] + q[2] * q[0]) },
//				{ 2 * (q[1] * q[2] + q[3] * q[0]), 1 - 2 * (q[1] * q[1] + q[3] * q[3]),
//						2 * (q[2] * q[3] - q[1] * q[0]) },
//				{ 2 * (q[1] * q[3] - q[2] * q[0]), 2 * (q[2] * q[3] + q[1] * q[0]),
//						1 - 2 * (q[1] * q[1] + q[2] * q[2]) } };
//
//		return new Matrix(data);
//	}
//
//	public Vector getPitchYawRoll() {
//		float x = this.getCoordinate().getDataElement(1);
//		float y = this.getCoordinate().getDataElement(2);
//		float z = this.getCoordinate().getDataElement(3);
//		float w = this.getCoordinate().getDataElement(0);
//
//		float yaw = (float) Math.toDegrees(Math.atan2(2 * y * w - 2 * x * z, 1 - 2 * y * y - 2 * z * z));
//		float pitch = (float) Math.toDegrees(Math.atan2(2 * x * w - 2 * y * z, 1 - 2 * x * x - 2 * z * z));
//		float roll = (float) Math.toDegrees(Math.asin(2 * x * y + 2 * z * w));
//
//		return new Vector(new float[] { pitch, yaw, roll });
//	}
//
//	public float getNorm() {
////		float res = 0;
////		res += Math.pow(coordinate.getDataElement(0),2);
////		res += Math.pow(coordinate.getDataElement(1),2);
////		res += Math.pow(coordinate.getDataElement(2),2);
////		res += Math.pow(coordinate.getDataElement(3),2);
//
//		return coordinate.getNorm();
//	}
//
//	public Vector getAxis() {
//		return axis;
//	}
//
//	public Vector getCoordinate() {
//		return coordinate;
//	}
//
//	public void setCoordinate(Vector v) {
//
//		try {
//			if (v.getNumberOfDimensions() == 4) {
//				this.coordinate = v;
//			} else if (v.getNumberOfDimensions() == 3) {
//				float[] dat = new float[4];
//				dat[0] = 0f;
//				dat[1] = v.getDataElement(0);
//				dat[2] = v.getDataElement(1);
//				dat[3] = v.getDataElement(2);
//				this.coordinate = new Vector(dat).normalise();
//			} else
//				throw new Exception("Coordinate of a quaternion should only have 3 or 4 elements ");
//		} catch (Exception e) {
//		}
//
//		reculateAxisAngle();
//	}
//
//	public void normalise() {
//		this.coordinate = this.coordinate.normalise();
//		this.axis = this.axis.normalise();
//	}
//
//	public void reculateAxisAngle() {
//		float angle = (float) (2 * Math.acos(this.coordinate.getDataElement(0)));
//		this.angle = (float) Math.toDegrees(angle);
//
//		if (this.angle != 0) {
//
//			float temp = (float) Math.sqrt(1 - (coordinate.getDataElement(0) * coordinate.getDataElement(0)));
//			float[] axisData = new float[] { (float) (coordinate.getDataElement(1) / temp),
//					(float) (coordinate.getDataElement(2) / temp), (float) (coordinate.getDataElement(3) / temp) };
//
//			this.axis = new Vector(axisData).normalise();
//
//		} else {
//			float[] axisData = new float[] { 0, 0, 1 };
//			this.axis = new Vector(axisData);
//		}
//	}
//
//	public void recalculateCoordinate() {
//		float a = (float) Math.toRadians(angle / 2f);
//		float[] data = new float[] { (float) Math.cos(a), (float) (Math.sin(a) * axis.getDataElement(0)),
//				(float) (Math.sin(a) * axis.getDataElement(1)), (float) (Math.sin(a) * axis.getDataElement(2)) };
//		this.coordinate = new Vector(data);
//	}
//
//	public Vector getPureVec() {
//		return new Vector(new float[] { coordinate.getDataElement(1), coordinate.getDataElement(2),
//				coordinate.getDataElement(3) });
//	}
//
//	public void setAxis(Vector axis) {
//		try {
//			if (axis.getNumberOfDimensions() != 3) {
//				throw new IllegalArgumentException("The axis of a quaternion can only have 3 elements");
//			}
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		}
//		this.axis = axis.normalise();
//		recalculateCoordinate();
//	}
//
//	public float getAngle() {
//		return angle;
//	}
//
//	public void setAngle(float angle) {
//
//		if (angle > 360) {
//			angle -= 360;
//		}
//
//		if (angle < -360) {
//			angle += 360;
//		}
//
//		this.angle = angle;
//		recalculateCoordinate();
//	}

}
