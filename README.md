# 🔐 Mangrypt

> ⚠️ **This application is currently under active development. Features may change, and bugs may occur. Use with caution.**

**Mangrypt** is a user-friendly encryption application designed to protect sensitive user data. It supports encryption for text, images, audio, and video, and includes a secure, fully enclosed web browser for private browsing.

---

## 🔽 Download & Installation

You can use Mangrypt by downloading a pre-built executable or building it manually.

### 📦 Executable (Recommended)

- Visit the [Releases Page](https://github.com/RedStoneMango/Mangrypt/releases) to download the latest version for your OS.
- Extract and run the executable:
  - `.exe` (Windows)
  - `.app` (macOS)
  - `.sh` (Linux)

### 🧱 Build from Source

To build Mangrypt yourself and generate a native executable, see:  
👉 [**BUILDING.md →**](./BUILDING.md)

---

## 🖼️ Application UI

Here are some references on what the application UI looks like:

| Setting Up The Launch Passphrase |
| - |
| ![Passphrase setup](MangryptSetup.gif)|

> ⚠️ UI elements shown may change in future versions and may not exactly match these screenshots.

---

## 🔒 Encryption Architecture

Mangrypt uses a **two-layer encryption model** to ensure data integrity and privacy.

### 🗂️ Architecture Overview

![Encryption Architecture Diagram](ARCHITECTURE_DIAGRAM.gif)

### 1️⃣ Layer One – *Launch Passphrase*

The first layer secures a minimal configuration structure (vault names and other insensitive metadata), encrypted using:

- **AES-128-GCM** with a 128-bit key and 12-byte IV  
- **128-bit authentication tag**  
- **PBKDF2 with HMAC-SHA256** (65,536 iterations)  
- **16-byte salt**  
- A **user-defined passphrase**

> This passphrase is required every time Mangrypt is launched. Use a strong and unique passphrase.

### 2️⃣ Layer Two – *Session Password*

Once Layer One is decrypted, sensitive data is unlocked by a second password. This layer uses:

- The same **AES-128-GCM** encryption configuration  
- A separate **Session Password**

> If the Mangrypt window loses focus, the app automatically obscures all content and re-prompts for the **Session Password** to protect unattended data.

### 🧠 Memory Usage

The **Java Virtual Machine (JVM)** employs a **Garbage Collector (GC)** that automatically identifies and clears unused objects from memory.

Mangrypt follows secure memory handling practices to reduce the risk of memory leaks and unintended data persistence. Sensitive data and passwords are immediately overwritten after use, encryption is performed only on demand, and no sensitive values are stored in immutable objects.

While **JavaFX (JFX)** components internally use `String` objects, Mangrypt ensures that any sensitive input from the UI is promptly converted to `char[]` for controlled handling. These UI fields are cleared as soon as they’re no longer needed, minimizing exposure within the application's memory.

---

## ⚠️ Security Notice

While Mangrypt is designed with strong encryption practices and careful attention to security, it has **not yet undergone a formal third-party security audit**. This means that, although it is built with best practices in mind, absolute protection cannot be guaranteed at this stage.

**Help harden Mangrypt. If you're a security expert, your review or audit would be extremely valuable.**

For contributions or feedback, see [here](#-feedback--contributions).

---

## 🛠️ Build and Framework

Mangrypt is built using:

- **Language:** Java 23
- **Build Tool:** Maven 3.8.5
- **UI Framework:** JavaFX 23
- **Native Packaging:** [`javapackager`](https://github.com/javapackager/JavaPackager) 1.7.4

---

## ✅ Benefits

- **Two-Layer Encryption:** Dual-password system ensures both configuration and session data are securely protected.
- **Multi-Media Support:** Encrypts text, images, audio, and video files.
- **Privacy-Focused Browser:** Built-in, isolated browser for secure, private browsing sessions.
- **Auto-Lock on Focus Loss:** Automatically obscures sensitive content when the app loses focus.
- **Cross-Platform:** Works on Windows, macOS, and Linux.
- **Open Source:** Transparent development with opportunities for community contributions.

---

## 💻 Requirements

To run or build Mangrypt, ensure your environment meets the following minimum requirements:

| Requirement       | Needed For     | Minimum Version             |
|-------------------|----------------|------------------------------|
| Java JDK          | Build only     | Java 23+                     |
| Apache Maven      | Build only     | 3.8.5+                       |
| JavaFX SDK        | Build only     | 23+ (matching your JDK)     |
| Operating System  | All            | Windows, macOS, or Linux     |
| Memory            | All            | 4 GB minimum (8 GB recommended) |

> 🧠 Info: Requirements marked as "Build only" are not needed if using a pre-built executable.

---

## 💬 Feedback & Contributions

Feedback, suggestions, and contributions are most welcome. To participate, [open an issue](https://github.com/RedStoneMango/Mangrypt/issues) or submit a pull request.
