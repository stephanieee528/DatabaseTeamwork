<template>
  <div class="analysis-page">
    <div class="header">
      <h2>📊 数据分析</h2>
      <p>深入了解经济和贫困指标的趋势。</p>
    </div>

    <div class="chart-section">
      <div class="chart" v-for="chart in charts" :key="chart.id">
        <h3>{{ chart.title }}</h3>
        <div class="chart-placeholder">📈 {{ chart.description }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAnalysisData } from '@/api';

const charts = ref([
  { id: 1, title: '经济增长趋势', description: '加载中...' },
  { id: 2, title: '贫困率变化', description: '加载中...' },
  { id: 3, title: '收入分布', description: '加载中...' },
]);

onMounted(async () => {
  try {
    const response = await getAnalysisData();
    const data = response.data;

    charts.value = data.map((chart, index) => ({
      id: index + 1,
      title: chart.title,
      description: chart.description,
    }));
  } catch (error) {
    console.error('Failed to load analysis data:', error);
  }
});
</script>

<style scoped>
.analysis-page {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.header {
  text-align: center;
  margin-bottom: 20px;
}

.header h2 {
  font-size: 28px;
  color: #2c5282;
}

.chart-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.chart {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.chart-placeholder {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #718096;
  font-size: 14px;
  background: #edf2f7;
  border-radius: 4px;
}
</style>