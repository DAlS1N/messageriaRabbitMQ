package com.atividade.messageria.service;

import com.atividade.messageria.model.PedidoCriadoEvent;
import com.atividade.messageria.model.PedidoRequest;
import com.atividade.messageria.producer.PedidoProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Requisito 1: cadastro de pedido. Salva o pedido e delega a publicacao ao producer. */
@Service
public class PedidoService {

    private final PedidoProducer producer;
    private final PedidoStore store;

    public PedidoService(PedidoProducer producer, PedidoStore store) {
        this.producer = producer;
        this.store = store;
    }

    public PedidoCriadoEvent criar(PedidoRequest request) {
        BigDecimal total = request.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                UUID.randomUUID(), request.cliente(), request.itens(), total, Instant.now());

        store.salvar(evento.pedidoId(), evento.cliente());
        producer.publicarPedidoCriado(evento);
        return evento;
    }
}
