-- Los clientes pueden registrar pedidos desde el menú: el pedido queda asociado al cliente
-- y el mesero se asigna cuando el personal lo atiende.
ALTER TABLE pedido ALTER COLUMN mesero_id DROP NOT NULL;
ALTER TABLE pedido ADD COLUMN cliente_id BIGINT REFERENCES usuario (id);
ALTER TABLE pedido ADD CONSTRAINT ck_pedido_responsable
    CHECK (mesero_id IS NOT NULL OR cliente_id IS NOT NULL);

CREATE INDEX ix_pedido_cliente ON pedido (cliente_id);
