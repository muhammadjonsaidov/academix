#!/usr/bin/env bash
# Deploy a commit that has already passed GitHub CI. This script runs on the VPS
# and deliberately keeps the production secrets in infra/.env on that host.

set -Eeuo pipefail

readonly DEPLOY_REVISION="${1:?Usage: deploy-vps.sh <git-sha>}"
readonly APP_DIR="${ACADEMIX_APP_DIR:-/home/ubuntu/academixai/academix}"
readonly COMPOSE_FILE="$APP_DIR/infra/docker-compose.yml"
readonly PRODUCTION_COMPOSE_FILE="$APP_DIR/infra/docker-compose.production.yml"
readonly TUNNEL_COMPOSE_FILE="$APP_DIR/infra/docker-compose.tunnel.yml"
readonly ENV_FILE="$APP_DIR/infra/.env"
readonly LOCK_FILE="$APP_DIR/.deploy.lock"
readonly HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-240}"

log() {
  printf '[deploy] %s\n' "$*"
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -f "$PRODUCTION_COMPOSE_FILE" -f "$TUNNEL_COMPOSE_FILE" "$@"
}

wait_for_healthy_stack() {
  local elapsed=0

  until compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health \
      | grep --quiet '"status":"UP"'; do
    if (( elapsed >= HEALTH_TIMEOUT_SECONDS )); then
      log "backend health check did not succeed within ${HEALTH_TIMEOUT_SECONDS}s"
      return 1
    fi
    sleep 5
    elapsed=$((elapsed + 5))
  done

  compose exec -T frontend node -e "fetch('http://127.0.0.1:3000/').then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"
  compose ps --status running --services | grep --quiet '^telegram-bot$'
}

deploy_revision() {
  local revision="$1"

  # Fetching main first ensures that GitHub advertises the verified commit;
  # fetching a raw SHA is disabled by some Git servers.
  git fetch --quiet origin main
  git cat-file -e "${revision}^{commit}"
  git checkout --quiet --detach "$revision"
  [[ -f "$COMPOSE_FILE" ]] || { log "Missing Compose file: $COMPOSE_FILE"; return 1; }
  [[ -f "$PRODUCTION_COMPOSE_FILE" ]] || {
    log "Missing production Compose file: $PRODUCTION_COMPOSE_FILE"
    return 1
  }
  [[ -f "$TUNNEL_COMPOSE_FILE" ]] || { log "Missing tunnel Compose file: $TUNNEL_COMPOSE_FILE"; return 1; }
  compose up --detach --build --remove-orphans
  wait_for_healthy_stack
}

main() {
  [[ -f "$ENV_FILE" ]] || { log "Missing production environment file: $ENV_FILE"; exit 1; }

  cd "$APP_DIR"
  exec 9>"$LOCK_FILE"
  flock --nonblock 9 || { log "Another deployment is already running"; exit 1; }

  # Production keeps host-only files (for example infra/.env) outside Git. Only
  # tracked-file changes are unsafe because checkout could overwrite them.
  if ! git diff --quiet || ! git diff --cached --quiet; then
    log "Refusing deployment: the server checkout has modified tracked files"
    exit 1
  fi

  local previous_revision
  previous_revision="$(git rev-parse HEAD)"
  log "Deploying $DEPLOY_REVISION (previous: $previous_revision)"

  if deploy_revision "$DEPLOY_REVISION"; then
    printf '%s\n' "$DEPLOY_REVISION" > "$APP_DIR/.deployed-revision"
    log "Deployment completed successfully"
    return
  fi

  log "Deployment failed; rolling back to $previous_revision"
  deploy_revision "$previous_revision"
  log "Rollback completed"
  exit 1
}

main "$@"
