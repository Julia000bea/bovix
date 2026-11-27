-- Machos no rebanho: Bezerro e Touro (além das categorias de fêmeas que já existiam).
ALTER TABLE animais DROP CONSTRAINT IF EXISTS animais_categoria_check;
ALTER TABLE animais ADD CONSTRAINT animais_categoria_check
    CHECK (categoria IN ('Bezerra','Novilha','Vaca lactante','Vaca seca','Bezerro','Touro'));
