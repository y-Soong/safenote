<template>
  <div class="min-h-screen bg-teal-100 flex flex-col" @click.self="closeMenu">
    <!-- Header -->
    <header class="flex justify-between items-center p-4 bg-white shadow-md relative">
      <div class="relative">
        <span class="text-lg font-semibold text-gray-800 cursor-pointer bg-teal-200 px-3 py-1 rounded-l-xl rounded-br-xl" @click.stop="toggleUserMenu">
          {{ userId }}
        </span>

        <!-- User Menu -->
        <div v-if="userMenuOpen" class="absolute left-0 mt-2 w-32 bg-white border rounded shadow z-50">
          <button class="block w-full text-left px-4 py-2 hover:bg-gray-100" @click.stop="goToMyInfo">내정보</button>
          <button class="block w-full text-left px-4 py-2 hover:bg-gray-100" @click.stop="logout">로그아웃</button>
        </div>
      </div>

      <button @click="toggleMenu">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>
    </header>

    <!-- Calendar Section -->
    <section class="p-4 flex-1">
      <div class="bg-white rounded-2xl shadow-lg p-4 mb-4 overflow-hidden relative">
        <div class="flex justify-between items-center mb-2">
          <button @click="prevMonth" class="text-xl">&lt;</button>
          <span class="font-bold text-lg">
            {{ currentMonthName }} {{ currentYear }}
          </span>
          <button @click="nextMonth" class="text-xl">&gt;</button>
        </div>

        <!-- Calendar Grid with animation -->
        <transition name="slide-fade" mode="out-in">
          <div :key="calendarKey" class="grid grid-cols-7 gap-2 text-center text-sm">
            <div v-for="day in daysOfWeek" :key="day" class="font-medium">{{ day }}</div>
            <div
              v-for="(day, index) in calendarDays"
              :key="index"
              :class="['py-2 rounded-full cursor-pointer transition', selectedDate === day.date ? 'bg-teal-300 text-white font-bold' : 'text-gray-700']"
              @click="selectDate(day.date)"
            >
              {{ day.display }}
            </div>
          </div>
        </transition>
      </div>

      <!-- Schedule List -->
      <div class="bg-white rounded-2xl shadow-lg p-4">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold">Schedule</h2>
          <button @click="openAddSchedule" class="text-2xl font-bold">+</button>
        </div>

        <div v-if="filteredSchedules.length === 0" class="text-gray-400 text-sm">No schedules for this day.</div>
        <div v-else>
          <div
            v-for="(schedule, index) in filteredSchedules"
            :key="index"
            class="mb-2 p-3 rounded-xl bg-teal-50 border-l-4 border-teal-400 shadow-sm"
          >
            <div class="font-semibold">{{ schedule.title }}</div>
            <div class="text-xs text-gray-500">{{ schedule.time }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const userId = userStore.userNm
const userMenuOpen = ref(false)
const router = useRouter()

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function closeMenu() {
  userMenuOpen.value = false
}

function goToMyInfo() {
  alert('내정보로 이동')
  userMenuOpen.value = false
}

function logout() {
  alert('로그아웃 처리')
  sessionStorage.removeItem('token')
  userStore.logout()
  userMenuOpen.value = false

  router.push('/') // 로그인 성공 → 메인화면 이동
}

const today = new Date()
const selectedDate = ref(new Date().toISOString().split('T')[0])
const currentMonth = ref(today.getMonth()) // 0-based month index
const currentMonthName = computed(() => {
  return new Date(currentYear.value, currentMonth.value).toLocaleString('default', { month: 'long' })
})
const currentYear = ref(today.getFullYear())
const calendarKey = ref(0)
const daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1).getDay()
  const lastDate = new Date(year, month + 1, 0).getDate()
  const result = []

  for (let i = 0; i < firstDay; i++) {
    result.push({ display: '', date: null })
  }
  for (let i = 1; i <= lastDate; i++) {
    const dateStr = new Date(year, month, i).toISOString().split('T')[0]
    result.push({ display: i, date: dateStr })
  }
  return result
})

const schedules = ref([
  { date: today.toISOString().split('T')[0], title: '청소 예약: 고객 김민수', time: '10:00 AM' },
  { date: today.toISOString().split('T')[0], title: '출장 일정 확인', time: '2:00 PM' }
])

const filteredSchedules = computed(() => {
  return schedules.value.filter(s => s.date === selectedDate.value)
})

function selectDate(date) {
  if (date) selectedDate.value = date
}

function nextMonth() {
  const date = new Date(currentYear.value, currentMonth.value + 1)
  currentMonth.value = date.getMonth()
  currentYear.value = date.getFullYear()
  calendarKey.value++
}

function prevMonth() {
  const date = new Date(currentYear.value, currentMonth.value - 1)
  currentMonth.value = date.getMonth()
  currentYear.value = date.getFullYear()
  calendarKey.value++
}

function toggleMenu() {
  alert('사이드 메뉴 열기')
}

function openAddSchedule() {
  alert('일정 추가 팝업')
}
</script>

<style scoped>
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.4s ease;
}
.slide-fade-enter-from {
  transform: translateX(100%);
  opacity: 0;
}
.slide-fade-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}
</style>
