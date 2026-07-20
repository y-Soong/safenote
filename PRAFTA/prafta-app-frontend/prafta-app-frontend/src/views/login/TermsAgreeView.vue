<template>
  <div class="min-h-screen flex flex-col bg-white">
    <!-- 헤더 (뒤로가기 = 동의 거부 → 로그아웃) -->
    <div class="flex items-center p-4 mt-10">
      <button
        @click="fnCancel"
        class="mr-2 text-gray-600 hover:text-gray-900"
        aria-label="뒤로가기"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-8 w-8"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M15 19l-7-7 7-7"
          />
        </svg>
      </button>
    </div>

    <!-- 본문 -->
    <div class="flex-1 px-6">
      <h3 class="text-gray-800 font-semibold mb-2 text-2xl">약관에 동의해주세요</h3>
      <p class="text-gray-500 text-sm mb-6">
        서비스 이용을 위해 아래 필수 약관에 동의가 필요합니다.
      </p>

      <!-- 로딩 -->
      <div v-if="isLoading" class="text-gray-400 text-sm py-10 text-center">불러오는 중...</div>

      <template v-else>
        <!-- 전체 동의하기 -->
        <label class="flex p-4 bg-gray-100 rounded-md">
          <input type="checkbox" class="hidden" v-model="allChecked" @click="fnAllClick" />
          <span
            class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2 rounded-md"
            :class="allChecked ? 'bg-green-500 border-green-500' : 'bg-transparent'"
          >
            <svg
              v-if="allChecked"
              xmlns="http://www.w3.org/2000/svg"
              class="h-4 w-4 text-white"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="3"
                d="M5 13l4 4L19 7"
              />
            </svg>
          </span>
          <span>전체 동의하기</span>
        </label>

        <!-- 세부 필수약관 -->
        <div class="form-container pt-5">
          <label
            class="flex items-center cursor-pointer select-none mb-4"
            v-for="terms in termsList"
            :key="terms.termsId"
          >
            <input type="checkbox" v-model="terms.checked" class="hidden" />
            <span
              class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2"
              :class="terms.checked ? 'bg-green-500 border-green-500' : 'bg-transparent'"
            >
              <svg
                v-if="terms.checked"
                xmlns="http://www.w3.org/2000/svg"
                class="h-4 w-4 text-white"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="3"
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </span>
            <span>{{ '(필수) ' + terms.termsNm }}</span>

            <!-- (보기): 기존 TermsDetail 재사용 -->
            <button
              type="button"
              class="text-green-600 hover:underline"
              @click.stop="fnViewTerms(terms)"
            >
              (보기)
            </button>
          </label>
        </div>
      </template>
    </div>

    <!-- 하단 버튼 -->
    <div class="p-6">
      <button
        class="w-full text-white py-3 rounded-md transition"
        :class="
          allRequiredChecked ? 'bg-green-600 hover:bg-green-700' : 'bg-gray-300 cursor-not-allowed'
        "
        :disabled="!allRequiredChecked || isSubmitting"
        @click="fnAgree"
      >
        동의하고 시작
      </button>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '@/api/axios'
import { forceLogout } from '@/composables/useAuth'
import { useUserStore } from '@/stores/userStore'
// PRAFTA-SUBCON-T4: 필수약관 통과 후 제3자 제공 동의 게이트(②)로 합류한다.
//   routeAfterLogin 을 부르면 ①(필수약관)을 재조회하므로 ②부터 시작하는 전용 함수를 쓴다.
import { routeAfterRequiredTerms } from '@/utils/termsGate'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

// 공통: alert 폴백
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
const allChecked = ref(false)
const termsList = ref([])
// 라우트 이탈 가드 우회 플래그(동의 완료 / 로그아웃 완료 후 통과).
let bypassGuard = false
// redirect 목적지(history state 로 전달; 없으면 /MainView).
const redirect = ref('/MainView')

// 모든 필수약관 체크 여부 — "동의하고 시작" 활성 조건.
const allRequiredChecked = computed(
  () => termsList.value.length > 0 && termsList.value.every((t) => t.checked),
)

onMounted(async () => {
  // history state 의 목록을 우선 사용하되, 새로고침으로 유실 대비 항상 서버에서 재조회한다.
  const state = window.history.state || {}
  if (state.redirect) redirect.value = state.redirect

  await fnLoadPending()
})

// 미동의 필수약관 재조회. 빈 목록이면 게이트 불필요 → 곧바로 목적지 진입.
const fnLoadPending = async () => {
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/terms01/required-terms-pending')
    const pending = Array.isArray(data?.terms) ? data.terms : []

    if (pending.length === 0) {
      // 동의할 필수약관 없음 → 게이트 통과(SUBCON-T4: 제3자 제공 동의 게이트 판정으로 합류).
      bypassGuard = true
      await routeAfterRequiredTerms(router, redirect.value)
      return
    }

    termsList.value = pending.map((t) => ({
      termsId: t.termsId,
      termsNm: t.termsNm,
      termsVersion: t.termsVersion,
      checked: false,
    }))
  } catch (e) {
    // 조회 실패는 가용성 우선으로 통과(다음 진입 시 재시도). 전체 화면 에러로 키우지 않음.
    console.warn('[TermsAgree] 필수약관 조회 실패(통과 처리):', e?.message)
    bypassGuard = true
    router.replace(redirect.value)
  } finally {
    isLoading.value = false
  }
}

// 전체 동의 토글.
function fnAllClick() {
  const next = !allChecked.value
  termsList.value.forEach((t) => {
    t.checked = next
  })
}

// (보기): 기존 TermsDetail 재사용(query termsId_p/termsNm_p).
function fnViewTerms(terms) {
  router.push({
    path: '/TermsDetail',
    query: {
      termsId_p: terms.termsId,
      termsNm_p: terms.termsNm,
    },
  })
}

// 동의하고 시작: 서버가 미동의 필수약관을 재산출하여 전부 동의 처리(멱등). 성공 시 목적지 진입.
const fnAgree = async () => {
  if (!allRequiredChecked.value || isSubmitting.value) return
  isSubmitting.value = true
  try {
    await api.post('/appApi/terms01/agree-required-terms')
    bypassGuard = true
    // SUBCON-T4: 필수약관 동의 완료 → 제3자 제공 동의 게이트 판정 후 목적지 진입.
    await routeAfterRequiredTerms(router, redirect.value)
  } catch (e) {
    await showAlert(e?.response?.data?.message || '약관 동의 처리에 실패했습니다.\n잠시 후 다시 시도해 주세요.')
  } finally {
    isSubmitting.value = false
  }
}

// 동의 거부/취소 → 로그아웃 후 로그인 화면 복귀(미동의 상태로 진입 불가).
const fnLogout = async () => {
  await forceLogout()
  try {
    userStore.logout()
  } catch (e) {
    console.warn('[TermsAgree] userStore logout skip:', e?.message)
  }
}

const fnCancel = async () => {
  const ok = await showConfirm(
    '필수 약관에 동의하지 않으면 서비스를 이용할 수 없어요.\n로그아웃하고 로그인 화면으로 돌아갈까요?',
  )
  if (!ok) return
  await fnLogout()
  bypassGuard = true
  router.replace('/')
}

// 뒤로가기/라우트 이탈 가드 — 동의/통과/로그아웃이 아닌 이탈은 로그아웃 후 허용.
onBeforeRouteLeave(async (to, from, next) => {
  if (bypassGuard) {
    next()
    return
  }
  // 방어: 세션이 이미 없으면(다른 API 의 AUTH_403_001 등으로 인터셉터가 강제 로그아웃한 뒤의
  //   리다이렉트) 사용자의 자발적 이탈이 아니다 — confirm 없이 통과시킨다(오발동 방지).
  if (!sessionStorage.getItem('token')) {
    next()
    return
  }
  const ok = await showConfirm(
    '필수 약관에 동의하지 않으면 서비스를 이용할 수 없어요.\n로그아웃하고 로그인 화면으로 돌아갈까요?',
  )
  if (ok) {
    await fnLogout()
    bypassGuard = true
    next({ path: '/' })
  } else {
    next(false)
  }
})
</script>

<style scoped>
/* TermsInfo.vue 와 동일하게 tailwind 유틸 기반(별도 scoped 규칙 불요). */
</style>
