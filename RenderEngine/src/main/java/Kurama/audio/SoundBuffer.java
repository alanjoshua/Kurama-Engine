package Kurama.audio;

import Kurama.utils.Utils;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;

// This class was taken mostly as-is from lwjglbook.

public class SoundBuffer {

    public final int bufferALID;
    public String bufferID;
    private ShortBuffer pcm = null;
    private ByteBuffer vorbis = null;

    public SoundBuffer(String bufferID, String file) throws Exception {
        this.bufferALID = alGenBuffers();
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            pcm = readVorbis(file, 32 * 408, info);
            alBufferData(bufferALID, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, info.sample_rate());
        }
        this.bufferID = bufferID;
        if (bufferID == null) {
            this.bufferID = Utils.getUniqueID();
        }
    }

    private ShortBuffer readVorbis(String resource, int bufferSize, STBVorbisInfo info) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vorbis = Utils.ioResourceToByteBuffer(resource, bufferSize);
            IntBuffer error = stack.mallocInt(1);
            long decoder = stb_vorbis_open_memory(vorbis, error, null);
            if (decoder == NULL) {
                throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));
            }

            stb_vorbis_get_info(decoder, info);

            int channels = info.channels();

            int lengthSamples = stb_vorbis_stream_length_in_samples(decoder);

            var pcm = MemoryUtil.memAllocShort(lengthSamples);

            pcm.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm) * channels);
            stb_vorbis_close(decoder);

            return pcm;
        }
    }

    public void cleanUp() {
        alDeleteBuffers(bufferALID);
        if(pcm!=null) {
            MemoryUtil.memFree(pcm);
        }
    }

}
