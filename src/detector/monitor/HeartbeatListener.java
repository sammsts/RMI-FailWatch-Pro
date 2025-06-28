package detector.monitor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class HeartbeatListener {
    private final Map<String, Long> timestamps;
    private final Set<String> suspects;
    private final PrintWriter log;

    public HeartbeatListener(Map<String, Long> timestamps, Set<String> suspects, PrintWriter log) {
        this.timestamps = timestamps;
        this.suspects = suspects;
        this.log = log;
    }

    public void start() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(5000)) {
                log("Monitor ouvindo na porta 5000");
                while (true) {
                    Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg = in.readLine();
                    if (msg != null && msg.startsWith("HEARTBEAT")) {
                        String[] parts = msg.split(" ");
                        if (parts.length == 2) {
                            String node = parts[1];
                            log("HEARTBEAT recebido de: " + node);

                            timestamps.put(node, System.currentTimeMillis());
                            if (suspects.remove(node)) {
                                log("[RECUPERAÇÃO] Nó voltou: " + node);
                            }
                        } else {
                            log("Mensagem HEARTBEAT mal formatada: " + msg);
                        }
                    }
                }
            } catch (IOException e) {
                log("Erro no listener: " + e.getMessage());
            }
        });
    }

    private void log(String msg) {
        String entry = String.format("[%s] %s", new Date(), msg);
        System.out.println(entry);
        log.println(entry);
        log.flush();
    }
}