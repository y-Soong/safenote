<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-normal">
        <div class="modal-header">
          <span>스냅샷 버전 선택</span>
          <button class="icon-button" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <!-- 안내 — 팝업은 표시 전용(자체 API 호출 없음). 데이터는 부모 versionList prop 그대로. -->
          <p class="ver-note">
            같은 요청 조건(출처 회사 · 사업장 · 기간 · 유형)의 버전 목록입니다. 행을 선택하면 해당 버전으로 상세가 전환됩니다.
          </p>

          <table class="ver-grid">
            <thead>
              <tr>
                <th style="width: 11%">버전</th>
                <th style="width: 21%">대상 기간</th>
                <th style="width: 12%">요청자</th>
                <th style="width: 17%">요청일시</th>
                <th style="width: 17%">승인일시</th>
                <th style="width: 8%">행수</th>
                <th>표식</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!props.versionList.length">
                <tr>
                  <td colspan="7" class="ver-empty">표시할 버전이 없습니다.</td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="row in props.versionList"
                  :key="row.snapshotId"
                  :class="{ 'is-current': row.snapshotId === props.currentSnapshotId }"
                  @click="fnSelectRow(row)"
                >
                  <td class="ta-c">
                    v{{ row.version }}
                    <span v-if="row.snapshotId === props.currentSnapshotId" class="cur-badge">현재</span>
                  </td>
                  <td>{{ periodLabel(row) }}</td>
                  <td>{{ row.reqUserNm || "-" }}</td>
                  <td>{{ row.reqDtime || "-" }}</td>
                  <td>{{ row.processDtime || "-" }}</td>
                  <td class="ta-c">{{ row.rowCnt }}</td>
                  <td>
                    <span v-if="row.unclosedIncludedYn === 'Y'" class="status-badge is-warn">미마감 포함</span>
                    <span v-if="row.consentExcludedCnt > 0" class="status-badge is-muted">
                      동의 제외 {{ row.consentExcludedCnt }}명
                    </span>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>

          <!-- 승인 주체는 상대사 행위자 마스킹 정책(T1)에 따라 표시하지 않는다 — 일시만 제공. -->
          <p class="ver-foot-note">승인 주체는 상대사 정보 보호 정책에 따라 표시하지 않습니다.</p>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { defineProps, defineEmits, defineOptions } from "vue";

// =========================== Define ===========================
defineOptions({ name: "SnapshotVersionSelectPop" });

// versionList: 부모(Subcon_04)의 동일 조건 그룹 computed 를 그대로 받는다 — 팝업 자체 API 호출 금지.
// currentSnapshotId: 현재 상세에 표시 중인 스냅샷 ID(현재 배지/하이라이트용).
const props = defineProps({
  versionList: { type: Array, default: () => [] },
  currentSnapshotId: [Number, String],
});
const emit = defineEmits(["close", "select"]);

// =========================== Methods ===========================
// 행 선택 — 현재 버전이면 재조회 없이 닫기만, 다른 버전이면 select emit 후 닫기.
//   상세 재조회(fnChangeVersion 흐름)는 부모 책임 — 팝업은 snapshotId 전달까지만.
const fnSelectRow = (row) => {
  if (row.snapshotId === props.currentSnapshotId) {
    emit("close");
    return;
  }
  emit("select", row.snapshotId);
  emit("close");
};

// 표시 포맷터(순수 표시용 — 비즈니스 로직 아님).
const fmtYmd = (v) => (v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : v || "");
const periodLabel = (row) => `${fmtYmd(row.periodStr)} ~ ${fmtYmd(row.periodEnd)}`;
</script>

<style scoped>
.ver-note {
  margin: 0 0 var(--space-sm, 0.5rem);
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}
.ver-foot-note {
  margin: var(--space-sm, 0.5rem) 0 0;
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}

.ver-grid {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  font-size: var(--btn-font-sm, 13px);
}
.ver-grid th,
.ver-grid td {
  padding: var(--space-sm, 0.5rem);
  border: 1px solid var(--color-border, #e5e7eb);
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ver-grid thead th {
  background: var(--color-bg-soft, #f9fafb);
  font-weight: 600;
}
.ver-grid tbody tr {
  cursor: pointer;
}
.ver-grid tbody tr:hover {
  background: var(--color-bg-soft, #f9fafb);
}
.ver-grid tbody tr.is-current {
  background: var(--color-primary-bg, #dcfce7);
}
.ver-empty {
  text-align: center;
  color: var(--color-text-muted, #6b7280);
}
.ta-c {
  text-align: center;
}

.cur-badge {
  display: inline-block;
  margin-left: 0.25rem;
  padding: 0.05rem 0.4rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}

.status-badge {
  display: inline-block;
  margin-right: 0.25rem;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-warn {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-muted {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}
</style>
