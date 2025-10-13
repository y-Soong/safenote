<template>
  <div class="min-h-screen flex flex-col bg-white">
    <!-- 상단 네비게이션 -->
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

    <!-- 본문 (입력 영역) -->
    <div class="flex-1 px-6">
      <h3 class="text-gray-800 font-semibold mb-6 text-2xl">
        이름과 휴대폰 번호를<br />입력해 주세요.
      </h3>

      <!-- 이름 입력 -->
      <div class="mt-20 mb-4 relative">
        <input
          id="userNm"
          type="text"
          ref="userNmFcs"
          v-model="userNm"
          placeholder="사용자명"
          :disabled="certShowFlg"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <button
          v-show="userNm"
          @click="userNm = ''"
          type="button"
          :disabled="certShowFlg"
          class="absolute top-1/4 right-3 -translate-y-1/4 text-gray-400 hover:text-gray-600"
        >
          ✕
        </button>
      </div>

      <!-- 휴대폰 번호 입력 -->
      <div class="mb-4 relative">
        <input
          id="mblNo"
          ref="mblNoFcs"
          v-model="mblNo"
          @blur="focusKill"
          placeholder="휴대폰 번호"
          :disabled="certShowFlg"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <button
          v-show="mblNo"
          @click="mblNo = ''"
          type="button"
          :disabled="certShowFlg"
          class="absolute top-1/4 right-3 -translate-y-1/4 text-gray-400 hover:text-gray-600"
        >
          ✕
        </button>
      </div>

      <!-- 인증번호 입력 -->
      <div class="mb-4 relative">
        <input
          id="certNo"
          type="text"
          v-model="certNo"
          ref="certNoFcs"
          maxlength="6"
          placeholder="인증번호 6자리"
          v-show="certShowFlg"
          :disabled="userIdShowFlg"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <button
          class="absolute top-1/4 right-3 -translate-y-1/4 text-red"
          v-show="certShowFlg && !userIdShowFlg"
          :disabled="timer > 0"
        >
          {{ timer > 0 ? `${timer}` : '인증시간만료' }}
        </button>
      </div>

      <div class="mb-4 relative">
        <input
          id="userId"
          v-model="userId"
          placeholder="아이디"
          disabled
          v-show="userIdShowFlg"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
      </div>

      <!-- 비밀번호 입력 -->
      <div class="mb-4 relative">
        <input
          id="userPw"
          ref="userPwFcs"
          type="password"
          v-model="userPw"
          @blur="focusKill"
          placeholder="비밀번호"
          v-show="userPwChgShowFlg"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <button
          v-show="userPw"
          @click="userPw = ''"
          type="button"
          class="absolute top-1/4 right-3 -translate-y-1/4 text-gray-400 hover:text-gray-600"
        >
          ✕
        </button>
      </div>

      <!-- 비밀번호 확인 입력 -->
      <div class="mb-4 relative">
        <input
          id="userPwConfirm"
          ref="userPwConfirmFcs"
          type="password"
          v-model="userPwConfirm"
          @blur="focusKill"
          placeholder="비밀번호 확인"
          v-show="userPwChgShowFlg"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <button
          v-show="userPwConfirm"
          @click="userPwConfirm = ''"
          type="button"
          class="absolute top-1/4 right-3 -translate-y-1/4 text-gray-400 hover:text-gray-600"
        >
          ✕
        </button>
      </div>
      <span class="form-msg">{{ pwConfirmMsg }}</span>
    </div>

    <!-- 하단 버튼 영역 -->
    <div class="p-6">
      <button
        @click="fnUserPwReset"
        class="w-full mb-4 bg-white text-green-700 border border-green-700 py-3 rounded-md hover:bg-green-50 transition"
        v-if="userIdShowFlg"
      >
        {{ btnPwRstType == '0' ? '비밀번호 초기화' : '저장' }}
      </button>

      <button
        @click="fnBtnTypeClick"
        class="w-full bg-green-700 text-white py-3 rounded-md hover:bg-green-800 transition"
      >
        {{
          btnType == '0'
            ? '인증번호 발송'
            : btnType == '1'
            ? '인증번호 확인'
            : btnType == '2'
            ? '인증번호 재발송'
            : '로그인'
        }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, getCurrentInstance, onUnmounted, defineEmits } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['close'])

const router = useRouter()
const cmpnyCd = ref('')
const userId = ref('')
const userNm = ref('')
const userNmFcs = ref(null)
const mblNo = ref('')
const mblNoFcs = ref(null)

const certNo = ref('')
const certNoFcs = ref(false)

const userPw = ref('')
const userPwFcs = ref('')
const userPwConfirm = ref('')
const userPwConfirmFcs = ref('')

const btnType = ref('0')
const btnPwRstType = ref('0')

const certShowFlg = ref(false)
const userIdShowFlg = ref(false)
const userPwChgShowFlg = ref(false)

const smsAuthReqBtnFcs = ref('')

const pwConfirmMsg = ref('')

/* 타이머 변수 */
const timer = ref(0)
let timerInterval = null

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})

watch(userPwConfirm, (newVal) => {
  if (newVal == userPw.value) {
    pwConfirmMsg.value = '✅'
  } else {
    pwConfirmMsg.value = '❌'
  }
})

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == 'mblNo') {
    if (proxy.$util.isNotEmpty(mblNo.value)) {
      mblNoFocusKill()
    }
  } else if (e.target.id == 'userPw') {
    if (proxy.$util.isNotEmpty(userPw.value)) {
      userPwFocusKill()
    }
  } else if (e.target.id == 'userPwConfirm') {
    if (proxy.$util.isNotEmpty(userPwConfirm.value)) {
      userPwConfirmFocusKill()
    }
  }
}

// API 호출
const fnUserIdSrch = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/getUserIdInfo', {
      userNm: userNm.value,
      mblNo: mblNo.value,
    })
    if (response.status === 200) {
      userId.value = response.data.USER_ID
      cmpnyCd.value = response.data.CMPNY_CD
      fnSmsAuthReqApi()
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message)
  }
}

const fnSmsAuthReqApi = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/getSmsAuthReq', {
      mblNo: mblNo.value,
      dupChkYn: 'N',
    })

    if (response.status === 200) {
      const alertMsg = '인증번호가 발송되었습니다.'
      fnAlertMsg(alertMsg, () => {
        certNoFcs.value.focus()
      })

      certShowFlg.value = true
      certNoFcs.value.focus()
      btnType.value = '1' /* 인증번호 확인 */

      // ✅ 타이머 시작
      timer.value = 60
      timerInterval = setInterval(() => {
        timer.value--
        if (timer.value <= 0) {
          clearInterval(timerInterval)
          if (btnType.value != '3') {
            btnType.value = '2' /* 인증번호 재발송 */
            cmpnyCd.value = ''
            userId.value = ''
            certShowFlg.value = false
          }
        }
      }, 1000)
    }
  } catch (err) {
    // const alertMsg = "인증번호 발송에 실패했습니다.\n관리자에게 문의해주세요.";
    if (err.response?.data?.message) {
      mblNo.value = ''
      fnAlertMsg(err.response.data.message)
    }
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
  }

  try {
    const response = await axios.post('/comApi/baseinfo/getSmsAuthChk', {
      mblNo: mblNo.value,
      certNo: certNo.value,
    })
    if (response.status === 200) {
      const alertMsg = '인증되었습니다.'
      fnAlertMsg(alertMsg, () => {
        certNoFcs.value.focus()
      })

      userIdShowFlg.value = true
      btnType.value = '3'
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      certNoFcs.value.focus()
    })
  }
}

const fnUserPwUpdate = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/updateUserPw', {
      cmpnyCd: cmpnyCd.value,
      userId: userId.value,
      userPw: userPwConfirm.value,
    })
    if (response.status === 200) {
      const alertMsg = '비밀번호가 재성정 되었습니다.'
      fnAlertMsg(alertMsg, () => {
        router.push({
          path: '/',
          query: {
            userId: userId.value,
          },
        })
      })

      emit('close')
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      smsAuthReqBtnFcs.value.focus()
    })
  }
}

/* User Function */
function fnUserPwReset() {
  if (btnPwRstType.value == '0') {
    userPwChgShowFlg.value = true
    btnPwRstType.value = '1' /* 저장 */
  } else if (btnPwRstType.value == '1') {
    if (proxy.$util.isEmpty(userPw.value) || proxy.$util.isEmpty(userPwConfirm.value)) {
      const alertMsg = '비밀번호를 확인해주세요.'

      fnAlertMsg(alertMsg, () => {
        if (proxy.$util.isEmpty(userPw.value)) {
          userPwFcs.value.focus()
        } else if (proxy.$util.isEmpty(userPwConfirm.value)) {
          userPwConfirmFcs.value.focus()
        }
      })
      return
    }
    fnConfirmMsg('비밀번호를 재설정 하시겠습니까 ?', async () => {
      fnUserPwUpdate()
    })
  }
}

function userPwFocusKill() {
  if (userPw.value != userPwConfirm.value) {
    userPwConfirm.value = ''
  }

  if (!proxy.$util.validatePasswordRule(userPw.value)) {
    const alertMsg = '숫자, 영문자, 특수문자 중 2가지 이상을\n포함하여 6~15자로 작성해 주세요.'
    fnAlertMsg(alertMsg, () => {
      userPw.value = ''
      userPwFcs.value.focus()
    })
  }
}

function userPwConfirmFocusKill() {
  if (userPw.value != userPwConfirm.value) {
    const alertMsg = '비밀번호를 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      userPwConfirm.value = ''
      userPwConfirmFcs.value.focus()
    })
  }
}

function fnBtnTypeClick() {
  if (btnType.value == '0' || btnType.value == '2') {
    fnUserIdSrch()
  } else if (btnType.value == '1') {
    if (timer.value > 0) {
      fnSmsAuthChk()
    } else {
      const alertMsg = '인증 시간이 만료되었습니다.'
      fnAlertMsg(alertMsg, () => {
        certNoFcs.value.focus()
      })
    }
  } else if (btnType.value == '3') {
    router.push({
      path: '/',
      query: {
        userId: userId.value,
      },
    })
  }
}

function mblNoFocusKill() {
  if (proxy.$util.validatePhoneNumber(mblNo.value)) {
    mblNo.value = proxy.$util.formatPhoneNumber(mblNo.value)
  } else {
    const alertMsg = '휴대폰 번호를 확인해주세요.'
    fnAlertMsg(alertMsg, () => {
      mblNo.value = ''
      mblNoFcs.value.focus()
    })
  }
}

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message)
  if (afterConfirmCallback) {
    afterConfirmCallback()
  }
}

async function fnConfirmMsg(message, afterConfirmCallback) {
  const result = await proxy.$confirm(message)
  if (result && afterConfirmCallback) {
    afterConfirmCallback() // ✅ 확인 눌렀을 때만 실행
  }
}

// 뒤로가기 (로그인 화면으로 이동)
const goBack = () => {
  router.back()
}
</script>
