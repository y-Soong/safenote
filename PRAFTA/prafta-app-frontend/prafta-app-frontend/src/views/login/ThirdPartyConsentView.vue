<!--
  ThirdPartyConsentView — 연동 회사 제3자 제공 동의 게이트(PRAFTA-SUBCON-T4-05).
  - 진입: routeAfterLogin()/routeAfterRequiredTerms() 가 GET /appApi/terms01/subcon-consent-gate 로 판정 후 replace 진입.
  - ★ 기존 TermsAgreeView(필수약관)와 결정적으로 다르다: 미동의도 정상 통과(로그아웃 없음, 이탈 가드 없음).
  - 응답(동의/미동의) 저장 = 게이트 해제. 응답하지 않고 닫으면 다음 로그인에 재노출된다.
  - 약관 본문/요약은 서버(TB_TERMS)에서 받아 렌더한다 — 코드에 약관 문구를 하드코딩하지 않는다.
  - 디자인: ForcedPasswordChangeView 패턴(루트 클래스 CSS 변수 블록 + scoped).
-->
<template>
  <div class="tpc-view">
    <!-- 헤더 (닫기 = 응답 없이 통과 — 강제 게이트 아님) -->
    <header class="tpc-hd">
      <h1 class="tpc-hd__title">연동 회사 자료 제공 동의</h1>
      <button type="button" class="tpc-hd__close" aria-label="닫기" @click="onSkip">✕</button>
    </header>

    <main class="tpc-body">
      <div v-if="isLoading" class="tpc-loading">불러오는 중...</div>

      <template v-else>
        <!-- 안내 노트: 자유의사 고지(강제 아님) -->
        <div class="tpc-notice">
          이 사업장은 다른 회사와 연동되어 있어요.<br />
          동의하지 않아도 서비스 이용에 제한이 없습니다.
        </div>

        <!-- 약관 제목 + 전문 보기 -->
        <div class="tpc-terms-head">
          <span class="tpc-terms-head__label">{{ '(선택) ' + termsNm }}</span>
          <button type="button" class="tpc-terms-head__view" @click="onViewTerms">전문 보기</button>
        </div>

        <!-- 약관 요약(서버 TERMS_DESC — v-html 금지) -->
        <p class="tpc-summary">{{ termsDesc }}</p>

        <!-- 철회/소급 없음 고지 -->
        <p class="tpc-hint">
          동의는 마이페이지에서 언제든 철회할 수 있어요.<br />
          다만 철회 전 이미 제공된 자료는 회수되지 않습니다.
        </p>
      </template>
    </main>

    <!-- 하단 고정 버튼: 동의/미동의 둘 다 선택 가능 -->
    <footer class="tpc-foot">
      <button
        type="button"
        class="tpc-btn tpc-btn--ghost"
        :disabled="isLoading || isSubmitting"
        @click="onDisagree"
      >
        동의하지 않음
      </button>
      <button
        type="button"
        class="tpc-btn tpc-btn--primary"
        :disabled="isLoading || isSubmitting"
        @click="onAgree"
      >
        동의합니다
      </button>
    </footer>
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const { proxy } = getCurrentInstance()
const router = useRouter()

// alert / confirm 폴백(TermsAgreeView 패턴).
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const showConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// 상태
const isLoading = ref(true)
const isSubmitting = ref(false)
const termsId = ref('')
const termsNm = ref('')
const termsDesc = ref('')
// 응답 후 복귀할 목적지(history state 로 전달, 없으면 /MainView).
const redirect = ref('/MainView')

onMounted(async () => {
  const state = window.history.state || {}
  if (state.redirect) redirect.value = state.redirect
  await fnLoadGate()
})

// 게이트 재조회(새로고침/직접진입 대비 — 화면이 스스로 필요 여부를 판정한다).
//   gateRequiredYn !== 'Y' → 화면 미표시 통과. 조회 실패 → 가용성 우선 통과($alert 금지).
const fnLoadGate = async () => {
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/terms01/subcon-consent-gate')

    if (data?.gateRequiredYn !== 'Y') {
      // 약관 미배포 / 비연동 사업장 / 이미 응답함 → 그릴 필요 없음.
      router.replace(redirect.value)
      return
    }

    termsId.value = data.termsId || ''
    termsNm.value = data.termsNm || ''
    termsDesc.value = data.termsDesc || ''
  } catch (e) {
    // 게이트 조회 실패는 비치명적: 진입을 막지 않는다(다음 로그인에 재시도).
    console.warn('[ThirdPartyConsent] 게이트 조회 실패(통과 처리):', e?.message)
    router.replace(redirect.value)
  } finally {
    isLoading.value = false
  }
}

// 응답 저장(동의/미동의 공통). 성공 시에만 목적지 진입 — 실패하면 화면에 머문다(미응답 = 다음 로그인 재노출).
//   ★ termsId 는 전송하지 않는다(서버 상수 006 고정 — 임의 약관 토글 주입면 제거).
const fnRespond = async (agrYn) => {
  if (isSubmitting.value) return
  isSubmitting.value = true
  try {
    await api.post('/appApi/terms01/subcon-consent-respond', { agrYn })
    router.replace(redirect.value)
  } catch (e) {
    await showAlert(
      e?.response?.data?.message || '동의 처리에 실패했어요.\n잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isSubmitting.value = false
  }
}

const onAgree = () => fnRespond('Y')

// 미동의: 확인 후 'N' 저장(로그아웃 없음 — 필수약관 게이트와 다른 흐름).
const onDisagree = async () => {
  const ok = await showConfirm(
    '동의하지 않아도 서비스 이용에는 제한이 없어요.\n마이페이지에서 언제든 동의로 변경할 수 있습니다.\n계속할까요?',
  )
  if (!ok) return
  await fnRespond('N')
}

// 닫기: 응답 없이 통과(의사표시가 없으므로 미동의로 기록하지 않는다 → 다음 로그인에 재노출).
const onSkip = () => {
  router.replace(redirect.value)
}

// 전문 보기: 기존 TermsDetail 재사용(query termsId_p/termsNm_p).
const onViewTerms = () => {
  router.push({
    path: '/TermsDetail',
    query: {
      termsId_p: termsId.value,
      termsNm_p: termsNm.value,
    },
  })
}
</script>

<style scoped>
.tpc-view {
  --color-primary: #16a34a;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-info-strong: #1d4ed8;
  --color-info-tint: #eff6ff;
  --radius-md: 10px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.tpc-hd {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tpc-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.tpc-hd__close {
  width: 44px;
  height: 44px;
  margin-right: -10px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: 18px;
  font-family: inherit;
}

/* 본문 */
.tpc-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-lg) 96px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.tpc-loading {
  padding: 40px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

/* 안내 노트 */
.tpc-notice {
  padding: var(--space-md);
  background: var(--color-info-tint);
  color: var(--color-info-strong);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
}

/* 약관 제목 행 */
.tpc-terms-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  margin-top: var(--space-xs);
}
.tpc-terms-head__label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.tpc-terms-head__view {
  flex-shrink: 0;
  background: transparent;
  border: 0;
  padding: 0;
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: underline;
  cursor: pointer;
  font-family: inherit;
}

/* 약관 요약 / 힌트 */
.tpc-summary {
  margin: 0;
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  white-space: pre-line;
}
.tpc-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-tertiary);
}

/* 하단 고정 버튼 */
.tpc-foot {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
}
.tpc-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.tpc-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.tpc-btn--ghost {
  background: var(--color-surface);
  border-color: var(--color-border);
  color: var(--color-text-secondary);
}
.tpc-btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
</style>
