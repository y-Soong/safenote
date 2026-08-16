<!--
  AdminSelfJoinView.vue — 관리자 모드 셀프가입(회원가입) 승인 (2탭 셸: 승인 대기 / 처리 이력)
  - 작업 ID: A6 (분해: .claude/requests/app_requests/작업지시서_통합테스트-결함_앱-셀프가입-승인화면.plan.md §2)
  - 진입: AdminLauncherView 의 SELF_JOIN 섹션 → /AdminSelfJoin?siteCd={currentSiteCd} (보호 라우트)
      진입 게이팅은 서버 access-context.moduleActiveMap.SELF_JOIN(A4). 본 화면에 역할(AUTH_CD) 분기 없음.
  - 데이터 인가는 서버 2단 게이트(사업장 인가 + 부서 관리 권한)가 최종 판정한다.
      화면 노출 제어만으로 끝내지 않는다.
  - 부서 칩(결정 J)은 셸이 소유해 두 탭이 공유한다. 탭 전환 시 초기화하지 않는다.
      비전사 역할이 nodeCd 없이 조회하면 서버가 403 이므로, 칩 선택값을 반드시 실어 보낸다.
  - ★디자인 토큰: 앱 프론트에는 :root 전역 토큰이 없다. 루트 클래스에 1회 선언해야 한다.
      빠뜨리면 자식 바텀시트가 투명하게 렌더된다(같은 뿌리로 3회 재발).
      값은 AdminApprovalView(.admin-approval-view) 세트를 복사했다.
  - 백엔드 경로는 kebab-case 확정본(/appApi/admin/self-join/*)을 쓴다. plan 초안의 /selfjoin/* 이 아니다.
-->
<template>
  <div class="admin-selfjoin-view">
    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="asj-hd">
      <button type="button" class="asj-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-asj-chev-left" />
        </svg>
      </button>
      <h1 class="asj-hd__title">가입 승인</h1>
      <span class="asj-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 탭바 (2탭, 디폴트=승인 대기) -->
    <nav class="asj-tabs" role="tablist" aria-label="가입 승인 탭">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="asj-tabs__btn"
        :class="{ 'is-active': activeTab === t.key }"
        role="tab"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >
        {{ t.label }}
        <span v-if="t.key === 'PENDING' && pendingTotal > 0" class="asj-tabs__badge">
          {{ pendingTotal }}
        </span>
      </button>
    </nav>

    <!-- 공유 필터: 부서 칩 + 검색 (두 탭이 함께 쓴다) -->
    <div class="asj-filter">
      <!-- 부서 칩: 전사역할이면 '전체' 1개, 노드관리자면 관리 부서 목록(결정 J) -->
      <div v-if="showNodeChips" class="asj-chips" role="tablist" aria-label="부서 필터">
        <button
          v-for="n in nodeChips"
          :key="n.nodeCd || 'ALL'"
          type="button"
          class="asj-chip"
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
        class="asj-search"
        placeholder="이름 또는 아이디 검색"
        @keyup.enter="onSearch"
      />
    </div>

    <!-- 본문 -->
    <main class="asj-body">
      <!-- 스코프 조회 통신 실패: 권한 문구로 오표기하지 않고 재시도를 제공한다(qa D2) -->
      <p v-if="scopeError" class="asj-state" aria-live="polite">
        부서 정보를 불러오지 못했습니다.
        <button type="button" class="asj-retry" @click="loadScopeNodes">다시 시도</button>
      </p>

      <!-- 관리 부서가 없는 계정: 목록 조회 자체를 하지 않는다(서버도 403) -->
      <p v-else-if="noManagedNode" class="asj-state" aria-live="polite">
        관리 중인 부서가 없습니다. 관리자에게 문의해 주세요.
      </p>

      <!-- 부서 스코프가 확정되기 전에는 리스트를 마운트하지 않는다.
           마운트하면 자식이 nodeCd 없이 조회를 날려 노드관리자가 진입 즉시 403 을 맞는다. -->
      <template v-else-if="scopeLoaded">
        <AdminSelfJoinPendingList
          v-if="activeTab === 'PENDING'"
          ref="pendingListRef"
          :site-cd="siteCd"
          :node-cd="activeNodeCd"
          :keyword="appliedKeyword"
          :submitting="submitting"
          @update:total="onPendingTotal"
          @approve="onOpenApprove"
          @reject="onOpenReject"
          @reset-filters="onResetFilters"
        />

        <AdminSelfJoinHistoryList
          v-else
          :site-cd="siteCd"
          :node-cd="activeNodeCd"
          :keyword="appliedKeyword"
          :show-site="activeNodeCd === null"
          @reset-filters="onResetFilters"
        />
      </template>
    </main>

    <!-- 승인 시트 -->
    <AdminSelfJoinApproveSheet
      :open="approveOpen"
      :target="approveTarget"
      :options="approveOptions"
      :submitting="submitting"
      @close="approveOpen = false"
      @confirm="onConfirmApprove"
    />

    <!-- 거부 시트 -->
    <AdminSelfJoinRejectSheet
      :open="rejectOpen"
      :submitting="submitting"
      @close="rejectOpen = false"
      @confirm="onConfirmReject"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="asj-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-asj-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

import AdminSelfJoinPendingList from './components/AdminSelfJoinPendingList.vue'
import AdminSelfJoinHistoryList from './components/AdminSelfJoinHistoryList.vue'
import AdminSelfJoinApproveSheet from './components/AdminSelfJoinApproveSheet.vue'
import AdminSelfJoinRejectSheet from './components/AdminSelfJoinRejectSheet.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — AdminApprovalView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 탭 정의(고정). 디폴트 = PENDING(승인 대기).
const tabs = [
  { key: 'PENDING', label: '승인 대기' },
  { key: 'HISTORY', label: '처리 이력' },
]
const activeTab = ref('PENDING')

// 현장 컨텍스트(런처가 query 로 전달). 없으면 서버가 토큰 gv_siteCd 로 폴백한다.
const siteCd = ref(typeof route.query.siteCd === 'string' ? route.query.siteCd : '')

// 부서 칩 상태(결정 J) — scope-nodes 응답으로 채운다.
//   activeNodeCd === null 이면 '전체'(전사역할).
const companyWide = ref(false)
const nodeChips = ref([]) // [{ nodeCd: null|'n1', nodeNm: '전체'|'생산1팀' }]
const activeNodeCd = ref(null)
const scopeLoaded = ref(false)
// 스코프 조회 통신 실패 여부 — "관리 부서 없음"(권한)과 구분해 표기/재시도를 제공한다.
const scopeError = ref(false)

// 검색어: 입력값(keyword)과 적용값(appliedKeyword)을 분리해 타이핑 중 재조회를 막는다.
const keyword = ref('')
const appliedKeyword = ref('')

// 대기 탭 배지용 총 건수(자식이 emit) — 표시 전용
const pendingTotal = ref(0)
const onPendingTotal = (total) => {
  pendingTotal.value = Number(total) || 0
}

// 시트 상태(UI 토글 — 허용 범위)
const approveOpen = ref(false)
const rejectOpen = ref(false)
const approveTarget = ref(null) // 승인 대상 행(서버 응답 1건)
const rejectTarget = ref(null) // 거부 대상 행(서버 응답 1건)
const approveOptions = ref(null) // approve-options 응답
const submitting = ref(false)

// 대기 리스트 재조회용 ref — 자식이 defineExpose({ reload }) 로 노출한다.
const pendingListRef = ref(null)

// 부서 칩 노출 여부 / 관리 부서 없음 판정
const showNodeChips = computed(() => scopeLoaded.value && nodeChips.value.length > 0)
// 스코프 조회가 실패한 상태는 "관리 부서 없음"과 구분한다 — 전자는 통신 오류(재시도 가능),
//   후자는 권한 문제(문의 필요)라 사용자가 취할 행동이 다르다.
const noManagedNode = computed(
  () =>
    scopeLoaded.value && !scopeError.value && !companyWide.value && nodeChips.value.length === 0,
)

// 조회 사업장 파라미터 — 비어 있으면 서버가 토큰 사업장으로 폴백하므로 키 자체를 보내지 않는다.
const buildSiteParams = () => (siteCd.value ? { siteCd: siteCd.value } : {})

// ── 조회 ──────────────────────────────────────────────────────────────
// 부서 스코프(칩 소스) 조회. 전사역할(master/hr)은 '전체' 칩 1개, 노드관리자는 관리 seed 노드 목록.
const loadScopeNodes = async () => {
  try {
    scopeError.value = false
    const { data } = await api.get('/appApi/admin/self-join/scope-nodes', {
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
    //   (조용히 목록을 조회하면 노드관리자는 nodeCd 없이 403 을 반복해 맞는다.)
    console.error('[AdminSelfJoinView] 부서 스코프 조회 실패')
    // ★통신 실패를 "관리 부서 없음"(권한 문제)으로 표기하지 않는다(qa D2).
    //   전사역할이 네트워크 오류를 맞고 "관리 중인 부서가 없습니다"를 보면 권한이 회수된 것으로 읽는다.
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

// 승인 시트 입력 옵션(소정근로 + 직급). 회사/사업장 단위 상수라 화면당 1회만 조회해 캐시한다.
const loadApproveOptions = async () => {
  if (approveOptions.value) return true
  try {
    const { data } = await api.get('/appApi/admin/self-join/approve-options', {
      params: buildSiteParams(),
    })
    approveOptions.value = data || {}
    return true
  } catch (e) {
    console.error('[AdminSelfJoinView] 승인 옵션 조회 실패')
    showAlert(
      resolveApiErrorMessage(e, '승인 입력 항목을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
    return false
  }
}

// 승인/거부 성공 후 대기 목록 재조회(이력 탭은 진입 시 조회하므로 건드리지 않는다).
const reloadPending = async () => {
  await pendingListRef.value?.reload?.()
}

// 입사일 전송 규격 변환: DateStepperField 의 'YYYY-MM-DD' → 서버 계약 'YYYYMMDD'.
const toYyyymmdd = (value) => String(value || '').replace(/-/g, '')

// ── 핸들러 ────────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/AdminHome')
}

// 부서 칩 선택 — 자식 리스트가 nodeCd prop 변화를 watch 해 스스로 재조회한다.
const onSelectNode = (n) => {
  if (!n || activeNodeCd.value === n.nodeCd) return
  activeNodeCd.value = n.nodeCd
}

// 검색 실행(Enter) — 적용값만 갱신하면 자식이 keyword prop 변화로 재조회한다.
const onSearch = () => {
  appliedKeyword.value = keyword.value
}

// 자식 빈 상태의 [필터 해제] — 셸이 소유한 검색어를 비운다(부서 칩은 권한 스코프라 유지).
const onResetFilters = () => {
  keyword.value = ''
  appliedKeyword.value = ''
}

const onOpenApprove = async (row) => {
  if (!row?.userCd || submitting.value) return
  approveTarget.value = row
  // 옵션을 못 받으면 시트를 열지 않는다(직급/사유 칩이 빈 채로 뜨는 것을 막는다).
  const ok = await loadApproveOptions()
  if (!ok) {
    approveTarget.value = null
    return
  }
  approveOpen.value = true
}

const onOpenReject = (row) => {
  if (!row?.userCd || submitting.value) return
  rejectTarget.value = row
  rejectOpen.value = true
}

// 승인 확정 — 시트는 입력값만 넘기고, 전송 규격 변환(하이픈 제거·null 치환)은 셸이 한다.
//   ★고용형태(REGULAR)는 보내지 않는다. 앱 요청 DTO(AppSelfJoinApproveRequest)에 필드 자체가 없고
//     서버가 상수로 채운다(2026-08-13 사용자 확정).
const onConfirmApprove = async (form) => {
  const userCd = approveTarget.value?.userCd
  if (!userCd || !form || submitting.value) return

  submitting.value = true
  try {
    await api.post('/appApi/admin/self-join/approve', {
      userCd,
      hireDate: toYyyymmdd(form.hireDate),
      rankCd: form.rankCd ? form.rankCd : null,
      stdWorkType: form.stdWorkType,
      stdWorkWeekMinutes: form.stdWorkWeekMinutes ?? null,
      stdWorkReasonCd: form.stdWorkReasonCd ? form.stdWorkReasonCd : null,
    })

    approveOpen.value = false
    approveTarget.value = null
    await reloadPending()
  } catch (e) {
    console.error('[AdminSelfJoinView] 가입 승인 실패')
    showAlert(resolveApiErrorMessage(e, '승인 처리에 실패했어요. 잠시 후 다시 시도해 주세요.'))
  } finally {
    submitting.value = false
  }
}

// 거부 확정 — 서버 계약 필드명은 rejectReason 이다(reason 아님).
const onConfirmReject = async (reason) => {
  const userCd = rejectTarget.value?.userCd
  const rejectReason = String(reason || '').trim()
  if (!userCd || !rejectReason || submitting.value) return

  submitting.value = true
  try {
    await api.post('/appApi/admin/self-join/reject', { userCd, rejectReason })

    rejectOpen.value = false
    rejectTarget.value = null
    await reloadPending()
  } catch (e) {
    console.error('[AdminSelfJoinView] 가입 거부 실패')
    showAlert(resolveApiErrorMessage(e, '거부 처리에 실패했어요. 잠시 후 다시 시도해 주세요.'))
  } finally {
    submitting.value = false
  }
}

// 진입 시 부서 스코프를 먼저 확정한다(칩이 정해져야 목록 조회에 nodeCd 를 실을 수 있다).
onMounted(loadScopeNodes)
</script>

<style scoped>
/* ★디자인 토큰 1회 선언(AdminApprovalView 세트) — 자식 scoped 가 상속.
   앱 프론트에는 :root 전역 토큰이 없다. 이 블록이 없으면 공용 바텀시트가 투명해진다. */
.admin-selfjoin-view {
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
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
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
.asj-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.asj-hd__back {
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
.asj-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.asj-hd__spacer {
  width: 36px;
}

/* 탭바 */
.asj-tabs {
  display: flex;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.asj-tabs__btn {
  position: relative;
  flex: 1;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.asj-tabs__btn.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}
.asj-tabs__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 11px;
  font-weight: 700;
}

/* 공유 필터 영역 */
.asj-filter {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg) 0;
}
.asj-chips {
  display: flex;
  gap: var(--space-sm);
  overflow-x: auto;
  padding-bottom: var(--space-xs);
  -webkit-overflow-scrolling: touch;
}
.asj-chip {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}
.asj-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}
.asj-search {
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
.asj-search:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 본문 */
.asj-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}
.asj-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 스코프 조회 실패 재시도 — 통신 오류는 사용자가 복구할 수 있어야 한다(라우트 재진입 강요 금지). */
.asj-retry {
  display: inline-block;
  margin-left: 6px;
  padding: 4px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
}

/* 스프라이트 */
.asj-sprite {
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
