@echo off
SETLOCAL

echo ============================================
echo   🔹 MeowAuth: Build All Modules
echo ============================================

:: Единая сборка через корневой gradlew — Gradle сам построит правильный порядок
call "%~dp0gradlew.bat" clean build
if %ERRORLEVEL% neq 0 (
    echo ❌ Build failed
    exit /b %ERRORLEVEL%
)

echo.
echo ============================================
echo   ✅ All modules built successfully!
echo ============================================
pause
ENDLOCAL
