package Kurama.Math;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	public static Vector randomVector(int size) {
		float[] data = new float[size];
		for(int i = 0;i < size;i++) {
			float sign = Math.random() > 0.5d ? -1.0f : 1.0f;
			data[i] = sign * (float)Math.random();
		}
		return new Vector(data);
	}

	// Load Vector from string
	public static Vector loadFromString(String string) {
		String[] tokens = string.split(" ");
		List<Float> val = new ArrayList<>();

		for(int i = 0; i < tokens.length; i++) {
			val.add(Float.parseFloat(tokens[i]));
		}

		return new Vector(val);
	}

	public Vector(List<Float> data) {
		this.data = new float[data.size()];
		for(int i = 0;i < data.size();i++) {
			this.data[i] = data.get(i);
		}
		this.numberOfDimensions = this.data.length;
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

	public Vector (float x, float y, float z) {
		this.numberOfDimensions = 3;
		data = new float[3];
		data[0] = x;
		data[1] = y;
		data[2] = z;
	}

	public Vector (float x, float y, float z,float w) {
		this.numberOfDimensions = 4;
		data = new float[4];
		data[0] = x;
		data[1] = y;
		data[2] = z;
		data[3] = w;
	}

	public Vector (float x, float y) {
		this.numberOfDimensions = 2;
		data = new float[2];
		data[0] = x;
		data[1] = y;
	}

	public Vector (float x) {
		this.numberOfDimensions = 1;
		data = new float[1];
		data[0] = x;
	}

	public Vector add(Vector v) {
		float[] res = null;

			if (getNumberOfDimensions() == v.getNumberOfDimensions()) {
				res = new float[getNumberOfDimensions()];
				for (int i = 0; i < v.getNumberOfDimensions(); i++) {
					res[i] = v.getData()[i] + this.getData()[i];
				}
			} else {
				throw new IllegalArgumentException("The vectors dont match in size");
			}

		return new Vector(res);
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) {
			return false;
		}
		Vector v = (Vector)o;

		if(this.getNumberOfDimensions() != v.getNumberOfDimensions()) {
			return false;
		}

		return this.sub(v).getNorm() == 0;
	}

	@Override
	public int hashCode() {

		String temp = "";
		for(float val:this.getData()) {
//			Convert float to int
			Double ref = (double) val;
			int decLen = ref.toString().split("\\.")[1].length();
			if (decLen >= 1)
				ref = ref * decLen;
			int ref_int = ref.intValue();

			temp += ref_int;
		}

		return Integer.parseInt(temp);
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
		return (float) Math.sqrt(sum);
	}

	public float sumSquared() {
		float sum = 0;
		for (float f : data) {
			sum += (f * f);
		}
		return (float) sum;
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

	public float sumAllElements() {
		Vector temp = new Vector(this.numberOfDimensions,1);
		return this.dot(temp);
	}

	public Vector sub(Vector v) {
		float[] res = null;

		if (getNumberOfDimensions() == v.getNumberOfDimensions()) {
			res = new float[getNumberOfDimensions()];
			for (int i = 0; i < v.getNumberOfDimensions(); i++) {
				res[i] = this.getData()[i] - v.getData()[i];
			}
		} else {
			throw new IllegalArgumentException("The vectors dont match in size: Vec 1 is of size "+this.getNumberOfDimensions()+" whereas input vector is of size "+v.getNumberOfDimensions());
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
					res[i] = this.getData()[i] / v.getData()[i];
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

	public void setValuesToBuffer(FloatBuffer buffer) {
		for(var v: data) {
			buffer.put(v);
		}
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

	public int geti(int index) {
		return (int)data[index];
	}

	public Vector setDataElement(int index, float val) {
		data[index] = val;
		return this;
	}

	public Vector cross(Vector v) {
		Vector u = this;
		if(u.getNumberOfDimensions() != v.getNumberOfDimensions()) {
			throw new IllegalArgumentException("Vectors do not match in size");
		}

		if(u.getNumberOfDimensions() == 2) {
			u = u.append(0);
			v = v.append(0);
		}

		float[] res = new float[3];
		res[0] = (u.get(1) * v.get(2)) - (u.get(2) * v.get(1));
		res[1] = -((u.get(0) * v.get(2)) - (u.get(2) * v.get(0)));
		res[2] = (u.get(0) * v.get(1)) - (u.get(1) * v.get(0));
		return new Vector(res);
	}

	public static Vector getRandomVector(Vector min, Vector max, Random random) {
		if(min.numberOfDimensions != max.numberOfDimensions) {
			throw new RuntimeException("Min and max should be of same size to generate random number");
		}

		int dimensions =  min.getNumberOfDimensions();
		Vector p = new Vector(dimensions,0);
		for (int i = 0; i < dimensions; i++) {
			float deltaVal = max.get(i) - min.get(i);
			float start = min.get(i);
			float randVal = start + deltaVal*random.nextFloat();
			p.setDataElement(i,randVal);
		}

		return p;
	}

	public static Vector getAverage(List<Vector> list) {
		if(list == null) {
			return null;
		}
		
		if(list.size() == 0) {
//			throw new RuntimeException("List size is zero. Could not create average");
			return null;
		}

		Vector avg = new Vector(list.get(0).getNumberOfDimensions(),0);
		for(Vector v: list) {
			avg = avg.add(v);
		}
		avg = avg.scalarMul(1f / (float)list.size());
		return avg;
	}

	public static Vector cross(Vector u, Vector v) {
		if(u.getNumberOfDimensions() != v.getNumberOfDimensions()) {
			throw new IllegalArgumentException("Vectors do not match in size");
		}

		if(u.getNumberOfDimensions() == 2) {
			u = u.append(0);
			v = v.append(1);
		}

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

	public static Vector[] append(Vector[] v, float val) {

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
	
	public static Matrix append(List<Vector> v, float val) {

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

	public Vector append(float val) {

		float[] tempDat = new float[this.getNumberOfDimensions() + 1];
		
		for (int j = 0; j < this.getNumberOfDimensions(); j++) {
			tempDat[j] = this.get(j);
		}
		tempDat[this.getNumberOfDimensions()] = val;

		return new Vector(tempDat);
	}

	public Vector removeDimensionFromVec(int ind) {

		List<Float> tempDat = new ArrayList<>();

		for (int j = 0; j < this.getNumberOfDimensions(); j++) {
			if(j != ind) {
				tempDat.add(this.get(j));
			}
		}
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
		for (int i = 0; i < getNumberOfDimensions(); i++) {
			res+=(getData()[i]);
			if (i + 1 != getNumberOfDimensions()) {
				res+=" ";
			}
		}

		return res;
	}

}
