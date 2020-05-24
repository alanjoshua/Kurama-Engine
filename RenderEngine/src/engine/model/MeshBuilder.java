package engine.model;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.utils.Utils;
import engine.DataStructure.LinkedList.CircularDoublyLinkedList;
import engine.DataStructure.LinkedList.DoublyLinkedList;
import engine.DataStructure.LinkedList.Node;
import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class MeshBuilder {

	public static class ModelBuilderHints {
		public boolean shouldSmartBakeVertexAttributes = false;
		public boolean shouldTriangulate = true;
		public boolean forceEarClipping = false;
		public boolean initLWJGLAttribs = false;
		public boolean addRandomColor = false;
		public boolean shouldGenerateTangentBiTangent = false;
		public Vector addConstantColor;
		public boolean convertToLines = false;
		public boolean shouldInvertNormals = false;
	}

	public static Mesh buildModelFromFileGL(String loc, Map<String,Mesh> meshInstances, ModelBuilderHints hints) {
//		Model res = null;
		Mesh resMesh = null;

		if(meshInstances != null) {
			resMesh = meshInstances.get(loc);
		}

		if(resMesh != null) {
			return resMesh;
//			return new Model(resMesh,loc);
		}
		else {
			resMesh = loadRawData(loc);

			if(hints == null) {
				resMesh = triangulate(resMesh,false);
				resMesh = dumbBake(resMesh,null);

				if(meshInstances != null) {
					meshInstances.put(loc, resMesh);
				}

			}
			else {
				if(hints.shouldTriangulate) {
					resMesh = triangulate(resMesh, hints.forceEarClipping);
				}

				if(hints.shouldInvertNormals) {
					resMesh = reverseNormals(resMesh);
				}

				if(hints.shouldGenerateTangentBiTangent) {
					resMesh = generateTangentAndBiTangentVectors(resMesh);
				}

				if(hints.shouldSmartBakeVertexAttributes) {
					resMesh = bakeMesh(resMesh,hints);
				}
				else {
					resMesh = dumbBake(resMesh,hints);
				}

				if(hints.addRandomColor) {
					resMesh = addRandomColor(resMesh);
				}

				if(hints.addConstantColor!=null) {
					resMesh = addColor(resMesh,hints.addConstantColor);
				}

				if(hints.convertToLines) {
					resMesh = convertToLines(resMesh,hints);
				}

				if(hints.initLWJGLAttribs) {
					resMesh.initOpenGLMeshData();
				}
			}

			if(meshInstances != null) {
				meshInstances.put(loc, resMesh);
			}

			resMesh.meshIdentifier = loc;
			return resMesh;
		}
	}

//	This functions converts models with triangular faces to lines
	public static Mesh convertToLines(Mesh mesh, ModelBuilderHints hints) {
		List<Integer> newIndices = new ArrayList<>();
		List<Face> newFaces = new ArrayList<>();

		if(mesh.indices == null) {
			System.err.println("Indices list is null. Returning same mesh. Mesh couldn't be converted to lines. Error with mesh: "+mesh.meshIdentifier);
			return mesh;
		}

		if(mesh.drawMode == GL_LINES) {
			System.err.println("Mesh draw mode already set to lines. Returning same mesh: " +mesh.meshIdentifier);
			return mesh;
		}

		for(int i = 0;i < mesh.indices.size();i+=3) {
			newIndices.add(mesh.indices.get(i));
			newIndices.add(mesh.indices.get(i+1));

			newIndices.add(mesh.indices.get(i+1));
			newIndices.add(mesh.indices.get(i+2));

			newIndices.add(mesh.indices.get(i+2));
			newIndices.add(mesh.indices.get(i));
		}

		for(Face f:mesh.faces) {
			if(f.vertices.size() != 3) {
				throw new IllegalArgumentException("convertToLines method can only convert triangulated meshes. Error with mesh: "+mesh.meshIdentifier);
			}

			Face line1 = new Face();
			line1.addVertex(f.vertices.get(0));
			line1.addVertex(f.vertices.get(1));

			Face line2 = new Face();
			line2.addVertex(f.vertices.get(1));
			line2.addVertex(f.vertices.get(2));

			Face line3 = new Face();
			line3.addVertex(f.vertices.get(2));
			line3.addVertex(f.vertices.get(0));

			newFaces.add(line1);
			newFaces.add(line2);
			newFaces.add(line3);
		}

		Mesh retMesh = new Mesh(newIndices,newFaces,mesh.vertAttributes);
		retMesh.drawMode = GL_LINES;
		retMesh.meshIdentifier = mesh.meshIdentifier;

		return retMesh;

	}

	public static Mesh dumbBake(Mesh mesh, ModelBuilderHints hints) {

		if(mesh.indices != null) {
			return mesh;
		}

		List<List<Vector>> newVertAttribs = new ArrayList<>(mesh.vertAttributes.size());
		List<Integer> indices = new ArrayList<>();
		List<Face> newFaces = new ArrayList<>();
		int counter = 0;

		for(int i = 0;i < mesh.vertAttributes.size();i++) {
			newVertAttribs.add(new ArrayList<Vector>());
		}

		for(Face f: mesh.faces) {

			if(f.vertices.size() != 3) {
				throw new RuntimeException("This method only works forn triangles");
			}

			for(Vertex v: f.vertices) {
				indices.add(counter);
				counter++;
				for(int i = 0;i < v.vertAttributes.size();i++) {
					Integer at = v.getAttribute(i);
					if(at != null) {
						try {
							newVertAttribs.get(i).add(mesh.getAttributeList(i).get(at));
						}catch(Exception e) {
							newVertAttribs.get(i).add(null);
							//System.out.println(i);
						}
					}
					else {
						newVertAttribs.get(i).add(null);
					}
				}
			}
		}

		for(int i = 0;i < indices.size();i+=3) {
			Face temp = new Face();
			for(int k = 0;k < 3;k++) {
				Vertex v = new Vertex();
				for (int j = 0; j < newVertAttribs.size(); j++) {
					v.setAttribute(i+k, j);
				}
				temp.addVertex(v);
			}
			newFaces.add(temp);
		}

		Mesh retMesh = new Mesh(indices,newFaces,newVertAttribs);
		retMesh.meshIdentifier = mesh.meshIdentifier;
		retMesh.drawMode = mesh.drawMode;
		return retMesh;

	}

	public static Mesh bakeMesh(Mesh mesh,ModelBuilderHints hints) {

		List<Integer> indexList = new ArrayList<>();
		List<List<Vector>> newVertAttribs = new ArrayList<>(mesh.vertAttributes.size());
		List<Vertex> uniqueVertices = new ArrayList<>();
		List<Face> newFaces = new ArrayList<>();

		for(int i = 0;i < mesh.vertAttributes.size();i++) {
			newVertAttribs.add(new ArrayList<>());
		}

		for(Face f:mesh.faces) {
			for(Vertex v: f.vertices) {
				int vInd = uniqueVertices.indexOf(v);
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

		for(Face f:mesh.faces) {
			if(f.vertices.size() != 3) {
				throw new IllegalArgumentException("only triangles could be baked in this method: "+mesh.meshIdentifier);
			}
			for(Vertex v: f.vertices) {
				indexList.add(uniqueVertices.indexOf(v));
			}
		}

//		Assumes there are only triangles
		for(int i = 0;i < indexList.size();i+=3) {
			Face temp = new Face();
			for(int k = 0;k < 3;k++) {
				Vertex v = new Vertex();
				for (int j = 0; j < newVertAttribs.size(); j++) {
					v.setAttribute(indexList.get(i+k), j);
				}
				temp.addVertex(v);
			}
			newFaces.add(temp);
		}

		Mesh retMesh = new Mesh(indexList,newFaces,newVertAttribs);
		retMesh.meshIdentifier = mesh.meshIdentifier;
		retMesh.drawMode = mesh.drawMode;
		return retMesh;
	}

	public static Mesh generateTangentAndBiTangentVectors(Mesh inMesh) {
		if(inMesh.getAttributeList(Mesh.TEXTURE) == null) {
			throw new RuntimeException("Cannot generate tangent and biTangent vectors if texture coordinates are not present: "+inMesh.meshIdentifier);
		}

		List<Face> faces = new ArrayList<>(inMesh.faces);
		List<Vector> tangents = new ArrayList<>();
		List<Vector> biTangents = new ArrayList<>();

		for(Face f:faces) {
			if(f.vertices.size() != 3) {
				throw new RuntimeException("Cannot generate tangent and biTangent vectors if face is not a triangle: "+inMesh.meshIdentifier);
			}

			Vertex v0 = f.getVertex(0);
			Vertex v1 = f.getVertex(1);
			Vertex v2 = f.getVertex(2);

			Vector edge1 = inMesh.getVertices().get(v1.getAttribute(Vertex.POSITION)).sub(inMesh.getVertices().get(v0.getAttribute(Vertex.POSITION)));
			Vector edge2 = inMesh.getVertices().get(v2.getAttribute(Vertex.POSITION)).sub(inMesh.getVertices().get(v0.getAttribute(Vertex.POSITION)));

			Vector deltaUV1 = inMesh.getAttributeList(Mesh.TEXTURE).get(v1.getAttribute(Vertex.TEXTURE)).sub(inMesh.getAttributeList(Mesh.TEXTURE).get(v0.getAttribute(Vertex.TEXTURE)));
			Vector deltaUV2 = inMesh.getAttributeList(Mesh.TEXTURE).get(v2.getAttribute(Vertex.TEXTURE)).sub(inMesh.getAttributeList(Mesh.TEXTURE).get(v0.getAttribute(Vertex.TEXTURE)));

			Vector tangent;
			Vector biTangent;
			float[] tanData = new float[3];
			float[] biTanData = new float[3];
			float determinantInv = 1.0f / (deltaUV1.get(0) * deltaUV2.get(1) - deltaUV2.get(0) * deltaUV1.get(1));

			tanData[0] = determinantInv * (deltaUV2.get(1) * edge1.get(0) - deltaUV1.get(1) * edge2.get(0));
			tanData[1] = determinantInv * (deltaUV2.get(1) * edge1.get(1)- deltaUV1.get(1) * edge2.get(1));
			tanData[2] = determinantInv * (deltaUV2.get(1) * edge1.get(2)- deltaUV1.get(1) * edge2.get(2));
			tangent = new Vector(tanData).normalise();

			biTanData[0] = determinantInv * (-deltaUV2.get(0) * edge1.get(0) + deltaUV1.get(0) * edge2.get(0));
			biTanData[1] = determinantInv * (-deltaUV2.get(0) * edge1.get(1) + deltaUV1.get(0) * edge2.get(1));
			biTanData[2] = determinantInv * (-deltaUV2.get(0) * edge1.get(2) + deltaUV1.get(0) * edge2.get(2));
			biTangent = new Vector(biTanData).normalise();

			tangents.add(tangent);
			biTangents.add(biTangent);

			v0.setAttribute(tangents.size()-1,Vertex.TANGENT);
			v1.setAttribute(tangents.size()-1,Vertex.TANGENT);
			v2.setAttribute(tangents.size()-1,Vertex.TANGENT);

			v0.setAttribute(biTangents.size()-1,Vertex.BITANGENT);
			v1.setAttribute(biTangents.size()-1,Vertex.BITANGENT);
			v2.setAttribute(biTangents.size()-1,Vertex.BITANGENT);
		}

		List<List<Vector>> newVertAttribs = new ArrayList<>(inMesh.vertAttributes);
		Mesh retMesh = new Mesh(inMesh.indices,faces,newVertAttribs);
		retMesh.setAttribute(tangents,Mesh.TANGENT);
		retMesh.setAttribute(biTangents,Mesh.BITANGENT);
		retMesh.meshIdentifier = inMesh.meshIdentifier;
		retMesh.drawMode = inMesh.drawMode;
		return retMesh;
	}

	public static Mesh reverseNormals(Mesh inMesh) {
		List<List<Vector>> vertList = inMesh.vertAttributes;
		List<Vector> newNormals = new ArrayList<>();
		for(Vector n:inMesh.getAttributeList(Mesh.NORMAL)) {
			newNormals.add(n.scalarMul(-1));
		}
		vertList.set(Mesh.NORMAL,newNormals);
		Mesh res = new Mesh(inMesh.indices,inMesh.faces,vertList);
		res.drawMode = inMesh.drawMode;
		res.meshIdentifier = inMesh.meshIdentifier;
		return res;
	}

	public static Mesh loadRawData(String loc) {

		MeshBuilder m = new MeshBuilder();
		if(!loc.substring(loc.length() - 3).equalsIgnoreCase("obj")) {
			String[] split = loc.split("/");
			loc = loc+"/"+split[split.length-1]+".obj";
		}

		URL url = m.getClass().getResource(loc);
		BufferedReader br = null;

		if(loc.charAt(0)=='/') {
			try {
				br = new BufferedReader(new InputStreamReader(url.openStream()));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				br = new BufferedReader(new FileReader(new File(loc)));
			}catch (Exception e) {
				e.printStackTrace();
			}
		}

		Mesh resMesh;

		List<Vector> vertex = new ArrayList<>();
		List<Vector> vn = new ArrayList<>();
		List<Vector> vt = new ArrayList<>();
		List<Material> matsList = new ArrayList<>();
		List<int[]> faces = new ArrayList<>();
		List<int[]> textureFaces = new ArrayList<>();
		List<int[]> normalFaces = new ArrayList<>();
		List<int[]> materialFaces = new ArrayList<>();
		int currentMaterialInd = 0;

		try {
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
							data[1] = 1 - Float.parseFloat(split[2]);
							data[2] = 0f;
						} else if (split.length == 4) {
							data[1] = 1 - Float.parseFloat(split[2]);
							data[2] = Float.parseFloat(split[3]);
						}

						vt.add(new Vector(data));
					}

					if (split[0].equalsIgnoreCase("f")) {

						int[] fdata = new int[split.length - 1];
						int[] tdata = new int[split.length - 1];
						int[] ndata = new int[split.length - 1];
						int[] materialIndData = new int[split.length - 1];
						for(int i = 0;i < split.length-1;i++) {
							materialIndData[i] = currentMaterialInd;
						}

						String[] doubleSplitTest = split[1].split("/");

						if (doubleSplitTest.length == 1) {
							tdata = null;
							ndata = null;
							for (int i = 1; i < split.length; i++) {
								int ind = Integer.parseInt(split[i]) - 1;
								if(ind < -1) {
									ind = vertex.size() - ind + 1;
								}
								fdata[i - 1] = ind;
							}
						} else if (doubleSplitTest.length == 2) {
							ndata = null;
							for (int i = 1; i < split.length; i++) {

								int fInd = (Integer.parseInt(split[i].split("/")[0]) - 1);
								int tInd = (Integer.parseInt(split[i].split("/")[1]) - 1);

								if(fInd < -1) {
									fInd = vertex.size() - fInd + 1;
								}
								if(tInd < -1) {
									tInd = vt.size() - tInd + 1;
								}

								fdata[i - 1] = fInd;
								tdata[i - 1] = tInd;
							}
						} else if (doubleSplitTest.length == 3) {

							if (doubleSplitTest[1].equalsIgnoreCase("")) {
								tdata = null;
								for (int i = 1; i < split.length; i++) {

									int fInd = (Integer.parseInt(split[i].split("/")[0]) - 1);
									int nInd = (Integer.parseInt(split[i].split("/")[2]) - 1);

									if(fInd < -1) {
										fInd = vertex.size() - fInd + 1;
									}
									if(nInd < -1) {
										nInd = vn.size() - nInd + 1;
									}

									fdata[i - 1] = fInd;
									ndata[i - 1] = nInd;
								}
							} else {
								for (int i = 1; i < split.length; i++) {

									int fInd = (Integer.parseInt(split[i].split("/")[0]) - 1);
									int tInd = (Integer.parseInt(split[i].split("/")[1]) - 1);
									int nInd = (Integer.parseInt(split[i].split("/")[2]) - 1);

									if(fInd < -1) {
										fInd = vertex.size() - fInd + 1;
									}
									if(tInd < -1) {
										tInd = vt.size() - tInd + 1;
									}
									if(nInd < -1) {
										nInd = vn.size() - nInd + 1;
									}

									fdata[i - 1] = fInd;
									tdata[i - 1] = tInd;
									ndata[i - 1] = nInd;
								}
							}
						}

						faces.add(fdata);
						textureFaces.add(tdata);
						normalFaces.add(ndata);
						materialFaces.add(materialIndData);
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
			Vector[] matArray = new Vector[currentMaterialInd+1];
			for(int i  =0;i < matArray.length;i++) {
				matArray[i] = new Vector(new float[]{i});
			}

			List<Face> facesListObj = new ArrayList<>(faces.size());

			for (int i = 0; i < faces.size(); i++) {

				Face tempFace = new Face();

				for (int j = 0; j < faces.get(i).length; j++) {

					Vertex temp = new Vertex();
					temp.setAttribute(faces.get(i)[j], Vertex.POSITION);
					temp.setAttribute(materialFaces.get(i)[j],Vertex.MATERIAL);

					if (textureFaces.get(i) != null) {
						temp.setAttribute(textureFaces.get(i)[j], Vertex.TEXTURE);
					}

					if (normalFaces.get(i) != null) {
						temp.setAttribute(normalFaces.get(i)[j], Vertex.NORMAL);
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
			//vertAttributes.add(Mesh.MATERIAL,new ArrayList<>(Arrays.asList(matArray)));

			resMesh = new Mesh(null,facesListObj, vertAttributes);
			resMesh.setAttribute(new ArrayList<>(Arrays.asList(matArray)),Mesh.MATERIAL);
			resMesh.meshIdentifier = loc;
			return resMesh;

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("The file could not be opened. The file you tried to open was: "+loc);
			return null;
		}
	}

	public static Mesh addRandomColor(Mesh inMesh) {
		List<Vector> colors = new ArrayList<>();
		Random rand = new Random();

		for(int i = 0;i < inMesh.getVertices().size();i++) {
			Vector col = new Vector(new float[]{rand.nextFloat(),rand.nextFloat(),rand.nextFloat(),1});
			colors.add(col);
		}

		for(Face f: inMesh.faces) {
			for(Vertex v: f.vertices) {
				v.setAttribute(v.getAttribute(Vertex.POSITION),Vertex.COLOR);
			}
		}

		inMesh.setAttribute(colors,Mesh.COLOR);
		return inMesh;

	}

//	Adds a constant color to the mesh
	public static Mesh addColor(Mesh inMesh, Vector color) {
		List<Vector> colors = new ArrayList<>();
		for(int i = 0;i < inMesh.getVertices().size();i++) {
			colors.add(color);
		}

		for(Face f: inMesh.faces) {
			for(Vertex v: f.vertices) {
				v.setAttribute(v.getAttribute(Vertex.POSITION), Vertex.COLOR);
			}
		}

		inMesh.setAttribute(colors,Mesh.COLOR);
		return inMesh;

	}

	public static Mesh triangulate(Mesh inMesh, boolean forceEarClipping) {
		List<Face> newFaces = new ArrayList<>();

		for(Face f: inMesh.faces) {
			newFaces.addAll(triangulate(f, inMesh.getVertices(),forceEarClipping));
		}
		Mesh retMesh = new Mesh(null,newFaces,inMesh.vertAttributes);
		retMesh.meshIdentifier = inMesh.meshIdentifier;
		retMesh.drawMode = GL_TRIANGLES;
		return retMesh;
	}

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

	public static Mesh buildGridDeprecated(int w, int d) {

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

		resMesh = new Mesh(null,facesListObj, vertAttributes);
		resMesh.meshIdentifier = "grid";
		return resMesh;
//		return new Model(resMesh,"grid");

	}

//	public static Mesh createNormals(Mesh mesh) {
//
//	}

	public static Mesh reverseWindingOrder(Mesh mesh) {
		List<Face> newFaces = new ArrayList<>();
		for(Face f:mesh.faces) {
			List<Vertex> newVerts = new ArrayList<>();
			for(int i = f.vertices.size()-1;i>=0;i--) {
				newVerts.add(f.vertices.get(i));
			}
			Vertex v1 = newVerts.remove(newVerts.size()-1);
			newVerts.add(0,v1);
			Face newFace = new Face();
			newFace.vertices = newVerts;
			newFaces.add(newFace);
		}

		Mesh resMesh = new Mesh(mesh.indices,newFaces,mesh.vertAttributes);
		resMesh.drawMode = mesh.drawMode;
		return resMesh;
	}

	public static Mesh buildAxes() {

		Vector xColor = new Vector(new float[]{1,0,0,1});
		Vector yColor = new Vector(new float[]{0,1,0,1});
		Vector zColor = new Vector(new float[]{0,0,1,1});

		Vector origin = new Vector(new float[]{0,0,0,1});
		Vector x = new Vector(new float[]{1,0,0,1});
		Vector y = new Vector(new float[]{0,1,0,1});
		Vector z = new Vector(new float[]{0,0,1,1});

		List<Vector> vertices = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();
		List<Face> faces = new ArrayList<>();
		List<Vector> colors = new ArrayList<>();

		vertices.add(origin);
		vertices.add(x);
		vertices.add(y);
		vertices.add(z);

		colors.add(new Vector(new float[]{1,1,1,1}));
		colors.add(xColor);
		colors.add(yColor);
		colors.add(zColor);

		indices.add(0);
		indices.add(1);
		indices.add(0);
		indices.add(2);
		indices.add(0);
		indices.add(3);

		Face tempFace = new Face();
		Vertex v0 = new Vertex();
		Vertex v1 = new Vertex();
		v0.setAttribute(0,Vertex.POSITION);
		v0.setAttribute(0,Vertex.COLOR);
		v1.setAttribute(1,Vertex.POSITION);
		v1.setAttribute(1,Vertex.COLOR);
		tempFace.addVertex(v0);
		tempFace.addVertex(v1);
		faces.add(tempFace);

		tempFace = new Face();
		v0 = new Vertex();
		v1 = new Vertex();
		v0.setAttribute(0,Vertex.POSITION);
		v0.setAttribute(0,Vertex.COLOR);
		v1.setAttribute(2,Vertex.POSITION);
		v1.setAttribute(2,Vertex.COLOR);
		tempFace.addVertex(v0);
		tempFace.addVertex(v1);
		faces.add(tempFace);

		tempFace = new Face();
		v0 = new Vertex();
		v1 = new Vertex();
		v0.setAttribute(0,Vertex.POSITION);
		v0.setAttribute(0,Vertex.COLOR);
		v1.setAttribute(3,Vertex.POSITION);
		v1.setAttribute(3,Vertex.COLOR);
		tempFace.addVertex(v0);
		tempFace.addVertex(v1);
		faces.add(tempFace);

		List<List<Vector>> attribs = new ArrayList<>();
		attribs.add(vertices);

		Mesh ret = new Mesh(indices,faces,attribs);
		ret.setAttribute(colors,Mesh.COLOR);
		ret.drawMode = GL_LINES;
		ret.initOpenGLMeshData();

		return ret;
	}

	//Normals not set properly
	public static Mesh buildGridTrigs(int w, int h, ModelBuilderHints hints) {

		List<Face> faces = new ArrayList<>();
		List<Vector> vertices = new ArrayList<>();
		List<Vector> normals = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();

		Vector n = new Vector(new float[]{0,-1,0});

		for(int i = 0;i < w;i++) {
			for(int j = 0;j < h;j++) {

				if(i == 0 && j == 0) { //Very first cell in first column

					Vector v1 = new Vector(new float[]{i, 0, j + 1,1});
					Vector v2 = new Vector(new float[]{i, 0, j,1});
					Vector v3 = new Vector(new float[]{i + 1, 0, j,1});
					Vector v4 = new Vector(new float[]{i + 1, 0, j + 1,1});

					vertices.add(v1);
					vertices.add(v2);
					vertices.add(v3);
					vertices.add(v4);

					normals.add(n);
					normals.add(n);
					normals.add(n);
					normals.add(n);

					for(int k = 0;k < 4;k++) {
						indices.add(indices.size());
					}

					Face tempFace = new Face();

					Vertex vert = new Vertex();
					vert.setAttribute(0,Vertex.POSITION);
					vert.setAttribute(0,Vertex.NORMAL);
					tempFace.addVertex(vert);

					vert = new Vertex();
					vert.setAttribute(1,Vertex.POSITION);
					vert.setAttribute(1,Vertex.NORMAL);
					tempFace.addVertex(vert);

					vert = new Vertex();
					vert.setAttribute(2,Vertex.POSITION);
					vert.setAttribute(2,Vertex.NORMAL);
					tempFace.addVertex(vert);

					vert = new Vertex();
					vert.setAttribute(3,Vertex.POSITION);
					vert.setAttribute(3,Vertex.NORMAL);
					tempFace.addVertex(vert);

					faces.add(tempFace);
				}

				else if(i == 0) { //Cell in first column but not first cell

					Vector v1 = new Vector(new float[]{i, 0, j + 1,1});
					Vector v4 = new Vector(new float[]{i+1, 0, j+1,1});

					vertices.add(v1);
					vertices.add(v4);

					normals.add(n);
					normals.add(n);

					int ind1 = indices.get(indices.size() - 4);
					int ind2 = indices.get(indices.size() - 1);

					indices.add(vertices.size() - 2);
					indices.add(ind1);
					indices.add(ind2);
					indices.add(vertices.size() - 1);

					Face tempFace = new Face();
					for(int k = 0;k < 4;k++) {
						Vertex vert = new Vertex();
						vert.setAttribute(indices.get(indices.size() - 4 + k), Vertex.POSITION);
						vert.setAttribute(indices.get(indices.size() - 4 + k), Vertex.NORMAL);
						tempFace.addVertex(vert);
					}
					faces.add(tempFace);
				}

				else { // Not first column

					if(j == 0) { //First cell in other columns
						Vector v3 = new Vector(new float[]{i+1,0,j,1});
						Vector v4 = new Vector(new float[]{i+1,0,j+1,1});

						vertices.add(v3);
						vertices.add(v4);
						normals.add(n);
						normals.add(n);


						int cellInd = (4 * (i*h + j)) - (4*h);
//						System.out.println("i: "+i+" j: "+j );

						int ind1 = indices.get(cellInd + 3);
						int ind2 = indices.get(cellInd + 2);
						indices.add(ind1);
						indices.add(ind2);
						indices.add(vertices.size() - 2);
						indices.add(vertices.size() - 1);

						Face tempFace = new Face();
						for(int k = 0;k < 4;k++) {
							Vertex vert = new Vertex();
							vert.setAttribute(indices.get(indices.size() - 4 + k), Vertex.POSITION);
							vert.setAttribute(indices.get(indices.size() - 4 + k), Vertex.NORMAL);
							tempFace.addVertex(vert);
						}
						faces.add(tempFace);
					}

					else {  //Not first cell of any column

						Vector v4 = new Vector(new float[]{i+1,0,j+1,1});
						vertices.add(v4);
						normals.add(n);

						int cellInd = (4 * (i*h + j)) - (4*h);
						int ind1 = indices.get(cellInd + 3);
						int ind2 = indices.get(cellInd + 2);
						int ind3 = indices.get(indices.size() - 1);
						int ind4 = vertices.size() - 1;

						indices.add(ind1);
						indices.add(ind2);
						indices.add(ind3);
						indices.add(ind4);

						Face tempFace = new Face();
						for(int k = 0;k < 4;k++) {
							Vertex vert = new Vertex();
							vert.setAttribute(indices.get(indices.size() - 4 + k), Vertex.POSITION);
							vert.setAttribute(indices.get(indices.size() - 4 + k), Vertex.NORMAL);
							tempFace.addVertex(vert);
						}
						faces.add(tempFace);

					}

				}


			}
		}

		List<List<Vector>> vertAttribs = new ArrayList<>(3);
		vertAttribs.add(vertices);
		vertAttribs.add(null);
		vertAttribs.add(normals);

		Mesh resMesh = new Mesh(null,faces,vertAttribs);
		resMesh = triangulate(resMesh,false);

		List<Integer> newIndices = new ArrayList<>();

		for(int i = 0; i < indices.size();i+=4) {
			int v1 = indices.get(i);
			int v2 = indices.get(i+1);
			int v3 = indices.get(i+2);
			int v4 = indices.get(i+3);

			newIndices.add(v1);
			newIndices.add(v2);
			newIndices.add(v3);

			newIndices.add(v1);
			newIndices.add(v3);
			newIndices.add(v4);
		}

		resMesh.indices = newIndices;
		resMesh = reverseWindingOrder(resMesh);

		if(hints != null) {

			if(hints.addRandomColor) {
				resMesh = addRandomColor(resMesh);
			}

			if(hints.addConstantColor!=null) {
				resMesh = addColor(resMesh,hints.addConstantColor);
			}

			if(hints.convertToLines) {
				resMesh = convertToLines(resMesh,hints);
			}

			if(hints.initLWJGLAttribs)
				resMesh.initOpenGLMeshData();
		}

		resMesh.meshIdentifier = "grid-trigs";
		return resMesh;
//		return new Model(resMesh,"grid-trigs");

	}

	public static Mesh buildGridLines(int w, int h,ModelBuilderHints hints) {
		List<Vector> vertices = new ArrayList<>();
		List<Face> faces = new ArrayList<>();

		for(int i = 0;i <= w;i++) {
			Vector v1 = new Vector(new float[]{i,0,0,1});
			Vector v2 = new Vector(new float[]{i,0,h,1});
			vertices.add(v1);
			vertices.add(v2);

			Vertex vert1 = new Vertex();
			Vertex vert2 = new Vertex();

			vert1.setAttribute(vertices.size() - 2,Vertex.POSITION);
			vert2.setAttribute(vertices.size() - 1,Vertex.POSITION);

			Face tempFace = new Face();
			tempFace.addVertex(vert1);
			tempFace.addVertex(vert2);

			faces.add(tempFace);
		}

		for(int i = 0;i <= h;i++) {
			Vector v1 = new Vector(new float[]{0,0,i,1});
			Vector v2 = new Vector(new float[]{w,0,i,1});
			vertices.add(v1);
			vertices.add(v2);

			Vertex vert1 = new Vertex();
			Vertex vert2 = new Vertex();

			vert1.setAttribute(vertices.size() - 2,Vertex.POSITION);
			vert2.setAttribute(vertices.size() - 1,Vertex.POSITION);

			Face tempFace = new Face();
			tempFace.addVertex(vert1);
			tempFace.addVertex(vert2);

			faces.add(tempFace);
		}

		List<List<Vector>> vertAttribs = new ArrayList<>();
		vertAttribs.add(vertices);

		Mesh resMesh = new Mesh(null,faces,vertAttribs);

		if(hints != null) {

			if(hints.addRandomColor) {
				resMesh = addRandomColor(resMesh);
			}

			if(hints.addConstantColor!=null) {
				resMesh = addColor(resMesh,hints.addConstantColor);
			}

			if(hints.initLWJGLAttribs)
				resMesh.initOpenGLMeshData();
		}

		resMesh.drawMode = GL_LINES;
		resMesh.meshIdentifier = "Grid-Lines";
		return resMesh;
//		return new Model(resMesh,"Grid-Lines");

	}

}
