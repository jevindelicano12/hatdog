@echo off
set PATH=%PATH%;C:\maven\apache-maven-3.9.6\bin
echo ================================================
echo   Coffee Shop System - Build Verification
echo ================================================
echo.
echo Checking Java installation...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)
echo.

echo Checking Maven installation...
mvn -version
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven 3.6 or higher
    pause
    exit /b 1
)
echo.

echo ================================================
echo Building project...
echo ================================================
mvn clean compile
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Build failed
    echo Please check the error messages above
    pause
    exit /b 1
)

echo.
echo ================================================
echo   BUILD SUCCESSFUL!
echo ================================================
echo.
echo Your Coffee Shop Management System is ready!
echo.
echo To run the applications:
echo   - Customer: run-customer.bat or mvn exec:java@customer
echo   - Cashier:  run-cashier.bat or mvn exec:java@cashier
echo   - Admin:    run-admin.bat or mvn exec:java@admin
echo.
echo Or use run.bat for an interactive menu
echo.
pause
