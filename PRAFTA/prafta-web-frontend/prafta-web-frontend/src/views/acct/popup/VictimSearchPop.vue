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
        <!-- 1. Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>재해자 검색 (정규 + 일용 · 다중 선택)</span>
          <button class="icon-button" @click="$emit('close')" aria-label="닫기">
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

        <!-- 2. 조회 Form (prafta-065: 다건 선택 — HazardSearchPop 계약) -->
        <div class="viewSearch">
          <div class="form-left">
            <label>사용자유형</label>
            <BaseSelect v-model="userTypeCd">
              <option value="">전체</option>
              <option value="REGULAR">정규</option>
              <option value="DAILY">일용</option>
            </BaseSelect>
            <label class="label-gap">사용자정보</label>
            <input
              v-model.trim="userNm"
              placeholder="이름 또는 ID"
              @keyup.enter="fnSearch"
            />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
            <button class="btn btn-primary" @click="fnSelect">선택 완료</button>
          </div>
        </div>

        <!-- 3. 그리드 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th class="check-col">
                    <input
                      type="checkbox"
                      :checked="isAllChecked"
                      :indeterminate.prop="isIndeterminate"
                      @change="toggleAll"
                    />
                  </th>
                  <th>유형</th>
                  <th>아이디</th>
                  <th>이름</th>
                  <th>휴대폰</th>
                  <th>사업장</th>
                  <th>소속</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!rows || rows.length === 0">
                  <tr>
                    <td colspan="7" class="edu-grid-empty">
                      조회된 재해자가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="item in rows"
                    :key="rowKey(item)"
                    :class="{
                      selected: selectedMap[rowKey(item)],
                      locked: lockedMap[rowKey(item)],
                    }"
                    @click="toggleRow(item)"
                  >
                    <td class="check-col">
                      <input
                        type="checkbox"
                        :checked="
                          !!selectedMap[rowKey(item)] ||
                          !!lockedMap[rowKey(item)]
                        "
                        :disabled="!!lockedMap[rowKey(item)]"
                        @click.stop
                        @change="toggleRow(item)"
                      />
                    </td>
                    <td>{{ item.userTypeNm || item.userTypeCd }}</td>
                    <td>{{ item.userId }}</td>
                    <td>
                      {{ item.userNm
                      }}<span
                        v-if="lockedMap[rowKey(item)]"
                        class="vs-locked-badge"
                        >등록됨</span
                      >
                    </td>
                    <td>
                      {{ item.mblNoLast4 ? "****" + item.mblNoLast4 : "-" }}
                    </td>
                    <td>{{ item.siteNm || "-" }}</td>
                    <td>{{ item.nodeNm || "-" }}</td>
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
  ref,
  reactive,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";

// prafta-065: 다건 선택 계약(HazardSearchPop 미러).
//   selectedKeys = 사전 선택(해제 가능) / lockedKeys = 이미 등록된 인원(체크 고정·해제 불가·반환 제외)
//   행 키 = userTypeCd + '_' + userCd (정규/일용 사용자코드 충돌 방지)
const props = defineProps({
  siteCd: { type: String, default: "" },
  selectedKeys: { type: Array, default: () => [] },
  lockedKeys: { type: Array, default: () => [] },
  onSelect: Function, // (list: VictimResult[]) => void — 잠기지 않은 체크 인원 전체
});
const emit = defineEmits(["close", "select"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const userTypeCd = ref("");
const userNm = ref("");
const rows = ref([]);
const selectedMap = reactive({}); // { [rowKey]: true }
const lockedMap = reactive({}); // { [rowKey]: true }

const rowKey = (it) => `${it.userTypeCd}_${it.userCd}`;

// 잠기지 않은 행만 전체체크 대상으로 계산한다.
const selectableRows = computed(() =>
  rows.value.filter((r) => !lockedMap[rowKey(r)])
);
const selectedCount = computed(
  () => selectableRows.value.filter((r) => selectedMap[rowKey(r)]).length
);
const isAllChecked = computed(
  () =>
    selectableRows.value.length > 0 &&
    selectedCount.value === selectableRows.value.length
);
const isIndeterminate = computed(
  () =>
    selectedCount.value > 0 && selectedCount.value < selectableRows.value.length
);

onMounted(() => {
  (props.lockedKeys || []).forEach((k) => {
    if (k) lockedMap[k] = true;
  });
  (props.selectedKeys || []).forEach((k) => {
    // 잠긴 인원은 selected 로 중복 표시하지 않는다(반환 대상에서 제외되어야 함).
    if (k && !lockedMap[k]) selectedMap[k] = true;
  });
  fnSearch();
});

const fnSearch = async () => {
  rows.value = [];
  try {
    const response = await axios.get("/webApi/acct01/victim-search", {
      params: {
        siteCd: props.siteCd,
        userNm: userNm.value,
        userTypeCd: userTypeCd.value,
      },
    });
    if (response.status === 200) {
      rows.value = response.data?.victimList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const toggleRow = (item) => {
  const key = rowKey(item);
  if (lockedMap[key]) return; // 등록된 인원은 해제 불가
  if (selectedMap[key]) delete selectedMap[key];
  else selectedMap[key] = true;
};

const toggleAll = () => {
  if (isAllChecked.value) {
    selectableRows.value.forEach((r) => delete selectedMap[rowKey(r)]);
  } else {
    selectableRows.value.forEach((r) => {
      selectedMap[rowKey(r)] = true;
    });
  }
};

// 선택 완료: 현재 조회 결과 중 체크된(잠기지 않은) 인원을 반환한다.
//   검색 조건을 바꿔 화면에서 사라진 사전선택 인원은 부모가 보존하므로(병합) 여기서는 조회 결과만 다룬다.
const fnSelect = async () => {
  const selected = rows.value.filter(
    (r) => selectedMap[rowKey(r)] && !lockedMap[rowKey(r)]
  );
  if (selected.length === 0) {
    await proxy.$alert("선택된 재해자가 없습니다.");
    return;
  }
  if (typeof props.onSelect === "function") {
    props.onSelect(selected);
  } else {
    emit("select", selected);
  }
  emit("close");
};
</script>

<style scoped>
/* 사용자유형 셀렉트가 BaseSelect 인라인 width:100% 때문에
   한 줄을 차지해 라벨 아래로 떨어지는 것을 방지(라벨과 같은 행 정렬) */
.viewSearch .form-left {
  align-items: center;
}
.viewSearch .form-left :deep(select) {
  width: 130px !important;
}
/* "사용자정보" 라벨을 앞 셀렉트와 조건 단위로 띄움(Acct_01 조건 간격 2rem 수준).
   .form-left gap 1rem + margin-left 1rem = 2rem */
.viewSearch .form-left .label-gap {
  margin-left: 1rem;
}
/* 사용자정보 입력칸을 기본(120px)보다 길게 */
.viewSearch .form-left input {
  width: 200px;
}
/* prafta-065 다건 선택 */
.check-col {
  width: 36px;
  text-align: center;
}
tbody tr {
  cursor: pointer;
}
tbody tr.selected {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.08));
}
tbody tr.locked {
  color: var(--color-text-muted, #8b94a3);
  cursor: default;
}
.vs-locked-badge {
  margin-left: 0.35rem;
  font-size: 0.64rem;
  font-weight: 700;
  padding: 0.05rem 0.35rem;
  border-radius: var(--radius-pill, 999px);
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #6b7280);
}
</style>
