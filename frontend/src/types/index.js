/**
 * Bovix — Tipagem dos dados (JSDoc, projeto em JavaScript puro).
 * Espelha as entidades do backend (com.bovix.model.*) e as tabelas do PostgreSQL.
 */

/**
 * @typedef {Object} Animal
 * @property {number} id
 * @property {number} fazendaId
 * @property {string} brinco
 * @property {string} [nome]
 * @property {string} [raca]
 * @property {string} [nascimento]      - 'YYYY-MM-DD'
 * @property {'Bezerra'|'Novilha'|'Vaca lactante'|'Vaca seca'} categoria
 * @property {'Ativa'|'Vendida'|'Morta'} status
 * @property {string} [notas]
 * @property {string} [fotoUrl]         - URL pública retornada pela API após upload
 */

/**
 * @typedef {Object} EventoReproducao
 * @property {number} id
 * @property {number} animalId
 * @property {'cio'|'inseminacao'|'diagnostico'|'parto'|'secagem'} tipo
 * @property {string} data              - 'YYYY-MM-DD'
 * @property {string} [detalhe]
 * @property {'positivo'|'negativo'} [resultado]
 */

/**
 * @typedef {Object} EventoSaude
 * @property {number} id
 * @property {number} animalId
 * @property {'vacina'|'medicamento'|'vermifugo'|'outro'} tipo
 * @property {string} produto
 * @property {string} data
 * @property {number} [carenciaLeiteDias]
 * @property {number} [proximaAplicacaoDias]
 * @property {string} [observacoes]
 */

/**
 * @typedef {Object} Ordenha
 * @property {number} id
 * @property {number} fazendaId
 * @property {number|null} animalId     - null = total do rebanho
 * @property {string} data
 * @property {'Manhã'|'Tarde'|'Dia todo'} periodo
 * @property {number} litros
 */

/**
 * @typedef {Object} Financeiro
 * @property {number} id
 * @property {number} fazendaId
 * @property {string} data
 * @property {'Ração'|'Insumos'|'Medicamentos'|'Mão de obra'|'Manutenção'|'Outros'} categoria
 * @property {string} [descricao]
 * @property {number} valor
 */

/**
 * @typedef {Object} Configuracao
 * @property {number} id
 * @property {number} fazendaId
 * @property {number} gestacaoDias
 * @property {number} cioDias
 */

export {}; // arquivo só de tipos (JSDoc)
