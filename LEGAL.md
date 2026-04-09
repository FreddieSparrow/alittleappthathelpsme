# Legal Disclaimer

**A Little App That Helps Me**
Last updated: April 2026

---

## 1. No Warranty

This software is provided **"as is"**, without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and non-infringement.

The authors and contributors make no representations or warranties that:
- The app will be error-free or uninterrupted
- Defects will be corrected
- The app is free of viruses or other harmful components
- The results of using the app will meet your requirements

Use of this software is entirely at your own risk.

---

## 2. Limitation of Liability

In no event shall the authors, contributors, or copyright holders be liable for any direct, indirect, incidental, special, exemplary, or consequential damages (including but not limited to: loss of data, loss of files, device damage, security incidents, financial loss) arising out of the use of or inability to use this software, even if advised of the possibility of such damages.

---

## 3. Privacy

This app is designed to operate **entirely offline** and to store all user data locally on the device.

- No personal data is collected, transmitted to, or stored on any external server by the app itself.
- No analytics, telemetry, tracking pixels, or crash reporting libraries are included.
- The NAS browser feature connects only to a server you manually configure on your local network.
- The file transfer feature communicates only with devices on your local Wi-Fi network, and all data is end-to-end encrypted (AES-256-GCM) before transmission.

**You are solely responsible** for the security of your device, your local network, and any servers (e.g., NAS) you choose to connect to using this app.

---

## 4. Encryption Notice

This software uses standard cryptographic algorithms (AES-256-GCM) provided by the Android platform (`javax.crypto`). The use of strong encryption may be subject to local laws and regulations in your jurisdiction. **You are responsible for ensuring that your use of encryption technology complies with all applicable laws.**

---

## 5. File Transfer

The file transfer feature transmits data over your local network. While the app encrypts file contents using AES-256-GCM:

- The app does **not** encrypt metadata visible at the network layer (e.g., IP addresses, TCP packet size).
- You are responsible for ensuring your network is secure.
- Do not use the file transfer feature on untrusted public Wi-Fi networks.

---

## 6. NAS Integration

When you configure the NAS browser, your NAS credentials (IP address, username, password) are stored locally on your device in Android DataStore. These credentials are never transmitted to any third party. You are responsible for:

- The security of your NAS device and its WebDAV configuration
- Using HTTPS where available on your NAS for additional transport security
- Managing NAS access credentials and permissions

---

## 7. Third-Party Libraries

This app uses open-source libraries including but not limited to: Jetpack Compose, Room, Hilt, Ktor, ZXing, Accompanist. Each library is subject to its own license terms. See the project dependencies for details.

---

## 8. User Responsibility

You are solely responsible for:
- The content you create, store, or transfer using this app
- Ensuring you have the right to transfer any file you send using the transfer feature
- The security of your device PIN/password and any vault PIN set within the app
- Regular backups of your data (the app does not back up your data automatically)

---

## 9. Open Source

This project is open source. The source code is available at the project repository. You are free to inspect, modify, and distribute the code under the terms of the project license. There is no "backdoor," hidden functionality, or obfuscation in the encryption implementation.

---

*This disclaimer does not constitute legal advice. If you have specific legal questions about the use of this software, consult a qualified legal professional in your jurisdiction.*
