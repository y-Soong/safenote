<!--
  AdminApprovalPendingList.vue — 승인 대기 리스트 (유형 칩 + 정렬 + 검색 + 카드)
  - 작업 ID: 001-P2-F2 (분해: 001-phase2-admin-approval-plan.md §2-1 / §3-B A-1)
  - web 단일 출처: Attd_10.vue 접수함 / 재기획서 05-screen-structure §5.4(필터·정렬)·§5.5(접수함)
  - 백엔드: GET /appApi/admin/approval/pending?group=&sort=&keyword=&page=&pageSize=&groupLeave=Y
            POST /appApi/admin/approval/bulk-process (prafta-leavemulti: 연차 묶음 일괄 승인/반려)
  - prafta-leavemulti(PRAFTA-APP-LM-5): groupLeave=Y 로 조회하면 연차 기간(From-To) 신청 묶음이
      카드 1건(leaveGroupId + groupItems[])으로 접혀 내려온다. 묶음은 AdminApprovalGroupCard 로,
      단건(leaveGroupId 없음)은 기존 AdminApprovalCard 로 분기 렌더한다(단건 경로 무회귀).
      일괄 처리 결과는 부분 성공이므로 성공/실패 건수를 반드시 분리 표기한다.
  - 유형 칩(요청서): 전체 / 스케줄수정* / 근태보정 / 초과근무 / 연차  (*스케줄수정 = A5 확정 전 비활성)
  - 정렬: 요청순(REQUESTED 최신) · 임박순(DEADLINE D-N, A1 deadline 확보 시)
  - 무한 스크롤: MyRequestsView 패턴 차용(IntersectionObserver) — 본 골격은 sentinel 마크업만, 관찰 로직은 developer.
  - 디자인 토큰: 부모(.admin-approval-view)에서 선언한 var(--...) 상속.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + TODO(developer) 골격만.
  - developer 라운드 스코프(TODO):
      (1) GET pending 조회(group/sort/keyword/page) → items/counts/totalCount/hasMore 바인딩
      (2) 칩/정렬/검색 변경 시 재조회(첫 페이지부터)
      (3) 무한 스크롤(IntersectionObserver, MyRequestsView 참조) 연결
      (4) 카드 선택 → emit('select', item) / 총 건수 → emit('update:total', totalCount)
-->
<template>
  <div class="ap-pending">
    <!-- 유형 칩 필터(단일 선택 세그먼트) -->
    <div class="ap-chips" role="tablist" aria-label="요청 유형 필터">
      <button
        v-for="c in chips"
        :key="c.key"
        type="button"
        class="ap-chip"
        :class="{ 'is-active': activeGroup === c.key, 'is-disabled': c.disabled }"
        :disabled="c.disabled"
        :aria-selected="activeGroup === c.key"
        @click="onSelectGroup(c)"
      >
        {{ c.label }}
        <span v-if="!c.disabled && countOf(c.key) > 0" class="ap-chip__count">
          {{ countOf(c.key) }}
        </span>
      </button>
    </div>

    <!-- 정렬 + 검색 -->
    <div class="ap-toolbar">
      <input
        v-model.trim="keyword"
        type="search"
        class="ap-toolbar__search"
        placeholder="요청자명 · 사번 검색"
        @keyup.enter="onSearch"
      />
      <!-- 임박순 정렬은 A1(마감 기준일 소스) 미확정으로 항상 요청순 동작 → 무동작 오인 방지 위해 토글 숨김(Fix2). -->
      <button
        v-if="deadlineSortEnabled"
        type="button"
        class="ap-toolbar__sort"
        :aria-pressed="sort === 'DEADLINE'"
        @click="onToggleSort"
      >
        {{ sortLabel }}
      </button>
    </div>

    <!-- 본문 -->
    <div ref="bodyRef" class="ap-list">
      <!-- 로딩(첫 페이지) -->
      <p v-if="isLoading && items.length === 0" class="ap-state" aria-live="polite">
        불러오는 중...
      </p>

      <!-- 빈 상태: 전체 0 -->
      <p
        v-else-if="!isLoading && items.length === 0 && totalCount === 0"
        class="ap-state"
        aria-live="polite"
      >
        처리할 승인 요청이 없습니다.
      </p>

      <!-- 빈 상태: 필터 0 -->
      <div
        v-else-if="!isLoading && items.length === 0 && totalCount > 0"
        class="ap-state ap-state--filtered"
        aria-live="polite"
      >
        <p class="ap-state__text">조건에 맞는 요청이 없습니다.</p>
        <button type="button" class="ap-state__reset" @click="onResetFilters">필터 해제</button>
      </div>

      <!-- 카드 리스트 -->
      <template v-else>
        <!-- key: 묶음 대표 reqId 와 단건 reqId 충돌 방지 -->
        <template v-for="item in items" :key="item.leaveGroupId || item.reqId">
          <!-- prafta-leavemulti: 묶음(기간신청)은 카드 1장으로 접어 표시 -->
          <AdminApprovalGroupCard
            v-if="item.leaveGroupId"
            :item="item"
            :expanded="isExpanded(item.leaveGroupId)"
            :submitting="bulkSubmittingId === item.leaveGroupId"
            @toggle="onToggleGroup"
            @select-child="onSelectChild"
            @bulk="onBulk(item, $event)"
          />
          <!-- 단건(단일일 연차·근태보정·초과·스케줄) — 종전과 100% 동일 -->
          <AdminApprovalCard v-else :item="item" @click="$emit('select', item)" />
        </template>

        <!-- 무한 스크롤 sentinel(관찰 로직은 developer) -->
        <div ref="sentinelRef" class="ap-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="ap-state ap-state--more">불러오는 중...</p>
      </template>
    </div>

    <!-- 일괄 반려 사유 — 단건 반려와 동일 시트 재사용(신규 시트 만들지 않음) -->
    <AdminApprovalRejectSheet
      :open="rejectSheetOpen"
      :submitting="!!bulkSubmittingId"
      @close="onRejectSheetClose"
      @confirm="onRejectSheetConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay, formatDateTimeDisplay } from '@/utils/approvalFormat'

import AdminApprovalCard from './AdminApprovalCard.vue'
// prafta-leavemulti: 묶음 카드 + 일괄 반려 사유 시트(단건 반려와 동일 시트 재사용)
import AdminApprovalGroupCard from './AdminApprovalGroupCard.vue'
import AdminApprovalRejectSheet from './AdminApprovalRejectSheet.vue'

// select: 카드 선택(상세 이동) / update:total: 대기 총 건수(셸 배지)
const emit = defineEmits(['select', 'update:total'])

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MyRequestsView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 공통: confirm 폴백(앱 전역 $confirm 우선) — MyAttendanceView 패턴 동일
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// 페이지 크기(MyRequestsView 동일 — 서버도 동일 기본값 가정, A7)
const PAGE_SIZE = 20

// 유형 칩(요청서). 스케줄수정은 029(SCHEDULE 처리경로 완성)으로 활성화.
//   유형 카드 라벨/요약은 서버(reqTypeNm/summaryLines)가 단일출처. 칩 label 은 필터 세그먼트 표기 전용.
const chips = [
  { key: 'ALL', label: '전체', disabled: false },
  { key: 'SCHEDULE', label: '스케줄수정', disabled: false },
  { key: 'CORRECTION', label: '근태보정', disabled: false },
  { key: 'OVERTIME', label: '초과근무', disabled: false },
  { key: 'LEAVE', label: '연차', disabled: false },
]
const activeGroup = ref('ALL')

// 정렬: REQUESTED(요청순 최신) | DEADLINE(임박순). 기본 요청순(재기획서 §5.4).
const sort = ref('REQUESTED')
const sortLabel = computed(() => (sort.value === 'DEADLINE' ? '마감 임박순' : '요청 최신순'))

// Fix2: A1(마감 기준일 소스) 미확정 → 백엔드가 DEADLINE 요청도 INSERT_DATE DESC 로만 정렬하므로
//   임박순 토글이 무동작으로 오인된다. A1 확정 전까지 토글을 숨긴다(백엔드 sort 파라미터/계약은 유지).
// TODO(developer): A1(마감 기준일) 소스 확정 후 deadlineSortEnabled = true 로 전환하여 임박순 토글을 활성화.
const deadlineSortEnabled = false

// 검색어(요청자명/사번)
const keyword = ref('')

// ── 데이터 상태(서버 응답으로 채움) ───────────────────────────────────────
const items = ref([]) // 정규화 요청 아이템[]
const counts = ref({}) // { ALL, CORRECTION, OVERTIME, LEAVE }
const totalCount = ref(0)
const hasMore = ref(false)
const page = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)
let intersectionObserver = null

// 동시 호출 가드(칩/정렬/검색 빠른 연속 변경 등) — 가장 최신 요청만 반영
let inflightSeq = 0

// 칩별 건수 배지(서버 counts) — 비활성 칩은 표시 안 함
const countOf = (key) => Number(counts.value?.[key]) || 0

// 서버 pending 아이템 → 카드 표시용 정규화.
//   카드 template 은 *Display 파생 필드를 소비(planner 골격). 식별자는 토큰 전용이므로 전송하지 않는다.
//   선점 잠금(lockedYn)은 v1 미구현(사용자 확정) → 항상 false 로 강제(‘처리 중’ 배지 비노출).
const normalizeItem = (raw) => ({
  ...raw,
  targetYmdDisplay: raw.targetYmdDisplay || formatYmdDisplay(raw.targetYmd),
  reqDateDisplay: raw.reqDateDisplay || formatDateTimeDisplay(raw.reqDate),
  summaryLines: Array.isArray(raw.summaryLines) ? raw.summaryLines : [],
  selfYn: raw.selfYn === 'Y' || raw.selfYn === true ? 'Y' : 'N',
  lockedYn: false,
  lockedByNm: '',
  // 가불표시-04: 가불 충당 일수(연차만 세팅, 그 외/구서버는 null → 카드 배지 미표시)
  borrowDays: raw.borrowDays ?? null,
  // prafta-leavemulti: 묶음 파생. 단건·구서버 응답은 leaveGroupId 가 없어 종전 카드 경로를 그대로 탄다.
  groupFromYmdDisplay: formatYmdDisplay(raw.groupFromYmd),
  groupToYmdDisplay: formatYmdDisplay(raw.groupToYmd),
  groupItems: (Array.isArray(raw.groupItems) ? raw.groupItems : []).map((c) => ({
    ...c,
    targetYmdDisplay: formatYmdDisplay(c.targetYmd),
    selfYn: c.selfYn === 'Y' || c.selfYn === true ? 'Y' : 'N',
  })),
})

// 칩/정렬/검색만 서버로 전달(식별자는 axios 인터셉터의 토큰 클레임 경유 — IDOR 차단, plan §3/§4).
const buildParams = (targetPage) => {
  const params = {
    group: activeGroup.value,
    sort: sort.value,
    page: targetPage,
    pageSize: PAGE_SIZE,
    // prafta-leavemulti: 연차 기간신청 묶음을 서버가 카드 1건으로 접어 내린다(페이징 단위도 묶음).
    //   미지정(구버전 앱)이면 서버가 종전대로 날짜별 N행을 내린다 — additive.
    groupLeave: 'Y',
  }
  if (keyword.value) params.keyword = keyword.value
  return params
}

const loadPage = async (append = false) => {
  const mySeq = ++inflightSeq

  if (append) {
    if (!hasMore.value || isLoadingMore.value || isLoading.value) return
    isLoadingMore.value = true
  } else {
    page.value = 1
    items.value = []
    hasMore.value = false
    isLoading.value = true
  }

  try {
    const res = await api.get('/appApi/admin/approval/pending', {
      params: buildParams(page.value),
    })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = (Array.isArray(data.items) ? data.items : []).map(normalizeItem)

    items.value = append ? items.value.concat(fetched) : fetched
    counts.value = data.counts || {}
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : items.value.length
    hasMore.value = !!data.hasMore

    // 셸 배지용 총 건수(전체 대기 건수 = counts.ALL 우선, 없으면 totalCount).
    emit('update:total', Number(counts.value?.ALL ?? totalCount.value) || 0)

    if (hasMore.value) page.value += 1
  } catch (e) {
    if (mySeq !== inflightSeq) return
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림.
    console.error('[AdminApprovalPendingList] 대기 목록 조회 실패')
    const msg = resolveApiErrorMessage(
      e,
      '요청 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
    showAlert(msg)
    if (!append) {
      items.value = []
      hasMore.value = false
    }
  } finally {
    if (mySeq === inflightSeq) {
      isLoading.value = false
      isLoadingMore.value = false
    }
  }
}

// ── 무한 스크롤(IntersectionObserver — MyRequestsView 패턴) ────────────────
const observeSentinel = () => {
  if (!intersectionObserver || !sentinelRef.value) return
  try {
    intersectionObserver.disconnect()
  } catch (_e) {
    // 무시
  }
  intersectionObserver.observe(sentinelRef.value)
}

const setupInfiniteScroll = () => {
  if (typeof IntersectionObserver === 'undefined') return
  intersectionObserver = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMore.value && !isLoadingMore.value && !isLoading.value) {
        loadPage(true)
      }
    },
    {
      root: bodyRef.value || null,
      rootMargin: '0px 0px 120px 0px',
      threshold: 0,
    },
  )
  observeSentinel()
}

const reload = async () => {
  expandedGroups.value = [] // prafta-leavemulti: 재조회 시 펼침 상태 초기화
  await loadPage(false)
  await nextTick()
  observeSentinel()
}

// ── 핸들러 ────────────────────────────────────────────────────────────────
const onSelectGroup = (chip) => {
  if (chip.disabled || activeGroup.value === chip.key) return
  activeGroup.value = chip.key
  reload()
}

const onToggleSort = () => {
  sort.value = sort.value === 'REQUESTED' ? 'DEADLINE' : 'REQUESTED'
  reload()
}

const onSearch = () => {
  reload()
}

const onResetFilters = () => {
  activeGroup.value = 'ALL'
  sort.value = 'REQUESTED'
  keyword.value = ''
  reload()
}

// ── prafta-leavemulti: 묶음 펼침 / 일괄 처리 ───────────────────────────────
// ★본인 신청(selfYn='Y') 건은 전송에서 제외하지 않는다. 2026-08-16 사용자 확정으로
//   "관리자 본인 결재 차단"이 전 유형에서 해제되어 백엔드 게이트가 제거됐다(배지는 정보성으로만 유지).
// ★items 상한은 백엔드 AppAdminApprovalBulkServiceImpl.MAX_ITEMS 와 동일하며,
//   초과 시 서버가 전건 400 을 던지므로 화면이 먼저 안내한다.
//   ★기간신청 상한(62일)보다 커야 한다 — 작으면 최대 묶음이 일괄에서 통째로 거부된다.
const MAX_BULK_ITEMS = 70
// 실패 사유를 몇 줄까지 노출할지(넘치면 "외 N건"으로 접는다)
const MAX_FAIL_LINES = 10

const expandedGroups = ref([]) // 펼쳐 놓은 leaveGroupId 목록
const bulkSubmittingId = ref('') // 처리 중인 묶음 ID(빈 문자열=없음)
const rejectSheetOpen = ref(false) // 일괄 반려 사유 시트
const pendingBulkGroup = ref(null) // 반려 사유 입력 대기 중인 묶음

const isExpanded = (gid) => expandedGroups.value.includes(gid)

const onToggleGroup = (gid) => {
  const i = expandedGroups.value.indexOf(gid)
  if (i >= 0) expandedGroups.value.splice(i, 1)
  else expandedGroups.value.push(gid)
}

// 펼친 개별 건 선택 → 기존 단건 상세 경로 재사용(부모 셸이 라우팅)
const onSelectChild = (child) => {
  if (!child?.reqId) return
  emit('select', { reqId: child.reqId, group: 'LEAVE' })
}

// 묶음의 전송 items(건별 approvalStep 을 그대로 싣는다 — 묶음이라도 단계가 갈릴 수 있다)
const buildBulkItems = (item) =>
  (Array.isArray(item?.groupItems) ? item.groupItems : [])
    .filter((c) => c?.reqId)
    .map((c) => ({ reqId: c.reqId, approvalStep: c.approvalStep }))

// 실패 건 안내 줄 — reqId 를 날짜로 환원해 관리자가 어느 날이 막혔는지 알 수 있게 한다.
const buildFailLines = (item, failedList) => {
  const dateOf = new Map(
    (Array.isArray(item?.groupItems) ? item.groupItems : []).map((c) => [
      c.reqId,
      c.targetYmdDisplay,
    ]),
  )
  const lines = failedList
    .slice(0, MAX_FAIL_LINES)
    .map(
      (f) => `· ${dateOf.get(f?.reqId) || f?.reqId || ''}: ${f?.reason || '처리하지 못했습니다.'}`,
    )
  const rest = failedList.length - lines.length
  if (rest > 0) lines.push(`외 ${rest}건`)
  return lines
}

// ★부분 성공 안내 — 성공/실패 건수를 항상 분리 표기한다.
//   전부 성공으로 뭉뚱그리면 5건 중 2건이 실패해도 관리자가 전건 처리됐다고 오인한다.
const buildBulkResultMessage = (item, decision, data) => {
  const actionNm = decision === 'REJECT' ? '반려' : '승인'
  const successCount = Number(data?.successCount) || 0
  const failedList = Array.isArray(data?.failedList) ? data.failedList : []
  const failedCount = Number(data?.failedCount) || failedList.length
  if (failedCount === 0) return `${successCount}건 ${actionNm} 처리되었습니다.`
  return [
    `${successCount}건 ${actionNm} 처리 · ${failedCount}건 제외`,
    ...buildFailLines(item, failedList),
  ].join('\n')
}

const submitBulk = async (item, decision, comment) => {
  if (bulkSubmittingId.value) return
  const groupId = item?.leaveGroupId || ''
  const bulkItems = buildBulkItems(item)

  if (bulkItems.length === 0) {
    await showAlert('처리할 대기 건이 없습니다. 목록을 새로고침해 주세요.')
    return
  }
  // 서버 400 을 그대로 노출하지 않고 화면이 먼저 안내한다(카드에도 사유 배지가 상시 노출된다).
  if (bulkItems.length > MAX_BULK_ITEMS) {
    await showAlert(
      `일괄 처리는 한 번에 최대 ${MAX_BULK_ITEMS}건까지 가능합니다. 펼쳐서 날짜별로 처리해 주세요.`,
    )
    return
  }

  bulkSubmittingId.value = groupId
  try {
    const res = await api.post('/appApi/admin/approval/bulk-process', {
      group: 'LEAVE',
      decision,
      comment: comment || '',
      items: bulkItems,
    })
    rejectSheetOpen.value = false
    pendingBulkGroup.value = null
    await showAlert(buildBulkResultMessage(item, decision, res?.data))
  } catch (e) {
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림.
    console.error('[AdminApprovalPendingList] 일괄 처리 실패')
    const msg = resolveApiErrorMessage(e, '처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
    await showAlert(msg)
  } finally {
    bulkSubmittingId.value = ''
    // 성공/부분성공/실패 어느 쪽이든 첫 페이지부터 재조회한다(남은 대기 건을 실제 상태로 다시 보여준다).
    await reload()
  }
}

const onBulk = async (item, payload) => {
  if (bulkSubmittingId.value) return
  const decision = payload?.decision
  if (decision === 'REJECT') {
    // 반려 사유는 기존 시트에서 입력받는다(전 건 동일 사유 — 정책 §7.7).
    pendingBulkGroup.value = item
    rejectSheetOpen.value = true
    return
  }
  const count = buildBulkItems(item).length
  const ok = await askConfirm(`${count}건을 승인 처리하시겠습니까?`)
  if (!ok) return
  await submitBulk(item, 'APPROVE_ASIS', '')
}

const onRejectSheetClose = () => {
  if (bulkSubmittingId.value) return
  rejectSheetOpen.value = false
  pendingBulkGroup.value = null
}

const onRejectSheetConfirm = async (reason) => {
  const target = pendingBulkGroup.value
  if (!target) {
    rejectSheetOpen.value = false
    return
  }
  await submitBulk(target, 'REJECT', reason)
}

// ── 라이프사이클 ──────────────────────────────────────────────────────────
onMounted(async () => {
  await loadPage(false)
  await nextTick()
  setupInfiniteScroll()
})

onBeforeUnmount(() => {
  if (intersectionObserver) {
    try {
      intersectionObserver.disconnect()
    } catch (_e) {
      // 무시
    }
    intersectionObserver = null
  }
})
</script>

<style scoped>
.ap-pending {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 0;
}

/* 유형 칩 */
.ap-chips {
  display: flex;
  gap: var(--space-sm);
  overflow-x: auto;
  padding-bottom: var(--space-xs);
  -webkit-overflow-scrolling: touch;
}
.ap-chip {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
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
.ap-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}
.ap-chip.is-disabled {
  background: var(--color-disabled-bg);
  color: var(--color-disabled-text);
  cursor: not-allowed;
}
.ap-chip__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 10px;
  font-weight: 700;
}
.ap-chip.is-active .ap-chip__count {
  background: var(--color-primary);
}

/* 정렬 + 검색 */
.ap-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.ap-toolbar__search {
  flex: 1;
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
.ap-toolbar__search:focus {
  outline: none;
  border-color: var(--color-primary);
}
.ap-toolbar__sort {
  flex: 0 0 auto;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}

/* 리스트 */
.ap-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.ap-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.ap-state--more {
  padding: 12px 0;
  font-size: 13px;
}
.ap-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.ap-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.ap-state__reset {
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
.ap-sentinel {
  height: 1px;
}
</style>
