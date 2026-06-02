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
