import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "tailwindcss";
import autoprefixer from "autoprefixer";

export default defineConfig({
  plugins: [vue()],
  css: {
    postcss: {
      plugins: [
        tailwindcss({ config: "./tailwind.config.cjs" }),
        autoprefixer(),
      ],
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 8081,
    host: true, // 0.0.0.0 - 내 IP로 접근 가능하게
    allowedHosts: ["web.prafta.com", ".prafta.com"],
    proxy: {
      "/prafta": {
        target: "http://localhost:8080",
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on("proxyRes", (proxyRes, req) => {
            // IP 접근 시 CORS: 요청 Origin으로 응답 헤더 덮어쓰기
            const origin = req.headers.origin;
            if (origin && proxyRes.headers["access-control-allow-origin"]) {
              proxyRes.headers["access-control-allow-origin"] = origin;
            }
          });
        },
      },
    },
  },
});
