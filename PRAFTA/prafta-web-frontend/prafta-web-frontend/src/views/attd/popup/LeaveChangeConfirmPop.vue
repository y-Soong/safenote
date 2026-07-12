<!--
  LeaveChangeConfirmPop.vue — 관리자 최종 확인 팝업 (prafta-com-008-C-3)
  유형: frontend-component (웹 관리자 팝업)
  연결 작업: PRAFTA-{C-4-web}
  참조 패턴: views/attd/popup/AttdDayDetailPop.vue (상세 + 처리 액션)
  역할: 근로자가 동의(AGREED)한 변경/삭제 요청을 관리자가 최종 확인 → 실제 반영(MOVE: START_DATE 갱신 / DELETE: CANCELLED + 차감 복원).
        거부(REJECTED)건은 열람만(원 연차 불변). 골격 = 상세 표시 + 확인 버튼.
-->
<template>
  <div class="modal-overlay" @click.self="onClose">
    <div class="modal-content lcc-pop">
      <header class="modal-header">
        <h2 class="modal-title">변경 요청 확인</h2>
        <button
          type="button"
          class="modal-close"
          aria-label="닫기"
          @click="onClose"
        >
          ×
        </button>
      </header>

      <div class="modal-body lcc-body">
        <p v-if="loading" class="lcc-state">불러오는 중...</p>

        <template v-else-if="detail">
          <!-- 요청 요약 -->
          <dl class="lcc-detail">
            <div>
              <dt>사용자</dt>
              <dd>{{ detail.userNm }}</dd>
            </div>
            <div>
              <dt>대상 연차일</dt>
              <dd>{{ detail.targetStartDate }}</dd>
            </div>
            <div>
              <dt>요청유형</dt>
              <dd>{{ detail.reqTypeNm }}</dd>
            </div>
            <div v-if="detail.reqType === 'MOVE'">
              <dt>이동대상일</dt>
              <dd>{{ detail.moveTargetDate }}</dd>
            </div>
            <div>
              <dt>발의주체</dt>
              <dd>{{ detail.initiatorTypeNm }}</dd>
            </div>
            <div>
              <dt>요청사유</dt>
              <dd>{{ detail.reqReason }}</dd>
            </div>
            <div>
              <dt>근로자응답</dt>
              <dd>{{ detail.workerResponseNm }}</dd>
            </div>
            <div v-if="detail.responseReason">
              <dt>응답사유</dt>
              <dd>{{ detail.responseReason }}</dd>
            </div>
            <div>
              <dt>상태</dt>
              <dd>{{ detail.reqStatusNm }}</dd>
            </div>
          </dl>

          <!-- 동의 건만 확인 가능, 거부/대기 건은 안내만 -->
          <p
            v-if="detail.reqStatus === 'REJECTED'"
            class="lcc-notice lcc-notice--danger"
          >
            거부된 요청입니다. 원 연차는 변경되지 않았습니다.
          </p>
          <p v-else-if="detail.reqStatus === 'REQUESTED'" class="lcc-notice">
            근로자 응답 대기 중입니다. 응답 전까지 기존 연차일이 유지됩니다.
          </p>

          <!-- 근로자 발의(WORKER) 건은 관리자가 승인 또는 반려. 반려 시 사유 필수. -->
          <div v-if="canReject" class="lcc-reject">
            <label class="lcc-reject__label"
              >반려 사유 <span class="lcc-req">*</span></label
            >
            <textarea
              v-model="rejectReason"
              class="lcc-textarea"
              rows="2"
              maxlength="500"
              placeholder="반려 시 사유를 입력하세요 (필수)"
            ></textarea>
          </div>
        </template>
      </div>

      <footer class="modal-footer lcc-footer">
        <button type="button" class="btn btn-ghost" @click="onClose">
          닫기
        </button>
        <button
          v-if="canReject"
          type="button"
          class="btn btn-ghost lcc-btn-reject"
          :disabled="!rejectReason.trim() || submitting"
          @click="onReject"
        >
          반려
        </button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="!canConfirm || submitting"
          @click="onConfirm"
        >
          {{ canReject ? "승인(반영)" : "최종 확인(반영)" }}
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from "vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

const props = defineProps({
  changeReqId: { type: String, default: "" },
});
const emit = defineEmits(["close", "confirmed"]);

const { proxy } = getCurrentInstance();

// ── 상태 ─────────────────────────────────────────────────────────────────
const loading = ref(true);
const submitting = ref(false);
const detail = ref(null);
const rejectReason = ref("");

// 코드 → 라벨 매핑 (서버 row 는 코드값만 반환)
const REQ_TYPE_NM = { MOVE: "이동", DELETE: "삭제" };
const INITIATOR_TYPE_NM = { ADMIN: "관리자", WORKER: "근로자" };
const REQ_STATUS_NM = {
  REQUESTED: "요청(응답대기)",
  AGREED: "동의(확인대기)",
  REJECTED: "거부",
  CONFIRMED: "확정",
  CLOSED: "종료",
};
const WORKER_RESPONSE_NM = { PENDING: "대기", AGREE: "동의", REJECT: "거부" };

// YYYYMMDD → "YYYY.MM.DD" 표기. dateFormat 단일 출처에 위임.
const fmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd ?? "";
  return formatYmdDot(ymd);
};

// 동의(AGREED) 상태만 최종 확인/승인 가능 (UI 게이트 — 서버도 동일 강제)
const canConfirm = computed(() => detail.value?.reqStatus === "AGREED");
// 근로자 발의(WORKER) + AGREED 건만 관리자 반려 가능
const canReject = computed(
  () =>
    detail.value?.reqStatus === "AGREED" &&
    detail.value?.initiatorType === "WORKER"
);

const onClose = () => emit("close");

// 상세 조회: GET /webApi/attd13/change-requests/{changeReqId}
const fnLoadDetail = async () => {
  loading.value = true;
  try {
    const res = await axios.get(
      `/webApi/attd13/change-requests/${props.changeReqId}`
    );
    if (res.status === 200) {
      const d = res.data?.detail;
      if (d) {
        detail.value = {
          changeReqId: d.changeReqId,
          reqStatus: d.reqStatus,
          reqType: d.reqType,
          initiatorType: d.initiatorType,
          userNm: d.targetUserNm,
          targetStartDate: fmtYmd(d.targetStartDate),
          moveTargetDate: fmtYmd(d.moveTargetDate),
          reqReason: d.reqReason,
          responseReason: d.responseReason,
          rejectReason: d.rejectReason,
          reqTypeNm: REQ_TYPE_NM[d.reqType] || d.reqType,
          initiatorTypeNm:
            INITIATOR_TYPE_NM[d.initiatorType] || d.initiatorType,
          reqStatusNm: REQ_STATUS_NM[d.reqStatus] || d.reqStatus,
          workerResponseNm:
            WORKER_RESPONSE_NM[d.workerResponse] || d.workerResponse,
        };
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR))
    );
    emit("close");
  } finally {
    loading.value = false;
  }
};

// 승인/확인: POST /webApi/attd13/change-requests/{id}/confirm
//   서버: AGREED 검증 + 마감(대상일·이동출발일) 재검증 + DIRECT_USE_KEY 충돌 재검증 + 실제 반영 + CONFIRMED + PUSH.
const onConfirm = async () => {
  if (!canConfirm.value || submitting.value) return;
  submitting.value = true;
  try {
    await axios.post(
      `/webApi/attd13/change-requests/${props.changeReqId}/confirm`
    );
    await proxy.$alert("요청을 확인(반영)했습니다.");
    emit("confirmed");
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)));
  } finally {
    submitting.value = false;
  }
};

// 반려: POST /webApi/attd13/change-requests/{id}/reject (WORKER 발의건). 원 연차 불변.
const onReject = async () => {
  if (!canReject.value || submitting.value) return;
  if (!rejectReason.value.trim()) {
    await proxy.$alert("반려 사유를 입력해 주세요.");
    return;
  }
  submitting.value = true;
  try {
    await axios.post(
      `/webApi/attd13/change-requests/${props.changeReqId}/reject`,
      {
        REJECT_REASON: rejectReason.value.trim(),
      }
    );
    await proxy.$alert("요청을 반려했습니다.");
    emit("confirmed");
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)));
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  if (props.changeReqId) fnLoadDetail();
  else loading.value = false;
});
</script>

<style scoped>
.lcc-pop {
  width: 440px;
  max-width: 92vw;
  max-height: 80vh;
  /* 기본 modal-content 의 20px 패딩 제거 → 헤더/본문/푸터가 박스 끝에 밀착.
     overflow:hidden 으로 헤더/푸터 모서리를 16px 라운드에 맞춰 클립. */
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.lcc-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: var(--card-padding, 20px);
}

.lcc-state {
  text-align: center;
  padding: var(--card-padding, 20px);
  color: var(--color-text-muted);
}

.lcc-detail {
  display: grid;
  gap: var(--space-sm, 8px);
  margin: 0;
}
.lcc-detail div {
  display: flex;
  gap: var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
}
.lcc-detail dt {
  width: 84px;
  flex-shrink: 0;
  color: var(--color-text-muted);
}
.lcc-detail dd {
  margin: 0;
  color: var(--color-text-strong);
}

.lcc-notice {
  margin: var(--space-sm, 8px) 0 0;
  padding: var(--space-sm, 8px);
  border-radius: var(--input-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  font-size: var(--btn-font-sm, 11px);
}
.lcc-notice--danger {
  background: var(--color-warning-bg);
  color: var(--color-danger);
}

.lcc-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 8px);
  padding: var(--space-sm, 8px) var(--card-padding, 20px);
  border-top: 1px solid var(--color-border);
}

.lcc-reject {
  margin-top: var(--space-sm, 8px);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs, 4px);
}
.lcc-reject__label {
  font-size: var(--btn-font, 11px);
  font-weight: 600;
  color: var(--color-text-strong);
}
.lcc-req {
  color: var(--color-danger);
}
.lcc-textarea {
  width: 100%;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  padding: var(--space-sm, 8px);
  font-family: inherit;
  font-size: var(--btn-font, 11px);
  resize: vertical;
}
.lcc-btn-reject {
  color: var(--color-danger);
  border-color: var(--color-danger);
}
</style>
