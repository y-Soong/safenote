// stores/loadingStore.js
import { defineStore } from "pinia";

export const useLoadingStore = defineStore("loading", {
  // 동시에 여러 요청이 떠 있을 수 있으므로 boolean 대신 참조 카운터로 관리한다.
  //   (boolean 이면 먼저 끝난 응답이 loading 을 꺼버려, 아직 진행 중인 요청이 있는데도
  //    로딩 오버레이가 조기 제거되어 저장 연타 등에 화면이 노출되는 창이 생긴다.)
  state: () => ({
    pending: 0,
  }),
  getters: {
    // 진행 중 요청이 1건이라도 있으면 로딩 상태. 외부 사용처(App.vue)는 loading 만 본다.
    loading: (state) => state.pending > 0,
  },
  actions: {
    startLoading() {
      this.pending++;
    },
    stopLoading() {
      // 음수 방지(중복 stop 호출 방어).
      if (this.pending > 0) this.pending--;
    },
  },
});
