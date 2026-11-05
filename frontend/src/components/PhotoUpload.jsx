import { useRef, useState } from "react";
import { Camera, Loader2 } from "lucide-react";
import { C } from "../utils/tokens";
import { animaisService } from "../services/animaisService";

// mesmos formatos aceitos pelo backend (AnimalService.salvarFoto)
const TIPOS_ACEITOS = ["image/jpeg", "image/png", "image/webp"];
const API_ORIGIN =(import.meta.env.VITE_API_URL || "http://localhost:8080/api").replace(/\/api\/?$/, "");

/**
 * Foto de perfil do animal. Se o animal já existe (animalId), envia o arquivo
 * direto para a API (multipart/form-data) e chama onUploaded com o animal atualizado.
 * Se ainda não existe (cadastro novo), guarda o arquivo localmente e devolve via onSelectFile,
 * para ser enviado logo após o animal ser criado (precisa do id).
 */
export default function PhotoUpload({ animalId, fotoUrl, onUploaded, onSelectFile, size = 72 }) {
  const inputRef = useRef(null);
  const [preview, setPreview] = useState(fotoUrl || null);
  const [enviando, setEnviando] = useState(false);

  const escolherArquivo = () => inputRef.current?.click();

  const aoSelecionar = async (e) => {
    const arquivo = e.target.files?.[0];
    e.target.value = ""; // permite escolher o mesmo arquivo de novo após um erro
    if (!arquivo) return;
    if (!TIPOS_ACEITOS.includes(arquivo.type)) {
      window.alert("Envie uma imagem JPG, PNG ou WEBP.");
      return;
    }

    setPreview(URL.createObjectURL(arquivo));

    if (!animalId) {
      onSelectFile?.(arquivo);
      return;
    }

    setEnviando(true);
    try {
      const animalAtualizado = await animaisService.enviarFoto(animalId, arquivo);
      onUploaded?.(animalAtualizado);
    } catch (err) {
      console.error("Falha ao enviar foto:", err);
      setPreview(fotoUrl || null); // não mostrar como salva uma foto que não foi enviada
      window.alert(`Não foi possível enviar a foto: ${err.message}`);
    } finally {
      setEnviando(false);
    }
  };

  const src = preview?.startsWith("blob:") ? preview : preview ? `${API_ORIGIN}${preview}` : null;

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={escolherArquivo}
        className="relative rounded-full overflow-hidden border flex items-center justify-center shrink-0"
        style={{ width: size, height: size, borderColor: C.line, backgroundColor: C.pasto50 }}
        title="Adicionar foto do animal"
      >
        {src ? (
          <img src={src} alt="Foto do animal" className="w-full h-full object-cover" />
        ) : (
          <Camera size={20} color={C.inkSoft} />
        )}
        {enviando && (
          <span className="absolute inset-0 bg-black/40 flex items-center justify-center">
            <Loader2 size={18} color="#fff" className="animate-spin" />
          </span>
        )}
      </button>
      <input ref={inputRef} type="file" accept={TIPOS_ACEITOS.join(",")} className="hidden" onChange={aoSelecionar} />
    </div>
  );
}
