<template>
  <div class="org-node-wrapper">
    <!-- 노드 카드: 아이콘 + 이름 + 직책, 계층 시각화 -->
    <div class="org-node-card" :style="{ '--node-color': branchColor }">
      <span v-if="node.isNew" class="org-node-badge-new">[등록전]</span>
      <div class="org-node-header">
        <span v-if="branchIndex >= 0" class="org-node-badge">
          {{ String(branchIndex + 1).padStart(2, "0") }}
        </span>
        <div class="org-node-icon" :style="{ background: branchColor }">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998-0A4.5 4.5 0 0 0 18 16.5h-1.5a4.5 4.5 0 0 0-4.5 4.5v.75Z"
            />
          </svg>
        </div>
      </div>
      <div class="org-node-body">
        <div class="org-node-name-wrap">
          <input
            :value="node.nodeNm"
            class="org-node-input"
            @input="$emit('update-node', node, 'nodeNm', $event.target.value)"
          />
        </div>
        <div class="org-node-count-row">
          관리자{{ managerCnt }}명 / 근로자{{ workerCnt }}명
        </div>
        <label v-if="!isRoot" class="org-node-check-row">
          <input
            v-model="selfAttdApprvYn"
            type="checkbox"
            class="org-node-checkbox"
          />
          <span class="org-node-check-label">자체근태승인여부</span>
        </label>
      </div>
      <div class="org-node-actions">
        <select
          :value="node.nodeType"
          class="org-node-type"
          :class="{ 'org-node-type--disabled': isRoot }"
          :disabled="isRoot"
          :title="isRoot ? '최상위 노드는 수정할 수 없습니다' : '구성요소 유형'"
          @change="onTypeChange($event)"
        >
          <option
            v-for="opt in typeOptionsResolved"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </option>
        </select>
        <button
          v-if="canAddChild"
          type="button"
          class="org-node-btn org-node-btn-add"
          title="하위 추가"
          @click="$emit('add-child', node)"
        >
          +
        </button>
        <button
          v-if="!isRoot"
          type="button"
          class="org-node-btn org-node-btn-del"
          title="삭제"
          @click="
            $emit('delete-node', {
              node,
              parentNode,
              parentChildren,
              indexInParent,
            })
          "
        >
          ×
        </button>
      </div>
    </div>

    <!-- 하위 노드 연결선 + 자식들 -->
    <div v-if="hasChildren" class="org-node-children">
      <div class="org-node-line-v"></div>
      <div class="org-node-line-h-wrap">
        <div class="org-node-line-h"></div>
      </div>
      <div class="org-node-children-list">
        <div
          v-for="(child, idx) in node.children"
          :key="child.nodeCd || child.nodeId || child.orgNodeId || child.id"
          class="org-node-child"
        >
          <OrgChartNode
            :node="child"
            :type-options="typeOptions"
            :level="level + 1"
            :branch-index="idx"
            :branch-color="childBranchColor(idx)"
            :parent-node="node"
            :parent-children="node.children"
            :index-in-parent="idx"
            @add-child="$emit('add-child', $event)"
            @delete-node="$emit('delete-node', $event)"
            @update-node="
              (n, field, val) => $emit('update-node', n, field, val)
            "
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable no-undef */
import { computed } from "vue";
import OrgChartNode from "./OrgChartNode.vue";

const props = defineProps({
  node: { type: Object, required: true },
  typeOptions: { type: Array, default: () => [] },
  level: { type: Number, default: 0 },
  branchIndex: { type: Number, default: 0 },
  branchColor: { type: String, default: "#16a34a" },
  parentNode: { type: Object, default: undefined },
  parentChildren: { type: Array, default: undefined },
  indexInParent: { type: Number, default: -1 },
});

const emit = defineEmits(["add-child", "delete-node", "update-node"]);

const typeOptionsResolved = computed(() =>
  props.typeOptions.map((opt) =>
    typeof opt === "string"
      ? { value: opt, label: opt }
      : {
          value: opt.baimValDCd ?? opt.value,
          label: opt.baimValDNm ?? opt.label ?? opt.baimValDCd,
        }
  )
);
const maxDepth = computed(() =>
  Math.max(0, typeOptionsResolved.value.length - 1)
);

const isRoot = computed(() => props.level === 0 && !props.parentNode);

const hasChildren = computed(
  () => Array.isArray(props.node.children) && props.node.children.length > 0
);

const canAddChild = computed(() => props.level < maxDepth.value);

const branchColors = ["#16a34a", "#6366f1", "#ea580c", "#059669", "#7c3aed"];
const childBranchColor = (idx) =>
  branchColors[(props.level + 1 + idx) % branchColors.length];

const managerCnt = computed(() => Number(props.node.managerCnt) || 0);

const workerCnt = computed(() => Number(props.node.workerCnt) || 0);

const selfAttdApprvYn = computed({
  get: () => Boolean(props.node.selfAttdApprvYn),
  set: (v) => emit("update-node", props.node, "selfAttdApprvYn", Boolean(v)),
});

const onTypeChange = (e) => {
  const value = e.target?.value;
  if (value != null) emit("update-node", props.node, "nodeType", value);
};
</script>

<style scoped>
.org-node-wrapper {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.org-node-card {
  --node-color: #16a34a;
  min-width: 160px;
  max-width: 200px;
  background: #ffffff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 12px;
  position: relative;
  border-top: 3px solid var(--node-color);
}

.org-node-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
}

.org-node-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--node-color);
  background: rgba(22, 163, 74, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.org-node-badge-new {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 10px;
  font-weight: 600;
  color: #b45309;
  background: #fef3c7;
  padding: 2px 6px;
  border-radius: 4px;
}

.org-node-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.org-node-icon svg {
  width: 20px;
  height: 20px;
}

.org-node-body {
  text-align: center;
  margin-bottom: 8px;
}

/* 노드명 영역 높이 고정 → 편집 시 카드 크기 변동 방지 */
.org-node-name-wrap {
  height: 32px;
  min-height: 32px;
  max-height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.org-node-label {
  /* font-weight: 700; */
  font-size: 14px;
  color: #111827;
  word-break: break-word;
  line-height: 1.4;
  max-height: 32px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.org-node-count-row {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
  text-align: center;
}

.org-node-check-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 8px;
  cursor: pointer;
  font-size: 10px;
  color: #374151;
}

.org-node-checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
}

.org-node-check-label {
  user-select: none;
}

.org-node-input {
  width: 100%;
  height: 30px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  padding: 0 6px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  text-align: center;
  box-sizing: border-box;
  flex-shrink: 0;
}

.org-node-input.org-node-position {
  font-size: 12px;
  margin-top: 4px;
}

.org-node-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: nowrap;
  min-width: 0;
}

.org-node-type {
  font-size: 11px;
  padding: 2px 6px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
  color: #374151;
  min-width: 0;
  flex-shrink: 1;
}

.org-node-type--disabled:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  background: #f3f4f6;
}

.org-node-btn {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.org-node-btn-add {
  background: var(--node-color);
  color: #fff;
}

.org-node-btn-add:hover {
  filter: brightness(1.1);
}

.org-node-btn-del {
  background: #fef2f2;
  color: #dc2626;
}

.org-node-btn-del:hover {
  background: #fee2e2;
}

/* 연결선: 세로 → 가로 분기 → 자식들 */
.org-node-children {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 8px;
}

.org-node-line-v {
  width: 2px;
  min-height: 20px;
  background: #d1d5db;
}

.org-node-line-h-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
}

.org-node-line-h {
  min-width: 2px;
  height: 2px;
  flex: 1;
  max-width: 100%;
  background: #d1d5db;
}

.org-node-children-list {
  display: flex;
  flex-direction: row;
  gap: 32px;
  justify-content: center;
  padding-top: 8px;
}

.org-node-child {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.org-node-child::before {
  content: "";
  width: 2px;
  height: 12px;
  background: #d1d5db;
  margin-bottom: 4px;
}
</style>
