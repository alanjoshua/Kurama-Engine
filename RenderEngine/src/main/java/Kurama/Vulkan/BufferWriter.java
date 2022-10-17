package Kurama.Vulkan;

import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.vmaMapMemory;
import static org.lwjgl.util.vma.Vma.vmaUnmapMemory;

public class BufferWriter {

    public AllocatedBuffer allocatedBuffer;
    public long vmaAllocator;
    public int alignmentSize;
    public int bufferSize;
    public ByteBuffer buffer;

    public BufferWriter(long vmaAllocator, AllocatedBuffer allocatedBuffer, int alignmentSize, int bufferSize) {
        this.allocatedBuffer = allocatedBuffer;
        this.vmaAllocator = vmaAllocator;
        this.alignmentSize = alignmentSize;
        this.bufferSize = bufferSize;
    }

    public void mapBuffer() {
        try (var stack = stackPush()) {
            var data = stack.mallocPointer(1);
            vmaMapMemory(vmaAllocator, allocatedBuffer.allocation, data);
            buffer = data.getByteBuffer(bufferSize);
        }
    }

    public void setPosition(int i) {
        int offset = i * alignmentSize;
        buffer.position(offset);
    }

    public void put(Matrix data) {
        put(data.getData());
    }

    public void put(int data) {
        buffer.putInt(data);
    }

    public void put(float[][] data) {
        for(int j = 0; j < data[0].length; j++) {
            for(int i = 0; i < data.length; i++) {
                buffer.putFloat(data[i][j]);
            }
        }
    }

    public void unmapBuffer() {
        vmaUnmapMemory(vmaAllocator, allocatedBuffer.allocation);
    }

}