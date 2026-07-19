<!--
  MyContractView.vue — 내 서명 근로계약서 열람 (앱, 일용직 전용)
  - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-04 / §2 T4
  - 요청서 근거: §5-3(마이페이지 진입점), §6-1(근로기준법 §17② 교부 의무 — 열람/저장은 필수 스코프)
  - 진입: MyPageView 메뉴 "내 근로계약서"(일용직 gv_employmentType==='DAILY' 에게만 노출, developer 추가)
      → router.push('/MyContract') (보호 라우트)
  - 참조 패턴: MyPageView(헤더/토큰), DailyContractSignView(이미지 뷰어)
  - planner 라운드 스코프: template + style 완성. script 는 선언 + TODO(developer).
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/dailycontract01/my-sign — 최신 서명본 메타(contractVer, signDtime, firstWorkDate)
      (2) GET /appApi/dailycontract01/my-sign-image (blob) — 합성본 이미지 로드
      (3) [이미지 저장] — blob 다운로드(a[download]) 또는 Web Share API(웹뷰 환경 동작 확인 필요)
      (4) /MyContract 보호 라우트 등록 + MyPageView 메뉴 행 추가
-->
<template>
  <div class="my-contract-view">
    <!-- 헤더 -->
    <header class="mc-hd">
      <button type="button" class="mc-hd__back" aria-label="뒤로" @click="onBack">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h1 class="mc-hd__title">내 근로계약서</h1>
      <span class="mc-hd__spacer" aria-hidden="true"></span>
    </header>

    <main class="mc-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="mc-state" aria-live="polite">불러오는 중...</div>

      <!-- 빈 상태: 서명본 없음 -->
      <div v-else-if="!hasSign" class="mc-state">
        <p>서명한 계약서가 없습니다.</p>
        <p class="mc-state__sub">입장 승인 후 첫 로그인 시 계약서에 서명하게 됩니다.</p>
      </div>

      <!-- 로드 실패 -->
      <div v-else-if="loadFailed" class="mc-state mc-state--error">
        <p>계약서를 불러오지 못했습니다.</p>
        <button type="button" class="mc-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else>
        <!-- 서명 메타 카드 -->
        <section class="mc-meta" aria-label="서명 정보">
          <dl class="mc-meta__rows">
            <div class="mc-meta__row">
              <dt>서명일시</dt>
              <dd>{{ signDtime }}</dd>
            </div>
            <div class="mc-meta__row">
              <dt>최초 근로일</dt>
              <dd>{{ firstWorkDate }}</dd>
            </div>
            <div class="mc-meta__row">
              <dt>계약서 버전</dt>
              <dd>v{{ contractVer }}</dd>
            </div>
          </dl>
        </section>

        <!-- 합성본 이미지 뷰어 -->
        <div class="mc-doc">
          <img class="mc-doc__img" :src="signImageUrl" alt="서명된 근로계약서" />
        </div>
      </template>
    </main>

    <!-- 하단: 저장(교부 의무 이행 수단) -->
    <footer v-if="hasSign && !isLoading && !loadFailed" class="mc-ft">
      <button type="button" class="mc-ft__btn" :disabled="isSaving" @click="onSave">
        {{ isSaving ? '저장 중...' : '이미지 저장' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MyPageView 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 ────────────────────────────────────────────────────────────
const isLoading = ref(true)
const loadFailed = ref(false)
const isSaving = ref(false)

// 서명본 존재 여부 + 메타
const hasSign = ref(false)
const contractVer = ref('')
const signDtime = ref('')
const firstWorkDate = ref('')

// 합성본 이미지 objectURL + 저장용 blob 보관(이미지 저장 시 재다운로드 없이 재사용)
const signImageUrl = ref('')
let signImageBlob = null

// YYYYMMDD → YYYY.MM.DD 표기(메타 카드용). 형식 불일치 시 원문 그대로.
const formatYmd = (ymd) => {
  const s = String(ymd || '')
  if (!/^\d{8}$/.test(s)) return s
  return `${s.slice(0, 4)}.${s.slice(4, 6)}.${s.slice(6, 8)}`
}

// ── 조회 ─────────────────────────────────────────────────────────────
// 1) my-sign 메타(signYn='N'=빈 상태) → 2) my-sign-image 합성본 스트림(blob).
const loadMySign = async () => {
  isLoading.value = true
  loadFailed.value = false
  try {
    const { data } = await api.get('/appApi/dailycontract01/my-sign')

    if (data?.signYn !== 'Y') {
      // 서명본 없음 → 빈 상태 화면(승인 후 첫 로그인 시 서명 안내).
      hasSign.value = false
      isLoading.value = false
      return
    }

    hasSign.value = true
    contractVer.value = data.contractVer ?? ''
    signDtime.value = data.signDtime || ''
    firstWorkDate.value = formatYmd(data.firstWorkDate)

    // 합성본 이미지 스트림(경로 미노출 — 인증 헤더는 api 인스턴스가 동봉).
    const { data: blob } = await api.get('/appApi/dailycontract01/my-sign-image', {
      responseType: 'blob',
    })
    if (signImageUrl.value) URL.revokeObjectURL(signImageUrl.value)
    signImageBlob = blob
    signImageUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    console.warn('[MyContract] 계약서 조회 실패:', e?.message)
    // 메타/이미지 어느 단계든 실패 → 에러 상태(재시도 버튼). 존재 미확정이어도 재시도 유도가 안전.
    hasSign.value = true
    loadFailed.value = true
  } finally {
    isLoading.value = false
  }
}

// ── 라이프사이클 ─────────────────────────────────────────────────────
onMounted(() => {
  loadMySign()
})

onUnmounted(() => {
  // objectURL 정리(메모리 누수 방지).
  if (signImageUrl.value) URL.revokeObjectURL(signImageUrl.value)
})

const onRetry = () => {
  loadMySign()
}

const onBack = () => {
  router.back()
}

// [이미지 저장] — 교부 의무(§6-1) 이행 수단. 이미 로드된 합성본 blob 을 a[download] 로 저장.
//   웹뷰(Flutter 셸)에서 blob 다운로드 미지원 기기가 있을 수 있음(실기기 확인 대상 — qa 인계).
const onSave = async () => {
  if (isSaving.value || !signImageBlob) return
  isSaving.value = true
  try {
    const ymd = String(firstWorkDate.value || '').replaceAll('.', '')
    const fileName = `근로계약서_${ymd || 'sign'}.png`

    const url = URL.createObjectURL(signImageBlob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    // 다운로드 트리거 후 지연 해제(즉시 revoke 시 일부 웹뷰에서 저장 실패).
    setTimeout(() => URL.revokeObjectURL(url), 10000)
  } catch (e) {
    console.warn('[MyContract] 이미지 저장 실패:', e?.message)
    await showAlert('이미지 저장에 실패했습니다. 화면을 캡처하여 보관해 주세요.')
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.my-contract-view {
  /* 디자인 토큰 자급(MyPageView 세트 미러) — 하드코딩 사용 금지 */
  --color-primary: #16a34a;
  --color-danger-text: #b91c1c;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --radius-md: 10px;
  --radius-lg: 14px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-bg);
}

/* 헤더 */
.mc-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.mc-hd__back {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.mc-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.mc-hd__spacer {
  width: 32px;
}

/* 본문 */
.mc-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--space-md) var(--space-lg);
  gap: var(--space-md);
  min-height: 0;
}

.mc-state {
  padding: 48px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.mc-state__sub {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  opacity: 0.8;
}
.mc-state--error {
  color: var(--color-danger-text);
}
.mc-retry {
  margin-top: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
}

/* 서명 메타 카드 */
.mc-meta {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-md) var(--space-lg);
}
.mc-meta__rows {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.mc-meta__row {
  display: flex;
  gap: var(--space-sm);
  font-size: 13px;
}
.mc-meta__row dt {
  flex: 0 0 84px;
  color: var(--color-text-secondary);
}
.mc-meta__row dd {
  margin: 0;
  color: var(--color-text-primary);
  font-weight: 600;
}

/* 합성본 이미지 뷰어 */
.mc-doc {
  flex: 1;
  overflow-y: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}
.mc-doc__img {
  display: block;
  width: 100%;
  height: auto;
}

/* 하단 저장 */
.mc-ft {
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.mc-ft__btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.mc-ft__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
