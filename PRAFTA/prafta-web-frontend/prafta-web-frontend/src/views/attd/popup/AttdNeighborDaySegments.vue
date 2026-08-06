<!--
  AttdNeighborDaySegments.vue — 앞뒤 근무일(D-1 / D+1) 근태 구간 표시 (표시 전용)
  - 연결 작업: PRAFTA-겹침가드-06 / UI 명세: plan §3-1
  - 부모: AttdDayDetailPop.vue 좌측 패널 (.work-notice 아래 / .req-section 위)
  - 데이터: GET /webApi/attd07/daily-attd-details → neighborAttdSegmentList (PRAFTA-겹침가드-05)
  - 목적: 겹침 가드(정책서 attd §7.6)가 이웃 근무일의 미마감 근태 때문에 발동할 때,
          관리자가 화면 이동 없이 원인 행을 특정할 수 있게 한다(2026-08-04 운영 실증 대응).
  - 참조 패턴: 동일 폴더 AttdGpsCoordPanel.vue (props-only 표시 컴포넌트 · CSS 변수 + rem)
  - 표시 문자열(dayLabel / seqLabel / checkInText / checkOutText)과 상태(status)는 모두
    서버 완성값이다. 이 컴포넌트에서 날짜·시각을 재포맷하거나 상태를 재판정하지 않는다.
  - 배지 규약(plan §0-3 D-1 확정): CLOSED=배지 없음 / OPEN=`퇴근 미입력` / CORRUPT=`퇴근 확인 필요`.
    CORRUPT 는 오류가 아니라 보정 대기 중인 정상 기록이므로 danger 가 아닌 warning 톤을 쓴다.
-->
<template>
  <div class="nb-seg">
    <div class="nb-seg__head">
      <h3 class="nb-seg__title">앞뒤 근무일 근태</h3>
      <span class="nb-seg__count">({{ segments.length }})</span>
    </div>

    <!-- loading: 부모 팝업이 전체를 덮으므로 방어용 -->
    <div v-if="loading" class="nb-seg__state">조회 중…</div>

    <div v-else class="nb-seg__list">
      <div
        v-for="seg in segments"
        :key="seg.workYmd + '-' + seg.workSeq"
        class="nb-seg__row"
      >
        <span class="nb-seg__day">{{ seg.dayLabel }}</span>
        <span class="nb-seg__seq">{{ seg.seqLabel }}</span>
        <span class="nb-seg__time">
          {{ seg.checkInText }} ~ {{ seg.checkOutText || "—" }}
        </span>
        <span
          v-if="seg.status === 'OPEN'"
          class="nb-seg__badge nb-seg__badge--open"
          >퇴근 미입력</span
        >
        <span
          v-else-if="seg.status === 'CORRUPT'"
          class="nb-seg__badge nb-seg__badge--corrupt"
          >퇴근 확인 필요</span
        >
        <span v-else class="nb-seg__badge-empty" aria-hidden="true"></span>
      </div>
    </div>

    <p v-if="!loading && hasAbnormal" class="nb-seg__hint">
      퇴근이 기록되지 않았거나 퇴근 시각 확인이 필요한 앞뒤 근무일이 있습니다.
      해당 날짜의 근태도 함께 정정해 주세요.
    </p>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  // 서버 완성 표시값 배열
  //   [{ workYmd, dayLabel, workSeq, seqLabel, checkInText, checkOutText, status }]
  //   status: 'CLOSED' | 'OPEN' | 'CORRUPT'  (서버 단일 출처 — 프론트 재판정 금지)
  segments: { type: Array, default: () => [] },
  // 부모의 일자상세 조회 로딩 상태
  loading: { type: Boolean, default: false },
});

// 안내문 노출 여부 — 서버가 내려준 status 값의 단순 존재 확인(표시 분기 전용).
//   서버 응답 item 스펙(NeighborAttdSegmentView)에 별도 요약 플래그는 없으므로 목록에서 파생한다.
//   status 자체를 재계산하지 않으므로 "프론트 재판정 금지" 규약에 어긋나지 않는다.
const hasAbnormal = computed(() =>
  props.segments.some((s) => s.status === "OPEN" || s.status === "CORRUPT")
);
</script>

<style scoped>
/* 부모(AttdDayDetailPop) 좌측 패널의 .req-section / .time-card 톤을 따른다.
   웹 토큰 파일에는 spacing 토큰이 없으므로 색상·radius 만 CSS 변수, 간격은 rem
   (동일 폴더 AttdGpsCoordPanel.vue 와 같은 규약). */
.nb-seg {
  margin-bottom: 0.875rem;
  padding: 0.75rem 0.875rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.nb-seg__head {
  display: flex;
  align-items: baseline;
  gap: 0.375rem;
  margin-bottom: 0.5rem;
}

.nb-seg__title {
  margin: 0;
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

.nb-seg__count {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.nb-seg__state {
  padding: 0.5rem 0;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.nb-seg__list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nb-seg__row {
  display: grid;
  grid-template-columns: 6.5rem 2.5rem 1fr auto;
  align-items: center;
  gap: 0.375rem;
  padding: 0.3rem 0;
  font-size: 0.8rem;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
}

.nb-seg__row:last-child {
  border-bottom: none;
}

.nb-seg__day {
  font-weight: 600;
  color: var(--color-text-strong);
  white-space: nowrap;
}

.nb-seg__seq {
  color: var(--color-text-muted);
  white-space: nowrap;
}

.nb-seg__time {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.nb-seg__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.05rem 0.5rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 600;
  white-space: nowrap;
}

.nb-seg__badge--open {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

/* CORRUPT = 오류가 아니라 "퇴근 확인이 필요한" 보정 대기 상태(plan §0-3 D-1).
   OPEN 과 같은 warning 톤을 쓰되, 두 배지를 구분하기 위해 채움 대신 외곽선으로 처리한다. */
.nb-seg__badge--corrupt {
  background: var(--color-surface);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-text);
}

.nb-seg__badge-empty {
  display: inline-block;
  width: 0;
}

.nb-seg__hint {
  margin: 0.5rem 0 0;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border-strong);
  font-size: 0.74rem;
  line-height: 1.5;
  color: var(--color-text-muted);
}
</style>
