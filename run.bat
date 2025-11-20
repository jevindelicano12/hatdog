@echo off
set PATH=%PATH%;C:\maven\apache-maven-3.9.6\bin
echo ================================================
echo   Coffee Shop Management System - Launcher
echo ================================================
echo.
echo Select which application to run:
echo.
echo   1. Customer Interface
echo   2. Cashier Interface
echo   3. Admin Interface
echo   4. Build Project First
echo   5. Exit
echo.
set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" (
    echo.
    echo Starting Customer Interface...
    mvn exec:java@customer
) else if "%choice%"=="2" (
    echo.
    echo Starting Cashier Interface...
    mvn exec:java@cashier
) else if "%choice%"=="3" (
    echo.
    echo Starting Admin Interface...
    mvn exec:java@admin
) else if "%choice%"=="4" (
    echo.
    echo Building project...
    mvn clean compile
    echo.
    echo Build complete! Run this script again to start an application.
    pause
) else if "%choice%"=="5" (
    echo.
    echo Goodbye!
    exit
) else (
    echo.
    echo Invalid choice. Please run the script again.
    pause
)
