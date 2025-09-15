@echo off

REM Detect Maven command (mvnd or mvn)
set MAVEN_CMD=
set FOUND_MAVEN=false
where mvnd >nul 2>nul
if %ERRORLEVEL%==0 (
    set MAVEN_CMD=mvnd
    set FOUND_MAVEN=true
    echo [INFO] Using mvnd (Maven Daemon)
) else (
    for /f "tokens=*" %%i in ('where mvn 2^>nul') do (
        set MAVEN_CMD=mvn
        set FOUND_MAVEN=true
        echo [INFO] Using mvn from: %%i
    )
)

if "%FOUND_MAVEN%"=="false" (
    echo [INFO] No Maven found in PATH, downloading Maven Daemon...
    if not exist "maven-mvnd-1.0.2-windows-amd64" (
        powershell -Command "Invoke-WebRequest -OutFile mvnd.zip https://dlcdn.apache.org/maven/mvnd/1.0.2/maven-mvnd-1.0.2-windows-amd64.zip"
        powershell -Command "Expand-Archive -Path mvnd.zip -DestinationPath ."
        del mvnd.zip
    )
    set MAVEN_CMD=%CD%\maven-mvnd-1.0.2-windows-amd64\bin\mvnd.cmd
    echo [INFO] Using downloaded Maven Daemon: %MAVEN_CMD%
)

REM Get version from pom.xml
for /f "delims=" %%v in ('powershell -Command "Select-String -Path pom.xml -Pattern '<version>' | Select-Object -First 1 | ForEach-Object { $_.Line -replace '.*<version>(.*)</version>.*','$1' }"') do set APP_VERSION=%%v
echo [INFO] Detected version: %APP_VERSION%

set APP_NAME=StreamWarden_Win_Portable_v%APP_VERSION%

REM Check JavaFX SDK presence
set JAVAFX_SDK_PATH=C:\Program Files\Java\javafx-sdk-24.0.2\lib
if not exist "%JAVAFX_SDK_PATH%" (
    echo [INFO] JavaFX SDK not found in Program Files, downloading to project root...
    powershell -Command "Invoke-WebRequest -OutFile javafx.zip https://download2.gluonhq.com/openjfx/24.0.2/openjfx-24.0.2_windows-x64_bin-sdk.zip"
    powershell -Command "Expand-Archive -Path javafx.zip -DestinationPath javafx-sdk-24.0.2"
    del javafx.zip
    set JAVAFX_SDK_PATH=%CD%\javafx-sdk-24.0.2\javafx-sdk-24.0.2\lib
)

REM Check if bin/windows exists
if not exist bin\windows (
    echo [INFO] Creating bin/windows structure...
    mkdir bin\windows

    REM Download and extract Streamlink portable
    echo [INFO] Downloading Streamlink portable...
    powershell -Command "Invoke-WebRequest -OutFile streamlink.zip https://github.com/streamlink/windows-builds/releases/download/7.6.0-1/streamlink-7.6.0-1-py313-x86_64.zip"
    powershell -Command "Expand-Archive -Path streamlink.zip -DestinationPath bin\windows"
    del streamlink.zip

    REM Prepare JavaFX native DLLs
    if exist "%CD%\javafx-sdk-24.0.2\javafx-sdk-24.0.2\bin" (
        echo [INFO] Copying JavaFX native DLLs from project root SDK...
        mkdir bin\windows\javafx-natives
        xcopy /Y /S "%CD%\javafx-sdk-24.0.2\javafx-sdk-24.0.2\bin" bin\windows\javafx-natives\
    ) else (
        echo [INFO] Downloading JavaFX native DLLs...
        powershell -Command "Invoke-WebRequest -OutFile javafx.zip https://download2.gluonhq.com/openjfx/24.0.2/openjfx-24.0.2_windows-x64_bin-sdk.zip"
        powershell -Command "Expand-Archive -Path javafx.zip -DestinationPath bin\windows"
        del javafx.zip
        mkdir bin\windows\javafx-natives
        xcopy /Y /S bin\windows\javafx-sdk-24.0.2\bin\* bin\windows\javafx-natives\
        rmdir /S /Q bin\windows\javafx-sdk-24.0.2
    )
)

REM Remove previous build folders if they exist
if exist target rmdir /S /Q target
if exist %APP_NAME% rmdir /S /Q %APP_NAME%

REM Build the fat jar with all dependencies
call %MAVEN_CMD% clean package

REM Generate the portable app-image with jpackage
call jpackage --type app-image ^
  --input target ^
  --name %APP_NAME% ^
  --main-jar StreamWarden-%APP_VERSION%-jar-with-dependencies.jar ^
  --main-class com.yougo.streamwarden.Main ^
  --module-path "%JAVAFX_SDK_PATH%" ^
  --add-modules javafx.controls,javafx.fxml ^
  --java-options "-Dprism.order=sw" ^
  --java-options "--enable-native-access=javafx.graphics" ^
  --java-options "-Djava.library.path=app/bin/windows/javafx-natives" ^
  --icon assets/images/app_icon.ico
REM  --win-console

REM Remove the target folder as it is no longer needed
if exist target rmdir /S /Q target

echo Build complete. You can now run %APP_NAME%.exe from the %APP_NAME% folder.
if not "%CI_BUILD%"=="true" pause