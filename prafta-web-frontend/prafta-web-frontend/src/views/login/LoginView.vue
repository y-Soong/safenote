<template>
  <div class="page">
    <!-- 좌/우 분리 -->
    <main class="split">
      <!-- LEFT: 캐릭터 패널 -->
      <aside
        class="art"
        ref="artRef"
        :style="artVars"
        :class="{
          'art--mouth-open': inputFocusState === 'userId',
          'art--eyes-closed': inputFocusState === 'password',
        }"
        aria-hidden="true"
      >
        <div class="art-inner">
          <!-- 보라 -->
          <div class="blob blob--purple">
            <div class="eyes eyes--tiny">
              <span class="eye eye--tiny"
                ><span class="pupil pupil--tiny"></span
              ></span>
              <span class="eye eye--tiny"
                ><span class="pupil pupil--tiny"></span
              ></span>
            </div>
            <span class="mouth" />
          </div>

          <!-- 검정 -->
          <div class="blob blob--black">
            <div class="eyes">
              <span class="eye"><span class="pupil"></span></span>
              <span class="eye"><span class="pupil"></span></span>
            </div>
            <span class="mouth" />
          </div>

          <!-- 주황 -->
          <div class="blob blob--orange">
            <div class="eyes">
              <span class="eye"><span class="pupil"></span></span>
              <span class="eye"><span class="pupil"></span></span>
            </div>
            <span class="mouth" />
          </div>

          <!-- 노랑 -->
          <div class="blob blob--yellow">
            <div class="eyes eyes--tiny eyes--side">
              <span class="eye eye--tiny"
                ><span class="pupil pupil--tiny"></span
              ></span>
              <span class="eye eye--tiny"
                ><span class="pupil pupil--tiny"></span
              ></span>
            </div>
            <span class="mouth" />
          </div>
        </div>

        <div class="art-glow" />
      </aside>

      <!-- RIGHT: 기존 로그인 UI -->
      <section class="right">
        <main class="login-wrapper">
          <section class="login-box" aria-label="로그인 폼">
            <!-- 헤더 -->
            <header class="login-header">
              <h1 class="title">로그인</h1>
            </header>

            <!-- 입력 그룹 -->
            <div class="input-group">
              <input
                id="userId"
                v-model.trim="userId"
                autocomplete="username"
                placeholder="아이디"
                @focus="inputFocusState = 'userId'"
                @blur="
                  (e) => {
                    inputFocusState = 'none';
                    focusKill(e);
                  }
                "
              />
              <input
                ref="passwordInputRef"
                type="password"
                placeholder="비밀번호"
                v-model.trim="password"
                autocomplete="current-password"
                @focus="inputFocusState = 'password'"
                @blur="inputFocusState = 'none'"
              />

              <div class="remember-wrap">
                <input id="rememberId" type="checkbox" v-model="rememberId" />
                <label for="rememberId">아이디 저장</label>
              </div>
            </div>

            <button class="login-btn" @click="fnSubmitLogin">로그인</button>

            <ul class="help-links" aria-label="도움말 링크">
              <li>
                <a class="aTagCls" href="#" @click="fnAcountInfoSrch">
                  아이디/비밀번호 찾기
                </a>
              </li>
            </ul>

            <div class="new-user">
              <p class="new-user-text">SafetyNote가 처음이신가요?</p>
              <button
                type="button"
                class="btn-secondary"
                @click="fnOpenTermsPop"
              >
                가입하기
              </button>
            </div>
          </section>
        </main>

        <footer class="footer">
          <div class="policy">
            <a href="#">개인정보처리방침</a>
            <span class="divider">|</span>
            <span class="center-info"
              >고객센터 1800-1152 (평일 09:00~18:00)</span
            >
          </div>
          <small>© SAFETYNOTE INC. ALL RIGHTS RESERVED.</small>
        </footer>
      </section>
    </main>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  getCurrentInstance,
  onMounted,
  onBeforeUnmount,
  computed,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/userStore";
import axios from "@/api/axios";
import TermsPop from "./popup/TermsPop.vue";
import ActInfoSrch from "@/components/popup/ActInfoSrchPop.vue";

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const router = useRouter();
const userStore = useUserStore();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const userTermsNonAgrList = ref({});
const userId = ref("");
const password = ref("");
const rememberId = ref(false);

/** 'userId' | 'password' | 'none' — ID 포커스 시 입 벌림, 비밀번호 포커스 시 눈 감김 */
const inputFocusState = ref("none");

// ===== LEFT ART (cursor follow) =====
const artRef = ref(null);
const mx = ref(0); // -1 ~ 1
const my = ref(0); // -1 ~ 1
let rafId = 0;

const clamp = (v, min, max) => Math.max(min, Math.min(max, v));

const artVars = computed(() => ({
  "--mx": String(mx.value),
  "--my": String(my.value),
}));

const onMouseMove = (e) => {
  if (!artRef.value) return;

  // rAF로 부하 줄이기
  if (rafId) return;
  rafId = requestAnimationFrame(() => {
    rafId = 0;

    const rect = artRef.value.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;

    const nx = (e.clientX - cx) / (rect.width / 2);
    const ny = (e.clientY - cy) / (rect.height / 2);

    mx.value = clamp(nx, -1, 1);
    my.value = clamp(ny, -1, 1);
  });
};

// ================ Life Cycle Functions ================
onMounted(() => {
  const savedId = localStorage.getItem("savedUserId");
  if (savedId) {
    userId.value = savedId;
    rememberId.value = true;
  }

  window.addEventListener("mousemove", onMouseMove, { passive: true });
});

onBeforeUnmount(() => {
  window.removeEventListener("mousemove", onMouseMove);
  if (rafId) cancelAnimationFrame(rafId);
});

// ================ API Functions ================
const fnSubmitLogin = async () => {
  if (!userId.value || !password.value) {
    await proxy.$alert("아이디와 비밀번호를 모두 입력해주세요");
    return;
  }

  try {
    const response = await axios.post("/comApi/login/loginChk", {
      userId: userId.value,
      userPw: password.value,
    });

    if (response.status === 200) {
      const {
        token,
        userId: id,
        userNm,
        cmpnyCd,
        authCd,
        mblNo,
        email,
        refreshToken,
      } = response.data;

      sessionStorage.setItem("token", token);
      sessionStorage.setItem("gv_cmpnyCd", cmpnyCd);
      sessionStorage.setItem("gv_userId", id);
      sessionStorage.setItem("gv_userNm", userNm);
      sessionStorage.setItem("gv_authCd", authCd);
      sessionStorage.setItem("gv_mblNo", mblNo);
      sessionStorage.setItem("gv_email", email);
      localStorage.setItem("refreshToken", refreshToken);
      userStore.setUser({ userId: id, userNm, cmpnyCd, authCd, mblNo, email });

      if (rememberId.value) localStorage.setItem("savedUserId", userId.value);
      else localStorage.removeItem("savedUserId");

      fnUserTermsAgrChk();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message || err?.message || "로그인에 실패했습니다.";
    await proxy.$alert(msg);
  }
};

const fnUserTermsAgrChk = async () => {
  try {
    const response = await axios.post("/comApi/login/getUserTermsAgrChk", {
      userId: userId.value,
    });

    if (response.status === 200) {
      userTermsNonAgrList.value = response.data;

      if (response.data.length > 0) {
        openPop(TermsPop, {
          loginFlg_p: "Y",
          userTermsNonAgrList_p: userTermsNonAgrList.value,
          onMoveMain: fnMoveMainPath,
          onUserLogout: fnUserLogout,
        });
      } else {
        fnMoveMainPath();
      }
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message || err?.message || "로그인에 실패했습니다..";
    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const focusKill = (e) => {
  if (e.target.id == "userId") {
    if (proxy.$util.isNotEmpty(userId.value)) userIdFocusKill();
  }
};

const fnMoveMainPath = () => router.push("/main");

const fnUserLogout = () => userStore.logout();

const fnOpenTermsPop = () => openPop(TermsPop, {});

const userIdFocusKill = () => {
  userId.value = proxy.$util.toUpperCase(userId.value);
};

const fnAcountInfoSrch = () => {
  fnSiteOpenPop(ActInfoSrch, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    fnUserIdSet: fnUserIdSet,
  });
};

const fnUserIdSet = (p_userId) => {
  userId.value = p_userId;
};

const fnSiteOpenPop = (popId, param) => openPop(popId, param);
</script>

<style>
/* ========== PRAFTA 디자인 토큰 (전역) ========== */
:root {
  --primary: #16a34a;
  --danger: #ef4444;
  --bg: #f9fafb;
  --border: #e5e7eb;
  --borderStrong: #d1d5db;
  --textStrong: #111827;
  --text: #374151;
  --textMuted: #4b5563;
  --focusRing: rgba(22, 163, 74, 0.35);
  --radius-card: 16px;
  --radius-input: 10px;
  --shadow-card: 0 1px 2px rgba(16, 24, 40, 0.06),
    0 1px 3px rgba(16, 24, 40, 0.1);
}

html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.page {
  min-height: 100vh;
  background: var(--bg);
}

/* =======================
   SPLIT LAYOUT
======================= */
.split {
  min-height: 100vh;
  display: flex;
}

/* LEFT PANEL (6) - 중립 톤만 (화이트~bg 약한 그라데이션) */
.art {
  flex: 6;
  min-width: 0;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, var(--bg) 100%);
  display: flex;
  align-items: center;
  justify-content: center;

  --mx: 0;
  --my: 0;
}

.art-glow {
  position: absolute;
  inset: -30%;
  background: radial-gradient(
      circle at 30% 40%,
      rgba(22, 163, 74, 0.06),
      transparent 55%
    ),
    radial-gradient(circle at 70% 60%, rgba(0, 0, 0, 0.02), transparent 55%);
  pointer-events: none;
  transform: translate(calc(var(--mx) * 10px), calc(var(--my) * 10px));
  transition: transform 120ms ease;
}

.art-inner {
  position: relative;
  width: min(520px, 85%);
  aspect-ratio: 1.1 / 1;
  transform: translate(calc(var(--mx) * 8px), calc(var(--my) * 8px));
  transition: transform 140ms ease;
}

/* 캐릭터 공통: 중심축 기준 커서 방향으로 몸 기울임 */
.blob {
  position: absolute;
  border-radius: 999px;
  filter: drop-shadow(0 14px 18px rgba(0, 0, 0, 0.1));
  animation: floaty 5.5s ease-in-out infinite;
  transition: transform 120ms ease-out;
  transform-origin: center center;
}

/* 보라 (왼쪽 위에 비스듬히) */
.blob--purple {
  width: 210px;
  height: 300px;
  left: 16%;
  top: 10%;
  background: #5a2dff;
  border-radius: 46px;
  transform: rotate(calc(-10deg + var(--mx) * 5deg + var(--my) * 3deg))
    translate(calc(var(--mx) * 6px), calc(var(--my) * 6px));
  animation-duration: 6.2s;
}

/* 검정 (가운데 뒤) */
.blob--black {
  width: 170px;
  height: 260px;
  left: 46%;
  top: 22%;
  background: #161616;
  border-radius: 44px;
  transform: rotate(calc(6deg + var(--mx) * 5deg + var(--my) * 3deg))
    translate(calc(var(--mx) * 5px), calc(var(--my) * 5px));
  animation-duration: 5.8s;
}

/* 주황 (왼쪽 아래) */
.blob--orange {
  width: 320px;
  height: 200px;
  left: 12%;
  top: 52%;
  background: #ff6a2b;
  border-radius: 220px 220px 70px 70px;
  transform: rotate(calc(var(--mx) * 5deg + var(--my) * 3deg))
    translate(calc(var(--mx) * 7px), calc(var(--my) * 7px));
  animation-duration: 5.2s;
}

/* 노랑 (오른쪽) */
.blob--yellow {
  width: 170px;
  height: 240px;
  left: 68%;
  top: 44%;
  background: #ffd24a;
  border-radius: 999px;
  transform: rotate(calc(4deg + var(--mx) * 5deg + var(--my) * 3deg))
    translate(calc(var(--mx) * 6px), calc(var(--my) * 6px));
  animation-duration: 6s;
}

/* 눈: 전체가 커서 방향으로 이동 (범위 2배) */
.eyes {
  position: absolute;
  top: 44%;
  left: 50%;
  transform: translate(calc(-50% + var(--mx) * 16px), calc(var(--my) * 16px));
  display: flex;
  gap: 14px;
  transition: transform 100ms ease-out;
}

.eye {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  position: relative;
  overflow: hidden;
}

/* 동공: 흰자(눈) 안에서만 움직이도록 범위 제한 (24px 눈 - 9px 동공 → 최대 7px) */
.pupil {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #111;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(
    calc(-50% + var(--mx) * 7px),
    calc(-50% + var(--my) * 7px)
  );
  transition: transform 90ms ease-out;
}

/* 작은 눈: 커서 따라 이동 (범위 2배) */
.eyes--tiny {
  top: 12%;
  left: 82%;
  gap: 8px;
  transform: translate(calc(-50% + var(--mx) * 12px), calc(var(--my) * 12px));
  transition: transform 100ms ease-out;
}
.eye--tiny {
  width: 14px;
  height: 14px;
  background: rgba(255, 255, 255, 0.82);
}
/* 작은 눈 동공: 흰자 안에서만 (14px 눈 - 5px 동공 → 최대 4px) */
.pupil--tiny {
  width: 5px;
  height: 5px;
  background: #111;
  transform: translate(
    calc(-50% + var(--mx) * 4px),
    calc(-50% + var(--my) * 4px)
  );
  transition: transform 90ms ease-out;
}

/* 노랑 캐릭터 눈 위치 살짝 옆 + 커서 따라 이동 (범위 2배) */
.eyes--side {
  top: 38%;
  left: 26%;
  transform: translate(calc(-50% + var(--mx) * 12px), calc(var(--my) * 12px));
  transition: transform 100ms ease-out;
}

/* 캐릭터 입: 커서 따라 이동 + 커서 방향으로 늘어남 (범위 2배) */
.mouth {
  position: absolute;
  left: 50%;
  top: 64%;
  width: 20px;
  height: 4px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.2);
  border-radius: 0 0 30px 30px;
  transform: translate(calc(-50% + var(--mx) * 12px), calc(var(--my) * 12px))
    scale(calc(1 + var(--mx) * 0.12), calc(1 + var(--my) * 0.08));
  transform-origin: center center;
  box-sizing: border-box;
  transition: transform 100ms ease-out, width 0.2s ease, height 0.2s ease,
    border 0.2s ease, border-radius 0.2s ease, background 0.2s ease;
}

/* ID 포커스 시: 입 벌린 느낌 (타원형) + 커서 따라 이동·늘어남 (범위 2배) */
.art--mouth-open .mouth {
  width: 28px;
  height: 18px;
  border: 2px solid rgba(0, 0, 0, 0.25);
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
  transform: translate(calc(-50% + var(--mx) * 12px), calc(var(--my) * 12px))
    scale(calc(1 + var(--mx) * 0.16), calc(1 + var(--my) * 0.12));
  transform-origin: center center;
}

/* 보라/노랑 등 작은 캐릭터 입 위치·크기 보정 */
.blob--purple .mouth {
  top: 28%;
  left: 82%;
  width: 12px;
  height: 3px;
  border-bottom-width: 1.5px;
}
.art--mouth-open .blob--purple .mouth {
  width: 18px;
  height: 12px;
  border: 1.5px solid rgba(0, 0, 0, 0.25);
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
}

.blob--yellow .mouth {
  top: 52%;
  left: 26%;
  width: 14px;
  height: 3px;
}
.art--mouth-open .blob--yellow .mouth {
  width: 20px;
  height: 14px;
  border: 1.5px solid rgba(0, 0, 0, 0.25);
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
}

/* 비밀번호 포커스 시: 4명 모두 눈 감김 (굵은 선) + 눈·입을 움직일 수 있는 범위의 가장 좌측으로 고정 */
.art--eyes-closed .eye {
  background: transparent !important;
  border: none;
  border-bottom: 8px solid rgba(0, 0, 0, 0.4);
  height: 8px !important;
  min-height: 8px;
  border-radius: 999px;
  transition: height 0.2s ease, background 0.2s ease, border 0.2s ease;
}
.art--eyes-closed .pupil {
  display: none;
}
.art--eyes-closed .eye--tiny {
  height: 6px !important;
  border-bottom: 6px solid rgba(0, 0, 0, 0.4);
}

/* 눈·입을 가장 좌측 위치로 고정 (범위 2배에 맞춤) */
.art--eyes-closed .eyes {
  transform: translate(calc(-50% + -16px), 0);
}
.art--eyes-closed .eyes--tiny,
.art--eyes-closed .eyes--side {
  transform: translate(calc(-50% + -12px), 0);
}
.art--eyes-closed .mouth {
  transform: translate(calc(-50% + -12px), 0) scale(0.88, 0.92);
}

/* 비밀번호 포커스 시: 캐릭터 기울기를 왼쪽 끝으로 고정 */
/* 주황·검정: 왼쪽 아래를 보는 각도 */
.art--eyes-closed .blob--orange {
  transform: rotate(-14deg) translate(-7px, 6px);
}
.art--eyes-closed .blob--black {
  transform: rotate(-8deg) translate(-5px, 5px);
}
/* 보라·노랑: 왼쪽 위를 보는 각도 */
.art--eyes-closed .blob--purple {
  transform: rotate(8deg) translate(-6px, -6px);
}
.art--eyes-closed .blob--yellow {
  transform: rotate(10deg) translate(-6px, -6px);
}

@keyframes floaty {
  0%,
  100% {
    translate: 0 0;
  }
  50% {
    translate: 0 -10px;
  }
}

/* RIGHT PANEL (4) */
.right {
  flex: 4;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.login-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(1rem, 5vw, 2rem);
}

/* 로그인 카드 (우측 패널) */
.login-box {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  padding: 20px 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  max-height: 90vh;
  overflow-y: auto;
}

.title {
  color: var(--textStrong);
  text-align: center;
  font-size: clamp(1.25rem, 3vw, 1.5rem);
  margin-bottom: 1.5rem;
  font-weight: 600;
}

.input-group {
  display: flex;
  flex-direction: column;
}

.input-group input {
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--textStrong);
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  border-radius: var(--radius-input);
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-group input::placeholder {
  color: var(--textMuted);
}

.input-group input:focus {
  border-color: var(--borderStrong);
  box-shadow: 0 0 0 3px var(--focusRing);
  outline: none;
}

/* Primary 버튼 = 로그인 */
.login-btn {
  width: 100%;
  height: 48px;
  padding: 0;
  background: var(--primary);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-input);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.login-btn:hover {
  background: #15803d;
}

.login-btn:focus-visible {
  box-shadow: 0 0 0 3px var(--focusRing);
  outline: none;
}

.help-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  list-style: none;
  padding: 0;
  margin: 0.5rem 0;
  font-size: 0.85rem;
}

.help-links a,
.aTagCls {
  color: var(--textMuted);
  text-decoration: none;
}

.help-links a:hover,
.aTagCls:hover {
  color: var(--textStrong);
  text-decoration: underline;
}

.divider {
  color: var(--borderStrong);
}

.new-user {
  text-align: center;
  margin-top: 1rem;
}

.new-user-text {
  color: var(--textMuted);
  margin: 0 0 0.75rem 0;
  font-size: 0.9rem;
}

.new-user-text strong {
  color: var(--textMuted);
  font-weight: 600;
}

/* Secondary 버튼 = 가입하기 */
.btn-secondary {
  width: 100%;
  height: 48px;
  padding: 0;
  background: #ffffff;
  border: 1px solid var(--border);
  color: var(--text);
  border-radius: var(--radius-input);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.btn-secondary:hover {
  background: var(--bg);
}

.btn-secondary:focus-visible {
  box-shadow: 0 0 0 3px var(--focusRing);
  outline: none;
}

.footer {
  text-align: center;
  padding: 1rem;
  font-size: 12px;
  color: var(--textMuted);
}

.footer a {
  color: inherit;
  text-decoration: none;
}

/* 체크박스: 아이디 저장 */
.remember-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  white-space: nowrap;
  width: fit-content;
  margin-bottom: 0.5rem;
  cursor: pointer;
  padding: 0.25rem 0;
}

.remember-wrap label {
  cursor: pointer;
  user-select: none;
  color: var(--text);
}

.remember-wrap input[type="checkbox"] {
  width: 1.125rem;
  height: 1.125rem;
  accent-color: var(--primary);
  cursor: pointer;
  flex-shrink: 0;
}

/* =======================
   RESPONSIVE
   - 모바일에서는 좌측 패널 숨김
======================= */
@media (max-width: 900px) {
  .art {
    display: none;
  }
  .right {
    flex: 1;
  }
}

@media (max-width: 480px) {
  .login-box {
    padding: 20px 16px;
  }
}
</style>
