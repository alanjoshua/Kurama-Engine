package Kurama.audio;

import Kurama.Math.Vector;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.utils.Utils;

import static org.lwjgl.openal.AL10.*;

public class SoundSource {

    public final int sourceALID;
    public String sourceID;
    public SoundBuffer currentBuffer;
    public Model attachedModel;
    public boolean isRelative;

    public SoundSource(String sourceID, boolean loop, boolean relative) {
        this.sourceID = sourceID;
        if(sourceID == null) {
            this.sourceID = Utils.getUniqueID();
        }
        this.sourceALID = alGenSources();
        if (loop) {
            alSourcei(sourceALID, AL_LOOPING, AL_TRUE);
        }
        if (relative) {
            alSourcei(sourceALID, AL_SOURCE_RELATIVE, AL_TRUE);
        }
        isRelative = relative;
    }

    public void setBuffer(SoundBuffer soundBuffer) {
        stop();
        this.currentBuffer = soundBuffer;
        alSourcei(sourceALID, AL_BUFFER, soundBuffer.bufferALID);
    }

    public void setPosition(Vector position) {
        alSource3f(sourceALID, AL_POSITION, position.get(0), position.get(1), position.get(2));
    }

    public void setSpeed(Vector speed) {
        alSource3f(sourceALID, AL_VELOCITY, speed.get(1), speed.get(1), speed.get(2));
    }

    public void setGain(float gain) {
        alSourcef(sourceALID, AL_GAIN, gain);
    }

    public void setProperty(int param, float value) {
        alSourcef(sourceALID, param, value);
    }

    public void play() {
        alSourcePlay(sourceALID);
    }

    public boolean isPlaying() {
        return alGetSourcei(sourceALID, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void pause() {
        alSourcePause(sourceALID);
    }

    public void stop() {
        alSourceStop(sourceALID);
    }

    public void cleanup() {
        stop();
        alDeleteSources(sourceALID);
    }

    public void attachToModel(Model model) {
        attachedModel = model;
    }

    public void tick(SoundSourceTickInput input) {
        if(attachedModel != null) {
            if(!isRelative) {
                setPosition(attachedModel.getPos());
                setSpeed(attachedModel.velocity);
            }
            else {
                var cam = input.camera;
                var relSpeed = cam.velocity.sub(attachedModel.velocity);
                var relPos = cam.getPos().sub(attachedModel.getPos());
                setPosition(relPos);
                setSpeed(relSpeed);
            }
        }
    }

}
