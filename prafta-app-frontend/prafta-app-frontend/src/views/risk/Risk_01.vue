<template>
  <div class="min-h-screen flex flex-col bg-white">
    <!-- 상단 뒤로가기 버튼 -->
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

    <!-- 메인 컨텐츠 -->
    <div class="px-5 pb-2 flex-1">
      <!-- 첨부 사진 섹션 -->
      <div class="mb-6">
        <h2 class="text-gray-800 font-semibold text-base sm:text-lg mb-3">첨부 사진</h2>
        <div
          class="border-2 border-dashed border-gray-300 bg-gray-50 rounded-lg p-8 flex flex-col items-center justify-center cursor-pointer hover:bg-gray-100 transition"
          @click="triggerFileInput"
        >
          <!-- 미리보기 이미지가 있을 때 -->
          <div v-if="previewImage" class="w-full">
            <div class="relative">
              <img
                :src="previewImage.url"
                alt="Preview"
                class="w-full h-64 object-cover rounded-lg"
              />
              <button
                class="absolute top-2 right-2 bg-black/60 text-white text-xs rounded px-2 py-1"
                @click.stop="removePreview"
              >
                삭제
              </button>
            </div>
          </div>
          <!-- 미리보기 이미지가 없을 때 -->
          <div v-else class="flex flex-col items-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-16 w-16 text-gray-400 mb-2"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
              />
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
            <p class="text-gray-500 text-sm">사진 촬영하기</p>
          </div>
        </div>
        <!-- 파일 입력 (오프스크린) - capture 속성 제거로 갤러리/카메라 선택 가능 -->
        <input
          type="file"
          id="photo_input"
          accept="image/*"
          :data-role="'risk-photo'"
          @change="onFileSelected"
          @input="onFileSelected"
          style="position: absolute; left: -9999px; width: 1px; height: 1px; opacity: 0"
        />
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          사업장
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div
        class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-2 mb-3"
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
          class="px-4 py-3 border border-green-700 text-green-700 rounded-md hover:bg-green-50 transition whitespace-nowrap text-sm sm:text-base"
          @click="openSiteSearch"
        >
          찾기
        </button>
      </div>

      <!-- 발굴 장소 섹션 -->
      <div class="mb-6">
        <h2 class="text-gray-800 font-semibold text-base sm:text-lg">구분</h2>
        <div class="space-y-3 mb-3">
          <!-- 구분 선택 -->
          <select
            v-model="processCd"
            class="w-full p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border border-gray-300 bg-white rounded-lg"
          >
            <option value="" disabled>위험 구분</option>
            <option
              v-for="item in riskCategoryList"
              :key="item.baimValDCd"
              :value="item.baimValDCd"
            >
              {{ item.baimValDNm }}
            </option>
          </select>
        </div>

        <h2 class="text-gray-800 font-semibold text-base sm:text-lg">분류</h2>
        <div class="space-y-3 mb-3">
          <!-- 분류 선택 -->
          <select
            v-model="riskTypeCd"
            class="w-full p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border border-gray-300 bg-white rounded-lg"
            :disabled="!processCd"
          >
            <option value="" disabled>위험 분류</option>
            <option
              v-for="item in filteredRiskTypeList"
              :key="item.riskTypeCd"
              :value="item.riskTypeCd"
            >
              {{ item.riskTypeNm }}
            </option>
          </select>
        </div>

        <h2 class="text-gray-800 font-semibold text-base sm:text-lg">발생상황</h2>
        <div class="space-y-3 mb-3">
          <!-- 발생상황 선택 -->
          <select
            v-model="hazardCd"
            class="w-full p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border border-gray-300 bg-white rounded-lg"
            :disabled="!riskTypeCd"
          >
            <option value="" disabled>위험 발생상황</option>
            <option value="self">직접입력</option>
            <option
              v-for="item in filteredRiskHazardList"
              :key="item.hazardCd"
              :value="item.hazardCd"
            >
              {{ item.hazardNm }}
            </option>
          </select>
          <!-- 직접입력 input (self 선택 시에만 표시) -->
          <input
            v-if="hazardCd === 'self'"
            v-model="assessmentDesc"
            type="text"
            placeholder="발생상황을 직접 입력해주세요."
            class="w-full p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border border-gray-300 bg-white rounded-lg"
          />
        </div>
      </div>

      <!-- 위험 내용 섹션 -->
      <div class="mb-6">
        <h2 class="text-gray-800 font-semibold text-base sm:text-lg mb-3">위험 내용</h2>
        <textarea
          v-model="initDesc"
          class="w-full p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border border-gray-300 bg-white rounded-lg"
          rows="6"
          placeholder="발견한 위험 요소에 대한 부가 설명을 적어주세요."
        ></textarea>
      </div>

      <!-- 프로그래스 바 섹션 -->
      <div class="mb-6">
        <!-- 발생빈도 프로그래스 바 -->
        <div class="mb-6">
          <h2 class="text-gray-800 font-semibold text-base sm:text-lg mb-3">발생빈도</h2>
          <div class="flex items-end justify-center flex-wrap">
            <div
              v-for="(step, index) in [1, 2, 3, 4, 5]"
              :key="`step5-${step}`"
              class="flex flex-col items-center"
            >
              <!-- 단계 원과 연결선 -->
              <div class="flex items-center">
                <button
                  type="button"
                  @click="handleStepClick5(step)"
                  class="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center font-semibold transition-all duration-300 cursor-pointer hover:scale-110 active:scale-95 progress-step flex-shrink-0"
                  :class="[
                    getStepColor5(step, initLikelihoodScore),
                    getTextColor5(step, initLikelihoodScore),
                    animatingStep5 === step ? 'animate-fill' : '',
                  ]"
                >
                  {{ step }}
                </button>
                <!-- 연결선 -->
                <div
                  v-if="index < 4"
                  class="h-1.5 w-8 sm:w-12 md:w-16 transition-all duration-300 flex-shrink-0"
                  :style="getLineGradient5(step, initLikelihoodScore)"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 중대성 프로그래스 바 -->
        <div>
          <h2 class="text-gray-800 font-semibold text-base sm:text-lg mb-3">중대성</h2>
          <div class="flex items-center justify-center flex-wrap">
            <div
              v-for="(step, index) in [1, 2, 3, 4]"
              :key="`step4-${step}`"
              class="flex items-center"
            >
              <!-- 단계 원 -->
              <button
                type="button"
                @click="handleStepClick4(step)"
                class="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center font-semibold transition-all duration-300 cursor-pointer hover:scale-110 active:scale-95 progress-step flex-shrink-0"
                :class="[
                  getStepColor4(step, initSeverityScore),
                  getTextColor4(step, initSeverityScore),
                  animatingStep4 === step ? 'animate-fill' : '',
                ]"
              >
                {{ step }}
              </button>
              <!-- 연결선 (5단계와 동일한 전체 너비를 위해 더 길게) -->
              <div
                v-if="index < 3"
                class="h-1.5 line-4step transition-all duration-300 flex-shrink-0"
                :style="getLineGradient4(step, initSeverityScore)"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 하단 저장 버튼 -->
    <div class="p-6">
      <button
        class="w-full bg-green-700 text-white py-3 rounded-md hover:bg-green-800 transition"
        @click="fnSave()"
      >
        저장
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
      keyField="SITE_ID"
      labelField="SITE_NM"
      :multiple="false"
      v-model="siteCd"
      @close="showSitePanel = false"
      @search="fetchSiteList"
      @confirm="selectSites"
    />
  </div>
</template>

<script>
/* eslint-disable */
export default {
  name: 'RiskAssessment01',
}
</script>

<script setup>
/* eslint-disable */
import { ref, reactive, onMounted, computed, watch, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import SidePanel from '@/components/common/SidePanel.vue'
import axios from '@/api/axios'

// ---------- Router ----------
const router = useRouter()

// ---------- Ref ----------
const { proxy } = getCurrentInstance()

const previewImage = ref(null) // { file, url }
const processCd = ref('') // 구분 (baimValDCd)
const riskTypeCd = ref('') // 분류 (riskTypeCd)
const hazardCd = ref('') // 발생상황 (hazardCd)
const assessmentDesc = ref('') // 직접입력 발생상황
const initDesc = ref('')
const siteCd = ref('')
const siteNo = ref('')
const siteNm = ref('')

// 프로그래스 바 현재 단계
const initLikelihoodScore = ref(1) // 발생가능성 - 5단계 프로그래스 바 현재 단계
const initSeverityScore = ref(1) // 중대성 - 4단계 프로그래스 바 현재 단계
const animatingStep5 = ref(null) // 5단계 애니메이션 중인 단계
const animatingStep4 = ref(null) // 4단계 애니메이션 중인 단계

// 단계 클릭 핸들러
const handleStepClick5 = (step) => {
  animatingStep5.value = step
  initLikelihoodScore.value = step
  setTimeout(() => {
    animatingStep5.value = null
  }, 500)
}

const handleStepClick4 = (step) => {
  animatingStep4.value = step
  initSeverityScore.value = step
  setTimeout(() => {
    animatingStep4.value = null
  }, 500)
}

// 5단계 색상 결정 함수
const getStepColor5 = (step, currentStep) => {
  // 활성화되지 않은 단계는 회색
  if (currentStep < step) {
    return 'bg-gray-300'
  }
  // 활성화된 단계는 단계별 색상 적용
  // 1=초록색, 2=연두색, 3=노란색, 4=빨간색, 5=빨간색
  switch (step) {
    case 1:
      return 'bg-green-500'
    case 2:
      return 'bg-green-500'
    case 3:
      return 'bg-orange-500'
    case 4:
    case 5:
      return 'bg-red-500'
  }
}

// 5단계 연결선 색상 결정 함수 (레거시 - 사용 안 함)
const getLineColor5 = (step, currentStep) => {
  if (currentStep <= step) {
    return 'bg-gray-300'
  }
  // 다음 단계의 색상을 사용
  const nextStep = step + 1
  switch (nextStep) {
    case 1:
      return 'bg-green-500'
    case 2:
      return 'bg-green-500'
    case 3:
      return 'bg-orange-500'
    case 4:
    case 5:
      return 'bg-red-500'
  }
}

// 5단계 연결선 그라데이션 스타일 함수
const getLineGradient5 = (step, currentStep) => {
  if (currentStep <= step) {
    return { background: 'rgb(209, 213, 219)' } // gray-300
  }

  // 현재 단계와 다음 단계의 색상 가져오기
  const currentColor = getStepColorCode5(step)
  const nextColor = getStepColorCode5(step + 1)

  return {
    background: `linear-gradient(to right, ${currentColor}, ${nextColor})`,
  }
}

// 5단계 색상 코드 반환 함수
const getStepColorCode5 = (step, currentStep) => {
  if (currentStep < step) {
    return 'bg-gray-300'
  }

  switch (step) {
    case 1:
      return 'rgb(34, 197, 94)' // green-500
    case 2:
      return 'rgb(34, 197, 94)' // green-500
    case 3:
      return 'rgb(249, 115, 22)' // orange-500
    case 4:
    case 5:
      return 'rgb(239, 68, 68)' // red-500
    default:
      return 'rgb(209, 213, 219)' // gray-300
  }
}

// 4단계 색상 결정 함수
const getStepColor4 = (step, currentStep) => {
  if (currentStep < step) {
    return 'bg-gray-300'
  }
  // 1=초록색, 2=연두색, 3=노란색, 4=빨간색
  switch (step) {
    case 1:
      return 'bg-green-500'
    case 2:
      return 'bg-yellow-500'
    case 3:
      return 'bg-orange-500'
    case 4:
      return 'bg-red-500'
  }
}

// 4단계 연결선 색상 결정 함수 (레거시 - 사용 안 함)
const getLineColor4 = (step, currentStep) => {
  if (currentStep <= step) {
    return 'bg-gray-300'
  }
  // 다음 단계의 색상을 사용
  const nextStep = step + 1
  switch (nextStep) {
    case 1:
      return 'bg-green-500'
    case 2:
      return 'bg-yellow-500'
    case 3:
      return 'bg-orange-500'
    case 4:
      return 'bg-red-500'
  }
}

// 4단계 연결선 그라데이션 스타일 함수
const getLineGradient4 = (step, currentStep) => {
  if (currentStep <= step) {
    return { background: 'rgb(209, 213, 219)' } // gray-300
  }

  // 현재 단계와 다음 단계의 색상 가져오기
  const currentColor = getStepColorCode4(step)
  const nextColor = getStepColorCode4(step + 1)

  return {
    background: `linear-gradient(to right, ${currentColor}, ${nextColor})`,
  }
}

// 4단계 색상 코드 반환 함수
const getStepColorCode4 = (step) => {
  switch (step) {
    case 1:
      return 'rgb(34, 197, 94)' // green-500
    case 2:
      return 'rgb(234, 179, 8)' // yellow-500
    case 3:
      return 'rgb(249, 115, 22)' // orange-500
    case 4:
      return 'rgb(239, 68, 68)' // red-500
    default:
      return 'rgb(209, 213, 219)' // gray-300
  }
}

// 5단계 텍스트 색상 결정 함수
const getTextColor5 = (step, currentStep) => {
  if (currentStep < step) {
    return 'text-gray-600'
  }
  // 2단계(emerald-400)는 검은색 텍스트, 나머지는 흰색
  switch (step) {
    default:
      return 'text-white'
  }
}

// 4단계 텍스트 색상 결정 함수
const getTextColor4 = (step, currentStep) => {
  if (currentStep < step) {
    return 'text-gray-600'
  }
  // 2단계(emerald-400)는 검은색 텍스트, 나머지는 흰색
  switch (step) {
    default:
      return 'text-white'
  }
}

/* side panel 변수 */
const showSitePanel = ref(false)
const isLoading = ref(false)
const isError = ref(false)
const siteList = ref([])
const selectedSites = ref([]) // ✅ v-model로 연결됨

// ---------- API 데이터 ----------
const riskCategoryList = ref([]) // 구분 목록
const riskTypeList = ref([]) // 분류 목록 (전체)
const riskHazardList = ref([]) // 발생상황 목록 (전체)

// computed로 분류 목록 동적 계산 (구분에 따라 필터링)
const filteredRiskTypeList = computed(() => {
  if (!processCd.value) return []
  return riskTypeList.value.filter((item) => item.processCd === processCd.value) || []
})

// computed로 발생상황 목록 동적 계산 (분류에 따라 필터링)
const filteredRiskHazardList = computed(() => {
  if (!riskTypeCd.value) return []
  return riskHazardList.value.filter((item) => item.riskTypeCd === riskTypeCd.value) || []
})

// 구분 변경 시 분류, 발생상황 초기화
watch(processCd, () => {
  riskTypeCd.value = ''
  hazardCd.value = ''
})

// 분류 변경 시 발생상황 초기화
watch(riskTypeCd, () => {
  hazardCd.value = ''
  assessmentDesc.value = ''
})

// 직접입력 선택 해제 시 직접입력 값 초기화
watch(hazardCd, (newVal) => {
  if (newVal !== 'self') {
    assessmentDesc.value = ''
  }
})

// ---------- 초기 로딩 ----------
onMounted(async () => {
  siteCd.value = sessionStorage.getItem('gv_siteCd') || ''
  siteNo.value = sessionStorage.getItem('gv_siteNo') || ''
  siteNm.value = sessionStorage.getItem('gv_siteNm') || ''

  await fnGetRiskTypeInfos()
})

const fnGetRiskTypeInfos = async () => {
  const payload = { siteCd: siteCd.value }
  try {
    const res = await axios.get('/appApi/risk01/risk-type-infos', {
      params: payload,
    })
    if (res.status >= 200 && res.status < 300) {
      const data = res.data
      // 구분 목록 설정
      riskCategoryList.value = data.riskCategoryList || []
      // 분류 목록 설정 (전체)
      riskTypeList.value = data.riskTypeList || []
      // 발생상황 목록 설정 (전체)
      riskHazardList.value = data.riskHazardList || []
    }
  } catch (err) {
    console.error('[fnGetRiskTypeInfos] error', err)
    proxy.$alert(err.response?.data?.message || '위험 유형 정보를 불러오는데 실패했습니다.')
  }
}

// ---------- Utils ----------
const isImageFile = (file) =>
  file && typeof file.type === 'string' && file.type.startsWith('image/')

// ---------- 파일 입력 트리거 ----------
function triggerFileInput() {
  const input = document.getElementById('photo_input')
  if (input) input.click()
}

// ---------- 파일 선택 콜백 ----------
function onFileSelected(evt) {
  try {
    const input = evt?.target
    const file = input?.files?.[0]

    console.log('[Risk_01] onFileSelected hasFile=', !!file)
    if (!file) return

    if (!isImageFile(file)) {
      alert('이미지 파일만 선택해주세요.')
      if (input) input.value = ''
      return
    }

    // 기존 프리뷰/URL 정리
    if (previewImage.value?.url) {
      URL.revokeObjectURL(previewImage.value.url)
    }

    const url = URL.createObjectURL(file)
    previewImage.value = { file, url }
    console.log('[Risk_01] preview set', { name: file.name, size: file.size })

    // 같은 파일 재선택 허용
    if (input) input.value = ''
  } catch (e) {
    console.log('[Risk_01] onFileSelected error', e)
  }
}

// ---------- 프리뷰 삭제 ----------
function removePreview() {
  try {
    if (previewImage.value?.url) {
      URL.revokeObjectURL(previewImage.value.url)
    }
    previewImage.value = null
    const input = document.getElementById('photo_input')
    if (input) input.value = ''
    console.log('[Risk_01] removePreview')
  } catch (e) {
    console.log('[Risk_01] removePreview error', e)
  }
}

// ---------- 파일명 생성 함수 (ChkLst.vue와 동일한 방식) ----------
function buildFileName(prefix, originalName = 'photo.jpg') {
  const ts = new Date().toISOString().replace(/[:.]/g, '')
  const safe = String(originalName).replace(/[^\w.\-]+/g, '_')
  return `${prefix}_${ts}_${safe}`
}

// ---------- 저장 함수 ----------
const fnSave = async () => {

  try {
    // 유효성 검사
    if (!processCd.value) {
      proxy.$alert('위험 구분을 선택해주세요.')
      return
    }
    if (!riskTypeCd.value) {
      proxy.$alert('위험 분류를 선택해주세요.')
      return
    }
    if (!hazardCd.value) {
      proxy.$alert('위험 발생상황을 선택해주세요.')
      return
    }
    if (hazardCd.value === 'self' && !assessmentDesc.value.trim()) {
      proxy.$alert('발생상황을 직접 입력해주세요.')
      return
    }
    if (!initDesc.value.trim()) {
      proxy.$alert('위험 내용을 입력해주세요.')
      return
    }

    const formData = new FormData()
    // formData.append('workDate', proxy.$util.getToday().replaceAll('-', '') || '')
    // formData.append('cmpnyCd', sessionStorage.getItem('gv_cmpnyCd') || '')
    // formData.append('userCd', sessionStorage.getItem('gv_userCd') || '')
    formData.append('siteCd', siteCd.value || '')
    formData.append('processCd', processCd.value)
    formData.append('riskTypeCd', riskTypeCd.value)
    // 직접입력인 경우 직접입력 값을, 아닌 경우 선택한 hazardCd를 전송
    formData.append('hazardCd', hazardCd.value)
    formData.append('assessmentDesc', assessmentDesc.value)
    formData.append('initDesc', initDesc.value)

    formData.append('initLikelihoodScore', initLikelihoodScore.value)
    formData.append('initSeverityScore', initSeverityScore.value)

    // 사진이 있으면 추가
    if (previewImage.value?.file) {
      const fileName = buildFileName('risk', previewImage.value.file.name)
      formData.append('item', previewImage.value.file, fileName)
    }

    const res = await axios.post('/appApi/risk01/save-risk-assessments', formData, {
      timeout: 60 * 1000,
      validateStatus: () => true,
    })

    if (res.status >= 200 && res.status < 300) {
      proxy.$alert('저장되었습니다.')
      //      router.push('/MainView')
    } else {
      const msg = res.data?.message || res.data?.error || `HTTP ${res.status}`
      alert(`저장 실패: ${msg}`)
    }
  } catch (err) {
    console.error('[SAVE] error', err)
    proxy.$alert(err.response?.data?.message || '저장 중 오류가 발생했습니다.')
  }
}

// ---------- 네비게이션 ----------
function goBack() {
  router.back()
}

// ---------- 정리 ----------
onBeforeUnmount(() => {
  if (previewImage.value?.url) {
    URL.revokeObjectURL(previewImage.value.url)
  }
})

const openSiteSearch = () => {
  showSitePanel.value = true
  fetchSiteList()
}

const fetchSiteList = async (keyword) => {
  isLoading.value = true
  isError.value = false
  try {
    const response = await axios.post('/comApi/baseinfo/getSiteInfoList', {
      cmpnyCd: sessionStorage.getItem('gv_cmpnyCd') || '',
      siteNm: keyword,
    })

    if (response.status === 200) {
      siteList.value = response.data
    }
  } catch (err) {
    isError.value = true
  } finally {
    isLoading.value = false
  }
}

const selectSites = (selected) => {
  siteCd.value = selected.SITE_CD
  siteNo.value = selected.SITE_NO
  siteNm.value = selected.SITE_NM
  showSitePanel.value = false
}
</script>

<style scoped>
/* 프로그래스 바 단계 버튼 애니메이션 */
.progress-step {
  position: relative;
  overflow: hidden;
}

.progress-step::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.3);
  transition: left 0.3s ease;
}

.progress-step:active::before {
  left: 100%;
}

/* 클릭 시 색상 차오르는 애니메이션 */
.progress-step.animate-fill {
  animation: fillUp 0.3s ease-in-out;
}

@keyframes fillUp {
  0% {
    background-color: rgb(209, 213, 219); /* gray-300 */
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    /* 색상은 각 단계별로 다르므로 여기서는 기본값만 설정 */
  }
}

/* 4단계 연결선 너비 (5단계와 동일한 전체 너비를 위해 계산된 값) */
/* 5단계: 5 * 버튼 + 4 * 선 = 4단계: 4 * 버튼 + 3 * 선 */
/* 작은 화면: (32 + 128) / 3 = 53.33px */
/* 중간 화면: (40 + 192) / 3 = 77.33px */
/* 큰 화면: (40 + 256) / 3 = 98.67px */
.line-4step {
  width: 3.33rem; /* 약 53.33px */
}

@media (min-width: 640px) {
  .line-4step {
    width: 4.83rem; /* 약 77.33px */
  }
}

@media (min-width: 768px) {
  .line-4step {
    width: 6.17rem; /* 약 98.67px */
  }
}
</style>
