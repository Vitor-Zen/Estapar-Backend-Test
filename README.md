# Estapar Backend - Teste Técnico

Sistema de gerenciamento de estacionamento desenvolvido em Java com Spring Boot.

## Tecnologias
- Java 21
- Spring Boot
- MySQL

## Como rodar

### Pré-requisitos
- Docker (para o simulador)
- Java 21

### Simulador
```bash
docker run -d -p 3000:3000 cfontes0estapar/garage-sim:1.0.0
```

### Banco de dados
```bash
docker run -d --name mysql-estapar -e MYSQL_ROOT_PASSWORD=your_password -e MYSQL_DATABASE=estapar -p 3306:3306 mysql:8
```
### Aplicação
Configure o `application.properties` baseado no `application.properties.example` e rode:
```bash
./mvnw spring-boot:run
```

### Observações
- A aplicação roda na porta `3003` (necessário para receber os eventos do simulador via webhook)
- Ao iniciar, a aplicação busca automaticamente os dados do simulador em `GET /garage` e persiste no banco