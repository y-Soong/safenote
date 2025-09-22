<template>
  <div class="min-h-screen flex items-center justify-center bg-[#afeeee] px-4">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-md px-8 py-10">
      <!-- 로고 + 타이틀 -->
      <div class="flex flex-col items-center mb-8">
        <img src="@/assets/logo.png" alt="CleanNote Logo" class="w-20 h-auto mb-4" />
        <h2 class="text-3xl font-extrabold text-gray-800">Welcome</h2>
        <p class="text-sm text-gray-500 mt-1 text-center">Login to your cleaning service account</p>
      </div>

      <!-- 로그인 폼 -->
      <form class="flex flex-col gap-4" @submit.prevent="submitLogin">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">아이디</label>
          <input
            type="userid"
            v-model="userId"
            class="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-[#afeeee]"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
          <input
            type="password"
            v-model="userPw"
            class="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-[#afeeee]"
          />
        </div>
        <button
          type="submit"
          class="w-full bg-[#afeeee] text-white font-semibold py-2 rounded-md hover:bg-[#9bdede] transition"
        >
          Login
        </button>
      </form>

      <p class="mt-6 text-center text-sm text-gray-500">
        Don’t have an account?
        <a href="#" class="text-[#53bfbf] font-medium hover:underline">Sign up</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from '@/api/axios'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
// import { useGlobalUserStore } from '@/stores/globalUserStore'

const userId = ref('')
const userPw = ref('')
const router = useRouter()
const userSotre = useUserStore();

const submitLogin = async () => {
  console.log('axios baseURL', axios.defaults.baseURL)
  console.log(userId.value)
  console.log(userPw.value)

  try {
    const response = await axios.post('/api/login', {
      userId: userId.value,
      userPw: userPw.value,
      userNm: '윤순기'
    })

    if (response.status == "200") {
      console.log("Success ###")
      console.log(response);

      sessionStorage.setItem('jwt', response.data.token)
      sessionStorage.setItem('token', response.data.token)
      sessionStorage.setItem('userId', response.data.userId)
      sessionStorage.setItem('userNm', response.data.userNm)
      sessionStorage.setItem('cmpnyCd', response.data.cmpnyCd)


      userSotre.setUser({
        userId : response.data.userId
        , userNm : response.data.userNm
        , cmpnyCd : response.data.cmpnyCd
      })

      router.push('/main') // 로그인 성공 → 메인화면 이동
    }
  } catch (err) {
    alert(err.response.data.message);
  }
}
</script>
