<template>
  <div>
    <LoadingSpinner v-if="loadingStore.loading" />
    <router-view />

    <AlertModal
      :visible="alertVisible"
      :message="alertMessage"
      @confirm="onAlertConfirm"
      @close="onAlertConfirm"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useUserStore } from "@/stores/userStore";
import { jwtDecode } from "jwt-decode";
import { useLoadingStore } from "@/stores/loadingStore";
import { registerAlertHandler } from "@/utils/alertUtil";
import router from "@/router";
import api from "@/api/axios";
import {
  refreshAccessToken,
  forceLogout,
  getRefreshToken,
} from "@/composables/useAuth";

import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import AlertModal from "@/components/modal/AlertModal.vue";
import "@/assets/fonts/Pretendard/pretendard.css";

const loadingStore = useLoadingStore();
const userStore = useUserStore();

// AlertModal 연동용 상태
const alertMessage = ref("");
const alertVisible = ref(false);
let alertResolve = null;

registerAlertHandler((message, resolve) => {
  alertMessage.value = message;
  alertVisible.value = true;
  alertResolve = resolve;
});

const onAlertConfirm = () => {
  alertVisible.value = false;
  if (alertResolve) alertResolve();
};

/**
 * JWT 토큰에서 사용자 식별·인가 정보를 추출하여 store에 반영.
 * 정책 §11.1에 따라 JWT 클레임에는 PII(휴대폰/이메일)가 포함되지 않는다 — 검사·매핑에서도 제외.
 */
function setUserFromToken(token) {
  try {
    const decoded = jwtDecode(token);

    if (
      decoded.gv_cmpnyCd &&
      decoded.gv_userCd &&
      decoded.gv_userNm &&
      decoded.gv_siteCd &&
      decoded.gv_siteNo &&
      decoded.gv_siteNm &&
      decoded.gv_nodeCd &&
      decoded.gv_nodeNm &&
      decoded.gv_authCd &&
      decoded.gv_authLevel
    ) {
      userStore.setUser({
        cmpnyCd: decoded.gv_cmpnyCd,
        userCd: decoded.gv_userCd,
        userId: decoded.gv_userId,
        userNm: decoded.gv_userNm,
        siteCd: decoded.gv_siteCd,
        siteNo: decoded.gv_siteNo,
        siteNm: decoded.gv_siteNm,
        nodeCd: decoded.gv_nodeCd,
        nodeNm: decoded.gv_nodeNm,
        authCd: decoded.gv_authCd,
        authLevel: decoded.gv_authLevel,
      });
      return true;
    }
  } catch (e) {
    console.error("Invalid JWT", e);
  }
  return false;
}

// 1) 모듈 로드 시점: sessionStorage에 token이 있으면 store를 즉시 복구한다.
const token = sessionStorage.getItem("token");
if (token) {
  const ok = setUserFromToken(token);
  if (!ok) {
    // fallback: sessionStorage 값 기반 (PII는 store에 두지 않는다)
    const gv_cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");
    const gv_userCd = sessionStorage.getItem("gv_userCd");
    const gv_userId = sessionStorage.getItem("gv_userId");
    const gv_userNm = sessionStorage.getItem("gv_userNm");
    const gv_siteCd = sessionStorage.getItem("gv_siteCd");
    const gv_siteNo = sessionStorage.getItem("gv_siteNo");
    const gv_siteNm = sessionStorage.getItem("gv_siteNm");
    const gv_nodeCd = sessionStorage.getItem("gv_nodeCd");
    const gv_nodeNm = sessionStorage.getItem("gv_nodeNm");
    const gv_authCd = sessionStorage.getItem("gv_authCd");
    const gv_authLevel = sessionStorage.getItem("gv_authLevel");

    // 권한 식별자(authCd/authLevel)도 부재 시 부분 채움을 막아 권한 분기 오작동을 방지한다.
    if (gv_cmpnyCd && gv_userCd && gv_userNm && gv_authCd && gv_authLevel) {
      userStore.setUser({
        cmpnyCd: gv_cmpnyCd,
        userCd: gv_userCd,
        userId: gv_userId,
        userNm: gv_userNm,
        siteCd: gv_siteCd,
        siteNo: gv_siteNo,
        siteNm: gv_siteNm,
        nodeCd: gv_nodeCd,
        nodeNm: gv_nodeNm,
        authCd: gv_authCd,
        authLevel: gv_authLevel,
      });
    } else {
      sessionStorage.clear();
    }
  }
}

/**
 * 새 탭/새로고침 대비: token 없고 refreshToken만 있으면 1회 복구 시도.
 * 성공 시 새 토큰을 sessionStorage에 저장하고, JWT 클레임으로 sessionStorage의 비-PII 키를 동기화한다.
 * 정책 §11.1에 따라 휴대폰/이메일은 JWT/세션스토리지에 저장하지 않는다.
 */
async function syncTokenIfNeeded() {
  const tokenNow = sessionStorage.getItem("token");
  const rt = getRefreshToken();

  if (tokenNow || !rt) return;

  try {
    const newToken = await refreshAccessToken();
    if (!newToken) return;

    // JWT에서 사용자 정보 추출하여 sessionStorage 동기화 (비-PII 키만)
    const decoded = jwtDecode(newToken);

    if (decoded.gv_cmpnyCd)
      sessionStorage.setItem("gv_cmpnyCd", decoded.gv_cmpnyCd);
    if (decoded.gv_userCd)
      sessionStorage.setItem("gv_userCd", decoded.gv_userCd);
    if (decoded.gv_userId)
      // PRAFTA-010: 이전 버그(gv_userCd 키에 gv_userId 값을 덮어쓰던 코드)를 올바른 매핑으로 수정.
      sessionStorage.setItem("gv_userId", decoded.gv_userId);
    if (decoded.gv_userNm)
      sessionStorage.setItem("gv_userNm", decoded.gv_userNm);
    if (decoded.gv_siteCd)
      sessionStorage.setItem("gv_siteCd", decoded.gv_siteCd);
    if (decoded.gv_siteNo)
      sessionStorage.setItem("gv_siteNo", decoded.gv_siteNo);
    if (decoded.gv_siteNm)
      sessionStorage.setItem("gv_siteNm", decoded.gv_siteNm);
    if (decoded.gv_nodeCd)
      sessionStorage.setItem("gv_nodeCd", decoded.gv_nodeCd);
    if (decoded.gv_nodeNm)
      sessionStorage.setItem("gv_nodeNm", decoded.gv_nodeNm);
    if (decoded.gv_authCd)
      sessionStorage.setItem("gv_authCd", decoded.gv_authCd);
    if (decoded.gv_authLevel)
      sessionStorage.setItem("gv_authLevel", decoded.gv_authLevel);

    // PRAFTA-011: 이전에 단독 localStorage.setItem("gv_cmpnyCd", ...) 라인이 있었으나,
    // sessionStorage 정책으로 통일되어 localStorage 저장은 제거되었다.

    // userStore에도 동기화
    setUserFromToken(newToken);

    // api 기본 헤더도 갱신
    api.defaults.headers.common.Authorization = `Bearer ${newToken}`;

    console.log("[TAB-SYNC] Token refreshed and synced to sessionStorage");
  } catch (e) {
    console.error("[TAB-SYNC] Refresh failed:", e);
    // refresh 실패 → 정리 후 로그인 페이지로 (현재 로그인 화면이 아니라면)
    await forceLogout();
    userStore.logout();
    if (router.currentRoute.value.path !== "/") {
      router.push("/");
    }
  }
}

// 탭 활성화 시 동기화 (다른 탭에서 로그인/로그아웃한 경우 대비)
let visibilitySyncing = false;
const handleVisibilityChange = async () => {
  if (document.visibilityState !== "visible" || visibilitySyncing) return;
  const tokenNow = sessionStorage.getItem("token");
  const rt = getRefreshToken();
  if (!tokenNow && rt) {
    visibilitySyncing = true;
    try {
      await syncTokenIfNeeded();
    } finally {
      visibilitySyncing = false;
    }
  }
};

onMounted(async () => {
  // 초기 로드 시 refresh 시도 (새 탭 케이스 등)
  await syncTokenIfNeeded();

  // 탭 활성화 이벤트 리스너 등록
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onBeforeUnmount(() => {
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
