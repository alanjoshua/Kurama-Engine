package Math;

public class Quaternion {

	private Vector axis;
	private float angle;
	private Vector coordinate;

	public Quaternion(Vector axis, float angle) {
		try {
			if (axis.getNumberOfDimensions() != 3) {
				throw new IllegalArgumentException("The axis of a quaternion can only have 3 elements");
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		this.axis = axis.normalise();
		this.setAngle(angle);
		recalculateCoordinate();
	}

	public Quaternion(Vector v) {

		try {
			if (v.getNumberOfDimensions() == 4) {
				this.coordinate = v.normalise();
			} else if (v.getNumberOfDimensions() == 3) {
				float[] dat = new float[4];
				dat[0] = 0f;
				dat[1] = v.getDataElement(0);
				dat[2] = v.getDataElement(1);
				dat[3] = v.getDataElement(2);
				this.coordinate = new Vector(dat).normalise();
			} else
				throw new Exception("Coordinate of a quaternion should only have 3 or 4 elements ");
		} catch (Exception e) {
		}

		reculateAxisAngle();
	}

	public Quaternion multiply(Quaternion r) {
//		float[] r = x.getCoordinate().getData();
//		float[] q = this.getCoordinate().getData();
//		float[] res = new float[4];
//
//		res[0] = r[0] * q[0] - r[1] * q[1] - r[2] * q[2] - r[3] * q[3];
//		res[1] = r[0] * q[1] + r[1] * q[0] - r[2] * q[3] - r[3] * q[2];
//		res[2] = r[0] * q[2] + r[1] * q[3] + r[2] * q[0] - r[3] * q[1];
//		res[3] = r[0] * q[3] - r[1] * q[2] + r[2] * q[1] - r[3] * q[0];
//
//		return new Quaternion(new Vector(res));
		
		Vector v = new Vector(new float[] {this.getCoordinate().getDataElement(1),this.getCoordinate().getDataElement(2),this.getCoordinate().getDataElement(3)});
		Vector w = new Vector(new float[] {r.getCoordinate().getDataElement(1),r.getCoordinate().getDataElement(2),r.getCoordinate().getDataElement(3)});
		float s = this.getCoordinate().getDataElement(0);
		float t = r.getCoordinate().getDataElement(0);
		
		float w_ = (s*t) - v.dot(w);
		Vector v_ = (w.scalarMul(s).add(v.scalarMul(t))).add(v.cross(w));
		
		float[] res = new float[] {
				w_,
				v_.getDataElement(0),
				v_.getDataElement(1),
				v_.getDataElement(2),
		};
		
		return new Quaternion(new Vector(res));
		
	}
	
	public static Quaternion eulerToQuaternion(Vector v) {
		Quaternion pitch = new Quaternion(new Vector(new float[] {1,0,0}),v.getDataElement(0));
		Quaternion yaw = new Quaternion(new Vector(new float[] {0,1,0}),v.getDataElement(1));
		Quaternion roll = new Quaternion(new Vector(new float[] {0,0,1}),v.getDataElement(2));
		
		Quaternion temp = pitch.multiply(roll);
		temp = yaw.multiply(temp);
		return temp;
	}

	public Quaternion getInverse() {
		Vector temp = getConjugate().getCoordinate();
		return new Quaternion(temp);
	}

	public Quaternion getConjugate() {
		float[] res = new float[] { coordinate.getDataElement(0), -1 * coordinate.getDataElement(1),
				-1 * coordinate.getDataElement(2), -1 * coordinate.getDataElement(3) };

		return new Quaternion(new Vector(res));
	}

	public Vector rotatePoint(Vector v) {
		Quaternion q_ = getInverse();
		Quaternion res = (new Quaternion(v)).multiply(q_);
		res = this.multiply(res);
		float[] resDat = new float[] { 
				res.getCoordinate().getDataElement(1), 
				res.getCoordinate().getDataElement(2),
				res.getCoordinate().getDataElement(3)
				};
		return new Vector(resDat);
	}

	public Matrix getRotationMatrix() {
		
		float[] q = this.getCoordinate().getData();

		float[][] data = new float[][] {
				{1 - 2*(q[2]*q[2] + q[3]*q[3]), 2*(q[1]*q[2] - q[3]*q[0]), 2*(q[1]*q[3] + q[2]*q[0])},
				{2*(q[1]*q[2] + q[3]*q[0]), 1 - 2*(q[1]*q[1] + q[3]*q[3]), 2*(q[2]*q[3] - q[1]*q[0])},
				{2*(q[1]*q[3] - q[2]*q[0]), 2*(q[2]*q[3] + q[1]*q[0]), 1 - 2*(q[1]*q[1] + q[2]*q[2])}
		};

		return new Matrix(data);
	}

	public float getNorm() {
//		float res = 0;
//		res += Math.pow(coordinate.getDataElement(0),2);
//		res += Math.pow(coordinate.getDataElement(1),2);
//		res += Math.pow(coordinate.getDataElement(2),2);
//		res += Math.pow(coordinate.getDataElement(3),2);

		return coordinate.getNorm();
	}

	public Vector getAxis() {
		return axis;
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
				dat[1] = v.getDataElement(0);
				dat[2] = v.getDataElement(1);
				dat[3] = v.getDataElement(2);
				this.coordinate = new Vector(dat).normalise();
			} else
				throw new Exception("Coordinate of a quaternion should only have 3 or 4 elements ");
		} catch (Exception e) {
		}

		reculateAxisAngle();
	}

	public void normalise() {
		this.coordinate = this.coordinate.normalise();
		this.axis = this.axis.normalise();
	}

	public void reculateAxisAngle() {
		float angle = (float) (2 * Math.acos(this.coordinate.getDataElement(0)));
		this.angle = (float) Math.toDegrees(angle);
		
		if (this.angle != 0) {
			
			float temp = (float) Math.sqrt(1 - (coordinate.getDataElement(0) * coordinate.getDataElement(0)));
			float[] axisData = new float[] { 
					(float) (coordinate.getDataElement(1) / temp),
					(float) (coordinate.getDataElement(2) / temp),
					(float) (coordinate.getDataElement(3) / temp)
					};

			this.axis = new Vector(axisData).normalise();

		}
		else {
			float[] axisData = new float[] {0,0,1};
			this.axis = new Vector(axisData);
		}
	}

	public void recalculateCoordinate() {
		float a = (float) Math.toRadians(angle / 2f);
		float[] data = new float[] { (float) Math.cos(a), (float) (Math.sin(a) * axis.getDataElement(0)),
				(float) (Math.sin(a) * axis.getDataElement(1)), (float) (Math.sin(a) * axis.getDataElement(2)) };
		this.coordinate = new Vector(data);
	}
	
	public Vector getPureVec() {
		return new Vector(new float[] {coordinate.getDataElement(1),coordinate.getDataElement(2),coordinate.getDataElement(3)});
	}

	public void setAxis(Vector axis) {
		try {
			if (axis.getNumberOfDimensions() != 3) {
				throw new IllegalArgumentException("The axis of a quaternion can only have 3 elements");
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		this.axis = axis.normalise();
		recalculateCoordinate();
	}

	public float getAngle() {
		return angle;
	}

	public void setAngle(float angle) {

		if (angle > 360) {
			angle -= 360;
		}

		if (angle < -360) {
			angle += 360;
		}

		this.angle = angle;
		recalculateCoordinate();
	}

}
