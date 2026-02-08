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

    <div class="px-5 pb-2" v-for="chkpt in chkptLst" :key="String(chkpt.inspectItemCd)">
      <!-- 점검 항목 -->
      <div
        class="border border-gray-300 bg-gray-50 rounded-lg p-3 shadow-sm hover:shadow-md transition"
        style="background: #d5eee5"
      >
        <div class="flex justify-between items-center">
          <span class="text-gray-800 font-semibold text-base sm:text-lg">
            {{ chkpt.INSPECT_ITEM_SUBJ }}
          </span>

          <!-- 라디오버튼 -->
          <div class="flex justify-end gap-4">
            <label class="flex items-center gap-1 text-sm text-gray-700">
              <input
                type="radio"
                :name="'chk_' + chkpt.inspectItemCd"
                v-model="chkpt.inspectValue"
                value="Y"
              />
              양호
            </label>
            <label class="flex items-center gap-1 text-sm text-gray-700">
              <input
                type="radio"
                :name="'chk_' + chkpt.inspectItemCd"
                v-model="chkpt.inspectValue"
                value="N"
              />
              불량
            </label>
          </div>
        </div>
      </div>

      <!-- 불량일 때만 추가영역 -->
      <div v-if="chkpt.inspectValue === 'N'" class="shadow-sm mt-0">
        <!-- 버튼(=label for로 파일입력 직접 트리거: WebView 호환 ↑) -->
        <div class="flex justify-end gap-2 px-3 pt-1 pb-1">
          <!-- 사진첨부 -->
          <label
            class="rounded text-white text-sm px-3 py-1 cursor-pointer"
            style="background-color: #30796a"
            :for="`gallery_${chkpt.inspectItemCd}`"
            @click="onOpen('gallery', chkpt)"
          >
            {{ (previews[chkpt.inspectItemCd] || []).length ? '사진교체' : '사진첨부' }}
          </label>
        </div>

        <!-- ⚠️ display:none 금지. 오프스크린으로 이동 -->
        <!-- 갤러리 -->
        <input
          type="file"
          :id="`gallery_${chkpt.inspectItemCd}`"
          accept="image/*"
          :multiple="false"
          :data-role="`gallery-${chkpt.inspectItemCd}`"
          @change="onFileSelected(chkpt, $event)"
          @input="onFileSelected(chkpt, $event)"
          style="position: absolute; left: -9999px; width: 1px; height: 1px; opacity: 0"
        />

        <!-- 미리보기 -->
        <div v-if="(previews[chkpt.inspectItemCd] || []).length" class="px-3 pb-2">
          <div class="mt-2 grid grid-cols-3 sm:grid-cols-4 gap-2">
            <div
              v-for="(img, i) in previews[chkpt.inspectItemCd]"
              :key="`${chkpt.inspectItemCd}_${i}`"
              class="relative border rounded overflow-hidden"
            >
              <img :src="img.url" alt="" class="w-full h-24 object-cover" />
              <button
                class="absolute top-1 right-1 bg-black/60 text-white text-xs rounded px-1"
                @click="removePreview(chkpt, i)"
              >
                삭제
              </button>
            </div>
          </div>
        </div>

        <!-- 메모 -->
        <textarea
          class="w-full p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border border-gray-300 bg-white rounded-lg"
          rows="2"
          placeholder="상세내용을 입력해주세요."
          v-model="answerDescByItem[chkpt.inspectItemCd]"
        ></textarea>
      </div>
    </div>

    <div class="p-6">
      <button
        class="w-full bg-green-700 text-white py-3 rounded-md hover:bg-green-800 transition"
        @click="fnSave()"
      >
        저장
      </button>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, reactive, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from '@/api/axios'

// ---------- Router / Route ----------
const router = useRouter()
const route = useRoute()

// ---------- Ref ----------
const chkptLst = ref([])
const qrData = ref(null)
const siteCd = ref('')
const chkptCd = ref('')

// ---------- Reactive ----------
const previews = reactive({}) // { [itemCd]: [{file, url}] }
const answerDescByItem = reactive({}) // { [itemCd]: 'memo...' }

const { proxy } = getCurrentInstance()

// ---------- Utils ----------
const isImageFile = (file) =>
  file && typeof file.type === 'string' && file.type.startsWith('image/')

function safeParseQr(raw) {
  try {
    if (!raw) return null
    if (typeof raw === 'object') return raw
    let s = Array.isArray(raw) ? raw[0] : raw
    try {
      return JSON.parse(s)
    } catch (_) {}
    try {
      return JSON.parse(decodeURIComponent(s))
    } catch (_) {}
    if (typeof s === 'string' && s.includes('=')) {
      const obj = {}
      s.split('&').forEach((p) => {
        const [k, v] = p.split('=')
        obj[decodeURIComponent(k)] = decodeURIComponent(v || '')
      })
      return obj
    }
    return null
  } catch (e) {
    console.log('[QR] parse error:', e)
    return null
  }
}

/* User Function */
const fnSave = async () => {
  try {
    const items = chkptLst.value.map((row) => {
      const itemCd = row.inspectItemCd
      const answerDesc = answerDescByItem[itemCd] || ''
      const inspectValue = row.inspectValue ?? null
      // 프리뷰에 1장만 유지하는 구조
      const fileObj = (previews[itemCd] && previews[itemCd][0]) || null
      const fileName = fileObj?.file?.name || null

      return {
        itemCd,
        inspectValue,
        answerDesc,
        fileName, // 서버 참고용(선택)
      }
    })

    const formData = new FormData()
    formData.append('workDate', proxy.$util.getToday().replaceAll('-', '') || '')
    formData.append('cmpnyCd', sessionStorage.getItem('gv_cmpnyCd') || '')
    formData.append('userCd', sessionStorage.getItem('gv_userCd') || '')
    formData.append('siteCd', siteCd.value || '')
    formData.append('chkptCd', chkptCd.value || '')
    formData.append('items', new Blob([JSON.stringify(items)], { type: 'application/json' }))

    chkptLst.value.forEach((row) => {
      const itemCd = row.inspectItemCd
      const entry = previews[itemCd]?.[0]
      if (entry?.file) {
        const name = buildFileName(itemCd, entry.file.name)
        formData.append(`files[${itemCd}]`, entry.file, name)
      }
    })

    const res = await axios.post('/appApi/chkLst01/save-inspect-result', formData, {
      timeout: 60 * 1000,
      validateStatus: () => true,
    })

    if (res.status >= 200 && res.status < 300) {
      proxy.$alert('저장되었습니다.')
      router.push('/MainView')
    } else {
      const msg = res.data?.message || res.data?.error || `HTTP ${res.status}`
      alert(`저장 실패: ${msg}`)
    }
  } catch (err) {
    console.error('[SAVE] error', err)
    proxy.$alert(err.response.data.message)
  }
}

function buildFileName(itemCd, originalName = 'photo.jpg') {
  const ts = new Date().toISOString().replace(/[:.]/g, '')
  const safe = String(originalName).replace(/[^\w.\-]+/g, '_')
  return `${itemCd}_${ts}_${safe}`
}

// ---------- Global change listener (for debug) ----------
const onGlobalChange = (e) => {
  try {
    const t = e.target
    if (t && t.tagName === 'INPUT' && t.type === 'file') {
      console.log('[global change] id=', t.id, 'files=', t.files && t.files.length)
    }
  } catch (err) {
    console.log('[global change] error', err)
  }
}

// ---------- 초기 로딩 ----------
onMounted(async () => {
  document.addEventListener('change', onGlobalChange, true)

  const rawQr = route.query.qr
  qrData.value = safeParseQr(rawQr) || {}
  siteCd.value = qrData.value.siteCd || ''
  chkptCd.value = qrData.value.chkptCd || ''

  await fnGetChkList()
})

onBeforeUnmount(() => {
  document.removeEventListener('change', onGlobalChange, true)
  Object.values(previews).forEach((list) => {
    ;(list || []).forEach((item) => URL.revokeObjectURL(item.url))
  })
})

// ---------- 리스트 조회 ----------
const fnGetChkList = async () => {
  const payload = { siteCd: siteCd.value, chkptCd: chkptCd.value }
  try {
    const res = await axios.get('/appApi/chkLst01/checklist-infos', {
      params: payload,
      validateStatus: () => true,
    })

    if (res.status >= 200 && res.status < 300) {
      const list = Array.isArray(res.data) ? res.data : res.data?.checklistInfos || []
      chkptLst.value = list.map((x) => ({
        inspectItemCd: x.inspectItemCd,
        INSPECT_ITEM_SUBJ: x.inspectItemSubj,
        inspectValue: x.inspectValue ?? undefined,
      }))
    } else {
      const msg =
        (typeof res.data === 'string' && res.data) ||
        res.data?.message ||
        res.data?.error ||
        `HTTP ${res.status}`
      alert(`목록 조회 실패: ${msg}`)
    }
  } catch (err) {
    console.log('[API] catch error =', err)
    alert(err?.message || '목록 조회 중 오류')
  }
}

// ---------- 네비게이션 ----------
function goBack() {
  router.back()
}

// ---------- 파일 열기(라벨 클릭시 진입) ----------
function onOpen(kind, chkpt) {
  try {
    console.log('[imagePicker] onOpen kind=', kind, 'item=', chkpt && chkpt.inspectItemCd)
    // 라벨의 for 속성이 input 클릭을 대신 처리 → 여기서는 로깅만
  } catch (e) {
    console.log('[imagePicker] onOpen error', e)
  }
}

// ---------- 파일 선택 콜백 ----------
function onFileSelected(chkpt, evt) {
  try {
    const itemCd = chkpt?.inspectItemCd
    const input = evt?.target
    const file = input?.files?.[0] // ✅ 딱 한 개만 사용

    console.log('[imagePicker] onFileSelected item=', itemCd, 'hasFile=', !!file)
    if (!file) return

    if (!isImageFile(file)) {
      alert('이미지 파일만 선택해주세요.')
      if (input) input.value = ''
      return
    }

    // ✅ 기존 프리뷰/URL 정리 후 한 개만 저장
    const list = previews[itemCd] || (previews[itemCd] = [])
    list.forEach((p) => p?.url && URL.revokeObjectURL(p.url))
    list.length = 0

    const url = URL.createObjectURL(file)
    list.push({ file, url })
    console.log('[imagePicker] preview set (single)', { itemCd, name: file.name, size: file.size })

    // 같은 파일 재선택 허용
    if (input) input.value = ''
  } catch (e) {
    console.log('[imagePicker] onFileSelected error', e)
  }
}

// ---------- 프리뷰 삭제 ----------
function removePreview(chkpt, idx) {
  try {
    const itemCd = chkpt?.inspectItemCd
    const list = previews[itemCd] || []
    const target = list[idx]
    if (target?.url) URL.revokeObjectURL(target.url)
    list.splice(idx, 1)
    console.log('[imagePicker] removePreview item=', itemCd, 'idx=', idx)
  } catch (e) {
    console.log('[imagePicker] removePreview error', e)
  }
}
</script>
