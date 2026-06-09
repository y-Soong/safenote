import { fileURLToPath, URL } from "node:url";
import fs from "node:fs";
import path from "node:path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "tailwindcss";
import autoprefixer from "autoprefixer";

// 모바일(휴대폰)에서 LAN IP로 접속해 카메라/QR(getUserMedia)을 쓰려면
// 보안 컨텍스트(HTTPS)가 필수다. mkcert로 발급한 인증서를 그대로 사용한다.
// cert 폴더의 '<IP>-key.pem' / '<IP>.pem' 쌍을 자동 감지하므로,
// 로컬 IP가 바뀌어 인증서를 재발급해도 이 파일은 수정할 필요가 없다.
const certDir = fileURLToPath(new URL("./cert", import.meta.url));
const keyFile = fs
  .readdirSync(certDir)
  .find((f) => f.endsWith("-key.pem"));
if (!keyFile) {
  throw new Error(
    `[vite] cert 폴더에 '*-key.pem' 인증서가 없습니다: ${certDir}\n` +
      `mkcert 로 '<IP>-key.pem' / '<IP>.pem' 쌍을 먼저 생성해 주세요.`
  );
}
const certFile = keyFile.replace(/-key\.pem$/, ".pem");
const certPath = path.join(certDir, certFile);
if (!fs.existsSync(certPath)) {
  throw new Error(
    `[vite] '${keyFile}' 에 대응하는 인증서 '${certFile}' 가 없습니다: ${certDir}`
  );
}
const https = {
  key: fs.readFileSync(path.join(certDir, keyFile)),
  cert: fs.readFileSync(certPath),
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
    host: true, // 0.0.0.0 - 휴대폰에서 LAN IP로 접근 가능하게
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
