<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>교대 스케줄 저장 결과</span>
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
        <div class="viewBody">
          <!-- 필수 안내 문구 -->
          <p class="shift-notice-guide">
            교대근무에 소속된 사용자들의 기존 연차는 변경되지 않으니 수정이
            필요한지 확인해주세요.
          </p>
          <p v-if="hasOt" class="shift-notice-guide shift-notice-guide-sub">
            초과근무가 등록된 날도 기존 스케줄을 유지하니 함께 확인해주세요.
          </p>

          <!-- 필터 -->
          <div class="shift-notice-filters">
            <button
              v-for="opt in filterOptions"
              :key="opt.key"
              type="button"
              class="shift-notice-filter-btn"
              :class="{ active: activeFilter === opt.key }"
              @click="activeFilter = opt.key"
            >
              {{ opt.label }}
              <span class="shift-notice-filter-count">{{ opt.count }}</span>
            </button>
          </div>

          <!-- 목록 -->
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th width="22%">일자</th>
                  <th width="22%">사용자</th>
                  <th>구분</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!filteredRows || filteredRows.length === 0">
                  <tr>
                    <td colspan="3" class="edu-grid-empty">
                      해당하는 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(row, idx) in filteredRows" :key="idx">
                    <td>{{ fmtYmdDisp(row.workYmd) }}</td>
                    <td>{{ row.userNm }}</td>
                    <td class="cell-wrap">{{ buildReasonLabel(row) }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <div class="btn-group" style="margin-top: 1rem">
            <button class="btn btn-second" @click="$emit('close')">확인</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, ref } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

/**
 * prafta-com-016-D-4: 교대 스케줄 저장(신규/조원추가/기간연장) 시 연차·OT 가 있어
 *   덮어쓰기에서 제외(보존)된 (사용자, 날짜) 목록을 안내하는 팝업.
 *
 * <p>BE 응답 blockedList 의 각 항목은 reason(LEAVE/OT), dayType(WORK/OFF),
 *   leaveUseUnitType(연차 사용단위 코드) 을 가진다. 사용자명은 호출처가 userNm 으로 채워 넘긴다.
 *
 * <p>필터: 전체 / 연차-근로일 / 연차-휴무일 / 초과근무(D-Q3).
 */
const props = defineProps({
  // blockedList 항목: { userCd, userNm, workYmd, reason, dayType, leaveUseUnitType }
  rows: { type: Array, default: () => [] },
});

defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const activeFilter = ref("ALL");

// 사용단위 코드 → 표시 라벨(웹 LeaveApplyPop 정의와 동일: 00=종일/01=반차/02=2시간/03=1시간/04=30분).
const USE_UNIT_LABEL = {
  "00": "종일",
  "01": "반차",
  "02": "2시간차",
  "03": "1시간차",
  "04": "30분차",
};

const fmtYmdDisp = (ymd) =>
  ymd && ymd.length === 8
    ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
    : ymd;

// reason/dayType 분류 키: LEAVE_WORK / LEAVE_OFF / OT
const classifyKey = (row) => {
  if (row.reason === "OT") return "OT";
  return row.dayType === "OFF" ? "LEAVE_OFF" : "LEAVE_WORK";
};

const hasOt = computed(() => (props.rows ?? []).some((r) => r.reason === "OT"));

const countOf = (key) =>
  (props.rows ?? []).filter((r) => classifyKey(r) === key).length;

const filterOptions = computed(() => [
  { key: "ALL", label: "전체", count: (props.rows ?? []).length },
  { key: "LEAVE_WORK", label: "연차(근로일)", count: countOf("LEAVE_WORK") },
  { key: "LEAVE_OFF", label: "연차(휴무일)", count: countOf("LEAVE_OFF") },
  { key: "OT", label: "초과근무", count: countOf("OT") },
]);

const filteredRows = computed(() => {
  const list = props.rows ?? [];
  if (activeFilter.value === "ALL") return list;
  return list.filter((r) => classifyKey(r) === activeFilter.value);
});

// 구분 라벨: 연차면 "연차 {사용단위} / {근로일|휴무일}", OT 면 "초과근무 등록일".
const buildReasonLabel = (row) => {
  if (row.reason === "OT") {
    return "초과근무 등록일";
  }
  const unit = USE_UNIT_LABEL[row.leaveUseUnitType] ?? "연차";
  const dayLabel = row.dayType === "OFF" ? "휴무일" : "근로일";
  return `연차 ${unit} / ${dayLabel}`;
};
</script>

<style scoped>
.shift-notice-guide {
  font-size: 0.9rem;
  line-height: 1.5;
  color: var(--color-text-strong, #111827);
  margin: 0 0 0.5rem 0;
}
.shift-notice-guide-sub {
  font-size: 0.825rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0 0 0.75rem 0;
}

.shift-notice-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin: 0 0 0.75rem 0;
}
.shift-notice-filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.7rem;
  font-size: 0.8rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 9999px;
  background: var(--color-surface, #fff);
  color: var(--color-text-muted, #4b5563);
  cursor: pointer;
}
.shift-notice-filter-btn.active {
  background: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
  color: #fff;
}
.shift-notice-filter-count {
  font-weight: 700;
}
</style>
