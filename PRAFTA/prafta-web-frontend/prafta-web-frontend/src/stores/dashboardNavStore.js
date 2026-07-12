// stores/dashboardNavStore.js
// 대시보드(Dashboard_01) → 타 화면 탭 열기 + 조회조건 1회성 주입 채널 (PRAFTA-DASHBOARD-T1).
//   - openTabRequest: MainLayout 이 watch 하여 기존 메뉴 트리(findMenuByRoute) 경유로 탭을 연다.
//     메뉴 트리에 없는 라우트(=권한 없음)는 MainLayout 에서 이동 거부된다.
//   - pendingParams: 수신 화면이 onMounted/onActivated 에서 consumeParams 로 1회 소비한다.
//     (keep-alive 로 이미 열린 탭에도 재주입 가능. 새로고침 시에는 초기화되어 stale 조건이 남지 않는다.)
import { defineStore } from "pinia";

export const useDashboardNavStore = defineStore("dashboardNav", {
  state: () => ({
    // MainLayout 이 watch 하는 탭 열기 요청. { routeName: 'Attd_05', ts: Date.now() }
    openTabRequest: null,
    // 화면별 1회성 주입 파라미터. { [routeName]: {...} }
    pendingParams: {},
  }),
  actions: {
    // 대시보드 위젯 이동 버튼이 호출.
    //   params 계약: 근태 탭 { siteCd, siteNo, siteNm, nodeCd, nodeNm, incSubNodeYn, ym }
    //               안전 탭 { siteCd, siteNo, siteNm, ym }  (ym = 'YYYY-MM')
    requestOpen(routeName, params) {
      if (params) this.pendingParams[routeName] = params;
      // ts 로 동일 화면 재요청도 watch 가 트리거되게 한다.
      this.openTabRequest = { routeName, ts: Date.now() };
    },
    // 수신 화면이 onMounted/onActivated 에서 호출 — 반환 후 즉시 삭제(consume-once).
    consumeParams(routeName) {
      const p = this.pendingParams[routeName] ?? null;
      if (p) delete this.pendingParams[routeName];
      return p;
    },
  },
});
