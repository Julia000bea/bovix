// Reduz a foto no próprio navegador antes de enviar: uma foto de celular (3–8 MB) vira um JPEG
// de ~150–400 KB. Economiza o plano de dados no campo, deixa o envio rápido e cabe folgado no banco.

const LADO_MAXIMO = 1280;
const QUALIDADE = 0.82;

export async function reduzirImagem(arquivo) {
  try {
    // imageOrientation: "from-image" respeita a rotação da foto tirada com o celular em pé
    const bitmap = await createImageBitmap(arquivo, { imageOrientation: "from-image" });
    const escala = Math.min(1, LADO_MAXIMO / Math.max(bitmap.width, bitmap.height));
    const largura = Math.round(bitmap.width * escala);
    const altura = Math.round(bitmap.height * escala);

    const canvas = document.createElement("canvas");
    canvas.width = largura;
    canvas.height = altura;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#fff"; // PNG com transparência não fica preto no JPEG
    ctx.fillRect(0, 0, largura, altura);
    ctx.drawImage(bitmap, 0, 0, largura, altura);
    bitmap.close?.();

    const blob = await new Promise((resolve) => canvas.toBlob(resolve, "image/jpeg", QUALIDADE));
    if (!blob || blob.size >= arquivo.size) return arquivo; // já era pequena: envia a original
    return new File([blob], "foto.jpg", { type: "image/jpeg" });
  } catch {
    return arquivo; // navegador sem suporte: envia a original (o servidor aceita até 3 MB)
  }
}
