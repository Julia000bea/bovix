"""
Análise exploratória e modelos de base para o TCC, a partir do CSV exportado pelo Bovix
(aba Indicadores → "Exportar dados para análise").

Uso:
    python analise_tcc.py bovix_dados_AAAA-MM-DD_AAAA-MM-DD.csv [--saida resultados] [--teste 0.25]

Gera, na pasta de saída:
    resumo.md                    qualidade dos dados, correlações e resultados dos modelos
    01_series.png                litros por vaca, vacas em lactação e ITU ao longo do tempo
    02_itu_x_litros.png          dispersão ITU médio (dia anterior) × litros por vaca
    03_correlacao_defasagem.png  correlação entre o ITU de k dias atrás e a produção de hoje
    04_previsto_x_real.png       período de teste: real × previsto pelos modelos

Importante: os modelos aqui são PONTOS DE PARTIDA (base de comparação), não o modelo final do TCC.
Leia cada etapa, entenda as escolhas e ajuste ao seu objetivo e à sua banca.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # gera arquivos PNG sem abrir janela
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

ALVO_PADRAO = "litros_por_vaca_em_lactacao"
COLUNAS_NUMERICAS = [
    "litros_total", "estimado", "vacas_em_lactacao", "del_medio", "litros_por_vaca_em_lactacao",
    "itu_medio", "itu_max", "horas_itu_acima_72", "iep_medio_12m", "vacas_vazias_acima_x",
    "perdas_gestacionais_30d",
]
TIPOS_OCORRENCIA = ["alimentacao", "agua", "equipamento", "clima", "sanidade", "manejo", "outro"]
DEFASAGENS_ITU = range(0, 8)  # ITU de hoje até 7 dias atrás

# mesmas cores validadas da aba Indicadores (azul e laranja; seguras para daltonismo)
AZUL, LARANJA, CINZA = "#2a78d6", "#eb6834", "#8a8f86"


# ----------------------------------------------------------------------------- 1. carregar

def carregar(caminho: Path) -> pd.DataFrame:
    """Lê o CSV do Bovix. Célula vazia = sem dado (NaN), nunca zero."""
    df = pd.read_csv(caminho, sep=",", decimal=".", encoding="utf-8", dtype={"ocorrencias": "string"})
    faltando = {"data", ALVO_PADRAO, "itu_medio"} - set(df.columns)
    if faltando:
        sys.exit(f"Arquivo sem as colunas {sorted(faltando)}. Use o CSV exportado pela aba Indicadores do Bovix.")
    df["data"] = pd.to_datetime(df["data"], format="%Y-%m-%d")
    df = df.set_index("data").sort_index()
    # garante um dia por linha, mesmo se o arquivo tiver buracos
    df = df.reindex(pd.date_range(df.index.min(), df.index.max(), freq="D"))
    for c in COLUNAS_NUMERICAS:
        if c in df.columns:
            df[c] = pd.to_numeric(df[c], errors="coerce")
    ocorr = df.get("ocorrencias", pd.Series(index=df.index, dtype="string")).fillna("")
    df["tem_ocorrencia"] = (ocorr != "").astype(int)
    for tipo in TIPOS_OCORRENCIA:
        df[f"ocorr_{tipo}"] = ocorr.str.split("|").apply(lambda tipos, t=tipo: int(t in tipos))
    return df


# ----------------------------------------------------------------------------- 2. variáveis

def criar_variaveis(df: pd.DataFrame, alvo: str) -> pd.DataFrame:
    """Defasagens do ITU (o efeito do calor costuma aparecer 1 a 3 dias depois) e sazonalidade."""
    out = df.copy()
    for k in DEFASAGENS_ITU:
        out[f"itu_medio_lag{k}"] = out["itu_medio"].shift(k)
    out["itu_medio_mm3"] = out["itu_medio"].rolling(3, min_periods=3).mean()       # média dos últimos 3 dias
    out["horas_itu72_lag1"] = out["horas_itu_acima_72"].shift(1)
    out["itu_acima72_lag1"] = (out["itu_medio"].shift(1) - 72).clip(lower=0)        # só o excesso acima de 72
    out[f"{alvo}_lag1"] = out[alvo].shift(1)                                         # produção de ontem
    out["mes"] = out.index.month
    return out


# ----------------------------------------------------------------------------- 3. qualidade

def qualidade(df: pd.DataFrame, alvo: str) -> tuple[pd.DataFrame, list[str]]:
    colunas = [c for c in COLUNAS_NUMERICAS if c in df.columns]
    tabela = pd.DataFrame({
        "dias com dado": df[colunas].notna().sum(),
        "% faltando": (df[colunas].isna().mean() * 100).round(1),
    })
    avisos = []
    n_alvo = int(df[alvo].notna().sum())
    if n_alvo < 180:
        avisos.append(f"Só {n_alvo} dias com {alvo}. Para modelos confiáveis, o ideal é ter pelo menos 6 a 12 meses.")
    n_itu = int(df["itu_medio"].notna().sum())
    if n_itu < n_alvo * 0.8:
        avisos.append(f"Clima em só {n_itu} dias: importe o histórico da estação para cobrir o período da produção.")
    if "estimado" in df and df["estimado"].fillna(0).sum() > 0:
        pct = df["estimado"].fillna(0).sum() / max(1, df["litros_total"].notna().sum()) * 100
        avisos.append(f"{pct:.0f}% dos dias com produção são estimados (coleta de vários dias): "
                      "a variação diária deles é artificial; teste o modelo com e sem esses dias.")
    meses = df.loc[df[alvo].notna()].index.month.nunique()
    if meses < 12:
        avisos.append(f"Os dados cobrem {meses} mês(es) do ano: o efeito das estações pode ficar confundido com o ITU.")
    return tabela, avisos


# ----------------------------------------------------------------------------- 4. correlações

def correlacoes(df: pd.DataFrame, alvo: str) -> pd.DataFrame:
    candidatas = ["del_medio", "vacas_em_lactacao", "itu_medio_mm3", "itu_acima72_lag1", "horas_itu72_lag1",
                  "itu_max", "iep_medio_12m", "vacas_vazias_acima_x", "perdas_gestacionais_30d", "tem_ocorrencia"]
    candidatas += [f"itu_medio_lag{k}" for k in DEFASAGENS_ITU]
    linhas = []
    for c in candidatas:
        if c not in df:
            continue
        par = df[[alvo, c]].dropna()
        if len(par) < 10 or par[c].nunique() < 2:
            continue
        linhas.append({
            "variável": c,
            "n": len(par),
            "Pearson": round(par[alvo].corr(par[c], method="pearson"), 3),
            "Spearman": round(par[alvo].corr(par[c], method="spearman"), 3),
        })
    if not linhas:
        return pd.DataFrame(columns=["variável", "n", "Pearson", "Spearman"])
    return pd.DataFrame(linhas).sort_values("Spearman", key=lambda s: s.abs(), ascending=False)


# ----------------------------------------------------------------------------- 5. modelos de base

def metricas(real: pd.Series, previsto: pd.Series) -> dict:
    erro = real - previsto
    return {
        "MAE": round(float(erro.abs().mean()), 3),
        "RMSE": round(float(np.sqrt((erro ** 2).mean())), 3),
        "MAPE (%)": round(float((erro.abs() / real.abs()).mean() * 100), 2),
    }


def modelos(df: pd.DataFrame, alvo: str, frac_teste: float):
    """
    Divisão TEMPORAL (sem embaralhar): os primeiros dias treinam, os últimos testam.
    Embaralhar vazaria o futuro para o treino e daria um erro otimista demais.

    - Persistência: amanhã = hoje. Qualquer modelo útil precisa ganhar dele.
    - Regressão linear (OLS) explicativa: sem a produção de ontem, para medir o efeito de DEL e ITU.
      Erros-padrão HAC (Newey-West), porque os resíduos de dias seguidos são correlacionados.
    - Random Forest preditiva: com a produção de ontem e as mesmas variáveis.
    """
    import statsmodels.api as sm
    from sklearn.ensemble import RandomForestRegressor

    explicativas = ["del_medio", "itu_medio_lag1", "itu_acima72_lag1", "tem_ocorrencia"]
    preditivas = explicativas + [f"{alvo}_lag1", "vacas_em_lactacao", "itu_medio_mm3"]
    usadas = sorted(set(preditivas) | {alvo})
    base = df[usadas].dropna()
    if len(base) < 40:
        return None, None, None, f"Só {len(base)} dias completos (com produção, DEL e ITU): poucos para treinar modelos."

    corte = int(len(base) * (1 - frac_teste))
    treino, teste = base.iloc[:corte], base.iloc[corte:]

    previsoes = pd.DataFrame({"real": teste[alvo]})
    previsoes["Persistência (ontem)"] = teste[f"{alvo}_lag1"]

    X_tr = sm.add_constant(treino[explicativas], has_constant="add")
    ols = sm.OLS(treino[alvo], X_tr).fit(cov_type="HAC", cov_kwds={"maxlags": 7})
    previsoes["Regressão linear (OLS)"] = ols.predict(sm.add_constant(teste[explicativas], has_constant="add"))

    rf = RandomForestRegressor(n_estimators=300, min_samples_leaf=5, random_state=42, n_jobs=-1)
    rf.fit(treino[preditivas], treino[alvo])
    previsoes["Random Forest"] = rf.predict(teste[preditivas])

    resultado = pd.DataFrame({nome: metricas(previsoes["real"], previsoes[nome])
                              for nome in previsoes.columns if nome != "real"}).T
    resultado.insert(0, "dias de teste", len(teste))
    importancia = pd.Series(rf.feature_importances_, index=preditivas).sort_values(ascending=False).round(3)
    info = (f"Treino: {treino.index.min():%d/%m/%Y} a {treino.index.max():%d/%m/%Y} ({len(treino)} dias). "
            f"Teste: {teste.index.min():%d/%m/%Y} a {teste.index.max():%d/%m/%Y} ({len(teste)} dias).")
    return (resultado, ols, importancia, info), previsoes, (treino, teste), None


# ----------------------------------------------------------------------------- 6. gráficos

def _estilo(ax):
    ax.grid(axis="y", color="#E1E3D8", linewidth=0.8)
    ax.spines[["top", "right"]].set_visible(False)
    ax.tick_params(colors="#5B6355", labelsize=9)


def graficos(df: pd.DataFrame, alvo: str, corr_defasagem: pd.DataFrame, previsoes: pd.DataFrame | None, saida: Path):
    # 1) séries em painéis separados (sem dois eixos Y no mesmo gráfico)
    fig, eixos = plt.subplots(3, 1, figsize=(11, 8), sharex=True)
    eixos[0].plot(df.index, df[alvo], color=AZUL, linewidth=1.5)
    eixos[0].set_title("Litros por vaca em lactação", loc="left", fontsize=11)
    eixos[1].plot(df.index, df["vacas_em_lactacao"], color=AZUL, linewidth=1.5)
    eixos[1].set_title("Vacas em lactação", loc="left", fontsize=11)
    eixos[2].plot(df.index, df["itu_medio"], color=AZUL, linewidth=1.5, label="ITU médio")
    if "itu_max" in df:
        eixos[2].plot(df.index, df["itu_max"], color=LARANJA, linewidth=1.5, label="ITU máximo")
    eixos[2].axhline(72, color=CINZA, linestyle="--", linewidth=1)
    eixos[2].text(df.index.min(), 72, " 72 · início do estresse", fontsize=8, color="#5B6355", va="bottom",
                  bbox={"facecolor": "white", "edgecolor": "none", "pad": 1})
    eixos[2].set_title("ITU", loc="left", fontsize=11)
    eixos[2].legend(frameon=False, fontsize=9)
    for ax in eixos:
        _estilo(ax)
    fig.tight_layout()
    fig.savefig(saida / "01_series.png", dpi=150)
    plt.close(fig)

    # 2) dispersão ITU de ontem × produção de hoje
    par = df[["itu_medio_lag1", alvo]].dropna()
    if len(par) >= 10:
        fig, ax = plt.subplots(figsize=(7, 5))
        ax.scatter(par["itu_medio_lag1"], par[alvo], s=14, color=AZUL, alpha=0.6, edgecolors="none")
        ax.axvline(72, color=CINZA, linestyle="--", linewidth=1)
        ax.set_xlabel("ITU médio do dia anterior")
        ax.set_ylabel("Litros por vaca em lactação")
        ax.set_title(f"ITU de ontem × produção de hoje (n = {len(par)} dias)", loc="left", fontsize=11)
        _estilo(ax)
        fig.tight_layout()
        fig.savefig(saida / "02_itu_x_litros.png", dpi=150)
        plt.close(fig)

    # 3) correlação por defasagem
    if not corr_defasagem.empty:
        fig, ax = plt.subplots(figsize=(7, 4))
        ax.bar(corr_defasagem["k"], corr_defasagem["Spearman"], color=AZUL, width=0.6)
        ax.axhline(0, color="#5B6355", linewidth=0.8)
        for _, l in corr_defasagem.iterrows():
            ax.text(l["k"], l["Spearman"], f"{l['Spearman']:.2f}", ha="center",
                    va="bottom" if l["Spearman"] >= 0 else "top", fontsize=8, color="#22291D")
        ax.set_xlabel("Defasagem do ITU médio (dias)")
        ax.set_ylabel("Correlação de Spearman")
        ax.set_title("Quanto o ITU de k dias atrás acompanha a produção de hoje", loc="left", fontsize=11)
        _estilo(ax)
        fig.tight_layout()
        fig.savefig(saida / "03_correlacao_defasagem.png", dpi=150)
        plt.close(fig)

    # 4) teste: real × previsto
    if previsoes is not None:
        fig, ax = plt.subplots(figsize=(11, 4.5))
        ax.plot(previsoes.index, previsoes["real"], color="#22291D", linewidth=1.8, label="Real")
        ax.plot(previsoes.index, previsoes["Regressão linear (OLS)"], color=AZUL, linewidth=1.5, label="Regressão linear (OLS)")
        ax.plot(previsoes.index, previsoes["Random Forest"], color=LARANJA, linewidth=1.5, label="Random Forest")
        ax.set_title("Período de teste: litros por vaca, real × previsto", loc="left", fontsize=11)
        ax.legend(frameon=False, fontsize=9)
        _estilo(ax)
        fig.tight_layout()
        fig.savefig(saida / "04_previsto_x_real.png", dpi=150)
        plt.close(fig)


# ----------------------------------------------------------------------------- 7. relatório

def tabela_md(df: pd.DataFrame, indice: bool = True) -> str:
    """Tabela em Markdown sem depender do pacote tabulate."""
    d = df.reset_index().rename(columns={"index": " "}) if indice else df
    cab = "| " + " | ".join(map(str, d.columns)) + " |"
    sep = "|" + "|".join("---" for _ in d.columns) + "|"
    linhas = ["| " + " | ".join("" if pd.isna(v) else str(v) for v in row) + " |" for row in d.itertuples(index=False)]
    return "\n".join([cab, sep, *linhas])


def main():
    p = argparse.ArgumentParser(description="Análise exploratória e modelos de base do TCC (Bovix).")
    p.add_argument("arquivo", type=Path, help="CSV exportado pela aba Indicadores do Bovix")
    p.add_argument("--saida", type=Path, default=Path("resultados"), help="pasta dos resultados (padrão: resultados)")
    p.add_argument("--teste", type=float, default=0.25, help="fração final dos dias usada como teste (padrão 0,25)")
    p.add_argument("--sem-estimados", action="store_true", help="descarta os dias com produção estimada")
    args = p.parse_args()
    alvo = ALVO_PADRAO
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # acentos certos no terminal do Windows

    args.saida.mkdir(parents=True, exist_ok=True)
    df = carregar(args.arquivo)
    if args.sem_estimados and "estimado" in df:
        df.loc[df["estimado"] == 1, [alvo, "litros_total", "estimado"]] = np.nan
    df = criar_variaveis(df, alvo)

    tab_qualidade, avisos = qualidade(df, alvo)
    corr = correlacoes(df, alvo)
    corr_def = (corr[corr["variável"].str.startswith("itu_medio_lag")]
                .assign(k=lambda t: t["variável"].str.replace("itu_medio_lag", "").astype(int))
                .sort_values("k"))
    resultado_modelos, previsoes, _, erro_modelos = modelos(df, alvo, args.teste)
    graficos(df, alvo, corr_def, previsoes, args.saida)

    md = [f"# Análise do rebanho — {df.index.min():%d/%m/%Y} a {df.index.max():%d/%m/%Y}", ""]
    md += ["## 1. Qualidade dos dados", "", tabela_md(tab_qualidade), ""]
    md += [f"- ⚠️ {a}" for a in avisos] or ["- Nenhum alerta de qualidade."]
    md += ["", "## 2. Correlações com litros por vaca em lactação", "",
           "Spearman mede relação monotônica (não só linear). n = dias com os dois valores. "
           "Correlação não é causa: DEL, estação do ano e ITU andam juntos.", "",
           tabela_md(corr, indice=False) if not corr.empty else "Dados insuficientes.", ""]
    md += ["## 3. Modelos de base (divisão temporal)", ""]
    if erro_modelos:
        md += [erro_modelos, ""]
    else:
        resultado, ols, importancia, info = resultado_modelos
        md += [info, "", tabela_md(resultado), "",
               "Um modelo só é útil se tiver erro menor que a **persistência** (repetir o valor de ontem).", "",
               "### Regressão linear (OLS) — coeficientes (erros HAC/Newey-West)", "",
               "Leitura: quanto muda a produção por vaca quando a variável sobe 1 unidade, mantendo as outras fixas.", "",
               tabela_md(pd.DataFrame({"coeficiente": ols.params.round(4), "erro-padrão": ols.bse.round(4),
                                       "p-valor": ols.pvalues.round(4)})),
               "", f"R² (treino): {ols.rsquared:.3f} · n = {int(ols.nobs)}", "",
               "### Random Forest — importância das variáveis", "", tabela_md(importancia.to_frame("importância")), ""]
    md += ["## 4. Gráficos", "", "![Séries](01_series.png)", "", "![ITU × litros](02_itu_x_litros.png)", "",
           "![Correlação por defasagem](03_correlacao_defasagem.png)", ""]
    if previsoes is not None:
        md += ["![Real × previsto](04_previsto_x_real.png)", ""]
    (args.saida / "resumo.md").write_text("\n".join(md), encoding="utf-8")
    print(f"Pronto. Resultados em: {args.saida.resolve()}")
    for a in avisos:
        print("Aviso:", a)


if __name__ == "__main__":
    main()
