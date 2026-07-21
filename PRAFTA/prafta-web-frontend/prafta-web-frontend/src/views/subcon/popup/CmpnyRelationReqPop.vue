<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>연동 요청</span>
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

        <div class="form-container">
          <p class="reg-guide">
            연동할 회사의 회사코드를 정확히 입력해 조회하세요. 요청은 상대 회사
            관리자가 수락해야 연동됩니다.
          </p>

          <div class="form-row-max">
            <label>회사코드</label>
            <input
              id="tgtCmpnyCd"
              ref="cmpnyCdFcs"
              v-model.trim="tgtCmpnyCd"
              placeholder="상대 회사코드 (정확일치)"
              maxlength="50"
              @keyup.enter="fnSearchCmpny"
            />
            <button class="btn btn-sm btn-primary" @click="fnSearchCmpny">
              조회
            </button>
          </div>

          <!-- 조회 결과 카드: found=결과 / searched&&!found=결과없음(사유 무구분) -->
          <div v-if="searched && found" class="cmpny-result-card">
            <div class="cmpny-result-row">
              <label>회사코드</label><span>{{ found.cmpnyCd }}</span>
            </div>
            <div class="cmpny-result-row">
              <label>회사명</label><span>{{ found.cmpnyNm }}</span>
            </div>
            <div class="cmpny-result-row">
              <label>사업자번호</label><span>{{ found.bsnsLcnNo }}</span>
            </div>
          </div>
          <p v-else-if="searched" class="cmpny-result-empty">
            조회 결과가 없습니다.
          </p>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              취소
            </button>
            <button
              class="btn btn-primary"
              :disabled="!found"
              @click="fnRequest"
            >
              연동 요청
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, watch, defineProps, defineEmits, getCurrentInstance, nextTick } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useUserStore } from "@/stores/userStore";

const props = defineProps({ onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();
const userStore = useUserStore();

// =========================== Ref ===========================
const tgtCmpnyCd = ref("");
const searched = ref(false);
const found = ref(null); // { cmpnyCd, cmpnyNm, bsnsLcnNo }
const cmpnyCdFcs = ref(null);

// 조회/요청 중복 클릭 방지 플래그.
const searching = ref(false);
const saving = ref(false);

// 입력 변경 시 이전 조회 결과 초기화(다른 코드로 요청 나가는 것 방지).
watch(tgtCmpnyCd, () => {
  searched.value = false;
  found.value = null;
});

// =========================== Methods ===========================
// 회사 정확일치 조회 — GET /webApi/subcon01/cmpny-exact-search?cmpnyCd=...
//   결과 유무만 표시. 미존재/비활성 사유는 서버가 구분하지 않는다(열거 방지 — 응답 그대로 표시).
//   자기 회사코드는 프론트 선안내(서버도 동일한 빈 결과를 반환하는 이중 가드).
const fnSearchCmpny = async () => {
  if (!tgtCmpnyCd.value) {
    await proxy.$alert("회사코드를 입력해주세요.");
    await nextTick();
    cmpnyCdFcs.value?.focus();
    return;
  }
  if (tgtCmpnyCd.value === userStore.gv_cmpnyCd) {
    await proxy.$alert("자기 회사에는 연동을 요청할 수 없습니다.");
    await nextTick();
    cmpnyCdFcs.value?.focus();
    return;
  }

  if (searching.value) return;
  searching.value = true;

  try {
    const response = await axios.get("/webApi/subcon01/cmpny-exact-search", {
      params: { cmpnyCd: tgtCmpnyCd.value },
    });

    if (response.status === 200) {
      found.value = response.data?.cmpny || null;
      searched.value = true;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    searching.value = false;
  }
};

// 연동 요청 — POST /webApi/subcon01/relation-request { tgtCmpnyCd }.
//   성공: 알림 → 부모 목록 재조회(onSaved) → 닫기. 실패: 서버 메시지 그대로(중복 요청 메시지 포함).
const fnRequest = async () => {
  if (!found.value) {
    await proxy.$alert("연동할 회사를 먼저 조회해주세요.");
    return;
  }

  const ok = await proxy.$confirm(`'${found.value.cmpnyNm}'에 연동을 요청하시겠습니까?`);
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon01/relation-request", {
      tgtCmpnyCd: found.value.cmpnyCd,
    });

    if (response.status === 200) {
      await proxy.$alert("연동을 요청했습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "연동 요청 중 오류가 발생했습니다.");
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
.cmpny-result-card {
  margin-top: var(--space-md, 0.75rem);
  padding: var(--space-md, 0.75rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-bg, #f9fafb);
}
.cmpny-result-row {
  display: flex;
  gap: var(--space-sm, 0.5rem);
  padding: 0.15rem 0;
}
.cmpny-result-row label {
  width: 90px;
  color: var(--color-text-muted, #6b7280);
  flex-shrink: 0;
}
.cmpny-result-empty {
  margin-top: var(--space-md, 0.75rem);
  color: var(--color-text-muted, #6b7280);
  text-align: center;
}
</style>
