<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup" @click.self="$emit('close')">
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
            <input v-model="sr_userId" />
            <label>사용자명</label>
            <input v-model="sr_userNm" />
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
                  <th>아이디</th>
                  <th>이름</th>
                  <th>전화번호</th>
                  <th>이메일</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="user in userActList"
                  :key="user.USER_ID"
                  @dblclick="
                    fnSelectRow(
                      user.USER_ID,
                      user.USER_NM,
                      user.MBL_NO,
                      user.EMAIL
                    )
                  "
                >
                  <td>{{ user.USER_ID }}</td>
                  <td>{{ user.USER_NM }}</td>
                  <td>{{ user.MBL_NO }}</td>
                  <td>{{ user.EMAIL }}</td>
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

const modalRef = ref(null);
const userActList = ref([]);
const cmpnyCd = ref("");
const sr_userId = ref("");
const sr_userNm = ref("");

const { proxy } = getCurrentInstance();

const props = defineProps({
  cmpnyCd_p: String,
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

  try {
    const response = await axios.post("/webApi/user01/getUserInfoList", {
      cmpnyCd: cmpnyCd.value,
      userId: sr_userId.value,
      userNm: sr_userNm.value,
    });

    if (response.status === 200) {
      userActList.value = response.data;
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message);
  }
};

/* user function */
function fnSelectRow(userId, userNm, mblNo, email) {
  // emit("select", siteCd, siteNo, siteNm); // SITE_CD 부모에 전달
  props.onSelect(userId, userNm, mblNo, email);
  emit("close"); // 팝업 닫기
}

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}
</script>
