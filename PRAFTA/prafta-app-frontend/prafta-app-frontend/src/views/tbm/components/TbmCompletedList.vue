<!--
  TbmCompletedList.vue — 교육완료(내 참여 이력: 이수/미이수) 세션 카드 리스트
  - 작업 ID: PRAFTA-TBM-LIST-DONE (분해: prafta-app-tbm-user-detail-plan.md §4 F3, §3 A3)
  - 백엔드: GET /appApi/tbm/sessions?tab=COMPLETED (내 출결 존재 + 세션 종료/내 EXIT 존재)
  - 카드 클릭 → 부모로 select emit (완료 상세 /TbmCompletedDetail 라우팅은 부모/developer 담당).
  - 디자인 토큰은 부모(.tbm-hub-view)에서 상속.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
-->
<template>
  <div class="tbm-list">
    <p v-if="isLoading" class="tbm-list__state">불러오는 중…</p>

    <div v-else-if="loadError" class="tbm-list__state">
      <p class="tbm-list__state-msg">목록을 불러오지 못했어요.</p>
      <button type="button" class="tbm-list__retry" @click="onRetry">다시 시도</button>
    </div>

    <p v-else-if="!sessions.length" class="tbm-list__state tbm-list__state--empty">
      완료한 TBM 이력이 없어요
    </p>

    <template v-else>
      <TbmSessionCard
        v-for="s in sessions"
        :key="s.sessionCd"
        variant="COMPLETED"
        :session="s"
        @select="$emit('select', s)"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import TbmSessionCard from './TbmSessionCard.vue'

defineEmits(['select'])

// ── 반응형 상태(developer: 조회 결과로 채움) ──────────────────────
const isLoading = ref(false)
const loadError = ref(false)
// 카드 항목: [{ sessionCd, title, managerUserNm, openedAt, startedAt, endedAt, completionStatusCd }]
const sessions = ref([])

// 교육완료(내 출결 존재 + 세션 종료/내 EXIT 존재) 세션 조회 — GET /appApi/tbm/sessions?tab=COMPLETED (A3)
const loadSessions = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await api.get('/appApi/tbm/sessions', { params: { tab: 'COMPLETED' } })
    sessions.value = Array.isArray(data?.sessions) ? data.sessions : []
  } catch (e) {
    console.error('[TbmCompletedList] 목록 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

const onRetry = () => {
  loadSessions()
}

onMounted(loadSessions)
</script>

<style scoped>
.tbm-list {
  display: flex;
  flex-direction: column;
}
.tbm-list__state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.tbm-list__state--empty {
  padding: var(--space-lg) 0;
}
.tbm-list__state-msg {
  margin: 0 0 var(--space-sm);
}
.tbm-list__retry {
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
</style>
