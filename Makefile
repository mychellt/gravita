DOCERCOMPOSECOMMAD=docker compose

docker-compose-up:
	$(DOCERCOMPOSECOMMAD) -f ./docker-compose.yaml up -d --force-recreate

docker-compose-down:
	$(DOCERCOMPOSECOMMAD) -f ./docker-compose.yaml down -v --remove-orphans