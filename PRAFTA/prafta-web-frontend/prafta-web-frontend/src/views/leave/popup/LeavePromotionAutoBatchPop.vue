<!--
  LeavePromotionAutoBatchPop.vue — 2차 촉진 자동배치 프리뷰/커밋 팝업 (관리자 웹, 신규)
  - 작업 ID: prafta-com-008-A-8 (UI 명세: UI-web-008-A-1)
  - 정책 출처: 작업지시서 §3-5(자동배치 2전략), autobatch 설계(프리뷰→확인→커밋 분리, 결정성, 미달 표시)
  - 참조 패턴: views/attd/popup/LeaveApplyPop.vue (modal 구조), prafta-052 실패행(미달) 표시
  - planner 라운드: template + scoped style 완성, script 는 props/emits/ref 선언 + TODO.
  - developer 라운드:
      · POST /webApi/leavepromo01/autobatch/preview { strategy, windowFrom, windowTo, 조회조건 } → proposal.
      · dailyLoad bar·peakLoad·shortages 표시 후 [커밋] → POST .../autobatch/commit { proposal } (DIRECT_USE_KEY 재검증).
-->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content lpb-modal">
        <div class="modal-header">
          <span>연차 자동배치 (2차 촉진)</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="modal-body lpb-body">
          <!-- 전략 / 기간 -->
          <div class="lpb-controls">
            <div class="lpb-field">
              <label>배치 전략</label>
              <select v-model="strategy" class="lpb-select">
                <option value="MIN_OVERLAP">하루 동시휴가 인원 최소화</option>
                <option value="YEAR_END">만료일부터 역방향 채우기</option>
              </select>
            </div>
            <div class="lpb-field">
              <label>기간 시작</label>
              <input v-model="windowFrom" type="date" class="lpb-date" />
            </div>
            <div class="lpb-field">
              <label>기간 종료</label>
              <input v-model="windowTo" type="date" class="lpb-date" />
            </div>
            <button class="lpb-preview-btn" :disabled="isPreviewing" @click="onPreview">
              {{ isPreviewing ? '계산 중...' : '미리보기' }}
            </button>
          </div>

          <!-- 프리뷰 결과 -->
          <template v-if="proposal">
            <!-- 검수 지표 -->
            <div class="lpb-metrics">
              <span>대상자 <strong>{{ proposal.assignments?.length || 0 }}명</strong></span>
              <span v-if="strategy === 'MIN_OVERLAP'">
                피크(동시휴가 최대) <strong>{{ proposal.peakLoad ?? '-' }}명</strong>
              </span>
              <span class="lpb-shortage" v-if="(proposal.shortages?.length || 0) > 0">
                미달 <strong>{{ proposal.shortages.length }}건</strong>
              </span>
            </div>

            <!-- 일자별 부하 (간이 막대) -->
            <div class="lpb-load">
              <p class="lpb-sub">일자별 휴가 인원</p>
              <div class="lpb-bars">
                <div
                  v-for="d in proposal.dailyLoad || []"
                  :key="d.ymd"
                  class="lpb-bar"
                  :title="`${d.ymd} : ${d.count}명`"
                >
                  <span class="lpb-bar__fill" :style="barStyle(d.count)" />
                </div>
              </div>
            </div>

            <!-- 미달 목록 (prafta-052 패턴: 사유 표시 + 다운로드 위임) -->
            <div v-if="(proposal.shortages?.length || 0) > 0" class="lpb-shortages">
              <p class="lpb-sub">미달 사용자 (가용일 부족)</p>
              <ul class="lpb-short-list">
                <li v-for="s in proposal.shortages" :key="s.userCd">
                  {{ s.userCd }} — 필요 {{ s.requiredDays }}일 / 배정 {{ s.assignedDays }}일 /
                  부족 {{ s.shortageDays }}일
                </li>
              </ul>
            </div>
          </template>

          <p v-else class="lpb-empty">전략·기간을 정하고 미리보기를 실행하세요.</p>
        </div>

        <div class="modal-footer lpb-footer">
          <button class="btn-ghost" @click="$emit('close')">닫기</button>
          <button
            class="btn-primary"
            :disabled="!proposal || isCommitting"
            @click="onCommit"
          >
            {{ isCommitting ? '커밋 중...' : '이 배치로 직권 지정' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, getCurrentInstance, defineProps, defineEmits } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const { proxy } = getCurrentInstance();

const props = defineProps({
  // 부모 화면의 조회조건 스냅샷 (대상자 산출용)
  //   { siteCd, nodeCd, incSubNodeYn, userNm, tenureType }
  filter: { type: Object, default: () => ({}) },
});

const emit = defineEmits(["close", "done"]);

// 전략 / 기간
const strategy = ref("MIN_OVERLAP");
const windowFrom = ref("");
const windowTo = ref("");

const isPreviewing = ref(false);
const isCommitting = ref(false);

// autobatch 프리뷰 응답
//   { strategy, assignments:[{userCd, ymds}], shortages:[{userCd, requiredDays, assignedDays, shortageDays}],
//     dailyLoad:[{ymd, count}], peakLoad }
const proposal = ref(null);

// 막대 높이(간이) — 최대 부하 기준 비율.
const barStyle = (count) => {
  const max = proposal.value?.peakLoad || 1;
  const pct = Math.max(4, Math.round((count / max) * 100));
  return { height: `${pct}%` };
};

// 미리보기 — 즉시 등록 금지(제안만).
const onPreview = async () => {
  if (isPreviewing.value) return;
  isPreviewing.value = true;
  try {
    // AutoBatchPreviewRequest: { strategy, windowFrom, windowTo, ...조회조건 }. siteCd 세션 IDOR 는 서버 강제.
    const { data } = await axios.post("/webApi/leavepromo01/autobatch/preview", {
      strategy: strategy.value,
      windowFrom: (windowFrom.value || "").replace(/-/g, ""),
      windowTo: (windowTo.value || "").replace(/-/g, ""),
      ...props.filter,
    });
    proposal.value = data || null;
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, "자동배치 미리보기 중 오류가 발생했습니다."));
  } finally {
    isPreviewing.value = false;
  }
};

// 커밋 — 관리자 확인 후 등록(2차/회사직권) + PUSH. 서버가 DIRECT_USE_KEY 재검증.
const onCommit = async () => {
  if (!proposal.value || isCommitting.value) return;
  isCommitting.value = true;
  try {
    // AutoBatchCommitRequest 계약: { proposal: { assignments:[{userCd, ymds}] } }.
    //   서버는 assignments(userCd+ymds)만 신뢰 입력으로 쓰고 스코프/권한·가드를 재검증한다(IDOR/TOCTOU).
    //   dailyLoad/peakLoad/shortages 는 표시용이라 전송해도 무시되나, assignments 만 추려 페이로드를 최소화한다.
    const assignments = (proposal.value?.assignments || []).map((a) => ({
      userCd: a.userCd,
      ymds: a.ymds || [],
    }));
    const { data } = await axios.post("/webApi/leavepromo01/autobatch/commit", {
      proposal: { assignments },
    });
    // AutoBatchCommitResponse: { totalDesignated, totalSkipped, totalFailed, userResults }
    const designated = data?.totalDesignated ?? 0;
    const skipped = data?.totalSkipped ?? 0;
    const failed = data?.totalFailed ?? 0;
    let msg = `자동배치 결과로 직권 지정이 완료되었습니다. (지정 ${designated}건`;
    if (skipped > 0) msg += `, 중복 ${skipped}건`;
    if (failed > 0) msg += `, 실패 ${failed}건`;
    msg += ")";
    await proxy.$alert(msg);
    emit("done");
    emit("close");
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, "자동배치 커밋 중 오류가 발생했습니다."));
  } finally {
    isCommitting.value = false;
  }
};
</script>

<style scoped>
.lpb-modal {
  width: 640px;
  max-width: 94vw;
}
.lpb-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.lpb-controls {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
.lpb-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.lpb-field label {
  font-size: 11px;
  color: var(--color-text-muted);
}
.lpb-select,
.lpb-date {
  height: var(--btn-height-lg, 32px);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius, 10px);
  padding: 0 10px;
  font-size: 12px;
}
.lpb-preview-btn {
  height: var(--btn-height-lg, 32px);
  padding: 0 14px;
  border: 1px solid var(--color-primary);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 12px;
  cursor: pointer;
}
.lpb-preview-btn:disabled {
  border-color: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}

.lpb-metrics {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-text);
}
.lpb-metrics strong {
  color: var(--color-text-strong);
}
.lpb-shortage strong {
  color: var(--color-danger);
}

.lpb-sub {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.lpb-load {
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
  padding: 10px;
}
.lpb-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 80px;
}
.lpb-bar {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: flex-end;
}
.lpb-bar__fill {
  width: 100%;
  background: var(--color-primary);
  border-radius: 2px 2px 0 0;
}

.lpb-shortages {
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
  padding: 10px;
  max-height: 160px;
  overflow-y: auto;
}
.lpb-short-list {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 12px;
  color: var(--color-text);
}
.lpb-short-list li {
  padding: 4px 0;
  border-bottom: 1px solid var(--color-border);
}

.lpb-empty {
  margin: 0;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

.lpb-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.btn-ghost {
  height: var(--btn-height-lg, 32px);
  padding: 0 var(--btn-padding-lg, 13px);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}
.btn-primary {
  height: var(--btn-height-lg, 32px);
  padding: 0 var(--btn-padding-lg, 13px);
  border: 0;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}
.btn-primary:disabled {
  background: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}
</style>
