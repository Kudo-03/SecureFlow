SecureFlow Cloud-Based VPN, Traffic Analysis & Anomaly Detection
Platform

SecureFlow is a Python-based network traffic monitoring and anomaly
detection platform that combines VPN-style traffic forwarding, real-time
traffic inspection, and machine learning-based anomaly detection.

It enables secure traffic routing through a TUN interface, analyzes
network flows using Zeek, and applies TensorFlow Lite models to detect
abnormal behavior in real time.

FEATURES

-   TUN-based VPN / Proxy
    -   Full traffic forwarding using Linux TUN interface
    -   NAT & iptables configuration
    -   Packet interception and redirection
-   Traffic Analysis
    -   Zeek-based network flow inspection
    -   Real-time log parsing and analysis
    -   Flow-level feature extraction
-   Machine Learning Detection
    -   TensorFlow Lite anomaly detection model
    -   Autoencoder-based traffic anomaly scoring
    -   Lightweight inference for edge deployment
-   Android Client Dashboard
    -   Built with Kotlin & Jetpack Compose
    -   REST API integration with FastAPI backend
    -   WebSocket-based real-time monitoring
    -   Live anomaly score visualization
-   Containerized Deployment
    -   Dockerized services
    -   Kubernetes-ready configuration
    -   Multi-node deployment support

ARCHITECTURE

Android Client (Jetpack Compose) 
↓
 FastAPI Backend (REST + WebSockets) 
↓
TensorFlow Lite Inference Engine 
↓
 Zeek Traffic Analysis 
↓
 Linux TUN
Interface + NAT / iptables

TECH STACK

Backend: Python, FastAPI Networking: Linux TUN, iptables, NAT Traffic
Analysis: Zeek ML: TensorFlow Lite (Autoencoder) Mobile Client: Kotlin,
Jetpack Compose Containerization: Docker Orchestration: Kubernetes

INSTALLATION

1.  Clone Repository

git clone https://github.com/your-username/secureflow.git cd secureflow

2.  Install Dependencies

pip install -r requirements.txt

3.  Run Backend

uvicorn main:app –host 0.0.0.0 –port 8000

4.  Docker Deployment

docker build -t secureflow . docker run -p 8000:8000 secureflow

HOW IT WORKS

1.  Traffic is routed through a TUN interface.
2.  Packets are forwarded using NAT.
3.  Zeek inspects and logs network flows.
4.  Extracted features are fed into a TensorFlow Lite model.
5.  The model produces anomaly scores.
6.  The FastAPI backend exposes results via REST and WebSockets.
7.  The Android client displays traffic statistics and anomaly alerts in
    real time.

USE CASES

-   Network anomaly detection
-   Edge traffic monitoring
-   Research & cybersecurity experimentation
-   DevOps network observability
-   Lightweight IDS prototype

AUTHOR

Anas Al Jboor Computer Science Student DevOps & Cloud Engineering |
AI-Driven Systems
