package com.atividade.messageria.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Exchange principal (TOPIC)
    public static final String EXCHANGE = "pedidos.exchange";

    // Routing keys
    public static final String RK_PEDIDO_CRIADO = "pedido.criado";
    public static final String RK_PEDIDO_PAGO = "pedido.pago";
    public static final String RK_PEDIDO_RECUSADO = "pedido.recusado";

    // Filas
    public static final String Q_ESTOQUE = "pedidos.estoque.queue";
    public static final String Q_PAGAMENTO = "pedidos.pagamento.queue";
    public static final String Q_NOTIFICACAO = "pedidos.notificacao.queue";

    // Dead Letter (mensagens invalidas)
    public static final String DLX = "pedidos.dlx";
    public static final String DLQ = "pedidos.dlq";
    public static final String DLQ_ROUTING_KEY = "invalida";

    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    private Queue filaComDlq(String nome) {
        return QueueBuilder.durable(nome)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue estoqueQueue() {
        return filaComDlq(Q_ESTOQUE);
    }

    @Bean
    public Queue pagamentoQueue() {
        return filaComDlq(Q_PAGAMENTO);
    }

    @Bean
    public Queue notificacaoQueue() {
        return filaComDlq(Q_NOTIFICACAO);
    }

    // Estoque recebe somente pedido.criado
    @Bean
    public Binding estoqueBinding() {
        return BindingBuilder.bind(estoqueQueue()).to(pedidosExchange()).with(RK_PEDIDO_CRIADO);
    }

    // Pagamento recebe somente pedido.criado
    @Bean
    public Binding pagamentoBinding() {
        return BindingBuilder.bind(pagamentoQueue()).to(pedidosExchange()).with(RK_PEDIDO_CRIADO);
    }

    // Notificacao recebe TUDO que comeca com "pedido." (criado, pago, recusado)
    @Bean
    public Binding notificacaoBinding() {
        return BindingBuilder.bind(notificacaoQueue()).to(pedidosExchange()).with("pedido.#");
    }

    // Mensagens em JSON
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
