<!--
  AdminApprovalNeighborSegments.vue — 앞뒤 근무일(D-1 / D+1) 근태 구간 표시 (표시 전용)
  - 연결 작업: PRAFTA-겹침가드-07 / UI 명세: plan §3-2
  - 부모: AdminApprovalDetailView.vue (③ 요청 내용 아래 · CORRECTION 그룹 한정)
  - 데이터: GET /appApi/admin/approval/detail → body.neighborSegments (PRAFTA-겹침가드-05)
  - 목적: 근태보정 승인이 이웃 근무일 미마감 때문에 막히는 원인을 관리자가 화면에서 확인(정책서 attd §7.6)
  - 참조 패턴: 동일 폴더 AdminApprovalAdjustSheet.vue (자식이 루트에서 토큰 자급 선언) + 부모 .ap-sec 카드
  - 표시 문자열(dayLabel / seqLabel / checkInText / checkOutText)과 상태(status)는 모두
    서버 완성값이다. 이 컴포넌트에서 날짜·시각을 재포맷하거나 상태를 재판정하지 않는다.
  - 배지 규약(plan §0-3 D-1 확정): CLOSED=배지 없음 / OPEN=`퇴근 미입력` / CORRUPT=`퇴근 확인 필요`.
    CORRUPT 는 오류가 아니라 보정 대기 중인 정상 기록이므로 danger 가 아닌 warning 톤을 쓴다.
-->
<template>
  <section class="nbs">
    <h2 class="nbs__title">앞뒤 근무일 근태</h2>

    <ul class="nbs__list">
      <li
        v-for="seg in segments"
        :key="seg.workYmd + '-' + seg.workSeq"
        class="nbs__item"
      >
        <div class="nbs__item-head">
          <span class="nbs__day">{{ seg.dayLabel }}</span>
          <span class="nbs__seq">{{ seg.seqLabel }}</span>
        </div>
        <div class="nbs__item-body">
          <span class="nbs__time">
            {{ seg.checkInText }} ~ {{ seg.checkOutText || "—" }}
          </span>
          <span
            v-if="seg.status === 'OPEN'"
            class="nbs__badge nbs__badge--open"
            >퇴근 미입력</span
          >
          <span
            v-else-if="seg.status === 'CORRUPT'"
            class="nbs__badge nbs__badge--corrupt"
            >퇴근 확인 필요</span
          >
        </div>
      </li>
    </ul>

    <p v-if="hasAbnormal" class="nbs__hint">
      퇴근이 기록되지 않았거나 퇴근 시각 확인이 필요한 앞뒤 근무일이 있어요. 웹
      근태관리에서 해당 날짜의 근태도 함께 정정해 주세요.
    </p>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 서버 완성 표시값 배열
  //   [{ workYmd, dayLabel, workSeq, seqLabel, checkInText, checkOutText, status }]
  //   status: 'CLOSED' | 'OPEN' | 'CORRUPT'  (서버 단일 출처 — 프론트 재판정 금지)
  segments: { type: Array, default: () => [] },
})

// 안내문 노출 여부 — 서버가 내려준 status 값의 단순 존재 확인(표시 분기 전용).
//   서버 응답 item 스펙에 별도 요약 플래그는 없으므로 목록에서 파생한다.
//   status 자체를 재계산하지 않으므로 "프론트 재판정 금지" 규약에 어긋나지 않는다.
const hasAbnormal = computed(() =>
  props.segments.some((s) => s.status === 'OPEN' || s.status === 'CORRUPT'),
)
</script>

<style scoped>
/* 부모(AdminApprovalDetailView)의 .ap-sec 카드 규격을 복제한다.
   이 화면 계열은 자식 컴포넌트가 토큰을 자급 선언하는 규약이다
   (AdminApprovalAdjustSheet.vue 동일). */
.nbs {
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --radius-full: 9999px;
  --radius-lg: 14px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;

  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
}

.nbs__title {
  margin: 0 0 var(--space-sm);
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.nbs__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.nbs__item {
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.nbs__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.nbs__item-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
}

.nbs__day {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.nbs__seq {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.nbs__item-body {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.nbs__time {
  font-size: 14px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.nbs__badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.nbs__badge--open {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}

/* CORRUPT = 오류가 아니라 "퇴근 확인이 필요한" 보정 대기 상태(plan §0-3 D-1).
   OPEN 과 같은 warning 톤을 쓰되, 두 배지를 구분하기 위해 채움 대신 외곽선으로 처리한다. */
.nbs__badge--corrupt {
  background: var(--color-surface);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-text);
}

.nbs__hint {
  margin: var(--space-md) 0 0;
  padding-top: var(--space-sm);
  border-top: 1px dashed var(--color-border);
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}
</style>
