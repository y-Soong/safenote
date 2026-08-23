<!--
  AttendanceDayDetailCard.vue — 선택일 상세 카드 (오늘/이번달 공용)
  - 작업 ID: APP002-09 (UI 명세: UI-A005)
  - 시안 화면 9·10 하단 카드 = 화면 1~5 상단 카드와 동일 본체.
  - 본체는 AttendanceTodayCard 를 그대로 재사용한다.
  - prafta-app-013(결정 §3): 종전 하단 빠른 액션 2버튼(근태 보정 / 초과근무)을 제거.
    "수정 요청"(본체 버튼)이 4액션 시트를 열어 통일한다. 시트 4행이 개별 게이팅 담당.
  - 정책: APP002-06 동일 + attd §9.2/§9.3/§9.4/§11.2 (게이팅은 서버 sheetActions 표시만)
-->
<template>
  <div v-if="detail" class="day-detail">
    <!-- 본체: 오늘 카드 컴포넌트 재사용 (3행 정보 구조 동일).
         본체의 "수정 요청" emit('action', {type:'requestModify'}) 가 그대로 상위로 전달되어
         MyAttendanceView.onDayDetailAction 에서 4액션 시트를 연다. -->
    <AttendanceTodayCard
      :detail="detail"
      :current-site-cd="currentSiteCd"
      @action="emit('action', $event)"
    />
  </div>
</template>

<script setup>
import AttendanceTodayCard from './AttendanceTodayCard.vue'

defineProps({
  // GET /api/app/attd/my/day-detail 응답 1건 (today 응답과 동일 구조). null=미선택/로딩
  detail: {
    type: Object,
    default: null,
  },
  // 작업지시서_소속이동-이력가시성-보정 T3: MyAttendanceView → 본 컴포넌트 → AttendanceTodayCard 그대로 전달.
  currentSiteCd: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['action'])
</script>

<style scoped>
.day-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
</style>
