<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
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
            <!-- 사용여부 필터: 회원가입(joinMode)에선 노출하지 않는다(서버가 'Y' 고정). -->
            <template v-if="!joinMode">
              <label>사용여부</label>
              <select v-model="useYn">
                <option value="">전체</option>
                <option
                  v-for="opt in (systCodeArr['SYS003'] || []).filter(
                    (o) => o.systValDCd != null
                  )"
                  :key="opt.systValDCd"
                  :value="opt.systValDCd"
                >
                  {{ opt.systValDNm }}
                </option>
              </select>
            </template>
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
                  <ThSortable
                    label="사업장번호"
                    col-key="siteNo"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.siteNo"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="사업장명"
                    col-key="siteNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.siteNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="관리자명"
                    col-key="siteAdminNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.siteAdminNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="사업장 전화번호"
                    col-key="telNo"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.telNo"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="주소"
                    col-key="addr1"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.addr1"
                    @sort="onSort"
                    @update:width="onResize"
                  />
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
                    v-for="site in sortedData"
                    :key="site.siteCd"
                    @dblclick="
                      fnSelectRow(site.siteCd, site.siteNo, site.siteNm)
                    "
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
import { resolveApiErrorMessage } from "@/utils/apiError";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

const cmpnyCd = ref("");
const siteList = ref([]);
const systCodeArr = ref({}); // SYS003(사용여부) 등 코드 목록
const useYn = ref(""); // 사용여부 필터(빈 값=전체). 일반 조회 전용
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(siteList);
const { colWidths, onResize } = useColumnResize({
  siteNo: 120,
  siteNm: 150,
  siteAdminNm: 100,
  telNo: 130,
  addr1: 200,
});
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
  // 회원가입(비로그인) 모드: true 면 활성기간 밖(개시일 이전·종료일 이후) 사업장을 제외하는
  //   NoAuth 회원가입 전용 엔드포인트를 사용한다. 미지정(false) 시 기존 일반 조회 그대로.
  joinMode: { type: Boolean, default: false },
  // PRAFTA-WEB_002-T1-04(1.3-2): 결과에서 제외할 사업장코드(소속이동 시 현재 사업장 제외용). 미지정 시 제외 없음.
  excludeSiteCd_p: { type: String, default: "" },
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
  // 사용여부 필터는 일반(로그인 후) 조회에서만 사용 → 회원가입 모드면 코드 로드 생략
  if (!props.joinMode) await fnGetSystinfoList();
  fnSearch(); // visible이 true일 때만 호출
});

// SYS003(사용여부) 코드 목록 로드
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS003"] },
    });
    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      systCodeArr.value = grouped;
    }
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

const siteNo = ref("");
const siteNm = ref("");

const fnSearch = async () => {
  siteList.value = [];
  try {
    if (!proxy.$util.isEmpty(cmpnyCd.value)) {
      // 회원가입 모드는 NoAuth 전용 엔드포인트(/join-site-lists, 활성기간 필터 서버 적용),
      //   그 외에는 기존 인증 엔드포인트(/site-lists). 응답 스키마는 동일(siteInfoResultList).
      const endpoint = props.joinMode
        ? "/comApi/baseinfo/join-site-lists"
        : "/comApi/baseinfo/site-lists";
      const response = await axios.get(endpoint, {
        params: {
          cmpnyCd: cmpnyCd.value,
          siteNo: siteNo.value,
          siteNm: siteNm.value,
          // 일반 조회만 사용여부 필터 전달. 회원가입(join)은 서버가 'Y' 강제 → 무시됨
          useYn: props.joinMode ? "" : useYn.value,
        },
      });

      if (response.status === 200) {
        const resData = response.data?.siteInfoResultList || [];

        let filtered = resData;
        // 기존: 특정 사업장번호 제외(siteNo_n_p).
        if (proxy.$util.isNotEmpty(props.siteNo_n_p)) {
          filtered = filtered.filter((item) => item.siteNo != props.siteNo_n_p);
        }
        // PRAFTA-WEB_002-T1-04(1.3-2): 현재 소속 사업장(excludeSiteCd_p) 제외.
        if (proxy.$util.isNotEmpty(props.excludeSiteCd_p)) {
          filtered = filtered.filter(
            (item) => item.siteCd !== props.excludeSiteCd_p
          );
        }
        siteList.value = filtered;
      }
    }
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

function fnSelectRow(siteCd, siteNo, siteNm) {
  // emit("select", siteCd, siteNo, siteNm); // siteCd 부모에 전달
  props.onSelect(siteCd, siteNo, siteNm);
  emit("close"); // 팝업 닫기
}
</script>
