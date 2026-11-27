# Modo offline (27/11/2026)

O Bovix agora funciona sem internet — pensado para o curral e a sala de ordenha, onde o sinal falha.

## Como funciona
- **App instalável (PWA)**: no celular, abra o site e escolha "Adicionar à tela inicial" / "Instalar app".
  As telas ficam guardadas no aparelho e o sistema abre mesmo sem internet.
- **Dados guardados no aparelho**: cada consulta à API guarda uma cópia (IndexedDB). Sem internet,
  a tela mostra essa cópia e avisa de quando ela é.
- **Lançamentos offline**: ordenha, reprodução, saúde (por animal e para o rebanho) e financeiro
  podem ser lançados sem internet. Ficam numa fila no aparelho, aparecem na tela como
  "aguardando envio" e são enviados sozinhos quando o sinal volta (também ao reabrir o app e a cada 30 s).
  Botão "Enviar agora" na faixa de aviso.
- **Recusados pela API** (ex.: data no futuro): ficam listados na faixa de aviso para tentar de novo ou descartar.
- **Precisa de internet**: login, cadastrar/editar animal, foto, excluir registros já enviados,
  configurações, importar CSV, Ocorrências e Indicadores (estes mostram a última cópia, se houver).

## Cuidados
- É preciso abrir o Bovix **uma vez com internet** em cada aparelho para ele guardar os dados.
- Sair da conta apaga a cópia dos dados do aparelho, mas **não** a fila: ela é enviada quando a
  mesma conta entrar de novo.
- O login vale 7 dias. Se expirar com lançamentos na fila, a faixa pede para entrar de novo — nada se perde.
- Se o sinal cair exatamente durante o envio, um lançamento pode chegar duas vezes. Confira a lista
  depois de sincronizar se o sinal estava muito instável.

## Arquivos
- `frontend/src/services/offline.js` — IndexedDB (cópias e fila) e estado do modo offline.
- `frontend/src/services/api.js` — cópia automática dos GETs, fila nos lançamentos, sincronização.
- `frontend/src/services/provisorio.js` — registros mostrados até o envio.
- `frontend/src/components/OfflineBanner.jsx` — faixa de aviso (sem internet, pendentes, recusados).
- `frontend/vite.config.js` — PWA (`vite-plugin-pwa`): manifest, ícones e service worker.

---

# Revisão de produção — mudanças (22/11/2026)

## Antes de subir
1. Rode os testes na sua máquina: `cd backend` e `.\mvnw.cmd test` (Windows) ou `./mvnw test`.
   Esta versão não foi compilada no ambiente da revisão (sem acesso ao repositório do Maven).
2. Novas variáveis no Render: `BOVIX_FRONTEND_URL` (URL da Vercel) e, de preferência, SMTP do Brevo.
3. O Flyway aplica sozinho as migrações V7, V8 e V9 no primeiro boot.

## O que mudou
- **E-mail diário**: se a API estava dormindo no horário, envia assim que acordar (até 12 h de atraso).
  O job só consulta o banco quando chega a hora do próximo envio, para o Neon poder desligar.
- **GET /api/health**: público, sem consultar o banco (health check do Render / cron-job.org).
- **Fotos no banco** (tabela `animal_fotos`), servidas em `/api/fotos/<chave>`; o navegador reduz a
  foto antes de enviar. Não somem mais a cada deploy/reinício.
- **Spring Boot 3.3.2 → 3.5.16** (última versão da linha 3.x). A linha 3.x também saiu do suporte
  gratuito em junho/2026: o próximo passo é migrar para a 4.x, com calma e testando.
- **Esqueci minha senha**: link de uso único por e-mail (1 h); trocar a senha derruba logins antigos.
- **Carência**: novo campo "Dias de tratamento"; a carência conta da última aplicação.
- **Quadro do dia**: se a API falhar, mostra o erro em vez de "tudo em dia".
- **Insights**: produção compara as duas últimas semanas completas (sem hoje, média por dia lançado);
  custos comparam o mesmo trecho do mês anterior.
- **"Enviar teste"**: no máximo 5 e-mails por hora por fazenda.
- **Pool de conexões** sem conexões ociosas presas (Neon pode desligar quando ninguém usa).

## Ainda não feito (próximos passos)
- Alertas de reprodução: ciclo de cio perdido e "vaca vazia há X dias após o parto".
- Receita (venda de leite) no financeiro, para calcular margem e custo por litro.
- Verificação de e-mail no cadastro e limite de cadastro por IP.
- Migração para Spring Boot 4.x.
