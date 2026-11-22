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
