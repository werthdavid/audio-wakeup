package de.dwerth.audiowakeup.output;

import de.dwerth.audiowakeup.main.WiringComponent;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantLock;

public class HyperionConnector implements IWakeupOutput {

    private static final Logger log = Logger.getLogger(HyperionConnector.class);

    private Socket socket = null;
    private boolean connected = false;
    private ReentrantLock lock = new ReentrantLock();
    private int brightness = 0;
    private boolean clearCommandSent = false;
    private BufferedReader inputStreamReader = null;
    private long lastSendTS = 0;

    private int priority;
    private String serverHost;
    private int serverPort;
    private int colorH;
    private int colorS;
    private int colorB;

    public HyperionConnector(int priority, String serverHost, int serverPort, int colorR, int colorG, int colorB) {
        this.priority = priority;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        float[] hsb = Color.RGBtoHSB(colorR, colorG, colorB, null);
        this.colorH = (int) hsb[0];
        this.colorS = (int) hsb[1];
        this.colorB = (int) hsb[2];

        getConnectionThread().start();
        if (log.isDebugEnabled()) {
            getOutputThread().start();
        }
        getHandlerThread().start();
    }

    public void serverInfo() {
        send("{ \"command\": \"serverinfo\" }");
    }

    public void increaseBrightness() {
        if (brightness < colorB) {
            brightness += 1;
            Color col = new Color(Color.HSBtoRGB(colorH, colorS, brightness));
            send("{ \"color\": [" + col.getRed() + "," + col.getBlue() + "," + col.getGreen() + "], " +
                    "\"command\": \"color\", \"priority\": " + priority + " }");
        }
    }


    public void sendClear() {
        send("{\"command\":\"clear\",\"priority\":" + priority + "}");
        brightness = 0;
    }

    private void send(String command) {
        lock.lock();
        try {
            if (!connected) {
                connect();
            }
            if (connected) {
                try {
                    lastSendTS = System.currentTimeMillis();
                    socket.getOutputStream().write((command + "\n").getBytes());
                    log.debug("Sending to Hyperion: " + command);
                } catch (IOException e) {
                    connected = false;
                    log.error("IOException while sending to Hyperion: " + e.getMessage());
                }
            } else {
                log.warn("Not sending command to hyperion: not connected");
            }
        } finally {
            lock.unlock();
        }
    }

    private Thread getConnectionThread() {
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    // Disconnect if inactive for more than 2 mins
                    if (connected && System.currentTimeMillis() - lastSendTS > (2 * 60 * 1000)) {
                        disconnect();
                    }
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        return t;
    }

    private Thread getHandlerThread() {
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if (WiringComponent.getInstance().shouldIncreaseBrightness()) {
                        increaseBrightness();
                        clearCommandSent = false;
                    } else {
                        if (!clearCommandSent && connected) {
                            sendClear();
                            clearCommandSent = true;
                            brightness = 0;
                        }
                    }
                    if (socket != null && socket.isClosed()) {
                        disconnect();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        return t;
    }

    private Thread getOutputThread() {
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if (socket != null && !socket.isClosed() && connected && inputStreamReader != null) {
                        try {
                            log.debug("Hyperion response: " + inputStreamReader.readLine());
                        } catch (SocketException se) {
                            disconnect();
                        } catch (IOException e) {
                            log.error("IOException while reading Hyperion response: " + e.getMessage());
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        return t;
    }

    private void disconnect() {
        try {
            if (connected) {
                sendClear();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("IOException on disconnect: " + e.getMessage());
        } finally {
            socket = null;
            connected = false;
            inputStreamReader = null;
        }
    }

    private void connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            connected = true;
            inputStreamReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverInfo();
        } catch (ConnectException ce) {
            log.warn(ce.getMessage());
        } catch (IOException e) {
            log.error("IOException on connect: " + e.getMessage());
            connected = false;
        }
    }

    @Override
    public void triggerWakeup() {
        // No Action required
    }

    @Override
    public void triggerWakeupDone() {
        // No Action required
    }
}
