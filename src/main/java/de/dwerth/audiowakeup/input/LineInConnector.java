package de.dwerth.audiowakeup.input;

import de.dwerth.audiowakeup.main.WiringComponent;
import org.apache.log4j.Logger;

import javax.sound.sampled.*;
import java.util.*;

public class LineInConnector implements IAudioInput {

    private static final Logger log = Logger.getLogger(LineInConnector.class);
    private static final String FOUND = "FOUND";
    private static final String NOT_FOUND = "NOT_FOUND";

    private String lineName;
    private int signalThreshold;

    private List<String> lastSignals = Collections.synchronizedList(new LinkedList<String>());

    public LineInConnector(String lineName, int signalThreshold) {
        this.lineName = lineName;
        this.signalThreshold = signalThreshold;
        getHandlerThread().start();
    }

    public static HashMap<String, Line> enumerateLines() {
        HashMap<String, Line> out = new HashMap<String, Line>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        log.debug("Available Mixers/Lines:");
        for (Mixer.Info mixerInfo : mixerInfos) {
            log.debug("Mixer: " + mixerInfo.getName() + " " + mixerInfo.getDescription());
            Mixer m = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = m.getTargetLineInfo();
            for (Line.Info lineInfo : lineInfos) {
                if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                    log.debug("- - Line: " + lineInfo.toString());
                    try {
                        out.put(mixerInfo.getName() + " " + mixerInfo.getDescription() + ": " + lineInfo.toString(), m.getLine(lineInfo));
                    } catch (LineUnavailableException e) {
                        log.error("Line Unavailable: " + e.getMessage());
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
                TargetDataLine targetDataLine = getLine();
                if (targetDataLine != null) {
                    try {
                        targetDataLine.open();
                    } catch (LineUnavailableException e) {
                        log.error("Line Unavailable: " + e.getMessage());
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
                } else {
                    log.error("No Line found!");
                }
            }
        };
        return t;
    }

    public TargetDataLine getLine() {
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        try {
            HashMap<String, Line> out = enumerateLines();
            log.debug("Looking for '" + lineName + "'");
            for (String key : out.keySet()) {
                if (key.contains(lineName)) {
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
