package detector.simulador;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.*;

public class NodeSimulator {
    private static boolean ativo = true;
    private static final String MONITOR_IP = "172.20.0.10";
    private static final int METRIC_PORT = 6000;
    private static final int METRIC_INTERVAL = 3000; // ms
    private static int porta;
    private static volatile boolean aguardandoMonitor = false;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Uso: java detector.simulador.NodeSimulator <porta>");
            return;
        }

        porta = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(porta);
        System.out.println("Nó ativo na porta " + porta);

        // Thread para lidar com comandos do usuário (falhar / voltar)
        Thread comandoThread = new Thread(() -> {
            if (System.console() == null) {
                // Não há terminal interativo, não imprime prompt nem lê comandos
                return;
            }
            Scanner scanner = new Scanner(System.in);
            String status = "\u001B[32mATIVO\u001B[0m";
            System.out.println("[INFO] Nó iniciado. Status atual: " + status);
            while (true) {
                try {
                    if (!aguardandoMonitor) {
                        System.out.println("\n[NodeSimulator] Digite 'falhar' para simular falha, 'voltar' para reativar, 'status' para ver o status atual ou 'ajuda' para comandos:");
                    }
                    if (!scanner.hasNextLine()) {
                        Thread.sleep(500);
                        continue;
                    }
                    String cmd = scanner.nextLine().trim().toLowerCase();
                    switch (cmd) {
                        case "falhar":
                            ativo = false;
                            status = "\u001B[31mFALHO\u001B[0m";
                            System.out.println("\u001B[41;97m[NodeSimulator] Nó está simulado como falho (sem resposta). Status: " + status + "\u001B[0m");
                            break;
                        case "voltar":
                            ativo = true;
                            status = "\u001B[32mATIVO\u001B[0m";
                            System.out.println("\u001B[42;30m[NodeSimulator] Nó reativado. Status: " + status + "\u001B[0m");
                            break;
                        case "status":
                            System.out.println("[NodeSimulator] Status atual: " + status);
                            break;
                        case "ajuda":
                            System.out.println("\nComandos disponíveis:\n  falhar  - Simula falha do nó (para de enviar métricas)\n  voltar  - Reativa o nó\n  status  - Mostra o status atual\n  ajuda   - Mostra esta ajuda\n");
                            break;
                        default:
                            System.out.println("[NodeSimulator] Comando desconhecido. Use 'falhar', 'voltar', 'status' ou 'ajuda'.");
                    }
                    // Sempre mostra status após comando
                    System.out.println("[NodeSimulator] Status atual: " + status);
                } catch (Exception e) {
                    System.out.println("[NodeSimulator] Erro no comando do usuário: " + e.getMessage());
                }
            }
        });
        comandoThread.setDaemon(true);
        comandoThread.start();

        // Thread para enviar métricas periodicamente ao monitor
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (ativo) {
                int maxBackoff = 30000; // 30 segundos máximo
                int backoff = 2000; // começa com 2 segundos
                int tentativasFalha = 0;
                boolean enviado = false;
                while (!enviado && ativo) {
                    try {
                        aguardandoMonitor = false;
                        // Coleta métricas
                        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                        double cpuLoad = -1;
                        long freeMem = -1;
                        long totalMem = -1;
                        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                            com.sun.management.OperatingSystemMXBean sunOs = (com.sun.management.OperatingSystemMXBean) osBean;
                            cpuLoad = sunOs.getCpuLoad();
                            freeMem = sunOs.getFreeMemorySize();
                            totalMem = sunOs.getTotalMemorySize();
                        }
                        String localIp = InetAddress.getLocalHost().getHostAddress();
                        String metric = String.format("%s:%d CPU:%.2f MEM:%d/%d TIMESTAMP:%d", localIp, porta, cpuLoad, (totalMem-freeMem), totalMem, System.currentTimeMillis());
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(MONITOR_IP, METRIC_PORT), 2000); // timeout 2s
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(metric);
                        socket.close();
                        if (tentativasFalha > 0) {
                            System.out.println("[NodeSimulator] Conexão restabelecida. Métrica enviada ao monitor.");
                        } else {
                            System.out.println("[NodeSimulator] Métrica enviada ao monitor com sucesso.");
                        }
                        enviado = true;
                        tentativasFalha = 0;
                        backoff = 2000;
                    } catch (Exception e) {
                        tentativasFalha++;
                        aguardandoMonitor = true;
                        if (tentativasFalha == 1) {
                            System.out.println("[NodeSimulator] Monitor indisponível. Aguardando para tentar novamente...");
                        }
                        try {
                            Thread.sleep(backoff);
                        } catch (InterruptedException ie) {
                            // Ignora
                        }
                        backoff = Math.min(backoff * 2, maxBackoff); // backoff exponencial
                    }
                }
                aguardandoMonitor = false;
            }
        }, 0, METRIC_INTERVAL, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Thread principal aceita conexões do monitor (mantém para compatibilidade)
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg = in.readLine();
                    // ...pode ignorar, pois agora o monitor não envia heartbeats...
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
