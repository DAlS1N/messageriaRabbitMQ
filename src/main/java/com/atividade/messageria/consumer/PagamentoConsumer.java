package com.atividade.messageria.consumer;

import com.atividade.messageria.config.RabbitConfig;
import com.atividade.messageria.model.PedidoCriadoEvent;
import com.atividade.messageria.model.PedidoStatusEvent;
import com.atividade.messageria.producer.PedidoProducer;
import com.atividade.messageria.service.PedidoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagamentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoConsumer.class);
    private static final BigDecimal LIMITE = new BigDecimal("10000");

    private final PedidoStore store;
    private final PedidoProducer producer;

    public PagamentoConsumer(PedidoStore store, PedidoProducer producer) {
        this.store = store;
        this.producer = producer;
    }

    @RabbitListener(queues = RabbitConfig.Q_PAGAMENTO)
    public void processar(PedidoCriadoEvent evento) {
        if (evento == null || evento.pedidoId() == null
                || evento.valorTotal() == null || evento.valorTotal().signum() <= 0) {
            throw new AmqpRejectAndDontRequeueException("Pagamento: valor invalido, enviando para a DLQ");
        }

        boolean aprovado = evento.valorTotal().compareTo(LIMITE) <= 0;
        log.info("[PAGAMENTO] Pedido {} valor R$ {} -> {}",
                evento.pedidoId(), evento.valorTotal(), aprovado ? "APROVADO" : "RECUSADO");

        String routingKey = aprovado ? RabbitConfig.RK_PEDIDO_PAGO : RabbitConfig.RK_PEDIDO_RECUSADO;
        String status = aprovado ? "PAGO" : "PAGAMENTO_RECUSADO";
        store.registrarEtapa(evento.pedidoId(), status);

        // Publica um novo evento: a Notificacao (pedido.#) tambem recebe esse
        producer.publicarStatus(routingKey,
                new PedidoStatusEvent(evento.pedidoId(), status,
                        aprovado ? "Pagamento aprovado" : "Pagamento acima do limite de R$ 10.000"));
    }
}
