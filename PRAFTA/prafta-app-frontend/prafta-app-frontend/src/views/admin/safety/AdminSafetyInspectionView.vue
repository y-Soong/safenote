<!--
  AdminSafetyInspectionView.vue — 관리자 모드 순회점검 결과 (조회 전용)
  - 작업 ID: prafta-app-025 J1-6 (.claude/requests/app_requests/job_1/J1-6-admin-safety.md §8 화면명세 2)
  - 진입: AdminSafetyView '순회점검 결과' 카드 → router.push('/AdminSafetyInspection') (보호 라우트)
  - 성격: 웹 chkLst03 매핑. 월 선택 → 점검 포인트별 결과 리스트(점검일수/불량수) → 상세 시트(일자별 답변 + 불량 사진/비고).
  - 권한: 진입 게이팅은 상위(AdminLauncher) access-context.SAFETY. 본 화면 조회 EP가 사업장 스코프 서버 재강제(C1).
  - 불량 = INSPECT_ANSWER_TYPE='N'(상세에서 'X' 표기). 불량행만 사진/비고 강조.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + 단순 UI 토글(월 이동/시트 open)만.
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/admin/safety/inspections?workMonth= → 포인트 리스트 바인딩
      (2) 포인트 탭 → GET /appApi/admin/safety/inspection-detail (siteCd/chkLstType/chkptCd/workMonth) → 상세 시트
      (3) 월 이동(prev/next) 시 재조회, loading/empty/error 분기
      (4) 사진 표시: filePath(서버 서빙 경로) → <img :src>
  - 디자인 토큰: AdminLauncherView 세트를 .admin-safety-inspect 루트에 1회 선언. 하드코딩 금지.
-->
<template>
  <div class="admin-safety-inspect">
    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="asi-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-asi-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-asi-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-asi-close" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
      </defs>
    </svg>

    <!-- 헤더 -->
    <header class="asi-hd">
      <button type="button" class="asi-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-asi-chev-left" /></svg>
      </button>
      <h1 class="asi-hd__title">순회점검 결과</h1>
      <span class="asi-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 월 네비게이션 -->
    <nav class="asi-month" aria-label="월 선택">
      <button type="button" class="asi-month__nav" aria-label="이전 달" @click="onPrevMonth">
        <svg class="icon" width="18" height="18" aria-hidden="true"><use href="#i-asi-chev-left" /></svg>
      </button>
      <span class="asi-month__label">{{ monthLabel }}</span>
      <button type="button" class="asi-month__nav" aria-label="다음 달" @click="onNextMonth">
        <svg class="icon" width="18" height="18" aria-hidden="true"><use href="#i-asi-chev-right" /></svg>
      </button>
    </nav>

    <!-- 본문: 포인트 리스트 -->
    <main class="asi-body">
      <div v-if="isLoading" class="asi-state" aria-live="polite">불러오는 중...</div>

      <div v-else-if="points.length === 0" class="asi-state" aria-live="polite">
        해당 월의 점검 결과가 없어요
      </div>

      <ul v-else class="asi-points">
        <li v-for="p in points" :key="p.chkptCd" class="asi-point">
          <button type="button" class="asi-point__btn" @click="onOpenDetail(p)">
            <span class="asi-point__main">
              <span class="asi-point__name">{{ p.chkptNm }}</span>
              <span class="asi-point__sub">점검 {{ p.inspectDayCnt }}일</span>
            </span>
            <span v-if="p.defectiveResultCnt > 0" class="asi-badge asi-badge--defect">
              불량 {{ p.defectiveResultCnt }}건
            </span>
            <svg class="icon asi-point__chev" width="18" height="18" aria-hidden="true"><use href="#i-asi-chev-right" /></svg>
          </button>
        </li>
      </ul>
    </main>

    <!-- 상세 시트(같은 화면 내 bottom sheet) -->
    <div v-if="detailOpen" class="asi-sheet" role="dialog" aria-modal="true" aria-label="점검 상세">
      <div class="asi-sheet__dim" @click="onCloseDetail" />
      <div class="asi-sheet__panel">
        <header class="asi-sheet__hd">
          <h2 class="asi-sheet__title">{{ selectedPoint?.chkptNm || '점검 상세' }}</h2>
          <button type="button" class="asi-sheet__close" aria-label="닫기" @click="onCloseDetail">
            <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-asi-close" /></svg>
          </button>
        </header>

        <div class="asi-sheet__body">
          <div v-if="isDetailLoading" class="asi-state" aria-live="polite">불러오는 중...</div>

          <div v-else-if="detailRows.length === 0" class="asi-state" aria-live="polite">
            점검 답변이 없어요
          </div>

          <ul v-else class="asi-answers">
            <li
              v-for="(r, idx) in detailRows"
              :key="idx"
              class="asi-answer"
              :class="{ 'asi-answer--defect': r.inspectAnswerType === 'X' }"
            >
              <div class="asi-answer__head">
                <span class="asi-answer__date">{{ formatDay(r.workDate) }}</span>
                <span
                  class="asi-answer__type"
                  :class="r.inspectAnswerType === 'X' ? 'is-defect' : 'is-ok'"
                >
                  {{ r.inspectAnswerType === 'X' ? '불량' : '양호' }}
                </span>
              </div>
              <p v-if="r.inspectItemSubj" class="asi-answer__subj">{{ r.inspectItemSubj }}</p>
              <!-- 불량 비고 -->
              <p v-if="r.inspectAnswerType === 'X' && r.answerDesc" class="asi-answer__desc">
                {{ r.answerDesc }}
              </p>
              <!-- 불량 사진(서버 서빙 URL: host + filePath + '/' + fileName) -->
              <div v-if="r.inspectAnswerType === 'X' && r.filePath && r.fileName" class="asi-answer__photo">
                <img :src="buildFileUrl(r.filePath, r.fileName)" :alt="r.fileName || '점검 사진'" />
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { buildFileUrl } from '@/utils/fileUrl'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통 alert 폴백(앱 전역 $alert 우선) — AdminLauncher/SiteOps 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 월 상태(YYYYMM). 초기 = 당월(onMounted 세팅). ─────────────────────────
const workMonth = ref('') // 'YYYYMM'
const monthLabel = computed(() => {
  // 'YYYYMM' → 'YYYY-MM' 표시. 빈값이면 placeholder.
  if (!workMonth.value || workMonth.value.length !== 6) return '----.--'
  return `${workMonth.value.slice(0, 4)}.${workMonth.value.slice(4, 6)}`
})

// ── 현재 사업장(현장전환 권위) — access-context.currentSiteCd 단일 출처(폴백 토큰 gv_siteCd). ──
//   서버가 멤버십을 재검증(C1)하므로 클라 폴백은 안전. 상세 조회 시 siteCd 로 전달.
const currentSiteCd = ref('')

// ── 리스트 상태 ───────────────────────────────────────────────────────────
const isLoading = ref(false)
const points = ref([]) // [{ siteCd, siteNm, chkptCd, chkptNm, chkLstType, inspectDayCnt, defectiveResultCnt }]

// ── 상세 시트 상태 ────────────────────────────────────────────────────────
const detailOpen = ref(false)
const isDetailLoading = ref(false)
const selectedPoint = ref(null)
const detailRows = ref([]) // [{ workDate, inspectItemSubj, inspectAnswerType('O'|'X'), answerDesc, filePath, fileName }]

// ── 보조: 일자(YYYYMMDD) → MM-DD 표시 ───────────────────────────────────
const formatDay = (v) => {
  if (v == null) return ''
  const s = String(v)
  if (s.length === 8) return `${s.slice(4, 6)}-${s.slice(6, 8)}`
  return s // 일(day) 숫자만 내려오는 경우 그대로
}

// 'YYYYMM' 에 개월 가감(prev/next). 1월 이전/12월 이후 연도 보정.
const shiftMonth = (ym, delta) => {
  if (!ym || ym.length !== 6) return ym
  let year = Number(ym.slice(0, 4))
  let month = Number(ym.slice(4, 6)) + delta
  while (month < 1) {
    month += 12
    year -= 1
  }
  while (month > 12) {
    month -= 12
    year += 1
  }
  return `${year}${String(month).padStart(2, '0')}`
}

// ── 현재 사업장 조회(현장전환 반영). 실패 시 토큰 사업장 폴백(서버가 최종 멤버십 재검증). ──
const loadCurrentSite = async () => {
  try {
    const { data } = await api.get('/appApi/admin/access-context')
    currentSiteCd.value = data?.currentSiteCd || sessionStorage.getItem('gv_siteCd') || ''
  } catch (e) {
    console.warn('[AdminSafetyInspection] access-context 조회 실패, 토큰 사업장 폴백:', e?.message)
    currentSiteCd.value = sessionStorage.getItem('gv_siteCd') || ''
  }
}

// ── H1 순회점검 결과 리스트 조회 ────────────────────────────────────────────
//   401/403 토큰 에러는 axios 인터셉터가 처리. 그 외 실패는 빈 목록 + 안내.
const loadInspections = async () => {
  isLoading.value = true
  try {
    const params = { workMonth: workMonth.value }
    if (currentSiteCd.value) params.siteCd = currentSiteCd.value
    const { data } = await api.get('/appApi/admin/safety/inspections', { params })
    points.value = Array.isArray(data?.points) ? data.points : []
  } catch (e) {
    const message = e?.response?.data?.message
    points.value = []
    await showAlert(message || '순회점검 결과를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

// ── 핸들러 ────────────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/AdminSafety')
}

const onPrevMonth = () => {
  workMonth.value = shiftMonth(workMonth.value, -1)
  loadInspections()
}
const onNextMonth = () => {
  workMonth.value = shiftMonth(workMonth.value, 1)
  loadInspections()
}

// ── H2 순회점검 상세 조회(포인트 1건). 불량 우선 정렬은 서버가 처리. ───────────
const onOpenDetail = async (point) => {
  selectedPoint.value = point
  detailOpen.value = true
  detailRows.value = []
  isDetailLoading.value = true
  try {
    const { data } = await api.get('/appApi/admin/safety/inspection-detail', {
      params: {
        siteCd: point.siteCd || currentSiteCd.value || undefined,
        chkLstType: point.chkLstType,
        chkptCd: point.chkptCd,
        workMonth: workMonth.value,
      },
    })
    detailRows.value = Array.isArray(data?.answers) ? data.answers : []
  } catch (e) {
    const message = e?.response?.data?.message
    detailRows.value = []
    await showAlert(message || '점검 상세를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    isDetailLoading.value = false
  }
}
const onCloseDetail = () => {
  detailOpen.value = false
  selectedPoint.value = null
  detailRows.value = []
}

// 진입 시: 당월 세팅 → 현재 사업장 확정 → 리스트 조회.
onMounted(async () => {
  const now = new Date()
  workMonth.value = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`
  await loadCurrentSite()
  await loadInspections()
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView 세트) — 자식 scoped 상속, 하드코딩 금지 */
.admin-safety-inspect {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-ok-text: #15803d;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-md: 10px;
  --radius-lg: 14px;

  height: 100vh;
  height: 100dvh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.asi-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
}
.asi-hd__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
}
.asi-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.asi-hd__spacer {
  min-width: 44px;
}

/* 월 네비 */
.asi-month {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 48px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
}
.asi-month__nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  min-height: 40px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
}
.asi-month__label {
  font-size: 15px;
  font-weight: 600;
  min-width: 84px;
  text-align: center;
}

/* 본문 */
.asi-body {
  flex: 1;
  min-height: 0;
  padding: 12px 16px;
  overflow-y: auto;
}
.asi-state {
  padding: 40px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 포인트 리스트 */
.asi-points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.asi-point__btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 14px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.asi-point__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.asi-point__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.asi-point__sub {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.asi-point__chev {
  color: var(--color-text-tertiary);
}
.asi-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 9999px;
}
.asi-badge--defect {
  color: var(--color-danger-text);
  background: var(--color-danger-tint);
}

/* 상세 시트 */
.asi-sheet {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.asi-sheet__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}
.asi-sheet__panel {
  position: relative;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}
.asi-sheet__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 0.5px solid var(--color-border);
}
.asi-sheet__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.asi-sheet__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  min-height: 36px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
}
.asi-sheet__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px 24px;
}

/* 답변 리스트 */
.asi-answers {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.asi-answer {
  padding: 12px;
  background: var(--color-bg);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.asi-answer--defect {
  background: var(--color-danger-tint);
  border-color: var(--color-danger-tint);
}
.asi-answer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.asi-answer__date {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.asi-answer__type {
  font-size: 11px;
  font-weight: 700;
}
.asi-answer__type.is-defect {
  color: var(--color-danger-text);
}
.asi-answer__type.is-ok {
  color: var(--color-ok-text);
}
.asi-answer__subj {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.asi-answer__desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
.asi-answer__photo {
  margin-top: 8px;
}
.asi-answer__photo img {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
.asi-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
