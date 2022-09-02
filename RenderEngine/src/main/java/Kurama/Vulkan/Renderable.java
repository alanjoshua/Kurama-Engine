package Kurama.Vulkan;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.util.vma.Vma.*;

// A renderable object will contain one mesh, one model, and the relevant vulkan specific render attributes
public class Renderable {

    public AllocatedBuffer vertexBuffer;
    public AllocatedBuffer indexBuffer;

    // assume each mesh only has one material
    public Mesh mesh;
    public Model model;


    public Renderable(Mesh mesh, Model model) {
        this.mesh = mesh;
        this.model = model;
    }

    // assume each mesh only has one material
    public Material getMaterial() {
        return mesh.materials.get(0);
    }

    public void cleanUp(long vmaAllocator) {
        vmaDestroyBuffer(vmaAllocator, vertexBuffer.buffer, vertexBuffer.allocation);
        vmaDestroyBuffer(vmaAllocator, indexBuffer.buffer, indexBuffer.allocation);
    }

    public static List<Renderable> getRenderablesFromModel(Model model) {

        var result = new ArrayList<Renderable>();
        for(var mesh: model.meshes) {
            result.add(new Renderable(mesh, model));
        }

        return result;
    }

}
