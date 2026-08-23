import { useState } from "react";
import { Milk, Loader2 } from "lucide-react";
import { C, fDisplay, fBody } from "../utils/tokens";
import { Field, inputCls, inputStyle } from "../components/ui";

export default function Login({ auth }) {
  const [modo, setModo] = useState("login"); // login | registro
  const [enviando, setEnviando] = useState(false);
  const [form, setForm] = useState({ nome: "", email: "", senha: "", fazendaNome: "", cidade: "", estado: "" });

  const enviar = async (e) => {
    e.preventDefault();
    setEnviando(true);
    if (modo === "login") {
      await auth.login(form.email, form.senha);
    } else {
      await auth.registrar(form);
    }
    setEnviando(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ backgroundColor: C.pasto50, ...fBody }}>
      <div className="w-full max-w-sm">
        <div className="flex items-center gap-2 justify-center mb-6">
          <Milk size={28} color={C.pasto900} />
          <div className="text-center">
            <h1 style={{ ...fDisplay, color: C.pasto900, fontWeight: 700 }} className="text-2xl leading-tight">Bovix</h1>
            <p style={{ color: C.inkSoft }} className="text-xs">Gestão Inteligente do Rebanho</p>
          </div>
        </div>

        <div className="rounded-2xl border bg-white p-5" style={{ borderColor: C.line }}>
          <div className="flex rounded-lg overflow-hidden border mb-4" style={{ borderColor: C.line }}>
            {[{ id: "login", l: "Entrar" }, { id: "registro", l: "Criar conta" }].map((m) => (
              <button
                key={m.id}
                onClick={() => setModo(m.id)}
                className="flex-1 py-2 text-sm font-semibold"
                style={{ backgroundColor: modo === m.id ? C.pasto900 : "#fff", color: modo === m.id ? "#fff" : C.inkSoft }}
              >
                {m.l}
              </button>
            ))}
          </div>

          <form onSubmit={enviar} className="flex flex-col gap-3">
            {modo === "registro" && (
              <Field label="Seu nome">
                <input required className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </Field>
            )}
            <Field label="E-mail">
              <input required type="email" className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Field>
            <Field label="Senha">
              <input required type="password" minLength={6} className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} />
            </Field>

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

            {auth.erro && <p className="text-xs" style={{ color: C.cio }}>{auth.erro}</p>}

            <button type="submit" disabled={enviando} className="mt-1 py-2.5 rounded-lg text-sm font-semibold flex items-center justify-center gap-2" style={{ backgroundColor: C.brinco, color: "#fff" }}>
              {enviando && <Loader2 size={14} className="animate-spin" />}
              {modo === "login" ? "Entrar" : "Criar conta e começar"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
