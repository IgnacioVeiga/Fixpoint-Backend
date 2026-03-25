#!/usr/bin/env bash
set -euo pipefail

ENVIRONMENT="${1:-dev}"
MODE="${2:-local}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.${ENVIRONMENT}"
DB_CONTAINER_NAME="Fixpoint_DB"

wait_for_db() {
  local attempts=30
  local sleep_seconds=2

  for ((i=1; i<=attempts; i++)); do
    local health
    health="$(docker inspect --format '{{.State.Health.Status}}' "${DB_CONTAINER_NAME}" 2>/dev/null || true)"
    if [[ "${health}" == "healthy" ]]; then
      echo "Database container '${DB_CONTAINER_NAME}' is healthy."
      return 0
    fi

    echo "Waiting for database health (${i}/${attempts})..."
    sleep "${sleep_seconds}"
  done

  echo "Database container '${DB_CONTAINER_NAME}' did not become healthy in time."
  return 1
}

if [[ "${MODE}" == "docker" || "${MODE}" == "auto" ]]; then
  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "Environment file not found: ${ENV_FILE}"
    echo "Copy .env.example to ${ENV_FILE} and complete the required values."
    exit 1
  fi

  echo "Starting PostgreSQL container for backend using '${ENV_FILE}'..."
  (
    cd "${PROJECT_ROOT}"
    docker compose --env-file "${ENV_FILE}" up -d postgres
  )
  if [[ "${MODE}" == "docker" ]]; then
    exit $?
  fi

  wait_for_db
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Environment file not found: ${ENV_FILE}"
  echo "Copy .env.example to ${ENV_FILE} and complete the required values."
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-${ENVIRONMENT}}"

echo "Starting backend locally with profile '${SPRING_PROFILES_ACTIVE}' using '${ENV_FILE}'..."
(
  cd "${PROJECT_ROOT}"
  ./mvnw spring-boot:run
)
