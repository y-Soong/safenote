<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content partial-leave-modal">
        <!-- ============ 헤더 ============ -->
        <div class="modal-header">
          <span>부분 휴가 상세</span>
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
        <div class="modal-body plp-body">
          <!-- 대상 직원 / 일자 요약 -->
          <div class="plp-summary">
            <span class="plp-summary-name">{{ userNm }}</span>
            <span class="plp-summary-date">{{ dateText }}</span>
            <span class="plp-summary-count">총 {{ leaves.length }}건</span>
          </div>

          <!-- 부분 휴가 목록 (반차/시간차) -->
          <div class="plp-table-wrap">
            <table class="plp-table">
              <colgroup>
                <col style="width: 22%" />
                <col style="width: 28%" />
                <col style="width: 30%" />
                <col style="width: 20%" />
              </colgroup>
              <thead>
                <tr>
                  <th>휴가 종류</th>
                  <th>사용 단위</th>
                  <th>시간대</th>
                  <th class="is-right">사용 시간</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(lv, idx) in leaves" :key="idx">
                  <td>{{ lv.leaveNm || "-" }}</td>
                  <td>{{ lv.useUnitNm || "-" }}</td>
                  <td>{{ formatRange(lv.startTime, lv.endTime) }}</td>
                  <td class="is-right">{{ formatMinutes(lv.leaveMinutes) }}</td>
                </tr>
                <tr v-if="leaves.length === 0">
                  <td colspan="4" class="plp-empty">부분 휴가가 없습니다.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- ============ 푸터 ============ -->
        <div class="modal-footer">
          <button class="btn btn-second" type="button" @click="fnClose">
            닫기
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from "vue";
import { formatYmdDot } from "@/utils/dateFormat";

// 부모(Attd_05)가 해당 셀의 부분 휴가(반차/시간차) 목록과 식별 정보를 그대로 넘긴다.
//   각 항목: { leaveNm, useUnitNm, useUnitType, startTime(HHMM|null), endTime(HHMM|null), leaveMinutes }.
const props = defineProps({
  userNm: { type: String, default: "" },
  workYmd: { type: String, default: "" },
  leaves: { type: Array, default: () => [] },
});
const emit = defineEmits(["close"]);

// YYYYMMDD → "YYYY.MM.DD" 표기(dateFormat 단일 출처). 형식 미충족이면 원문.
const dateText = computed(() => {
  const s = String(props.workYmd || "");
  return s.length === 8 ? formatYmdDot(s) : s;
});

// HHMM(4자리) → "HH:MM". 시각 미보유(반차 등 null)면 빈 문자열.
const formatHm = (hhmm) =>
  hhmm && String(hhmm).length === 4
    ? `${String(hhmm).slice(0, 2)}:${String(hhmm).slice(2)}`
    : "";

// 시작~종료 시각 범위 문자열. 둘 다 있으면 "HH:MM~HH:MM", 없으면 "-".
const formatRange = (s, e) => {
  const a = formatHm(s);
  const b = formatHm(e);
  return a && b ? `${a}~${b}` : "-";
};

// 사용 분 → "N분"(0/결측이면 "-").
const formatMinutes = (min) => {
  const n = Number(min);
  return Number.isFinite(n) && n > 0 ? `${n}분` : "-";
};

const fnClose = () => {
  emit("close");
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.partial-leave-modal {
  width: 100%;
  max-width: 540px;
}

/* 헤더/푸터/본문 패딩은 공용 가이드(modal-popup-guide.css)의 표준값을 그대로 따른다
   (다른 팝업 LeaveDetailPop 등과 동일: 헤더 52px·본문 20px·푸터 60px, 좌우 0 20px).
   본문은 요약 + 목록 테이블을 세로로 쌓기 위한 플렉스 컬럼만 지정한다. */
.partial-leave-modal .plp-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ===== 요약(직원/일자/건수) ===== */
.plp-summary {
  display: flex;
  align-items: baseline;
  gap: 0.625rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.plp-summary-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.plp-summary-date {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.plp-summary-count {
  margin-left: auto;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-primary);
}

/* ===== 목록 테이블 ===== */
.plp-table-wrap {
  overflow-x: auto;
}

.plp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
  table-layout: fixed;
  border-top: 2px solid var(--color-primary);
}

.plp-table thead {
  background: rgba(22, 163, 74, 0.06);
}

.plp-table th {
  text-align: left;
  padding: 0.5rem 0.625rem;
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.plp-table th.is-right {
  text-align: right;
}

.plp-table td {
  padding: 0.625rem;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
  color: var(--color-text);
  word-break: keep-all;
}

.plp-table td.is-right {
  text-align: right;
  font-weight: 600;
  color: var(--color-text-strong);
}

.plp-empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 1.5rem 0.625rem;
}
</style>
