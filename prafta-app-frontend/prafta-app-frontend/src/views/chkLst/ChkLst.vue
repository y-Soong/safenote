<template>
  <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-6">
    <div class="bg-white shadow-lg rounded-2xl p-6 w-full max-w-md text-center">
      <h1 class="text-xl font-bold text-green-700 mb-4">QR 코드 인식 결과</h1>

      <p
        v-if="qrValue"
        class="text-gray-800 bg-green-50 border border-green-200 p-4 rounded-md break-all"
      >
        {{ qrValue }}
      </p>

      <p v-else class="text-gray-500">QR 코드 데이터가 없습니다.</p>

      <button
        @click="goBack"
        class="mt-6 bg-green-700 text-white px-6 py-2 rounded-md hover:bg-green-800 transition"
      >
        돌아가기
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const qrValue = route.query.qr || ''

function goBack() {
  router.push('/MainView') // ✅ 필요시 다른 경로로 수정 가능
}
</script>
