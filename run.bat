@echo off
REM Suppress Java warnings when running Maven commands
set MAVEN_OPTS=-Xmx512m --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED

echo Running %1 application...
mvn -DskipTests exec:java@%1
