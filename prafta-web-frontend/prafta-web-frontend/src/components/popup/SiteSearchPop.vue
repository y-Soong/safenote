<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
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
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
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
                <template v-if="!siteList || siteList.length === 0">
                  <tr>
                    <td colspan="5" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="site in siteList"
                    :key="site.SITE_CD"
                    @dblclick="
                      fnSelectRow(site.SITE_CD, site.SITE_NO, site.SITE_NM)
                    "
                  >
                    <td style="display: none">{{ site.SITE_CD }}</td>
                    <td>{{ site.SITE_NO }}</td>
                    <td>{{ site.SITE_NM }}</td>
                    <td>{{ site.SITE_ADMIN_ID }}</td>
                    <td>
                      {{
                        proxy.$util.isNotEmpty(site.TEL_NO)
                          ? proxy.$util.formatPhoneNumber(site.TEL_NO)
                          : site.TEL_NO
                      }}
                    </td>
                    <td>{{ site.ADDR_1 || site.ADDR_2 }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  defineProps,
  defineEmits,
  ref,
  getCurrentInstance,
  onMounted,
} from "vue";
// import { useDraggable } from "@/composables/useDraggable";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";

const cmpnyCd = ref("");
const siteList = ref([]);
const emit = defineEmits(["select", "close"]);
const modalRef = ref(null);

const { proxy } = getCurrentInstance();

const props = defineProps({
  visible: Boolean,
  cmpnyCd_p: String,
  siteNo_p: String,
  siteNm_p: String,
  siteNo_n_p: String,
  onSelect: Function,
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(async () => {
  cmpnyCd.value = props.cmpnyCd_p;

  if (props.siteNo_p) siteNo.value = props.siteNo_p;
  if (props.siteNm_p) siteNm.value = props.siteNm_p;
  fnSearch(); // visible이 true일 때만 호출
});

const siteNo = ref("");
const siteNm = ref("");

const fnSearch = async () => {
  siteList.value = [];
  try {
    if (!proxy.$util.isEmpty(cmpnyCd.value)) {
      const response = await axios.post("/comApi/baseinfo/getSiteInfoList", {
        cmpnyCd: cmpnyCd.value,
        siteNo: siteNo.value,
        siteNm: siteNm.value,
        userId: sessionStorage.getItem("gv_userId"),
      });

      if (response.status === 200) {
        console.log(response.data);
        console.log(props.siteNo_n_p);

        if (proxy.$util.isEmpty(props.siteNo_n_p)) {
          siteList.value = response.data;
        } else {
          siteList.value = response.data.filter((item) => {
            console.log("item.SITE_NO :: ");
            console.log(item.SITE_NO);
            if (item.SITE_NO != props.siteNo_n_p) {
              return true;
            } else {
              return false;
            }
          });
        }
      }
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

function fnSelectRow(siteCd, siteNo, siteNm) {
  // emit("select", siteCd, siteNo, siteNm); // SITE_CD 부모에 전달
  props.onSelect(siteCd, siteNo, siteNm);
  emit("close"); // 팝업 닫기
}
</script>
