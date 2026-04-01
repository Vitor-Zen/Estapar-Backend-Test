# Estapar Backend - Teste Técnico

Sistema de gerenciamento de estacionamento desenvolvido em Java com Spring Boot.

## Tecnologias
- Java 21
- Spring Boot
- MySQL
- Docker

## Arquitetura
- REST API com Spring Web
- Persistência com Spring Data JPA + MySQL
- Mapeamento DTO → Entity com Mappers
- Tratamento global de erros com `@ControllerAdvice`
- Testes unitários com JUnit 5 e Mockito

## Como rodar

### Pré-requisitos
- Docker
- Java 21

### Simulador
```bash
docker run -d -p 3000:3000 cfontes0estapar/garage-sim:1.0.0
```

### Banco de dados
```bash
docker run -d --name mysql-estapar \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=estapar \
  -p 3306:3306 mysql:8
```

### Aplicação
Configure o `application.properties` baseado no `application.properties.example` e rode:
```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta `3003` (necessário para receber os eventos do simulador via webhook).
Ao iniciar, busca automaticamente os dados do simulador em `GET /garage` e persiste no banco.

## Endpoints

### Webhook
```
POST /webhook
```
Recebe eventos do simulador.

**ENTRY**
```json
{
    "license_plate": "ZUL0001",
    "entry_time": "2025-01-01T12:00:00.000Z",
    "event_type": "ENTRY"
}
```

**PARKED**
```json
{
    "license_plate": "ZUL0001",
    "lat": -23.561684,
    "lng": -46.655981,
    "event_type": "PARKED"
}
```

**EXIT**
```json
{
    "license_plate": "ZUL0001",
    "exit_time": "2025-01-01T14:30:00.000Z",
    "event_type": "EXIT"
}
```

### Receita
```
GET /revenue
```
Retorna a receita total por setor e data.

Request:
```json
{
    "date": "2025-01-01",
    "sector": "A"
}
```

Response:
```json
{
    "amount": 109.35,
    "currency": "BRL",
    "timestamp": "2025-01-01T12:00:00.000Z"
}
```

## Observações
- Para resetar o simulador: pare e remova o container, depois crie um novo
- Ao reiniciar a aplicação, o banco é limpo e os dados são recarregados automaticamente