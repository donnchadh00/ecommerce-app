# Ecommerce Platform

A microservices-based ecommerce application built with Spring Boot, React, RabbitMQ, Postgres, and Kubernetes.
It demonstrates saga orchestration for order workflows, idempotent event handling, and observability with logs, metrics, and traces.

## Demo

After `docker compose up --build`, the repo boots with:

- seeded admin account: `admin@demo.local` / `Admin123!`
- seeded products in the catalog
- frontend-ready API paths through the Nginx gateway

That makes it possible to log in, browse products, add items to cart, and exercise the order workflow without manual database setup.

## Features

- User authentication with JWT
- Product catalog and shopping cart
- Order placement with saga orchestration
- Payment authorization and capture flow
- Observability with Prometheus, Grafana, OpenTelemetry, and Tempo
- Local deployment via Docker Compose and Kubernetes manifests

## Architecture

The system is split into independent services, each with its own Postgres database:

- Auth Service: user registration, login, JWT issuing
- Product Service: catalog and pricing
- Cart Service: per-user shopping cart
- Order Service: order lifecycle and saga kickoff
- Inventory Service: reservation and stock checks
- Payment Service: payment authorization, capture, and refunds

Shared modules:

- `common-lib`: shared auth utilities
- `events`: event contracts for RabbitMQ
- `common-obs`: JSON logging, correlation IDs, and observability wiring

## Saga Flow

Order workflows are coordinated across services using RabbitMQ and the Saga pattern.

1. Order Service persists the order and writes `order.v1.placed` to its outbox.
2. Inventory Service reserves or rejects stock.
3. Payment Service authorizes payment after inventory reservation.
4. Order Service confirms or cancels the order based on downstream events.
5. Payment Service captures or voids the Stripe payment as the workflow progresses.

Key properties:

- asynchronous event-driven communication
- idempotent listeners and outbox publishing
- dead-letter queues for failed messages
- eventual consistency without distributed transactions

## Local Run With Docker Compose

1. Make sure Docker is installed.
2. Clone the repo:

   ```bash
   git clone https://github.com/donnchadh00/ecommerce-app.git
   cd ecommerce-microservices
   ```

3. Create a local env file:

   ```bash
   cp .env.example .env
   ```

4. Fill in at least:

   ```env
   JWT_SECRET=change-me-local-dev-jwt-secret-32bytes-min
   JWT_EXPIRATION_MS=36000000
   PRODUCT_INTERNAL_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   STRIPE_SECRET_KEY=
   POSTGRES_USER=user
   POSTGRES_PASSWORD=password
   RABBITMQ_VHOST=/
   RABBITMQ_USER=admin
   RABBITMQ_PASSWORD=admin123
   ```

   `JWT_SECRET` is now required by every service that issues or validates JWTs.
   `PRODUCT_INTERNAL_TOKEN` in `.env.example` is pre-signed with a local demo secret so cart/order can call product-service out of the box.

5. Start the stack:

   ```bash
   docker compose up --build
   ```

6. Access the local services:

- App gateway: [http://localhost:8080](http://localhost:8080)
- RabbitMQ management: [http://localhost:15672](http://localhost:15672)
- Grafana: [http://localhost:3000](http://localhost:3000)
- Prometheus: [http://localhost:9090](http://localhost:9090)

7. Demo flow:

- register a normal user, or
- log in as `admin@demo.local` / `Admin123!`
- browse the seeded products
- add items to cart
- place an order and inspect resulting service logs/events

## Frontend (React + Vite)

1. Create `frontend/.env.local`:

   ```env
   VITE_API_AUTH=/api/auth
   VITE_API_PRODUCT=/api/products
   VITE_API_ORDER=/api/orders
   VITE_API_CART=/api/cart
   VITE_API_PAYMENT=/api/payments
   VITE_API_INVENTORY=/api/inventory
   ```

2. Start the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Access the UI at [http://localhost:5173](http://localhost:5173).

The Vite dev server proxies `/api/*` calls to the Docker Compose gateway on port `8080`.

## Kubernetes Notes

The repo includes manifests under `k8s/` and a `kustomization.yaml` that generates the `app-secrets` secret from the repo-root `.env` file.

Example:

```bash
k3d cluster create ecommerce-cluster --agents 2 -p "80:80@loadbalancer"
kubectl create namespace ecommerce
kubectl apply -k k8s/
kubectl get pods -n ecommerce
```

Access the ingress at [http://localhost](http://localhost).

## Observability

- JSON structured logs with correlation and trace IDs
- Spring Boot actuator health and Prometheus metrics
- OpenTelemetry Java agent exporting traces to Tempo
- Grafana dashboards and Prometheus config included in-repo

## Load Testing

`k6` scripts live under `infra/load/`.

- `checkout-login.js`: browse plus checkout traffic
- `auth-smoke.js`: registration/login smoke load

Run with Docker Compose:

```bash
docker compose run --rm k6 run /scripts/checkout-login.js
```

Run against Kubernetes:

```bash
k6 run -e BASE_URL=http://localhost infra/load/checkout-login.js
```
