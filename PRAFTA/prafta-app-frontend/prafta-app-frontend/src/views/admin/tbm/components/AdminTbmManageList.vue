<!--
  AdminTbmManageList.vue — 탭1 TBM 교육 관리 리스트 (임시저장/개설/진행중 세션)
  - 작업 ID: 001-P5-T-F2 (분해: 001-phase5-admin-tbm-plan.md §2-1, §3 T-A1)
  - 백엔드: GET /appApi/admin/tbm/sessions?tab=MANAGE&statusCd=&keyword=&page=&pageSize= (T-A1)
      식별자(회사/사용자/사업장)·노드 스코프는 서버(토큰+scopedNodeCds). 프론트는 필터 값만 전송.
  - 표시(요청서): 상태 / 교육 제목 / 출결·이수·미이수. 카드 클릭 → 부모(AdminTbmView)로 select emit.
  - 상태별 동작: loading / error(재시도) / empty / success.
  - 디자인 토큰은 부모(.admin-tbm-view)에서 상속.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격 + UI 필터 토글만(조회 로직은 developer).
-->
<template>
  <div class="admin-tbm-mlist">
    <!-- 상태 필터 칩(UI 토글 — 선택 시 재조회는 developer) -->
    <div class="admin-tbm-mlist__filter" role="tablist" aria-label="상태 필터">
      <button
        v-for="f in statusFilters"
        :key="f.value"
        type="button"
        class="filter-chip"
        :class="{ 'is-active': statusCd === f.value }"
        @click="onChangeStatus(f.value)"
      >
        {{ f.label }}
      </button>
    </div>

    <!-- loading -->
    <p v-if="isLoading" class="admin-tbm-mlist__state">불러오는 중…</p>

    <!-- error -->
    <div v-else-if="loadError" class="admin-tbm-mlist__state">
      <p class="admin-tbm-mlist__state-msg">목록을 불러오지 못했어요.</p>
      <button type="button" class="admin-tbm-mlist__retry" @click="onRetry">다시 시도</button>
    </div>

    <!-- empty -->
    <p v-else-if="!sessions.length" class="admin-tbm-mlist__state admin-tbm-mlist__state--empty">
      조회된 TBM 교육이 없어요
    </p>

    <!-- success -->
    <template v-else>
      <AdminTbmSessionCard
        v-for="s in sessions"
        :key="s.sessionCd"
        :session="s"
        @select="$emit('select', s)"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import AdminTbmSessionCard from './AdminTbmSessionCard.vue'

defineEmits(['select'])

// 상태 필터(UI). MANAGE 탭은 작성중/개설/진행중을 다룬다(종료/취소는 이력 탭).
const statusFilters = [
  { value: '', label: '전체' },
  { value: 'DRAFT', label: '작성중' },
  { value: 'OPENED', label: '개설' },
  { value: 'IN_PROGRESS', label: '진행중' },
]

// ── 반응형 상태 ───────────────────────────────────────────────────
const isLoading = ref(false)
const loadError = ref(false)
const statusCd = ref('') // 선택된 상태 필터
// 카드 항목: [{ sessionCd, statusCd, statusNm, title, attendanceCount, completedCount, notCompletedCount, managerUserNm, openedAt }]
const sessions = ref([])

// 교육관리 세션 조회 — GET /appApi/admin/tbm/sessions (T-A1).
//   식별자/노드 스코프는 서버(토큰)가 산출하므로 클라이언트는 상태/검색 필터만 전달한다(C1).
const loadSessions = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const params = {}
    if (statusCd.value) params.statusCd = statusCd.value
    const { data } = await api.get('/appApi/admin/tbm/sessions', { params })
    sessions.value = Array.isArray(data?.sessions) ? data.sessions : []
  } catch (e) {
    console.error('[AdminTbmManageList] 목록 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 상태 필터 변경(UI 토글) → 재조회
const onChangeStatus = (value) => {
  if (statusCd.value === value) return
  statusCd.value = value
  loadSessions()
}

const onRetry = () => {
  loadSessions()
}

onMounted(loadSessions)
</script>

<style scoped>
.admin-tbm-mlist {
  display: flex;
  flex-direction: column;
}

/* 상태 필터 칩 */
.admin-tbm-mlist__filter {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}
.filter-chip {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.filter-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}

/* 상태 메시지 */
.admin-tbm-mlist__state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-mlist__state--empty {
  padding: var(--space-lg) 0;
}
.admin-tbm-mlist__state-msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-mlist__retry {
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
