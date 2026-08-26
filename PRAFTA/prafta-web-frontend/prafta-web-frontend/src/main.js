import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router/index.js";
import commonUtil from "@/utils/common";
import alertPlugin from "@/plugins/alert"; // ⬅️ 플러그인 import
import "@/assets/css/tailwind.css";
import "@/assets/css/common.css";

const app = createApp(App);
const pinia = createPinia();

// 💡 모든 함수들을 this.$util.함수명 으로 접근 가능하게 설정
app.config.globalProperties.$util = commonUtil;
window.__appGlobalProperties = app.config.globalProperties; // 💡 전역 복사 저장

app.use(pinia);
app.use(router);
app.use(alertPlugin); // ⬅️ 전역 alert 등록
app.mount("#app");

// 관리자가 탭을 오래 켜놓은 채로 재배포가 일어날 수 있다(S3 sync --delete + 청크 1년 캐시).
// 재배포 시 이전 빌드의 청크 파일(*.js)이 삭제되므로, 아직 방문하지 않은 화면으로 이동할 때
// 그 화면의 지연 로딩(import()) 청크가 404 로 실패한다. 이미 방문한 화면은 메모리에 남아있어
// 정상 동작하지만, 처음 방문하는 화면은 아무 반응 없이 네비게이션이 조용히 중단된다
// (router.onError 도 없어 사용자에게 안내조차 안 됨). vite:preloadError 를 잡아 페이지를
// 새로고침시키면 최신 청크 목록을 다시 받아와 문제를 해소한다(Vite 공식 권장 패턴).
window.addEventListener("vite:preloadError", () => {
  window.location.reload();
});
