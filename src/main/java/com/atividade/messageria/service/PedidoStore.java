package com.atividade.messageria.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** "Banco de dados" em memoria, so para a demonstracao. */
@Component
public class PedidoStore {

    public static class PedidoInfo {
        private final UUID id;
        private final String cliente;
        private final List<String> etapas = new CopyOnWriteArrayList<>();

        public PedidoInfo(UUID id, String cliente) {
            this.id = id;
            this.cliente = cliente;
            this.etapas.add("RECEBIDO");
        }

        public UUID getId() { return id; }
        public String getCliente() { return cliente; }
        public List<String> getEtapas() { return new ArrayList<>(etapas); }
    }

    private final Map<UUID, PedidoInfo> pedidos = new ConcurrentHashMap<>();

    public void salvar(UUID id, String cliente) {
        pedidos.put(id, new PedidoInfo(id, cliente));
    }

    public void registrarEtapa(UUID id, String etapa) {
        PedidoInfo info = pedidos.get(id);
        if (info != null) {
            info.etapas.add(etapa);
        }
    }

    public Optional<PedidoInfo> buscar(UUID id) {
        return Optional.ofNullable(pedidos.get(id));
    }
}
