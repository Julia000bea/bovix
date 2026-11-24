import { useState } from "react";
import { Loader2 } from "lucide-react";
import { C, fDisplay, fBody } from "../utils/tokens";
import { Field, inputCls, inputStyle } from "../components/ui";
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

  const mostraAbas = modo === "login" || modo === "registro";
  const erro = erroLocal || (mostraAbas ? auth.erro : "");

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ backgroundColor: C.pasto50, ...fBody }}>
      <div className="w-full max-w-sm">
        <div className="flex items-center gap-2 justify-center mb-6">
          <BrandLogo />
          <div className="text-center hidden">
            <h1 style={{ ...fDisplay, color: C.pasto900, fontWeight: 700 }} className="text-2xl leading-tight">Bovix</h1>
            <p style={{ color: C.inkSoft }} className="text-xs">Gestão Inteligente do Rebanho</p>
          </div>
        </div>

        <div className="rounded-2xl border bg-white p-5" style={{ borderColor: C.line }}>
          {mostraAbas ? (
            <div className="flex rounded-lg overflow-hidden border mb-4" style={{ borderColor: C.line }}>
              {[{ id: "login", l: "Entrar" }, { id: "registro", l: "Criar conta" }].map((m) => (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => trocarModo(m.id)}
                  className="flex-1 py-2 text-sm font-semibold"
                  style={{ backgroundColor: modo === m.id ? C.pasto900 : "#fff", color: modo === m.id ? "#fff" : C.inkSoft }}
                >
                  {m.l}
                </button>
              ))}
            </div>
          ) : (
            <div className="mb-4">
              <h2 className="text-base font-semibold" style={{ color: C.pasto900 }}>
                {modo === "esqueci" ? "Esqueci minha senha" : "Criar nova senha"}
              </h2>
              <p className="text-xs mt-1" style={{ color: C.inkSoft }}>
                {modo === "esqueci"
                  ? "Informe o e-mail da sua conta. Vamos enviar um link para você criar uma nova senha."
                  : "Escolha uma nova senha com pelo menos 8 caracteres."}
              </p>
            </div>
          )}

          <form onSubmit={enviar} className="flex flex-col gap-3">
            {modo === "registro" && (
              <Field label="Seu nome">
                <input required className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </Field>
            )}
            {modo !== "redefinir" && (
              <Field label="E-mail">
                <input required type="email" className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} autoComplete="email" />
              </Field>
            )}
            {modo !== "esqueci" && (
              <Field label={modo === "redefinir" ? "Nova senha" : "Senha"}>
                <input
                  required
                  type="password"
                  minLength={modo === "login" ? undefined : 8}
                  maxLength={72}
                  className={inputCls}
                  style={{ ...inputStyle, width: "100%" }}
                  value={form.senha}
                  onChange={(e) => setForm({ ...form, senha: e.target.value })}
                  autoComplete={modo === "login" ? "current-password" : "new-password"}
                />
              </Field>
            )}
            {modo === "redefinir" && (
              <Field label="Repita a nova senha">
                <input required type="password" minLength={8} maxLength={72} className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.confirmarSenha} onChange={(e) => setForm({ ...form, confirmarSenha: e.target.value })} autoComplete="new-password" />
              </Field>
            )}

            {modo === "registro" && (
              <>
                <Field label="Nome da fazenda">
                  <input required className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.fazendaNome} onChange={(e) => setForm({ ...form, fazendaNome: e.target.value })} />
                </Field>
                <div className="grid grid-cols-2 gap-2">
                  <Field label="Cidade">
                    <input className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.cidade} onChange={(e) => setForm({ ...form, cidade: e.target.value })} />
                  </Field>
                  <Field label="UF">
                    <input maxLength={2} className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.estado} onChange={(e) => setForm({ ...form, estado: e.target.value.toUpperCase() })} />
                  </Field>
                </div>
              </>
            )}

            {aviso && <p className="text-xs" style={{ color: C.pasto900 }}>{aviso}</p>}
            {erro && <p className="text-xs" style={{ color: C.cio }}>{erro}</p>}

            {!(modo === "esqueci" && aviso) && (
              <button type="submit" disabled={enviando} className="mt-1 py-2.5 rounded-lg text-sm font-semibold flex items-center justify-center gap-2" style={{ backgroundColor: C.brinco, color: "#fff" }}>
                {enviando && <Loader2 size={14} className="animate-spin" />}
                {textoBotao}
              </button>
            )}

            {modo === "login" && (
              <button type="button" onClick={() => trocarModo("esqueci")} className="text-xs underline self-center" style={{ color: C.inkSoft }}>
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
                className="text-xs underline self-center"
                style={{ color: C.inkSoft }}
              >
                Voltar para o login
              </button>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
