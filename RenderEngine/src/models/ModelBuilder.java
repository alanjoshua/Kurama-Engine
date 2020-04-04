package models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import Math.Vector;
import Math.Utils;
import models.DataStructure.LinkedList.CircularDoublyLinkedList;
import models.DataStructure.LinkedList.DoublyLinkedList;
import models.DataStructure.LinkedList.Node;
import models.DataStructure.Mesh.Face;
import models.DataStructure.Mesh.Mesh;
import models.DataStructure.Mesh.MeshLWJGL;
import models.DataStructure.Mesh.Vertex;

public class ModelBuilder {

	public static Model buildModelFromFile(String loc,Map<String,Mesh> meshInstances) {
		Model res;
		Mesh resMesh = meshInstances.get(loc);

		if(resMesh != null) {
			return new models.Model(resMesh,loc);
		}
		else {
			resMesh = loadRawData(loc);
			resMesh = triangulate(resMesh,false);

			meshInstances.put(loc,resMesh);

			res = new Model(resMesh,loc);
			return res;
		}
	}

	public static models.ModelLWJGL buildModelLWJGLFromFile(String loc, Map<String,MeshLWJGL> meshInstances) {

		models.ModelLWJGL res;
		MeshLWJGL resMesh = meshInstances.get(loc);

		if(resMesh != null) {
			return new models.ModelLWJGL(resMesh,loc);
		}
		else {
			Mesh temp = loadRawData(loc);
			temp = triangulate(temp,false);
			resMesh = restructureMeshToSingleIndexList(temp);

			resMesh.initOpenGLMeshData();
			meshInstances.put(loc,resMesh);

			res = new models.ModelLWJGL(resMesh,loc);
			return res;
		}
	}

	public static MeshLWJGL restructureMeshToSingleIndexList(Mesh mesh) {

		List<Integer> indexList = new ArrayList<>();
		List<List<Vector>> newVertAttribs = new ArrayList<>(mesh.vertAttributes.size());
		List<Vertex> uniqueVertices = new ArrayList<>();
//		int totalVerts = 0;

		for(int i = 0;i < mesh.vertAttributes.size();i++) {
			newVertAttribs.add(new ArrayList<>());
		}

		for(Face f:mesh.faces) {
			for(Vertex v: f.vertices) {
//				totalVerts++;
				int vInd = uniqueVertices.indexOf(v);
//				uniqueVertices.add(v);
				if(uniqueVertices.size() == 0 || vInd < 0) {
					uniqueVertices.add(v);
					indexList.add(uniqueVertices.size() - 1);
					for(int i =0;i < newVertAttribs.size();i++) {
						Integer ind = v.getAttribute(i);
						if(ind!=null) {
							newVertAttribs.get(i).add(mesh.getAttributeList(i).get(ind));
						}
						else {
							newVertAttribs.get(i).add(null);
						}
					}

				}
				else {
					indexList.add(vInd);
				}
			}
		}

//		Integer[] tempIndices = new Integer[totalVerts];
//
//		List<Vertex> uniqueList = new ArrayList<Vertex>(uniqueVertices);
//		int uvCounter = 0;
//		for(Vertex uv:uniqueList) {
//			int vertCounter = 0;
//			for(Face f: mesh.faces) {
//				for(Vertex v:f.vertices) {
//					if(uv.equals(v)) {
//						tempIndices[vertCounter] = uvCounter;
//					}
//					vertCounter++;
//				}
//			}
//			uvCounter++;
//		}
//
//		indexList = new ArrayList<Integer>(Arrays.asList(tempIndices));

		for(Face f:mesh.faces) {
			for(Vertex v: f.vertices) {
//				int ind = temp.get(v.toString());
				indexList.add(uniqueVertices.indexOf(v));
//				System.out.println(v.toString());
			}
		}

		return new MeshLWJGL(indexList,newVertAttribs);
	}

	public static Mesh loadRawData(String loc) {

		ModelBuilder m = new ModelBuilder();
		InputStream url = m.getClass().getResourceAsStream("/models/resources/" + loc);

		Mesh resMesh = null;

		List<Vector> vertex = new ArrayList<Vector>();
		List<Vector> vn = new ArrayList<Vector>();
		List<Vector> vt = new ArrayList<Vector>();
		List<int[]> faces = new ArrayList<>();
		List<int[]> textureFaces = new ArrayList<>();
		List<int[]> normalFaces = new ArrayList<>();

		// noinspection StringOperationCanBeSimplified
		if (loc.substring(loc.length() - 3, loc.length()).equalsIgnoreCase("obj")) {

			try (BufferedReader br = new BufferedReader(new InputStreamReader(url))) {
				String line;
//				BufferedReader br = new BufferedReader(new InputStreamReader(url));

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

				for (int i = 0; i < faces.size(); i++) {

					Face tempFace = new Face();

					for (int j = 0; j < faces.get(i).length; j++) {

						Vertex temp = new Vertex();
						temp.setAttribute(faces.get(i)[j], Vertex.POSITION);

						if (vtArray != null) {
							if (textureFaces.get(i) != null) {
								temp.setAttribute(textureFaces.get(i)[j], Vertex.TEXTURE);
							}
						}
						if (vnArray != null) {
							if (normalFaces.get(i) != null) {
								temp.setAttribute(normalFaces.get(i)[j], Vertex.NORMAL);
							}
						}
//						((ArrayList<Integer>)temp.vertAttributes).trimToSize();
						tempFace.vertices.add(temp);
					}
//					((ArrayList<Vertex>)tempFace.vertices).trimToSize();
					facesListObj.add(tempFace);
				}

				List<List<Vector>> vertAttributes = new ArrayList<>(3);
				vertAttributes.add(Mesh.POSITION, new ArrayList<>(Arrays.asList(vertArr)));
				vertAttributes.add(Mesh.TEXTURE, new ArrayList<>(Arrays.asList(vtArray)));
				vertAttributes.add(Mesh.NORMAL, new ArrayList<>(Arrays.asList(vnArray)));

				resMesh = new Mesh(facesListObj, vertAttributes);
				resMesh.trimEverything();
				return resMesh;

			} catch (IOException e) {
				throw new IllegalArgumentException("The file could not be opened. The file you tried to open was: "+loc);
			}

		}
		else {
			throw new IllegalArgumentException("This file type cannot be opened. Only .obj files can be opened. The file you tried to open was: "+loc);
		}
	}

	public static Mesh triangulate(Mesh inMesh, boolean forceEarClipping) {
		List<Face> newFaces = new ArrayList<>();
		for(Face f: inMesh.faces) {
			newFaces.addAll(triangulate(f, inMesh.getVertices(),forceEarClipping));
		}
		return new Mesh(newFaces,inMesh.vertAttributes);
	}

//	public static MeshLWJGL triangulate(MeshLWJGL inMesh, boolean forceEarClipping) {
//		List<Face> newFaces = new ArrayList<>();
//		for(Face f: inMesh.faces) {
//			newFaces.addAll(triangulate(f, inMesh.getVertices(),forceEarClipping));
//		}
//		return new MeshLWJGL(newFaces,inMesh.vertAttributes);
//	}

	public static List<Face> triangulate(Face f, List<Vector> vertices,boolean forceEarClipping) {
		if(f.vertices.size() > 4 || forceEarClipping) {
			return performEarClipping(f, vertices);
		}
		else {
			if(f.vertices.size() == 4) {
				return performSimpleQuadTriangulation(f);
			}
			else {
				List<Face> list = new ArrayList<>();
				list.add(f);
				return list;
			}
		}
	}

	public static List<Face> performSimpleQuadTriangulation(Face currFace) {
		List<Face> retFaces = new ArrayList<>();

		Face f1 = new Face();
		f1.addVertex(currFace.getVertex(0));
		f1.addVertex(currFace.getVertex(1));
		f1.addVertex(currFace.getVertex(3));

		Face f2 = new Face();
		f2.addVertex(currFace.getVertex(3));
		f2.addVertex(currFace.getVertex(1));
		f2.addVertex(currFace.getVertex(2));

		retFaces.add(f1);
		retFaces.add(f2);

		return retFaces;
	}

	public static List<Face> performEarClipping(Face currFace, List<Vector> vertList) {

		List<Face> retFaces = new ArrayList<>();

		CircularDoublyLinkedList<Vertex> verts = new CircularDoublyLinkedList<>(currFace.vertices);
		CircularDoublyLinkedList<Vertex> earTips = new CircularDoublyLinkedList<>();
		DoublyLinkedList<Vertex> convex = new DoublyLinkedList<>();
		DoublyLinkedList<Vertex> reflex = new DoublyLinkedList<>();

//		Construction of reflex and convex vertices lists
		for(int i = 0;i < verts.getSize();i++) {
			Node<Vertex> curr = verts.peekNextNode();
			if(Utils.isVertexConvex(curr.previous.data,curr.data,curr.next.data,vertList)) {
				convex.pushTail(curr.data);
			}
			else {
				reflex.pushTail(curr.data);
			}
		}
		verts.resetLoc();

//		Construction of ears list
		for(int i = 0;i < verts.getSize();i++) {
			Node<Vertex> curr = verts.peekNextNode();
			if(Utils.isEar(curr.previous.data,curr.data,curr.next.data,reflex,vertList)) {
				earTips.pushTail(curr.data);
			}
		}

		earTips.resetLoc();
		while(earTips.hasNext()) {

			if(verts.getSize() <=3) {
				Face newFace = new Face();
				newFace.addVertex(verts.popHead());
				newFace.addVertex(verts.popHead());
				newFace.addVertex(verts.popHead());
				retFaces.add(newFace);
				break;
			}
			else {
//			Remove one ear from top of list and create a new triangular Face object
				Vertex poppedEar = earTips.popHead();
				Node<Vertex> currNode = verts.searchAndRemoveNode(poppedEar);

				Face newFace = new Face();
				newFace.addVertex(currNode.previous.data);
				newFace.addVertex(currNode.data);
				newFace.addVertex(currNode.next.data);
				retFaces.add(newFace);

				Node<Vertex> v0 = currNode.previous;
				if (reflex.isPresent(v0.data)) {
					if (Utils.isVertexConvex(v0.previous.data, v0.data, currNode.data, vertList)) {
						reflex.searchAndRemoveNode(v0.data);
						convex.pushHead(v0.data);
						if (Utils.isEar(v0.previous.data, v0.data, currNode.data, reflex, vertList)) {
							earTips.pushHead(v0.data);
						}
					}
				} else {
					if (Utils.isEar(v0.previous.data, v0.data, currNode.data, reflex, vertList)) {
						if(!earTips.isPresent(v0.data)) {
							earTips.pushHead(v0.data);
						}
					}
				}

				Node<Vertex> v2 = currNode.next;
				if (reflex.isPresent(v2.data)) {
					if (Utils.isVertexConvex(currNode.data, v2.data, v2.next.data, vertList)) {
						reflex.searchAndRemoveNode(v2.data);
						convex.pushHead(v2.data);
						if (Utils.isEar(currNode.data, v2.data, v2.next.data, reflex, vertList)) {
							earTips.pushHead(v2.data);
						}
					}
				} else {
					if (Utils.isEar(currNode.data, v2.data, v2.next.data, reflex, vertList)) {
						if(!earTips.isPresent(v2.data)) {
							earTips.pushHead(v2.data);
						}
					}
				}
			}

		}

		return retFaces;
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
			System.arraycopy(verts[i], 0, vertices, i * w, verts[i].length);
		}

		List<Face> facesListObj = new ArrayList<>(cons.size());
		List<Vertex> tempVertList;
		Mesh resMesh;

		for (int[] con : cons) {
			tempVertList = new ArrayList<>(con.length);
			for (int i : con) {
				tempVertList.add(new Vertex(i));
			}
			facesListObj.add(new Face(tempVertList));
		}

		List<List<Vector>> vertAttributes = new ArrayList<>(1);
		vertAttributes.add(Mesh.POSITION, Arrays.asList(vertices));

		resMesh = new Mesh(facesListObj, vertAttributes);
		return new Model(resMesh,"grid");

//		return new Model(vertices, cons, null, null, null, null);
	}

}
