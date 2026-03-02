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
import api, { performServerLogout } from "@/api/axios";
import { resolveBaseURL } from "@/api/baseUrl";
import axios from "axios";

import LoadingSpinner from "@/components/common/LoadingSpinner";
import AlertModal from "@/components/modal/AlertModal";
import "@/assets/fonts/Pretendard/pretendard.css";

const loadingStore = useLoadingStore();
const userStore = useUserStore();

// ✅ AlertModal 연동용 상태
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

function setUserFromToken(token) {
  try {
    const decoded = jwtDecode(token);

    if (
      decoded.gv_cmpnyCd &&
      decoded.gv_userId &&
      decoded.gv_userNm &&
      decoded.gv_authCd &&
      decoded.gv_mblNo &&
      decoded.gv_email
    ) {
      userStore.setUser({
        gv_cmpnyCd: decoded.gv_cmpnyCd,
        gv_userId: decoded.gv_userId,
        gv_userNm: decoded.gv_userNm,
        gv_authCd: decoded.gv_authCd,
        gv_mblNo: decoded.gv_mblNo,
        gv_email: decoded.gv_email,
      });
      return true;
    }
  } catch (e) {
    console.error("Invalid JWT", e);
  }
  return false;
}

// ✅ 1) 기존 사용자 초기화 로직 유지
const token = sessionStorage.getItem("token");
if (token) {
  const ok = setUserFromToken(token);
  if (!ok) {
    // fallback: sessionStorage 값 기반
    const gv_cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");
    const gv_userId = sessionStorage.getItem("gv_userId");
    const gv_userNm = sessionStorage.getItem("gv_userNm");
    const gv_authCd = sessionStorage.getItem("gv_authCd");
    const gv_mblNo = sessionStorage.getItem("gv_mblNo");
    const gv_email = sessionStorage.getItem("gv_email");

    if (gv_userId && gv_userNm) {
      userStore.setUser({
        gv_cmpnyCd,
        gv_userId,
        gv_userNm,
        gv_authCd,
        gv_mblNo,
        gv_email,
      });
    } else {
      sessionStorage.clear();
    }
  }
}

// ✅ refresh 전용 axios 인스턴스 (인터셉터 없음, 무한루프 방지)
const plainAxios = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
});

// ✅ refreshToken으로 새 토큰 받아서 sessionStorage에 저장하는 공통 함수
async function refreshTokenAndSync() {
  // 로그인 페이지에서는 실행하지 않음
  // if (router.currentRoute.value.path === "/") {
  //   return;
  // }

  const tokenNow = sessionStorage.getItem("token");
  const refreshToken = localStorage.getItem("refreshToken");

  // token이 없고 refreshToken이 있을 때만 실행
  if (!tokenNow && refreshToken) {
    try {
      const res = await plainAxios.post("/comApi/auth/refresh", {
        refreshToken,
      });

      const newToken = res.data?.token;
      if (!newToken) {
        throw new Error("NO_TOKEN_IN_REFRESH_RESPONSE");
      }

      // ✅ 새 토큰 저장
      sessionStorage.setItem("token", newToken);

      // ✅ 새 refreshToken이 있으면 저장 (서버가 새 refreshToken을 반환하는 경우)
      if (res.data?.refreshToken) {
        localStorage.setItem("refreshToken", res.data.refreshToken);
      }

      // ✅ JWT에서 사용자 정보 추출하여 sessionStorage에 저장
      const decoded = jwtDecode(newToken);

      console.log("decoded", decoded);
      localStorage.setItem("gv_cmpnyCd", decoded.gv_cmpnyCd);
      if (decoded.gv_cmpnyCd) {
        sessionStorage.setItem("gv_cmpnyCd", decoded.gv_cmpnyCd);
      }
      if (decoded.gv_userId) {
        sessionStorage.setItem("gv_userId", decoded.gv_userId);
      }
      if (decoded.gv_userNm) {
        sessionStorage.setItem("gv_userNm", decoded.gv_userNm);
      }
      if (decoded.gv_authCd) {
        sessionStorage.setItem("gv_authCd", decoded.gv_authCd);
      }
      if (decoded.gv_mblNo) {
        sessionStorage.setItem("gv_mblNo", decoded.gv_mblNo);
      }
      if (decoded.gv_email) {
        sessionStorage.setItem("gv_email", decoded.gv_email);
      }

      // ✅ userStore에도 동기화
      setUserFromToken(newToken);

      // ✅ api 기본 헤더도 갱신
      api.defaults.headers.common.Authorization = `Bearer ${newToken}`;

      console.log("[TAB-SYNC] Token refreshed and synced to sessionStorage");
    } catch (e) {
      console.error("[TAB-SYNC] Refresh failed:", e);
      // refresh 실패 -> 서버에 로그아웃 요청 후 정리
      await performServerLogout(refreshToken);
      sessionStorage.clear();
      localStorage.removeItem("refreshToken");
      userStore.logout();
      // 로그인 페이지로 이동
      if (router.currentRoute.value.path !== "/") {
        router.push("/");
      }
    }
  }
}

// ✅ 3) 탭 활성화 시 동기화 (다른 탭에서 로그인한 경우 대비)
let isRefreshing = false;
const handleVisibilityChange = async () => {
  // 탭이 활성화되고, 현재 refresh 중이 아닐 때만 실행
  if (document.visibilityState === "visible" && !isRefreshing) {
    const tokenNow = sessionStorage.getItem("token");
    const refreshToken = localStorage.getItem("refreshToken");

    // token이 없고 refreshToken이 있을 때만 실행
    if (!tokenNow && refreshToken) {
      isRefreshing = true;
      try {
        await refreshTokenAndSync();
      } finally {
        isRefreshing = false;
      }
    }
  }
};

// ✅ 2) 새 탭/새로고침 대비: token 없으면 refresh로 복구 시도 + 탭 활성화 이벤트 등록
onMounted(async () => {
  // 초기 로드 시 refresh 시도
  await refreshTokenAndSync();

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
