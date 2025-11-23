<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-50">
    <div id="qr-reader" class="w-full max-w-sm"></div>
    <p v-if="qrResult" class="mt-4 text-green-700 font-semibold">QR 코드 인식값: {{ qrResult }}</p>
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
    const devices = await Html5Qrcode.getCameras()
    if (!devices.length) throw new Error('No camera found')
    // 후면 카메라를 찾아보고 없으면 첫 번째 사용
    const backCam = devices.find((d) => /back|rear|environment/i.test(d.label)) || devices[0]

    await html5QrCode.start(
      { deviceId: { exact: backCam.id } },
      config,
      (decodedText) => {
        qrResult.value = decodedText
        router.replace({ path: '/ChkLst', query: { qr: decodedText } })
        stopScanner()
      },
      (err) => console.warn('QR error', err)
    )
  } catch (err) {
    proxy.$alert(
      '카메라 초기화에 실패했습니다.\n다른 앱이 카메라를 사용 중이거나 후면 카메라 인식이 불가합니다.'
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
