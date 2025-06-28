from flask import Flask, render_template_string, jsonify, send_file, request
import threading
import time
import os
import csv

LOG_PATH_HOST = os.path.join('src', 'logs', 'failwatch_log_host.txt')
LOG_PATH_DOCKER = os.path.join('src', 'logs', 'failwatch_log.txt')
CONFIG_PATH = os.path.join('src', 'logs', 'config.txt')

app = Flask(__name__)

# Configuração dinâmica (timeout e intervalo)
def read_config():
    config = {'timeout': 15, 'interval': 3}
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH) as f:
            for line in f:
                if '=' in line:
                    k, v = line.strip().split('=', 1)
                    if k in config:
                        config[k] = float(v)
    return config

def write_config(timeout, interval):
    with open(CONFIG_PATH, 'w') as f:
        f.write(f'timeout={timeout}\ninterval={interval}\n')

@app.route('/')
def index():
    return render_template_string('''
    <html><head>
    <title>FailWatch Web Monitor</title>
    <meta http-equiv="refresh" content="10">
    <style>
    body { font-family: Arial; background: #222; color: #eee; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #444; padding: 6px; text-align: left; }
    th { background: #333; }
    .suspeito { background: #a33; color: #fff; }
    .recuperado { background: #ffb300; color: #222; }
    .ativo { background: #2e7d32; color: #fff; }
    </style>
    </head><body>
    <h2>FailWatch - Monitor Web</h2>
    <p><a href="/export/csv">Exportar CSV</a> | <a href="/export/json">Exportar JSON</a></p>
    <form method="post" action="/config">
      Timeout (s): <input name="timeout" value="{{config['timeout']}}" size=3>
      Intervalo (s): <input name="interval" value="{{config['interval']}}" size=3>
      <button type="submit">Salvar</button>
    </form>
    <h3>Eventos Recentes</h3>
    <table>
      <tr><th>Data/Hora</th><th>Evento</th></tr>
      {% for e in eventos[-20:] %}
      <tr class="{{e[2]}}"><td>{{e[0]}}</td><td>{{e[1]}}</td></tr>
      {% endfor %}
    </table>
    </body></html>
    ''', eventos=get_events(), config=read_config())

@app.route('/export/csv')
def export_csv():
    return send_file(get_log_path(), mimetype='text/csv', as_attachment=True, download_name='failwatch_log.csv')

@app.route('/export/json')
def export_json():
    events = get_events()
    return jsonify([
        {'timestamp': e[0], 'evento': e[1], 'tipo': e[2]} for e in events
    ])

@app.route('/config', methods=['POST'])
def config():
    timeout = request.form.get('timeout', '15')
    interval = request.form.get('interval', '3')
    write_config(timeout, interval)
    return '<script>window.location="/"</script>'

def get_log_path():
    if os.path.exists(LOG_PATH_HOST):
        return LOG_PATH_HOST
    return LOG_PATH_DOCKER

def get_events():
    eventos = []
    log_path = get_log_path()
    if os.path.exists(log_path):
        with open(log_path, encoding='utf-8') as f:
            for line in f:
                if '[SUSPEITA]' in line:
                    eventos.append((line[:25], 'FALHA ' + line.strip().split(':',1)[-1], 'suspeito'))
                elif '[RECUPERAÇÃO]' in line:
                    eventos.append((line[:25], 'RECUPERADO ' + line.strip().split(':',1)[-1], 'recuperado'))
    return eventos

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)
