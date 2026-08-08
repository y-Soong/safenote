<template>
  <!-- TBM_AI T3-4: 동영상(02)·유튜브(03) 세부항목 텍스트 입력 확정 팝업(대화형 없음, AI 미분석).
       입력 가이드(①핵심 안전주제 ②강조 안전수칙 ③교육 활용방식) + 사용처 안내 + [확정].
       설명 텍스트 필수(빈 확정 불허 — 교육안 생성 게이트 TBM_409_060 과 정합, 2026-07-11).
       ★template/style 골격만. 확정 저장(confirm-item) 호출은 developer 가 채운다. -->
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @keydown.ctrl.a.stop
      @keydown.meta.a.stop
    >
      <div
        class="modal-content-narrow tbmtxt-panel"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>자료 설명 확정 — {{ itemTypeLabel }}</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- 본문 -->
        <div class="modal-body tbmtxt-body">
          <!-- 입력 가이드 -->
          <div class="tbmtxt-guide">
            <div class="tbmtxt-guide__title">입력 가이드</div>
            <ul class="tbmtxt-guide__list">
              <li>① 이 자료의 핵심 안전 주제</li>
              <li>② 강조할 안전 수칙</li>
              <li>③ 교육에서의 활용 방식</li>
            </ul>
            <p class="tbmtxt-guide__use">
              이 의견은 추후 <b>TBM 교육관리</b>에서 교육 생성 시 AI가
              참고합니다.
            </p>
          </div>

          <!-- 설명 입력 -->
          <label class="tbmtxt-label" for="tbmtxtDesc">자료 설명</label>
          <textarea
            id="tbmtxtDesc"
            v-model="confirmDesc"
            class="tbmtxt-textarea"
            rows="8"
            placeholder="예) 프레스 금형 교체 작업의 끼임 위험과 방호장치 사용 수칙을 다룬 영상. 교체 전 전원 차단·기동 잠금(LOTO) 확인을 강조."
          ></textarea>

          <!-- 필수 입력 안내 -->
          <p v-if="isEmptyDesc" class="tbmtxt-warn">
            자료 설명을 입력해야 확정할 수 있습니다.
          </p>
        </div>

        <!-- 푸터 (F-10 규약: 왼쪽=진행/확정(확정), 오른쪽=이탈(닫기)) -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnConfirm">확정</button>
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
// ================ Imports ================
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

// ================ Instance ================
const { proxy } = getCurrentInstance();

// ================ Props & Emits ================
// mtrlCd_p/mtrlItemCd_p: 확정 대상 세부항목. itemType_p: '02'(동영상)/'03'(유튜브).
// desc_p: 기존 확정 설명(AI_CONFIRM_DESC) 재편집 시 초기값. onConfirmed: 확정 후 배지 갱신 콜백.
const props = defineProps({
  mtrlCd_p: String,
  mtrlItemCd_p: String,
  itemType_p: String,
  desc_p: String,
  onConfirmed: Function,
});
const emit = defineEmits(["close"]);

// ================ Refs (Variables) ================
const modalRef = ref(null);
const confirmDesc = ref(""); // developer: onMounted 에서 props.desc_p 로 초기화

// 화면 중앙(살짝 위) 배치 + 드래그
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 4,
});

// ================ Computed ================
const itemTypeLabel = computed(() =>
  props.itemType_p === "03" ? "유튜브" : "동영상"
);
const isEmptyDesc = computed(() => !confirmDesc.value.trim());

// ================ Life Cycle Functions ================
onMounted(() => {
  confirmDesc.value = props.desc_p || "";
});

// ================ API Functions ================
// 확정: 동영상·유튜브는 LLM 미호출 경로로 AI_CONFIRM_DESC 저장 + AI_STATUS='CONFIRMED'.
// 설명 텍스트 필수 — 빈 확정은 교육안 생성 게이트(TBM_409_060)에 걸려 생성이 차단되므로 불허(서버도 400).
const submitting = ref(false);
const fnConfirm = async () => {
  if (submitting.value) return;

  const desc = confirmDesc.value.trim();

  // 빈 확정 불허: 입력 없으면 차단.
  if (!desc) {
    await proxy.$alert("자료 설명을 입력해야 확정할 수 있습니다.");
    return;
  }

  submitting.value = true;
  try {
    // LLM 미호출 확정 경로. confirmText 로 AI_CONFIRM_DESC 저장 + CONFIRMED 전이.
    await axios.post(
      "/webApi/tbmai01/confirm-item",
      { mtrlItemCd: props.mtrlItemCd_p, confirmText: desc },
      { headers: { "Content-Type": "application/json" } }
    );
    if (typeof props.onConfirmed === "function") props.onConfirmed();
    emit("close");
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "확정 중 오류가 발생했습니다.")
    );
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
/* ── 컨테이너 ── */
.tbmtxt-panel {
  width: 90%;
}

/* ── 본문 ── */
.tbmtxt-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* 입력 가이드 카드 */
.tbmtxt-guide {
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  background: var(--color-bg);
  padding: 0.75rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.tbmtxt-guide__title {
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: var(--btn-font-sm);
}
.tbmtxt-guide__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: var(--color-text);
  font-size: var(--btn-font);
}
.tbmtxt-guide__use {
  margin: 0.15rem 0 0;
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.tbmtxt-label {
  font-weight: 600;
  color: var(--color-text);
  font-size: var(--btn-font-sm);
}

.tbmtxt-textarea {
  resize: vertical;
  min-height: 8rem;
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--btn-font);
  font-family: inherit;
}

/* 빈 확정 경고문(위험색 강조) */
.tbmtxt-warn {
  margin: 0;
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--color-danger);
  border-radius: var(--btn-radius);
  color: var(--color-danger);
  font-size: var(--btn-font-sm);
  background: var(--color-surface);
}
</style>
