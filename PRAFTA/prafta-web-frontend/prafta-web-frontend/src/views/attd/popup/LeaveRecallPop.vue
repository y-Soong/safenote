<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-normal leave-recall-modal">
        <!-- ============ 헤더 ============ -->
        <div class="modal-header">
          <span>부여 연차 회수</span>
          <button class="icon-button" type="button" @click="fnClose">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- ============ 바디 ============ -->
        <div class="modal-body leave-recall">
          <!-- ===== 회수 대상 부여 요약 카드 ===== -->
          <div class="lrc-target-card">
            <p class="lrc-target-title">회수 대상 부여</p>
            <dl class="lrc-target-grid">
              <div class="lrc-target-item">
                <dt>대상 직원</dt>
                <dd>{{ grant.userNm || "-" }}</dd>
              </div>
              <div class="lrc-target-item">
                <dt>부여일</dt>
                <dd>{{ fnFormatDate(grant.grantDate) }}</dd>
              </div>
              <div class="lrc-target-item">
                <dt>부여 일수</dt>
                <dd>{{ grant.granted }}일</dd>
              </div>
              <div class="lrc-target-item">
                <dt>사용 일수</dt>
                <dd>{{ grant.used }}일</dd>
              </div>
            </dl>
          </div>

          <!-- ===== 회수 사유 (필수, 최대 500자) ===== -->
          <div class="lrc-field">
            <label class="lrc-label">
              회수 사유<span class="lrc-required">*</span>
            </label>
            <textarea
              v-model="reason"
              class="lrc-textarea"
              rows="4"
              maxlength="500"
              placeholder="예: 부여 대상 착오로 인한 회수"
            ></textarea>
            <p class="lrc-char-count">{{ reason.length }} / 500</p>
          </div>

          <!-- ===== 경고 안내 박스 ===== -->
          <div class="lrc-warn-box">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path
                d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"
              />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            <p class="lrc-warn-text">
              회수 시 해당 부여는 취소 상태로 변경되며, 부여 이력에 회수 기록과
              사유가 영구 보존됩니다. 단 한 건도 사용되지 않은 부여만 회수할 수
              있습니다.
            </p>
          </div>
        </div>

        <!-- ============ 푸터 ============ -->
        <!-- btn-danger 전역 클래스 부재(button.css) → btn-primary 기반 + scoped 위험 색상 덮어쓰기 -->
        <div class="modal-footer">
          <button class="btn btn-second" type="button" @click="fnClose">
            취소
          </button>
          <button
            class="btn btn-primary lrc-recall-btn"
            type="button"
            :disabled="isLoading || !canSubmit"
            @click="fnSubmit"
          >
            회수하기
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

// ================ Props & Emits ================
const props = defineProps({
  // 회수 대상 부여 정보 (LeaveDetailPop 부여 이력 행에서 전달)
  //   { grantId, userNm, grantDate, granted, used }
  grant: { type: Object, default: () => ({}) },
  // 회수 성공 시 부모(상세/대시보드) 재조회 신호 콜백
  //   (useModal이 onClose를 덮어쓰므로 재조회는 별도 콜백 prop으로 받는다)
  onRecalled: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();

// ================ Refs (Variables) ================
// props 복사 (회수 대상 부여)
const grant = ref({
  grantId: props.grant?.grantId ?? "",
  userNm: props.grant?.userNm ?? "",
  grantDate: props.grant?.grantDate ?? "",
  granted: props.grant?.granted ?? 0,
  used: props.grant?.used ?? 0,
});

// 회수 사유 (필수, 최대 500자)
const reason = ref("");

const isLoading = ref(false);

// ================ Computed ================
// 제출 가능 여부 (사유 필수 — 공백만 입력 차단)
const canSubmit = computed(() => reason.value.trim().length > 0);

// ================ API Functions ================
// 회수하기 — POST /attd09/leave-grant/{grantId}/recall
const fnSubmit = async () => {
  // 1차 검증 (필수 입력). 최종 권위는 백엔드(MANUAL·ACTIVE·USED_DAYS=0 재검증).
  if (!fnValidate()) return;

  // 회수 의사 재확인 (되돌릴 수 없는 작업)
  const ok = await proxy.$confirm(
    "선택한 부여 연차를 회수하시겠습니까? 되돌릴 수 없습니다."
  );
  if (!ok) return;

  isLoading.value = true;
  try {
    await axios.post(
      `/webApi/attd09/leave-grant/${encodeURIComponent(
        grant.value.grantId
      )}/recall`,
      { reason: reason.value.trim() }
    );
    await proxy.$alert("회수되었습니다.");
    // 회수 성공 → 부모(상세/대시보드) 재조회 신호 전파 후 모달 닫기
    if (typeof props.onRecalled === "function") props.onRecalled();
    emit("close");
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "회수 중 오류가 발생했습니다."));
  } finally {
    isLoading.value = false;
  }
};

// ================ Methods/Functions ================
// 1차 검증 (사유 필수 / 길이 500자 — maxlength로도 강제)
const fnValidate = () => {
  if (reason.value.trim().length === 0) {
    proxy.$alert("회수 사유를 입력해 주세요.");
    return false;
  }
  if (reason.value.length > 500) {
    proxy.$alert("회수 사유는 최대 500자까지 입력할 수 있습니다.");
    return false;
  }
  return true;
};

// 모달 닫기
const fnClose = () => {
  emit("close");
};

// ================ 내부 유틸 ================
// YYYYMMDD → "YYYY.MM.DD" 표기. 빈값/형식불충분은 "-".
const fnFormatDate = (yyyymmdd) => {
  const s = String(yyyymmdd || "");
  if (s.length !== 8) return s || "-";
  return formatYmdDot(s);
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.leave-recall-modal {
  width: 100%;
  max-width: 480px;
}

.leave-recall {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* ===== 회수 대상 카드 ===== */
.lrc-target-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.875rem 1rem;
}

.lrc-target-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  margin: 0 0 0.625rem;
}

.lrc-target-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.625rem 1rem;
  margin: 0;
}

.lrc-target-item {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.lrc-target-item dt {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.lrc-target-item dd {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

/* ===== 폼 필드 ===== */
.lrc-field {
  display: flex;
  flex-direction: column;
}

.lrc-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-strong);
  margin-bottom: 0.375rem;
}

.lrc-required {
  color: var(--color-danger);
  margin-left: 0.125rem;
}

.lrc-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.5rem 0.625rem;
  font-size: 0.8125rem;
  line-height: 1.5;
  resize: vertical;
  min-height: 5.625rem;
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}

.lrc-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

.lrc-char-count {
  align-self: flex-end;
  font-size: 0.625rem;
  color: var(--color-text-muted);
  margin: 0.25rem 0 0;
}

/* ===== 경고 안내 박스 ===== */
.lrc-warn-box {
  display: flex;
  gap: 0.5rem;
  background: rgba(239, 68, 68, 0.08);
  border-radius: var(--input-radius);
  padding: 0.625rem 0.75rem;
}

.lrc-warn-box svg {
  color: var(--color-danger);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.lrc-warn-text {
  font-size: 0.6875rem;
  color: var(--color-danger);
  line-height: 1.5;
  margin: 0;
}

/* ===== 회수(위험) 버튼 — btn-primary 기반 위험 색상 덮어쓰기 =====
   전역 button.css 에 btn-danger 가 없어 회수 행위 강조를 scoped 로 처리한다.
   토큰만 사용(--color-danger 계열). developer 가 토큰 추가 시 그에 맞춰 교체 가능. */
.lrc-recall-btn {
  background: var(--color-danger);
  border-color: var(--color-danger);
}

.lrc-recall-btn:hover:not(:disabled) {
  background: var(--color-danger-pressed, var(--color-danger));
  border-color: var(--color-danger-pressed, var(--color-danger));
}

/* ===== 반응형 ===== */
@media (max-width: 480px) {
  .lrc-target-grid {
    grid-template-columns: 1fr;
  }
}
</style>
