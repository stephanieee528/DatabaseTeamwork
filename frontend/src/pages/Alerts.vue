<template>
  <div class="alerts-page">
    <div class="header">
      <h2>⚠️ 警报管理</h2>
      <p>查看和管理所有警报规则和事件。</p>
    </div>

    <div class="alerts-section">
      <div class="alert-card" v-for="alert in alerts" :key="alert.id">
        <h3>{{ alert.title }}</h3>
        <p>{{ alert.description }}</p>
        <button class="btn" @click="resolveAlert(alert.id)">标记为已解决</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAlerts, resolveAlertById } from '@/api';

const alerts = ref([
  { id: 1, title: '加载中...', description: '加载中...' },
]);

const resolveAlert = async (id) => {
  try {
    await resolveAlertById(id);
    alerts.value = alerts.value.filter((alert) => alert.id !== id);
    alert('警报已标记为已解决');
  } catch (error) {
    console.error('Failed to resolve alert:', error);
  }
};

onMounted(async () => {
  try {
    const response = await getAlerts();
    alerts.value = response.data;
  } catch (error) {
    console.error('Failed to load alerts:', error);
  }
});
</script>

<style scoped>
.alerts-page {
  padding: 20px;
  background: #fefefe;
  border-radius: 8px;
}

.header {
  text-align: center;
  margin-bottom: 20px;
}

.header h2 {
  font-size: 28px;
  color: #e53e3e;
}

.alerts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.alert-card {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-left: 5px solid #e53e3e;
}

.alert-card h3 {
  font-size: 18px;
  color: #2d3748;
}

.alert-card p {
  font-size: 14px;
  color: #4a5568;
  margin: 10px 0;
}

.btn {
  background: #e53e3e;
  color: #fff;
  border: none;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.btn:hover {
  background: #c53030;
}
</style>