<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
    />

    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane org-chart-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">사업장 조직도</span>
          </div>
          <div class="subtitle" v-if="siteNm">
            <span class="subtitle-text">[{{ siteNm }}]</span>
          </div>
          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnNodeCopySiteSearchPopOpen">
              타사업장 조직노드 가져오기
            </button>
            <button class="btn btn-custom" @click="fnSiteNodeAllDelete">
              조직노드 일괄 삭제
            </button>
          </div>
        </div>

        <div class="org-chart-box" style="--box-h: 70vh">
          <div v-if="!siteCd" class="org-chart-empty">
            사업장을 선택해주세요.
          </div>
          <div v-else class="org-chart-container">
            <div v-if="rootNode" class="org-chart-tree">
              <OrgChartNode
                :node="rootNode"
                :type-options="typeOptionsFromBase"
                :level="0"
                :branch-index="0"
                :branch-color="branchColors[0]"
                @add-child="fnAddChild"
                @delete-node="fnDeleteNode"
                @update-node="fnUpdateNode"
              />
            </div>
            <div v-else class="org-chart-empty-inner">
              <p>조직이 없습니다.</p>
              <button class="btn btn-primary" @click="fnAddRootNode">
                최상위 구성요소 추가
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import OrgChartNode from "./components/OrgChartNode.vue";

// ================ Options ================
defineOptions({ name: "Baim_06" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const localButtons = ref({ ...props.buttons });
const baseInfoArr = ref([]);

// 조회조건 변수
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);

// 조직도 트리 데이터
const orgTree = ref(null);

const branchColors = ["#16a34a", "#6366f1", "#ea580c", "#059669", "#7c3aed"];

// ================ Computed ================
const rootNode = computed(() => orgTree.value);
// 구성요소 유형: baseInfoArr COM004 → OrgChartNode typeOptions
const typeOptionsFromBase = computed(() => baseInfoArr.value["COM004"] ?? []);

// ================ Life Cycle Functions ================
onMounted(async () => {
  fnButtonControll();
  await fnGetBaseinfoList();
});

// ================ Org Tree ↔ Flat 변환 (mock 제거 후에도 유지) ================
/**
 * 플랫 배열(DB 조회 결과) → 트리 구조(root + children) 변환
 */
const flatToOrgTree = (flatList) => {
  if (!Array.isArray(flatList) || flatList.length === 0) return { root: null };
  const list = flatList.map((n) => ({
    ...n,
    nodeCd: n.nodeCd ?? n.nodeId ?? n.orgNodeId,
    parentNodeCd: n.parentNodeCd ?? n.parentNodeId ?? n.parentOrgNodeCd,
    nodeNm: n.nodeNm ?? n.label,
    nodeType: n.nodeType ?? n.type,
    selfAttdApprvYn: n.selfAttdApprvYn ?? n.selfAttendanceApprovalYn,
    children: [],
  }));
  const byId = new Map(list.map((n) => [n.nodeCd, n]));
  const root = list.find((n) => n.parentNodeCd == null);
  if (!root) return { root: null };
  for (const node of list) {
    if (node.parentNodeCd == null) continue;
    const parent = byId.get(node.parentNodeCd);
    if (parent) parent.children.push(node);
  }
  const sortByOrder = (nodes) => {
    if (nodes.some((n) => n.sortOrder != null)) {
      nodes.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
    }
    nodes.forEach((n) => sortByOrder(n.children));
  };
  sortByOrder(root.children);
  return { root };
};

/**
 * 트리 → 플랫 배열. type 은 typeOptions 기준 코드 값(baimValDCd)으로 출력
 * @param {Object} options.typeOptions - baseInfo COM004 형태 배열 (baimValDCd, baimValDNm)
 */
const orgTreeToFlat = (tree, options = {}) => {
  const {
    includeSortOrder = true,
    includeDepthLevel = true,
    typeOptions = [],
  } = options;
  const root = tree?.root ?? tree;
  if (!root || typeof root !== "object") return [];

  const result = [];
  const typeToCode = (typeVal) => {
    if (!typeVal) return typeVal;
    const opt = typeOptions.find(
      (o) =>
        (o.baimValDCd ?? o.value) === typeVal ||
        (o.baimValDNm ?? o.label) === typeVal
    );
    return opt ? opt.baimValDCd ?? opt.value : typeVal;
  };

  const visit = (node, parentNode, sortOrder) => {
    const depthLevel =
      parentNode == null ? 0 : (parentNode.depthLevel ?? -1) + 1;
    const row = {
      siteCd: siteCd.value,
      nodeCd: node.nodeCd,
      parentNodeCd: parentNode?.nodeCd ?? null,
      nodeNm: node.nodeNm,
      nodeType: typeToCode(node.nodeType),
      managerCnt: node.managerCnt ?? 0,
      workerCnt: node.workerCnt ?? 0,
      selfAttdApprvYn: node.selfAttdApprvYn ? "Y" : "N" ?? "N",
    };
    if (includeSortOrder) row.sortOrder = sortOrder;
    if (includeDepthLevel) row.depthLevel = depthLevel;
    result.push(row);

    const children = Array.isArray(node.children) ? node.children : [];
    children.forEach((child, idx) =>
      visit(child, { ...node, depthLevel }, idx + 1)
    );
  };

  visit(root, null, 1);
  return result;
};

// ================ API Functions ================
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-list", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM004"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (item.baimValDCd == null) return;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseInfoArr.value = grouped;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }
  try {
    const response = await axios.get("/webApi/baim06/site-node-lists", {
      params: { siteCd: siteCd.value },
    });
    if (response.status !== 200) {
      orgTree.value = null;
      return;
    }
    const data = response.data?.siteNodeList;

    if (Array.isArray(data)) {
      const { root } = flatToOrgTree(data);
      orgTree.value = root;
      return;
    }
    orgTree.value = null;
  } catch (err) {
    orgTree.value = null;
  }
};

const fnSave = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }
  if (!orgTree.value) {
    proxy.$alert("저장할 조직 데이터가 없습니다.");
    return;
  }

  const flatList = orgTreeToFlat(orgTree.value, {
    includeSortOrder: true,
    includeDepthLevel: true,
    typeOptions: typeOptionsFromBase.value,
  });

  const ok = await proxy.$confirm("저장하시겠습니까?");
  if (!ok) return;
  try {
    // 백엔드가 트리(root)를 받는 경우
    await axios.post("/webApi/baim06/save-site-nodes", flatList);

    proxy.$alert("처리되었습니다.");
    fnSearch();
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

/** 저장된 노드 삭제 API 호출 후 재조회 */
const fnDeleteOrgNode = async (node) => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  const ok = await proxy.$confirm("해당 노드를 삭제하시겠습니까?");
  if (!ok) return;

  try {
    await axios.post("/webApi/baim06/delete-site-nodes", node);
    proxy.$alert("삭제되었습니다.");
    await fnSearch();
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "삭제 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnSiteNodeAllDelete = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }

  const ok = await proxy.$confirm(
    "[" + siteNm.value + "] 사업장의 조직도를\n일괄 삭제하시겠습니까?"
  );
  if (!ok) return;

  try {
    await axios.post("/webApi/baim06/delete-site-all-nodes", {
      siteCd: siteCd.value,
    });
    proxy.$alert("삭제되었습니다.");
    await fnSearch();
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "삭제 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnNodeCopy = async (siteCdVal, siteNoVal, siteNmVal) => {
  const ok = await proxy.$confirm(
    "[" +
      siteNm.value +
      "] 사업장의 조직도를\n[" +
      siteNmVal +
      "] 사업장 조직도로\n덮어쓰기 하시겠습니까 ?"
  );
  if (!ok) return;

  try {
    await axios.post("/webApi/baim06/copy-site-nodes", {
      siteCd: siteCd.value,
      targetSiteCd: siteCdVal,
    });

    fnSearch();
    proxy.$alert("조직노드 덮어쓰기가 완료되었습니다.");
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조직노드 덮어쓰기 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/getSiteInfoList", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo: siteNo.value,
      siteNm: siteNm.value,
    });
    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
    }
  }
};

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url?.split("/").pop();
    if (apiId === "getSiteInfoList") {
      if (res.data?.length === 1) {
        siteCd.value = res.data[0].SITE_CD;
        siteNo.value = res.data[0].SITE_NO;
        siteNm.value = res.data[0].SITE_NM;
        fnSearch();
      } else if (res.data?.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
      }
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  fnSearch();
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnNodeCopySiteSearchPopOpen = () => {
  console.log("siteNo.value :: ", siteNo.value);
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }

  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_n_p: siteNo.value,
    onSelect: fnNodeCopy,
  });
};

// 현재 트리에서 가장 큰 번호를 찾아 +1 한 값으로 nodeCd 생성
const nextNodeCd = () => {
  const root = orgTree.value;
  if (!root) return "n1";

  let max = 0;
  const stack = [root];

  while (stack.length) {
    const cur = stack.pop();
    if (cur?.nodeCd && /^n\d+$/.test(cur.nodeCd)) {
      const num = parseInt(cur.nodeCd.slice(1), 10);
      if (!Number.isNaN(num) && num > max) max = num;
    }
    if (Array.isArray(cur?.children) && cur.children.length) {
      stack.push(...cur.children);
    }
  }

  const next = max + 1;
  return `n${next || 1}`;
};

const fnAddRootNode = () => {
  if (!siteCd.value) return;
  const opts = typeOptionsFromBase.value;
  const firstTypeCode = opts[0]?.baimValDCd ?? opts[0]?.value ?? "";
  const nodeCd = nextNodeCd();
  orgTree.value = {
    nodeCd,
    parentNodeCd: null,
    nodeNm: "구성요소명",
    nodeType: firstTypeCode,
    managerCnt: 0,
    workerCnt: 0,
    selfAttdApprvYn: false,
    isNew: true,
    children: [],
  };
};

const fnAddChild = (parentNode) => {
  const opts = typeOptionsFromBase.value;
  const parentNodeType = parentNode.nodeType;
  const parentIdx = opts.findIndex(
    (o) => (o.baimValDCd ?? o.value) === parentNodeType
  );
  const nextIdx = Math.min(parentIdx + 1, opts.length - 1);
  const nextTypeCode = opts[nextIdx]?.baimValDCd ?? opts[nextIdx]?.value ?? "";
  const nodeCd = nextNodeCd();
  const parentKey =
    parentNode.nodeCd ??
    parentNode.nodeId ??
    parentNode.orgNodeId ??
    parentNode.id;
  const child = {
    nodeCd,
    parentNodeCd: parentKey,
    nodeNm: "구성요소명",
    nodeType: nextTypeCode,
    managerCnt: 0,
    workerCnt: 0,
    selfAttdApprvYn: false,
    isNew: true,
    children: [],
  };
  if (!parentNode.children) parentNode.children = [];
  parentNode.children.push(child);
};

const fnDeleteNode = async (payload) => {
  const { node, parentNode, parentChildren, indexInParent: index } = payload;
  const isNewNode = Boolean(node?.isNew);

  if (isNewNode) {
    if (parentNode == null) {
      orgTree.value = null;
      return;
    }
    if (Array.isArray(parentChildren) && index >= 0) {
      parentChildren.splice(index, 1);
    }
    return;
  }

  await fnDeleteOrgNode(node);
};

const fnUpdateNode = (node, field, value) => {
  if (node && field) node[field] = value;
};
</script>

<style scoped>
.org-chart-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.org-chart-box {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-sm, 8px);
  background: linear-gradient(180deg, #fafafa 0%, #f5f5f5 100%);
  padding: 24px;
}

.org-chart-empty,
.org-chart-empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: var(--color-text-muted, #6b7280);
  gap: 12px;
}

.org-chart-container {
  display: inline-block;
  min-width: 100%;
}
</style>
