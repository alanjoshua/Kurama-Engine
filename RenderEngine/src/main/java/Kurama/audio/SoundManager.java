package Kurama.audio;

import Kurama.camera.Camera;
import Kurama.utils.Logger;
import Kurama.utils.Utils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.sql.Types.NULL;
import static org.lwjgl.openal.ALC10.*;

public class SoundManager {

    public long device;
    public long context;
    public SoundListener listener;

    public final List<SoundBuffer> soundBufferList;
    public final Map<String, SoundBuffer> soundBufferMap;

    public final List<SoundSource> soundSourceList;
    public final Map<String, SoundSource> soundSourceMap;

    public SoundManager() {
        soundBufferList = new ArrayList<>();
        soundSourceMap = new HashMap<>();
        soundSourceList = new ArrayList<>();
        soundBufferMap = new HashMap<>();
    }

    public void init() throws Exception {
        this.device = alcOpenDevice((ByteBuffer)null);
        if(device == NULL) {
            throw new IllegalArgumentException("Failed to open the default OpenAL device.");
        }

        var deviceCaps = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        if(context == NULL) {
            throw new IllegalArgumentException("Failed to create OpenAL context.");
        }

        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        listener = new SoundListener();
    }

//    This assumes you are trying to add a unique source, so if the sourceID is already found in the sources map,
//    the new source would be given a unique ID and added as a unique entry into the map
    public void addSoundSource(SoundSource newSource) {

        if(newSource == null) {
            Logger.logError("null was attempted to be added as a sound source.");
            return;
        }

        if(newSource.sourceID == null || soundSourceMap.containsKey(newSource.sourceID) ) {
            newSource.sourceID = Utils.getUniqueID();
        }
        soundSourceMap.put(newSource.sourceID, newSource);
        soundSourceList.add(newSource);

        if(newSource.currentBuffer != null) {
            if(newSource.currentBuffer.bufferID == null) {
                newSource.currentBuffer.bufferID = Utils.getUniqueID();
            }
            if(!soundBufferMap.containsKey(newSource.currentBuffer.bufferID)) {
                soundBufferMap.put(newSource.currentBuffer.bufferID, newSource.currentBuffer);
                soundBufferList.add(newSource.currentBuffer);
            }
        }

    }

//    This assumes you are trying to add a unique buffer, so if the bufferID is already found in the buffer map,
//    the new buffer would be given a unique ID and added as a unique entry into the map
    public void addSoundBuffer(SoundBuffer newBuffer) {

        if(newBuffer == null) {
            Logger.logError("null was attempted to be added as a sound buffer.");
            return;
        }

        if(newBuffer.bufferID == null || soundBufferMap.containsKey(newBuffer.bufferID)) {
            newBuffer.bufferID = Utils.getUniqueID();
        }
        soundBufferList.add(newBuffer);
        soundBufferMap.put(newBuffer.bufferID , newBuffer);
    }

    public void tick(Camera camera, float tick) {
        listener.setPosition(camera.getPos());
        listener.setSpeed(camera.velocity);
        listener.setOrientation(camera.getOrientation());

        SoundSourceTickInput sourceInput = new SoundSourceTickInput(tick, camera);
        soundSourceList.forEach(s -> s.tick(sourceInput));
    }

}
