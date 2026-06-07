<template>
  <div class="org-node-wrapper">
    <!-- 노드 카드: 아이콘 + 이름 + 직책, 계층 시각화 -->
    <div class="org-node-card" :style="{ '--node-color': branchColor }">
      <span v-if="node.isNew" class="org-node-badge-new">[등록전]</span>
      <!-- 저장된 노드: 좌측 번호 배지와 동일 양식으로 우측 상단에 노드 코드 노출 -->
      <span
        v-else
        class="org-node-badge-code"
        :title="`노드 코드: ${node.nodeCd || '-'}`"
        >{{ node.nodeCd || "-" }}</span
      >
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
        <!-- 담당 관리자 정/부 -->
        <div class="org-node-managers">
          <div class="org-node-manager-row">
            <span class="org-node-manager-label">담당 정</span>
            <button
              type="button"
              class="org-node-manager-chip"
              :class="{ empty: !node.managerPrimaryUserNm }"
              @click="
                $emit('open-user-search', { node, field: 'managerPrimary' })
              "
            >
              <span class="org-node-manager-name">{{
                node.managerPrimaryUserNm || "미지정"
              }}</span>
              <svg
                class="org-node-manager-icon"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fill-rule="evenodd"
                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <button
              v-if="node.managerPrimaryUserNm"
              type="button"
              class="org-node-manager-clear"
              title="선택 해제"
              @click.stop="
                $emit('delete-manager', { node, field: 'managerPrimary' })
              "
            >
              ×
            </button>
          </div>
          <div class="org-node-manager-row">
            <span class="org-node-manager-label">담당 부</span>
            <button
              type="button"
              class="org-node-manager-chip"
              :class="{ empty: !node.managerDeputyUserNm }"
              @click="
                $emit('open-user-search', { node, field: 'managerDeputy' })
              "
            >
              <span class="org-node-manager-name">{{
                node.managerDeputyUserNm || "미지정"
              }}</span>
              <svg
                class="org-node-manager-icon"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fill-rule="evenodd"
                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <button
              v-if="node.managerDeputyUserNm"
              type="button"
              class="org-node-manager-clear"
              title="선택 해제"
              @click.stop="
                $emit('delete-manager', { node, field: 'managerDeputy' })
              "
            >
              ×
            </button>
          </div>
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
            @delete-manager="$emit('delete-manager', $event)"
            @open-user-search="$emit('open-user-search', $event)"
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

const emit = defineEmits([
  "add-child",
  "delete-node",
  "update-node",
  "delete-manager",
  "open-user-search",
]);

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

/* 노드 코드 배지: 좌측 번호 배지(.org-node-badge)와 동일 양식, 우측 상단 배치 */
.org-node-badge-code {
  position: absolute;
  top: 8px;
  right: 8px;
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 700;
  color: var(--node-color);
  background: rgba(22, 163, 74, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.org-node-managers {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.org-node-manager-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.org-node-manager-row .org-node-manager-chip {
  flex: 1;
}

.org-node-manager-label {
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  flex-shrink: 0;
  width: 44px;
}

.org-node-manager-chip {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  font-size: 11px;
  color: #374151;
  transition: all 0.2s;
}

.org-node-manager-chip:hover {
  background: #f1f5f9;
  border-color: var(--node-color);
  color: var(--node-color);
}

.org-node-manager-chip.empty {
  color: #9ca3af;
  font-style: italic;
}

.org-node-manager-chip.empty:hover {
  color: var(--node-color);
}

.org-node-manager-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-node-manager-icon {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  opacity: 0.6;
}

.org-node-manager-chip:hover .org-node-manager-icon {
  opacity: 1;
}

.org-node-manager-clear {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: #fee2e2;
  color: #dc2626;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.org-node-manager-clear:hover {
  background: #fecaca;
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
