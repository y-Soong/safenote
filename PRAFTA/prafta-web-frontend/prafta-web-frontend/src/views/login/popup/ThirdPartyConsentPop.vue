<!--
  ThirdPartyConsentPop — 연동 회사 제3자 제공 동의(006) 로그인 게이트(웹).
  - 진입: LoginView 가 필수약관 단계를 통과한 뒤 GET /comApi/consent/subcon-consent-gate 로 판정 후 오픈.
  - ★ 필수약관 팝업(TermsPop)과 결정적으로 다르다: 미동의도 정상 통과(로그아웃 없음).
  - 응답(동의/미동의) 저장 = 게이트 해제. 응답 없이 닫으면 다음 로그인에 재노출된다.
  - 약관 요약/제목은 서버(TB_TERMS)에서 받아 렌더한다 — 화면에 약관 문구를 하드코딩하지 않는다.
  - 앱 ThirdPartyConsentView 와 동작/문구를 맞춘다(채널 간 동의 획득 조건 불일치 방지).
-->
<template>
  <div class="modal-overlay prafta-modal-popup min-h-screen">
    <div
      class="modal-content-narrow"
      :style="{ top: position.y + 'px', left: position.x + 'px' }"
      ref="modalRef"
    >
      <div class="modal-header" @mousedown="startDrag">
        <span>연동 회사 자료 제공 동의</span>
        <button class="icon-button" @click="fnSkip">✕</button>
      </div>

      <div class="form-container">
        <!-- 자유의사 고지(강제 아님) -->
        <p class="tpc-notice">
          이 사업장은 다른 회사와 연동되어 있습니다.<br />
          동의하지 않아도 서비스 이용에 제한이 없습니다.
        </p>

        <!-- 약관 제목 + 전문 보기 -->
        <div class="tpc-terms-head">
          <span class="tpc-terms-title">{{ "(선택) " + termsNm }}</span>
          <button type="button" class="tpc-terms-view" @click="fnViewTerms">
            전문 보기
          </button>
        </div>

        <!-- 약관 요약(서버 TERMS_DESC) — v-html 금지(XSS 방지) -->
        <p class="tpc-summary">{{ termsDesc }}</p>

        <!-- 철회/소급 없음 고지 -->
        <p class="tpc-hint">
          동의는 내 정보에서 언제든 철회할 수 있습니다.<br />
          다만 철회 전 이미 제공된 자료는 회수되지 않습니다.
        </p>
      </div>

      <div class="modal-footer">
        <div class="btn-group">
          <button
            class="btn btn-primary"
            :disabled="isSubmitting"
            @click="fnAgree"
          >
            동의합니다
          </button>
          <button
            class="btn btn-default"
            :disabled="isSubmitting"
            @click="fnDisagree"
          >
            동의하지 않음
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import { ref, onMounted, getCurrentInstance, defineProps, defineEmits } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import TermsDetailPop from "@/components/popup/TermsDetailPop.vue";

// ================ Props & Emits ================
const props = defineProps({
  // 게이트 판정 응답(LoginView 가 이미 조회한 값). termsId/termsNm/termsDesc 를 그대로 받는다.
  gate_p: Object,
  // 응답 저장/건너뛰기 후 이어질 다음 단계(메인 진입 등).
  onDone: Function,
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Refs (Variables) ================
const termsId = ref("");
const termsNm = ref("");
const termsDesc = ref("");
const isSubmitting = ref(false);

// ================ Life Cycle Functions ================
onMounted(() => {
  const gate = props.gate_p || {};
  termsId.value = gate.termsId || "";
  termsNm.value = gate.termsNm || "";
  termsDesc.value = gate.termsDesc || "";
});

// ================ API Functions ================
// 응답 저장(동의/미동의 공통). 성공 시에만 다음 단계로 — 실패하면 팝업에 머문다(미응답 = 다음 로그인 재노출).
//   ★ termsId 는 전송하지 않는다(서버 상수 006 고정 — 임의 약관 토글 주입면 제거).
const fnRespond = async (agrYn) => {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  try {
    await axios.post("/comApi/consent/subcon-consent-respond", { agrYn });
    fnDone();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.THIRD_PARTY_CONSENT_FAILED))
    );
  } finally {
    isSubmitting.value = false;
  }
};

// ================ Methods/Functions ================
const fnAgree = () => fnRespond("Y");

// 미동의: 확인 후 'N' 저장(로그아웃 없음 — 필수약관 게이트와 다른 흐름).
const fnDisagree = async () => {
  const ok = await proxy.$confirm(
    getMessage(MSG.THIRD_PARTY_CONSENT_DISAGREE_CONFIRM)
  );
  if (!ok) return;
  await fnRespond("N");
};

// 닫기: 응답 없이 통과(의사표시가 없으므로 미동의로 기록하지 않는다 → 다음 로그인에 재노출).
const fnSkip = () => fnDone();

// 다음 단계로 이어준 뒤 팝업을 닫는다(로그인 흐름을 끊지 않는다).
const fnDone = () => {
  if (props.onDone) props.onDone();
  emit("close");
};

const fnViewTerms = () => {
  openPop(TermsDetailPop, {
    termsId_p: termsId.value,
    termsNm_p: termsNm.value,
  });
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding: 1rem 1.2rem;
}

.tpc-notice {
  margin: 0;
  padding: 0.75rem;
  background: var(--color-info-tint, #eff6ff);
  color: var(--color-info-strong, #1d4ed8);
  border-radius: 8px;
  font-size: 0.85rem;
  line-height: 1.5;
}

.tpc-terms-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.tpc-terms-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

.tpc-terms-view {
  flex-shrink: 0;
  background: transparent;
  border: 0;
  padding: 0;
  font-size: 0.8rem;
  color: var(--color-primary, #16a34a);
  text-decoration: underline;
  cursor: pointer;
}

.tpc-summary {
  margin: 0;
  padding: 0.75rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  font-size: 0.85rem;
  line-height: 1.6;
  color: var(--color-text, #374151);
  white-space: pre-line;
  max-height: 40vh;
  overflow-y: auto;
}

.tpc-hint {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--color-text-muted, #6b7280);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0.75rem 1.2rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}
</style>
