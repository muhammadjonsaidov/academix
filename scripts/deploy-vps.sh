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
readonly RUNTIME_DIR="$APP_DIR/infra/.runtime"
readonly SEAWEEDFS_RUNTIME_CONFIG_FILE="$RUNTIME_DIR/seaweedfs-s3-config.json"
readonly LOCK_FILE="$APP_DIR/.deploy.lock"
readonly HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-240}"

log() {
  printf '[deploy] %s\n' "$*"
}

compose() {
  env SEAWEEDFS_CONFIG_FILE="$SEAWEEDFS_RUNTIME_CONFIG_FILE" \
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -f "$PRODUCTION_COMPOSE_FILE" -f "$TUNNEL_COMPOSE_FILE" "$@"
}

environment_value() {
  local key="$1"
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1
}

validate_production_environment() {
  local invalid_keys=()
  local key value forbidden

  while IFS=':' read -r key forbidden; do
    value="$(environment_value "$key")"
    if [[ -z "$value" || "$value" == "$forbidden" ]]; then
      invalid_keys+=("$key")
    fi
  done <<'EOF'
JWT_SECRET:local-dev-only-placeholder-not-for-production-use
DATABASE_PASSWORD:academix
DATABASE_APP_PASSWORD:academix_app_local_dev_only
REDIS_PASSWORD:
RABBITMQ_PASSWORD:academix_local_dev
SEAWEEDFS_ACCESS_KEY:local-dev-access-key
SEAWEEDFS_SECRET_KEY:local-dev-secret-key
EOF

  if (( ${#invalid_keys[@]} > 0 )); then
    log "Production environment has missing or unsafe secrets: ${invalid_keys[*]}"
    return 1
  fi
}

prepare_runtime_configuration() {
  local access_key secret_key temporary_file
  access_key="$(environment_value SEAWEEDFS_ACCESS_KEY)"
  secret_key="$(environment_value SEAWEEDFS_SECRET_KEY)"

  umask 077
  mkdir -p "$RUNTIME_DIR"
  temporary_file="$(mktemp "$RUNTIME_DIR/seaweedfs-s3-config.XXXXXX")"
  cat > "$temporary_file" <<EOF
{
  "identities": [
    {
      "name": "academix",
      "credentials": [
        {
          "accessKey": "$access_key",
          "secretKey": "$secret_key"
        }
      ],
      "actions": ["Admin", "Read", "Write"]
    }
  ]
}
EOF
  mv "$temporary_file" "$SEAWEEDFS_RUNTIME_CONFIG_FILE"
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
  validate_production_environment
  prepare_runtime_configuration
  compose up --detach --build --remove-orphans
  # nginx resolves Docker service names only when its worker starts. When a
  # rebuilt backend gets a new container IP, an unchanged nginx-dev container
  # would otherwise keep proxying to the old address and return 502 until its
  # next restart. Recreate only the ingress after the stack is up so every
  # deployment points it at the current backend/frontend containers.
  compose up --detach --force-recreate nginx-dev
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
