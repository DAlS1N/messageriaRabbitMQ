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
| 1 | Cadastro de pedido | `PedidoController#criar` / `PedidoService` |
| 2 | Publicação do evento | `PedidoService` (`rabbitTemplate.convertAndSend`) |
| 3 | Consumidor de estoque | `EstoqueConsumer` |
| 4 | Consumidor de pagamento | `PagamentoConsumer` |
| 5 | Consumidor de notificação | `NotificacaoConsumer` |
| 6 | Exchange TOPIC | `RabbitConfig#pedidosExchange` (`pedidos.exchange`) |
| 7 | Routing keys | `pedido.criado`, `pedido.pago`, `pedido.recusado`, binding `pedido.#` |
| 8 | Pelo menos 3 filas | estoque, pagamento, notificação (+ DLQ) |
| 9 | Monitoramento | RabbitMQ Management em http://localhost:15672 (guest/guest) |
| 10 | Mensagens inválidas | validação `@Valid` na API (400) + DLX/DLQ nos consumers |

## Como rodar

```bash
docker compose up -d          # sobe o RabbitMQ (AMQP 5672, Management 15672)
./mvnw spring-boot:run        # ou: mvn spring-boot:run
```

## Testes manuais

Pedido válido (aprovado):
```bash
curl -X POST localhost:8080/pedidos -H "Content-Type: application/json" -d '{
  "cliente": "Kauã",
  "itens": [{"produto":"Teclado","quantidade":2,"precoUnitario":150.00}]
}'
```

Pedido acima de R$ 10.000 (pagamento recusado, routing key `pedido.recusado`):
```bash
curl -X POST localhost:8080/pedidos -H "Content-Type: application/json" -d '{
  "cliente": "Kauã",
  "itens": [{"produto":"Notebook","quantidade":5,"precoUnitario":4000.00}]
}'
```

Consultar as etapas de um pedido: `GET /pedidos/{pedidoId}`

Pedido inválido na API (cliente vazio, quantidade 0) -> `400 Bad Request`, nada vai para o RabbitMQ.

Mensagem inválida direto na fila (JSON quebrado) -> vai para a DLQ:
```bash
curl -X POST localhost:8080/pedidos/teste-invalido
```
Abra Queues and Streams -> `pedidos.dlq` e use "Get messages" para ver o conteúdo
(chegam 3 cópias: uma por fila que recebeu `pedido.criado`).

## Tratamento de mensagens inválidas

- Toda fila é criada com `x-dead-letter-exchange = pedidos.dlx`.
- `spring.rabbitmq.listener.simple.default-requeue-rejected=false`: mensagem rejeitada **não** volta para a fila (evita loop infinito).
- JSON quebrado -> `MessageConversionException` (rejeição fatal automática do Spring AMQP).
- Conteúdo inválido (sem itens, valor <= 0, sem pedidoId) -> `AmqpRejectAndDontRequeueException` no consumer.
- Nenhum consumer lê a `pedidos.dlq` de propósito, para a mensagem ficar visível no Management.
