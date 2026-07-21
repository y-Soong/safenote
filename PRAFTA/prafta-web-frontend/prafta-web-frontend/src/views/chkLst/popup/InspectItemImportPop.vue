<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 1. Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>타 사업장 점검문항 가져오기</span>
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

        <!-- 2. 원본 사업장 / 점검구분 -->
        <div class="viewSearch">
          <div>
            <label>원본 사업장</label>
            <input
              type="text"
              v-model="srcSiteNo"
              placeholder="사업장코드"
              readonly
            />
            <button class="search-btn" @click="fnSrcSiteSearchPopOpen">
              <img class="search_icon" :src="search_icon" alt="검색" />
            </button>
            <input
              type="text"
              v-model="srcSiteNm"
              placeholder="사업장명"
              readonly
            />
          </div>
          <div>
            <label>점검구분</label>
            <input type="text" :value="chkLstTypeNm_p" disabled />
          </div>
        </div>

        <!-- 3. 안내 -->
        <div class="import-notice">
          선택한 문항은 현재 사업장 [{{ chkLstTypeNm_p }}] 목록 뒤에 추가되며,
          문항코드는 새로 발급되고 시행일은 오늘로 초기화됩니다(저장 후 수정
          가능). 변경이력은 '등록'부터 새로 시작합니다.
        </div>

        <!-- 4. 문항 미리보기 그리드 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th style="text-align: center; width: 6%">No</th>
                  <th style="width: 6%">
                    <input
                      id="importHeadChk"
                      v-model="headChk"
                      type="checkbox"
                      @click="fnHeadChk"
                    />
                  </th>
                  <th style="width: 12%">정렬순서</th>
                  <th>점검항목명</th>
                  <th style="width: 10%">사용여부</th>
                  <th style="width: 14%">시행일</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!srcSiteCd">
                  <tr>
                    <td colspan="6" class="import-grid-empty">
                      원본 사업장을 선택하세요.
                    </td>
                  </tr>
                </template>
                <template v-else-if="!previewList || previewList.length === 0">
                  <tr>
                    <td colspan="6" class="import-grid-empty">
                      가져올 문항이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="(item, idx) in previewList"
                    :key="item.inspectItemCd"
                  >
                    <td style="text-align: center">{{ idx + 1 }}</td>
                    <td>
                      <input type="checkbox" v-model="item.chk" />
                    </td>
                    <td style="text-align: center">
                      {{ item.sortIdx ?? "-" }}
                    </td>
                    <td>{{ item.inspectItemSubj }}</td>
                    <td>{{ item.useYn === "N" ? "미사용" : "사용" }}</td>
                    <td>{{ formatStrDate(item.strDate) }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <!-- 5. Footer (guide css 가 밴드 스타일 자동 적용) -->
          <div class="btn-group">
            <button
              class="btn btn-custom"
              :disabled="!srcSiteCd || checkedCount === 0 || isLoading"
              @click="fnImport"
            >
              가져오기 ({{ checkedCount }}건)
            </button>
            <button class="btn btn-custom" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, defineProps, defineEmits, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";

// 대상(현재) 사업장/점검구분 — 부모(ChkLst_02)가 전달
const props = defineProps({
  dstSiteCd_p: String,   // 대상 사업장코드(현재 화면 사업장)
  dstSiteNo_p: String,   // 대상 사업장번호(SiteSearchPop 제외 조건용)
  dstSiteNm_p: String,   // 대상 사업장명(confirm 문구용)
  chkLstType_p: String,  // 점검구분 코드(COM001)
  chkLstTypeNm_p: String, // 점검구분명(표시용)
});

const emit = defineEmits(["close", "imported"]);

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ 반응형 상태 ================
const srcSiteCd = ref("");   // 원본 사업장코드
const srcSiteNo = ref("");
const srcSiteNm = ref("");
const previewList = ref([]); // 원본 사업장 문항 미리보기(행에 chk 부여)
const headChk = ref(false);
const isLoading = ref(false);

const checkedCount = computed(
  () => previewList.value.filter((item) => item.chk).length
);

// 시행일 표기 YYYYMMDD -> YYYY-MM-DD (InspectItemHistPop 과 동일 규칙)
const formatStrDate = (strDate) => {
  const s = String(strDate ?? "");
  if (s.length === 8)
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4, 6)}`;
  return s;
};

// ================ UI 토글 ================
function fnHeadChk() {
  headChk.value = !headChk.value;
  previewList.value.forEach((item) => {
    item.chk = headChk.value;
  });
}

// 원본 사업장 선택 팝업 (현재/대상 사업장 제외 — Baim_06 siteNo_n_p 준거, 중첩 오버레이는 guide css .prafta-nested-modal-overlay 지원)
const fnSrcSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_n_p: props.dstSiteNo_p,
    onSelect: onSrcSiteSelected,
  });
};

const onSrcSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  srcSiteCd.value = siteCdVal;
  srcSiteNo.value = siteNoVal;
  srcSiteNm.value = siteNmVal;
  fnPreviewSearch();
};

// ================ API ================
// 원본 사업장 문항 미리보기 조회 — 전체 표시 + 기본 체크는 사용('Y') 문항만
const fnPreviewSearch = async () => {
  previewList.value = [];
  headChk.value = false;

  if (proxy.$util.isEmpty(srcSiteCd.value)) return;

  isLoading.value = true;
  try {
    const response = await axios.get(
      "/webApi/chkLst02/chkpt-inspect-item-lists",
      {
        params: {
          siteCd: srcSiteCd.value,
          codeCd: props.chkLstType_p,
        },
      }
    );

    if (response.status === 200) {
      const resList = response.data?.chkptInspectItemResultList || [];
      previewList.value = resList.map((item) => ({
        ...item,
        chk: item.useYn !== "N", // 기본 체크 = 사용('Y') 문항만
      }));
      headChk.value =
        previewList.value.length > 0 &&
        previewList.value.every((item) => item.chk);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// 가져오기 실행 — 확인 후 copy API 호출, 성공 시 부모 재조회 + 팝업 닫기
const fnImport = async () => {
  // 체크 행 0건 방어(버튼 disabled 와 이중)
  const checkedItems = previewList.value.filter((item) => item.chk);
  if (checkedItems.length === 0) {
    await proxy.$alert("가져올 문항을 선택해주세요.");
    return;
  }

  // 중복 클릭 방지
  if (isLoading.value) return;

  const ok = await proxy.$confirm(
    `'${srcSiteNm.value}'의 문항 ${checkedItems.length}건을\n'${props.dstSiteNm_p}'(으)로 가져오시겠습니까?`
  );
  if (!ok) return;

  isLoading.value = true;
  try {
    const response = await axios.post(
      "/webApi/chkLst02/copy-chkpt-inspect-items",
      {
        srcSiteCd: srcSiteCd.value,
        dstSiteCd: props.dstSiteCd_p,
        chkLstType: props.chkLstType_p,
        inspectItemCdList: checkedItems.map((item) => item.inspectItemCd),
      }
    );

    if (response.status === 200) {
      await proxy.$alert(`${checkedItems.length}건을 가져왔습니다.`);
      emit("imported");
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "가져오기 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* 안내 밴드 — 팝업 헤더 톤(guide 토큰)만 재사용, 하드코딩 색상 없음 */
.import-notice {
  flex-shrink: 0;
  margin: 0 20px;
  padding: 10px 12px;
  background: var(--modal-header-bg);
  border: 1px solid var(--modal-border);
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
}

/* 빈 상태 셀 */
.import-grid-empty {
  text-align: center;
  padding: 24px 0;
}
</style>
