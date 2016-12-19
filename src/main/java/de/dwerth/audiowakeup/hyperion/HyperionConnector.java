package de.dwerth.audiowakeup.hyperion;

import de.dwerth.audiowakeup.main.WiringComponent;
import de.dwerth.audiowakeup.output.IWakeupOutput;
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
    private BufferedReader in = null;

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
        brightness++;
        send("{ \"color\": [" + brightness + "," + brightness + "," + brightness + "], \"command\": \"color\", \"priority\": " + priority + " }");

    }

    public void sendClear() {
        send("{\"command\":\"clear\",\"priority\":" + priority + "}");
    }

    private void send(String command) {
        lock.lock();
        try {
            if (connected) {
                try {
                    socket.getOutputStream().write((command + "\n").getBytes());
                    log.info("Sending: " + command);
                } catch (IOException e) {
                    connected = false;
                    e.printStackTrace();
                }
            } else {
                log.warn("Not connected");
            }
        } finally {
            lock.unlock();
        }
    }

    private Thread getConnectionThread() {
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if (!connected) {
                        connect();
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

    private Thread getHandlerThread() {
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if (connected) {
                        if (WiringComponent.getInstance().shouldIncreaseBrightness()) {
                            increaseBrightness();
                            clearCommandSent = false;
                        } else {
                            if (!clearCommandSent) {
                                sendClear();
                                clearCommandSent = true;
                            }
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
                    if (socket != null && connected && in != null) {
                        try {
                            log.debug("Hyperion response: " + in.readLine());
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

    private void connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            connected = true;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
