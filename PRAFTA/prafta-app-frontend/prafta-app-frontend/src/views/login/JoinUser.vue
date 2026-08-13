<template>
  <div class="min-h-screen flex flex-col bg-white overflow-hidden">
    <div class="flex items-center p-4 mt-10 flex-shrink-0">
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

    <div class="flex-1 overflow-y-auto px-4 sm:px-6 pb-4">
      <h3 class="text-gray-800 font-semibold mb-3 text-xl sm:text-2xl">회원가입</h3>

      <div class="mt-5 relative">
        <label class="block mb-1 font-medium text-gray-700">
          회사코드
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-2">
        <input
          id="cmpnyCd"
          ref="cmpnyCdFcs"
          v-model="cmpnyCd"
          placeholder="회사코드"
          :disabled="!cmpnyCdDisabled"
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />

        <button
          class="px-4 py-3 border border-green-600 text-green-600 rounded-md hover:bg-green-50 transition whitespace-nowrap text-sm sm:text-base"
          @click="cmpnyInfoSrch"
        >
          회사코드 확인
        </button>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          회사명
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          v-model="cmpnyNm"
          placeholder="회사명"
          disabled
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          아이디
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          id="userId"
          ref="userIdFcs"
          v-model="userId"
          @blur="focusKill"
          :disabled="cmpnyCdDisabled"
          minlength="4"
          maxlength="10"
          placeholder="4 ~ 10자"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          비밀번호
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="relative" v-if="!cmpnyCdDisabled">
        <input
          id="userPw"
          ref="userPwFcs"
          v-model="userPw"
          type="password"
          @blur="focusKill"
          :disabled="cmpnyCdDisabled"
          placeholder="6 ~ 15자"
          class="w-full px-4 py-3 pr-12 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
        <!-- APP-PRAFTA-001: top-1/2 -translate-y-1/2 는 이모지 베이스라인 때문에 약간 아래로 쳐진다.
             인증번호 확인(flex items-center)과 동일하게 입력 높이를 채워 세로 중앙 정렬한다. -->
        <span class="form-msg absolute right-3 top-0 bottom-0 flex items-center">{{ pwMsg }}</span>
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          비밀번호 확인
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="relative" v-if="!cmpnyCdDisabled">
        <input
          id="userPwConfirm"
          ref="userPwConfirmFcs"
          v-model="userPwConfirm"
          type="password"
          @blur="focusKill"
          :disabled="cmpnyCdDisabled"
          placeholder="6 ~ 15자"
          class="w-full px-4 py-3 pr-12 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
        <!-- APP-PRAFTA-001: 비밀번호 확인 체크 아이콘도 동일하게 세로 중앙 정렬. -->
        <span class="form-msg absolute right-3 top-0 bottom-0 flex items-center">
          {{ pwConfirmMsg }}
        </span>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          이름
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          id="userNm"
          ref="userNmFcs"
          v-model="userNm"
          placeholder="최대15자리"
          maxlength="15"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          휴대폰 번호
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div
        class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-2"
        v-if="!cmpnyCdDisabled"
      >
        <input
          id="mblNo"
          ref="mblNoFcs"
          v-model="mblNo"
          @blur="focusKill"
          :disabled="mblNoDisabled"
          placeholder="최대12자리"
          maxlength="12"
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />

        <button
          ref="smsAuthReqBtnFcs"
          class="px-4 py-3 border border-green-600 text-green-600 rounded-md hover:bg-green-50 transition whitespace-nowrap text-sm sm:text-base"
          @click="fnSmsAuthReq"
          :disabled="timer > 0"
          v-show="btnAuthChkDisabled"
        >
          {{ timer > 0 ? `${timer}초 후 재요청` : '인증요청' }}
        </button>
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          인증번호
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <!-- APP-PRAFTA-001: 다른 버튼 행(회사코드/휴대폰)과 동일한 반응형 래퍼를 사용해
           모바일 웹뷰(<640px)에서 입력↔"확인" 버튼이 동일하게 세로 스택되고 우측 끝단이 정렬되도록 한다.
           인증 성공 체크 아이콘은 비밀번호 입력과 동일하게 input 안쪽 우측에 얹는다
           (absolute 기준점이 되도록 input 을 relative 래퍼로 감쌌다). -->
      <div
        class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-2"
        v-if="!cmpnyCdDisabled"
      >
        <div class="relative flex-1">
          <input
            id="certNo"
            ref="certNoFcs"
            v-model="certNo"
            placeholder="인증번호6자리"
            maxlength="6"
            :disabled="mblNoDisabled"
            class="w-full px-4 py-3 pr-12 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
          />
          <span
            class="form-msg absolute right-3 top-0 bottom-0 flex items-center"
            v-show="smsCertNoChk"
          >
            ✅
          </span>
        </div>

        <button
          class="px-4 py-3 border border-green-600 text-green-600 rounded-md hover:bg-green-50 transition whitespace-nowrap text-sm sm:text-base"
          @click="fnSmsAuthChk"
          v-show="btnAuthChkDisabled"
        >
          확인
        </button>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700"> 이메일 </label>
        <input
          id="email"
          ref="emailFcs"
          v-model="email"
          @blur="focusKill"
          placeholder="이메일"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700"> 성별 </label>
        <select
          id="gender"
          ref="genderFcs"
          v-model="gender"
          class="w-full py-3 px-4 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        >
          <option
            v-for="opt in (systCodeArr['SYS004'] || []).filter((o) => o.systValDCd != null)"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          사업장
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div
        class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-2"
        v-if="!cmpnyCdDisabled"
      >
        <input
          v-model="siteNm"
          placeholder="사업장"
          disabled
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />

        <button
          id="siteSrchBtn"
          ref="siteSrchBtnFcs"
          class="px-4 py-3 border border-green-600 text-green-600 rounded-md hover:bg-green-50 transition whitespace-nowrap text-sm sm:text-base"
          @click="openSiteSearch"
        >
          찾기
        </button>
      </div>

      <!-- prafta-039: 웹 회원가입과 동기화 위해 소속부서 항목 추가 (선택값) -->
      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700"> 소속부서 </label>
      </div>

      <div
        class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-2"
        v-if="!cmpnyCdDisabled"
      >
        <input
          v-model="nodeNm"
          placeholder="소속부서"
          disabled
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />

        <button
          id="nodeSrchBtn"
          ref="nodeSrchBtnFcs"
          class="px-4 py-3 border border-green-600 text-green-600 rounded-md hover:bg-green-50 transition whitespace-nowrap text-sm sm:text-base disabled:opacity-50 disabled:cursor-not-allowed"
          @click="openNodeSearch"
          :disabled="nodeDisabled"
        >
          찾기
        </button>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          생년월일
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          id="birthDt"
          ref="birthDtFcs"
          v-model="birthDt"
          placeholder="YYMMDD"
          minlength="6"
          maxlength="6"
          @blur="focusKill"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 text-sm sm:text-base"
        />
      </div>
    </div>

    <div class="p-4 sm:p-6 flex-shrink-0" v-if="!cmpnyCdDisabled">
      <button
        class="w-full bg-green-600 text-white py-3 rounded-md hover:bg-green-700 transition text-sm sm:text-base"
        @click="fnUserJoin()"
      >
        가입하기
      </button>
    </div>

    <!-- ✅ 공통 SidePanel 컴포넌트 -->
    <SidePanel
      :visible="showSitePanel"
      title="사업장 찾기"
      :showSearch="true"
      :hasResults="siteList.length > 0"
      :isLoading="isLoading"
      :isError="isError"
      :items="siteList"
      keyField="siteCd"
      labelField="siteNm"
      :multiple="false"
      v-model="selectedSites"
      @close="showSitePanel = false"
      @search="fetchSiteList"
      @confirm="selectSites"
    />

    <!-- prafta-039: 소속부서 찾기 SidePanel (사업장 선택 후 활성화) -->
    <SidePanel
      :visible="showNodePanel"
      title="소속부서 찾기"
      :showSearch="true"
      :hasResults="nodeList.length > 0"
      :isLoading="isNodeLoading"
      :isError="isNodeError"
      :items="nodeList"
      keyField="nodeCd"
      labelField="nodeNm"
      :multiple="false"
      v-model="selectedNodes"
      @close="showNodePanel = false"
      @search="fetchNodeList"
      @confirm="selectNodes"
    />
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, getCurrentInstance, onUnmounted, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
// 소정-12: 셀프가입 승인제 — 가입 직후 승인대기 안내 분기(판별 규칙은 utils/joinApproval.js).
import { resolveJoinApprovalStatus, JOIN_APPROVAL_PENDING } from '@/utils/joinApproval'
import SidePanel from '@/components/common/SidePanel.vue'

const { proxy } = getCurrentInstance()

/* side panel 변수 */
const showSitePanel = ref(false)
const isLoading = ref(false)
const isError = ref(false)
const siteList = ref([])
const selectedSites = ref([]) // ✅ v-model로 연결됨

/* prafta-039: 소속부서 side panel 변수 */
const showNodePanel = ref(false)
const isNodeLoading = ref(false)
const isNodeError = ref(false)
const nodeList = ref([])
const selectedNodes = ref([])

const router = useRouter()

// 앞 단계(TermsInfo)에서 동의한 약관 목록 — [{ termsId }] 형태. 가입 제출 시 서버로 보낸다.
//   ★서버는 이 목록에 필수약관이 모두 들어 있는지 검증한다(LoginServiceImpl.assertJoinTermsAgreed).
//   약관 화면을 거치지 않고 이 화면에 직접 들어오면 빈 배열이 되고, 그때는 서버가
//   구버전 클라이언트로 보고 통과시킨다(과도기). 넘겨받은 값을 임의로 만들어 채우지 말 것.
const agrTermsList = ref(
  Array.isArray(window.history.state?.agrTermsList) ? window.history.state.agrTermsList : [],
)

const systCodeArr = ref({})
const cmpnyCd = ref('')
const cmpnyNm = ref('')
const userId = ref('')
const userPw = ref('')
const userPwConfirm = ref('')
const userNm = ref('')
const mblNo = ref('')
const certNo = ref('')
const email = ref('')
const gender = ref('100')
const siteCd = ref('')
const siteNo = ref('')
const siteNm = ref('')
const nodeCd = ref('')
const nodeNm = ref('')
const birthDt = ref('')
const pwMsg = ref('')
const pwConfirmMsg = ref('')
const userIdMsg = ref('')
const smsCertNoChk = ref(false)

/* focus 변수 */
const cmpnyCdFcs = ref(null)
const userIdFcs = ref(null)
const userPwConfirmFcs = ref(null)
const userPwFcs = ref(null)
const userNmFcs = ref(null)
const mblNoFcs = ref(null)
const smsAuthReqBtnFcs = ref(null)
const certNoFcs = ref(null)
const emailFcs = ref(null)
const genderFcs = ref(null)
const siteSrchBtnFcs = ref(null)
const nodeSrchBtnFcs = ref(null)
const birthDtFcs = ref(null)

/* disabled 변수 */
const cmpnyCdDisabled = ref(true)
const btnAuthChkDisabled = ref(true)
const siteDisabled = ref(true)
const nodeDisabled = ref(true)
const mblNoDisabled = ref(false)

/* 타이머 변수 */
const timer = ref(0)
let timerInterval = null

watch(userPw, (newVal) => {
  if (proxy.$util.validatePasswordRule(newVal)) {
    pwMsg.value = '✅'
  } else {
    pwMsg.value = '❌'
  }
})

watch(userPwConfirm, (newVal) => {
  if (newVal == userPw.value) {
    pwConfirmMsg.value = '✅'
  } else {
    pwConfirmMsg.value = '❌'
  }
})

onMounted(async () => {
  await fnGetSystinfoList()
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})

// focusKill 이벤트
const focusKill = (e) => {
  if (e.target.id == 'userId') {
    if (proxy.$util.isNotEmpty(userId.value)) {
      userIdFocusKill()
    }
  } else if (e.target.id == 'userPw') {
    if (proxy.$util.isNotEmpty(userPw.value)) {
      userPwFocusKill()
    }
  } else if (e.target.id == 'userPwConfirm') {
    if (proxy.$util.isNotEmpty(userPwConfirm.value)) {
      userPwConfirmFocusKill()
    }
  } else if (e.target.id == 'mblNo') {
    if (proxy.$util.isNotEmpty(mblNo.value)) {
      mblNoFocusKill()
    }
  } else if (e.target.id == 'email') {
    if (proxy.$util.isNotEmpty(email.value)) {
      emailFocusKill()
    }
  } else if (e.target.id == 'birthDt') {
    if (proxy.$util.isNotEmpty(birthDt.value)) {
      birthDtFocusKill()
    }
  }
}

const userIdFocusKill = () => {
  if (userId.value.length < 4 || userId.value.length > 20) {
    fnAlertMsg('아이디는 4 ~ 20자 사이의 값만 입력 가능합니다.')
    userId.value = ''
  } else {
    fnUserIdDupleChk()
  }
}

const userPwFocusKill = () => {
  if (userPw.value != userPwConfirm.value) {
    userPwConfirm.value = ''
  }

  if (!proxy.$util.validatePasswordRule(userPw.value)) {
    const alertMsg =
      '비밀번호는 숫자, 영문자, 특수문자 중 2가지\n이상을 포함하여 6~15자로 작성해 주세요.'
    fnAlertMsg(alertMsg, () => {
      userPw.value = ''
      userPwFcs.value.focus()
    })
  }
}

const userPwConfirmFocusKill = () => {
  if (userPw.value != userPwConfirm.value) {
    const alertMsg = '비밀번호를 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      userPwConfirm.value = ''
      userPwConfirmFcs.value.focus()
    })
  }
}

const mblNoFocusKill = () => {
  if (proxy.$util.validatePhoneNumber(mblNo.value)) {
    mblNo.value = proxy.$util.formatPhoneNumber(mblNo.value)
    smsAuthReqBtnFcs.value.focus()
  } else {
    const alertMsg = '휴대폰 번호를 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      mblNo.value = ''
      mblNoFcs.value.focus()
    })
  }
}

const emailFocusKill = () => {
  if (proxy.$util.validateEmail(email.value)) {
    genderFcs.value.focus()
  } else {
    const alertMsg = '이메일주소를 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      email.value = ''
      emailFcs.value.focus()
    })
  }
}

const birthDtFocusKill = () => {
  if (proxy.$util.isEmpty(birthDt.value) || proxy.$util.isValidbirthDtdate(birthDt.value)) {
    const alertMsg = '생년월일을 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      birthDt.value = ''
      birthDtFcs.value.focus()
    })
  }
}

// API 호출
const fnGetSystinfoList = async () => {
  const systCodeList = ['SYS004']

  try {
    // prafta-036-A: 백엔드 케밥 정렬에 맞춰 URL 변경 (syst-info-list → syst-info-lists)
    const response = await axios.get('/comApi/baseinfo/syst-info-lists', {
      params: {
        systCodeList: systCodeList,
      },
    })

    if (response.status === 200) {
      const resData = response.data?.systInfoList || []

      const grouped = {}
      resData.forEach((item) => {
        const key = item.systValCd
        if (!grouped[key]) {
          grouped[key] = []
        }
        grouped[key].push(item)
      })

      systCodeArr.value = grouped
    }
  } catch (err) {
    // prafta-036-A: 옵셔널 체이닝으로 NPE 안전화
    alert(err.response?.data?.message || '코드 정보를 불러올 수 없습니다.')
  }
}

const fnGetCmpnyInfo = async () => {
  if (proxy.$util.isEmpty(cmpnyCd.value)) {
    proxy.$alert('회사코드를 입력해주세요.')
    return
  }

  try {
    // prafta-036-A: 백엔드 GET + @ModelAttribute 정렬에 맞춰 POST → GET 전환
    const response = await axios.get('/comApi/baseinfo/cmpny-infos', {
      params: {
        cmpnyCd: cmpnyCd.value,
      },
    })
    if (response.status === 200) {
      // prafta-036-A: record 래핑(cmpnyInfoResult) + camelCase + useYn 명시 비교('Y' 외 비활성)
      const isUseY = response.data?.cmpnyInfoResult?.useYn === 'Y'
      cmpnyCdDisabled.value = !isUseY
      cmpnyNm.value = response.data?.cmpnyInfoResult?.cmpnyNm || ''
      siteDisabled.value = !isUseY
    }
  } catch (err) {
    // prafta-036-A: 옵셔널 체이닝으로 NPE 안전화
    fnAlertMsg(err.response?.data?.message || '회사코드를 확인할 수 없습니다.', () => {
      cmpnyCd.value = ''
      cmpnyCdFcs.value.focus()
    })
  }
}

const fnUserIdDupleChk = async () => {
  if (proxy.$util.isEmpty(userId.value)) {
    const alertMsg = '아이디를 입력해주세요.'

    fnAlertMsg(alertMsg, () => {
      userIdFcs.value.focus()
    })
  }

  try {
    // prafta-036-A: 백엔드 GET + @ModelAttribute 정렬에 맞춰 POST → GET 전환
    const response = await axios.get('/comApi/baseinfo/user-id-duple-checks', {
      params: {
        cmpnyCd: cmpnyCd.value,
        userId: userId.value,
      },
    })
    if (response.status === 200) {
      // prafta-036-A: camelCase(uniqueYn) + 엄격 비교
      if (response.data?.uniqueYn === 'N') {
        userIdMsg.value = '❌'
        const alertMsg = '이미 사용중인 아이디 입니다.'
        fnAlertMsg(alertMsg, () => {
          userId.value = ''
          userIdFcs.value.focus()
        })
      } else {
        userIdMsg.value = '✅'
      }
    }
  } catch (err) {
    // prafta-036-A: 옵셔널 체이닝으로 NPE 안전화
    fnAlertMsg(err.response?.data?.message || '아이디 중복확인 중 오류가 발생했습니다.', () => {
      cmpnyCd.value = ''
      cmpnyCdFcs.value.focus()
    })
  }
}

const fnSmsAuthReq = async () => {
  if (proxy.$util.isEmpty(mblNo.value) || !proxy.$util.validatePhoneNumber(mblNo.value)) {
    const alertMsg = '휴대폰 번호를 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      mblNo.value = ''
      mblNoFcs.value.focus()
    })
    return
  }

  try {
    // prafta-036-A: 백엔드 케밥 정렬에 맞춰 URL 변경 + cmpnyCd 본문 추가
    const response = await axios.post('/comApi/baseinfo/sms-auth-sends', {
      cmpnyCd: cmpnyCd.value,
      mblNo: mblNo.value,
      dupChkYn: 'Y',
    })

    if (response.status === 200) {
      const alertMsg = '인증번호가 발송되었습니다.'
      fnAlertMsg(alertMsg, () => {
        certNoFcs.value.focus()
      })

      // ✅ 타이머 시작
      timer.value = 60
      timerInterval = setInterval(() => {
        timer.value--
        if (timer.value <= 0) {
          clearInterval(timerInterval)
        }
      }, 1000)
    }
  } catch (err) {
    // SMS-PPURIO-08: message 유무 가드를 제거한다.
    // 발송 실패는 502/타임아웃/네트워크 단절 형태로 오는데, 가드가 있으면 알럿이 아예 뜨지 않아
    // 사용자에게는 "아무 일도 안 일어남"으로 보인다(본 작업의 목적 자체가 무산).
    // 발송 실패인데 입력한 번호를 지워 재입력을 강요하지 않는다(mblNo 초기화 제거).
    fnAlertMsg(resolveApiErrorMessage(err, '인증번호 발송에 실패했습니다.\n잠시 후 다시 시도해 주세요.'))
  }
}

const fnSmsAuthChk = async () => {
  if (proxy.$util.isEmpty(certNo.value)) {
    const alertMsg = '인증번호를 입력해주세요.'
    fnAlertMsg(alertMsg, () => {
      certNo.value = ''
      certNoFcs.value.focus()
    })
    return
  } else if (proxy.$util.isEmpty(mblNo.value)) {
    const alertMsg = '휴대폰번호를 입력해주세요.'
    fnAlertMsg(alertMsg, () => {
      mblNo.value = ''
      mblNoFcs.value.focus()
    })
    return
  }

  try {
    // prafta-036-A: 백엔드 케밥 정렬에 맞춰 URL 변경 + cmpnyCd 본문 추가
    const response = await axios.post('/comApi/baseinfo/sms-auth-checks', {
      cmpnyCd: cmpnyCd.value,
      mblNo: mblNo.value,
      certNo: certNo.value,
    })
    if (response.status === 200) {
      btnAuthChkDisabled.value = false
      mblNoDisabled.value = true
      smsCertNoChk.value = true

      const alertMsg = '인증번호가 확인되었습니다.'
      fnAlertMsg(alertMsg, () => {
        emailFcs.value.focus()
      })
    }
  } catch (err) {
    // prafta-036-A: 옵셔널 체이닝으로 NPE 안전화
    fnAlertMsg(err.response?.data?.message || '인증번호 확인에 실패했습니다.', () => {
      smsAuthReqBtnFcs.value.focus()
    })
  }
}

const fnUserJoin = async () => {
  if (!fnUserInfoValidationChk()) {
    return
  }

  try {
    // prafta-036-A: 백엔드 케밥 정렬에 맞춰 URL 변경 (insertUserInfo → insert-user-info)
    const response = await axios.post('/comApi/login/insert-user-info', {
      cmpnyCd: cmpnyCd.value,
      userId: userId.value,
      userPw: userPw.value,
      userNm: userNm.value,
      siteCd: siteCd.value,
      nodeCd: nodeCd.value,
      mblNo: mblNo.value,
      email: email.value,
      gender: gender.value,
      birthDt: birthDt.value,
      agrTermsList: agrTermsList.value,
    })
    if (response.status === 200) {
      // 소정-12: 셀프가입 승인제 — 서버가 승인대기('06') 신호를 주면 로그인 안내 대신 승인대기 화면으로.
      //   서버가 신호를 주지 않으면(승인제 미적용 형상) 기존 "로그인 해주세요" 흐름을 그대로 유지한다.
      if (resolveJoinApprovalStatus(response.data)) {
        router.replace({ path: '/JoinApprovalPending', state: { status: JOIN_APPROVAL_PENDING } })
        return
      }
      const alertMsg = '회원가입에 성공했습니다.\n로그인 해주세요.'
      fnAlertMsg(alertMsg, () => {
        router.push({
          path: '/',
          query: {
            userId: userId.value,
          },
        })
      })
    }
  } catch (err) {
    // ★서버 안내 문구를 우선 노출한다(qa F-1). 고정 문구로 덮으면 휴대폰 본인인증 만료
    //   (LOGIN_400_022 — 30분 창)·아이디/휴대폰 중복(400_016/020/021) 안내가 전부 사장돼
    //   사용자가 재인증하면 된다는 사실을 알 수 없다.
    fnAlertMsg(
      resolveApiErrorMessage(err, '회원가입에 실패했습니다.\n관리자에게 문의해주세요.')
    )
  }
}

/* User Function */
const fnUserInfoValidationChk = () => {
  let alertMsg = ''
  let retVal = true

  if (proxy.$util.isEmpty(cmpnyCd.value)) {
    alertMsg = '회사코드를 입력해주세요.'

    fnAlertMsg(alertMsg, () => {
      cmpnyCdFcs.value.focus()
    })
    retVal = false
  } else if (proxy.$util.isEmpty(userId.value)) {
    alertMsg = '아이디를 입력해주세요.'

    fnAlertMsg(alertMsg, () => {
      userIdFcs.value.focus()
    })
    retVal = false
  } else if (proxy.$util.isEmpty(userPw.value) || proxy.$util.isEmpty(userPwConfirm.value)) {
    alertMsg = '비밀번호를 확인해주세요.'

    fnAlertMsg(alertMsg, () => {
      if (proxy.$util.isEmpty(userPw.value)) {
        userPwFcs.value.focus()
      } else if (proxy.$util.isEmpty(userPwConfirm.value)) {
        userPwConfirmFcs.value.focus()
      }
    })
    retVal = false
  } else if (proxy.$util.isEmpty(userNm.value)) {
    alertMsg = '이름을 입력해주세요.'

    fnAlertMsg(alertMsg, () => {
      userNmFcs.value.focus()
    })
    retVal = false
  } else if (!smsCertNoChk.value) {
    alertMsg = '휴대폰 번호를 인증해주세요.'

    fnAlertMsg(alertMsg, () => {
      certNoFcs.value.focus()
    })
    retVal = false
  } else if (proxy.$util.isEmpty(siteCd.value)) {
    alertMsg = '사업장을 선택해주세요.'

    fnAlertMsg(alertMsg, () => {
      siteSrchBtnFcs.value.focus()
    })
    retVal = false
  } else if (proxy.$util.isEmpty(nodeCd.value)) {
    alertMsg = '소속부서를 선택해주세요.'

    fnAlertMsg(alertMsg, () => {
      nodeSrchBtnFcs.value.focus()
    })
    retVal = false
  } else if (proxy.$util.isEmpty(birthDt.value)) {
    alertMsg = '생년월일을 인증해주세요.'

    fnAlertMsg(alertMsg, () => {
      birthDtFcs.value.focus()
    })
    retVal = false
  }

  return retVal
}

const cmpnyInfoSrch = async () => {
  await fnGetCmpnyInfo()

  if (proxy.$util.isNotEmpty(cmpnyNm.value)) {
    userIdFcs.value.focus()
  }
}

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message)
  if (afterConfirmCallback) {
    afterConfirmCallback()
  }
}

const goBack = () => {
  router.back()
}

const openSiteSearch = () => {
  showSitePanel.value = true
  fetchSiteList()
}

const fetchSiteList = async (keyword) => {
  isLoading.value = true
  isError.value = false
  try {
    // 회원가입(비로그인) 단계 — /site-lists 는 토큰 필수라 401. NoAuth 변형 endpoint 호출.
    const response = await axios.get('/comApi/baseinfo/join-site-lists', {
      params: {
        cmpnyCd: cmpnyCd.value,
        siteNm: keyword || '',
      },
    })

    if (response.status === 200) {
      // 응답 스키마는 /site-lists 와 동일 ({ siteInfoResultList: [...] })
      siteList.value = response.data?.siteInfoResultList || []
    }
  } catch (err) {
    isError.value = true
  } finally {
    isLoading.value = false
  }
}

const selectSites = (selected) => {
  // prafta-036-A: 응답 키 camelCase 전환 (SITE_CD/SITE_NO/SITE_NM → siteCd/siteNo/siteNm)
  siteCd.value = selected.siteCd
  siteNo.value = selected.siteNo
  siteNm.value = selected.siteNm
  // prafta-039: 사업장 선택 시 소속부서 검색 활성화 + 기존 부서 선택 초기화
  nodeDisabled.value = false
  nodeCd.value = ''
  nodeNm.value = ''
  showSitePanel.value = false
}

// prafta-039: 소속부서 검색 (웹 JoinUserPop과 동기화, 사업장 선택 후 호출)
const openNodeSearch = () => {
  showNodePanel.value = true
  fetchNodeList()
}

const fetchNodeList = async (keyword) => {
  isNodeLoading.value = true
  isNodeError.value = false
  try {
    // 회원가입(비로그인) 단계 — NoAuth 변형 endpoint 호출.
    const response = await axios.get('/comApi/baseinfo/join-site-node-lists', {
      params: {
        cmpnyCd: cmpnyCd.value,
        siteCd: siteCd.value,
        nodeNm: keyword || '',
      },
    })

    if (response.status === 200) {
      // 응답 스키마는 /site-node-lists 와 동일 ({ siteNodeInfoList: [...] })
      nodeList.value = response.data?.siteNodeInfoList || []
    }
  } catch (err) {
    isNodeError.value = true
  } finally {
    isNodeLoading.value = false
  }
}

const selectNodes = (selected) => {
  nodeCd.value = selected.nodeCd
  nodeNm.value = selected.nodeNm
  showNodePanel.value = false
}
</script>

<style scoped>
.form-msg {
  font-size: 0.875rem;
  min-width: 2rem;
  text-align: center;
}

@media (max-width: 640px) {
  .form-msg {
    font-size: 0.75rem;
  }
}
</style>
