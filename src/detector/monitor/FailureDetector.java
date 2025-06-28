package detector.monitor;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

public class FailureDetector {
    private final List<String> nodes;
    private final Map<String, Long> timestamps;
    private final Set<String> suspects;
    private final int timeout;
    private final PrintWriter log;

    public FailureDetector(List<String> nodes, Map<String, Long> timestamps, Set<String> suspects, int timeout, PrintWriter log) {
        this.nodes = nodes;
        this.timestamps = timestamps;
        this.suspects = suspects;
        this.timeout = timeout;
        this.log = log;
    }

    public void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (String node : nodes) {
                Long last = timestamps.getOrDefault(node, 0L);
                if (now - last > timeout) {
                    if (suspects.add(node)) {
                        log(String.format("[SUSPEITA] Falha detectada no nó: %s", node));
                        UIMonitor.registrarEvento(node, "FALHA");
                    }
                } else {
                    if (suspects.remove(node)) {
                        log(String.format("[RECUPERAÇÃO] Nó voltou: %s", node));
                        UIMonitor.registrarEvento(node, "RECUPERADO");
                    }
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void log(String msg) {
        String entry = String.format("[%s] %s", new Date(), msg);
        System.out.println(entry);
        log.println(entry);
    }
}
