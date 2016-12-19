package de.dwerth.audiowakeup.main;

import de.dwerth.audiowakeup.input.IAudioInput;
import de.dwerth.audiowakeup.output.IWakeupOutput;

import java.util.ArrayList;
import java.util.List;

public class WiringComponent {

    private final static WiringComponent instance = new WiringComponent();

    private List<IAudioInput> audioInputs = new ArrayList<>();
    private List<IWakeupOutput> wakeupOutputs = new ArrayList<>();

    private WiringComponent() {
    }

    public static WiringComponent getInstance() {
        return instance;
    }

    public void registerWakeupOutput(IWakeupOutput output) {
        this.wakeupOutputs.add(output);
    }

    public void registerAudioInput(IAudioInput input) {
        this.audioInputs.add(input);
    }

    public boolean shouldIncreaseBrightness() {
        for (IAudioInput input : audioInputs) {
            if (input.shouldIncreaseBrightness()) {
                return true;
            }
        }
        return false;
    }

    public void triggerWakeup() {
        for (IWakeupOutput wakeupOutput : wakeupOutputs) {
            wakeupOutput.triggerWakeup();
        }
    }

    public void triggerWakeupDone() {
        for (IWakeupOutput wakeupOutput : wakeupOutputs) {
            wakeupOutput.triggerWakeupDone();
        }
    }
}
