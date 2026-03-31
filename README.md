# Reveria

A content and social media platform with video streaming, built as a microservices architecture.

## Features

- User registration, login, and OAuth (Google, Facebook, GitHub)
- Video upload, transcoding, and streaming (HLS)
- Live streaming with real-time chat
- Channels with subscriptions
- Social posts with media attachments
- Stories
- Friend requests, follows, and user discovery
- Content moderation tools

## Architecture

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8090 | Routes requests, JWT auth, CORS |
| User Service | 8080 | Auth, profiles, OAuth |
| Content Service | 8081 | Videos, channels, live streams, chat |
| Social Service | 8082 | Posts, stories, relationships |
| Frontend | 80 | Angular SPA |

Infrastructure: PostgreSQL, Redis, Kafka, MinIO, NGINX-RTMP, Jenkins

## Prerequisites

- Java 17
- Node.js 20+
- Docker and Docker Compose
- Maven (or use the included `mvnw` wrapper)

## Local Development

1. Start infrastructure services:

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, Kafka, MinIO, NGINX-RTMP, and Jenkins.

2. Run backend services (from the project root):

```bash
./mvnw spring-boot:run -pl services/user-service
./mvnw spring-boot:run -pl services/api-gateway
./mvnw spring-boot:run -pl services/content-service
./mvnw spring-boot:run -pl services/social-service
```

3. Run the frontend:

```bash
cd services/frontend
npm install
npm start
```

The frontend will be available at `http://localhost:4200`.

## Kubernetes (Minikube)

1. Start minikube and enable the ingress addon:

```bash
minikube start
minikube addons enable ingress
```

2. Build and push service images (or use the Jenkins CI/CD pipeline).

3. Deploy everything:

```bash
cd infrastructure
./apply.sh
```

4. Restart deployments:

```bash
./deploy.sh              # all services
./deploy.sh user-service # single service
```

5. Access the frontend:

```bash
minikube tunnel
```

Then open `http://localhost`.

## Project Structure

```
Reveria/
  services/
    user-service/        Spring Boot - auth and profiles
    content-service/     Spring Boot - videos and streaming
    social-service/      Spring Boot - posts and relationships
    api-gateway/         Spring Cloud Gateway
    frontend/            Angular 20
    nginx-rtmp/          RTMP streaming server
  infrastructure/
    k8s/                 Kubernetes manifests
    apply.sh             Full cluster deployment
    deploy.sh            Rolling restart utility
    Jenkinsfile          CI/CD pipeline
```
