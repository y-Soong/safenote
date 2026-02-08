<template>
  <div class="min-h-screen flex flex-col bg-gray-50 p-5">
    <!-- 상단 영역 -->
    <div class="flex justify-between items-center mt-6 relative">
      <div class="w-full text-xl">
        <p class="text-green-700 font-semibold">{{ userNm }}님</p>
        <p class="text-lg font-bold">안녕하세요</p>
      </div>

      <!-- 프로필 아이콘 + 유저 메뉴 -->
      <div class="relative" ref="userMenuWrapper">
        <button
          class="flex w-12 h-12 justify-center items-center bg-gray-100 rounded-full"
          @click="toggleUserMenu"
        >
          <img src="@/assets/img/user_icon.png" alt="user-icon" class="w-12 h-12 object-contain" />
        </button>

        <!-- User Menu -->
        <transition name="fade">
          <div
            v-if="userMenuOpen"
            class="absolute bottom-0 left-0 transform -translate-x-full translate-y-full w-32 bg-white border rounded-lg shadow-lg z-50"
          >
            <button
              class="block w-full text-left px-4 py-2 text-sm hover:bg-gray-100"
              @click.stop="goToMyInfo"
            >
              내정보
            </button>
            <button
              class="block w-full text-left px-4 py-2 text-sm hover:bg-gray-100 border-t"
              @click.stop="logout"
            >
              로그아웃
            </button>
          </div>
        </transition>
      </div>
    </div>

    <p class="w-full text-base text-gray-600 mt-2">
      <span class="text-green-700 font-semibold">점검률 83%</span> 거의 완료하였습니다!<br />
      남은 점검을 마무리 해 주세요.
    </p>
    <!-- 진행률 바 -->
    <div class="w-full bg-gray-200 rounded-full h-2 mt-2">
      <div class="bg-green-700 h-2 rounded-full" style="width: 83%"></div>
    </div>

    <!-- 주요 기능 버튼 -->
    <div class="grid grid-cols-2 gap-3 mt-5">
      <!-- ① 일일 안전점검 -->
      <button
        class="bg-white pt-5 rounded-2xl shadow flex flex-col items-center hover:bg-gray-100 transition"
        @click="fnDayChkLst"
      >
        <div class="relative w-24 h-24 flex justify-center items-center mb-2">
          <!-- 바닥 아이콘 (fullscreen) -->
          <img
            src="@/assets/img/vector_icon.png"
            alt="fullscreen-icon"
            class="w-24 h-24 object-contain"
          />

          <!-- 위에 겹칠 아이콘 (shield) -->
          <img
            src="@/assets/img/shield_icon.png"
            alt="shield-icon"
            class="absolute w-15 h-15 top-1/3.5 left-1/3.2 -translate-x-1/2 -translate-y-1/2"
          />
        </div>

        <p
          class="font-semibold text-gray-700 m-2 p-1 border border-green-700 text-green-700 rounded-md"
        >
          일일 안전점검
        </p>
      </button>

      <!-- ② 위험성 발굴 -->
      <button
        class="bg-white pt-5 rounded-2xl shadow flex flex-col items-center hover:bg-gray-100 transition"
        @click="fnRisk_01"
      >
        <div class="relative w-24 h-24 flex justify-center items-center mb-2">
          <!-- 바닥 아이콘 (fullscreen) -->
          <img
            src="@/assets/img/vector_icon.png"
            alt="fullscreen-icon"
            class="w-24 h-24 object-contain"
          />

          <!-- 위에 겹칠 아이콘 (pencil) -->
          <img
            src="@/assets/img/pencil_square_icon.png"
            alt="pencil-icon"
            class="absolute w-15 h-15 top-1/3.5 left-1/3 -translate-x-1/2 -translate-y-1/2"
          />
        </div>

        <p
          class="font-semibold text-gray-700 m-2 p-1 border border-green-700 text-green-700 rounded-md"
        >
          위험성 평가
        </p>
      </button>
    </div>

    <!-- TBM 섹션 -->
    <div class="bg-white rounded-2xl shadow px-5 py-4 mt-6">
      <div class="flex justify-between items-center mb-2">
        <p class="font-semibold text-gray-700">TBM 참석</p>
        <p class="text-sm text-gray-500">09:00 ~ 12:00</p>
      </div>
      <div class="flex justify-between items-center">
        <span class="bg-green-50 text-green-700 px-3 py-1 rounded-md text-sm font-semibold"
          >TBM</span
        >
        <span class="text-gray-600 text-sm">TBM 교육명 12345</span>
        <span class="text-gray-500 text-sm">홍길동</span>
      </div>
    </div>

    <!-- 근태 관리 -->
    <div class="bg-white rounded-2xl shadow px-5 py-4 mt-6">
      <div class="flex justify-between items-center">
        <p class="font-semibold text-gray-700">근태 관리</p>
        <label class="relative inline-flex items-center cursor-pointer">
          <input type="checkbox" v-model="isWorking" class="sr-only peer" />
          <div
            class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-green-400 rounded-full peer peer-checked:bg-green-600 transition"
          ></div>
          <div
            class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition peer-checked:translate-x-5"
          ></div>
        </label>
      </div>

      <div class="flex justify-between items-center mt-3 bg-green-50 px-3 py-2 rounded-md">
        <span class="text-green-800 font-semibold text-sm">
          {{ isWorking ? '근무 중' : '근무 전' }}
        </span>
        <span class="text-gray-700 text-sm">YYYY/MM/DD hh:mm:ss</span>
      </div>
    </div>

    <!-- 공지사항 -->
    <div class="bg-white rounded-2xl shadow px-5 py-4 mt-6 mb-20">
      <div class="flex justify-between items-center mb-2">
        <p class="font-semibold text-gray-700">공지사항</p>
        <button class="text-sm text-green-700 font-medium">더보기</button>
      </div>
      <div class="bg-yellow-50 border-l-4 border-yellow-400 p-3 rounded">
        <span class="bg-yellow-400 text-white text-xs font-semibold px-2 py-1 rounded">NEW</span>
        <span class="ml-2 text-gray-800 text-sm">공지사항 제목이 표시됩니다.</span>
      </div>
    </div>

    <!-- 하단 네비게이션 -->
    <nav
      class="fixed bottom-0 left-0 w-full bg-white border-t flex justify-around items-center py-2 shadow-sm"
    >
      <div class="flex flex-col items-center text-green-700">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="w-6 h-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M3 9.75V19a1 1 0 001 1h6v-7h4v7h6a1 1 0 001-1V9.75M9 22V10h6v12"
          />
        </svg>
        <span class="text-xs mt-1">HOME</span>
      </div>

      <div class="flex flex-col items-center text-gray-500">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="w-6 h-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M13 16h-1v-4h-1m1-4h.01M12 18h.01M8 18h.01M16 18h.01M4 6h16M4 10h16M4 14h16"
          />
        </svg>
        <span class="text-xs mt-1">안전점검</span>
      </div>

      <div class="flex flex-col items-center text-gray-500">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="w-6 h-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M12 22a10 10 0 100-20 10 10 0 000 20zm0 0v-4m0-8v4"
          />
        </svg>
        <span class="text-xs mt-1">알림</span>
      </div>

      <div class="flex flex-col items-center text-gray-500">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="w-6 h-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M12 4v16m8-8H4"
          />
        </svg>
        <span class="text-xs mt-1">전체</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'

const { proxy } = getCurrentInstance()

const router = useRouter()

const isWorking = ref(true)
const cmpnyCd = ref('')
const userId = ref('')
const userNm = ref('')

const userMenuOpen = ref(false)
const userMenuWrapper = ref(null)

/* User Function */
function fnDayChkLst() {
  router.push('/QrScanner')
}

function fnRisk_01() {
  router.push('/Risk_01')
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

/* 바깥 클릭 시 메뉴 닫기 */
function handleClickOutside(e) {
  if (userMenuWrapper.value && !userMenuWrapper.value.contains(e.target)) {
    userMenuOpen.value = false
  }
}

onMounted(() => {
  if (proxy.$util?.isNotEmpty(sessionStorage.gv_userId)) {
    cmpnyCd.value = sessionStorage.gv_cmpnyCd
    userId.value = sessionStorage.gv_userId
    userNm.value = sessionStorage.gv_userNm
  }

  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

function goToMyInfo() {
  alert('내정보 페이지로 이동')
}

const logout = async () => {
  try {
    const response = await axios.post('/comApi/login/logout', {})
    if (response.status === 200) {
      proxy.$alert('로그아웃 처리됐습니다.')
      //      sessionStorage.removeItem("token");
      sessionStorage.clear()
      userMenuOpen.value = false
      router.push('/') // 로그인 성공 → 메인화면 이동
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message || err?.message || '로그아웃 처리 중 오류가 발생했습니다.'

    await proxy.$alert(msg)
  }
  // proxy.$alert('로그아웃 처리됐습니다.')
  // sessionStorage.removeItem('token')
  // userMenuOpen.value = false

  // router.push('/') // 로그인 성공 → 메인화면 이동
}
</script>
<style scoped>
/* .fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
} */
</style>
