package de.dwerth.audiowakeup.main;


import de.dwerth.audiowakeup.input.LineInConnector;
import de.dwerth.audiowakeup.output.CmdConnector;
import de.dwerth.audiowakeup.output.HyperionConnector;
import de.dwerth.audiowakeup.output.IFTTTConnector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting up");

        Properties props = new Properties();

        File propsFile = new File("audio-wakeup.properties");
        if (propsFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(propsFile);
                props.load(fis);
                fis.close();
            } catch (Exception e) {
                log.warn("Could not load Properties: " + e.getMessage());
            }
        }
        log.setLevel(Level.toLevel(props.getProperty("log.level", "INFO")));

        LineInConnector lineInConnector = new LineInConnector(props.getProperty("mixer.name", "plughw:3,0"),
                Integer.parseInt(props.getProperty("line.signalthreshold", "2000")));
        WiringComponent.getInstance().registerAudioInput(lineInConnector);

        HyperionConnector hyperionConnector = new HyperionConnector(Integer.parseInt(props.getProperty("hyperion.priority", "100")),
                props.getProperty("hyperion.host", "localhost"), Integer.parseInt(props.getProperty("hyperion.port", "19444")));
        WiringComponent.getInstance().registerWakeupOutput(hyperionConnector);

        if (props.getProperty("ifttt.makerurl") != null) {
            IFTTTConnector iftttConnector = new IFTTTConnector(props.getProperty("ifttt.makerurl"));
            WiringComponent.getInstance().registerWakeupOutput(iftttConnector);
        }

        if (props.getProperty("cmd.wakeup") != null || props.getProperty("cmd.wakeupDone") != null) {
            CmdConnector cmdConnector = new CmdConnector(props.getProperty("cmd.wakeup"), props.getProperty("cmd.wakeupDone"));
            WiringComponent.getInstance().registerWakeupOutput(cmdConnector);
        }
    }
}
