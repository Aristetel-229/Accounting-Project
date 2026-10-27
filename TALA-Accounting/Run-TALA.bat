@echo off
title TALA
cd /d "%~dp0"
mvn -q clean compile exec:java
pause
