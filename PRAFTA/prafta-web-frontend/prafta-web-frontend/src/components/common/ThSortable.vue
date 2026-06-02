<template>
  <th
    class="th-sortable"
    :style="width ? { width: width + 'px', minWidth: width + 'px' } : {}"
    @click.stop="emit('sort', colKey)"
  >
    <span class="th-label">{{ label }}</span>
    <span class="th-sort-icon">
      <template v-if="sortKey === colKey && sortOrder === 'asc'">↑</template>
      <template v-else-if="sortKey === colKey && sortOrder === 'desc'"
        >↓</template
      >
      <template v-else><span class="th-sort-idle">⇅</span></template>
    </span>
    <span
      class="th-resize-handle"
      @mousedown.stop="startResize"
      @click.stop
    ></span>
  </th>
</template>

<script setup>
const props = defineProps({
  label: { type: String, required: true },
  colKey: { type: String, required: true },
  sortKey: { type: String, default: null },
  sortOrder: { type: String, default: null },
  width: { type: Number, default: null },
});

const emit = defineEmits(["sort", "update:width"]);

function startResize(event) {
  event.preventDefault();
  const startX = event.clientX;
  const startWidth =
    props.width ?? event.currentTarget.closest("th").offsetWidth;

  const onMouseMove = (e) => {
    const newWidth = Math.max(40, startWidth + (e.clientX - startX));
    emit("update:width", { key: props.colKey, width: newWidth });
  };

  const onMouseUp = () => {
    document.removeEventListener("mousemove", onMouseMove);
    document.removeEventListener("mouseup", onMouseUp);
  };

  document.addEventListener("mousemove", onMouseMove);
  document.addEventListener("mouseup", onMouseUp);
}
</script>

<style scoped>
.th-sortable {
  position: relative;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}

.th-label {
  margin-right: 2px;
}

.th-sort-icon {
  font-size: 11px;
}

.th-sort-idle {
  opacity: 0.3;
}

.th-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  width: 4px;
  height: 100%;
  cursor: col-resize;
  background: transparent;
}

.th-resize-handle:hover {
  background: rgba(0, 0, 0, 0.15);
}
</style>
