# Bovix – Gestão Inteligente do Rebanho

Sistema para produtores de leite acompanharem o rebanho: reprodução (cio, inseminação,
parto), saúde e medicação (com carência do leite), ordenha, financeiro e um painel do dia
com alertas e insights automáticos de decisão. Autenticação por conta (login/registro) com JWT.

## Arquitetura

```
Frontend (React + Vite)  →  API Java (Spring Boot + JWT)  →  PostgreSQL
```

```
bovix/
├── backend/     API Java (Spring Boot + JPA + Flyway + Spring Security/JWT)
│   └── src/main/java/com/bovix/{model,repository,service,controller,security,config,dto}
├── frontend/    React (Vite)
│   └── src/{pages,components,services,hooks,utils,models,types}
└── docker-compose.yml   sobe só o PostgreSQL
```

## Banco de dados

8 tabelas, criadas automaticamente pelo Flyway na primeira execução do backend:
`usuarios`, `fazendas`, `animais`, `eventos_reproducao`, `eventos_saude`,
`ordenhas`, `financeiro`, `configuracoes`.

## Autenticação

- Login/registro em `/api/auth/registrar` e `/api/auth/login`, retornam um JWT.
- O registro já cria a fazenda do produtor (nome, cidade, UF) junto com a conta.
- Todas as rotas de dados exigem `Authorization: Bearer <token>`.
- Cada endpoint valida que a fazenda/animal pertence ao usuário autenticado
  (`FazendaService.verificarPropriedade` / `AnimalService.verificarPropriedade`) — ninguém
  acessa dados de outra conta trocando o id na URL.
- O frontend guarda o token no `localStorage` e o injeta automaticamente em toda chamada (`services/api.js`).

## Rodando no Windows + VSCode (o seu caso)

Este projeto roda em Windows normalmente, mas o README original tinha comandos de
bash (Mac/Linux) que não funcionam direto no PowerShell. Use esta seção.

### 0. Abra o projeto certo no VSCode
Abra o arquivo `bovix.code-workspace` (não a pasta `bovix` direto) — ele já abre
`backend` e `frontend` como duas pastas separadas lado a lado, do jeito que o
VSCode espera para projetos Java + Node no mesmo repositório, e já sugere as
extensões certas (Java Extension Pack, Spring Boot, ESLint, Tailwind).

### 1. Banco de dados — duas opções

**Opção A: Docker** (porta 5433, ver docker-compose.yml)
```powershell
docker compose up -d
```

**Opção B: PostgreSQL nativo já instalado no Windows** (sem Docker) — evita conflito de porta
se você já tem um Postgres instalado direto na máquina (ex: via instalador com pgAdmin).
Abra o pgAdmin, conecte no servidor com o usuário `postgres`, abra a Query Tool e rode
**cada linha separadamente** (CREATE DATABASE não pode rodar junto de outro comando):
```sql
CREATE USER bovix WITH PASSWORD 'bovix';
```
```sql
CREATE DATABASE bovix OWNER bovix;
```
```sql
GRANT ALL PRIVILEGES ON DATABASE bovix TO bovix;
```
Depois, antes de rodar o backend, aponte para essa instância (troque a porta se a sua não for 5432):
```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/bovix"
$env:SPRING_DATASOURCE_USERNAME = "bovix"
$env:SPRING_DATASOURCE_PASSWORD = "bovix"
```
> Em qualquer uma das duas opções, as 8 tabelas do sistema são criadas sozinhas pelo Flyway
> na primeira vez que o backend sobe — você só precisa criar o usuário e o banco *vazio*.

### 2. Suba o backend
Você **não precisa ter o Maven instalado** — incluí o Maven Wrapper (`mvnw.cmd`), ele baixa
o Maven sozinho na primeira execução.
```powershell
cd backend
$env:BOVIX_JWT_SECRET = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 64 | % {[char]$_})
.\mvnw.cmd spring-boot:run
```
> Se você usou a **Opção B** (Postgres nativo) no passo 1, defina também as 3 variáveis
> `SPRING_DATASOURCE_*` mostradas ali **na mesma janela do PowerShell**, antes do `.\mvnw.cmd`.
> Atenção: essas variáveis só valem para essa janela. Se abrir um terminal novo, defina de novo
> (ou configure como variável de ambiente permanente do Windows em "Editar variáveis de ambiente do sistema").

API em `http://localhost:8080/api`.

### 3. Suba o frontend (em outro terminal, deixe o backend rodando)
```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```
Abre em `http://localhost:5173`.

---

## Rodando no Mac/Linux

### 1. Banco de dados
```bash
docker compose up -d
```

### 2. Backend (Java 17 + Maven)
```bash
cd backend
export BOVIX_JWT_SECRET=$(openssl rand -base64 64)
./mvnw spring-boot:run
```
API em `http://localhost:8080/api`.

### 3. Frontend (Node 18+)
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```
Abre em `http://localhost:5173` — crie sua conta na tela de login (isso já cria sua fazenda).

## Relatório diário por e-mail

Cada fazenda pode ligar, no ícone de engrenagem do sistema, o envio automático do "Quadro do
dia" por e-mail, todo dia, no horário escolhido. Um job roda a cada 10 minutos no backend e
confere se é a hora de alguma fazenda — sem depender de ninguém estar com o navegador aberto.

### Configurar o SMTP (necessário para o e-mail sair de verdade)
Defina estas variáveis de ambiente antes de subir o backend (senão o envio falha silenciosamente
e fica só registrado no log):
```powershell
$env:BOVIX_SMTP_HOST = "smtp.gmail.com"
$env:BOVIX_SMTP_PORT = "587"
$env:BOVIX_SMTP_USER = "seuemail@gmail.com"
$env:BOVIX_SMTP_PASS = "sua-senha-de-app-de-16-letras"
```
> **Gmail**: não funciona com a senha normal da conta. Você precisa gerar uma "senha de app"
> em https://myaccount.google.com/apppasswords (exige verificação em duas etapas ativada).
> Para testar sem mexer em conta de e-mail real, dá pra usar o [Mailtrap](https://mailtrap.io)
> (grátis, sandbox) — só trocar host/porta/usuário/senha pelos dele.

### Testando
Na tela de Configurações do Bovix, preencha o e-mail de destino e clique em "Enviar teste
agora" — isso manda o quadro do dia na hora, sem esperar o horário configurado.

### WhatsApp (não incluído)
Diferente do e-mail, WhatsApp automático exige uma conta paga em um provedor (ex: Twilio,
Meta Business API) com número verificado e modelo de mensagem aprovado — não dá pra ativar
só com código, precisa de conta própria nesse provedor. A arquitetura já separa "calcular o
quadro do dia" (`PainelDoDiaService`) de "enviar" (`EmailService`), então dá pra adicionar um
`WhatsAppService` do mesmo jeito no futuro, se você abrir uma conta em algum desses provedores.

## Foto do animal

O navegador reduz a foto antes de enviar (lado maior de 1280 px, JPEG ~150–400 KB) e a API guarda
no próprio PostgreSQL (tabela `animal_fotos`). A foto é servida em `/api/fotos/<chave>`, onde a chave
é aleatória e muda a cada novo envio.

Por que no banco e não no disco: no Render (e na maioria das hospedagens de container) o disco é
apagado a cada deploy, reinício ou quando o serviço "dorme". Com fotos pequenas, 100 animais ocupam
cerca de 30 MB — folgado no 1 GB do plano grátis do Neon. Fotos enviadas antes desta versão
(em `/uploads/animais/...`) continuam sendo servidas enquanto existirem no disco.

## Esqueci minha senha

Na tela de login, "Esqueci minha senha" envia um link de uso único que vale por 1 hora
(no máximo 3 pedidos por e-mail por hora). Ao trocar a senha, os logins abertos em outros
aparelhos deixam de valer. Precisa de:
- SMTP configurado (mesmas variáveis do relatório diário);
- `BOVIX_FRONTEND_URL` com o endereço público do frontend (ex.: `https://bovix.vercel.app`),
  que é usado para montar o link do e-mail.

---

## Deploy gratuito (Render + Neon + Vercel)

Caminho recomendado pra começar sem custo: banco no **Neon**, backend no **Render** (ambos
grátis), frontend na **Vercel** (grátis). Única ressalva: no plano grátis do Render, o backend
"dorme" depois de 15 minutos sem uso — o primeiro acesso do dia demora uns 30-60s pra responder.

### 0. Suba o código pro GitHub
Os três serviços fazem deploy automático a partir de um repositório Git — se o projeto ainda
não está no GitHub, crie um repositório novo e suba o conteúdo desta pasta `bovix/`.

### 1. Banco (Neon, via integração da Vercel)
O Neon mudou a forma de criar conta nova: hoje passa pela Vercel.
1. Crie conta em vercel.com (login com GitHub).
2. No Vercel Marketplace, procure "Neon" → Install → escolha "Create New Neon Account".
3. Isso cria um projeto Postgres grátis. Abra "Open in Neon" (ou a aba Storage do seu projeto
   Vercel) para ver os detalhes de conexão: host, usuário, senha, nome do banco.

### 2. Backend (Render, com o Dockerfile incluído)
1. Crie conta em render.com (login com GitHub).
2. "New" → "Web Service" → conecte o repositório do GitHub → aponte o "Root Directory" para `backend`.
3. O Render detecta o `Dockerfile` sozinho (incluído no projeto) — não precisa configurar build command.
4. Em "Environment", adicione as variáveis:
   - `BOVIX_JWT_SECRET` (**obrigatória**: valor aleatório com pelo menos 32 bytes — sem ela a API não sobe)
   - `BOVIX_FUSO_HORARIO` (opcional, padrão `America/Sao_Paulo` — o servidor do Render roda em UTC)
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<host-do-neon>/<banco>?sslmode=require`
     (sem usuário/senha na URL e sem `channel_binding` — esse parâmetro é da libpq, não do driver JDBC)
   - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (do Neon)
   - Alternativa: colar a string do Neon como veio (`postgresql://usuario:senha@host/banco?...`) em
     `DATABASE_URL` (ou em `SPRING_DATASOURCE_URL`) — a API converte sozinha para o formato JDBC.
   - `PORT` não precisa ser criada: o Render define e a API usa automaticamente.
   - `BOVIX_SMTP_HOST` / `_PORT` / `_USER` / `_PASS` (relatório diário e "esqueci minha senha")
   - `BOVIX_FRONTEND_URL` = URL do frontend na Vercel (usada no link de redefinição de senha)
   - `BOVIX_CORS_ALLOWED_ORIGINS` = a mesma URL do frontend
   - Em "Health Check Path", coloque `/api/health` (não consulta o banco)
5. Plano: escolha "Free". Deploy.
6. Quando terminar, o Render te dá uma URL tipo `https://bovix-api.onrender.com` — é essa URL
   + `/api` que vai entrar no `VITE_API_URL` do frontend.

### 3. Frontend (Vercel)
1. No mesmo painel da Vercel, "Add New" → "Project" → conecte o repositório → "Root Directory" = `frontend`.
2. Framework preset: Vite (detecta sozinho).
3. Em "Environment Variables", adicione `VITE_API_URL` = `https://bovix-api.onrender.com/api` (a URL do passo anterior).
4. Deploy. A Vercel te dá uma URL tipo `https://bovix.vercel.app`.

### 4. Feche o ciclo do CORS
Volte no Render → variáveis de ambiente do backend → adicione/edite (via `application.yml`,
propriedade `bovix.cors.allowed-origins`, ou variável `BOVIX_CORS_ALLOWED_ORIGINS` se preferir
parametrizar) apontando para a URL final da Vercel. Sem isso, o navegador bloqueia as chamadas
do frontend pro backend.

## Passo a passo para colocar em produção 100% funcional

O código está pronto, mas nenhuma IA consegue "publicar" isso por você — são decisões e contas
que só você pode tomar/criar. Aqui está o roteiro, na ordem:

### 1. Segredos e variáveis de ambiente
- Gere um `BOVIX_JWT_SECRET` forte e único: `openssl rand -base64 64`. Não há valor padrão: a API se recusa a subir sem ele (ou com menos de 32 bytes).
- Defina senha forte do banco (não use `bovix/bovix` do exemplo).
- Guarde esses segredos num gerenciador (Doppler, AWS Secrets Manager, Railway/Render env vars, etc.), nunca no código.

### 2. Banco de dados gerenciado
- Suba um PostgreSQL gerenciado (Neon, Supabase, RDS, Railway, Render Postgres — qualquer um serve).
- Aponte `spring.datasource.url/username/password` do backend (via env vars) para ele.
- O Flyway roda a migration sozinho no primeiro boot — não precisa criar tabelas manualmente.
- Configure backup automático (a maioria dos provedores gerenciados já oferece isso).

### 3. Deploy do backend (API Java)
- Buildar: `./mvnw clean package` (Mac/Linux) ou `.\mvnw.cmd clean package` (Windows) gera um `.jar` executável em `backend/target`.
- Hospedar em algo simples primeiro: Railway, Render ou Fly.io (suportam Docker/Java direto).
  Para AWS/GCP "de verdade", container em ECS/Cloud Run é o caminho, mas exige mais configuração.
- Definir variáveis de ambiente no host: `BOVIX_JWT_SECRET`, dados do datasource, `bovix.cors.allowed-origins` (URL do seu frontend em produção).
- Configurar HTTPS (a maioria dos PaaS já entrega isso automaticamente).

### 4. Armazenamento das fotos
- Resolvido: as fotos ficam no PostgreSQL (ver "Foto do animal"), então não dependem do disco do servidor.
- Se um dia o volume crescer muito (milhares de animais), dá para mover para um bucket (Cloudflare R2, S3)
  trocando só o `AnimalFotoRepository`.

### 5. Deploy do frontend
- Buildar: `npm run build` gera a pasta `frontend/dist` (arquivos estáticos).
- Hospedar em Vercel, Netlify ou Cloudflare Pages — são gratuitos para esse porte e fazem deploy automático a cada push no Git.
- Definir `VITE_API_URL` apontando para a URL pública do backend (a de produção, com HTTPS).

### 6. Domínio e CORS
- Comprar/apontar um domínio (ex: `app.suafazenda.com.br` para o frontend, `api.suafazenda.com.br` para o backend).
- Atualizar `bovix.cors.allowed-origins` no backend para a URL final do frontend — senão o navegador bloqueia as chamadas.

### 7. Checklist de segurança antes de abrir para usuários reais
- [ ] `BOVIX_JWT_SECRET` trocado e fora do código-fonte.
- [ ] Senha do banco forte, banco não exposto publicamente sem necessidade.
- [ ] HTTPS ativo em backend e frontend.
- [ ] CORS restrito só ao domínio do seu frontend (não `*`).
- [x] Limite de tamanho de upload de foto (5 MB na requisição, 3 MB por foto).
- [x] Limite de tentativas de login por e-mail (5 erros em 15 min) e limite de e-mails de teste/redefinição.
- [ ] Limite por IP no registro de contas — se aparecer abuso, coloque o Cloudflare na frente da API.
- [ ] Backup do banco testado (não só configurado — teste restaurar uma vez).

### 8. Antes de vender/lançar de verdade
- [x] Recuperação de senha ("esqueci minha senha").
- Verificação de e-mail no cadastro.
- Monitoramento de erros (Sentry) e logs centralizados.
- Testes automatizados mínimos nos fluxos críticos (login, criar animal, registrar parto).

Nenhum desses passos precisa ser feito hoje para você testar o sistema — só são obrigatórios
antes de colocar dados reais de produção e usuários de verdade nele.

---

## Rodando numa fazenda de verdade (recomendado)

O deploy 100% grátis acima serve para testar. Para uma fazenda depender do sistema todo dia:

| Peça | Recomendação | Custo |
|---|---|---|
| Frontend | Vercel (plano grátis) | R$ 0 |
| API | Render **Starter** (não dorme) | US$ 7/mês |
| Banco | Neon Free (1 GB, desliga sozinho quando ninguém usa) | R$ 0 |
| E-mail | Brevo SMTP grátis (300 e-mails/dia) no lugar do Gmail | R$ 0 |
| Monitoramento | cron-job.org ou UptimeRobot chamando `/api/health` | R$ 0 |

Por que pagar a API: no plano grátis ela dorme após 15 min sem uso, o primeiro acesso demora
até 1 minuto e o Render pode reiniciá-la a qualquer momento. O Starter roda direto.

**Se precisar ficar no grátis por enquanto:** configure no cron-job.org uma chamada a
`https://<sua-api>.onrender.com/api/health` a cada 10 minutos. Isso mantém a API acordada
(cabe nas 750 horas grátis do mês para **um** serviço) sem manter o banco ligado, porque
`/api/health` não consulta o banco.

**E-mail:** crie conta no Brevo, valide um remetente (ex.: o e-mail da fazenda) e use em
`BOVIX_SMTP_HOST=smtp-relay.brevo.com`, `BOVIX_SMTP_PORT=587` e o usuário/senha SMTP que o
Brevo mostra. Assim o relatório diário e o "esqueci minha senha" não dependem da sua conta pessoal do Gmail.

**Backup:** o Neon guarda um histórico curto. Faça também um `pg_dump` semanal guardado fora do
Neon (ex.: GitHub Actions agendado salvando o arquivo num Google Drive) e teste restaurar uma vez.

**No campo:** o sistema precisa de internet. Antes de levar para a fazenda, teste no celular,
no curral, com o sinal que existe lá.
