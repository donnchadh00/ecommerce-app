# Ecommerce Platform

A microservices-based ecommerce application built with Spring Boot, React, RabbitMQ, Postgres, and Kubernetes.
It uses asynchronous order processing, idempotent event handling, and observability with logs, metrics, and traces.

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

## Order Flow

1. Order Service persists the order and writes `order.v1.placed` to its outbox.
2. Inventory Service reserves or rejects stock.
3. Payment Service authorizes payment after inventory reservation.
4. Order Service confirms or cancels the order based on downstream events.
5. Payment Service captures or voids the Stripe payment as the workflow progresses.

- asynchronous event-driven communication
- idempotent listeners and outbox publishing
- dead-letter queues for failed messages
- eventual consistency without distributed transactions

## Local Run With Docker Compose

1. Install Docker.
2. Clone the repo:

   ```bash
   git clone https://github.com/donnchadh00/ecommerce-app.git
   cd ecommerce-app
   ```

3. Create a local env file:

   ```bash
   cp .env.example .env
   ```

4. Set at least:

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

   Local stack:

   - seeded admin account: `admin@demo.local` / `Admin123!`
   - seeded guest account: `guest@demo.local` / `Admin123!`
   - seeded products in the catalog
   - frontend-ready API paths through the Nginx gateway
   - Grafana, Prometheus, Tempo, and the OpenTelemetry collector

6. Local service URLs:

- App gateway: [http://localhost:8080](http://localhost:8080)
- RabbitMQ management: [http://localhost:15672](http://localhost:15672)
- Grafana: [http://localhost:3000](http://localhost:3000)
- Prometheus: [http://localhost:9090](http://localhost:9090)

7. Example flow:

- register a normal user, or
- log in as `admin@demo.local` / `Admin123!`
- use the `Continue as guest` button on the sign-in page
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

3. UI: [http://localhost:5173](http://localhost:5173).

The Vite dev server proxies `/api/*` calls to the Docker Compose gateway on port `8080`.

## Production Deployment Profile

Production profile with traces enabled and Grafana/Prometheus disabled by default:

```bash
cp .env.production.example .env.production
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

This profile keeps Tempo and the OpenTelemetry collector enabled for the order confirmation page trace view.

Grafana and Prometheus can be added with the `dashboards` profile:

```bash
docker compose --profile dashboards --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

Compose commands in this mode:

```bash
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml logs product-service --tail=200
```

## Vercel + Oracle Cloud Demo Deployment

Deployment shape:

- Vercel hosts the React frontend from `frontend/`
- one Oracle Cloud Always Free compute instance runs the backend stack with Docker Compose
- Tempo and the OpenTelemetry collector stay on the Oracle side so `/api/orders/traces/{traceId}` still works for the confirmation page

Production frontend/backend routing:

- `frontend/src/api/config.ts` falls back to relative `/api/*` paths in production
- `frontend/vercel.json` rewrites `/api/*` requests to the backend VM and rewrites client-side routes to `index.html`

Steps:

1. In Oracle Cloud, create an Ubuntu compute instance and give it a public IPv4 address.
2. Open ingress for SSH and the backend HTTP port exposed by this repo (`8080` by default).
3. Copy the repo to the instance and create `.env.production` from `.env.production.example`.
4. Generate an internal product-service token that matches your `JWT_SECRET`.
5. Start the backend stack:

   ```bash
   python3 scripts/generate_internal_token.py --secret 'YOUR_LONG_JWT_SECRET' --expires-days 730
   docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up --build -d
   ```

6. Seed inventory for the six demo products. Product records are seeded automatically, but inventory rows are not.

   ```bash
   docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres-inventory psql -U user -d inventorydb <<'SQL'
   INSERT INTO inventory (product_id, quantity)
   VALUES
     (1, 1000),
     (2, 1000),
     (3, 1000),
     (4, 1000),
     (5, 1000),
     (6, 1000);
   SQL
   ```

7. Verify the backend:

   ```bash
   curl http://localhost:8080/healthz
   curl http://localhost:8080/api/products
   ```

8. In Vercel, create a project with `frontend/` as the root directory and deploy it.

The frontend stays same-origin with Vercel, so no additional CORS configuration is required.

Notes:

- The rewrite target is currently committed in `frontend/vercel.json`. If the Oracle VM public IP changes, update that file and redeploy the frontend.
- On a fresh VM, `/api/products` can return `502` while `product-service` is still starting.
- Keep RabbitMQ management (`15672`) closed in OCI security rules.

## Kubernetes Notes

The repo includes manifests under `k8s/` and a `kustomization.yaml` that generates the `app-secrets` secret from the repo-root `.env` file.

Commands:

```bash
k3d cluster create ecommerce-cluster --agents 2 -p "80:80@loadbalancer"
kubectl create namespace ecommerce
kubectl apply -k k8s/
kubectl get pods -n ecommerce
```

Ingress: [http://localhost](http://localhost).

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
docker compose --profile loadtest run --rm k6 run /scripts/checkout-login.js
```

Run against Kubernetes:

```bash
k6 run -e BASE_URL=http://localhost infra/load/checkout-login.js
```
