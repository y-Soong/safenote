<!--
  LeavePromotionPlanView.vue — 연차 사용촉진 1차 계획서 화면 (모바일 앱, 신규)
  - 작업 ID: prafta-com-008-A-7 (분해: .claude/requests/common/refs/prafta-com-008/prafta-com-008-A-decomposition.md)
  - UI 명세: UI-app-008-A-1
  - 라우트: /LeavePromotionPlan (보호 — beforeEach 토큰 게이트, publicPaths 미포함)
  - 정책 출처: 작업지시서 §2(1차 촉진 — 캘린더 + 행추가/키인, 1일 단위, 미제출/일부 허용)
  - 참조 패턴: views/leave/LeaveApplyView.vue (헤더 + 본문 스크롤 + 폼 + 제출 + 디자인 토큰 루트 1회)
  - 역할 분담: 컨테이너 = active 조회(A-3) + 계획 제출(A-3) + 라우팅. 자식 = 캘린더/날짜행 입력.
  - planner 라운드: template + scoped style 완성, script 는 import/ref/onMounted 골격 + TODO.
  - developer 라운드:
      · GET /appApi/leavepromo01/active 로 보유/잔여/기간/selectable/existing 조회.
      · POST /appApi/leavepromo01/plan 로 selectedYmds 제출(1일 단위, PROMOTION_STAGE=1차/자발은 서버 기록).
      · 월 네비에 따른 selectableYmds 의 월별 필터링은 active 응답 가공 또는 월별 재조회 정책 확정 후.
-->
<template>
  <div class="lpp-view">
    <!-- 헤더 -->
    <header class="lpp-hd">
      <button type="button" class="lpp-hd__back" aria-label="뒤로" @click="onCancel">‹</button>
      <h1 class="lpp-hd__title">연차 사용 계획서</h1>
      <span class="lpp-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="lpp-body">
      <!-- 로딩 -->
      <p v-if="isLoading" class="lpp-state">불러오는 중...</p>

      <!-- 에러 -->
      <div v-else-if="loadError" class="lpp-state lpp-state--err">
        <p>{{ loadError }}</p>
        <button type="button" class="lpp-retry" @click="loadActive">다시 시도</button>
      </div>

      <!-- 진행 중 촉진 없음 -->
      <div v-else-if="!promotion" class="lpp-state">
        <p>진행 중인 연차 사용촉진이 없습니다.</p>
      </div>

      <!-- 계획서 -->
      <template v-else>
        <!-- 보유/미지정 요약 -->
        <section class="lpp-summary">
          <div class="lpp-summary__item">
            <span class="lpp-summary__label">보유</span>
            <strong class="lpp-summary__value">{{ promotion.grantedDays }}일</strong>
          </div>
          <div class="lpp-summary__item">
            <span class="lpp-summary__label">미지정 잔여</span>
            <strong class="lpp-summary__value lpp-summary__value--accent">
              {{ promotion.remainingDays }}일
            </strong>
          </div>
          <div class="lpp-summary__item">
            <span class="lpp-summary__label">사용 기한</span>
            <strong class="lpp-summary__value">{{ formatYmd(promotion.availTo) }}</strong>
          </div>
        </section>

        <!-- 캘린더 -->
        <LeavePromotionCalendar
          v-model="selectedYmds"
          :ym="viewYm"
          :selectable-ymds="selectableYmds"
          :existing-leave-ymds="existingLeaveYmds"
          @prev-month="onPrevMonth"
          @next-month="onNextMonth"
        />

        <!-- 선택 날짜 행 + 키인 -->
        <LeavePromotionDateRows
          v-model="selectedYmds"
          :selectable-ymds="selectableYmds"
          :min-date="minDate"
          :max-date="maxDate"
        />
      </template>
    </main>

    <!-- 하단 고정 저장 -->
    <footer v-if="promotion && !isLoading && !loadError" class="lpp-foot">
      <button
        type="button"
        class="lpp-submit"
        :disabled="isSubmitting || selectedYmds.length === 0"
        @click="onSubmit"
      >
        {{ isSubmitting ? '저장 중...' : '계획 저장' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay } from '@/utils/approvalFormat'

import LeavePromotionCalendar from './components/LeavePromotionCalendar.vue'
import LeavePromotionDateRows from './components/LeavePromotionDateRows.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 공통 confirm 폴백(앱 전역 $confirm 우선) — 계획 미제출 이탈 경고에 사용.
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 반응형 상태 (developer: 조회/제출/월 네비 로직 보완) ──────────────────
const isLoading = ref(true)
const loadError = ref('')
const isSubmitting = ref(false)

// A-3 active 응답(PromotionActiveResponse):
//   { inProgress, grantedDays, remainingDays, baseAvailToDate(YYYYMMDD),
//     designatedDates:[YYYYMMDD], loginNotifiedYn('Y'/'N') }
// 템플릿이 기대하는 promotion.availTo 는 baseAvailToDate 로 매핑한다(아래 loadActive).
// 진행 중 촉진이 없으면(inProgress=false) null 로 두어 미노출.
const promotion = ref(null)
// 선택 가능일 = 오늘~기준 만료일 사이 평일(주말 제외, 이미 지정된 날 제외). 휴일/교대 비근무 등
// 정밀 가용성은 백엔드가 plan 응답 failedDates 로 최종 판정한다(클라는 best-effort 평일 게이트).
const selectableYmds = ref([])
// 이미 등록(CONFIRMED)된 촉진 연차일 = active.designatedDates (캘린더 토글 불가, 표시만).
const existingLeaveYmds = ref([])

// 사용자가 선택(계획)한 날짜 (YYYYMMDD)
const selectedYmds = ref([])

// 표시 중 연/월 (YYYYMM) — 초기값은 active 로드 후 세팅
const viewYm = ref('')

// 키인 min/max (YYYY-MM-DD) — 오늘 ~ 기준 만료일.
const minDate = computed(() => toDashYmd(todayYmd()))
const maxDate = computed(() => toDashYmd(promotion.value?.availTo))

// YYYYMMDD → "YYYY.MM.DD" (표시 단일 출처 위임, D1)
const formatYmd = (ymd) => formatYmdDisplay(ymd)
// YYYYMMDD → "YYYY-MM-DD"
const toDashYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}

// 오늘(YYYYMMDD, 로컬)
const todayYmd = () => {
  const d = new Date()
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
}

// YYYYMM 한 달 가감(delta: -1/+1) → 'YYYYMM'
const shiftYm = (ym, delta) => {
  if (!ym || ym.length !== 6) return ym
  const y = Number(ym.slice(0, 4))
  const m = Number(ym.slice(4, 6))
  const total = y * 12 + (m - 1) + delta
  const ny = Math.floor(total / 12)
  const nm = (total % 12) + 1
  return `${ny}${String(nm).padStart(2, '0')}`
}

// 선택 가능일 산출 — 오늘~기준 만료일 사이 평일(월~금), 이미 지정된 날 제외.
//   휴일/교대 비근무 등 정밀 가용성은 서버 권위(plan failedDates)로 최종 판정한다.
const buildSelectableYmds = (fromYmd, toYmd, excludeSet) => {
  const result = []
  if (!fromYmd || !toYmd || fromYmd > toYmd) return result
  const toDate = (s) =>
    new Date(Number(s.slice(0, 4)), Number(s.slice(4, 6)) - 1, Number(s.slice(6, 8)))
  const cur = toDate(fromYmd)
  const end = toDate(toYmd)
  while (cur <= end) {
    const dow = cur.getDay() // 0=일, 6=토
    const ymd = `${cur.getFullYear()}${String(cur.getMonth() + 1).padStart(2, '0')}${String(cur.getDate()).padStart(2, '0')}`
    if (dow !== 0 && dow !== 6 && !excludeSet.has(ymd)) {
      result.push(ymd)
    }
    cur.setDate(cur.getDate() + 1)
  }
  return result
}

const onCancel = () => {
  // 계획 0건으로 이탈 시 1회 경고(확정-3) 후에만 뒤로.
  guardLeave(() => router.back())
}

// 월 네비 — 표시 월만 변경(selectableYmds 는 전체 기간 기준이라 자식이 viewYm 으로 필터링).
const onPrevMonth = () => {
  viewYm.value = shiftYm(viewYm.value, -1)
}
const onNextMonth = () => {
  viewYm.value = shiftYm(viewYm.value, 1)
}

// 계획 미제출 이탈 경고(확정-3). 이미 등록분이 있거나 선택분이 있으면 경고 없이 진행.
const guardLeave = async (proceed) => {
  const hasExisting = existingLeaveYmds.value.length > 0
  if (selectedYmds.value.length > 0 || hasExisting || !promotion.value) {
    proceed()
    return
  }
  const ok = await askConfirm(
    '1차 촉진 기간에 계획서를 제출하지 않으면 2차 촉진 때 남은 모든 연차 날짜를 회사가 직접 지정합니다. 그대로 종료하시겠습니까?',
  )
  if (ok) proceed()
}

// ── A-3 active 조회 ───────────────────────────────────────────────────────
const loadActive = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const { data } = await api.get('/appApi/leavepromo01/active')
    // 진행 중 1차 촉진 없음 → 미노출.
    if (!data || data.inProgress !== true) {
      promotion.value = null
      selectableYmds.value = []
      existingLeaveYmds.value = []
      return
    }
    // 템플릿 계약(promotion.availTo)에 맞춰 baseAvailToDate 를 availTo 로 매핑.
    promotion.value = {
      grantedDays: data.grantedDays,
      remainingDays: data.remainingDays,
      availTo: data.baseAvailToDate || '',
    }
    const designated = Array.isArray(data.designatedDates) ? data.designatedDates : []
    existingLeaveYmds.value = designated
    // 선택 가능일 = 오늘~만료일 평일(이미 지정분 제외).
    selectableYmds.value = buildSelectableYmds(
      todayYmd(),
      data.baseAvailToDate || '',
      new Set(designated),
    )
    // 표시 월 = 오늘 기준(만료일이 과거면 만료월). 선택분 초기화.
    const today = todayYmd()
    viewYm.value =
      data.baseAvailToDate && data.baseAvailToDate < today
        ? data.baseAvailToDate.slice(0, 6)
        : today.slice(0, 6)
    selectedYmds.value = []
  } catch (err) {
    console.error('[LeavePromotion] active 조회 실패:', err?.message)
    loadError.value = resolveApiErrorMessage(err, '촉진 정보를 불러오지 못했어요.')
  } finally {
    isLoading.value = false
  }
}

// ── A-3 계획 제출 ─────────────────────────────────────────────────────────
const onSubmit = async () => {
  if (isSubmitting.value || selectedYmds.value.length === 0) return
  isSubmitting.value = true
  try {
    // 식별값(cmpny/site/user)은 서버 JWT 강제 — 선택 날짜 목록만 전송(1일 단위).
    // PromotionPlanRequest.dates 계약에 맞춤.
    const { data } = await api.post('/appApi/leavepromo01/plan', {
      dates: selectedYmds.value,
    })
    // PromotionPlanResultResponse: registeredDates/skippedDates/failedDates/remainingDays.
    const registered = Array.isArray(data?.registeredDates) ? data.registeredDates : []
    const skipped = Array.isArray(data?.skippedDates) ? data.skippedDates : []
    const failed = Array.isArray(data?.failedDates) ? data.failedDates : []

    let msg = `계획이 저장되었어요. (등록 ${registered.length}건`
    if (skipped.length > 0) msg += `, 중복 ${skipped.length}건`
    if (failed.length > 0) msg += `, 실패 ${failed.length}건`
    msg += ')'
    if (failed.length > 0) {
      msg += `\n실패한 날짜는 휴일/마감/근무일 아님 등으로 등록되지 않았습니다.`
    }
    await showAlert(msg)
    // 잔여/이미지정 갱신을 위해 재조회(selectedYmds 는 loadActive 가 초기화).
    await loadActive()
  } catch (err) {
    console.error('[LeavePromotion] 계획 저장 실패:', err?.message)
    showAlert(resolveApiErrorMessage(err, '계획 저장 중 오류가 발생했습니다.'))
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  loadActive()
})
</script>

<style scoped>
.lpp-view {
  --color-primary: #16a34a;
  --color-primary-text-deep: #15803d;
  /* com-014-7 F1: 하위 LeavePromotionCalendar 가 소비하는 토큰을 호스트에서 일괄 제공
     (자식 컴포넌트의 리터럴 재정의 제거 → 토큰 단일 출처화) */
  --color-primary-tint: #f0fdf4;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-md: 10px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.lpp-hd {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}
.lpp-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  font-size: 24px;
  cursor: pointer;
}
.lpp-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
}
.lpp-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.lpp-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + 72px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.lpp-state {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.lpp-state--err {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.lpp-retry {
  height: 40px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

/* 요약 */
.lpp-summary {
  display: flex;
  gap: var(--space-sm);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
}
.lpp-summary__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  align-items: center;
}
.lpp-summary__label {
  font-size: 11px;
  color: var(--color-text-secondary);
}
.lpp-summary__value {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.lpp-summary__value--accent {
  color: var(--color-primary-text-deep);
}

/* 하단 고정 저장 */
.lpp-foot {
  position: sticky;
  bottom: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-md) + env(safe-area-inset-bottom));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border);
}
.lpp-submit {
  width: 100%;
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 15px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
}
.lpp-submit:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: default;
}
</style>
