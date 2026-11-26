import marca from "../assets/bovix-marca.png";
import { C, fDisplay } from "../utils/tokens";

// Duas versões, como na folha da marca:
// - light: para fundos escuros — vaca dentro de um círculo creme, nome em branco
// - padrão: para fundos claros — vaca solta, nome em verde
// size: tamanho do símbolo em px (o nome acompanha a proporção)
export default function BrandLogo({ light = false, compact = false, size, subtitle = "Gestão do rebanho leiteiro" }) {
  const s = size ?? (compact ? 40 : 56);
  const nomeCor = light ? "#FFFFFF" : C.pasto900;
  const subCor = light ? "rgba(255,255,255,0.78)" : C.inkSoft;
  const nomePx = Math.max(20, Math.round(s * 0.34));

  return (
    <div className="flex items-center" style={{ gap: Math.round(s * 0.17) }} aria-label="Bovix">
      {light ? (
        <span className="rounded-full flex items-center justify-center shrink-0" style={{ width: s, height: s, backgroundColor: C.creme }}>
          <img src={marca} alt="" style={{ width: s * 0.62, height: s * 0.62, objectFit: "contain" }} />
        </span>
      ) : (
        <img src={marca} alt="" className="shrink-0" style={{ width: s, height: s, objectFit: "contain" }} />
      )}
      <div>
        <p style={{ ...fDisplay, color: nomeCor, fontWeight: 700, fontSize: nomePx, lineHeight: 1.1 }}>Bovix</p>
        {!compact && subtitle && <p style={{ color: subCor, fontSize: Math.max(12, Math.round(s * 0.13)) }} className="mt-0.5">{subtitle}</p>}
      </div>
    </div>
  );
}
