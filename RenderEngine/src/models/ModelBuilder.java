package models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Math.Utils;
import Math.Vector;
import models.DataStructure.Face;
import models.DataStructure.Mesh;
import models.DataStructure.Vertex;

public class ModelBuilder {

	public static Model buildModelFromFile(String loc) {

		ModelBuilder m = new ModelBuilder();
		InputStream url = m.getClass().getResourceAsStream("/models/resources/"+loc);

		Model res = null;
		List<Vector> vertex = new ArrayList<Vector>();
		List<Vector> vn = new ArrayList<Vector>();
		List<Vector> vt = new ArrayList<Vector>();
		List<int[]> faces = new ArrayList<>();
		List<int[]> textureFaces = new ArrayList<>();
		List<int[]> normalFaces = new ArrayList<>();

		// noinspection StringOperationCanBeSimplified
		if (loc.substring(loc.length() - 3, loc.length()).equalsIgnoreCase("obj")) {

			try {
				String line = "";
				BufferedReader br = new BufferedReader(new InputStreamReader(url));

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

					}
				}

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
					if (v.get(0) < dataMin[0]) {
						dataMin[0] = v.get(0);
					}
					if (v.get(1) < dataMin[1]) {
						dataMin[1] = v.get(1);
					}
					if (v.get(2) < dataMin[2]) {
						dataMin[2] = v.get(2);
					}

					if (v.get(0) > dataMax[0]) {
						dataMax[0] = v.get(0);
					}
					if (v.get(1) > dataMax[1]) {
						dataMax[1] = v.get(1);
					}
					if (v.get(2) > dataMax[2]) {
						dataMax[2] = v.get(2);
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
				for (int i = 0; i < vnArray.length; i++) {
					vnArray[i] = vn.get(i);
				}

				Vector[] vtArray = new Vector[vt.size()];
				for (int i = 0; i < vtArray.length; i++) {
					vtArray[i] = vt.get(i);
				}

				List<Face> facesListObj = new ArrayList<>(faces.size());
				List<Vertex> tempVertList;
				List<Integer> tempVertAttributesList;

				for(int i = 0;i < faces.size();i++) {

					tempVertList = new ArrayList<>(faces.get(i).length);

					for(int j = 0;j < faces.get(i).length;j++) {
						tempVertAttributesList = new ArrayList<>(3);
						tempVertAttributesList.add(Vertex.POSITION,faces.get(i)[j]);
						if(vtArray != null) {
							if(textureFaces.get(i) != null) {
								tempVertAttributesList.add(Vertex.TEXTURE,textureFaces.get(i)[j]);
							}
						}
						if(vnArray != null) {
							if(normalFaces.get(i) != null) {
								tempVertAttributesList.add(Vertex.NORMAL,normalFaces.get(i)[j]);
							}
						}
						((ArrayList<Integer>)tempVertAttributesList).trimToSize();	//Trims arraylist to data size
						tempVertList.add(new Vertex(tempVertAttributesList));
					}
					facesListObj.add(new Face(tempVertList));
				}

				List<List<Vector>> vertAttributes = new ArrayList<>(3);
				vertAttributes.add(Mesh.POSITION,Arrays.asList(vertArr));
				vertAttributes.add(Mesh.TEXTURE, Arrays.asList(vtArray));
				vertAttributes.add(Mesh.NORMAL, Arrays.asList(vnArray));

				Mesh resMesh = new Mesh(facesListObj,vertAttributes);

				res = new Model(resMesh);
//				res = new Model(vertArr, faces, vtArray, textureFaces, vnArray, normalFaces);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return res;
	}

	public static List<Object> triangulate(Vector[] vertices, List<int[]> faces,
			List<int[]> textureFaces, List<int[]> normalFaces, boolean forceUseEarClipping) {

		List<int[]> facesTriangulated = new ArrayList<>();
		List<int[]> textureFacesTriangulated = new ArrayList<>();
		List<int[]> normalFacesTriangulated = new ArrayList<>();

		for (int count = 0; count < faces.size(); count++) {
			int[] x = faces.get(count);
			if (x != null && x.length > 3) {

				if (x.length == 4 && !forceUseEarClipping) {
					int[] t1 = new int[3];
					int[] t2 = new int[3];
					t1[0] = x[0];
					t1[1] = x[1];
					t1[2] = x[3];
					t2[0] = x[1];
					t2[1] = x[2];
					t2[2] = x[3];
					facesTriangulated.add(t1);
					facesTriangulated.add(t2);

					if (textureFaces != null) {
						int[] y = textureFaces.get(count);
						if (y != null) {
							int[] tex1 = new int[3];
							int[] tex2 = new int[3];

							tex1[0] = y[0];
							tex1[1] = y[1];
							tex1[2] = y[3];
							tex2[0] = y[1];
							tex2[1] = y[2];
							tex2[2] = y[3];
							textureFacesTriangulated.add(tex1);
							textureFacesTriangulated.add(tex2);
						}
					}

					if (normalFaces != null) {
						int[] z = normalFaces.get(count);
						if (z != null) {
							int[] norm1 = new int[3];
							int[] norm2 = new int[3];

							norm1[0] = z[0];
							norm1[1] = z[1];
							norm1[2] = z[3];
							norm2[0] = z[1];
							norm2[1] = z[2];
							norm2[2] = z[3];
							normalFacesTriangulated.add(norm1);
							normalFacesTriangulated.add(norm2);
						}
					}
				} else {
					List<Integer> reflex = new ArrayList<>();
					List<Integer> convex = new ArrayList<>();
					List<Integer> ears = new ArrayList<>();
					List<Integer> vertsLeft = new ArrayList<>();
					List<Integer> totalVerts = new ArrayList<>();
					List<Integer> totalTextures = new ArrayList<>();
					List<Integer> totalNormals = new ArrayList<>();
					
					int countI = 0;
					for (int t : x) {
						totalVerts.add(t);
						vertsLeft.add(x[countI]);
						countI++;
					}

					if (textureFaces != null) {
						if (textureFaces.get(count) != null) {
							for (int t : textureFaces.get(count)) {
								totalTextures.add(t);
							}
						} else {
							totalTextures = null;
						}
					}

					if (normalFaces != null) {
						if (normalFaces.get(count) != null) {
							for (int t : normalFaces.get(count)) {
								totalNormals.add(t);
							}
						} else {
							totalNormals = null;
						}
					}

//					for (int i = 0; i < x.length; i++) {
//						vertsLeft.add(x[i]);
//					}

//				Calculating the internal angle of each vertex and adds them to either the reflex or concave list 
					for (int i = 0; i < x.length; i++) {
						int v0, vi, v2;
						int currentVert = x[i];
						int indexInVertsLeftList = vertsLeft.indexOf(currentVert);
						
						if (indexInVertsLeftList == 0) {
							v0 = vertsLeft.get(vertsLeft.size() - 1);
							vi = currentVert;
							v2 = vertsLeft.get(indexInVertsLeftList + 1);
						} else if (indexInVertsLeftList == vertsLeft.size() - 1) {
							v0 = vertsLeft.get(indexInVertsLeftList - 1);
							vi = currentVert;
							v2 = vertsLeft.get(0);
						} else {
							v0 = vertsLeft.get(indexInVertsLeftList - 1);
							vi = currentVert;
							v2 = vertsLeft.get(indexInVertsLeftList + 1);
						}
						
						float angle = (vertices[v0].sub(vertices[vi]))
								.getAngleBetweenVectors(vertices[v2].sub(vertices[vi]));
						if (angle < 0) {
							angle = 180 - angle;
						}
						if (angle > 180) {
							reflex.add(currentVert);
						} else if (angle < 180) {
							convex.add(currentVert);
						}
					}

//				Calculating which convex vertices are ears
					for (int i = 0; i < convex.size(); i++) {
						int v0, vi, v2;
						int currentVert = x[i];
						int indexInVertsLeftList = vertsLeft.indexOf(currentVert);

						if (indexInVertsLeftList == 0) {
							v0 = vertsLeft.get(vertsLeft.size() - 1);
							vi = currentVert;
							v2 = vertsLeft.get(indexInVertsLeftList + 1);
						} else if (indexInVertsLeftList == vertsLeft.size() - 1) {
							v0 = vertsLeft.get(indexInVertsLeftList - 1);
							vi = currentVert;
							v2 = vertsLeft.get(0);
						} else {
							v0 = vertsLeft.get(indexInVertsLeftList - 1);
							vi = currentVert;
							v2 = vertsLeft.get(indexInVertsLeftList + 1);
						}

						boolean isEar = true;
						for (int j = 0; j < reflex.size(); j++) {
							if (Utils.isPointInsideTriangle(vertices[v0], vertices[vi], vertices[v2],vertices[reflex.get(j)],true)) {
								isEar = false;
							}
						}
						if (isEar)
							ears.add(convex.get(i));
					}

					for (int i = 0; i < ears.size(); i++) {

						if (vertsLeft.size() == 3) {
							int[] temp = new int[] { vertsLeft.get(0), vertsLeft.get(1), vertsLeft.get(2) };
							facesTriangulated.add(temp);
							break;
						}

						int earVertex = ears.get(i);
						int v0, vi, v2;
						int earIndexInVertsLeftList = vertsLeft.indexOf(earVertex);

						if (earIndexInVertsLeftList == 0) {
							v0 = vertsLeft.get(vertsLeft.size() - 1);
							vi = earVertex;
							v2 = vertsLeft.get(earIndexInVertsLeftList + 1);
						} else if (earIndexInVertsLeftList == vertsLeft.size() - 1) {
							v0 = vertsLeft.get(earIndexInVertsLeftList - 1);
							vi = earVertex;
							v2 = vertsLeft.get(0);
						} else {
							v0 = vertsLeft.get(earIndexInVertsLeftList - 1);
							vi = earVertex;
							v2 = vertsLeft.get(earIndexInVertsLeftList + 1);
						}

						int[] temp = new int[] { v0, vi, v2 };
						facesTriangulated.add(temp);

						int ind0 = totalVerts.indexOf(v0);
						int ind1 = totalVerts.indexOf(vi);
						int ind2 = totalVerts.indexOf(v2);

						if (totalTextures != null) {
							textureFacesTriangulated.add(new int[] { totalTextures.get(ind0), totalTextures.get(ind1),
									totalTextures.get(ind2) });
						}
						if (totalNormals != null) {
							normalFacesTriangulated.add(new int[] { totalNormals.get(ind0), totalNormals.get(ind1),
									totalNormals.get(ind2) });
						}

						ears.remove((Integer)earVertex);
						i--;
						vertsLeft.remove(earIndexInVertsLeftList);

						if (reflex.indexOf(v0) != -1) {
							int indexInVertsLeftList = vertsLeft.indexOf(v0);
							int r0, ri, r2;
							if (indexInVertsLeftList == 0) {
								r0 = vertsLeft.get(vertsLeft.size() - 1);
								ri = v0;
								r2 = vertsLeft.get(indexInVertsLeftList + 1);
							} else if (indexInVertsLeftList == vertsLeft.size() - 1) {
								r0 = vertsLeft.get(indexInVertsLeftList - 1);
								ri = v0;
								r2 = vertsLeft.get(0);
							} else {
								r0 = vertsLeft.get(indexInVertsLeftList - 1);
								ri = v0;
								r2 = vertsLeft.get(indexInVertsLeftList + 1);
							}
							float angle = (vertices[r0].sub(vertices[ri]))
									.getAngleBetweenVectors(vertices[r2].sub(vertices[ri]));
							if (angle < 180) {
								convex.add(v0);
								reflex.remove((Integer) v0);
							}

						}

						if (reflex.indexOf(v2) != -1) {
							int indexAtVertsLeftList = vertsLeft.indexOf(v2);
							int r0, ri, r2;
							if (indexAtVertsLeftList == 0) {
								r0 = vertsLeft.get(vertsLeft.size() - 1);
								ri = v2;
								r2 = vertsLeft.get(indexAtVertsLeftList + 1);
							} else if (indexAtVertsLeftList == vertsLeft.size() - 1) {
								r0 = vertsLeft.get(indexAtVertsLeftList - 1);
								ri = v2;
								r2 = vertsLeft.get(0);
							} else {
								r0 = vertsLeft.get(indexAtVertsLeftList - 1);
								ri = v2;
								r2 = vertsLeft.get(indexAtVertsLeftList + 1);
							}
							float angle = (vertices[r0].sub(vertices[ri]))
									.getAngleBetweenVectors(vertices[r2].sub(vertices[ri]));
							if (angle < 180) {
								convex.add(v2);
								reflex.remove((Integer) v2);
							}

						}

						if (convex.indexOf(v0) != -1) {
							int indexArVertsLeftList = vertsLeft.indexOf(v0);
							int c0, ci, c2;
							if (indexArVertsLeftList == 0) {
								c0 = vertsLeft.get(vertsLeft.size() - 1);
								ci = v0;
								c2 = vertsLeft.get(indexArVertsLeftList + 1);
							} else if (indexArVertsLeftList == vertsLeft.size() - 1) {
								c0 = vertsLeft.get(indexArVertsLeftList - 1);
								ci = v0;
								c2 = vertsLeft.get(0);
							} else {
								c0 = vertsLeft.get(indexArVertsLeftList - 1);
								ci = v0;
								c2 = vertsLeft.get(indexArVertsLeftList + 1);
							}

							boolean isEar = true;
							for (int k = 0; k < reflex.size(); k++) {
//								if (Utils.isPointInsideTriangle(vertices[c0], vertices[ci], vertices[c2],
//										vertices[reflex.get(k)])) {
								if (Utils.isPointInsideTriangle(vertices[c0], vertices[ci], vertices[c2],vertices[reflex.get(k)],true)) {
									isEar = false;
								}
							}
							if (isEar) {
								ears.add(i + 1, convex.get(convex.indexOf(v0)));
							}

						}

						if (convex.indexOf(v2) != -1) {
							int indexInVertsLeftList = vertsLeft.indexOf(v2);
							int c0, ci, c2;
							if (indexInVertsLeftList == 0) {
								c0 = vertsLeft.get(vertsLeft.size() - 1);
								ci = v2;
								c2 = vertsLeft.get(indexInVertsLeftList + 1);
							} else if (indexInVertsLeftList == vertsLeft.size() - 1) {
								c0 = vertsLeft.get(indexInVertsLeftList - 1);
								ci = v2;
								c2 = vertsLeft.get(0);
							} else {
								c0 = vertsLeft.get(indexInVertsLeftList - 1);
								ci = v2;
								c2 = vertsLeft.get(indexInVertsLeftList + 1);
							}

							boolean isEar = true;
							for (int k = 0; k < reflex.size(); k++) {
								if (Utils.isPointInsideTriangle(vertices[c0], vertices[ci], vertices[c2],vertices[reflex.get(k)],true)) {
									isEar = false;
								}
							}
							if (isEar)
								ears.add(i + 1, convex.get(convex.indexOf(v2)));
						}

					}
				}

			} else {
				facesTriangulated.add(x);
				textureFacesTriangulated.add(textureFaces.get(count));
				normalFacesTriangulated.add(normalFaces.get(count));
			}
		}

		List<Object> res = new ArrayList<>();
		res.add(facesTriangulated);
		res.add(textureFacesTriangulated);
		res.add(normalFacesTriangulated);
		return res;

	}

	public static boolean isPointInsideTriangle(Vector v0, Vector v1, Vector v2, Vector p) {
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

	public static Model buildGrid(int w, int d) {

		Vector[][] verts = new Vector[w][d];
		List<int[]> cons = new ArrayList<>();

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

		Vector[] vertices = new Vector[verts.length * verts[0].length];

		for (int i = 0; i < verts.length; i++) {
			for (int j = 0; j < verts[i].length; j++) {
				vertices[i * w + j] = verts[i][j];
			}
		}

		List<Face> facesListObj = new ArrayList<>(cons.size());
		List<Vertex> tempVertList;
		Mesh resMesh;

		for(int i = 0;i < cons.size();i++) {
			tempVertList = new ArrayList<>(cons.get(i).length);
			for(int j = 0;j < cons.get(i).length;j++) {
				tempVertList.add(new Vertex(cons.get(i)[j]));
			}
			facesListObj.add(new Face(tempVertList));
		}

		List<List<Vector>> vertAttributes = new ArrayList<>(1);
		vertAttributes.add(Mesh.POSITION, Arrays.asList(vertices));

		resMesh = new Mesh(facesListObj, vertAttributes);
		return new Model(resMesh);

//		return new Model(vertices, cons, null, null, null, null);
	}

}
