package com.atividade.messageria.controller;

import com.atividade.messageria.model.PedidoCriadoEvent;
import com.atividade.messageria.model.PedidoRequest;
import com.atividade.messageria.producer.PedidoProducer;
import com.atividade.messageria.service.PedidoService;
import com.atividade.messageria.service.PedidoStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;
    private final PedidoStore store;
    private final PedidoProducer producer;

    public PedidoController(PedidoService service, PedidoStore store, PedidoProducer producer) {
        this.service = service;
        this.store = store;
        this.producer = producer;
    }

    /** Cadastro do pedido + publicacao do evento. Responde 202: o processamento e assincrono. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(@Valid @RequestBody PedidoRequest request) {
        PedidoCriadoEvent evento = service.criar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "pedidoId", evento.pedidoId(),
                "valorTotal", evento.valorTotal(),
                "status", "RECEBIDO"));
    }

    /** Mostra por quais etapas o pedido ja passou (estoque, pagamento, notificacao). */
    @GetMapping("/{id}")
    public ResponseEntity<Object> consultar(@PathVariable UUID id) {
        return store.buscar(id)
                .<ResponseEntity<Object>>map(info -> ResponseEntity.ok(Map.of(
                        "pedidoId", info.getId(),
                        "cliente", info.getCliente(),
                        "etapas", info.getEtapas())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Apenas para DEMONSTRACAO da DLQ (requisito 10). */
    @PostMapping("/teste-invalido")
    public ResponseEntity<Map<String, String>> publicarInvalida() {
        producer.publicarMensagemInvalida();
        return ResponseEntity.accepted().body(Map.of("status", "mensagem invalida publicada"));
    }
}
