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

## Upload de foto do animal

Backend salva em disco (`bovix.uploads-dir`) e expõe em `/uploads/animais/<arquivo>`.
Para produção, troque por um bucket (S3, GCS, etc.) em `AnimalService.salvarFoto`.

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
   - `BOVIX_JWT_SECRET` (gere um valor aleatório forte)
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<host-do-neon>/<banco>?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (do Neon)
   - `BOVIX_SMTP_HOST` / `_PORT` / `_USER` / `_PASS` (se for usar o relatório por e-mail)
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
- Gere um `BOVIX_JWT_SECRET` forte e único: `openssl rand -base64 64`. Nunca reuse o valor padrão do `application.yml`.
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
- Em produção, disco local do servidor é arriscado (some se o container reiniciar/escalar).
- Trocar por um bucket S3 (ou Cloudflare R2/GCS) é a mudança recomendada — altera só o método `salvarFoto` no `AnimalService`, o resto do sistema não muda.

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
- [ ] Limite de tamanho de upload de foto configurado (`spring.servlet.multipart.max-file-size`) para evitar abuso.
- [ ] Rate limiting básico nas rotas de login/registro (evita força bruta) — pode ser no próprio Nginx/Cloudflare na frente da API.
- [ ] Backup do banco testado (não só configurado — teste restaurar uma vez).

### 8. Antes de vender/lançar de verdade
- Recuperação de senha ("esqueci minha senha") — hoje não existe, só login/registro.
- Verificação de e-mail no cadastro.
- Monitoramento de erros (Sentry) e logs centralizados.
- Testes automatizados mínimos nos fluxos críticos (login, criar animal, registrar parto).

Nenhum desses passos precisa ser feito hoje para você testar o sistema — só são obrigatórios
antes de colocar dados reais de produção e usuários de verdade nele.
