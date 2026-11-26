# Análise dos dados do TCC

Script que lê o CSV exportado pelo Bovix (aba **Indicadores → Exportar dados para análise**) e gera uma
análise exploratória com modelos de base para comparação.

## Como usar

```bash
cd analise
python -m venv .venv
.venv\Scripts\activate          # Windows (no Linux/macOS: source .venv/bin/activate)
pip install -r requirements.txt
python analise_tcc.py caminho/bovix_dados_2025-01-01_2026-11-30.csv --saida resultados
```

Abra `resultados/resumo.md` (no VS Code: botão de pré-visualização do Markdown).

Opções:
- `--teste 0.25`: fração final dos dias usada como teste (padrão 25%).
- `--sem-estimados`: descarta os dias com produção estimada (coleta de vários dias).

## O que o script faz

1. **Qualidade dos dados:** dias com dado e % faltando em cada coluna, com avisos (poucos dias, clima incompleto,
   dias estimados, meses do ano cobertos).
2. **Correlações** (Pearson e Spearman, com o número de dias usados) entre litros por vaca em lactação e DEL,
   vacas em lactação, ITU (de hoje até 7 dias atrás), indicadores reprodutivos e ocorrências.
3. **Modelos de base**, com divisão temporal (os últimos dias ficam para teste, sem embaralhar):
   - persistência (amanhã = hoje), a referência mínima;
   - regressão linear (OLS) explicativa, com erros-padrão HAC (Newey-West), para medir o efeito de DEL e ITU;
   - Random Forest preditiva, com a produção de ontem.
4. **Gráficos:** séries, ITU × produção, correlação por defasagem e real × previsto.

## Cuidados

- São **pontos de partida**, não o modelo final. Entenda e justifique cada escolha no texto do TCC.
- Correlação não é causa: DEL, estação do ano e ITU variam juntos. Com menos de 12 meses de dados, o efeito
  do calor pode se confundir com o da estação.
- O ideal é ter pelo menos 6 a 12 meses de dados diários, cobrindo verão e inverno.
- O efeito do ITU costuma aparecer com 1 a 3 dias de atraso: veja o gráfico de correlação por defasagem.
