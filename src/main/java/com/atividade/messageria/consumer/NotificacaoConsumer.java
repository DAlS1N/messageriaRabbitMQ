package com.atividade.messageria.consumer;

import com.atividade.messageria.config.RabbitConfig;
import com.atividade.messageria.store.PedidoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Recebe todos os eventos "pedido.#": usa Map porque o formato muda (criado x status). */
@Component
public class NotificacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoConsumer.class);
    private final PedidoStore store;

    public NotificacaoConsumer(PedidoStore store) {
        this.store = store;
    }

    @RabbitListener(queues = RabbitConfig.Q_NOTIFICACAO)
    public void notificar(@Payload Map<String, Object> evento,
                          @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        Object id = evento == null ? null : evento.get("pedidoId");
        if (id == null) {
            throw new AmqpRejectAndDontRequeueException("Notificacao: evento sem pedidoId, enviando para a DLQ");
        }
        log.info("[NOTIFICACAO] Evento '{}' do pedido {} -> enviando e-mail/SMS ao cliente", routingKey, id);
        store.registrarEtapa(UUID.fromString(id.toString()), "NOTIFICADO(" + routingKey + ")");
    }
}
