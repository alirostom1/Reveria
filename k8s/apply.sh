kubectl apply -f k8s/infra/pgsql/configmap.yml
kubectl apply -f k8s/infra/pgsql/pv.yml
kubectl apply -f k8s/infra/pgsql/deployment.yml