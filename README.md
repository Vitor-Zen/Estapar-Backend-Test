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

### Aplicação
Configure o `application.properties` baseado no `application.properties.example` e rode:
```bash
./mvnw spring-boot:run
```