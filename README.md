# RMI FailWatch Pro – Sistema Distribuído de Monitoramento de Recursos

## Visão Geral

Este projeto implementa um sistema distribuído de monitoramento de falhas e recursos, com dashboard interativo em terminal, simulação de nós, tolerância a falhas, interface web e fácil apresentação via Docker Compose.

- **Monitor**: Centraliza o recebimento de métricas, detecta falhas, exibe dashboard e mantém log de eventos.
- **NodeSimulator**: Simula um nó, envia métricas de CPU/memória, permite simular falha/recuperação via comandos.
- **Dashboard (Terminal)**: Interface em terminal com destaque visual, feed de eventos, ordenação automática, tempo desde último heartbeat e resumo global.
- **Interface Web**: Visualização dos eventos recentes, exportação de logs, configuração dinâmica de timeout/intervalo.

## Funcionalidades

- **Detecção de falhas distribuída** (timeout configurável)
- **Dashboard em terminal** com:
  - Destaque visual para eventos recentes (RECUPERADO/FALHA)
  - Coluna "Último Evento" e feed de eventos
  - Tempo desde o último heartbeat
  - Ordenação automática dos nós (SUSPEITO primeiro)
  - Resumo global no topo
- **NodeSimulator**:
  - Comandos interativos: `falhar`, `voltar`, `status`, `ajuda`
  - Mensagens coloridas e status visual
- **Interface Web (Flask)**:
  - Visualização dos eventos recentes
  - Exportação de logs em CSV/JSON
  - Configuração dinâmica de timeout/intervalo
- **Logs de eventos**: em `src/logs/failwatch_log.txt`
- **Fácil execução via Docker Compose**

## Como Rodar

### Pré-requisitos
- Docker e Docker Compose instalados
- Python 3 e Flask (`pip install flask`) para interface web
- (Opcional) Java 17+ para rodar localmente sem Docker

### Passos

1. **Build das imagens**
   ```sh
   docker-compose build --no-cache
   ```

2. **Suba o monitor** (em um terminal):
   ```sh
   docker-compose up monitor
   ```

3. **Suba cada nó em terminais separados (modo interativo):**
   ```sh
   docker-compose run --rm --service-ports -it node1
   docker-compose run --rm --service-ports -it node2
   docker-compose run --rm --service-ports -it node3
   ```
   > Dica: Use `-it` para poder digitar comandos nos nós.

4. **Comandos do NodeSimulator:**
   - `falhar` – Simula falha do nó (para de enviar métricas)
   - `voltar` – Reativa o nó
   - `status` – Mostra o status atual
   - `ajuda` – Mostra todos os comandos

5. **Dashboard/Monitor (Terminal):**
   - Mostra status dos nós, tempo desde último heartbeat, eventos recentes e feed de falhas/recuperações.
   - Resumo global no topo.
   - Ordenação automática: SUSPEITO primeiro.
   - Logs de eventos em `src/logs/failwatch_log.txt`.

6. **Interface Web:**
   - Instale Flask: `pip install flask`
   - Execute: `python web_monitor.py`
   - Acesse: [http://localhost:8080](http://localhost:8080)
   - Visualize eventos recentes, exporte logs (CSV/JSON) e altere timeout/intervalo pelo navegador.

7. **Configuração dinâmica:**
   - Timeout e intervalo podem ser alterados via interface web (salvos em `src/logs/config.txt`).
   - (Opcional) Adapte o monitor Java para ler `config.txt` e aplicar dinamicamente.

8. **Exportação de logs:**
   - Baixe o log em CSV ou JSON pela interface web.

9. **Resetar ambiente:**
   - `docker-compose down -v` para remover containers e volumes.
   - Apague logs manualmente se desejar.

## Como Funciona

- Cada nó envia métricas periodicamente para o monitor.
- O monitor detecta falhas se não receber métricas dentro do timeout.
- Eventos de falha/recuperação são destacados no dashboard e logados.
- O usuário pode simular falha/recuperação dos nós em tempo real.
- A interface web lê o log e permite exportação/configuração dinâmica.

## Estrutura do Projeto
```
├── docker-compose.yml
├── Dockerfile
├── src/
│   ├── detector/
│   │   ├── monitor/
│   │   │   ├── NodeMonitor.java
│   │   │   ├── UIMonitor.java
│   │   │   ├── FailureDetector.java
│   │   │   ├── ...
│   │   ├── simulador/
│   │   │   └── NodeSimulator.java
│   ├── logs/
│   │   └── failwatch_log.txt
├── web_monitor.py
```

## Observações
- O sistema é facilmente extensível para mais nós ou integrações.
- Para healthcheck no Docker Compose, adicione:
  ```yaml
  healthcheck:
    test: ["CMD", "nc", "-z", "localhost", "5000"]
    interval: 5s
    timeout: 2s
    retries: 5
  ```
- Para limpar logs: apague o arquivo em `src/logs/failwatch_log.txt`.

---

Dúvidas ou sugestões? Abra uma issue ou entre em contato!
