<!--
  AdminEmployeeStatusView.vue — 관리자 모드 "직원 관리" (실시간 근태 현황 + 외근 위치)
  - 작업 ID: PRAFTA-004 (UI 명세: UI-001, 요청서: 작업지시서_관리자앱-직원관리-신규화면.md)
  - 진입: AdminLauncherView 의 EMPLOYEE_STATUS 섹션 → /AdminEmployeeStatus?siteCd={currentSiteCd}(보호 라우트)
      진입 게이팅은 서버 access-context.moduleActiveMap.EMPLOYEE_STATUS(PRAFTA-001). 본 화면에 역할(AUTH_CD) 분기 없음.
  - 참조 패턴: AdminSelfJoinView.vue(부서 칩/스코프 로딩 셸) + AdminAttdDailyList.vue(날짜 네비)
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만(API 호출·라우팅 로직은 developer).
  - ⚠️ C1 원칙: 화면 진입 게이팅은 서버 산출(moduleActiveMap)만 신뢰. 화면 내부에는 역할 분기 없음.
  - 디자인 토큰: 1회 선언(AdminSelfJoinView/.admin-selfjoin-view 세트 이식) — 자식 scoped 가 상속.
-->
<template>
  <div class="ees-view">
    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="ees-hd">
      <button type="button" class="ees-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-ees-chev-left" />
        </svg>
      </button>
      <h1 class="ees-hd__title">직원 관리</h1>
      <span class="ees-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 날짜 네비 -->
    <div class="ees-datenav">
      <button type="button" class="ees-datenav__btn" aria-label="이전 날짜" @click="shiftDate(-1)">
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-ees-chev-l" />
        </svg>
      </button>
      <div class="ees-datenav__center">
        <span class="ees-datenav__label">{{ dateLabel }}</span>
        <button v-if="!isToday" type="button" class="ees-datenav__today" @click="goToday">
          오늘
        </button>
      </div>
      <button
        type="button"
        class="ees-datenav__btn"
        aria-label="다음 날짜"
        :disabled="isToday"
        @click="shiftDate(1)"
      >
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-ees-chev-r" />
        </svg>
      </button>
    </div>

    <!-- 공유 필터: 부서 칩 + 검색 -->
    <div class="ees-filter">
      <div v-if="showNodeChips" class="ees-chips" role="tablist" aria-label="부서 필터">
        <button
          v-for="n in nodeChips"
          :key="n.nodeCd || 'ALL'"
          type="button"
          class="ees-chip"
          :class="{ 'is-active': activeNodeCd === n.nodeCd }"
          :aria-selected="activeNodeCd === n.nodeCd"
          @click="onSelectNode(n)"
        >
          {{ n.nodeNm }}
        </button>
      </div>

      <input
        v-model.trim="keyword"
        type="search"
        class="ees-search"
        placeholder="이름 검색"
        @keyup.enter="onSearch"
      />
    </div>

    <!-- 본문 -->
    <main class="ees-body">
      <p v-if="scopeError" class="ees-state" aria-live="polite">
        부서 정보를 불러오지 못했습니다.
        <button type="button" class="ees-retry" @click="loadScopeNodes">다시 시도</button>
      </p>

      <p v-else-if="noManagedNode" class="ees-state" aria-live="polite">
        관리 중인 부서가 없습니다. 관리자에게 문의해 주세요.
      </p>

      <template v-else-if="scopeLoaded">
        <AdminEmployeeStatusList
          ref="listRef"
          :site-cd="siteCd"
          :node-cd="activeNodeCd"
          :work-ymd="workYmd"
          :keyword="appliedKeyword"
          @select-offsite="onSelectOffsite"
          @reset-filters="onResetFilters"
        />
      </template>
    </main>

    <!-- GPS 지도 시트 -->
    <AdminEmployeeGpsMapSheet
      v-model="gpsSheetOpen"
      :user-nm="gpsTargetUserNm"
      :attd-ids="gpsTargetAttdIds"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="ees-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-ees-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-ees-chev-l"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-ees-chev-r"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay } from '@/utils/approvalFormat'
import { dowShort } from '@/views/attd/attdFormat'

import AdminEmployeeStatusList from './components/AdminEmployeeStatusList.vue'
import AdminEmployeeGpsMapSheet from './components/AdminEmployeeGpsMapSheet.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — AdminSelfJoinView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 현장 컨텍스트(런처가 query 로 전달). 없으면 서버가 토큰 gv_siteCd 로 폴백(AdminSelfJoinView 패턴 동일).
const siteCd = ref(typeof route.query.siteCd === 'string' ? route.query.siteCd : '')

// ── 날짜 상태(YYYYMMDD) — AdminAttdDailyList 패턴 동일 ─────────────────────
const pad2 = (n) => String(n).padStart(2, '0')
const toYmd = (d) => `${d.getFullYear()}${pad2(d.getMonth() + 1)}${pad2(d.getDate())}`
const todayYmd = toYmd(new Date())

const workYmd = ref(todayYmd)
const isToday = computed(() => workYmd.value === todayYmd)
// 표시 통일: 점 일자 + 요일은 AdminAttdDailyList 와 동일하게 approvalFormat/attdFormat 로 산출
const dateLabel = computed(() => {
  const y = workYmd.value
  if (!y || y.length !== 8) return ''
  return `${formatYmdDisplay(y)} (${dowShort(null, y)})`
})

// ── 부서 칩 상태(AdminSelfJoinView 패턴 동일) ───────────────────────────────
const companyWide = ref(false)
const nodeChips = ref([]) // [{ nodeCd: null|'n1', nodeNm: '전체'|'생산1팀' }]
const activeNodeCd = ref(null)
const scopeLoaded = ref(false)
const scopeError = ref(false)

const showNodeChips = computed(() => scopeLoaded.value && nodeChips.value.length > 0)
const noManagedNode = computed(
  () =>
    scopeLoaded.value && !scopeError.value && !companyWide.value && nodeChips.value.length === 0,
)

// ── 검색 ────────────────────────────────────────────────────────────────
const keyword = ref('')
const appliedKeyword = ref('')

// ── 목록 ref(재조회 트리거용) ───────────────────────────────────────────────
const listRef = ref(null)

// ── GPS 시트 상태 ────────────────────────────────────────────────────────
const gpsSheetOpen = ref(false)
const gpsTargetUserNm = ref('')
const gpsTargetAttdIds = ref([])

// 조회 사업장 파라미터 — 비어 있으면 서버가 토큰 사업장으로 폴백하므로 키 자체를 보내지 않는다.
const buildSiteParams = () => (siteCd.value ? { siteCd: siteCd.value } : {})

// ── 스코프 조회 — GET /appApi/admin/employee-status/scope-nodes(PRAFTA-002) ──
const loadScopeNodes = async () => {
  try {
    scopeError.value = false
    const { data } = await api.get('/appApi/admin/employee-status/scope-nodes', {
      params: buildSiteParams(),
    })

    companyWide.value = data?.companyWide === true
    const nodes = Array.isArray(data?.nodes) ? data.nodes : []

    if (companyWide.value) {
      // 전사역할은 부서를 고를 필요가 없다(nodeCd 미전송 = 사업장 전체).
      nodeChips.value = [{ nodeCd: null, nodeNm: '전체' }]
      activeNodeCd.value = null
    } else if (nodes.length > 0) {
      nodeChips.value = nodes.map((n) => ({ nodeCd: n.nodeCd, nodeNm: n.nodeNm || n.nodeCd }))
      activeNodeCd.value = nodeChips.value[0].nodeCd
    } else {
      nodeChips.value = []
      activeNodeCd.value = null
    }
  } catch (e) {
    // 401/403 토큰 에러는 axios 인터셉터가 처리. 그 외는 사유를 알리고 fail-closed 로 둔다.
    console.error('[AdminEmployeeStatusView] 부서 스코프 조회 실패')
    // ★통신 실패를 "관리 부서 없음"(권한 문제)으로 표기하지 않는다(AdminSelfJoinView qa D2 동일 원칙).
    scopeError.value = true
    companyWide.value = false
    nodeChips.value = []
    activeNodeCd.value = null
    showAlert(
      resolveApiErrorMessage(e, '부서 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
  } finally {
    scopeLoaded.value = true
  }
}

// ── 핸들러 ──────────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/AdminHome')
}

// 부서 칩 선택 — 자식 리스트가 nodeCd prop 변화를 watch 해 스스로 재조회한다(AdminSelfJoinView 패턴 동일).
const onSelectNode = (node) => {
  if (!node || activeNodeCd.value === node.nodeCd) return
  activeNodeCd.value = node.nodeCd
}

// 검색 실행(Enter) — 적용값만 갱신하면 자식이 keyword prop 변화로 재조회한다.
const onSearch = () => {
  appliedKeyword.value = keyword.value
}

// 자식 빈 상태의 [검색 해제] — 셸이 소유한 검색어를 비운다(부서 칩은 권한 스코프라 유지).
const onResetFilters = () => {
  keyword.value = ''
  appliedKeyword.value = ''
}

const shiftDate = (deltaDays) => {
  const y = workYmd.value
  const dt = new Date(Number(y.slice(0, 4)), Number(y.slice(4, 6)) - 1, Number(y.slice(6, 8)))
  dt.setDate(dt.getDate() + deltaDays)
  const next = toYmd(dt)
  // 미래 날짜는 막는다(오늘까지).
  if (next > todayYmd) return
  workYmd.value = next
}

const goToday = () => {
  if (isToday.value) return
  workYmd.value = todayYmd
}

const onSelectOffsite = (item) => {
  gpsTargetUserNm.value = item?.userNm || ''
  gpsTargetAttdIds.value = Array.isArray(item?.attdIds) ? item.attdIds : []
  gpsSheetOpen.value = true
}

// 현장 전환 반영 — 런처 헤더에서 현장을 바꾼 뒤 이 화면으로 재진입하면 query.siteCd 가 갱신된다.
//   동일 라우트로의 재진입은 컴포넌트를 리마운트하지 않으므로 watch 로 반영한다(사용자 플로우 §7).
watch(
  () => route.query.siteCd,
  (val) => {
    const next = typeof val === 'string' ? val : ''
    if (next === siteCd.value) return
    siteCd.value = next
    // siteCd prop 변화는 목록(AdminEmployeeStatusList)이 자체 watch 로 재조회하므로,
    //   여기서는 부서 스코프만 재조회한다(스코프가 사업장에 종속되므로 활성 노드 재확정 필요).
    loadScopeNodes()
  },
)

// 진입 시 부서 스코프를 먼저 확정한다(칩이 정해져야 목록 조회에 nodeCd 를 실을 수 있다).
onMounted(loadScopeNodes)
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminSelfJoinView 세트 이식) — 자식 scoped 가 상속 */
.ees-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-disabled-bg: #f3f4f6;
  --color-disabled-text: #9ca3af;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.ees-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.ees-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.ees-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ees-hd__spacer {
  width: 36px;
}

/* 날짜 네비 */
.ees-datenav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-sm) var(--space-lg) 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-xs) var(--space-sm);
}
.ees-datenav__btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.ees-datenav__btn:disabled {
  color: var(--color-disabled-text);
  cursor: not-allowed;
}
.ees-datenav__center {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
}
.ees-datenav__label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ees-datenav__today {
  height: 26px;
  padding: 0 var(--space-sm);
  border: 1px solid var(--color-primary-tint-border);
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 필터(부서 칩 + 검색) */
.ees-filter {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.ees-chips {
  display: flex;
  gap: var(--space-xs);
  overflow-x: auto;
}
.ees-chip {
  flex-shrink: 0;
  height: 32px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.ees-chip.is-active {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.ees-search {
  height: 40px;
  box-sizing: border-box;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 14px;
  font-family: inherit;
}
.ees-search:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 본문 */
.ees-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}
.ees-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.ees-retry {
  display: block;
  margin: var(--space-sm) auto 0;
  height: 36px;
  padding: 0 var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 스프라이트 */
.ees-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
