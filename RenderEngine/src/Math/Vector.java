package Math;

import java.util.List;

public class Vector {

	private float[] data;
	private int numberOfDimensions;

	public int getNumberOfDimensions() {
		return numberOfDimensions;
	}

	public Vector(float[] data) {
		this.data = data;
		this.numberOfDimensions = data.length;
	}

	public Vector(int size) {
		this.numberOfDimensions = size;
		data = new float[size];
		for (int i = 0; i < size; i++) {
			data[i] = 0;
		}
	}

	public Vector(int size, float val) {
		this.numberOfDimensions = size;
		data = new float[size];
		for (int i = 0; i < size; i++) {
			data[i] = val;
		}
	}
	
	public Vector(Vector v) {
		this.numberOfDimensions = v.getNumberOfDimensions();
		data = new float[numberOfDimensions];
		for (int i = 0; i < numberOfDimensions; i++) {
			data[i] = v.get(i);
		}
	}

	public Vector add(Vector v) {
		float[] res = null;
		try {
			if (getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = v.getData()[i] + this.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Vector(res);
	}
	
	public float getAngleBetweenVectors(Vector x) {
		Vector v = this.normalise();
		Vector w = x.normalise();
		
		float angle = v.dot(w);
		return (float) Math.toDegrees(angle);
	}

	public static Vector add(Vector u, Vector v) {
		float[] res = null;

		try {
			if (u.getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[u.getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = v.getData()[i] + u.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Vector(res);

	}

	public float getNorm() {
		float sum = 0;
		for (float f : data) {
			sum += (f * f);
		}
		float l = (float) Math.sqrt(sum);
		return l;
	}

	public static double getLength(Vector v) {
		double sum = 0;
		for (float f : v.getData()) {
			sum += (f * f);
		}
		double l = Math.sqrt(sum);
		return l;
	}

	public Vector normalise() {
		if(this.getNorm() != 0) {
			return this.scalarMul((float) (1.0 / this.getNorm()));
		}
		else {
			return this;
		}
	}

	public static Vector normalise(Vector v) {
		if(v.getNorm() != 0) {
			return v.scalarMul((float) (1.0 / v.getNorm()));
		}
		else {
			return v;
		}
	}

	public Vector sub(Vector v) {
		float[] res = null;

		try {
			if (getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = this.getData()[i] - v.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Vector(res);
	}

	public static Vector sub(Vector u, Vector v) {
		float[] res = null;

		try {
			if (u.getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[u.getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = u.getData()[i] - v.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Vector(res);

	}

	public Vector mul(Vector v) {
		float[] res = null;

		try {
			if (getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = v.getData()[i] * this.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Vector(res);

	}

	public static Vector mul(Vector u, Vector v) {
		float[] res = null;

		try {
			if (u.getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[u.getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = v.getData()[i] * u.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Vector(res);

	}

	public Vector divide(Vector v) {
		float[] res = null;

		try {
			if (getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = this.getData()[i] + v.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Vector(res);
	}

	public static Vector divide(Vector u, Vector v) {
		float[] res = null;

		try {
			if (u.getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[u.getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = u.getData()[i] / v.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Vector(res);
	}

	public Vector scalarAdd(float v) {
		float[] res;

		res = new float[getNumberOfDimensions()];
		for (int i = 0; i < getNumberOfDimensions(); i++) {
			res[i] = getData()[i] + v;
		}
		return new Vector(res);

	}

	public static Vector scalarAdd(Vector v, float x) {
		float[] res;

		res = new float[v.getNumberOfDimensions()];
		for (int i = 0; i < v.getNumberOfDimensions(); i++) {
			res[i] = v.getData()[i] + x;
		}
		return new Vector(res);

	}

	public Vector scalarMul(float v) {
		float[] res;

		res = new float[getNumberOfDimensions()];
		for (int i = 0; i < getNumberOfDimensions(); i++) {
			res[i] = getData()[i] * v;
		}
		return new Vector(res);

	}

	public static Vector scalarMul(Vector v, float x) {
		float[] res;

		res = new float[v.getNumberOfDimensions()];
		for (int i = 0; i < v.getNumberOfDimensions(); i++) {
			res[i] = v.getData()[i] * x;
		}
		return new Vector(res);

	}

	public float[] getData() {
		return data;
	}

	public void setData(float[] data) {
		this.data = data;
		this.numberOfDimensions = data.length;
	}

	public float get(int index) {
		return data[index];
	}

	public void setDataElement(int index, float val) {
		data[index] = val;
	}

	public Vector cross(Vector v) {
		float[] res = new float[3];
		res[0] = (this.get(1) * v.get(2)) - (this.get(2) * v.get(1));
		res[1] = -((this.get(0) * v.get(2)) - (this.get(2) * v.get(0)));
		res[2] = (this.get(0) * v.get(1)) - (this.get(1) * v.get(0));
		return new Vector(res);
	}

	public static Vector cross(Vector u, Vector v) {
		float[] res = new float[3];
		res[0] = (u.get(1) * v.get(2)) - (u.get(2) * v.get(1));
		res[1] = -((u.get(0) * v.get(2)) - (u.get(2) * v.get(0)));
		res[2] = (u.get(0) * v.get(1)) - (u.get(1) * v.get(0));
		return new Vector(res);
	}

	public float dot(Vector v) {
		float res = 0;

		try {
			if (numberOfDimensions == v.getNumberOfDimensions()) {
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res += v.getData()[i] * this.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public static float dot(Vector u, Vector v) {
		float res = 0;

		try {
			if (u.getNumberOfDimensions() == v.getNumberOfDimensions()) {
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res += v.getData()[i] * u.getData()[i];
				}
			} else {
				throw new Exception("The vectors dont match in size");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public void display() {
		System.out.print("[");
		for (int i = 0; i < getNumberOfDimensions(); i++) {
			System.out.print(getData()[i]);
			if (i + 1 != getNumberOfDimensions()) {
				System.out.print("\t");
			}
		}
		System.out.print("]");
		System.out.println();
	}

	public static void display(Vector v) {
		for (int i = 0; i < v.getNumberOfDimensions(); i++) {
			System.out.println(v.getData()[i]);
		}
	}

	public static Vector[] addDimensionToVec(Vector[] v, float val) {

		Vector[] resData = new Vector[v.length];

		for (int i = 0; i < v.length; i++) {
			float[] tempDat = new float[v[i].getNumberOfDimensions() + 1];
			for (int j = 0; j < v[i].getNumberOfDimensions(); j++) {
				tempDat[j] = v[i].get(j);
			}
			tempDat[v[i].getNumberOfDimensions()] = val;
			resData[i] = new Vector(tempDat);
		}

		return resData;
	}
	
	public static Matrix addDimensionToVec(List<Vector> v, float val) {

		Vector[] resData = new Vector[v.size()];

		for (int i = 0; i < v.size(); i++) {
			float[] tempDat = new float[v.get(i).getNumberOfDimensions() + 1];
			for (int j = 0; j < v.get(i).getNumberOfDimensions(); j++) {
				tempDat[j] = v.get(i).get(j);
			}
			tempDat[v.get(i).getNumberOfDimensions()] = val;
			resData[i] = new Vector(tempDat);
		}

		return new Matrix(resData);
	}

	public Vector addDimensionToVec(float val) {

		float[] tempDat = new float[this.getNumberOfDimensions() + 1];
		
		for (int j = 0; j < this.getNumberOfDimensions(); j++) {
			tempDat[j] = this.get(j);
		}
		tempDat[this.getNumberOfDimensions()] = val;

		return new Vector(tempDat);
	}

	public Vector getCopy() {
		float[] res = new float[this.getNumberOfDimensions()];
		
		for(int i = 0;i < this.getNumberOfDimensions();i++) {
			res[i] = this.get(i);
		}
		return new Vector(res);
	}
	
	public String toString() {
		String res = "";
		res+="[";
		for (int i = 0; i < getNumberOfDimensions(); i++) {
			res+=(getData()[i]);
			if (i + 1 != getNumberOfDimensions()) {
				res+="    ";
			}
		}
		res+="]";
		return res;
	}

}
