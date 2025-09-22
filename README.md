# Ecommerce Platform

A microservices-based **ecommerce application** built with Spring Boot, React, RabbitMQ, Postgres, and Kubernetes.  
It demonstrates **saga orchestration for order workflows**, **idempotent event handling**, and **observability** with logs/metrics.

---

## Features
- User authentication with JWT
- Product catalog & shopping cart
- Order placement with saga orchestration
- Payment authorization & capture (auth-then-capture flow)
- Observability: logs, metrics (Prometheus, Grafana), traces (Opentelemetry + Tempo)
- Deployable via Docker Compose or Kubernetes (k3d)

---

## Architecture

The system is split into independent services, each with its own Postgres database:

- **Auth Service** – user registration, login, JWT issuing  
- **Product Service** – product catalog (read-heavy, pricing & details)  
- **Cart Service** – shopping cart per user  
- **Order Service** – order lifecycle (Placed → Confirmed/Cancelled)  
- **Inventory Service** – stock reservation, rejection if unavailable  
- **Payment Service** – Stripe-based authorization, capture, refunds  

Shared modules:
- **common-lib** – DTOs, shared utilities  
- **events** – event contracts (published/consumed via RabbitMQ as JSON)
- **common-obs** – observability config (JSON logging w/ MDC, Prometheus/Actuator setup)  

---

## Saga Orchestration

Order workflows are coordinated across multiple services using the **Saga pattern** with RabbitMQ as the message broker.  
Each service emits and consumes events, ensuring consistency without distributed transactions.

### Flow

1. **Order Placed** (Order Service → event emitted)  
2. **Inventory Reserved** (reject if stock unavailable)
3. **Payment Authorized** (hold funds, not yet captured; void on failure)  
4. **Order Confirmed** (after successful reservation + auth)  
5. **Payment Captured** (charge funds after confirmation)  
6. **Order Cancelled** (if auth, reservation, or capture fails; triggers compensation)  

---

### Key Properties

- **Asynchronous, event-driven**: services communicate via RabbitMQ topics/queues.  
- **Idempotent handlers**: each consumer checks for existing state before applying side-effects, preventing duplicates.  
- **Dead Letter Queues (DLQs)**: failures are rerouted for retry/inspection without blocking the main flow.  
- **Compensating actions**: e.g., refund on payment failure, stock release on order cancel.

This design keeps each service autonomous while still achieving **eventual consistency** across the system.

---

## UI

A **React + Vite frontend**:

- Login/Register (Auth service integration)
- Product page (List products items)  
- Cart page (add/remove items)  
- Checkout flow (calls Order API → clears cart on success)  
- Basic Orders list (view past orders)

---

## Observability

- **Logs**: JSON structured logs w/ MDC (`traceId`, etc.) via `common-obs/observability-logback-spring.xml`  
- **Health**: Spring Actuator (`/actuator/health`) with detailed output enabled  
- **Metrics**: Exposed for Prometheus scraping  
- **Dashboards**: Prebuilt Grafana dashboards for Spring Boot & k6 metrics  
- **Tracing**: OpenTelemetry Java agent exports traces to **Tempo**, with `traceId` correlated in logs  
- **Load testing**: k6 integrated with Prometheus remote write  

---

## Deployment

### Environment

This project uses a **single `.env` file at the repo root** for both Docker Compose and Kubernetes.

1. Create `.env`:
   ```bash
   cp .env.example .env
   # edit values
   ```

### Local (Docker Compose)

1. Make sure you have [Docker](https://docs.docker.com/get-docker/) installed.

1. Clone the repo  
   ```bash
   git clone https://github.com/donnchadh00/ecommerce-app.git
   cd ecommerce
   ```

2. Start dependencies and services (Docker will build each service JAR during image build) 
   ```bash
   docker compose up --build
   ```

3. Access:
   - Nginx gateway → [http://localhost:8080](http://localhost:8080)  
   - RabbitMQ mgmt → [http://localhost:15672](http://localhost:15672) (default: `admin/admin`)  

---

### Kubernetes (K8s Manifests)

1. Make sure you have [k3d](https://k3d.io/), [kubectl](https://kubernetes.io/docs/tasks/tools/), and [Docker](https://docs.docker.com/get-docker/) installed.

2. Create a local cluster (example with [k3d](https://k3d.io/)):  
   ```bash
   k3d cluster create ecommerce-cluster --agents 2 -p "80:80@loadbalancer"
   ```

3. Apply manifests:
   ```bash
   kubectl apply -f k8s/
   ```

4. Verify:
   ```bash
   kubectl get pods -n ecommerce
   kubectl get svc -n ecommerce
   ```

5. Access:
   - In k3d → [http://localhost:80](http://localhost:80) (via ingress controller)

---

### Frontend (React + Vite)

1. Configure API endpoints by creating `frontend/.env.local`:
   ```env
   VITE_API_AUTH=/api/auth
   VITE_API_PRODUCT=/api/products
   VITE_API_ORDER=/api/orders
   VITE_API_CART=/api/cart
   VITE_API_PAYMENT=/api/payments
   VITE_API_INVENTORY=/api/inventory
   ```

2. Install and start:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Access the UI at → [http://localhost:3000](http://localhost:3000)  

---

## Load Testing

**k6** is used to validate end-to-end flows under load.

Scenarios defined in `/infra/load/checkout-login.js`:

- **browse** → users list products  
- **checkout** → full flow (add to cart → place order → capture payment)

### Run with Docker Compose

   ```bash
   docker compose run --rm k6 run /scripts/checkout-login.js
   ```

### Run against Kubernetes

   ```bash
   k6 run -e BASE_URL=http://localhost infra/load/checkout-login.js
   ```

Results show request durations, success rates, and failure percentages to help spot bottlenecks.
