###

<div align="center">

![StreamWarden icon](./assets/images/app_icon.png)

# StreamWarden : Auto Stream Recorder

Desktop app to automatically monitor Twitch / YouTube / Kick channels and record livestreams as soon as they go live.

</div>

###

<div align="center">

## ⚠️ Early Development Warning

**This application is currently in early development and has not had its first official release yet.**

While you can build and run the application in its current state, it contains only the basic minimum features for stream monitoring and recording. The functionality works but the user experience is still rough around the edges.

**Use at your own discretion** - expect bugs, missing features, and potential breaking changes.

</div>

###

<div align="center">

  ## Features:

</div>
  
- **Multi-Platform Support**: Monitor Twitch, YouTube and Kick channels simultaneously
- **Automatic Detection**: Start recording immediately when streams go live
- **Custom Quality Settings**: Choose recording quality per channel (best, 1080p, 720p, etc.)
- **Organized Storage**: Recordings are automatically sorted by channel in separate folders
- **Cross-Platform**: Works on Windows and Linux with automatic tool detection

<div align="center">

The application uses Streamlink under the hood for reliable stream capture and supports automatic monitoring with customizable check intervals.

<div align="center">

  ## Screenshot:

</div>

![App screenshot](./assets/images/app_screenshot.png)

</div>



###

<div align="center">
  
  # Build it yourself

</div>

  For now, the only way to run the app is to build it yourself.  

---

### Clone the repository
```
# Clone the repository
git clone https://github.com/YouG-o/StreamWarden.git
cd StreamWarden
```

### Then choose the method that fits your needs:

### 1. Development Build (Cross-platform)

#### Prerequisites
- Java Development Kit (JDK) 17 or higher (not just the JRE)
- Maven 3.6+
- **Streamlink** installed on your system (7.3.0 or higher for Kick support) (tested on 7.6.0)
- **Python** (required by Streamlink)

#### Installation & Run
```bash
# Build and run the application in development mode
mvn clean javafx:run
```
This will launch the app directly from source.  
You must have Streamlink and Python installed and available in your system PATH.

---

### 2. Windows: Build a Portable Executable

If you are on Windows, you can generate a portable `.exe` version of StreamWarden using the provided batch script:

```bat
build-windows-portable.bat
```

This script will:
- Automatically download and prepare all required native dependencies (Streamlink portable and JavaFX)
- Build the application with Maven
- Package everything into a portable app-image using jpackage

#### How to use:
1. Make sure you have the Java Development Kit (JDK) 17 or higher (not just the JRE) and Maven installed.
2. Open a terminal in the project root directory.
3. Run the script
4. After completion, you will find the portable executable in the `StreamWarden_Win_Portable` folder.

---

###

<div align="center">
  
  # Contributors:
  

  Contributions are welcome! Whether you want to fix bugs, add features, or improve documentation, your help is appreciated.

</div>

###

<div align="center">
  
  # Support This Project

</div>  

This application is completely free and open-source. If you find it valuable, you can support its development with a pay-what-you-want contribution!

<br>

<div align="center">

  [![Support me on Ko-Fi](./assets/images/support_me_on_kofi.png)](https://ko-fi.com/yougo)
    
  [![Support with Cryptocurrency](https://img.shields.io/badge/Support-Cryptocurrency-8256D0?style=for-the-badge&logo=bitcoin&logoColor=white)](https://youtube-no-translation.vercel.app/?donate=crypto)

</div>

<br>

You can also support this project by:

- Starring this repository
- Sharing it with others who might find it useful
- Following me on [GitHub](https://github.com/YouG-o)

###

<div align="center">

# Legal Notice ⚠️

**Important:** Before recording any stream, make sure you have the creator's permission or that the content is not protected by copyright.

Always respect the terms of service of streaming platforms (Twitch, YouTube, etc.).

This software is provided for educational and personal use only. I do not encourage copyright infringement and cannot be held responsible for improper use of this application.


# LICENSE

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE)

</div>