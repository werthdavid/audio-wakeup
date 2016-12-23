package de.dwerth.audiowakeup.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class CmdConnector implements IWakeupOutput {

    private static final Logger log = Logger.getLogger(CmdConnector.class);

    private String wakeup;
    private String wakeupDone;

    public CmdConnector(String wakeup, String wakeupDone) {
        this.wakeup = wakeup;
        this.wakeupDone = wakeupDone;
    }

    @Override
    public void triggerWakeup() {
        if (wakeup != null) {
            exec(wakeup);
        }
    }

    @Override
    public void triggerWakeupDone() {
        if (wakeupDone != null) {
            exec(wakeupDone);
        }
    }

    private void exec(String command) {
        try {
            String[] commandArr = command.split(" ");
            ProcessBuilder processBuilder = new ProcessBuilder(commandArr);
            processBuilder.directory(new File("."));

            Process process = processBuilder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                log.debug(line);
            }

            //Wait to get exit value
            try {
                int exitValue = process.waitFor();
            } catch (InterruptedException e) {
                log.error("Waiting for command execution interrupted: " + e.getMessage());
            }
        } catch (IOException e) {
            log.error("Problem executing shell command: " + e.getMessage());
        }

    }
}
