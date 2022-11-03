package Kurama.Vulkan;

import java.util.stream.IntStream;

public class QueueFamilyIndices {
    // We use Integer to use null as the empty value
    public Integer graphicsFamily;
    public Integer transferFamily;
    public Integer presentFamily;
    public Integer computeFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null && computeFamily != null && transferFamily != null;
    }
    public int[] unique() {
        return IntStream.of(graphicsFamily, presentFamily, computeFamily, transferFamily).distinct().toArray();
    }

    public int[] array() {
        return new int[] {graphicsFamily, presentFamily, computeFamily, transferFamily};
    }
}