@echo off
rem =====================================================================
rem KnowFlow - cópia de segurança diária do banco (RNF11)
rem
rem Gera um arquivo .backup (formato do pg_dump) por dia na pasta BACKUPS
rem e apaga os com mais de 30 dias. Os anexos (RF16) ficam no banco,
rem então também entram na cópia.
rem
rem Agendar para rodar todo dia às 23h (Prompt de Comando como administrador):
rem   schtasks /create /tn "KnowFlow backup" /sc daily /st 23:00 /tr "C:\caminho\backup-diario.bat"
rem
rem Restaurar uma cópia (pgAdmin > botão direito no banco > Restore, ou):
rem   pg_restore -U postgres -d fluxoconhecimento --clean "C:\KnowFlow\backups\knowflow-AAAA-MM-DD.backup"
rem =====================================================================

rem Ajuste estes valores ao seu computador
set PGBIN=C:\Program Files\PostgreSQL\16\bin
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set PGPASSWORD=postgres
set BANCO=fluxoconhecimento
set BACKUPS=C:\KnowFlow\backups

if not exist "%BACKUPS%" mkdir "%BACKUPS%"

rem Data no formato AAAA-MM-DD (independe do idioma do Windows)
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd"') do set HOJE=%%d

"%PGBIN%\pg_dump.exe" -h %PGHOST% -p %PGPORT% -U %PGUSER% -F c -f "%BACKUPS%\knowflow-%HOJE%.backup" %BANCO%
if errorlevel 1 (
    echo [%date% %time%] ERRO no backup do KnowFlow >> "%BACKUPS%\backup.log"
    exit /b 1
)
echo [%date% %time%] Backup gerado: knowflow-%HOJE%.backup >> "%BACKUPS%\backup.log"

rem Mantém apenas os últimos 30 dias
forfiles /p "%BACKUPS%" /m knowflow-*.backup /d -30 /c "cmd /c del @path" 2>nul
exit /b 0
