# BibliotekaApp-UI-API
*Aplikacija pravljena za biblioteku, koristeci springboot / App made for a library, using springboot*

This is an application intended for librarians and library members. It is designed primarily to be used during a librarian's working day, when someone comes in to borrow a book, but the application can also function as a platform on which any member can reserve a book for themselves in advance, and see which books in the library have already been borrowed, along with the time they are scheduled to be returned.

## Running the application
The application has to be started in parts. Here are the complete instructions for running it:

### 1. Create and start the PostgreSQL database
This application does not work without a Postgres database, so you have to start that first. Postgres 17 was used, and the local development credentials are `postgres` / `1`.

The repository contains `library-db-mkfile.sql`, which creates the `library_metadata` database and fills it with test data. Note that it is a **data-only** dump: it contains the `CREATE DATABASE` statement and the `INSERT`s, but no `CREATE TABLE`s, because the tables are created by Hibernate (`ddl-auto: update`). So the order is:

1. Run the `CREATE DATABASE library_metadata ...` line from the dump (or create the database by hand).
2. Start the services once, so that Hibernate creates the `books`, `lending`, `seats` and `users` tables.
3. Run the rest of the dump against `library_metadata` to load the test data.

#### Configuration
Nothing has to be configured for a local run — every setting below has a working default. Override them with environment variables when the defaults do not fit, and always override `JWT_SECRET` outside of local development:

| Variable | Default | Meaning |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/library_metadata` | JDBC URL of the database |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `1` | Database password |
| `JWT_SECRET` | a clearly-marked development value | HMAC-SHA signing key. **All services must use the same value**, otherwise tokens issued by the user service will not validate anywhere else |
| `JWT_EXPIRATION_MS` | `36000000` (10 hours) | Token lifetime |

### 2. Start the Eureka and Gateway services
Before starting the whole application, it is important to first start the Eureka Discovery Server (the folder of the same name) and then LibGateway (the corresponding gateway service for this application), in exactly that order.

Every module is a Maven project on Java 17 and Spring Boot 3.2.5 (the Eureka server uses a newer parent), so they can all be opened in a single IntelliJ window. An earlier version of this repository also carried a stray Gradle build inside the UI module that declared a different Spring Boot version; it has been removed, since it was what made a single-window setup fail.
### 3. Start all the other services in exactly the same way
Nothing special — just open them all in IntelliJ and check whether they register with the Eureka server, which runs on port/link http://localhost:8761/ .
The application itself will be available on the gateway port http://localhost:8080/ .

![eureka pic](https://github.com/ericges/BibliotekaApp-UI-API/blob/main/Pasted%20image%2020260622040136.png)

## Running with Docker Compose
The three sections above describe running the modules straight out of IntelliJ. Every module now also has a `Dockerfile`, and `docker-compose.yml` at the repository root builds and wires all seven of them together, so this is the shorter route.

Copy `.env.example` to `.env` and fill it in — `POSTGRES_PASSWORD` and `JWT_SECRET` have no defaults there on purpose. Then:

```bash
docker compose up --build -d
docker compose ps          # every service should reach (healthy)
```

The first build takes a while because each module is compiled inside its own Maven container. Once it settles, the application is on http://localhost:8080/ and the Eureka dashboard on http://localhost:8761/. Only those two ports are published; the other five services talk to each other over the internal Compose network, which is exactly how they behave on Kubernetes.

The database still has to be filled once, for the reason explained in step 1 — Hibernate creates the tables, the dump only carries the rows:

```bash
sed -e '/^\\/d' -e '/^DROP DATABASE/d' -e '/^CREATE DATABASE/d' -e '/^ALTER DATABASE/d' \
  library-db-mkfile.sql \
  | docker compose exec -T postgres psql -U postgres -d library_metadata -v ON_ERROR_STOP=1
```

The `sed` is there because the dump is `psql` output rather than portable SQL: it opens with `DROP DATABASE` / `CREATE DATABASE` and a `\connect`, all of which fight the database the Postgres container already created for you. Stripping those four kinds of line leaves just the `INSERT`s and the sequence resets.

Use `docker compose stop` to pause the stack and `docker compose down -v` to remove it along with its database volume.

## Running on Kubernetes (kind)
This is the deployment target the Docker work was building towards. Everything needed lives in the `k8s/` directory, and the whole thing runs locally inside WSL2 — no cloud account, no registry.

The cluster is [kind](https://kind.sigs.k8s.io/), which runs Kubernetes nodes as Docker containers on the Docker daemon you already have. The manifests are plain YAML tied together with Kustomize, which is built into `kubectl`, so there is nothing else to install.

### 1. Install the tools
You need `kubectl` and `kind`. Neither requires root if you put them somewhere already on your `PATH`, such as `~/.local/bin`:

```bash
mkdir -p ~/.local/bin
curl -Lo ~/.local/bin/kubectl "https://dl.k8s.io/release/v1.34.11/bin/linux/amd64/kubectl"
curl -Lo ~/.local/bin/kind    "https://kind.sigs.k8s.io/dl/v0.33.0/kind-linux-amd64"
chmod +x ~/.local/bin/kubectl ~/.local/bin/kind
```

Docker itself must be running in WSL. Everything below was verified on Ubuntu 24.04 under WSL2 with Docker 29.

### 2. Build the images
Kubernetes does not build anything — it only runs images. Reuse the Compose build, which is the same seven Dockerfiles:

```bash
docker compose build
```

If the Compose stack is currently up, stop it now: it holds port 8080, which the cluster is about to want.

```bash
docker compose stop
```

### 3. Create the cluster
```bash
kind create cluster --config k8s/kind-cluster.yaml
```

`k8s/kind-cluster.yaml` does two things worth knowing about. It pins the node image to Kubernetes v1.34.11 rather than taking kind's newest default, because the ingress controller installed in the next step does not support anything newer than 1.35 yet. And it maps host port **8080** onto port 80 inside the node.

That port number is not arbitrary. The gateway and the four services only accept browser requests from the origins listed in `CORS_ALLOWED_ORIGINS` (`k8s/config.env`), and the browser's `Origin` header travels through the gateway to them. `http://localhost:8080` is in that list; to serve the application on another host or port, add it there and re-apply, otherwise logins fail with a 403.

Then install the ingress controller and wait for it:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx wait --for=condition=ready pod \
  -l app.kubernetes.io/component=controller --timeout=180s
```

### 4. Load the images into the cluster
The images exist only in your local Docker, and they are tagged `:dev`, not pushed to any registry. kind has to copy them into the node, and the manifests use `imagePullPolicy: IfNotPresent` so that nothing ever tries to pull them from Docker Hub:

```bash
kind load docker-image --name biblioteka \
  biblioteka/eureka-server:dev biblioteka/gateway-service:dev \
  biblioteka/user-service:dev biblioteka/library-service:dev \
  biblioteka/loan-service:dev biblioteka/seat-service:dev \
  biblioteka/ui-service:dev postgres:17 busybox:stable
```

`postgres:17` and `busybox:stable` are in the list because the manifests use them for the database and for the small "wait until my dependency is up" containers; loading them saves the cluster from pulling them again.

### 5. Fill in the secrets
```bash
cp k8s/secrets.env.example k8s/secrets.env
```

Edit it and set `POSTGRES_PASSWORD` and `JWT_SECRET`. The file is gitignored. Kustomize turns it into a single Kubernetes Secret that every service reads, which is the point — if `JWT_SECRET` ever differed between services, tokens issued by the user service would silently fail to validate everywhere else.

### 6. Deploy
```bash
kubectl apply -k k8s/
kubectl -n biblioteka get pods -w
```

Give it two to three minutes. Kubernetes has no equivalent of Compose's `depends_on`, so the same eureka → gateway → everything else order is enforced with init containers instead. A pod showing `Init:1/2` is not stuck; it is waiting for something it needs, and `kubectl -n biblioteka logs <pod> -c wait-for-gateway` will say what. All eight pods should end up `1/1 Running`.

### 7. Load the test data
Same bootstrap order as always: the dump is data-only, so the tables have to exist first. The job below handles the waiting itself — it polls until Hibernate has created all four tables, skips the load entirely if the data is already there, and strips the psql-only lines out of the dump before running it.

```bash
kubectl -n biblioteka create configmap db-seed --from-file=library-db-mkfile.sql
kubectl -n biblioteka apply -f k8s/db-seed-job.yaml
kubectl -n biblioteka wait --for=condition=complete job/db-seed --timeout=180s
kubectl -n biblioteka logs job/db-seed | tail -8
```

The tail should print 27 books, 8 users, 32 seats and 8 lendings.

### 8. Use it
The application is on http://localhost:8080/ — the same address as always, so the screenshots further down still apply. Log in with any user from the database; every password is `123`.

The Eureka dashboard is not exposed through the ingress, since nothing outside the cluster needs it. Forward it when you want to look:

```bash
kubectl port-forward -n biblioteka svc/eureka-server 8761:8761
```

Six applications should be registered — the gateway, the four services and the UI. They register their **pod** IP rather than a hostname, which is why `lb://` routing inside the gateway keeps working even when a pod is replaced and gets a new address.

### 9. Changing a service, and tearing down
After editing one module, rebuild just that image, push it into the node and restart its Deployment:

```bash
docker compose build user-service
kind load docker-image --name biblioteka biblioteka/user-service:dev
kubectl -n biblioteka rollout restart deploy/user-service
```

To remove everything, including the database volume:

```bash
kind delete cluster --name biblioteka
```

### Troubleshooting
- **A pod sits in `Init:0/2` or `Init:1/2`.** It is waiting on a dependency. `kubectl -n biblioteka logs <pod> -c wait-for-postgres` (or `-c wait-for-gateway`) prints what it is waiting for.
- **`ErrImageNeverPull` or `ImagePullBackOff`.** The image was not loaded into the node. Re-run step 4, and check `docker images | grep biblioteka` first — if a tag is missing, the build in step 2 did not finish.
- **A 403 on login.** The address in your browser is not listed in `CORS_ALLOWED_ORIGINS` in `k8s/config.env`. Add it (scheme, host and port), `kubectl apply -k k8s/`, and restart the Deployments. See the note in step 3.
- **A 405 right after a pod restart.** The gateway caches the Eureka registry for about 30 seconds, so for a moment it still routes to the address of a pod that no longer exists, the circuit breaker opens, and the fallback handler — which only accepts GET — answers a POST with 405. It clears on its own; wait half a minute and try again.
- **`too many open files` during cluster creation.** WSL's inotify limits are too low for kind. Raise them with `sudo sysctl fs.inotify.max_user_instances=512`.

## Running on k3s (one server, two agents)
The same `k8s/` manifests also run on a multi-node k3s cluster. The server machine runs only the control plane and the k3s add-ons. All eight Biblioteka pods run on the two agents, which here are WSL2 machines.

Three rules keep the workloads off the server and out of trouble:
- The server is tainted `CriticalAddonsOnly=true:NoExecute`, so only k3s's own add-ons run there.
- Every workload selects `node-role.kubernetes.io/worker=worker`, and only the agents carry that label.
- Nothing is pinned by `kubernetes.io/hostname`. If a pinned name does not exactly match the node's label, the kubelet rejects the pod and the ReplicaSet recreates it in an endless loop. That is how this cluster once piled up ~11k dead pods.

**Server** — install `k8s/k3s/server-config.yaml` as `/etc/rancher/k3s/config.yaml`, restart, and put Traefik on port 8080. ServiceLB publishes it on every node, so the app is reachable at `http://<node-ip>:8080` once that origin is listed in `CORS_ALLOWED_ORIGINS` in `k8s/config.env`:

```bash
sudo install -m 600 k8s/k3s/server-config.yaml /etc/rancher/k3s/config.yaml
sudo systemctl restart k3s
kubectl taint nodes "$(hostname)" CriticalAddonsOnly=true:NoExecute --overwrite   # the config only taints at first registration
kubectl apply -f k8s/k3s/traefik-config.yaml
```

**Agents** — first switch WSL to mirrored networking. Otherwise the agents register unreachable NAT addresses and traffic between nodes silently fails; `k8s/k3s/agent-config.yaml` explains the details. Then fill in that file, install it as `/etc/rancher/k3s/config.yaml` on each agent, and restart `k3s-agent`. From the server, label both agents:

```bash
kubectl label node <agent-1> <agent-2> node-role.kubernetes.io/worker=worker --overwrite
```

k3s agents cannot set a `node-role.kubernetes.io/*` label on themselves, which is why this step is manual.

**Images** — the `:dev` images are never pulled from a registry, so each agent needs its own copy:

```bash
docker save biblioteka/eureka-server:dev biblioteka/gateway-service:dev \
  biblioteka/user-service:dev biblioteka/library-service:dev \
  biblioteka/loan-service:dev biblioteka/seat-service:dev \
  biblioteka/ui-service:dev | ssh <agent> sudo k3s ctr images import -
```

Then deploy and seed the database exactly as in steps 5–7 above. `kubectl -n biblioteka get pods -o wide` should show every pod on one of the two agents.

## Application features
### Existing services
This application consists of 5 separate services:
- *UI Service* - **port 8017** - which provides the user with a visual aid for navigating the application
- *Library Management Service* - **port 8081** - which deals exclusively with the books in the library themselves (browsing, adding, deleting books)
- *Loan Management Service* - **port 8083** - which deals only with lending (borrowing and returning books)
- *Seat Management Service* - **port 8084**  - through which you can reserve a table in the library for studying (reserving a table, cancelling reservations, ...)
- *User Service* - **port 8082** - which deals with users and authentication in the system.

All services that need a Swagger UI have one:
![swagger pic](https://github.com/ericges/BibliotekaApp-UI-API/blob/main/Screenshot_58.png)
### Application overview
When you open the link localhost:8080, if you have started everything properly, you should see this:
![login pic](https://github.com/ericges/BibliotekaApp-UI-API/blob/main/Screenshot_60.png)

To log in to the application, you can use any user from the database.

Every password is hashed, and every user profile can be logged into with the password "123".

Once you have successfully logged into the system, you will see this:
![app pic](https://github.com/ericges/BibliotekaApp-UI-API/blob/main/Screenshot_59.png)
(specifically, this is the librarian's view)

