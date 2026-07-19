<!--
  PlatformSmsGateOverlay.vue — 위치정보 열람 SMS 인증 게이트 오버레이 (Platform_04 전용)
  - 역할: 화면 내 절대배치 오버레이로 조회 UI 를 전면 차단하고, 세션 운영자 본인의
          등록 휴대폰 SMS 인증(발송 → 6자리 입력 → 검증)을 수행한다.
  - 서버가 진짜 게이트: 검증 통과 여부는 서버가 판정(gps-lists 호출마다 재검증, 10분 유효).
          본 오버레이는 프론트 보조 게이트다 (요청서 §3-2, §5-1).
  - 휴대폰번호는 클라이언트가 알지 못한다: sms-send 는 body 없이 호출하고 서버가
          토큰 사용자(TB_USER)의 등록 휴대폰으로 발송한다 (PII 최소화 — 요청서 §5-3).
  - 코드 유효시간: 발송 후 60초(기존 baseinfo SMS 코어 전례 — EXPIRED_AT = NOW()+1분).
  - emit: verified — 검증 성공 시 부모(Platform_04)가 오버레이를 제거하고 조회 UI 활성.
  - 골격: planner 작성(template + scoped style), script 로직: developer 작성(PLT-LOC-06).
-->
<template>
  <div class="sms-gate" role="dialog" aria-modal="true" aria-label="위치정보 열람 SMS 인증">
    <div class="sms-gate__card">
      <!-- 잠금 아이콘 -->
      <div class="sms-gate__icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="36" height="36">
          <path
            d="M12 2a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-5-5zm-3 8V7a3 3 0 1 1 6 0v3H9zm3 4a2 2 0 0 1 1 3.73V19h-2v-1.27A2 2 0 0 1 12 14z"
          />
        </svg>
      </div>

      <h3 class="sms-gate__title">위치정보 열람 본인 인증</h3>
      <p class="sms-gate__desc">
        개인위치정보 보호를 위해 SMS 본인 인증이 필요합니다.<br />
        운영자 계정에 등록된 휴대폰으로 인증번호를 발송합니다.<br />
        인증은 통과 후 <b>10분간</b> 유효합니다.
      </p>

      <!-- 1단계: 발송 -->
      <div v-if="step === 'idle'" class="sms-gate__actions">
        <button type="button" class="sms-gate__btn sms-gate__btn--primary" :disabled="sending" @click="fnSend">
          {{ sending ? "발송 중…" : "인증번호 발송" }}
        </button>
      </div>

      <!-- 2단계: 입력 + 검증 -->
      <div v-else class="sms-gate__verify">
        <div class="sms-gate__input-row">
          <input
            v-model.trim="certNo"
            type="text"
            inputmode="numeric"
            maxlength="6"
            placeholder="인증번호 6자리"
            class="sms-gate__input"
            :disabled="verifying"
            @keyup.enter="fnVerify"
          />
          <span class="sms-gate__timer" :class="{ 'is-expired': remainSec <= 0 }">
            {{ remainSec > 0 ? fnFormatRemain(remainSec) : "만료" }}
          </span>
        </div>

        <!-- 오류 인라인 표시 (공통 정책서 §13.2 피드백: Alert 인라인) -->
        <p v-if="errorMsg" class="sms-gate__error">{{ errorMsg }}</p>

        <div class="sms-gate__actions">
          <button
            type="button"
            class="sms-gate__btn sms-gate__btn--primary"
            :disabled="verifying || remainSec <= 0 || certNo.length !== 6"
            @click="fnVerify"
          >
            {{ verifying ? "확인 중…" : "확인" }}
          </button>
          <button type="button" class="sms-gate__btn sms-gate__btn--ghost" :disabled="sending" @click="fnSend">
            재발송
          </button>
        </div>
      </div>

      <p class="sms-gate__note">
        본 열람은 위치정보법상 이용·제공사실 확인자료로 기록됩니다.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, getCurrentInstance, defineEmits } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const emit = defineEmits(["verified"]);

const { proxy } = getCurrentInstance();

/* 진행 단계: 'idle'(발송 전) / 'sent'(입력 대기) */
const step = ref("idle");
const certNo = ref("");
const sending = ref(false);
const verifying = ref(false);
const remainSec = ref(0); // 코드 입력 유효시간 카운트다운(발송 후 60초 — 서버 EXPIRED_AT 전례)
const errorMsg = ref("");

/* 코드 유효시간 카운트다운 interval 핸들 (반응형 불필요 — 일반 변수) */
let countdownTimer = null;

function fnStartCountdown() {
  fnStopCountdown();
  countdownTimer = setInterval(() => {
    if (remainSec.value > 0) remainSec.value -= 1;
    if (remainSec.value <= 0) fnStopCountdown();
  }, 1000);
}

function fnStopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
}

/*
 * 표시 직후 인증 상태 재확인 — 이미 유효(10분 창) 인증이 있으면 즉시 통과.
 * (부모의 403 재게이트 등으로 오버레이가 다시 뜬 경우 불필요한 재발송 방지.
 *  실제 게이트는 서버가 gps-lists 호출마다 재판정하므로 보안 무영향.)
 */
onMounted(async () => {
  try {
    const response = await axios.get("/platformApi/location/sms-status");
    if (response.status === 200 && response.data?.verified === true) {
      emit("verified");
    }
  } catch (err) {
    // 상태 확인 실패는 무시 — 정상 발송 흐름으로 진행(서버가 진짜 게이트)
    console.error("[PlatformSmsGateOverlay] sms-status 확인 실패:", err);
  }
});

onBeforeUnmount(() => {
  fnStopCountdown();
});

/*
 * 인증번호 발송(재발송 겸용) — POST /platformApi/location/sms-send (PLT-LOC-03).
 *   body 없음: 서버가 토큰 운영자 본인의 등록 휴대폰으로 발송(위조 불가, PII 미노출).
 *   성공: step='sent' + 60초 카운트다운. 실패(PLATFORM_400_012 등):
 *   sent 단계면 인라인 표시, idle 단계면 인라인 영역이 없으므로 $alert.
 */
async function fnSend() {
  if (sending.value) return;
  errorMsg.value = "";
  sending.value = true;

  try {
    const response = await axios.post("/platformApi/location/sms-send");
    if (response.status === 200) {
      step.value = "sent";
      certNo.value = "";
      remainSec.value = 60;
      fnStartCountdown();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "인증번호 발송 중 오류가 발생했습니다.");
    if (step.value === "sent") {
      errorMsg.value = msg;
    } else {
      await proxy.$alert(msg);
    }
  } finally {
    sending.value = false;
  }
}

/*
 * 인증번호 검증 — POST /platformApi/location/sms-verify { certNo } (PLT-LOC-03).
 *   성공: emit("verified") — 부모가 오버레이 제거 + 조회 UI 활성.
 *   실패(PLATFORM_400_010 불일치/만료): 인라인 오류 표시 + 입력 초기화.
 */
async function fnVerify() {
  if (verifying.value || remainSec.value <= 0 || certNo.value.length !== 6) return;
  errorMsg.value = "";
  verifying.value = true;

  try {
    const response = await axios.post("/platformApi/location/sms-verify", {
      certNo: certNo.value,
    });
    if (response.status === 200) {
      fnStopCountdown();
      emit("verified");
    }
  } catch (err) {
    errorMsg.value = resolveApiErrorMessage(err, "인증번호 확인 중 오류가 발생했습니다.");
    certNo.value = "";
  } finally {
    verifying.value = false;
  }
}

/* 잔여초 → m:ss 표시 */
function fnFormatRemain(sec) {
  const s = Math.max(0, Number(sec) || 0);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}
</script>

<style scoped>
/* 부모(viewBody, position: relative) 전면 차단 오버레이 */
.sms-gate {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.45); /* --color-text-strong 기반 딤 */
  border-radius: var(--input-radius);
}

/* 인증 카드 (tokens.css 카드 토큰) */
.sms-gate__card {
  width: 360px;
  max-width: calc(100% - 2rem);
  padding: var(--card-padding);
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  text-align: center;
}

.sms-gate__icon {
  margin: 0 auto 0.5rem;
  color: var(--color-primary);
}
.sms-gate__icon svg {
  fill: currentColor;
}

.sms-gate__title {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

.sms-gate__desc {
  margin: 0 0 1rem;
  font-size: 0.82rem;
  line-height: 1.55;
  color: var(--color-text-muted);
}

.sms-gate__verify {
  margin: 0 0 0.25rem;
}

.sms-gate__input-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.sms-gate__input {
  width: 160px;
  height: 38px;
  padding: 0 0.6rem;
  font-size: 1rem;
  letter-spacing: 0.35em;
  text-align: center;
  color: var(--color-text-strong);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
}
.sms-gate__input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width) var(--color-focus-ring);
}

.sms-gate__timer {
  min-width: 3.2em;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary);
}
.sms-gate__timer.is-expired {
  color: var(--color-danger);
}

.sms-gate__error {
  margin: 0 0 0.5rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-danger);
}

.sms-gate__actions {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
  margin-top: 0.25rem;
}

.sms-gate__btn {
  height: var(--btn-height-lg);
  padding: 0 var(--btn-padding-lg);
  font-size: var(--btn-font);
  font-weight: 600;
  font-family: inherit;
  border-radius: var(--btn-radius-lg);
  cursor: pointer;
}
.sms-gate__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.sms-gate__btn--primary {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}
.sms-gate__btn--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}
.sms-gate__btn--ghost {
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
}

.sms-gate__note {
  margin: 1rem 0 0;
  font-size: 0.72rem;
  color: var(--color-text-muted);
}
</style>
