package Math;

import java.util.ArrayList;
import java.util.List;

public class Matrix {

	private float[][] data;
	private int rows, cols;

	public Matrix(float[][] data) {
		this.data = data;
		rows = data.length;
		cols = data[0].length;
	}

	public Matrix(int r, int c) {
		rows = r;
		cols = c;
		data = new float[r][c];
		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				data[i][j] = 0f;
			}
		}

	}

	public Matrix(int r, int c, float val) {
		rows = r;
		cols = c;
		data = new float[r][c];

		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				data[i][j] = val;
			}
		}
	}

	public Matrix(Vector[] v) {

		int r = v[0].getNumberOfDimensions();
		int count = 0;

		try {
			for (Vector t : v) {
				if (t.getNumberOfDimensions() != r) {
					throw new Exception("Vectors weren't the same size. Vector at the " + count
							+ "th position was a different size from the first vector");
				}
				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		int c = v.length;
		data = new float[r][c];

		for (int j = 0; j < c; j++) {
			for (int i = 0; i < r; i++) {
				data[i][j] = v[j].getData()[i];
			}
		}

		this.rows = r;
		this.cols = c;

	}

	public Matrix(List<Vector> v) {
		int r = v.get(0).getNumberOfDimensions();
		int count = 0;

		try {
			for (Vector t : v) {
				if (t.getNumberOfDimensions() != r) {
					throw new Exception("Vectors weren't the same size. Vector at the " + count
							+ "th position was a different size from the first vector");
				}
				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		int c = v.size();
		data = new float[r][c];

		for (int j = 0; j < c; j++) {
			for (int i = 0; i < r; i++) {
				data[i][j] = v.get(j).getData()[i];
			}
		}

		this.rows = r;
		this.cols = c;

	}

	public static Matrix getIdentityMatrix(int r) {
		float[][] res = new float[r][r];
		for (int i = 0; i < r; i++) {
			res[i][i] = 1f;
		}
		return new Matrix(res);
	}

	public static Matrix getDiagonalMatrix(Vector v) {
		float[][] res = new float[v.getNumberOfDimensions()][v.getNumberOfDimensions()];
		for (int i = 0; i < res.length; i++) {
			res[i][i] = v.get(i);
		}
		return new Matrix(res);
	}

	public static Matrix add(Matrix a, Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[a.getRows()][a.getCols()];
			if (a.getRows() == b.getRows() && a.getCols() == b.getCols()) {
				for (int i = 0; i < a.getRows(); i++) {
					for (int j = 0; j < a.getCols(); j++) {
						resData[i][j] = a.getData()[i][j] + b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(a.getRows(), a.getCols(), 1f);
				throw new Exception("Size of Matrix A and B should be the same");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;

	}

	public static Matrix sub(Matrix a, Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[a.getRows()][a.getCols()];
			if (a.getRows() == b.getRows() && a.getCols() == b.getCols()) {
				for (int i = 0; i < a.getRows(); i++) {
					for (int j = 0; j < a.getCols(); j++) {
						resData[i][j] = a.getData()[i][j] - b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(a.getRows(), a.getCols(), 1f);
				throw new Exception("Size of Matrix A and B should be the same");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;

	}

	public static Matrix mul(Matrix a, Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[a.getRows()][a.getCols()];
			if (a.getRows() == b.getRows() && a.getCols() == b.getCols()) {
				for (int i = 0; i < a.getRows(); i++) {
					for (int j = 0; j < a.getCols(); j++) {
						resData[i][j] = a.getData()[i][j] * b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(a.getRows(), a.getCols(), 1f);
				throw new Exception("Size of Matrix A and B should be the same");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;

	}

	public Matrix scalarAdd(float x) {
		float[][] res = new float[rows][cols];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				res[i][j] = data[i][j] + x;
			}
		}
		return new Matrix(res);
	}

	public static Matrix scalarAdd(Matrix a, float x) {
		float[][] res = new float[a.getRows()][a.getCols()];

		for (int i = 0; i < a.getRows(); i++) {
			for (int j = 0; j < a.getCols(); j++) {
				res[i][j] = a.getData()[i][j] + x;
			}
		}
		return new Matrix(res);
	}

	public Matrix scalarMul(float x) {
		float[][] res = new float[rows][cols];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				res[i][j] = data[i][j] * x;
			}
		}
		return new Matrix(res);
	}

	public static Matrix scalarMul(Matrix a, float x) {
		float[][] res = new float[a.getRows()][a.getCols()];

		for (int i = 0; i < a.getRows(); i++) {
			for (int j = 0; j < a.getCols(); j++) {
				res[i][j] = a.getData()[i][j] * x;
			}
		}
		return new Matrix(res);
	}

	public static Matrix divide(Matrix a, Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[a.getRows()][a.getCols()];
			if (a.getRows() == b.getRows() && a.getCols() == b.getCols()) {
				for (int i = 0; i < a.getRows(); i++) {
					for (int j = 0; j < a.getCols(); j++) {
						resData[i][j] = a.getData()[i][j] / b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(a.getRows(), a.getCols(), 1f);
				throw new Exception("Size of Matrix A and B should be the same");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;

	}

	public Matrix add(Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[rows][cols];
			if (rows == b.getRows() && cols == b.getCols()) {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						resData[i][j] = data[i][j] + b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
				throw new Exception("Size of Matrix A and B should be the same");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;

	}

	public Matrix sub(Matrix b) {

		Matrix res = null;
		float[][] resData = null;

		try {
			resData = new float[rows][cols];
			if (rows == b.getRows() && cols == b.getCols()) {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						resData[i][j] = data[i][j] - b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
				throw new Exception("Size of Matrix A and B should be the same");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		data = resData;
		return res;

	}

	public Matrix mul(Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[rows][cols];
			if (rows == b.getRows() && cols == b.getCols()) {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						resData[i][j] = data[i][j] * b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
				throw new Exception("Size of Matrix A and B should be the same");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public Matrix columnMul(Vector b) {

		Matrix res = null;

		try {
			float[][] resData = new float[rows][cols];
			if (rows == b.getNumberOfDimensions()) {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						resData[i][j] = data[i][j] * b.getData()[i];
					}
				}
				res = new Matrix(resData);
			}

			else {
				throw new Exception("Number of rows of matrix and the vector should be the same");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public Matrix divide(Matrix b) {

		Matrix res = null;

		try {
			float[][] resData = new float[rows][cols];
			if (rows == b.getRows() && cols == b.getCols()) {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						resData[i][j] = data[i][j] / b.getData()[i][j];
					}
				}
				res = new Matrix(resData);
			}

			else {
				throw new Exception("Size of Matrix A and B should be the same");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;

	}

	public static Matrix matMul(Matrix a, Matrix m) {
		Matrix res = null;

		try {
			float[][] resData;

			if (a.getCols() == m.rows) {

				resData = new float[a.getRows()][m.getCols()];

				for (int colsInM = 0; colsInM < m.getCols(); colsInM++) {

					for (int rowsInA = 0; rowsInA < a.getRows(); rowsInA++) {

						for (int colsInA = 0; colsInA < a.getCols(); colsInA++) {

							resData[rowsInA][colsInM] += a.getData()[rowsInA][colsInA] * m.getData()[colsInA][colsInM];

						}

					}

				}
				res = new Matrix(resData);
			} else {
//			System.err.println(" number of Columns on Mat A and rows of Mat B do not match");
//			System.out.println("Returning Matrix with values 1");
//			res = new Matrix(a.getRows(), m.getCols(), 1f);
				throw new Exception("Size of Matrix A and B should be the same");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
	}

	public Matrix matMul(Matrix m) {
		Matrix res = null;

		try {
			float[][] resData;

			if (cols == m.rows) {

				resData = new float[rows][m.getCols()];

				for (int colsInM = 0; colsInM < m.getCols(); colsInM++) {

					for (int rowsInA = 0; rowsInA < rows; rowsInA++) {

						for (int colsInA = 0; colsInA < cols; colsInA++) {

							resData[rowsInA][colsInM] += data[rowsInA][colsInA] * m.getData()[colsInA][colsInM];

						}

					}

				}
				res = new Matrix(resData);
			} else {
				throw new Exception("number of Columns on Mat A (" + this.cols + ") and rows of Mat B ("+ m.getRows() +") do not match\n ");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public Matrix matMul(Vector m) {
		Matrix res = null;

		try {
			float[][] resData;

			if (cols == m.getNumberOfDimensions()) {

				resData = new float[rows][1];

				for (int rowsInA = 0; rowsInA < rows; rowsInA++) {

					for (int colsInA = 0; colsInA < cols; colsInA++) {
						resData[rowsInA][0] += data[rowsInA][colsInA] * m.getData()[colsInA];

					}

				}

				res = new Matrix(resData);
			} else {
				throw new Exception("The number of Columns of the matrix and the rows of the vector do not match");
//			System.err.println("Size of Matrix A and B should be the same");
//			System.out.println("Returning Matrix of size A with values 1");
//			res = new Matrix(rows, cols, 1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;

	}

	public static Matrix matMul(Matrix a, Vector m) {
		Matrix res = null;

		try {
			float[][] resData;

			if (a.getCols() == m.getNumberOfDimensions()) {

				resData = new float[a.getRows()][1];

				for (int rowsInA = 0; rowsInA < a.getRows(); rowsInA++) {

					for (int colsInA = 0; colsInA < a.getRows(); colsInA++) {

						resData[rowsInA][0] += a.getData()[rowsInA][colsInA] * m.getData()[colsInA];

					}

				}

				res = new Matrix(resData);
			} else {
//			System.err.println(" number of Columns on Mat A and rows of Mat B do not match");
//			res = new Matrix(a.getRows(), 1, 1f);
				throw new Exception("The number of Columns of the matrix and the rows of the vector do not match");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
	}

	public float[][] getData() {
		return data;
	}

	public void setData(float[][] data) {
		this.data = data;
		rows = data.length;
		cols = data[0].length;
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public Matrix transpose() {
		float[][] res = new float[cols][rows];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				res[j][i] = getData()[i][j];
			}
		}
		return new Matrix(res);
	}

	public static Matrix transpose(Matrix a) {
		float[][] res = new float[a.getCols()][a.getRows()];
		for (int i = 0; i < a.getRows(); i++) {
			for (int j = 0; j < a.getCols(); j++) {
				res[j][i] = a.getData()[i][j];
			}
		}
		return new Matrix(res);
	}

	public Vector toVector() {
		
		float[] data = null;
		
		try {
			if (this.getCols() == 1) {
				data = this.transpose().getData()[0];
			} else if (this.getRows() == 1) {
				data = this.getData()[0];
			} else {
				throw new IllegalArgumentException("Matrix is not a row or column vector");
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return new Vector(data);
	}

	public static Vector toVector(Matrix a) {
		float[] data = null;
		
		try {
			if (a.getCols() == 1) {
				data = a.transpose().getData()[0];
			} else if (a.getRows() == 1) {
				data = a.getData()[0];
			} else {
				throw new IllegalArgumentException("Matrix is not a row or column vector");
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return new Vector(data);
	}

	public void display() {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				System.out.print(data[i][j] + "\t");
			}
			System.out.println();
		}
	}

	public static void display(Matrix a) {
		for (int i = 0; i < a.getRows(); i++) {
			for (int j = 0; j < a.getCols(); j++) {
				System.out.print(a.getData()[i][j] + "\t");
			}
			System.out.println();
		}
	}
	
	public Matrix AddVectorToColumns(Vector v) {
		float[][] data = null;
		try {
			if(v.getNumberOfDimensions() == this.rows) {
				data = new float[this.getRows()][this.getCols()];
				for(int i = 0; i < this.getRows();i++) {
					for(int j= 0 ;j < this.getCols();j++) {
						data[i][j] = this.getData()[i][j] + v.get(i);
					}
				}
			}
			else {
				throw new IllegalArgumentException("Number of rows of input vectors and matrix should be the same");
			}
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return new Matrix(data);
		
	}

	public Matrix getCopy() {
		return new Matrix(this.data);
	}

	public Matrix getInverse() {
		float[][] data = null;

		if (this.cols != this.rows) {
			System.err.println("Only the inverse of a square matrix could be computed");
			return null;
		}

		float det = Matrix.getDeterminant(this);

		if (det == 0) {
			System.out.println("Determinant is zero. Returning null");
			return null;
		}

		data = new float[this.rows][this.cols];

		if (this.cols == 2) {
			data[0][0] = this.getData()[1][1];
			data[1][1] = this.getData()[0][0];
			data[1][0] = this.getData()[1][0] * -1;
			data[0][1] = this.getData()[0][1] * -1;

		} else {
			for (int i = 0; i < this.rows; i++) {
				for (int j = 0; j < this.cols; j++) {
					data[i][j] = (float) (Math.pow(-1, i + j + 2)
							* Matrix.getDeterminant(Matrix.getSmallerMatrix(this, j, i)));
				}
			}
		}

		Matrix fin = new Matrix(data).scalarMul(1 / det);

		return fin;
	}

	public static float getDeterminant(Matrix m) {
		float res = 0;
		if (m.cols != m.rows) {
			System.err.println("Only the inverse of a square matrix could be computed. Stopped at Matrix : ");
			m.display();
			return res;
		}

		if (m.rows == 2) {
			res = m.getData()[0][0] * m.getData()[1][1] - m.getData()[1][0] * m.getData()[0][1];
		}

		else {
			for (int j = 0; j < m.cols; j++) {
				Matrix small = Matrix.getSmallerMatrix(m, 0, j);
				double temp = m.getData()[0][j] * Math.pow(-1, j + 2) * Matrix.getDeterminant(small);
				res += temp;
			}
		}

		return res;
	}

	public static Matrix getSmallerMatrix(Matrix m, int r, int c) {

		if ((m.rows - 1) < 1 || (m.cols - 1) < 1) {
			System.err.println(
					"This function only works for square matrices of size 2 or larger.Returning a Null Matrix...");
			return null;
		}

		float[] data = new float[(m.rows - 1) * (m.cols - 1)];
		int count = 0;
		for (int i = 0; i < m.rows; i++) {
			for (int j = 0; j < m.cols; j++) {
				if (i != r && j != c) {
					data[count] = m.getData()[i][j];
					count++;
				}
			}
		}
		float[][] data2D = new float[(m.rows - 1)][m.cols - 1];

		for (int i = 0; i < data.length; i++) {
			int rr = Math.floorDiv(i, (m.cols - 1));
			int cc = i % (m.cols - 1);
			data2D[rr][cc] = data[i];
		}

		return new Matrix(data2D);
	}
	
	public Matrix getMatrixWithoutLastRow() {
		float[][] data = new float[this.rows - 1][this.cols];
		for(int i = 0;i < data.length;i++) {
			for(int j = 0;j < this.cols;j++) {
				data[i][j] = this.data[i][j];
			}
		}
		return new Matrix(data);
	}

	public Vector[] convertToVectorArray() {
		Vector[] v = new Vector[cols];

		for (int j = 0; j < cols; j++) {
			float[] vecData = new float[rows];
			for (int i = 0; i < rows; i++) {
				vecData[i] = data[i][j];
			}
			v[j] = new Vector(vecData);
		}

		return v;
	}

	public List<Vector> convertToVectorList() {
		List<Vector> v = new ArrayList<Vector>();

		for (int j = 0; j < cols; j++) {
			float[] vecData = new float[rows];
			for (int i = 0; i < rows; i++) {
				vecData[i] = data[i][j];
			}
			v.add(new Vector(vecData));
		}

		return v;
	}

	public static Matrix getRotateX(double x) {

		double angle = Math.toRadians(x);

		float[][] data = new float[][] { { 1f, 0f, 0f },
				{ 0f, (float) (Math.cos(angle)), (float) (-1 * Math.sin(angle)) },
				{ 0f, (float) (Math.sin(angle)), (float) (Math.cos(angle)) } };

		return new Matrix(data);
	}

	public static Matrix getRotateY(double x) {

		double angle = Math.toRadians(x);

		float[][] data = new float[][] { { (float) (Math.cos(angle)), 0f, (float) (Math.sin(angle)) }, { 0f, 1f, 0f },
				{ (float) (-1 * Math.sin(angle)), 0, (float) (Math.cos(angle)) } };

		return new Matrix(data);
	}

	public static Matrix getRotateZ(double x) {
		double angle = Math.toRadians(x);
		float[][] data = new float[][] { { (float) (Math.cos(angle)), (float) (-1 * Math.sin(angle)), 0f },
				{ (float) (Math.sin(angle)), (float) (Math.cos(angle)), 0f }, { 0f, 0f, 1f } };
		return new Matrix(data);

	}

	public Matrix addColumn(Vector v) {
		Matrix res = null;
		float[][] dat = new float[this.rows][this.cols + 1];
		try {
			if (v.getNumberOfDimensions() == this.getRows()) {
				for (int i = 0; i < this.getRows(); i++) {
					dat[i][this.getCols()] = v.get(i);
				}
				for (int i = 0; i < this.getRows(); i++) {
					for (int j = 0; j < this.getCols(); j++) {
						dat[i][j] = this.getData()[i][j];
					}
				}
			} else {
				throw new IllegalArgumentException(
						"The number of rows of the vector column being added should have the same number of rows as the matrix it is being added to");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		res = new Matrix(dat);
		return res;
	}

	public static Matrix addColumn(Matrix m, Vector v) {
		Matrix res = null;
		float[][] dat = new float[m.rows][m.cols + 1];
		try {
			if (v.getNumberOfDimensions() == m.getRows()) {
				for (int i = 0; i < m.getRows(); i++) {
					dat[i][m.getCols()] = v.get(i);
				}
				for (int i = 0; i < m.getRows(); i++) {
					for (int j = 0; j < m.getCols(); j++) {
						dat[i][j] = m.getData()[i][j];
					}
				}
			} else {
				throw new IllegalArgumentException(
						"The number of rows of the vector column being added should have the same number of rows as the matrix it is being added to");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		res = new Matrix(dat);
		return res;
	}

	public Matrix addRow(Vector v) {
		Matrix res = null;
		float[][] dat = new float[this.rows + 1][this.cols];

		try {
			if (v.getNumberOfDimensions() == this.getCols()) {
				for (int i = 0; i < this.getCols(); i++) {
					dat[this.getRows()][i] = v.get(i);
				}
				for (int i = 0; i < this.getRows(); i++) {
					for (int j = 0; j < this.getCols(); j++) {
						dat[i][j] = this.getData()[i][j];
					}
				}
			} else {
				throw new IllegalArgumentException(
						"The number of columns of the vector row being added should have the same number of columns as the matrix it is being added to");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		res = new Matrix(dat);
		return res;
	}

	public static Matrix addRow(Matrix m, Vector v) {
		Matrix res = null;
		float[][] dat = new float[m.rows + 1][m.cols];
		try {
			if (v.getNumberOfDimensions() == m.getCols()) {
				for (int i = 0; i < m.getCols(); i++) {
					dat[m.getRows()][i] = v.get(i);
				}
				for (int i = 0; i < m.getRows(); i++) {
					for (int j = 0; j < m.getCols(); j++) {
						dat[i][j] = m.getData()[i][j];
					}
				}
			} else {
				throw new IllegalArgumentException(
						"The number of columns of the vector row being added should have the same number of columns as the matrix it is being added to");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

}
