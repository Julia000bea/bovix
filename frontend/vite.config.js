import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),
    // App instalável (PWA): o service worker guarda as telas do sistema no aparelho, para o
    // Bovix abrir mesmo sem internet. Os DADOS da fazenda não passam por aqui — ficam no
    // IndexedDB (src/services/offline.js), que também guarda os lançamentos feitos offline.
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.png", "apple-touch-icon.png"],
      manifest: {
        name: "Bovix – Gestão do rebanho leiteiro",
        short_name: "Bovix",
        description: "Reprodução, saúde, ordenha e financeiro do rebanho leiteiro — funciona também sem internet.",
        lang: "pt-BR",
        start_url: "/",
        scope: "/",
        display: "standalone",
        orientation: "portrait",
        background_color: "#F5F0E6",
        theme_color: "#1F3A2E",
        icons: [
          { src: "/pwa-192.png", sizes: "192x192", type: "image/png" },
          { src: "/pwa-512.png", sizes: "512x512", type: "image/png" },
          { src: "/pwa-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
        ],
      },
      workbox: {
        globPatterns: ["**/*.{js,css,html,png,svg,ico,woff2}"],
        navigateFallback: "/index.html",
        cleanupOutdatedCaches: true,
        runtimeCaching: [
          {
            // fontes do Google (Inter, Zilla Slab, IBM Plex Mono)
            urlPattern: ({ url }) => url.origin === "https://fonts.googleapis.com" || url.origin === "https://fonts.gstatic.com",
            handler: "CacheFirst",
            options: {
              cacheName: "bovix-fontes",
              expiration: { maxEntries: 30, maxAgeSeconds: 60 * 60 * 24 * 365 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            // fotos dos animais (/api/fotos/<chave>): a chave muda quando a foto muda
            urlPattern: ({ url }) => url.pathname.startsWith("/api/fotos/"),
            handler: "CacheFirst",
            options: {
              cacheName: "bovix-fotos",
              expiration: { maxEntries: 400, maxAgeSeconds: 60 * 60 * 24 * 90 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
    }),
  ],
  server: { port: 5173 },
});
