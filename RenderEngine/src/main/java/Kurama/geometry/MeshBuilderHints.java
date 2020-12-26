package Kurama.geometry;

import Kurama.Math.Vector;

public class MeshBuilderHints {
    public boolean shouldSmartBakeVertexAttributes = true;
    public boolean shouldDumbBakeVertexAttributes = false;
    public boolean shouldTriangulate = true;
    public boolean forceEarClipping = false;
//    public boolean initLWJGLAttribs = true;
    public boolean addRandomColor = false;
    public boolean shouldGenerateTangentBiTangent = true;
    public Vector addConstantColor;
    public boolean convertToLines = false;
    public boolean shouldInvertNormals = false;
    public int shouldRotate = 180;
    public boolean shouldReverseWindingOrder = false;
    public boolean isInstanced = false;
    public int numInstances = 0;

    public String toString() {
        return "shouldSmartBakeVertexAttributes:" + shouldSmartBakeVertexAttributes +
                " shouldDumbBakeVertexAttributes:"+ shouldDumbBakeVertexAttributes +
                " shouldTriangulate:"+ shouldTriangulate +
                " forceEarClipping:"+ forceEarClipping +
//                " initLWJGLAttribs:"+ initLWJGLAttribs +
                " addRandomColor:"+ addRandomColor +
                " shouldGenerateTangentBiTangent:" + shouldGenerateTangentBiTangent +
                " addConstantColor:" + addConstantColor +
                " convertToLines:" + convertToLines +
                " shouldInvertNormals:" + shouldInvertNormals +
                " shouldRotate:" + shouldRotate +
                "isInstanced:" + isInstanced +
                "numInstanced:" + numInstances;
    }

}