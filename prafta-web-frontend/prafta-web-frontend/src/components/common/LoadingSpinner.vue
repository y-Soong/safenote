<!-- src/components/common/LoadingSpinner.vue -->
<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-30"
  >
    <div class="loader"></div>
  </div>
</template>

<style scoped>
.loader {
  border: 6px solid #f3f3f3;
  border-top: 6px solid #3498db;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
