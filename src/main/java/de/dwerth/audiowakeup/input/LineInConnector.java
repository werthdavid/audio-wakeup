package de.dwerth.audiowakeup.input;

import de.dwerth.audiowakeup.main.WiringComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import org.apache.log4j.Logger;

public class LineInConnector implements IAudioInput {

    private static final Logger log = Logger.getLogger(LineInConnector.class);
    private static final String FOUND = "FOUND";
    private static final String NOT_FOUND = "NOT_FOUND";

    private String mixerName;
    private int signalThreshold;

    private List<String> lastSignals = Collections.synchronizedList(new LinkedList<String>());

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
                if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                    log.debug("\tLine: " + lineInfo.toString());
                    try {
                        out.put(mixerInfo.getName() + ": " + lineInfo.toString(), m.getLine(lineInfo));
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
        if (lastSignals.size() >= 10) {
            Iterator<String> iter = lastSignals.iterator();
            iter.next();
            iter.remove();
        }
        lastSignals.add(signal);
        if (lastSignals.size() == 10) {
            ListIterator<String> listIterator = lastSignals.listIterator(6);
            String lastSignal6 = listIterator.next();
            String lastSignal7 = listIterator.next();
            String lastSignal8 = listIterator.next();
            String lastSignal9 = listIterator.next();
            if (lastSignal7.equals(lastSignal8) && lastSignal7.equals(lastSignal9) && !lastSignal7.equals(lastSignal6)) {
                if (signal.equals(NOT_FOUND)) {
                    WiringComponent.getInstance().triggerWakeupDone();
                } else {
                    WiringComponent.getInstance().triggerWakeup();
                }
            }
        }
    }

    public Thread getHandlerThread() {
        Thread t = new Thread() {
            public void run() {
                TargetDataLine targetDataLine = (TargetDataLine) getLine();
                if (targetDataLine != null) {
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
                            log.debug("Line in signal: " + max);
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
                    log.info("Found Line: " + key);
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
