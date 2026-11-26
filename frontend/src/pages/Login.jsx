import { useState } from "react";
import { Loader2, Tag, Heart, Syringe, Droplet, Wallet } from "lucide-react";
import { C, fDisplay, fBody } from "../utils/tokens";
import BrandLogo from "../components/BrandLogo";
import { authService } from "../services/authService";

// O link do e-mail de "esqueci minha senha" chega como https://.../#redefinir=<token>.
// O token fica no "#" da URL, que o navegador não envia para nenhum servidor.
function tokenDaUrl() {
  const hash = window.location.hash || "";
  return hash.startsWith("#redefinir=") ? decodeURIComponent(hash.slice("#redefinir=".length)) : null;
}

function limparTokenDaUrl() {
  window.history.replaceState(null, "", window.location.pathname + window.location.search);
}

const RECURSOS = [
  { icon: Tag, t: "Rebanho", d: "Cada animal com brinco, lote e histórico." },
  { icon: Heart, t: "Reprodução", d: "Inseminação, prenhez e previsão de parto." },
  { icon: Syringe, t: "Saúde", d: "Vacinas, tratamentos e carência do leite." },
  { icon: Droplet, t: "Ordenha", d: "Litros por vaca e por ordenha, todo dia." },
  { icon: Wallet, t: "Financeiro", d: "Despesas por categoria e custos do mês." },
];

function Rotulo({ texto, children }) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span style={{ color: C.ink, fontWeight: 500 }}>{texto}</span>
      {children}
    </label>
  );
}

function Campo(props) {
  return (
    <input
      {...props}
      className="w-full h-12 rounded-xl border px-4 text-[15px] outline-none bg-white transition focus:ring-2"
      style={{ borderColor: C.line, color: C.ink, "--tw-ring-color": C.pasto900 + "33" }}
    />
  );
}

export default function Login({ auth }) {
  const [tokenRedefinicao] = useState(tokenDaUrl);
  const [modo, setModo] = useState(tokenRedefinicao ? "redefinir" : "login"); // login | registro | esqueci | redefinir
  const [enviando, setEnviando] = useState(false);
  const [form, setForm] = useState({ nome: "", email: "", senha: "", confirmarSenha: "", fazendaNome: "", cidade: "", estado: "" });
  const [aviso, setAviso] = useState("");
  const [erroLocal, setErroLocal] = useState("");

  const trocarModo = (novo) => {
    setModo(novo);
    setAviso("");
    setErroLocal("");
  };

  const set = (campo) => (e) => setForm({ ...form, [campo]: e.target.value });

  const enviar = async (e) => {
    e.preventDefault();
    setEnviando(true);
    setAviso("");
    setErroLocal("");
    try {
      if (modo === "login") {
        await auth.login(form.email, form.senha);
      } else if (modo === "registro") {
        await auth.registrar(form);
      } else if (modo === "esqueci") {
        const resp = await authService.esqueciSenha(form.email);
        setAviso(resp?.mensagem || "Se houver uma conta com este e-mail, enviamos um link para criar uma nova senha.");
      } else if (modo === "redefinir") {
        if (form.senha !== form.confirmarSenha) {
          setErroLocal("As duas senhas não são iguais.");
          return;
        }
        const resp = await authService.redefinirSenha(tokenRedefinicao, form.senha);
        limparTokenDaUrl();
        setForm({ ...form, senha: "", confirmarSenha: "" });
        setModo("login");
        setAviso(resp?.mensagem || "Senha alterada. Entre com a nova senha.");
      }
    } catch (err) {
      setErroLocal(err.message || "Não foi possível concluir. Tente de novo.");
    } finally {
      setEnviando(false);
    }
  };

  const textoBotao = {
    login: "Entrar",
    registro: "Criar conta e começar",
    esqueci: "Enviar link por e-mail",
    redefinir: "Salvar nova senha",
  }[modo];

  const titulo = {
    login: "Entrar no Bovix",
    registro: "Criar conta no Bovix",
    esqueci: "Esqueci minha senha",
    redefinir: "Criar nova senha",
  }[modo];

  const mostraAbas = modo === "login" || modo === "registro";
  const erro = erroLocal || (mostraAbas ? auth.erro : "");

  return (
    <div
      className="min-h-screen w-full bg-cover bg-center"
      style={{
        ...fBody,
        // A foto fica em public/login-fundo.jpg. Sem ela, aparece só o degradê verde.
        backgroundImage: `linear-gradient(90deg, rgba(20,38,30,0.82) 0%, rgba(20,38,30,0.55) 55%, rgba(20,38,30,0.25) 100%), url(/login-fundo.jpg), linear-gradient(160deg, ${C.pasto700}, ${C.pasto900})`,
        backgroundColor: C.pasto900,
      }}
    >
      <div className="max-w-6xl mx-auto min-h-screen px-4 sm:px-8 py-8 lg:py-14 flex flex-col lg:flex-row lg:items-center gap-8 lg:gap-16">
        {/* apresentação */}
        <section className="flex-1 text-white">
          <div className="hidden sm:block"><BrandLogo light size={112} /></div>
          <div className="sm:hidden"><BrandLogo light size={72} /></div>
          <h1 style={{ ...fDisplay, fontWeight: 700 }} className="mt-8 text-3xl sm:text-4xl lg:text-[48px] leading-[1.1] max-w-lg">
            O controle da fazenda leiteira, do brinco ao balanço.
          </h1>
          <p className="hidden sm:block mt-5 text-base lg:text-lg leading-relaxed max-w-xl" style={{ color: "rgba(255,255,255,0.88)" }}>
            O Bovix reúne rebanho, reprodução, saúde, ordenha e financeiro. Você abre o Quadro do dia de manhã e já sabe o
            que precisa de atenção hoje.
          </p>
          <ul className="hidden md:grid grid-cols-2 gap-x-8 gap-y-5 mt-9 max-w-xl">
            {RECURSOS.map(({ icon: Icone, t, d }) => (
              <li key={t} className="flex gap-3">
                <Icone size={20} color={C.brinco} className="shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold">{t}</p>
                  <p className="text-sm leading-snug" style={{ color: "rgba(255,255,255,0.8)" }}>{d}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>

        {/* formulário */}
        <section className="w-full lg:w-[420px] shrink-0">
          <div className="rounded-3xl bg-white p-6 sm:p-8 shadow-2xl">
            <h2 style={{ ...fDisplay, color: C.pasto900, fontWeight: 700 }} className="text-2xl mb-5">{titulo}</h2>

            {mostraAbas ? (
              <div className="flex p-1 rounded-xl mb-6" style={{ backgroundColor: "#EEF1E8" }}>
                {[{ id: "login", l: "Entrar" }, { id: "registro", l: "Criar conta" }].map((m) => (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => trocarModo(m.id)}
                    className="flex-1 py-2.5 rounded-lg text-sm font-semibold transition"
                    style={{ backgroundColor: modo === m.id ? C.pasto900 : "transparent", color: modo === m.id ? "#fff" : C.inkSoft }}
                  >
                    {m.l}
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-sm -mt-2 mb-5" style={{ color: C.inkSoft }}>
                {modo === "esqueci"
                  ? "Informe o e-mail da sua conta. Vamos enviar um link para você criar uma nova senha."
                  : "Escolha uma nova senha com pelo menos 8 caracteres."}
              </p>
            )}

            <form onSubmit={enviar} className="flex flex-col gap-4">
              {modo === "registro" && (
                <Rotulo texto="Seu nome">
                  <Campo required value={form.nome} onChange={set("nome")} autoComplete="name" />
                </Rotulo>
              )}
              {modo !== "redefinir" && (
                <Rotulo texto="E-mail">
                  <Campo required type="email" placeholder="voce@fazenda.com.br" value={form.email} onChange={set("email")} autoComplete="email" />
                </Rotulo>
              )}
              {modo !== "esqueci" && (
                <Rotulo texto={modo === "redefinir" ? "Nova senha" : "Senha"}>
                  <Campo
                    required
                    type="password"
                    minLength={modo === "login" ? undefined : 8}
                    maxLength={72}
                    value={form.senha}
                    onChange={set("senha")}
                    autoComplete={modo === "login" ? "current-password" : "new-password"}
                  />
                </Rotulo>
              )}
              {modo === "redefinir" && (
                <Rotulo texto="Repita a nova senha">
                  <Campo required type="password" minLength={8} maxLength={72} value={form.confirmarSenha} onChange={set("confirmarSenha")} autoComplete="new-password" />
                </Rotulo>
              )}

              {modo === "registro" && (
                <>
                  <Rotulo texto="Nome da fazenda">
                    <Campo required value={form.fazendaNome} onChange={set("fazendaNome")} />
                  </Rotulo>
                  <div className="grid grid-cols-[1fr_88px] gap-3">
                    <Rotulo texto="Cidade">
                      <Campo value={form.cidade} onChange={set("cidade")} />
                    </Rotulo>
                    <Rotulo texto="UF">
                      <Campo maxLength={2} value={form.estado} onChange={(e) => setForm({ ...form, estado: e.target.value.toUpperCase() })} />
                    </Rotulo>
                  </div>
                </>
              )}

              {aviso && <p className="text-sm" style={{ color: C.pasto900 }}>{aviso}</p>}
              {erro && <p className="text-sm" style={{ color: C.cio }}>{erro}</p>}

              {!(modo === "esqueci" && aviso) && (
                <button
                  type="submit"
                  disabled={enviando}
                  className="mt-2 h-12 rounded-xl text-base font-semibold flex items-center justify-center gap-2 transition hover:brightness-110 disabled:opacity-70"
                  style={{ backgroundColor: C.cobre, color: "#fff" }}
                >
                  {enviando && <Loader2 size={16} className="animate-spin" />}
                  {textoBotao}
                </button>
              )}

              {modo === "login" && (
                <button type="button" onClick={() => trocarModo("esqueci")} className="text-sm underline self-center" style={{ color: C.pasto900 }}>
                  Esqueci minha senha
                </button>
              )}
              {(modo === "esqueci" || modo === "redefinir") && (
                <button
                  type="button"
                  onClick={() => {
                    if (modo === "redefinir") limparTokenDaUrl();
                    trocarModo("login");
                  }}
                  className="text-sm underline self-center"
                  style={{ color: C.pasto900 }}
                >
                  Voltar para o login
                </button>
              )}
            </form>
          </div>
        </section>
      </div>
    </div>
  );
}
