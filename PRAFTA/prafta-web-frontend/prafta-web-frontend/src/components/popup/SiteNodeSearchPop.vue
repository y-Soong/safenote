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
          <span>부서 검색</span>
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
            <div>
              <label>부서번호</label>
              <input v-model="nodeCd" />
            </div>
            <div>
              <label>부서명</label>
              <input v-model="nodeNm" />
            </div>
            <div>
              <label>부서타입</label>
              <select v-model="nodeType" name="combo">
                <option
                  v-for="opt in baseCodeArr['COM004'] || []"
                  :key="opt.baimValDCd"
                  :value="opt.baimValDCd"
                >
                  {{ opt.baimValDNm }}
                </option>
              </select>
            </div>
            <div>
              <label>상위부서명</label>
              <input v-model="parentNodeNm" />
            </div>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 🔹 3. 그리드 영역 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <!-- PRAFTA-WEB_002-T1-04(1.3-3): 무담당 부서 포함 조회(includeNoAdmin_p=true) 시에는
                 "담당자가 정해진 부서만 조회됩니다" 안내를 숨긴다(담당 미지정 부서도 노출되므로). -->
            <p class="site-node-notice" v-if="!includeNoAdmin_p">
              담당자가 정해진 부서만 조회됩니다.
            </p>
            <table class="data-grid">
              <thead>
                <tr>
                  <ThSortable
                    label="부서번호"
                    col-key="nodeCd"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.nodeCd"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="부서명"
                    col-key="nodeNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.nodeNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="부서타입"
                    col-key="nodeType"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.nodeType"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="상위부서명"
                    col-key="parentNodeNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.parentNodeNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="자체근태승인여부"
                    col-key="selfAttdApprvYn"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.selfAttdApprvYn"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                </tr>
              </thead>
              <tbody>
                <template v-if="!nodeList || nodeList.length === 0">
                  <tr>
                    <td colspan="5" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="node in sortedData"
                    :key="node.nodeCd"
                    @dblclick="fnSelectRow(node.nodeCd, node.nodeNm)"
                  >
                    <td>{{ node.nodeCd }}</td>
                    <td>{{ node.nodeNm }}</td>
                    <td>
                      <BaseSelect
                        v-model="node.nodeType"
                        name="combo"
                        :readonly="true"
                      >
                        <option
                          v-for="opt in baseCodeArr['COM004'] || []"
                          :key="opt.baimValDCd"
                          :value="opt.baimValDCd"
                        >
                          {{ opt.baimValDNm }}
                        </option>
                      </BaseSelect>
                    </td>
                    <td>{{ node.parentNodeNm }}</td>
                    <td>{{ node.selfAttdApprvYn }}</td>
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
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import BaseSelect from "@/components/common/BaseSelect.vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

const { proxy } = getCurrentInstance();

const props = defineProps({
  visible: Boolean,
  cmpnyCd_p: String,
  siteCd_p: String,
  nodeCd_p: String,
  nodeNm_p: String,
  userId_p: String,
  onSelect: Function,
  // PRAFTA-WEB_002-T1-04(1.3-3)/T1-02(1.4-1): true 면 담당(정/부) 미지정 부서도 조회 결과에 포함한다.
  //   소속이동/생성의 정규직 컨텍스트에서 true. 미지정(false) 시 기존 동작(담당 지정 부서만).
  includeNoAdmin_p: { type: Boolean, default: false },
  // PRAFTA-001-3: 회원가입(비로그인) 모드. true 면 토큰이 없어도 동작하도록 NoAuth 전용 부서조회
  //   엔드포인트(/join-site-node-lists)를 사용하고, 인증이 필요한 부서타입 코드(COM004) 로드는 생략한다.
  //   (SiteSearchPop.joinMode 패턴과 동일). 미지정(false) 시 기존 인증 경로 그대로.
  joinMode: { type: Boolean, default: false },
});

const emit = defineEmits(["select", "close"]);
const modalRef = ref(null);
const nodeList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(nodeList);
const { colWidths, onResize } = useColumnResize({
  nodeCd: 110,
  nodeNm: 140,
  nodeType: 100,
  parentNodeNm: 130,
  selfAttdApprvYn: 120,
});
const baseCodeArr = ref([]);

const nodeCd = ref(props.nodeCd_p || "");
const nodeNm = ref(props.nodeNm_p || "");
const nodeType = ref("");
const parentNodeNm = ref("");

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(async () => {
  // 부서타입 코드(COM004)는 인증 필요 엔드포인트라 회원가입(비로그인) 모드에선 로드를 생략한다.
  //   (미로드 시 nodeType 필터 미적용 → 부서 전체 조회. SiteSearchPop.joinMode 선례와 동일)
  if (!props.joinMode) await fnGetBaseinfoList();
  await fnSearch();
});

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd") || props.cmpnyCd_p,
        baseCodeList: ["COM004"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseCodeArr.value = grouped;

      nodeType.value = baseCodeArr.value.COM004[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  nodeList.value = [];
  try {
    // PRAFTA-001-3: 회원가입(비로그인) 모드는 NoAuth 전용 엔드포인트(/join-site-node-lists) 사용,
    //   그 외에는 기존 인증 엔드포인트(/site-node-lists). 응답 스키마는 동일(siteNodeInfoList).
    const endpoint = props.joinMode
      ? "/comApi/baseinfo/join-site-node-lists"
      : "/comApi/baseinfo/site-node-lists";
    const response = await axios.get(endpoint, {
      params: {
        cmpnyCd: props.cmpnyCd_p,
        siteCd: props.siteCd_p,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
        nodeType: nodeType.value,
        parentNodeNm: parentNodeNm.value,
        // PRAFTA-WEB_002-T1-04(1.3-3)/T1-02(1.4-1): 담당 미지정 부서 포함 여부(정규직 컨텍스트면 true).
        includeNoAdmin: props.includeNoAdmin_p,
      },
    });

    if (response.status === 200) {
      nodeList.value = response.data?.siteNodeInfoList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSelectRow = (nodeCd, nodeNm) => {
  // emit("select", siteCd, siteNo, siteNm); // SITE_CD 부모에 전달
  if (proxy.$util.isEmpty(props.userId_p)) {
    props.onSelect(nodeCd, nodeNm);
  } else {
    props.onSelect(props.userId_p, nodeCd, nodeNm);
  }

  emit("close"); // 팝업 닫기
};
</script>

<style scoped>
.site-node-notice {
  color: #dc2626;
  font-size: 0.875rem;
  margin: 0 0 0.5rem 0;
}
</style>
