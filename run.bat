@echo off
echo ==============================================================
echo        Compiling and Launching Smart Bill Splitter
echo ==============================================================
cd /d "%~dp0"
echo Compiling Java source files...
javac billsplitter/*.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed. Please ensure JDK 11+ is installed.
    pause
    exit /b %errorlevel%
)
echo Launching Smart Bill Splitter...
java billsplitter.Main %*
pause
