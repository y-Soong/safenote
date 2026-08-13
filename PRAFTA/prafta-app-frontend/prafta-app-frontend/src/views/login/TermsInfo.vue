<template>
  <div class="min-h-screen flex flex-col bg-white">
    <div class="flex items-center p-4 mt-10">
      <button @click="goBack" class="mr-2 text-gray-600 hover:text-gray-900">
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

    <!-- 전체동의하기 -->
    <div class="flex-1 px-6">
      <h3 class="text-gray-800 font-semibold mb-6 text-2xl">약관에 동의해주세요</h3>

      <label class="flex p-4 bg-gray-100 rounded-md terms-row terms-row--box">
        <input type="checkbox" class="hidden" v-model="checked" @click="fnAllClick" />
        <span
          class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2 rounded-md"
          :class="checked ? 'bg-green-500 border-green-500' : 'bg-transparent'"
        >
          <svg
            v-if="checked"
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

      <!-- 세부약관 내용 -->
      <div class="form-container pt-5">
        <label
          class="flex items-center cursor-pointer select-none mb-4 terms-row"
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

          <!-- 오른쪽 영역: (보기) 버튼 -->
          <button
            type="button"
            class="text-green-600 hover:underline"
            @click.stop="fnViewTerms(terms)"
          >
            (보기)
          </button>
        </label>
      </div>
    </div>

    <!-- 하단 버튼 영역 -->
    <div class="p-6">
      <button
        class="w-full bg-green-600 text-white py-3 rounded-md hover:bg-green-700 transition"
        @click="fnJoinUser()"
      >
        다음
      </button>
    </div>
  </div>
</template>
<script setup>
/* eslint-disable */
import { ref, getCurrentInstance, onMounted, defineEmits } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['close'])

const router = useRouter()

const termsList = ref([])

const checked = ref(false)

onMounted(async () => {
  await fnGetSystinfoList()
})

// API 호출
//
// ★필수약관 목록은 서버(TB_TERMS.REQUIRED_YN='Y')가 판정한다.
//   종전에는 SYS008 코드표(syst-info-lists)를 받아 "SYS008 에 있으면 전부 필수"로 그렸다.
//   그 탓에 선택약관인 006(연동 회사 제3자 제공 동의)까지 필수 체크를 강요했고, 006 은
//   가입 시 저장되지 않는 약관이라 로그인 후 게이트(termsGate ②)가 다시 물었다.
//   006 은 연동 사업장 소속자에게만 묻는 게이트 전용 약관이므로 가입 화면에서 제외한다.
//   여기를 코드표 기반으로 되돌리지 말 것(중복 동의 재발).
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/join-terms-lists')

    if (response.status === 200) {
      termsList.value = (response.data?.joinTermsList || []).map((o) => ({
        ...o,
        checked: false, // 각 항목별 체크 상태 추가
      }))
    }
  } catch (err) {
    // prafta-036-A: 옵셔널 체이닝으로 NPE 안전화
    proxy.$alert(err.response?.data?.message || '약관 코드 정보를 불러올 수 없습니다.')
  }
}

/* User Function */
function fnJoinUser() {
  // 목록이 비면(조회 실패) 통과시키지 않는다 — 종전 forEach 판정은 빈 배열에서 joinFlg 가
  //   true 로 남아 약관을 하나도 못 본 채 가입으로 넘어갔다(fail-open).
  if (termsList.value.length === 0) {
    proxy.$alert('약관 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
    return
  }

  const joinFlg = termsList.value.every((terms) => terms.checked)

  if (joinFlg) {
    // ★동의 결과는 history state 로 넘긴다(TermsAgreeView 와 동일 방식).
    //   종전에는 query 로 넘겼는데, ① JoinUser 가 useRoute 조차 import 하지 않아 읽지 않았고
    //   ② query 는 문자열만 담아 객체 배열이 "[object Object]" 로 뭉개지는 이중 결함이었다.
    //   그 탓에 동의 기록이 사용자의 체크가 아니라 서버의 가정('Y' 리터럴)으로 만들어졌다.
    router.push({
      path: '/JoinUser',
      state: {
        agrTermsList: termsList.value.map((terms) => ({ termsId: terms.termsId })),
      },
    })
  } else {
    proxy.$alert('동의하지 않은 필수 약관이 있습니다.')
  }
}

function fnAllClick() {
  termsList.value.forEach((terms) => {
    terms.checked = !checked.value
  })
}

function fnViewTerms(terms) {
  router.push({
    path: '/TermsDetail',
    query: {
      termsId_p: terms.termsId,
      termsNm_p: terms.termsNm,
    },
  })
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
/* 동의 행 눌림 피드백 — TermsAgreeView.vue 와 동일 규칙(전역 label tap-highlight 제거 보완).
   Tailwind 2 는 active variant 가 기본 비활성이라 scoped 규칙으로 처리한다. */
.terms-row {
  transition: background-color 0.15s ease, opacity 0.15s ease;
}

/* 회색 박스형(전체 동의): 배경을 한 단계 진하게 */
.terms-row--box:active {
  background-color: #e5e7eb; /* tailwind gray-200 */
}

/* 배경 없는 행(세부 약관): 레이아웃 변화 없이 살짝 흐리게 */
.terms-row:not(.terms-row--box):active {
  opacity: 0.6;
}
</style>
