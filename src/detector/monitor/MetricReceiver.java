package detector.monitor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class MetricReceiver {
    private final ConcurrentMap<String, String> nodeMetrics;
    private final PrintWriter log;
    private final Map<String, Long> heartbeatTimestamps;

    public MetricReceiver(ConcurrentMap<String, String> nodeMetrics, PrintWriter log, Map<String, Long> heartbeatTimestamps) {
        this.nodeMetrics = nodeMetrics;
        this.log = log;
        this.heartbeatTimestamps = heartbeatTimestamps;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(6000)) {
                while (true) {
                    Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String metrics = in.readLine();
                    String nodeId = metrics.split(" ")[0]; // ip:porta
                    nodeMetrics.put(nodeId, metrics);
                    heartbeatTimestamps.put(nodeId, System.currentTimeMillis()); // Atualiza timestamp do nó
                }
            } catch (IOException e) {
                log("Erro no MetricReceiver: " + e.getMessage());
            }
        }).start();
    }

    private void log(String msg) {
        String entry = String.format("[%s] %s", new Date(), msg);
        System.out.println(entry);
        log.println(entry);
    }
}