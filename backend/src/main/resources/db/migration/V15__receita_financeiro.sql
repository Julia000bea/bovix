-- Receitas no financeiro (venda de leite). Lançamentos antigos são todos despesas.
ALTER TABLE financeiro
    ADD COLUMN tipo VARCHAR(10) NOT NULL DEFAULT 'despesa' CHECK (tipo IN ('despesa', 'receita'));

ALTER TABLE financeiro DROP CONSTRAINT IF EXISTS financeiro_categoria_check;
ALTER TABLE financeiro ADD CONSTRAINT financeiro_categoria_check
    CHECK (categoria IN ('Ração','Insumos','Medicamentos','Mão de obra','Manutenção','Outros','Venda de leite'));

-- "Venda de leite" só faz sentido como receita.
ALTER TABLE financeiro ADD CONSTRAINT financeiro_venda_leite_receita
    CHECK (categoria <> 'Venda de leite' OR tipo = 'receita');

CREATE INDEX idx_financeiro_fazenda_tipo_data ON financeiro(fazenda_id, tipo, data);
