@echo off
REM Script para abrir monitor, N nodes e interface web em terminais separados (Windows)
set /p NODES=Quantos nodes deseja simular? 
start "Monitor" cmd /k "docker-compose up monitor"
for /L %%i in (1,1,%NODES%) do (
    start "Node%%i" cmd /k "docker-compose run --rm -it node%%i"
)
start "WebMonitor" cmd /k "python web_monitor.py"
