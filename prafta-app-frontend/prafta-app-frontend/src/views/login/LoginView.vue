<template>
  <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50">
    <!-- 로그인 박스 -->
    <img :src="safenote_logo" class="pt-10 pb-10 w-56" alt="logo" />

    <!-- 로그인 박스 -->
    <div class="w-full max-w-sm bg-white rounded-2xl shadow-md p-8">
      <!-- 아이디 입력 -->
      <div class="mb-4">
        <input
          id="userId"
          type="text"
          v-model="userId"
          placeholder="아이디"
          @blur="focusKill"
          class="w-full px-4 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
        />
      </div>

      <!-- 비밀번호 입력 -->
      <div class="mb-1">
        <input
          type="password"
          v-model="password"
          placeholder="비밀번호"
          class="w-full px-4 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
        />
      </div>

      <!-- 에러 메시지 -->
      <p v-if="errorMessage" class="text-red-500 text-sm mb-3">
        {{ errorMessage }}
      </p>

      <!-- 아이디 저장 -->
      <div class="flex items-center mb-4 pt-1">
        <input id="rememberId" type="checkbox" v-model="rememberId" class="mr-2 pt-1" />
        <label for="rememberId" class="text-sm text-gray-700">아이디 저장</label>
      </div>

      <!-- 로그인 버튼 -->
      <button
        @click="fnSubmitLogin"
        class="w-full bg-green-700 text-white py-2 rounded-md hover:bg-green-800 transition"
      >
        로그인
      </button>

      <!-- 아이디 찾기 / 비밀번호 초기화 -->
      <div class="flex justify-center items-center mt-4 text-sm text-gray-600 help-links">
        <a class="aTagCls" href="#" @click.prevent="acountInfoSrch">아이디/비밀번호 찾기</a>
      </div>
    </div>

    <!-- 가입하기 영역 -->
    <div class="w-full max-w-sm mt-6 text-center pl-8 pr-8">
      <p class="text-sm text-gray-700 mb-2" style="color: #084538">SAFE NOTE 가 처음이신가요?</p>
      <button
        class="w-full py-2 border border-green-700 text-green-700 rounded-md hover:bg-green-50 transition"
        @click="fnOpenTerms"
      >
        가입하기
      </button>
    </div>

    <!-- Footer -->
    <footer class="mt-10 text-center text-xs text-gray-500">
      <p class="font-semibold">YW LOGIS</p>
      <p>고객센터 1234-5678</p>
      <p>© 순기량여진이랑 INC. ALL RIGHTS RESERVED.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import safenote_logo from '@/assets/img/safenote_sign.png'
import axios from '@/api/axios'

const userId = ref('')
const password = ref('')
const rememberId = ref(false)
const router = useRouter()
const route = useRoute()
const errorMessage = ref('')

const { proxy } = getCurrentInstance()

onMounted(() => {
  if (proxy.$util.isNotEmpty(route) && proxy.$util.isNotEmpty(route.query.userId)) {
    userId.value = route.query.userId
  }

  const savedId = localStorage.getItem('savedUserId')
  if (savedId) {
    userId.value = savedId
    rememberId.value = true
  }
})

function focusKill(e) {
  if (e.target.id == 'userId') {
    if (proxy.$util.isNotEmpty(userId.value)) {
      userIdFocusKill()
    }
  }
}

/* API Call */
const fnSubmitLogin = async () => {
  if (!userId.value || !password.value) {
    await proxy.$alert('아이디와 비밀번호를 모두 입력해주세요')
    return
  }

  try {
    const response = await axios.post('/comApi/login/loginChk', {
      userId: userId.value,
      userPw: password.value,
    })

    if (response.status === 200) {
      const { token, userId: id, userNm, cmpnyCd, authCd, mblNo, email } = response.data

      // ✅ 로그인 토큰 세팅
      sessionStorage.setItem('jwt', token)
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('gv_cmpnyCd', cmpnyCd)
      sessionStorage.setItem('gv_userId', id)
      sessionStorage.setItem('gv_userNm', userNm)
      sessionStorage.setItem('gv_authCd', authCd)
      sessionStorage.setItem('gv_mblNo', mblNo)
      sessionStorage.setItem('gv_email', email)
      // userStore.setUser({ userId: id, userNm, cmpnyCd, authCd, mblNo, email });

      // ✅ 아이디 저장 처리
      if (rememberId.value) {
        localStorage.setItem('savedUserId', userId.value)
      } else {
        localStorage.removeItem('savedUserId')
      }

      /* 약관동의 체크 */
      //fnUserTermsAgrChk()

      router.push('/MainView')
    }
  } catch (err) {
    await proxy.$alert(err.response?.data?.message || '로그인에 실패했습니다.')
  }
}

// const fnUserTermsAgrChk = async () => {
//   try {
//     const response = await axios.post("/comApi/login/getUserTermsAgrChk", {
//       userId: userId.value,
//     });

//     if (response.status === 200) {
//       userTermsNonAgrList.value = response.data;

//       if (response.data.length > 0) {
//         openPop(TermsPop, {
//           loginFlg_p: "Y",
//           userTermsNonAgrList_p: userTermsNonAgrList.value,
//           onMoveMain: fnMoveMainPath,
//           onfnUserLogout: fnUserLogout,
//         });
//         // fnUserLogout();
//       } else {
//         fnMoveMainPath();
//       }
//     }
//   } catch (err) {
//     await proxy.$alert(err.response?.data?.message || "로그인에 실패했습니다.");
//   }
// };

/* User Function */
function userIdFocusKill() {
  userId.value = proxy.$util.toUpperCase(userId.value)
}

function fnOpenTerms() {
  router.push('/TermsInfo')
}

function acountInfoSrch() {
  router.push('/ActInfoSrch')
}
</script>

<style scoped>
/* 필요 시 추가 커스텀 스타일 */
.help-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  list-style: none;
  padding: 0;
  margin: 0.5rem;
  font-size: 0.85rem;
}

.help-links a {
  color: #084538;
  text-decoration: none;
}

.help-links li {
  color: #084538;
}

.aTagCls:hover {
  color: #30796a;
}
</style>
