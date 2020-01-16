package models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import Math.Vector;

public class ModelBuilder {

	public static Model buildCube() {
		Vector[] vertexO = new Vector[8];
		vertexO[0] = new Vector(new float[] { 0, 0, 0 });
		vertexO[1] = new Vector(new float[] { 1, 0, 0 });
		vertexO[2] = new Vector(new float[] { 0, 1, 0 });
		vertexO[3] = new Vector(new float[] { 1, 1, 0 });
		vertexO[4] = new Vector(new float[] { 0, 0, 1 });
		vertexO[5] = new Vector(new float[] { 1, 0, 1 });
		vertexO[6] = new Vector(new float[] { 0, 1, 1 });
		vertexO[7] = new Vector(new float[] { 1, 1, 1 });

//		Vector[] cons = new Vector[12];
		List<int[]> cons = new ArrayList<int[]>();

		cons.add(new int[] { 0, 1 });
		cons.add(new int[] { 1, 3 });
		cons.add(new int[] { 3, 2 });
		cons.add(new int[] { 0, 2 });

		cons.add(new int[] { 0, 4 });
		cons.add(new int[] { 2, 6 });
		cons.add(new int[] { 4, 6 });

		cons.add(new int[] { 1, 5 });
		cons.add(new int[] { 4, 7 });
		cons.add(new int[] { 5, 7 });

		cons.add(new int[] { 6, 7 });
		cons.add(new int[] { 4, 5 });

		return new Model(vertexO, cons, null, null, null, null);
	}

	public static Model buildModelFromFile(String loc) {

		URL url = ModelBuilder.class.getResource("Resources");
		String path = url.getPath() + File.separator + loc;

		Model res = null;
		List<Vector> vertex = new ArrayList<Vector>();
		List<Vector> vn = new ArrayList<Vector>();
		List<Vector> vt = new ArrayList<Vector>();
		List<int[]> faces = new ArrayList<int[]>();
		List<int[]> textureFaces = new ArrayList<int[]>();
		List<int[]> normalFaces = new ArrayList<int[]>();

		if (path.substring(path.length() - 3, path.length()).equalsIgnoreCase("obj")) {

			try {
				String line = "";
				BufferedReader br = new BufferedReader(new FileReader(new File(path)));

				while ((line = br.readLine()) != null) {

					String[] split = line.split("\\s+");

					if (split.length > 1) {

//						Reads lines which start with "v" and stores the vertex data in the vertex list. 
//						If the fourth coordinate "w" is not given, it is given a default value of 1.0

						if (split[0].equalsIgnoreCase("v")) {
							float[] data = new float[4];
							for (int i = 0; i < 3; i++) {
								data[i] = Float.parseFloat(split[i + 1]);
							}
							if (split.length == 4) {
								data[3] = 1.0f;
							} else if (split.length == 5) {
								data[3] = Float.parseFloat(split[4]);
							}
							vertex.add(new Vector(data));
						}

						if (split[0].equalsIgnoreCase("vn")) {
							float[] data = new float[4];
							for (int i = 0; i < 3; i++) {
								data[i] = Float.parseFloat(split[i + 1]);
							}
							if (split.length == 4) {
								data[3] = 1.0f;
							} else if (split.length == 5) {
								data[3] = Float.parseFloat(split[4]);
							}
							vn.add(new Vector(data));
						}

						if (split[0].equalsIgnoreCase("vt")) {
							float[] data = new float[3];
							data[0] = Float.parseFloat(split[1]);
							if (split.length == 2) {
								data[1] = 0f;
								data[2] = 0f;
							} else if (split.length == 3) {
								data[1] = Float.parseFloat(split[2]);
								data[2] = 0f;
							} else if (split.length == 4) {
								data[1] = Float.parseFloat(split[2]);
								data[2] = Float.parseFloat(split[3]);
							}

							vt.add(new Vector(data));
						}

						if (split[0].equalsIgnoreCase("f")) {

							int[] fdata = new int[split.length - 1];
							int[] tdata = new int[split.length - 1];
							int[] ndata = new int[split.length - 1];

							String[] doubleSplitTest = split[1].split("/");

							if (doubleSplitTest.length == 1) {
								tdata = null;
								ndata = null;
								for (int i = 1; i < split.length; i++) {
									fdata[i - 1] = (Integer.parseInt(split[i]) - 1);
								}
							} else if (doubleSplitTest.length == 2) {
								ndata = null;
								for (int i = 1; i < split.length; i++) {
									fdata[i - 1] = (Integer.parseInt(split[i].split("/")[0]) - 1);
									tdata[i - 1] = (Integer.parseInt(split[i].split("/")[1]) - 1);
								}
							} else if (doubleSplitTest.length == 3) {

								if (doubleSplitTest[1].equalsIgnoreCase("")) {
									tdata = null;
									for (int i = 1; i < split.length; i++) {
										fdata[i - 1] = (Integer.parseInt(split[i].split("/")[0]) - 1);
										ndata[i - 1] = (Integer.parseInt(split[i].split("/")[2]) - 1);
									}
								} else {
									for (int i = 1; i < split.length; i++) {
										fdata[i - 1] = (Integer.parseInt(split[i].split("/")[0]) - 1);
										tdata[i - 1] = (Integer.parseInt(split[i].split("/")[1]) - 1);
										ndata[i - 1] = (Integer.parseInt(split[i].split("/")[2]) - 1);
									}
								}
							}

							faces.add(fdata);
							textureFaces.add(tdata);
							normalFaces.add(ndata);
						}

//						if (split[0].equalsIgnoreCase("f")) {
//
//							List<Integer> data = new ArrayList<Integer>();
//							for (int i = 1; i < split.length; i++) {
//								try {
//									data.add(Integer.parseInt(split[i].split("/")[0]) - 1);
//
//								} catch (Exception e) {
//								}
//								;
//							}
//
//							int[] data2 = new int[data.size()];
//							for (int i = 0; i < data2.length; i++) {
//								data2[i] = data.get(i);
//							}
//							faces.add(data2);
//
//						}
						
					}
				}
				
//				List<int[]> facesTriangle = new ArrayList<int[]>();
//				List<int[]> textureFacesTriangle = new ArrayList<int[]>();
//				List<int[]> normalFacesTriangle = new ArrayList<int[]>();
//				
//				if (faces != null) {
//				for (int[] x : faces) {
//					if (x.length == 4) {
//						int[] t1 = new int[3];
//						int[] t2 = new int[3];
//						t1[0] = x[0];
//						t1[1] = x[1];
//						t1[2] = x[3];
//						t2[0] = x[1];
//						t2[1] = x[2];
//						t2[2] = x[3];
//						facesTriangle.add(t1);
//						facesTriangle.add(t2);
//					} else {
//						facesTriangle.add(x);
//					}
//				}
//			} else {
//				facesTriangle = null;
//			}
//
//			if (textureFaces != null) {
//				for (int[] x : textureFaces) {
//					if (x != null && x.length == 4) {
//						int[] t1 = new int[3];
//						int[] t2 = new int[3];
//						t1[0] = x[0];
//						t1[1] = x[1];
//						t1[2] = x[3];
//						t2[0] = x[1];
//						t2[1] = x[2];
//						t2[2] = x[3];
//						textureFacesTriangle.add(t1);
//						textureFacesTriangle.add(t2);
//					} else {
//						textureFacesTriangle.add(x);
//					}
//				}
//			} else {
//				textureFacesTriangle = null;
//			}
//
//			if (normalFaces != null) {
//				for (int[] x : normalFaces) {
//					if (x != null && x.length == 4) {
//						int[] t1 = new int[3];
//						int[] t2 = new int[3];
//						t1[0] = x[0];
//						t1[1] = x[1];
//						t1[2] = x[3];
//						t2[0] = x[1];
//						t2[1] = x[2];
//						t2[2] = x[3];
//						normalFacesTriangle.add(t1);
//						normalFacesTriangle.add(t2);
//					} else {
//						normalFacesTriangle.add(x);
//					}
//				}
//			} else {
//				normalFacesTriangle = null;
//			}

				float[] dataMin = new float[4];
				dataMin[0] = Float.POSITIVE_INFINITY;
				dataMin[1] = Float.POSITIVE_INFINITY;
				dataMin[2] = Float.POSITIVE_INFINITY;
				dataMin[3] = 0;

				float[] dataMax = new float[4];
				dataMax[0] = Float.NEGATIVE_INFINITY;
				dataMax[1] = Float.NEGATIVE_INFINITY;
				dataMax[2] = Float.NEGATIVE_INFINITY;
				dataMax[3] = 0;

				for (Vector v : vertex) {
					if (v.getDataElement(0) < dataMin[0]) {
						dataMin[0] = v.getDataElement(0);
					}
					if (v.getDataElement(1) < dataMin[1]) {
						dataMin[1] = v.getDataElement(1);
					}
					if (v.getDataElement(2) < dataMin[2]) {
						dataMin[2] = v.getDataElement(2);
					}

					if (v.getDataElement(0) > dataMax[0]) {
						dataMax[0] = v.getDataElement(0);
					}
					if (v.getDataElement(1) > dataMax[1]) {
						dataMax[1] = v.getDataElement(1);
					}
					if (v.getDataElement(2) > dataMax[2]) {
						dataMax[2] = v.getDataElement(2);
					}
				}

				Vector min = new Vector(dataMin);
				Vector max = new Vector(dataMax);

				Vector change = ((max.sub(min)).scalarMul(0.5f)).add(min);

				Vector[] vertArr = new Vector[vertex.size()];
				for (int i = 0; i < vertArr.length; i++) {
					vertArr[i] = vertex.get(i).sub(change);
				}
				
				Vector[] vnArray = new Vector[vn.size()];
				for(int i = 0; i < vnArray.length; i++) {
					vnArray[i] = vn.get(i);
				}
				
				Vector[] vtArray = new Vector[vt.size()];
				for(int i = 0; i < vtArray.length; i++) {
					vtArray[i] = vt.get(i);
				}

				res = new Model(vertArr, faces, vtArray, textureFaces, vnArray, normalFaces);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return res;
	}
	
	public static boolean isVertexInsideTriangle(Vector v0, Vector v1, Vector v2, Vector p) {
		Vector e1 = v0.sub(v1);
		Vector e2 = v2.sub(v1);
		Vector proj1 = e1.normalise().scalarMul(((e1).dot(p))/e1.getNorm());
		Vector proj2 = e2.normalise().scalarMul(((e1).dot(p))/e2.getNorm());
		Vector p_ = proj1.add(proj2);
		
		float pa = (v0.sub(p_)).getNorm();
		float pb = (v1.sub(p_)).getNorm();
		float pc = (v2.sub(p_)).getNorm();
		float totalArea = e1.getNorm() * e2.getNorm() /2.0f;
		
		float alpha = pa * pb / 2.0f;
		float beta = pb * pc / 2.0f;
		float gamma = pc * pa / 2.0f;
		
		if(alpha >= 0 && alpha <= 1 && beta >= 0 && beta <= 1 && gamma >= 0 && gamma <= 1 && alpha + beta + gamma == 1) {
			return true;
		}
		else {
			return false;
		}
		
	}

	public static Model buildGrid(int w, int d) {

		Vector[][] verts = new Vector[w][d];
		List<int[]> cons = new ArrayList<int[]>();

		for (int i = -w / 2; i < w / 2; i++) {
			for (int k = -d / 2; k < d / 2; k++) {
				verts[i + w / 2][k + d / 2] = new Vector(new float[] { i, 0, k, 1 });
			}
		}

		for (int i = 0; i < w; i++) {
			for (int j = 0; j < (d - 1); j++) {
				cons.add(new int[] { (i * w) + j, (i * w) + (j + 1) });
			}
		}

		for (int j = 0; j < d; j++) {
			for (int i = 0; i < w - 1; i++) {
				cons.add(new int[] { (i * w) + j, ((i + 1) * w) + (j) });
			}
		}

//		for (int i = 0; i < connections.length; i++) {
//			connections[i] = new Vector(new float[] { i });
//		}

		Vector[] vertices = new Vector[verts.length * verts[0].length];

		for (int i = 0; i < verts.length; i++) {
			for (int j = 0; j < verts[i].length; j++) {
				vertices[i * w + j] = verts[i][j];
			}
		}

		return new Model(vertices, cons, null, null, null, null);
	}

	public static Model buildShip() {
		double[][] vert = new double[][] { { -2.5703, 0.78053, -2.4e-05 }, { -0.89264, 0.022582, 0.018577 },
				{ 1.6878, -0.017131, 0.022032 }, { 3.4659, 0.025667, 0.018577 }, { -2.5703, 0.78969, -0.001202 },
				{ -0.89264, 0.25121, 0.93573 }, { 1.6878, 0.25121, 1.1097 }, { 3.5031, 0.25293, 0.93573 },
				{ -2.5703, 1.0558, -0.001347 }, { -0.89264, 1.0558, 1.0487 }, { 1.6878, 1.0558, 1.2437 },
				{ 3.6342, 1.0527, 1.0487 }, { -2.5703, 1.0558, 0 }, { -0.89264, 1.0558, 0 }, { 1.6878, 1.0558, 0 },
				{ 3.6342, 1.0527, 0 }, { -2.5703, 1.0558, 0.001347 }, { -0.89264, 1.0558, -1.0487 },
				{ 1.6878, 1.0558, -1.2437 }, { 3.6342, 1.0527, -1.0487 }, { -2.5703, 0.78969, 0.001202 },
				{ -0.89264, 0.25121, -0.93573 }, { 1.6878, 0.25121, -1.1097 }, { 3.5031, 0.25293, -0.93573 },
				{ 3.5031, 0.25293, 0 }, { -2.5703, 0.78969, 0 }, { 1.1091, 1.2179, 0 }, { 1.145, 6.617, 0 },
				{ 4.0878, 1.2383, 0 }, { -2.5693, 1.1771, -0.081683 }, { 0.98353, 6.4948, -0.081683 },
				{ -0.72112, 1.1364, -0.081683 }, { 0.9297, 6.454, 0 }, { -0.7929, 1.279, 0 }, { 0.91176, 1.2994, 0 } };

		Vector[] vertices = new Vector[vert.length];

		for (int i = 0; i < vert.length; i++) {
			Vector v = new Vector(new float[] { (float) vert[i][0], (float) (vert[i][1]), (float) vert[i][2] });
			vertices[i] = v;
		}

		int[] connections = new int[] { 4, 0, 5, 0, 1, 5, 1, 2, 5, 5, 2, 6, 3, 7, 2, 2, 7, 6, 5, 9, 4, 4, 9, 8, 5, 6, 9,
				9, 6, 10, 7, 11, 6, 6, 11, 10, 9, 13, 8, 8, 13, 12, 10, 14, 9, 9, 14, 13, 10, 11, 14, 14, 11, 15, 17,
				16, 13, 12, 13, 16, 13, 14, 17, 17, 14, 18, 15, 19, 14, 14, 19, 18, 16, 17, 20, 20, 17, 21, 18, 22, 17,
				17, 22, 21, 18, 19, 22, 22, 19, 23, 20, 21, 0, 21, 1, 0, 22, 2, 21, 21, 2, 1, 22, 23, 2, 2, 23, 3, 3,
				23, 24, 3, 24, 7, 24, 23, 15, 15, 23, 19, 24, 15, 7, 7, 15, 11, 0, 25, 20, 0, 4, 25, 20, 25, 16, 16, 25,
				12, 25, 4, 12, 12, 4, 8, 26, 27, 28, 29, 30, 31, 32, 34, 33 };

//		Vector[] cons = new Vector[connections.length / 3];
//
//		for (int i = 0; i < cons.length; i++) {
//			cons[i] = new Vector(new float[] { connections[i * 3], connections[i * 3 + 1], connections[i * 3 + 2] });
//		}

		List<int[]> cons = new ArrayList<int[]>();
		for (int i = 0; i < connections.length / 3; i++) {
			cons.add(new int[] { connections[i * 3], connections[i * 3 + 1], connections[i * 3 + 2] });
//			cons[i] = new Vector(new float[] { connections[i * 3], connections[i * 3 + 1], connections[i * 3 + 2] });
		}

		return new Model(vertices, cons, null, null, null, null);
	}

	public static Model buildTree() {
		double[][] vert = new double[][] { { 0, 39.034, 0 }, { 0.76212, 36.843, 0 }, { 3, 36.604, 0 }, { 1, 35.604, 0 },
				{ 2.0162, 33.382, 0 }, { 0, 34.541, 0 }, { -2.0162, 33.382, 0 }, { -1, 35.604, 0 }, { -3, 36.604, 0 },
				{ -0.76212, 36.843, 0 }, { -0.040181, 34.31, 0 }, { 3.2778, 30.464, 0 }, { -0.040181, 30.464, 0 },
				{ -0.028749, 30.464, 0 }, { 3.2778, 30.464, 0 }, { 1.2722, 29.197, 0 }, { 1.2722, 29.197, 0 },
				{ -0.028703, 29.197, 0 }, { 1.2722, 29.197, 0 }, { 5.2778, 25.398, 0 }, { -0.02865, 25.398, 0 },
				{ 1.2722, 29.197, 0 }, { 5.2778, 25.398, 0 }, { 3.3322, 24.099, 0 }, { -0.028683, 24.099, 0 },
				{ 7.1957, 20.299, 0 }, { -0.02861, 20.299, 0 }, { 5.2778, 19.065, 0 }, { -0.028663, 18.984, 0 },
				{ 9.2778, 15.265, 0 }, { -0.028571, 15.185, 0 }, { 9.2778, 15.265, 0 }, { 7.3772, 13.999, 0 },
				{ -0.028625, 13.901, 0 }, { 9.2778, 15.265, 0 }, { 12.278, 8.9323, 0 }, { -0.028771, 8.9742, 0 },
				{ 12.278, 8.9323, 0 }, { 10.278, 7.6657, 0 }, { -0.028592, 7.6552, 0 }, { 15.278, 2.5994, 0 },
				{ -0.028775, 2.6077, 0 }, { 15.278, 2.5994, 0 }, { 13.278, 1.3329, 0 }, { -0.028727, 1.2617, 0 },
				{ 18.278, -3.7334, 0 }, { 18.278, -3.7334, 0 }, { 2.2722, -1.2003, 0 }, { -0.028727, -1.3098, 0 },
				{ 4.2722, -5, 0 }, { 4.2722, -5, 0 }, { -0.028727, -5, 0 }, { -3.3582, 30.464, 0 },
				{ -3.3582, 30.464, 0 }, { -1.3526, 29.197, 0 }, { -1.3526, 29.197, 0 }, { -1.3526, 29.197, 0 },
				{ -5.3582, 25.398, 0 }, { -1.3526, 29.197, 0 }, { -5.3582, 25.398, 0 }, { -3.4126, 24.099, 0 },
				{ -7.276, 20.299, 0 }, { -5.3582, 19.065, 0 }, { -9.3582, 15.265, 0 }, { -9.3582, 15.265, 0 },
				{ -7.4575, 13.999, 0 }, { -9.3582, 15.265, 0 }, { -12.358, 8.9323, 0 }, { -12.358, 8.9323, 0 },
				{ -10.358, 7.6657, 0 }, { -15.358, 2.5994, 0 }, { -15.358, 2.5994, 0 }, { -13.358, 1.3329, 0 },
				{ -18.358, -3.7334, 0 }, { -18.358, -3.7334, 0 }, { -2.3526, -1.2003, 0 }, { -4.3526, -5, 0 },
				{ -4.3526, -5, 0 }, { 0, 34.31, 0.040181 }, { 0, 30.464, -3.2778 }, { 0, 30.464, 0.040181 },
				{ 0, 30.464, 0.028749 }, { 0, 30.464, -3.2778 }, { 0, 29.197, -1.2722 }, { 0, 29.197, -1.2722 },
				{ 0, 29.197, 0.028703 }, { 0, 29.197, -1.2722 }, { 0, 25.398, -5.2778 }, { 0, 25.398, 0.02865 },
				{ 0, 29.197, -1.2722 }, { 0, 25.398, -5.2778 }, { 0, 24.099, -3.3322 }, { 0, 24.099, 0.028683 },
				{ 0, 20.299, -7.1957 }, { 0, 20.299, 0.02861 }, { 0, 19.065, -5.2778 }, { 0, 18.984, 0.028663 },
				{ 0, 15.265, -9.2778 }, { 0, 15.185, 0.028571 }, { 0, 15.265, -9.2778 }, { 0, 13.999, -7.3772 },
				{ 0, 13.901, 0.028625 }, { 0, 15.265, -9.2778 }, { 0, 8.9323, -12.278 }, { 0, 8.9742, 0.028771 },
				{ 0, 8.9323, -12.278 }, { 0, 7.6657, -10.278 }, { 0, 7.6552, 0.028592 }, { 0, 2.5994, -15.278 },
				{ 0, 2.6077, 0.028775 }, { 0, 2.5994, -15.278 }, { 0, 1.3329, -13.278 }, { 0, 1.2617, 0.028727 },
				{ 0, -3.7334, -18.278 }, { 0, -3.7334, -18.278 }, { 0, -1.2003, -2.2722 }, { 0, -1.3098, 0.028727 },
				{ 0, -5, -4.2722 }, { 0, -5, -4.2722 }, { 0, -5, 0.028727 }, { 0, 30.464, 3.3582 },
				{ 0, 30.464, 3.3582 }, { 0, 29.197, 1.3526 }, { 0, 29.197, 1.3526 }, { 0, 29.197, 1.3526 },
				{ 0, 25.398, 5.3582 }, { 0, 29.197, 1.3526 }, { 0, 25.398, 5.3582 }, { 0, 24.099, 3.4126 },
				{ 0, 20.299, 7.276 }, { 0, 19.065, 5.3582 }, { 0, 15.265, 9.3582 }, { 0, 15.265, 9.3582 },
				{ 0, 13.999, 7.4575 }, { 0, 15.265, 9.3582 }, { 0, 8.9323, 12.358 }, { 0, 8.9323, 12.358 },
				{ 0, 7.6657, 10.358 }, { 0, 2.5994, 15.358 }, { 0, 2.5994, 15.358 }, { 0, 1.3329, 13.358 },
				{ 0, -3.7334, 18.358 }, { 0, -3.7334, 18.358 }, { 0, -1.2003, 2.3526 }, { 0, -5, 4.3526 },
				{ 0, -5, 4.3526 } };

		Vector[] vertices = new Vector[vert.length];

		for (int i = 0; i < vert.length; i++) {
			Vector v = new Vector(new float[] { (float) vert[i][0], (float) (vert[i][1]), (float) vert[i][2] });
			vertices[i] = v;
		}

		int[] connections = new int[] { 8, 7, 9, 6, 5, 7, 4, 3, 5, 2, 1, 3, 0, 9, 1, 5, 3, 7, 7, 3, 9, 9, 3, 1, 10, 12,
				11, 13, 15, 14, 15, 13, 16, 13, 17, 16, 18, 20, 19, 17, 20, 21, 20, 23, 22, 20, 24, 23, 23, 26, 25, 24,
				26, 23, 26, 27, 25, 26, 28, 27, 27, 30, 29, 28, 30, 27, 30, 32, 31, 30, 33, 32, 27, 30, 34, 32, 36, 35,
				33, 36, 32, 36, 38, 37, 36, 39, 38, 38, 41, 40, 39, 41, 38, 41, 43, 42, 41, 44, 43, 44, 45, 43, 44, 47,
				46, 44, 48, 47, 48, 49, 47, 48, 51, 50, 10, 52, 12, 13, 53, 54, 55, 17, 54, 13, 54, 17, 56, 57, 20, 17,
				58, 20, 20, 59, 60, 20, 60, 24, 60, 61, 26, 24, 60, 26, 26, 61, 62, 26, 62, 28, 62, 63, 30, 28, 62, 30,
				30, 64, 65, 30, 65, 33, 62, 66, 30, 65, 67, 36, 33, 65, 36, 36, 68, 69, 36, 69, 39, 69, 70, 41, 39, 69,
				41, 41, 71, 72, 41, 72, 44, 44, 72, 73, 44, 74, 75, 44, 75, 48, 48, 75, 76, 48, 77, 51, 78, 80, 79, 81,
				83, 82, 83, 81, 84, 81, 85, 84, 86, 88, 87, 85, 88, 89, 88, 91, 90, 88, 92, 91, 91, 94, 93, 92, 94, 91,
				94, 95, 93, 94, 96, 95, 95, 98, 97, 96, 98, 95, 98, 100, 99, 98, 101, 100, 95, 98, 102, 100, 104, 103,
				101, 104, 100, 104, 106, 105, 104, 107, 106, 106, 109, 108, 107, 109, 106, 109, 111, 110, 109, 112, 111,
				112, 113, 111, 112, 115, 114, 112, 116, 115, 116, 117, 115, 116, 119, 118, 78, 120, 80, 81, 121, 122,
				123, 85, 122, 81, 122, 85, 124, 125, 88, 85, 126, 88, 88, 127, 128, 88, 128, 92, 128, 129, 94, 92, 128,
				94, 94, 129, 130, 94, 130, 96, 130, 131, 98, 96, 130, 98, 98, 132, 133, 98, 133, 101, 130, 134, 98, 133,
				135, 104, 101, 133, 104, 104, 136, 137, 104, 137, 107, 137, 138, 109, 107, 137, 109, 109, 139, 140, 109,
				140, 112, 112, 140, 141, 112, 142, 143, 112, 143, 116, 116, 143, 144, 116, 145, 119 };

//		Vector[] cons = new Vector[connections.length / 3];
		List<int[]> cons = new ArrayList<int[]>();

		for (int i = 0; i < connections.length / 3; i++) {
			cons.add(new int[] { connections[i * 3], connections[i * 3 + 1], connections[i * 3 + 2] });
//			cons[i] = new Vector(new float[] { connections[i * 3], connections[i * 3 + 1], connections[i * 3 + 2] });
		}

		return new Model(vertices, cons, null, null, null, null);
	}

}
