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

    <!-- 약관 기본정보 -->
    <div class="ml-5 mr-5 items-center">
      <h3 class="text-gray-800 font-semibold mb-6 text-2xl">
        {{ termsNm }}
      </h3>

      <div class="mt-5 bg-gray-100">
        <QuillEditor
          v-model:content="termsContent"
          contentType="html"
          theme="bubble"
          style="height: 30rem"
          :readOnly="readonly"
          ref="editorRef"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, getCurrentInstance } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import { useRouter, useRoute } from 'vue-router'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import axios from '@/api/axios'

const { proxy } = getCurrentInstance()

const router = useRouter()
const route = useRoute()
const termsId = ref('')
const termsNm = ref('')
const termsContent = ref('')
const readonly = ref(true)

onMounted(async () => {
    if (proxy.$util.isNotEmpty(route) && proxy.$util.isNotEmpty(route.query.termsId_p)) {
        // cmpnyCd.value = sessionStorage.getItem('gv_cmpnyCd')
        termsId.value = route.query.termsId_p
        termsNm.value = route.query.termsNm_p
    }

  await fnGetTermsInfo()
})

// API 호출
const fnGetTermsInfo = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/getTermsDInfo', {
      termsId: termsId.value,
    })

    if (response.status === 200) {
      termsContent.value = response.data.TERMS_CONTENT
    }
  } catch (err) {
    alert(err.response.data.message)
  }
}

const goBack = () => {
  router.back()
}

/* User Function */
</script>

<style scoped>
.input {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px;
  width: 100%;
}
.btn-save {
  background-color: #0d9488;
  color: white;
  padding: 10px 16px;
  border-radius: 6px;
}
.btn-cancel {
  background-color: #ddd;
  padding: 10px 16px;
  border-radius: 6px;
}
</style>
