<!--
  TbmCompletedDetailView.vue — TBM 교육완료 상세(내 이력)
  - 작업 ID: PRAFTA-TBM-DONE-DETAIL (분해: prafta-app-tbm-user-detail-plan.md §4 F8, §3 A10)
  - 진입: 교육완료 탭 카드 선택 → router.push('/TbmCompletedDetail?sessionCd=...')
  - 백엔드: GET /appApi/tbm/sessions/{sessionCd}/my-completion (A10)
    응답: { title, contentBody, materialTitles:[], riskTitles:[], mySignFileMgmtCd, completionStatusCd, endedAt }
  - 표시: 제목 / 교육내용 / 교육자료'명만' / 위험성평가'제목만' / 내 서명 이미지 / 이수상태.
  - 디자인 토큰: TbmEntryView 세트를 .tbm-done-view 루트에 1회 선언.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
-->
<template>
  <div class="tbm-done-view">
    <!-- 헤더 -->
    <header class="tbm-hd">
      <button type="button" class="tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-tbm-chev-left" />
        </svg>
      </button>
      <h1 class="tbm-hd__title">교육 이력</h1>
      <span class="tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main class="tbm-done-body">
      <p v-if="isLoading" class="tbm-state">불러오는 중…</p>

      <div v-else-if="loadError" class="tbm-state">
        <p class="tbm-state__msg">정보를 불러오지 못했어요.</p>
        <button type="button" class="tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else>
        <!-- 제목 + 이수상태 -->
        <div class="tbm-done__head">
          <h2 class="tbm-done__title">{{ detail.title || 'TBM 세션' }}</h2>
          <span v-if="completionLabel" class="badge" :class="completionToneClass">
            {{ completionLabel }}
          </span>
        </div>
        <p v-if="detail.endedAt" class="tbm-done__date">{{ detail.endedAt }} 종료</p>

        <!-- 교육 내용 -->
        <section class="card">
          <p class="card__label">교육 내용</p>
          <!-- contentBody: 관리자 입력 리치 HTML. 교육내용 HTML 은 서버(AppTbm01ServiceImpl, Jsoup Safelist.relaxed)
               에서 저장형 XSS sanitize 후 전달됨. 프론트는 별도 sanitize 미적용(이중 방어 아님, 서버 단일 방어). -->
          <div class="tbm-content" v-html="detail.contentBody"></div>
        </section>

        <!-- 교육 자료(명만) -->
        <section class="card">
          <p class="card__label">교육 자료</p>
          <ul v-if="detail.materialTitles && detail.materialTitles.length" class="name-list">
            <li v-for="(t, i) in detail.materialTitles" :key="i" class="name-list__item">
              {{ t }}
            </li>
          </ul>
          <p v-else class="tbm-state tbm-state--sm">등록된 자료가 없어요</p>
        </section>

        <!-- 위험성평가(제목만) -->
        <section class="card">
          <p class="card__label">위험성평가</p>
          <ul v-if="detail.riskTitles && detail.riskTitles.length" class="name-list">
            <li v-for="(t, i) in detail.riskTitles" :key="i" class="name-list__item">
              {{ t }}
            </li>
          </ul>
          <p v-else class="tbm-state tbm-state--sm">연계된 위험성평가가 없어요</p>
        </section>

        <!-- 내 서명 -->
        <!-- mySignUrl: 서명 이미지 서명 절대 URL(FileUrlSigner). 자료 미리보기와 동일 인프라.
             URL 이 있으면 인라인 렌더(릴리즈 APK·브라우저). dev 서버 모드(https 페이지 + http 파일 =
             mixed content)에서는 앱 웹뷰가 인라인 이미지를 막으므로, TbmMaterialSlider 와 동일하게
             onSignError → '서명 이미지 열기' 외부 열기 링크로 폴백한다. -->
        <section class="card">
          <p class="card__label">내 서명</p>
          <div class="sign-view">
            <img
              v-if="detail.mySignUrl && !signImgError"
              class="sign-view__img"
              :src="detail.mySignUrl"
              alt="내 서명"
              @error="onSignError"
            />
            <!-- 인라인 렌더 실패(앱 웹뷰 mixed-content 등) 시 외부 열기 링크 폴백(자료 슬라이더와 동일 패턴) -->
            <a
              v-else-if="detail.mySignUrl"
              class="sign-view__link"
              :href="detail.mySignUrl"
              target="_blank"
              rel="noopener noreferrer"
            >서명 이미지 열기</a>
            <p v-else-if="detail.mySignFileMgmtCd" class="tbm-state tbm-state--sm">
              서명 완료 (이미지를 불러올 수 없어요)
            </p>
            <p v-else class="tbm-state tbm-state--sm">서명 정보가 없어요</p>
          </div>
        </section>
      </template>
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-tbm-chev-left"
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

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 우선, 없으면 window)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 반응형 상태(developer: 조회 결과로 채움) ──────────────────────
const isLoading = ref(false)
const loadError = ref(false)
// 서명 이미지 로드 실패(서명 URL 만료/네트워크) 시 플레이스홀더 폴백 플래그
const signImgError = ref(false)

// 완료 상세(A10):
//  { title, contentBody, materialTitles:[], riskTitles:[], mySignFileMgmtCd, mySignUrl, completionStatusCd, endedAt }
//  mySignUrl: 서명 이미지 서명 절대 URL(FileUrlSigner). 없으면 '서명 정보가 없어요'.
const detail = ref({
  title: '',
  contentBody: '',
  materialTitles: [],
  riskTitles: [],
  mySignFileMgmtCd: '',
  mySignUrl: '',
  completionStatusCd: '',
  endedAt: '',
})

const completionLabel = computed(() => {
  switch (detail.value.completionStatusCd) {
    case 'COMPLETED':
      return '이수'
    case 'NOT_COMPLETED':
      return '미이수'
    default:
      return ''
  }
})
const completionToneClass = computed(() =>
  detail.value.completionStatusCd === 'COMPLETED' ? 'badge--ok' : 'badge--danger',
)

// 완료 상세 조회 — GET /appApi/tbm/sessions/{sessionCd}/my-completion (A10)
const loadDetail = async () => {
  const sessionCd = route.query.sessionCd || ''
  if (!sessionCd) return
  isLoading.value = true
  loadError.value = false
  signImgError.value = false
  try {
    const { data } = await api.get(`/appApi/tbm/sessions/${sessionCd}/my-completion`)
    detail.value = {
      title: data?.title || '',
      contentBody: data?.contentBody || '',
      materialTitles: Array.isArray(data?.materialTitles) ? data.materialTitles : [],
      riskTitles: Array.isArray(data?.riskTitles) ? data.riskTitles : [],
      mySignFileMgmtCd: data?.mySignFileMgmtCd || '',
      // 서명 이미지 절대 URL(FileUrlSigner). 없으면 빈 문자열.
      mySignUrl: data?.mySignUrl || '',
      completionStatusCd: data?.completionStatusCd || '',
      endedAt: data?.endedAt || '',
    }
  } catch (e) {
    console.error('[TbmCompletedDetail] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/TbmHub')
}
const onRetry = () => {
  loadDetail()
}
// 서명 이미지 로드 실패 → 플레이스홀더 폴백
const onSignError = () => {
  signImgError.value = true
}

// ── 진입 ────────────────────────────────────────────────────────
onMounted(() => {
  const sessionCd = route.query.sessionCd || ''
  if (!sessionCd) {
    showAlert('세션 정보가 없어 화면을 열 수 없어요.')
    router.replace('/TbmHub')
    return
  }
  loadDetail()
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(TbmEntryView 세트와 동일) */
.tbm-done-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
}

/* 헤더 */
.tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tbm-hd__back {
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
.tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.tbm-hd__spacer {
  width: 36px;
}

/* 본문 */
.tbm-done-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.tbm-state--sm {
  margin: var(--space-sm) 0;
  font-size: 13px;
  text-align: left;
}
.tbm-state__msg {
  margin: 0 0 var(--space-sm);
}
.tbm-retry {
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

/* 제목/상태 */
.tbm-done__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.tbm-done__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.tbm-done__date {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.badge--ok {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.badge--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger-text);
}

/* 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__label {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 교육 내용(리치 HTML) */
.tbm-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  word-break: break-word;
}
.tbm-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-md);
}
.tbm-content :deep(p) {
  margin: 0 0 var(--space-sm);
}

/* 이름 리스트(자료명/위험성 제목) */
.name-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.name-list__item {
  padding: 10px 0;
  border-bottom: 0.5px solid var(--color-border-light);
  font-size: 14px;
  color: var(--color-text-primary);
}
.name-list__item:last-child {
  border-bottom: 0;
}

/* 서명 이미지 */
.sign-view {
  width: 100%;
  min-height: 120px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.sign-view__img {
  max-width: 100%;
  height: auto;
  display: block;
}
/* 인라인 렌더 실패 시 외부 열기 링크(자료 슬라이더 .mtrl-slide__link 와 동일 톤) */
.sign-view__link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

/* 스프라이트 */
.tbm-sprite {
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
