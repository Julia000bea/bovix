import { C, fDisplay } from "../utils/tokens";

export default function BrandLogo({ light = false, compact = false }) {
  const color = light ? C.pasto50 : C.pasto900;
  const subtitleColor = light ? "#B7C2B0" : C.inkSoft;

  return (
    <div className="flex items-center gap-2.5" aria-label="Bovix">
      <svg width={compact ? 34 : 46} height={compact ? 34 : 46} viewBox="0 0 64 64" fill="none" aria-hidden="true">
        <path d="M20 17C13 17 10 12 10 7c6 1 10 4 13 9M44 17c7 0 10-5 10-10-6 1-10 4-13 9" stroke={color} strokeWidth="5" strokeLinecap="round" />
        <path d="M18 24c-5-4-10-2-14 2 5 4 10 5 15 1M46 24c5-4 10-2 14 2-5 4-10 5-15 1" stroke={color} strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M20 19c3-6 8-8 12-8s9 2 12 8v17c0 12-5 21-12 23-7-2-12-11-12-23V19Z" fill={color} />
        <path d="M26 33c1-2 3-3 6-3s5 1 6 3M25 42c4 4 10 4 14 0M32 19v8" stroke={light ? C.pasto900 : "#F5F7F0"} strokeWidth="3" strokeLinecap="round" />
        <circle cx="27" cy="27" r="1.8" fill={light ? C.pasto900 : "#F5F7F0"} />
        <circle cx="37" cy="27" r="1.8" fill={light ? C.pasto900 : "#F5F7F0"} />
      </svg>
      <div>
        <h1 style={{ ...fDisplay, color, fontWeight: 700 }} className={compact ? "text-xl leading-tight" : "text-2xl leading-tight tracking-wide"}>Bovix</h1>
        {!compact && <p style={{ color: subtitleColor }} className="text-xs tracking-[0.16em] uppercase">Gestão inteligente do rebanho</p>}
      </div>
    </div>
  );
}
