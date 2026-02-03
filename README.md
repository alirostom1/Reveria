# Reveria

kubectl create secret generic postgresql-secret \ --from-env-file=.env \ -n reveria-dev

docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \ --bootstrap-server localhost:9092 \ --topic user-events \ --from-beginning \ --property print.key=true
