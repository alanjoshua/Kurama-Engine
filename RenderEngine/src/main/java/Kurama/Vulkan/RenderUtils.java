package Kurama.Vulkan;

import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;

import java.util.ArrayList;
import java.util.List;

public class RenderUtils {

    public static class IndirectBatch {

        public Mesh mesh;
        public Material material;
        public int first;
        public int count;

        public IndirectBatch(Mesh mesh, Material material, int first, int count) {
            this.mesh = mesh;
            this.material = material;
            this.first = first;
            this.count = count;
        }

    }

    public static List<IndirectBatch>  compactDraws(List<Renderable> renderables) {
        var draws = new ArrayList<IndirectBatch>();

        var lastDraw = new IndirectBatch(renderables.get(0).mesh, renderables.get(0).getMaterial(), 0, 1);
        draws.add(lastDraw);

        for(int i = 1; i < renderables.size(); i++) {

            var sameMesh = renderables.get(i).mesh == lastDraw.mesh;
            var sameMaterial = renderables.get(i).getMaterial() == lastDraw.material;

            if(sameMaterial) {
                lastDraw.count++;
            }
            else {
                lastDraw = new IndirectBatch(renderables.get(i).mesh, renderables.get(i).getMaterial(), i, 1);
                draws.add(lastDraw);
            }

        }

        return draws;
    }

}
