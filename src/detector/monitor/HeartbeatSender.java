package detector.monitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.*;

public class HeartbeatSender {
    private final int interval;
    private final String monitorIp;
    private final int monitorPort;
    private final String localNodeId;

    public HeartbeatSender(int interval, String monitorIp, int monitorPort, String localNodeId) {
        this.interval = interval;
        this.monitorIp = monitorIp;
        this.monitorPort = monitorPort;
        this.localNodeId = localNodeId;
    }

    public void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(monitorIp, monitorPort), 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("HEARTBEAT " + localNodeId); // envia IP:porta do nó local
                socket.close();
            } catch (IOException ignored) {}
        }, 0, interval, TimeUnit.MILLISECONDS);
    }
}