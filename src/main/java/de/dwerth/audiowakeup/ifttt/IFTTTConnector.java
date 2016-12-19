package de.dwerth.audiowakeup.ifttt;

import de.dwerth.audiowakeup.output.IWakeupOutput;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IFTTTConnector implements IWakeupOutput {

    private static final Logger log = Logger.getLogger(IFTTTConnector.class);
    private String makerUrl;

    public IFTTTConnector(String makerUrl) {
        this.makerUrl = makerUrl;
    }

    @Override
    public void triggerWakeup() {
        try {
            sendGet(makerUrl);
        } catch (Exception e) {
            log.error("Could not trigger IFTTT: " + e.getMessage());
        }
    }

    @Override
    public void triggerWakeupDone() {

    }

    private void sendGet(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Java Audio-Wakeup");
        int responseCode = con.getResponseCode();
        log.info("Sending 'GET' request to URL : " + url);
        log.debug("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        log.debug("Response from IFTTT: " + response.toString());
    }

}
