#!/bin/bash
set -e

NAMESPACE=${NAMESPACE:-reveria-dev}
ALL_SERVICES="user-service api-gateway content-service social-service frontend"

deploy_service() {
  local service=$1
  echo "Restarting deployment/${service} in namespace ${NAMESPACE}..."
  kubectl rollout restart "deployment/${service}" -n "${NAMESPACE}"
  kubectl rollout status "deployment/${service}" -n "${NAMESPACE}" --timeout=120s
  echo "✅ ${service} deployed successfully."
}

if [ -z "$1" ]; then
  echo "Deploying all services in namespace ${NAMESPACE}..."
  for svc in $ALL_SERVICES; do
    deploy_service "$svc"
  done
  echo "✅ All services deployed."
elif [ "$1" = "--help" ]; then
  echo "Usage: bash deploy.sh [service-name]"
  echo "  No args    - deploy all services"
  echo "  <service>  - deploy a single service"
  echo "Examples:"
  echo "  bash deploy.sh"
  echo "  bash deploy.sh user-service"
else
  deploy_service "$1"
fi
