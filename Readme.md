Dankook University, 3rd Year 1st Semester (Spring 2025)

# Android Rhythm Game with Embedded Hardware

This repository contains the source code and documentation for a rhythm game developed for the **Mobile System Programming** course at Dankook University. This project, completed in Spring 2025, integrates an **Android app** with external hardware via an **FPGA board**.

---

## Project Summary

- **Objective**: To develop an interactive rhythm game that connects an Android app to physical I/O devices (keypad, display, buzzer, etc.).
- **Hardware Platform**: SM5-S4210 FPGA board.
- **Core Technology**: The project links Android to hardware through **JNI** (C/C++), a **Linux kernel device driver**, and memory-mapped I/O.

---

## Core Features

- **Gameplay**: Uses a physical **3x3 keypad** for player input.
- **Real-time Feedback**: Provides visual and audio feedback through a **Dot-Matrix**, **7-Segment**, **LCD** (for combo display), **LEDs**, and a **Piezo buzzer**.
- **JNI Interface**: Real-time hardware control is managed via `open()`, `write()`, and `read()` calls in C to `/dev/` device nodes.

---

## Repository Structure

ANDRIOD_STUDIOLLL/
├── app/                      # Android App Source Code
├── Device_manual.pdf         # Hardware Pinmap and Device Explanation
├── Project_Report.pdf        # Architecture, Flow, Result, Team Roles
├── Project_Demo.mp4          # System Demo Video


---

## Build Guide

### Prerequisites
- **Android Studio** (Arctic Fox or later)
- **NDK** (Native Development Kit)
- **Gradle** (Kotlin DSL)

### Steps
1. **NDK Configuration**: Set your NDK directory in `local.properties`.
2. **Build the APK**: Use `./gradlew assembleDebug` or Android Studio's build menu.
3. **Connect & Install**: Install the `app-debug.apk` onto the device using `adb`.

---

## JNI Integration Architecture

The architecture is designed as a layered system:

- **[ Android App (Java) ]**
  - Communicates with native code via **JNI**.
- **[ Native C/C++ Code ]**
  - Performs file operations (`open`, `write`, `read`) on device nodes.
- **[ Linux Kernel Device Driver ]**
  - Interacts directly with the FPGA.
- **[ FPGA Hardware ]**
  - The physical components like the keypad and LEDs.

---

## Future Development

- **Architecture**: Transition from JNI to **AIDL** or **HAL** for improved scalability.
- **Modernization**: Migrate to modern Android APIs like **Kotlin** and **Jetpack**.
- **Features**: Implement a persistent high-score system and different difficulty levels.
- **Performance**: Optimize threads for real-time input buffering to enhance gameplay responsiveness.
