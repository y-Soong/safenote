<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content">
        <div class="modal-header">
          <span>데이터 공유 승인</span>
          <button class="icon-button" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <!-- 요청 요약 -->
          <ul class="approve-summary">
            <li><b>요청 회사</b> {{ info.reqCmpnyNm }}</li>
            <li><b>대상 사업장</b> {{ info.siteNm }}</li>
            <li><b>기간</b> {{ info.periodLabel }}</li>
            <li><b>마감 근태만</b> {{ info.closedOnlyYn === "Y" ? "예" : "아니오" }}</li>
            <li><b>제공 목적</b> {{ info.purpose }}</li>
          </ul>

          <!-- 마감 상태 -->
          <div v-if="info.closedOnlyYn === 'Y' && !info.closedAll" class="gate-block">
            <p class="gate-title">근태 마감이 완료되지 않았습니다.</p>
            <p class="gate-body">미마감 월: {{ (info.unclosedYms || []).join(", ") }}</p>
            <p class="gate-body">해당 월을 마감한 뒤 승인할 수 있습니다.</p>
          </div>
          <div v-else-if="info.closedOnlyYn === 'N' && !info.closedAll" class="gate-warn">
            <p class="gate-body">미마감 근태가 포함됩니다. 스냅샷에 <b>미마감 포함</b> 표식이 영구 기록됩니다.</p>
          </div>

          <!-- 릴레이 후보(연동사로부터 수신 보유 중인 자료) -->
          <div v-if="(info.relayCandidates || []).length" class="relay-box">
            <p class="relay-title">함께 제공할 연동사 수신자료 (선택)</p>
            <p class="relay-note">
              선택한 자료는 우리 회사 소속으로 표시되어 함께 전달됩니다(하위 회사 정보는 포함되지 않습니다).
            </p>
            <label v-for="c in info.relayCandidates" :key="c.snapshotId" class="relay-item">
              <input v-model="bundleIds" type="checkbox" :value="c.snapshotId" />
              <span>
                {{ c.periodLabel }} · v{{ c.version }} · {{ c.rowCnt }}건
                <span v-if="c.unclosedIncludedYn === 'Y'" class="mini-badge">미마감 포함</span>
              </span>
            </label>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">닫기</button>
            <button class="btn btn-primary" :disabled="!canApprove" @click="fnApprove">승인</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, onMounted, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ shareReqId: [Number, String], onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const info = ref({}); // GET share-req-approve-info 응답(요청 요약 + closedAll/unclosedYms + relayCandidates)
const bundleIds = ref([]); // 묶을 수신 스냅샷 ID (서버가 4조건 재검증 — §5-7)

// 승인 중복 클릭 방지 플래그.
const saving = ref(false);

// 마감만 요청인데 미마감이면 승인 불가(서버도 차단). 조회 전(빈 객체)에도 버튼이 눌리지 않도록 로드 여부를 함께 본다.
const canApprove = computed(
  () => !!info.value.shareReqId && !(info.value.closedOnlyYn === "Y" && !info.value.closedAll)
);

// =========================== Life Cycle ===========================
// 승인 사전정보 조회 — GET /webApi/subcon03/share-req-approve-info?shareReqId=...
//   마감 상태(closedAll/unclosedYms)와 릴레이 후보를 서버가 판정해 내려준다.
onMounted(async () => {
  try {
    const response = await axios.get("/webApi/subcon03/share-req-approve-info", {
      params: { shareReqId: props.shareReqId },
    });

    if (response.status === 200) {
      info.value = response.data || {};
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
    emit("close");
  }
});

// =========================== Methods ===========================
// 승인 — POST /webApi/subcon03/share-req-approve { shareReqId, bundleSnapshotIds }.
//   승인 시점에 서버가 마감/관계/릴레이 후보를 재검사한 뒤 스냅샷을 생성한다(단일 트랜잭션).
const fnApprove = async () => {
  const ok = await proxy.$confirm("승인 시 해당 기간 근태가 요청 회사로 복제됩니다. 진행할까요?");
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon03/share-req-approve", {
      shareReqId: props.shareReqId,
      bundleSnapshotIds: bundleIds.value,
    });

    if (response.status === 200) {
      const rowCnt = response.data?.rowCnt ?? 0;
      await proxy.$alert(`승인되었습니다. (제공 ${rowCnt}건)`);
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "승인 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.approve-summary {
  margin: 0 0 var(--space-md, 0.75rem);
  padding: 0;
  list-style: none;
  line-height: 1.8;
}
.approve-summary b {
  display: inline-block;
  width: 90px;
  color: var(--color-text-muted, #6b7280);
  font-weight: 500;
}
.gate-block,
.gate-warn,
.relay-box {
  margin-top: var(--space-md, 0.75rem);
  padding: var(--space-sm, 0.5rem) var(--space-md, 0.75rem);
  border-radius: var(--btn-radius, 8px);
}
.gate-block {
  background: var(--color-danger-bg, #fee2e2);
}
.gate-warn {
  background: var(--color-warning-bg, #fef3c7);
}
.relay-box {
  border: 1px solid var(--color-border, #e5e7eb);
}
.gate-title {
  margin: 0 0 0.25rem;
  font-weight: 600;
  color: var(--color-danger, #dc2626);
}
.gate-body {
  margin: 0;
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}
.relay-title {
  margin: 0 0 0.25rem;
  font-weight: 600;
}
.relay-note {
  margin: 0 0 var(--space-sm, 0.5rem);
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}
.relay-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
  padding: 0.15rem 0;
}
.mini-badge {
  margin-left: 0.35rem;
  padding: 0.05rem 0.4rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  font-size: var(--btn-font-sm, 11px);
}
</style>
