<!--
  SafetyInspectContextCard.vue — 체크포인트 컨텍스트 카드 (prafta-app-011 화면 B)
  - 좌측 아이콘 + 체크포인트명 + 메타({타입명} · {사업장명} · {작업일자}).
  - 디자인 토큰은 부모(.chk-view) 루트에서 상속.
-->
<template>
  <div class="ctx">
    <div class="ctx-row">
      <div class="ctx-ico" aria-hidden="true">
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M9 17h6M5 17H3V5a1 1 0 0 1 1-1h11v13h-2" />
          <path d="M15 9h4l3 4v4h-2" />
          <circle cx="7.5" cy="17.5" r="2.5" />
          <circle cx="17.5" cy="17.5" r="2.5" />
        </svg>
      </div>
      <div class="ctx-b">
        <p class="ctx-tt">{{ chkptName || '안전점검' }}</p>
        <p class="ctx-mt">{{ metaText }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

import { formatYmdDisplay } from '@/utils/approvalFormat'

const props = defineProps({
  // 체크포인트명 (CHKPT_NM)
  chkptName: { type: String, default: '' },
  // 체크리스트 타입명 (마스터 부재 시 빈 값 → 메타에서 생략)
  chklstTypeName: { type: String, default: '' },
  // 사업장명 (SITE_NM)
  siteName: { type: String, default: '' },
  // 작업일자 (YYYYMMDD)
  workDate: { type: String, default: '' },
})

// YYYYMMDD → YYYY.MM.DD (D1 점 통일, 표시 단일 출처 위임)
const formattedDate = computed(() => formatYmdDisplay(props.workDate))

// 메타: 존재하는 값만 ' · ' 로 연결 (graceful 폴백)
const metaText = computed(() => {
  return [props.chklstTypeName, props.siteName, formattedDate.value].filter(Boolean).join(' · ')
})
</script>

<style scoped>
.ctx {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 14px 16px;
}
.ctx-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ctx-ico {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--color-primary-tint);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ctx-b {
  flex: 1;
  min-width: 0;
}
.ctx-tt {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.3;
}
.ctx-mt {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--color-text-secondary);
}
</style>
