# Sistema de Pedidos com Spring Boot + RabbitMQ

API REST que recebe pedidos e os publica em um exchange TOPIC do RabbitMQ.
Três consumidores (Estoque, Pagamento, Notificação) processam de forma assíncrona.

```
Cliente --POST /pedidos--> Pedido API --> [pedidos.exchange (TOPIC)]
                                              |  pedido.criado  -> pedidos.estoque.queue     -> EstoqueConsumer
                                              |  pedido.criado  -> pedidos.pagamento.queue   -> PagamentoConsumer
                                              |  pedido.#       -> pedidos.notificacao.queue -> NotificacaoConsumer
                                              |
   PagamentoConsumer publica de volta:  pedido.pago / pedido.recusado  (a Notificação recebe também)

   Mensagem inválida -> [pedidos.dlx] -> pedidos.dlq
```

## Requisitos da atividade x onde está

| # | Requisito | Onde |
|---|-----------|------|
| 1 | Cadastro de pedido | `controller/PedidoController#criar` + `service/PedidoService` |
| 2 | Publicação do evento | `producer/PedidoProducer#publicarPedidoCriado` |
| 3 | Consumidor de estoque | `consumer/EstoqueConsumer` |
| 4 | Consumidor de pagamento | `consumer/PagamentoConsumer` |
| 5 | Consumidor de notificação | `consumer/NotificacaoConsumer` |
| 6 | Exchange TOPIC | `config/RabbitConfig#pedidosExchange` (`pedidos.exchange`) |
| 7 | Routing keys | `pedido.criado`, `pedido.pago`, `pedido.recusado`, binding `pedido.#` |
| 8 | Pelo menos 3 filas | estoque, pagamento, notificação (+ DLQ) em `config/RabbitConfig` |
| 9 | Monitoramento | RabbitMQ Management em http://localhost:15672 (guest/guest) |
| 10 | Mensagens inválidas | `@Valid` na API (400) + DLX/DLQ + `AmqpRejectAndDontRequeueException` nos consumers |

## Estrutura

```
com.atividade.messageria
├── MessageriaApplication
├── config/       RabbitConfig (exchange, filas, bindings, DLQ, conversor JSON)
├── controller/   PedidoController, ApiExceptionHandler
├── model/        PedidoRequest, ItemPedido, PedidoCriadoEvent, PedidoStatusEvent
├── producer/     PedidoProducer (publica no RabbitMQ)
├── service/      PedidoService, PedidoStore (memória)
└── consumer/     EstoqueConsumer, PagamentoConsumer, NotificacaoConsumer
```

## Como rodar

Pré-requisitos: JDK 17 ou superior e um RabbitMQ em `localhost:5672`.

**1. Subir o RabbitMQ** (uma das opções):

```bash
docker compose up -d          # AMQP 5672, Management 15672
```

Ou instale o RabbitMQ localmente (Erlang + RabbitMQ Server) e habilite o painel com
`rabbitmq-plugins enable rabbitmq_management`.

Confirme abrindo http://localhost:15672 (guest / guest).

**2. Subir a aplicação** (uma das opções):

- **IntelliJ / IDE:** abra a pasta do projeto, aguarde o Maven baixar as dependências e execute a classe
  `MessageriaApplication`.
- **Terminal, com Maven instalado:** `mvn spring-boot:run`

## Testes manuais

Os exemplos abaixo usam o PowerShell (Windows). Se preferir, use o Postman ou o `curl` do Linux/macOS.

Pedido válido (aprovado):
```powershell
$body = '{"cliente":"Kauã","itens":[{"produto":"Teclado","quantidade":2,"precoUnitario":150.00}]}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/pedidos -ContentType "application/json" -Body $body
```

Pedido acima de R$ 10.000 (pagamento recusado, routing key `pedido.recusado`):
```powershell
$body = '{"cliente":"Kauã","itens":[{"produto":"Notebook","quantidade":5,"precoUnitario":4000.00}]}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/pedidos -ContentType "application/json" -Body $body
```

Consultar as etapas de um pedido:
```powershell
Invoke-RestMethod http://localhost:8080/pedidos/<pedidoId>
```

Pedido inválido na API (cliente vazio, quantidade 0) -> `400 Bad Request`, nada vai para o RabbitMQ.

Mensagem inválida direto na fila (JSON quebrado) -> vai para a DLQ:
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/pedidos/teste-invalido
```
Abra Queues and Streams -> `pedidos.dlq` e use "Get messages" para ver o conteúdo
(chegam 3 cópias: uma por fila que recebeu `pedido.criado`).

## Tratamento de mensagens inválidas

- Toda fila é criada com `x-dead-letter-exchange = pedidos.dlx`.
- `spring.rabbitmq.listener.simple.default-requeue-rejected=false`: mensagem rejeitada **não** volta para a fila (evita loop infinito).
- JSON quebrado -> `MessageConversionException` (rejeição fatal automática do Spring AMQP).
- Conteúdo inválido (sem itens, valor <= 0, sem pedidoId) -> `AmqpRejectAndDontRequeueException` no consumer.
- Nenhum consumer lê a `pedidos.dlq` de propósito, para a mensagem ficar visível no Management.
