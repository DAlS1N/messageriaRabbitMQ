package com.atividade.messageria.consumer;

import com.atividade.messageria.config.RabbitConfig;
import com.atividade.messageria.dto.PedidoCriadoEvent;
import com.atividade.messageria.store.PedidoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EstoqueConsumer {

    private static final Logger log = LoggerFactory.getLogger(EstoqueConsumer.class);
    private final PedidoStore store;

    public EstoqueConsumer(PedidoStore store) {
        this.store = store;
    }

    @RabbitListener(queues = RabbitConfig.Q_ESTOQUE)
    public void reservar(PedidoCriadoEvent evento) {
        if (evento == null || evento.pedidoId() == null || evento.itens() == null || evento.itens().isEmpty()) {
            throw new AmqpRejectAndDontRequeueException("Estoque: evento invalido, enviando para a DLQ");
        }
        log.info("[ESTOQUE] Reservando {} item(ns) do pedido {}", evento.itens().size(), evento.pedidoId());
        evento.itens().forEach(i ->
                log.info("[ESTOQUE]   -> {} x {}", i.quantidade(), i.produto()));
        store.registrarEtapa(evento.pedidoId(), "ESTOQUE_RESERVADO");
    }
}
