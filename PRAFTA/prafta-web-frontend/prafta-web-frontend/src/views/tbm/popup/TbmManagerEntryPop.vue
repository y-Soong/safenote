<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup" @click.self="$emit('close')">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>대리 입실 처리</span>
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

        <!-- 탭(정규직 / 일용직) -->
        <div class="tab-bar">
          <button
            type="button"
            class="tab-item"
            :class="{ active: userTypeCd === 'REGULAR' }"
            @click="fnSwitchTab('REGULAR')"
          >
            정규직
          </button>
          <button
            type="button"
            class="tab-item"
            :class="{ active: userTypeCd === 'DAILY' }"
            @click="fnSwitchTab('DAILY')"
          >
            일용직
          </button>
        </div>

        <p class="tab-hint">
          {{ userTypeCd === "REGULAR"
            ? "휴대전화 사용이 불가한 정규직을 검색해 관리자 권한으로 입실 처리합니다."
            : "만료되지 않은 일용직만 검색됩니다. 관리자 권한으로 입실 처리합니다." }}
        </p>

        <!-- 검색 -->
        <div class="viewSearch">
          <div class="form-left">
            <label>이름 / 아이디</label>
            <input v-model="keyword" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 그리드 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>이름</th>
                  <th>아이디</th>
                  <th>사업장</th>
                  <th>상태</th>
                  <th>처리</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="isLoading">
                  <td colspan="5" class="grid-msg">조회 중...</td>
                </tr>
                <tr v-else-if="candidates.length === 0">
                  <td colspan="5" class="grid-msg">대상 사용자가 없습니다.</td>
                </tr>
                <tr v-for="row in candidates" :key="row.userCd">
                  <td>{{ row.userNm }}</td>
                  <td>{{ row.userId }}</td>
                  <td>{{ row.siteNm || row.siteCd }}</td>
                  <td>
                    <span :class="row.alreadyEntered ? 'tag-entered' : 'tag-pending'">
                      {{ row.alreadyEntered ? "입실됨" : "미입실" }}
                    </span>
                  </td>
                  <td>
                    <button
                      class="btn btn-second btn-sm"
                      :disabled="row.alreadyEntered || isBusy"
                      @click="fnManagerEnter(row)"
                    >
                      입실 처리
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const { proxy } = getCurrentInstance();

const props = defineProps({
  sessionCd_p: String,
  siteCd_p: String,
  onSearch: Function,
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);
const userTypeCd = ref("REGULAR"); // REGULAR | DAILY
const keyword = ref("");
const candidates = ref([]);
const isLoading = ref(false);
const isBusy = ref(false);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(() => {
  fnSearch();
});

const fnSwitchTab = (type) => {
  if (userTypeCd.value === type) return;
  userTypeCd.value = type;
  candidates.value = [];
  fnSearch();
};

// 입실 후보 검색(정규직/일용직). EntryCandidateResponse: { userTypeCd, candidateList }
const fnSearch = async () => {
  if (isLoading.value) return;
  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/tbm02/entry-candidates", {
      params: {
        sessionCd: props.sessionCd_p,
        userTypeCd: userTypeCd.value,
        keyword: keyword.value,
      },
    });

    if (response.status === 200) {
      candidates.value = response.data?.candidateList || [];
    }
  } catch (err) {
    candidates.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

// 관리자 직접 입실(MANAGER_DIRECT). 성공 시 행 입실표시 + 부모 갱신
const fnManagerEnter = async (row) => {
  if (row.alreadyEntered) return;
  const ok = await proxy.$confirm(
    `${row.userNm} 님을 관리자 권한으로 입실 처리하시겠습니까?`
  );
  if (!ok) return;
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/manager-enter",
      {
        sessionCd: props.sessionCd_p,
        userTypeCd: userTypeCd.value,
        userCd: row.userCd,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      row.alreadyEntered = true;
      await proxy.$alert(`${row.userNm} 님이 입실 처리되었습니다.`);
      if (typeof props.onSearch === "function") props.onSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "입실 처리 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};
</script>

<style scoped>
.tab-bar {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem 0;
}

.tab-item {
  height: var(--btn-height);
  padding: 0 var(--btn-padding);
  font-size: var(--btn-font);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
}

.tab-item.active {
  background: var(--color-primary);
  color: var(--color-surface);
  border-color: var(--color-primary);
}

.tab-hint {
  margin: 0;
  padding: 0.5rem 1rem;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.grid-msg {
  text-align: center;
  padding: 1rem;
  color: var(--color-text-muted);
}

.tag-entered {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.tag-pending {
  color: var(--color-warning-text);
  font-size: var(--btn-font-sm);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
