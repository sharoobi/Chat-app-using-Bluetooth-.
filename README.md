# 📶 Sharoobi Bluetooth Chat App

A simple, lightweight, and open-source Android chat app that enables **offline communication** via Bluetooth.  
Perfect for **indoor use**, **remote locations**, or **areas with poor/no internet**.

---

## 🌟 Features

- **Offline Messaging**: Communicate via Bluetooth without internet or SIM
- **Real-Time Chat**: Send and receive messages instantly
- **Manual Device Selection**: Choose exactly which device to connect to
- **Server/Client Roles**: One device hosts, another joins
- **Scroll Chat View**: Messages appear in an auto-scrolling chat area
- **Clean & Simple UI**: Focused on core functionality
- **Pure Java**: No external libraries, fully native Android code
- **Open Source & Expandable**: Easily add features like voice, files, multi-device mesh, and more

---

## 🚀 How to Use

1. Install the app on two Android devices
2. Enable Bluetooth and pair the devices via system settings
3. On one device, tap **“Start Server”**
4. On the other, tap **“Connect to Device”** and select the paired device
5. Start chatting offline!

> Make sure to grant **Bluetooth** and **Location** permissions when prompted.

---

## 💡 Use Cases

✅ **Indoor Communication** in buildings with no signal  
✅ **Emergency Chatting** in disasters or blackouts  
✅ **Remote Areas** without internet or towers  
✅ **Simple Classroom Demos** without network setup  
✅ **Device-to-Device Messaging** for fun or testing

---

## 🛠️ Future Enhancements (Ideas)

- 🔊 **Voice Messaging via Bluetooth**
- 📁 **File Sharing (Images, Documents)**
- 📡 **Mesh Networking** (Multi-device relay chat)
- 🔔 **Notification Sounds**
- 🧠 **Smart UI Improvements**
- 🌐 **Fallback to Internet/Wi-Fi Direct if available**

---

## 🔐 Permissions Used

- `BLUETOOTH` / `BLUETOOTH_ADMIN` – for core communication  
- `ACCESS_FINE_LOCATION` – required by Android for device discovery  
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` – for Android 12+  

> Don’t worry – the app doesn’t use internet or track anything.

---

## 👨‍💻 Developer Notes

This project is fully written in **Java**, with zero external dependencies.  
Optimized for Android development inside tools like **CodeAssist**, making it easy to modify, extend, or rebuild.

```java
System.out.println("🔥 Built by Sharoobi with 💙 for offline communication.");
