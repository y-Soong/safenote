<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>사업장 연동 제안</span>
          <button class="icon-button" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <p class="reg-guide">
            연동 중인 회사에 내 사업장을 제안합니다. 상대 회사가 수락하면 사업장과 근무타입이
            상대 회사에 복제(미러)되며, 이후 내 수정 사항이 자동 반영됩니다.
          </p>

          <div class="form-row-max">
            <label>대상 회사</label>
            <select v-model="tgtCmpnyCd">
              <option value="">선택하세요</option>
              <option v-for="c in cmpnyList" :key="c.cmpnyCd" :value="c.cmpnyCd">
                {{ c.cmpnyNm }} ({{ c.cmpnyCd }})
              </option>
            </select>
          </div>

          <div class="form-row-max">
            <label>내 사업장</label>
            <select v-model="siteCd">
              <option value="">선택하세요</option>
              <option v-for="s in siteList" :key="s.siteCd" :value="s.siteCd">
                {{ s.siteNm }}<template v-if="s.linkYn === 'Y'"> [연동받은 사업장]</template>
              </option>
            </select>
          </div>

          <p class="propose-note">
            연동받은(미러) 사업장도 재제안할 수 있습니다. 단, 연동 출처(상위) 회사로는 다시 제안할 수 없습니다.
          </p>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">취소</button>
            <button class="btn btn-primary" :disabled="!tgtCmpnyCd || !siteCd" @click="fnPropose">연동 제안</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const cmpnyList = ref([]); // 관계 ACTIVE 상대 회사 목록
const siteList = ref([]);  // 내 활성 사업장 목록(미러 포함 — linkYn)
const tgtCmpnyCd = ref("");
const siteCd = ref("");

// 제안 중복 클릭 방지 플래그.
const saving = ref(false);

// =========================== Life Cycle ===========================
// 후보 조회 — GET /webApi/subcon02/link-propose-candidates.
//   관계 수립 회사가 0건이면 제안 자체가 불가하므로 안내 후 닫는다.
onMounted(async () => {
  try {
    const response = await axios.get("/webApi/subcon02/link-propose-candidates");

    if (response.status === 200) {
      cmpnyList.value = response.data?.cmpnyList || [];
      siteList.value = response.data?.siteList || [];

      if (!cmpnyList.value.length) {
        await proxy.$alert("연동 중인 회사가 없습니다.\n연동회사 관리에서 먼저 관계를 수립하세요.");
        emit("close");
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
    emit("close");
  }
});

// =========================== Methods ===========================
// 연동 제안 — POST /webApi/subcon02/site-link-propose { tgtCmpnyCd, siteCd }.
//   루프 차단·중복 링크 등 서버 가드 실패는 서버 에러코드 메시지 그대로 표기.
const fnPropose = async () => {
  if (!tgtCmpnyCd.value || !siteCd.value) {
    await proxy.$alert("대상 회사와 사업장을 선택해주세요.");
    return;
  }

  const cmpnyNm = cmpnyList.value.find((c) => c.cmpnyCd === tgtCmpnyCd.value)?.cmpnyNm || tgtCmpnyCd.value;
  const siteNm = siteList.value.find((s) => s.siteCd === siteCd.value)?.siteNm || siteCd.value;

  const ok = await proxy.$confirm(`'${cmpnyNm}'에 '${siteNm}' 연동을 제안하시겠습니까?`);
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon02/site-link-propose", {
      tgtCmpnyCd: tgtCmpnyCd.value,
      siteCd: siteCd.value,
    });

    if (response.status === 200) {
      await proxy.$alert("연동을 제안했습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "연동 제안 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.form-row-max {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}
.form-row-max label {
  width: 90px;
  flex-shrink: 0;
  color: var(--color-text-muted, #6b7280);
}
.form-row-max select {
  flex: 1;
  min-width: 0;
}
.propose-note {
  margin-top: var(--space-md, 0.75rem);
  color: var(--color-text-muted, #6b7280);
  font-size: var(--btn-font-sm, 12px);
}
</style>
