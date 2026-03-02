<template>
  <div class="dashboard-view">
    <h2 class="title">Dashboard</h2>

    <div class="dashboard-grid">
      <section class="card">
        <h3>Stability</h3>
        <div class="chart-placeholder">[Graph Placeholder]</div>
      </section>

      <section class="card">
        <h3>PID Tuning</h3>
        <div class="chart-placeholder">[Graph Placeholder]</div>
      </section>

      <section class="card">
        <h3>P</h3>
        <ul class="pid-values">
          <li>1.20</li>
          <li>1.80</li>
          <li>55</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, defineProps } from "vue";

defineProps({ buttons: Object });

onMounted(() => {
  console.log("DashboardView mounted");
});
</script>

<style scoped>
.dashboard-view {
  padding: 1.5rem;
}

.title {
  font-size: 1.8rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.card {
  background: white;
  padding: 1rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.chart-placeholder {
  height: 100px;
  background: #f0f3f9;
  border-radius: 8px;
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 0.9rem;
}

.pid-values {
  margin-top: 0.5rem;
  font-size: 1.1rem;
  font-weight: 500;
  padding-left: 1rem;
}
</style>
