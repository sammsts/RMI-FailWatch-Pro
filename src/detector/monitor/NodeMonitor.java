package detector.monitor;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class NodeMonitor {
    private final Map<String, Long> heartbeatTimestamps = new ConcurrentHashMap<>();
    private final Set<String> suspectedNodes = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, String> nodeMetrics = new ConcurrentHashMap<>();
    private final List<String> nodes;
    private final int heartbeatInterval;
    private final int timeout;
    private final PrintWriter logWriter;
    private final String monitorIp = "127.0.0.1"; // IP do monitor fixo
    private final int monitorPort = 5000; // Porta do monitor fixa

    public NodeMonitor(List<String> nodes, int heartbeatInterval, int timeout) throws IOException {
        this.nodes = nodes;
        this.heartbeatInterval = heartbeatInterval;
        // Aumenta o timeout para 15 segundos para evitar falsos positivos
        this.timeout = 15000;
        String cwd = new File("").getAbsolutePath();
        System.out.println("[DEBUG] Diretório de trabalho atual: " + cwd);
        new File("logs").mkdirs();
        String logPath;
        if (cwd.contains("/app") || cwd.contains("\\app")) {
            // Docker: salva em logs padrão
            logPath = "logs/failwatch_log.txt";
        } else {
            // Host: salva em src/logs/failwatch_log_host.txt
            new File("src/logs").mkdirs();
            logPath = "src/logs/failwatch_log_host.txt";
        }
        System.out.println("[DEBUG] Log será salvo em: " + logPath);
        this.logWriter = new PrintWriter(new FileWriter(logPath, true), true);
    }

    public void start() {
        // Passa o mapa de heartbeatTimestamps para o MetricReceiver
        new MetricReceiver(nodeMetrics, logWriter, heartbeatTimestamps).start();
        new HeartbeatListener(heartbeatTimestamps, suspectedNodes, logWriter).start();
        new FailureDetector(nodes, heartbeatTimestamps, suspectedNodes, timeout, logWriter).start();
        new UIMonitor(nodeMetrics, suspectedNodes).start();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java detector.monitor.NodeMonitor <ip1:porta1> <ip2:porta2> ...");
            return;
        }
        try {
            List<String> nodeList = Arrays.asList(args);
            NodeMonitor monitor = new NodeMonitor(nodeList, 3000, 7000);
            monitor.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
