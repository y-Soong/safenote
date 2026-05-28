import { fileURLToPath, URL } from "node:url";
import fs from "node:fs";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "tailwindcss";
import autoprefixer from "autoprefixer";

// 모바일(휴대폰)에서 LAN IP로 접속해 카메라/QR(getUserMedia)을 쓰려면
// 보안 컨텍스트(HTTPS)가 필수다. mkcert로 발급한 인증서를 그대로 사용한다.
const https = {
  key: fs.readFileSync(
    fileURLToPath(new URL("./cert/172.30.1.4-key.pem", import.meta.url))
  ),
  cert: fs.readFileSync(
    fileURLToPath(new URL("./cert/172.30.1.4.pem", import.meta.url))
  ),
};

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
    port: 8082,
    host: true, // 0.0.0.0 - 휴대폰에서 LAN IP(172.30.1.4)로 접근 가능하게
    https,
    proxy: {
      "/prafta": {
        // web 프론트와 동일하게 백엔드를 로컬에서 찾는다. 백엔드가 별도 서버에 있으면 해당 IP로 변경.
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
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
