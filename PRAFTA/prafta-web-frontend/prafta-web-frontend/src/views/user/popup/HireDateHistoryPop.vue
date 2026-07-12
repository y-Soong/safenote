<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal hire-hist-pop"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>입사일 변경 이력</span>
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

        <!-- 🔹 Body -->
        <div class="hire-hist-pop__body">
          <!-- 대상자 + 현재(마지막) 상태 요약 -->
          <div class="hire-hist-pop__summary">
            <p class="hire-hist-pop__target">
              <strong>{{ userNm }}</strong>
              <span v-if="nodeNm"> · {{ nodeNm }}</span>
              <span v-if="userId"> · {{ userId }}</span>
            </p>
            <p class="hire-hist-pop__current" v-if="latest">
              현재 입사일
              <strong>{{ fnFormatDate(latest.newHireDate) }}</strong>
              <span class="hire-hist-pop__current-sub">
                · 최근 변경 {{ latest.changedAt
                }}{{ latest.changerNm ? " · " + latest.changerNm : "" }}
              </span>
            </p>
          </div>

          <!-- loading -->
          <div class="hire-hist-pop__state" v-if="isLoading">
            이력 조회 중...
          </div>

          <!-- table -->
          <div class="hire-hist-pop__table-wrap" v-else-if="list.length > 0">
            <table class="hire-hist-pop__table">
              <thead>
                <tr>
                  <th>변경일시</th>
                  <th>변경 (전 → 후)</th>
                  <th>처리 방식</th>
                  <th>적용</th>
                  <th>변경자</th>
                  <th>사유</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, idx) in list"
                  :key="row.histId"
                  :class="{ 'is-latest': idx === 0 }"
                >
                  <td class="hh-nowrap">
                    <span
                      v-if="idx === 0"
                      class="hire-hist-pop__badge hire-hist-pop__badge--latest"
                      >최근</span
                    >
                    {{ row.changedAt || "-" }}
                  </td>
                  <td class="hh-nowrap">
                    <span class="hh-prev">{{
                      fnFormatDate(row.prevHireDate)
                    }}</span>
                    <span class="hh-arrow" aria-hidden="true">→</span>
                    <span class="hh-new">{{
                      fnFormatDate(row.newHireDate)
                    }}</span>
                  </td>
                  <td>{{ fnHandlingLabel(row.handlingType) }}</td>
                  <td class="hh-nowrap">
                    <span
                      class="hire-hist-pop__applied"
                      :class="
                        row.appliedYn === 'Y' ? 'is-applied' : 'is-pending'
                      "
                    >
                      {{ row.appliedYn === "Y" ? "적용완료" : "미적용" }}
                    </span>
                    <span
                      v-if="row.appliedYn === 'Y' && row.appliedDate"
                      class="hh-applied-date"
                      >{{ row.appliedDate }}</span
                    >
                  </td>
                  <td class="hh-nowrap">{{ row.changerNm || "-" }}</td>
                  <td>
                    <button
                      v-if="row.changeReason"
                      class="hist-reason-btn"
                      type="button"
                      @click="openReasonPopup(row.changeReason)"
                    >
                      보기
                    </button>
                    <span v-else class="hh-muted">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- empty -->
          <div class="hire-hist-pop__state" v-else>변경 이력이 없습니다.</div>
        </div>

        <!-- 🔹 Footer -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- 사유 보기 팝업 (읽기 전용) — Attd_07 처리이력 사유 팝업 패턴 차용.
       사유가 길어도 본 이력 테이블 레이아웃이 깨지지 않도록 별도 팝업으로 분리한다.
       body로 Teleport + prafta-nested-modal-overlay로 중첩 모달 위에 띄운다. -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="reasonPopup.open"
        class="del-pop-backdrop reason-pop-backdrop prafta-nested-modal-overlay"
        @click.self="closeReasonPopup"
      >
        <div class="del-pop" @click.stop>
          <div class="del-pop-head">
            <h3>변경 사유</h3>
            <button
              class="del-pop-close"
              type="button"
              @click="closeReasonPopup"
            >
              ×
            </button>
          </div>
          <div class="del-pop-body">
            <div class="reason-view">{{ reasonPopup.reason || "-" }}</div>
          </div>
          <div class="del-pop-foot">
            <button class="btn-cancel" type="button" @click="closeReasonPopup">
              닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  cmpnyCd_p: String, // 회사 코드 (안내용, 서버는 토큰으로 스코프 강제)
  userCd_p: String, // 대상 사용자 코드
  userId_p: String, // 대상 사용자 ID (안내용)
  userNm_p: String, // 대상 사용자명 (안내용)
  nodeNm_p: String, // 소속부서명 (안내용)
});

// =========================== Ref ===========================
const modalRef = ref(null);
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 5,
});

const userCd = ref(props.userCd_p || "");
const userId = ref(props.userId_p || "");
const userNm = ref(props.userNm_p || "");
const nodeNm = ref(props.nodeNm_p || "");

const list = ref([]);
const isLoading = ref(false);

// 사유 보기 팝업 상태 (읽기 전용). 사유가 길어도 표 레이아웃이 깨지지 않도록 분리 (Attd_07 패턴)
const reasonPopup = ref({ open: false, reason: "" });
const openReasonPopup = (reason) => {
  reasonPopup.value = { open: true, reason: reason || "" };
};
const closeReasonPopup = () => {
  reasonPopup.value.open = false;
};

// =========================== Computed ===========================
// 최근(마지막) 변경 = 목록 첫 행(서버가 INSERT_DATE 내림차순으로 내려줌)
const latest = computed(() => (list.value.length > 0 ? list.value[0] : null));

// =========================== Static ===========================
// 처리 방식[SYS039] 라벨 (HireDateEditPop 옵션과 동일 의미, 표 표기용 축약)
const HANDLING_LABELS = {
  KEEP_AND_BACKFILL: "기존 유지 + 누락 소급",
  KEEP_AND_APPLY_NEW: "기존 유지 + 신규만 적용",
  RESET_ALL: "전체 삭제 후 재계산",
};

// =========================== Methods ===========================
const fnHandlingLabel = (code) => HANDLING_LABELS[code] || code || "-";

// YYYYMMDD(또는 YYYY-MM-DD) -> "YYYY.MM.DD" 표기. 빈값/형식불충분은 "-".
const fnFormatDate = (val) => {
  const s = String(val || "").replace(/-/g, "");
  if (s.length !== 8) return s || "-";
  return formatYmdDot(s);
};

const fnLoad = async () => {
  if (!userCd.value) return;
  isLoading.value = true;
  try {
    // 대상 userCd는 path, 회사 스코프/권한은 서버가 토큰으로 강제(IDOR 방지)
    const res = await axios.get(
      `/webApi/user01/${userCd.value}/hire-date-history`
    );
    list.value = Array.isArray(res.data?.list) ? res.data.list : [];
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "변경 이력 조회 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

// =========================== Life Cycle ===========================
onMounted(fnLoad);
</script>

<style scoped>
.hire-hist-pop {
  width: 90%;
  /* PRAFTA-WEB_002-T1-05(1.5): 처리방식/적용/사유 컬럼 값 개행 방지 위해 가로폭 약 30% 확대(790→1030px). */
  max-width: 1030px;
  max-height: 88vh;
}

.hire-hist-pop__body {
  padding: 1.25rem 1.5rem;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  font-family: "Pretendard", sans-serif;
  color: var(--color-text, #374151);
}

/* 요약 영역 */
.hire-hist-pop__summary {
  margin-bottom: 1rem;
  padding: 0.75rem 0.875rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  line-height: 1.5;
}
.hire-hist-pop__target {
  font-size: 0.875rem;
  color: var(--color-text-strong, #111827);
  margin: 0 0 0.25rem;
}
.hire-hist-pop__target strong {
  font-weight: 600;
}
.hire-hist-pop__current {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  margin: 0;
}
.hire-hist-pop__current strong {
  color: var(--color-primary, #16a34a);
  font-weight: 700;
  margin: 0 0.125rem;
}
.hire-hist-pop__current-sub {
  color: var(--color-text-muted, #4b5563);
  font-size: 0.75rem;
}

/* 로딩/빈 상태 */
.hire-hist-pop__state {
  padding: 2rem 0.75rem;
  text-align: center;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

/* 테이블 */
.hire-hist-pop__table-wrap {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  overflow: hidden;
}
.hire-hist-pop__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}
.hire-hist-pop__table thead th {
  background: var(--color-bg, #f3f4f6);
  color: var(--color-text-strong, #111827);
  font-weight: 600;
  text-align: left;
  padding: 0.5rem 0.625rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  white-space: nowrap;
}
.hire-hist-pop__table tbody td {
  padding: 0.5rem 0.625rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
  vertical-align: middle;
}
.hire-hist-pop__table tbody tr:last-child td {
  border-bottom: none;
}
/* 최근(마지막) 변경 행 강조 */
.hire-hist-pop__table tbody tr.is-latest td {
  background: rgba(22, 163, 74, 0.06);
}

.hh-nowrap {
  white-space: nowrap;
}
.hh-prev {
  color: var(--color-text-muted, #4b5563);
}
.hh-arrow {
  margin: 0 0.375rem;
  color: var(--color-text-muted, #9ca3af);
}
.hh-new {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}
.hh-muted {
  color: var(--color-text-muted, #9ca3af);
}
.hh-applied-date {
  display: block;
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
  margin-top: 0.125rem;
}

/* 배지 */
.hire-hist-pop__badge {
  display: inline-block;
  font-size: 0.625rem;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: var(--btn-radius, 8px);
  margin-right: 0.25rem;
}
.hire-hist-pop__badge--latest {
  background: rgba(22, 163, 74, 0.12);
  color: var(--color-primary, #16a34a);
}

.hire-hist-pop__applied {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--btn-radius, 8px);
}
.hire-hist-pop__applied.is-applied {
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-primary, #16a34a);
}
.hire-hist-pop__applied.is-pending {
  background: var(--color-bg, #f3f4f6);
  color: var(--color-text-muted, #4b5563);
}

.modal-footer {
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}

/* 사유 "보기" 버튼 (Attd_07 hist-reason-btn 패턴) */
.hist-reason-btn {
  background: #fff;
  border: 1px solid var(--color-border-strong, #d1d5db);
  color: var(--color-text, #374151);
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: "Pretendard", sans-serif;
}
.hist-reason-btn:hover {
  background: var(--color-bg, #f9fafb);
  border-color: #9ca3af;
}

/* ── 사유 보기 팝업 (읽기 전용, Attd_07 del-pop 패턴 차용) ── */
.del-pop-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  /* 프레임워크 nested overlay와 동일 레이어 */
  z-index: 10001;
  font-family: "Pretendard", sans-serif;
}
/* 본 팝업(입사일 변경 이력)은 중첩 모달이라 그 위에 한 단계 더 떠야 한다 */
.reason-pop-backdrop {
  z-index: 10002;
}
.del-pop {
  background: #fff;
  border-radius: 12px;
  width: 440px;
  max-width: calc(100% - 32px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
}
.del-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.del-pop-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: var(--color-text-strong, #111827);
}
.del-pop-close {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  border-radius: 6px;
}
.del-pop-close:hover {
  background: var(--color-bg, #f3f4f6);
  color: var(--color-text-strong, #111827);
}
.del-pop-body {
  padding: 18px;
}
.reason-view {
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 90px;
  max-height: 50vh;
  overflow-y: auto;
  padding: 12px 14px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  background: #fafafa;
  color: var(--color-text-strong, #111827);
  font-size: 13.5px;
  line-height: 1.6;
}
.del-pop-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 18px;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: #fafafa;
  border-radius: 0 0 12px 12px;
}
.del-pop-foot .btn-cancel {
  height: 36px;
  padding: 0 16px;
  border-radius: 6px;
  border: 1px solid var(--color-border-strong, #d1d5db);
  background: #fff;
  color: var(--color-text, #374151);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
}
.del-pop-foot .btn-cancel:hover {
  background: var(--color-bg, #f9fafb);
}
</style>
