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
          proxy.on("proxyReq", (proxyReq, req) => {
            // 브라우저가 접속한 원본 host/프로토콜을 백엔드에 전달.
            //   FileUrlSigner(TBM 서명 URL)가 X-Forwarded-* 로 공개 base 를 결정하므로,
            //   Cloudflare 터널(web.prafta.com) 경유 시 서명 URL 이 localhost:8080 이 아니라
            //   현재 도메인 기준(https://web.prafta.com/uploads/...)으로 발급된다.
            if (req.headers.host) {
              proxyReq.setHeader("X-Forwarded-Host", req.headers.host);
            }
            // cloudflared 가 붙여준 X-Forwarded-Proto(https)를 그대로 전달, 없으면 http(로컬 직접 접속).
            proxyReq.setHeader(
              "X-Forwarded-Proto",
              req.headers["x-forwarded-proto"] || "http"
            );
          });
        },
      },
      // 업로드 파일 정적 서빙(/uploads/**)도 API 와 동일하게 백엔드로 프록시.
      //   FE 는 첨부 URL 을 동일 출처 상대경로(/uploads/...)로 조립하므로(utils/fileUrl.js),
      //   localhost:8081 직접 접속과 Cloudflare 터널 도메인 접속 모두에서 이미지/영상/PDF 가 서빙된다.
      "/uploads": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
