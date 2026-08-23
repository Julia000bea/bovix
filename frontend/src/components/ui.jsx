import { C, fBody } from "../utils/tokens";

export function Field({ label, children }) {
  return (
    <label className="flex flex-col gap-1 text-sm" style={fBody}>
      <span style={{ color: C.inkSoft, fontWeight: 500 }}>{label}</span>
      {children}
    </label>
  );
}

export const inputCls = "border rounded-md px-3 py-2 text-sm outline-none focus:ring-2 bg-white";
export const inputStyle = { borderColor: C.line, color: C.ink, fontFamily: "'Inter', sans-serif" };

export function Pill({ children, color }) {
  return (
    <span
      className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold"
      style={{ backgroundColor: color + "22", color }}
    >
      {children}
    </span>
  );
}

export function EmptyState({ text }) {
  return (
    <div className="text-center py-10 rounded-xl border border-dashed text-sm" style={{ borderColor: C.line, color: C.inkSoft, ...fBody }}>
      {text}
    </div>
  );
}
