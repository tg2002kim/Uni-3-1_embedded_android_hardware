Android Rhythm Game with Embedded Hardware (2025 Spring Project)
This repository contains the source code and documentation for an embedded-system-integrated rhythm game developed as a team project in the Mobile System Programming course at Dankook University, during the Spring 2025 semester (3rd year, 1st semester).
The project was designed to bridge Android application development with physical I/O device control via FPGA, Linux kernel drivers, and JNI.

🧠 Project Summary
Objective: Develop an interactive rhythm game using an Android app and external hardware (keypad, display, buzzer, etc.).

Hardware Platform: SM5-S4210 FPGA board (Hanback Electronics)

Interaction: Android ↔ JNI (C/C++) ↔ Linux Device Driver ↔ FPGA Hardware

📁 Repository Structure
plaintext
복사
편집
ANDRIOD_STUDIOLLL/
├── app/                         # Android App Source Code
├── .idea/, .gradle/, gradle/    # Android Studio Project Config
├── build.gradle.kts             # Gradle Configuration (Kotlin DSL)
├── settings.gradle.kts
├── local.properties
├── gradlew, gradlew.bat         # Gradle Wrapper
│
├── Device_manual.pdf            # Hardware Pinmap and Device Explanation
├── Project_Report.pdf           # Architecture, Flow, Result, Team Roles
├── Project_Demo.mp4             # System Demo Video
🧩 Core Features
Gameplay using physical 3x3 keypad

Visual feedback through:

Dot-Matrix

7-Segment

LCD (combo display)

LEDs

Piezo buzzer

JNI interface for real-time control

I/O is controlled via /dev/ device nodes using open(), write(), and read() in C

⚙️ Build Guide
📌 Prerequisites
Android Studio (Arctic Fox or later)

NDK (Native Development Kit) – r21e or compatible

Gradle (via wrapper) – Kotlin DSL based

✅ Steps
NDK Configuration

In local.properties:

properties
복사
편집
ndk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk\\ndk\\21.4.7075529
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
Build the APK

bash
복사
편집
./gradlew assembleDebug
or via Android Studio:
Build → Build Bundle(s) / APK(s) → Build APK(s)

Connect Device & Install

Use USB or adb over Wi-Fi.

bash
복사
편집
adb install app/build/outputs/apk/debug/app-debug.apk
🔗 JNI Integration Architecture
plaintext
복사
편집
[ Android Activity (Java) ]
        ⇅ JNI
[ Native C/C++ Code (.cpp) ]
        ⇅ open/write/read
[ Linux Kernel Device Driver ]
        ⇅ Memory-Mapped I/O
[ FPGA Hardware (e.g., Keypad, 7-seg, LED) ]
Java calls native methods via System.loadLibrary(...)

Native methods defined with JNIEXPORT in C

File operations (/dev/fpga_key, etc.) control I/O

JNI handles bidirectional messaging between app and hardware

Example JNI Signature:

cpp
복사
편집
JNIEXPORT void JNICALL
Java_com_example_app_MainActivity_controlLED(JNIEnv* env, jobject thiz, jint pattern);
🔌 Hardware Interface & UART Setup
FPGA ↔ Android Communication
The board uses UART serial communication to connect with Android.

Communication flow:

plaintext
복사
편집
[ Android App ]
       ⇅
  /dev/ttySACx (e.g., ttySAC3)
       ⇅
  FPGA Peripheral (I/O device)
UART Configuration
In init.rc or via driver:

c
복사
편집
// Example for configuring UART
int fd = open("/dev/ttySAC3", O_RDWR | O_NOCTTY);
struct termios tty;
cfsetispeed(&tty, B115200);
cfsetospeed(&tty, B115200);
tty.c_cflag |= (CLOCAL | CREAD);
tcsetattr(fd, TCSANOW, &tty);
Device Node Creation
If needed:

bash
복사
편집
mknod /dev/fpga_key c 246 0
chmod 666 /dev/fpga_key
Use appropriate major/minor numbers based on driver allocation

Verify device node visibility via ls /dev/

🎬 Demo & Results
Demo Video: Project_Demo.mp4

Report: Project_Report.pdf (design architecture, timing diagram, test logs)

Hardware Manual: Device_manual.pdf

🏫 Course Information
Item	Details
Course Title	Mobile System Programming
Semester	2025 Spring (3rd year, 1st semester)
University	Dankook University
Department	Mobile System Engineering
Instructor	Prof. Kim Jeong-hun

🔮 Future Development
Move from JNI to AIDL or HAL abstraction (for scalability)

Use modern Android API (Kotlin, Jetpack) instead of AppCompat

Add persistent score system and difficulty levels

Real-time thread optimization for input buffering