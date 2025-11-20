# Wide-Path

Wide-Path pairs a lightweight Java API for bidirectional A* path queries with a React + Vite frontend for visualizing results. This guide turns the repository into a runnable, Linux-friendly project with clear dependency and startup instructions.

## Prerequisites (Linux)
- **Java 17+ JDK** (for `javac` and `java`).
- **Node.js 18+ and npm** (for the Vite frontend).
- **curl** (optional, handy for quick API smoke tests).

Example install on Ubuntu/Debian:
```bash
sudo apt update
sudo apt install openjdk-17-jdk nodejs npm curl
```

## Project layout
- `src/` – Java sources for the API server and bidirectional A* implementation.
- `frontend/` – React + Vite UI for interacting with the API.
- `run.sh` – Helper script to build and run the stack with or without the frontend.

## Quick start (full stack)
1. Ensure the prerequisites above are installed.
2. From the repo root, run:
   ```bash
   ./run.sh
   ```
   This compiles the Java sources, starts the API on port **8080**, and launches the Vite dev server on **5173** with `VITE_API_BASE` pointed at the API. Visit http://localhost:5173 to use the UI.

Options:
- `BACKEND_PORT` and `FRONTEND_PORT` environment variables override the defaults.
- `--port` and `--frontend-port` flags set the ports explicitly.

## Running without the frontend
Run only the Java API server (good for headless benchmarking or hooking up a different client):
```bash
./run.sh --backend-only --port 9000
```
The server seeds a tiny in-memory network so the endpoints respond immediately even without a dataset on disk.

## Running only the frontend
Point the UI at an existing API host (for example, a remote or already running backend):
```bash
./run.sh --frontend-only --api-base "http://localhost:8080/api" --frontend-port 5173
```
Alternatively, run the commands manually:
```bash
cd frontend
npm install
VITE_API_BASE="http://localhost:8080/api" npm run dev -- --host 0.0.0.0 --port 5173
```
You can also copy `frontend/.env.example` to `.env` and edit `VITE_API_BASE` once instead of passing it each run.

## Manual backend build/run
If you prefer not to use `run.sh` you can compile and start the API directly:
```bash
javac -d build src/*.java
java -cp build ApiServer 8080
```
The server exposes:
- `GET /api/nodes?search=<id substring>` – simple node search.
- `GET /api/network/meta` – vertex count and bounding box.
- `POST /api/queries/run` – run a bidirectional A* query.
- `GET /api/metrics/live` – JVM memory snapshot for the live metrics widget.

## Production frontend build
For a static build of the UI:
```bash
cd frontend
npm install
npm run build
```
The output lands in `frontend/dist/` and can be served by any static file server. Set `VITE_API_BASE` at build time to point at your deployed API URL.

## Troubleshooting
- If `run.sh` reports missing commands, install the prerequisite packages above.
- Use `--api-base` when the frontend and backend are on different hosts/ports.
- Stop the script with `Ctrl+C`; it will shut down the background Java process when started in full-stack mode.
