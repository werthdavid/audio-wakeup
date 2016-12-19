package de.dwerth.audiowakeup.audio;

import de.dwerth.audiowakeup.input.IAudioInput;
import de.dwerth.audiowakeup.main.WiringComponent;
import org.apache.log4j.Logger;

import javax.sound.sampled.*;
import java.util.*;

public class LineInConnector implements IAudioInput {

    private static final Logger log = Logger.getLogger(LineInConnector.class);
    private static final String FOUND = "FOUND";
    private static final String NOT_FOUND = "NOT_FOUND";

    private String mixerName;
    private int signalThreshold;

    private List<String> lastSignals = Collections.synchronizedList(new LinkedList<>());

    public LineInConnector(String mixerName, int signalThreshold) {
        this.mixerName = mixerName;
        this.signalThreshold = signalThreshold;
        getHandlerThread().start();
    }

    public static HashMap<String, Line> enumerateLines() {
        HashMap<String, Line> out = new HashMap<String, Line>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            log.debug("Mixer: " + mixerInfo.getName() + " " + mixerInfo.getDescription());
            Mixer m = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = m.getTargetLineInfo();
            for (Line.Info lineInfo : lineInfos) {
                log.debug("\tLine: " + lineInfo.toString());
                if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                    try {
                        out.put(mixerInfo.getName(), m.getLine(lineInfo));
                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return out;
    }

    @Override
    public boolean shouldIncreaseBrightness() {
        if (lastSignals.size() == 10) {
            ListIterator<String> listIterator = lastSignals.listIterator(7);
            while (listIterator.hasNext()) {
                String signal = listIterator.next();
                if (!signal.equals(FOUND)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void pushSignal(String signal) {
        if (lastSignals.size() > 0) {
            ListIterator<String> listIterator = lastSignals.listIterator(lastSignals.size() - 1);
            String lastSignal = listIterator.next();
            if (!lastSignal.equals(signal)) {
                if (signal.equals(NOT_FOUND)) {
                    WiringComponent.getInstance().triggerWakeupDone();
                } else {
                    WiringComponent.getInstance().triggerWakeup();
                }
            }
        } else {
            if (signal.equals(FOUND)) {
                WiringComponent.getInstance().triggerWakeup();
            }
        }
        if (lastSignals.size() >= 10) {
            Iterator<String> iter = lastSignals.iterator();
            iter.next();
            iter.remove();
        }
        lastSignals.add(signal);
    }

    public Thread getHandlerThread() {
        Thread t = new Thread() {
            public void run() {
                TargetDataLine targetDataLine = (TargetDataLine) getLine();
                if (targetDataLine != null) {
                    log.info("Found line: " + targetDataLine.getLineInfo());
                    try {
                        targetDataLine.open();
                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                    }
                    targetDataLine.start();
                    while (true) {
                        byte[] buffer = new byte[2000];
                        int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

                        short max;
                        if (bytesRead >= 0) {
                            max = (short) (buffer[0] + (buffer[1] << 8));
                            for (int p = 2; p < bytesRead - 1; p += 2) {
                                short thisValue = (short) (buffer[p] + (buffer[p + 1] << 8));
                                if (thisValue > max) max = thisValue;
                            }
//                            log.debug("Max value is " + max);
                            if (max > signalThreshold) {
                                pushSignal(FOUND);
                            } else {
                                pushSignal(NOT_FOUND);
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        return t;
    }

    public TargetDataLine getLine() {
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        try {
            HashMap<String, Line> out = enumerateLines();
            for (String key : out.keySet()) {
                if (key.contains(mixerName)) {
                    return (TargetDataLine) out.get(key);
                }
            }
            log.info("Returning OS Default Line");
            return AudioSystem.getTargetDataLine(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }
}
