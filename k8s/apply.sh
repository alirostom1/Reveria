#POSTGRES
kubectl apply -f k8s/infra/pgsql/configmap.yml
kubectl apply -f k8s/infra/pgsql/pv.yml
kubectl apply -f k8s/infra/pgsql/deployment.yml

#REDIS
kubectl apply -f k8s/infra/redis/pv.yml
kubectl apply -f k8s/infra/redis/deployment.yml

#KAFKA
kubectl apply -f k8s/infra/kafka/pv.yml
kubectl apply -f k8s/infra/kafka/deployment.yml

#JENKINS
kubectl apply -f k8s/infra/jenkins/pv.yml
kubectl apply -f k8s/infra/jenkins/deployment.yml

