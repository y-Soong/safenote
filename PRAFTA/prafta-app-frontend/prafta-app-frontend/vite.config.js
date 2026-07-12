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
          proxy.on("proxyReq", (proxyReq, req) => {
            // 파일 서명 URL(FileUrlSigner)이 '폰이 접속한 https 출처'로 생성되도록
            // 원래 접속 호스트/프로토콜을 백엔드에 전달한다.
            // → 이미지가 페이지(https://<host>:8082)와 same-origin 이 되어 앱 웹뷰 mixed-content
            //   차단을 피하고 인라인 렌더된다. IP 를 박지 않으므로 DHCP 로 PC IP 가 바뀌어도 무수정.
            //   (백엔드 file.public-base-url 은 비어 있어야 이 헤더가 사용된다.)
            proxyReq.setHeader("X-Forwarded-Proto", "https");
            if (req.headers.host) {
              proxyReq.setHeader("X-Forwarded-Host", req.headers.host);
            }
          });
          proxy.on("proxyRes", (proxyRes, req) => {
            // IP 접근 시 CORS: 요청 Origin으로 응답 헤더 덮어쓰기
            const origin = req.headers.origin;
            if (origin && proxyRes.headers["access-control-allow-origin"]) {
              proxyRes.headers["access-control-allow-origin"] = origin;
            }
          });
        },
      },
      "/uploads": {
        // 업로드 파일 서빙(이미지/서명)을 dev 서버 같은 https 출처로 받아 백엔드(/uploads/** 루트 매핑)로 전달.
        // 파일 URL 이 https://<host>:8082/uploads/... = 페이지와 same-origin 이 되어 앱 웹뷰에서
        // 인라인 이미지가 정상 렌더된다(http://...:8080 직결 시 발생하던 mixed-content 회피).
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
