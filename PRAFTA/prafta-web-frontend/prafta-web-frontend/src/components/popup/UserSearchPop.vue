<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 1. Title 영역 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>사용자 검색</span>
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
            <label>아이디</label>
            <input v-model="userId" />
            <label>사용자명</label>
            <input v-model="userNm" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnGetUserInfoList">
              조회
            </button>
          </div>
        </div>

        <!-- 🔹 3. 그리드 영역 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <ThSortable
                    label="아이디"
                    col-key="userId"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.userId"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="이름"
                    col-key="userNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.userNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="전화번호"
                    col-key="mblNo"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.mblNo"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="이메일"
                    col-key="email"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.email"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="user in sortedData"
                  :key="user.userId"
                  @dblclick="
                    fnSelectRow(
                      user.userCd,
                      user.userNm,
                      user.mblNo,
                      user.email
                    )
                  "
                >
                  <td>{{ user.userId }}</td>
                  <td>{{ user.userNm }}</td>
                  <td>{{ user.mblNo }}</td>
                  <td>{{ user.email }}</td>
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
import {
  ref,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

const modalRef = ref(null);
const userActList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(userActList);
const { colWidths, onResize } = useColumnResize({
  userId: 120,
  userNm: 120,
  mblNo: 130,
  email: 180,
});
const cmpnyCd = ref("");
const userId = ref("");
const userNm = ref("");

const { proxy } = getCurrentInstance();

const props = defineProps({
  cmpnyCd_p: String,
  siteCd_p: String,
  nodeCd_p: String,
  searchMode_p: String,
  onSelect: Function,
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const emit = defineEmits(["select", "close"]);

onMounted(async () => {
  cmpnyCd.value = props.cmpnyCd_p;
  fnGetUserInfoList();
});

// API 호출
const fnGetUserInfoList = async () => {
  userActList.value = [];
  let apiUrl = "/webApi/user01/user-info-lists";
  let params = {
    userId: userId.value,
    userNm: userNm.value,
  };

  if (props.searchMode_p === "siteNodeAdmin") {
    apiUrl = "/webApi/user01/site-node-admin-candidate-lists";
    params = {
      userId: userId.value,
      userNm: userNm.value,
      siteCd: props.siteCd_p,
      nodeCd: props.nodeCd_p,
    };
  }

  try {
    const response = await axios.get(apiUrl, {
      params: params,
    });

    if (response.status === 200) {
      userActList.value = response.data.userInfoList;
    }
  } catch (err) {
    fnAlertMsg(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

/* user function */
const fnSelectRow = (userCd, userNm, mblNo, email) => {
  // emit("select", siteCd, siteNo, siteNm); // SITE_CD 부모에 전달
  props.onSelect(userCd, userNm, mblNo, email);
  emit("close"); // 팝업 닫기
};

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}
</script>
