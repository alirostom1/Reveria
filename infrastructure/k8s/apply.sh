#SECRETS
kubectl create secret generic postgresql-secret --from-env-file=k8s/secrets/.env.pgsql -n reveria-dev --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic minio-secret --from-env-file=k8s/secrets/.env.minio -n reveria-dev --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic user-service-secret --from-env-file=k8s/secrets/.env.user-service -n reveria-dev --dry-run=client -o yaml | kubectl apply -f -

#POSTGRES
kubectl apply -f k8s/pgsql/configmap.yml
kubectl apply -f k8s/pgsql/pv.yml
kubectl apply -f k8s/pgsql/deployment.yml

#REDIS
kubectl apply -f k8s/redis/pv.yml
kubectl apply -f k8s/redis/deployment.yml

#KAFKA
kubectl apply -f k8s/kafka/pv.yml
kubectl apply -f k8s/kafka/deployment.yml

#JENKINS
kubectl apply -f k8s/jenkins/pv.yml
kubectl apply -f k8s/jenkins/deployment.yml

#MINIO
kubectl apply -f k8s/minio/pv.yml
kubectl apply -f k8s/minio/deployment.yml

#USER-SERVICE
kubectl apply -f k8s/services/user-service/deployment.yml
