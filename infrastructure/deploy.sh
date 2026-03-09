#!/bin/bash
set -e

SERVICE=$1
NAMESPACE=${2:-reveria-dev}

if [ -z "$SERVICE" ]; then
  echo "Usage: bash deploy.sh <service-name> [namespace]"
  echo "Example: bash deploy.sh user-service"
  exit 1
fi

echo "Restarting deployment/${SERVICE} in namespace ${NAMESPACE}..."
kubectl rollout restart "deployment/${SERVICE}" -n "${NAMESPACE}"
kubectl rollout status "deployment/${SERVICE}" -n "${NAMESPACE}" --timeout=120s
echo "✅ ${SERVICE} deployed successfully."
