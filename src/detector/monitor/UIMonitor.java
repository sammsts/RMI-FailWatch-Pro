package detector.monitor;

import java.util.*;
import java.util.concurrent.*;

public class UIMonitor {
    private final ConcurrentMap<String, String> nodeMetrics;
    private final Set<String> suspectedNodes;
    // Novo: mapa de eventos recentes (nó -> [evento, timestamp])
    private static final Map<String, String[]> eventosRecentes = new ConcurrentHashMap<>();
    // Novo: lista de eventos para feed
    private static final Deque<String> feedEventos = new ArrayDeque<>();
    private static final int FEED_SIZE = 5;

    public UIMonitor(ConcurrentMap<String, String> nodeMetrics, Set<String> suspectedNodes) {
        this.nodeMetrics = nodeMetrics;
        this.suspectedNodes = suspectedNodes;
    }

    // Novo: método para registrar eventos
    public static void registrarEvento(String node, String evento) {
        String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        eventosRecentes.put(node, new String[]{evento, ts});
        synchronized (feedEventos) {
            String cor = evento.equals("FALHA") ? "\u001B[31m" : "\u001B[33m";
            feedEventos.addFirst(cor + ts + ": " + evento + " em " + node + "\u001B[0m");
            while (feedEventos.size() > FEED_SIZE) feedEventos.removeLast();
        }
    }

    public void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            // Limpa o terminal (ANSI escape code)
            System.out.print("\033[H\033[2J");
            System.out.flush();
            // Resumo global
            int ativos = 0, suspeitos = 0;
            for (String node : nodeMetrics.keySet()) {
                if (suspectedNodes.contains(node)) suspeitos++; else ativos++;
            }
            String agora = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            System.out.println("================ MONITOR DE RECURSOS DISTRIBUÍDOS ================");
            System.out.printf("Nós ativos: %d | Suspeitos: %d | Última atualização: %s\n", ativos, suspeitos, agora);
            System.out.println("Legenda: \u001B[32mATIVO\u001B[0m | \u001B[31mSUSPEITO\u001B[0m | \u001B[33mRECUPERADO\u001B[0m | \u001B[31mFALHA\u001B[0m");
            System.out.println("------------------------------------------------------------------");
            System.out.printf("%-20s %-10s %-15s %-25s %-15s %-15s %-15s\n", "NÓ", "STATUS", "CPU", "MEMÓRIA (usada/total)", "Última métrica", "Tempo", "Último Evento");
            System.out.println("------------------------------------------------------------------");
            // Ordenação: SUSPEITO primeiro, depois por tempo desde último heartbeat
            List<Map.Entry<String, String>> lista = new ArrayList<>(nodeMetrics.entrySet());
            lista.sort((a, b) -> {
                boolean sa = suspectedNodes.contains(a.getKey());
                boolean sb = suspectedNodes.contains(b.getKey());
                if (sa != sb) return sa ? -1 : 1;
                long ta = getLastTs(a.getValue());
                long tb = getLastTs(b.getValue());
                return Long.compare(tb, ta); // mais recente primeiro
            });
            for (Map.Entry<String, String> entry : lista) {
                String node = entry.getKey();
                String status = suspectedNodes.contains(node) ? "\u001B[31mSUSPEITO\u001B[0m" : "\u001B[32mATIVO\u001B[0m";
                String[] parts = entry.getValue().split(" ");
                String cpu = "?", mem = "?", total = "?", ts = "?";
                for (String p : parts) {
                    if (p.startsWith("CPU:")) cpu = p.substring(4);
                    if (p.startsWith("MEM:")) {
                        String[] memParts = p.substring(4).split("/");
                        mem = memParts[0];
                        total = memParts.length > 1 ? memParts[1] : "?";
                    }
                    if (p.startsWith("TIMESTAMP:")) ts = p.substring(10);
                }
                String tsFmt = "?", tempo = "?";
                try {
                    long t = Long.parseLong(ts);
                    tsFmt = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(t));
                    long diff = (System.currentTimeMillis() - t) / 1000;
                    tempo = "há " + diff + "s";
                } catch (Exception ignored) {}
                // Último evento
                String evento = "-", corEvento = "";
                String[] ev = eventosRecentes.get(node);
                if (ev != null) {
                    evento = ev[0];
                    corEvento = evento.equals("FALHA") ? "\u001B[31m" : "\u001B[33m";
                    evento = corEvento + evento + "\u001B[0m";
                }
                System.out.printf("%-20s %-10s %-15s %-25s %-15s %-15s %-15s\n", node, status, cpu, mem+"/"+total, tsFmt, tempo, evento);
            }
            System.out.println("------------------------------------------------------------------");
            // Feed de eventos recentes
            System.out.println("\nÚltimos eventos:");
            synchronized (feedEventos) {
                for (String ev : feedEventos) System.out.println(ev);
            }
            System.out.println("------------------------------------------------------------------\n");
        }, 0, 5, TimeUnit.SECONDS);
    }

    // Auxiliar para ordenação
    private static long getLastTs(String metric) {
        try {
            for (String p : metric.split(" ")) if (p.startsWith("TIMESTAMP:")) return Long.parseLong(p.substring(10));
        } catch (Exception ignored) {}
        return 0;
    }
}
