#!/bin/bash
# Boots the Task 2 backend (Quarkus, port 8081) and frontend (Vite via Deno,
# port 5174) together in this one terminal, so it can run alongside Task 1
# without port clashes. Ctrl+C stops both.
trap 'kill $(jobs -p)' EXIT

(cd backend && \
  MONGODB_URI="mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/quiz_db" \
  FRONTEND_ORIGIN="http://localhost:5174" \
  mvn quarkus:dev -Dquarkus.http.port=8081) &

(cd frontend && \
  [ -f .env ] || cp .env.example .env && \
  deno install && \
  deno task dev) &

wait
