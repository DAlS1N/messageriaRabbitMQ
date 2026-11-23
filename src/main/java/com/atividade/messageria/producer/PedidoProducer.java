package com.atividade.messageria.producer;

import com.atividade.messageria.config.RabbitConfig;
import com.atividade.messageria.model.PedidoCriadoEvent;
import com.atividade.messageria.model.PedidoStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Unico ponto da aplicacao que publica mensagens no RabbitMQ (requisito 2). */
@Component
public class PedidoProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Evento de pedido criado: exchange TOPIC + routing key pedido.criado. */
    public void publicarPedidoCriado(PedidoCriadoEvent evento) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_PEDIDO_CRIADO, evento);
        log.info("[PRODUCER] Pedido {} publicado em '{}' com routing key '{}'",
                evento.pedidoId(), RabbitConfig.EXCHANGE, RabbitConfig.RK_PEDIDO_CRIADO);
    }

    /** Evento de resultado do pagamento: pedido.pago ou pedido.recusado. */
    public void publicarStatus(String routingKey, PedidoStatusEvent evento) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, evento);
        log.info("[PRODUCER] Status '{}' do pedido {} publicado com routing key '{}'",
                evento.status(), evento.pedidoId(), routingKey);
    }

    /** Somente para DEMONSTRACAO: publica um JSON quebrado para provar o tratamento de mensagens invalidas. */
    public void publicarMensagemInvalida() {
        MessageProperties props = new MessageProperties();
        props.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Message msg = MessageBuilder
                .withBody("{ isto nao e um pedido valido".getBytes(StandardCharsets.UTF_8))
                .andProperties(props)
                .build();
        rabbitTemplate.send(RabbitConfig.EXCHANGE, RabbitConfig.RK_PEDIDO_CRIADO, msg);
        log.warn("[PRODUCER] Mensagem invalida publicada de proposito (teste da DLQ)");
    }
}
