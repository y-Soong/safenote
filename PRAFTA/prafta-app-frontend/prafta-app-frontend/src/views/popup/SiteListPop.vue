<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay">
      <div class="modal-content-wide" ref="modalRef">
        <!-- 🔹 1. Title 영역 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>사업장 검색</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <!-- 🔹 2. 조회 Form 영역 -->
        <div class="viewSearch">
          <div class="form-left">
            <label>사업장번호</label>
            <input v-model="siteNo" />
            <label>사업장명</label>
            <input v-model="siteNm" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 🔹 3. 그리드 영역 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th style="display: none">사업장코드</th>
                  <th>사업장번호</th>
                  <th>사업장명</th>
                  <th>관리자명</th>
                  <th>사업장 전화번호</th>
                  <th>주소</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="site in siteList"
                  :key="site.siteCd"
                  @dblclick="fnSelectRow(site.siteCd, site.siteNo, site.siteNm)"
                >
                  <td style="display: none">{{ site.siteCd }}</td>
                  <td>{{ site.siteNo }}</td>
                  <td>{{ site.siteNm }}</td>
                  <td>{{ site.siteAdminNm }}</td>
                  <td>
                    {{
                      proxy.$util.isNotEmpty(site.telNo)
                        ? proxy.$util.formatPhoneNumber(site.telNo)
                        : site.telNo
                    }}
                  </td>
                  <td>{{ site.addr1 || site.addr2 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { defineProps, defineEmits, ref, getCurrentInstance, onMounted } from 'vue'
import axios from '@/api/axios'

const cmpnyCd = ref('')
const siteList = ref([])
const emit = defineEmits(['select', 'close'])

const { proxy } = getCurrentInstance()

const props = defineProps({
  visible: Boolean,
  cmpnyCd_p: String,
  siteNo_p: String,
  siteNm_p: String,
  onSelect: Function,
})

onMounted(async () => {
  cmpnyCd.value = props.cmpnyCd_p

  if (props.siteNo_p) siteNo.value = props.siteNo_p
  if (props.siteNm_p) siteNm.value = props.siteNm_p
  fnSearch() // visible이 true일 때만 호출
})

const siteNo = ref('')
const siteNm = ref('')

const fnSearch = async () => {
  siteList.value = []
  try {
    if (!proxy.$util.isEmpty(cmpnyCd.value)) {
      // 앱 통신정렬: GET /comApi/baseinfo/site-lists (인증 변형, cmpnyCd 는 JWT 클레임에서 도출)
      const response = await axios.get('/comApi/baseinfo/site-lists', {
        params: {
          siteNo: siteNo.value,
          siteNm: siteNm.value,
        },
      })

      if (response.status === 200) {
        // 응답 스키마 { siteInfoResultList: [...] }
        siteList.value = response.data?.siteInfoResultList || []
      }
    }
  } catch (err) {
    alert(err.response.data.message)
  }
}

function fnSelectRow(siteCd, siteNo, siteNm) {
  // emit("select", siteCd, siteNo, siteNm); // SITE_CD 부모에 전달
  props.onSelect(siteCd, siteNo, siteNm)
  emit('close') // 팝업 닫기
}
</script>
