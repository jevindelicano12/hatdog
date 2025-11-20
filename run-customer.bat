@echo off
set PATH=%PATH%;C:\maven\apache-maven-3.9.6\bin
echo Starting Customer Interface...
mvn exec:java@customer
pause
