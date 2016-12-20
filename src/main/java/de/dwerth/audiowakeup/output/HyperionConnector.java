package de.dwerth.audiowakeup.output;

import de.dwerth.audiowakeup.main.WiringComponent;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
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

    public HyperionConnector(int priority, String serverHost, int serverPort) {
        this.priority = priority;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
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
        if (brightness < 255) {
            brightness += 1;
            send("{ \"color\": [" + brightness + "," + brightness + "," + brightness + "], \"command\": \"color\", \"priority\": " + priority + " }");
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
                    log.info("Sending: " + command);
                } catch (IOException e) {
                    connected = false;
                    e.printStackTrace();
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
                    if (socket != null && connected && inputStreamReader != null) {
                        try {
                            log.debug("Hyperion response: " + inputStreamReader.readLine());
                        } catch (IOException e) {
                            e.printStackTrace();
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
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
