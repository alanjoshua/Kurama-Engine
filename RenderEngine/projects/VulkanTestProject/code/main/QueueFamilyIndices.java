package main;

import java.util.stream.IntStream;

public class QueueFamilyIndices {
    // We use Integer to use null as the empty value
    public Integer graphicsFamily;
    public Integer presentFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }

    public int[] unique() {
        return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
    }

    public int[] array() {
        return new int[] {graphicsFamily, presentFamily};
    }
}
