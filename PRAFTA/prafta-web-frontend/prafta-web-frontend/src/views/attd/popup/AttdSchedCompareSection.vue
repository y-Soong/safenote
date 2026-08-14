<template>
  <div class="ra-schcmp">
    <div class="ra-schcmp__title">스케줄 변경 내용</div>

    <div class="ra-schcmp__grid">
      <!-- 현재 스케줄 -->
      <section class="ra-schcmp__card ra-schcmp__card--cur">
        <header class="ra-schcmp__head">
          <span class="ra-schcmp__head-label">현재</span>
          <span v-if="props.row?.curSchCd" class="ra-schcmp__code">
            {{ props.row.curSchNo || props.row.curSchCd }}
          </span>
        </header>

        <p v-if="!props.row?.curSchCd" class="ra-schcmp__empty">
          현재 스케줄 없음 (신규 배정)
        </p>
        <dl v-else class="ra-schcmp__dl">
          <dt>1차 근무</dt>
          <dd>
            {{ formatHm(props.row.curFstStrTime) }} ~
            {{ formatHm(props.row.curFstEndTime) }}
          </dd>
          <template v-if="props.row.curSecStrTime">
            <dt>2차 근무</dt>
            <dd>
              {{ formatHm(props.row.curSecStrTime) }} ~
              {{ formatHm(props.row.curSecEndTime) }}
            </dd>
          </template>
          <template v-if="props.row.curPreFixedOtStrTime">
            <dt>고정연장(전)</dt>
            <dd>
              {{ formatHm(props.row.curPreFixedOtStrTime) }} ~
              {{ formatHm(props.row.curPreFixedOtEndTime) }}
            </dd>
          </template>
          <template v-if="props.row.curFixedOtStrTime">
            <dt>고정연장(후)</dt>
            <dd>
              {{ formatHm(props.row.curFixedOtStrTime) }} ~
              {{ formatHm(props.row.curFixedOtEndTime) }}
            </dd>
          </template>
        </dl>
      </section>

      <div class="ra-schcmp__arrow" aria-hidden="true">→</div>

      <!-- 요청 스케줄 -->
      <section class="ra-schcmp__card ra-schcmp__card--req">
        <header class="ra-schcmp__head">
          <span class="ra-schcmp__head-label">요청</span>
          <span v-if="props.row?.reqSchCd" class="ra-schcmp__code">
            {{ props.row.reqSchNo || props.row.reqSchCd }}
          </span>
        </header>

        <p v-if="!props.row?.reqSchCd" class="ra-schcmp__warn">
          요청 스케줄 정보를 찾을 수 없습니다.
        </p>
        <dl v-else class="ra-schcmp__dl">
          <dt>1차 근무</dt>
          <dd>
            {{ formatHm(props.row.reqFstStrTime) }} ~
            {{ formatHm(props.row.reqFstEndTime) }}
          </dd>
          <template v-if="props.row.reqSecStrTime">
            <dt>2차 근무</dt>
            <dd>
              {{ formatHm(props.row.reqSecStrTime) }} ~
              {{ formatHm(props.row.reqSecEndTime) }}
            </dd>
          </template>
          <template v-if="props.row.reqPreFixedOtStrTime">
            <dt>고정연장(전)</dt>
            <dd>
              {{ formatHm(props.row.reqPreFixedOtStrTime) }} ~
              {{ formatHm(props.row.reqPreFixedOtEndTime) }}
            </dd>
          </template>
          <template v-if="props.row.reqFixedOtStrTime">
            <dt>고정연장(후)</dt>
            <dd>
              {{ formatHm(props.row.reqFixedOtStrTime) }} ~
              {{ formatHm(props.row.reqFixedOtEndTime) }}
            </dd>
          </template>
        </dl>
      </section>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import { defineProps } from "vue";
import { formatHm } from "@/utils/dateFormat";

defineOptions({ name: "AttdSchedCompareSection" });

// 순수 프레젠테이션 컴포넌트 — API/store/router 호출 없음.
// row = /webApi/reqinbox/pending?reqTypeGroup=schedule 응답의 pendingList 1건(PendingSchedReqResult).
const props = defineProps({
  row: { type: Object, required: true },
});
</script>

<style scoped>
/* 부모(Attd_10) scoped 스타일은 자식 내부 엘리먼트에 적용되지 않으므로 제목 스타일을 자체 정의한다. */
.ra-schcmp {
  margin-bottom: 0.9rem;
}
.ra-schcmp__title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin-bottom: 0.4rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid var(--color-border);
}
.ra-schcmp__grid {
  display: flex;
  align-items: stretch;
  gap: 0.5rem;
}
.ra-schcmp__card {
  flex: 1 1 0;
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: 0.5rem;
  padding: 0.5rem 0.6rem;
  background: var(--color-surface);
}
.ra-schcmp__card--req {
  border-color: var(--color-primary);
}
.ra-schcmp__head {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.35rem;
}
.ra-schcmp__head-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
}
.ra-schcmp__card--req .ra-schcmp__head-label {
  color: var(--color-primary);
}
.ra-schcmp__code {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border-strong);
  border-radius: 0.3rem;
  padding: 0.02rem 0.28rem;
}
.ra-schcmp__dl {
  display: grid;
  grid-template-columns: 4.6rem 1fr;
  gap: 0.22rem 0.4rem;
  margin: 0;
  font-size: 0.82rem;
}
.ra-schcmp__dl dt {
  color: var(--color-text-muted);
}
.ra-schcmp__dl dd {
  margin: 0;
  color: var(--color-text);
}
.ra-schcmp__arrow {
  align-self: center;
  flex: 0 0 auto;
  font-size: 0.95rem;
  color: var(--color-text-muted);
}
.ra-schcmp__empty {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.ra-schcmp__warn {
  margin: 0;
  font-size: 0.8rem;
  border-radius: 0.3rem;
  padding: 0.25rem 0.35rem;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
</style>
