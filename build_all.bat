@echo off
SETLOCAL

echo ============================================
echo   🔹 MeowAuth: Build All Modules
echo ============================================

:: 1. Сборка common
echo.
echo 🔹 Building meowauth-common...
cd meowauth-common
gradlew clean build
if %ERRORLEVEL% neq 0 (
    echo ❌ Failed to build meowauth-common
    exit /b %ERRORLEVEL%
)
cd ..

:: 2. Сборка server
echo.
echo 🔹 Building meowauth-server...
cd meowauth-server
gradlew clean build
if %ERRORLEVEL% neq 0 (
    echo ❌ Failed to build meowauth-server
    exit /b %ERRORLEVEL%
)
cd ..

:: 3. Сборка client
echo.
echo 🔹 Building meowauth-client...
cd meowauth-client
gradlew clean build
if %ERRORLEVEL% neq 0 (
    echo ❌ Failed to build meowauth-client
    exit /b %ERRORLEVEL%
)
cd ..

echo.
echo ============================================
echo   ✅ All modules built successfully!
echo ============================================
pause
ENDLOCAL
