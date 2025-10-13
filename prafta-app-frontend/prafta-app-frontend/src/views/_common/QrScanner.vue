<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-50">
    <div id="qr-reader" class="w-full max-w-sm"></div>
    <p v-if="qrResult" class="mt-4 text-green-700 font-semibold">QR 코드 인식값: {{ qrResult }}</p>
    <button @click="stopScanner" class="mt-6 bg-red-500 text-white px-4 py-2 rounded">
      카메라 종료
    </button>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted, onBeforeUnmount } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()

const router = useRouter()
const qrResult = ref(null)
let html5QrCode = null

const startScanner = async () => {
  const qrRegionId = 'qr-reader'
  html5QrCode = new Html5Qrcode(qrRegionId)
  const config = { fps: 10, qrbox: { width: 250, height: 250 } }

  try {
    // ✅ 카메라 권한 먼저 확인
    await navigator.mediaDevices.getUserMedia({ video: true })

    await html5QrCode.start(
      { facingMode: 'environment' },
      config,
      (decodedText) => {
        qrResult.value = decodedText
        router.replace({ path: '/ChkLst', query: { qr: decodedText } })
        stopScanner()
      },
      (errorMessage) => {
        console.log('QR Scan error:', errorMessage)
      }
    )
  } catch (err) {
    console.error('카메라 접근 실패:', err)

    // ✅ 권한 거부 시 사용자 안내
    proxy.$alert(
      '카메라 접근 권한이 거부되었습니다.\n브라우저 설정에서 카메라 권한을 허용해주세요.'
    )
  }
}

const stopScanner = () => {
  if (html5QrCode) {
    html5QrCode.stop().then(() => {
      html5QrCode.clear()
      html5QrCode = null
    })
  }
}

onMounted(() => {
  startScanner()
})

onBeforeUnmount(() => {
  stopScanner()
})
</script>

<style scoped>
#qr-reader video {
  border-radius: 0.75rem;
}
</style>
