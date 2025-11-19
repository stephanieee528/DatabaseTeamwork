<template>
  <div class="county-detail">
    <div class="county-header">
      <h2 class="county-title">📍 示例县 - 详细信息</h2>
      <button class="btn" style="width: auto;" @click="followCounty">+ 关注该县</button>
    </div>

    <div class="county-info">
      <div class="info-card" v-for="info in countyInfo" :key="info.title">
        <h3>{{ info.title }}</h3>
        <p v-for="detail in info.details" :key="detail.label">
          <strong>{{ detail.label }}：</strong>{{ detail.value }}
        </p>
      </div>
    </div>

    <div class="chart-container">
      <div class="chart-placeholder">
        📈 经济指标趋势图<br />
        <small>显示近5年GDP、收入等关键指标变化</small>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getCountyIndicators } from '@/api';

const countyInfo = ref([
  {
    title: '基本信息',
    details: [
      { label: '所属省份', value: '加载中...' },
      { label: '行政级别', value: '加载中...' },
      { label: '人口数量', value: '加载中...' },
      { label: '面积', value: '加载中...' },
    ],
  },
  {
    title: '贫困指标',
    details: [
      { label: '贫困发生率', value: '加载中...' },
      { label: '脱贫户数', value: '加载中...' },
      { label: '脱贫人口', value: '加载中...' },
      { label: '脱贫时间', value: '加载中...' },
    ],
  },
  {
    title: '经济指标',
    details: [
      { label: 'GDP总量', value: '加载中...' },
      { label: '人均可支配收入', value: '加载中...' },
      { label: '主导产业', value: '加载中...' },
      { label: '投资总额', value: '加载中...' },
    ],
  },
  {
    title: '政策支持',
    details: [
      { label: '扶贫项目', value: '加载中...' },
      { label: '资金投入', value: '加载中...' },
      { label: '产业项目', value: '加载中...' },
      { label: '教育支持', value: '加载中...' },
    ],
  },
]);

const followCounty = () => {
  alert('已添加到关注列表');
};

onMounted(async () => {
  try {
    const response = await getCountyIndicators(1); // 示例县 ID 为 1
    const data = response.data;

    countyInfo.value = [
      {
        title: '基本信息',
        details: [
          { label: '所属省份', value: data.province || 'N/A' },
          { label: '行政级别', value: data.level || 'N/A' },
          { label: '人口数量', value: `${data.population || 'N/A'}万人` },
          { label: '面积', value: `${data.area || 'N/A'}平方公里` },
        ],
      },
      {
        title: '贫困指标',
        details: [
          { label: '贫困发生率', value: `${data.povertyRate || 'N/A'}%` },
          { label: '脱贫户数', value: `${data.delistedHouseholds || 'N/A'}户` },
          { label: '脱贫人口', value: `${data.delistedPopulation || 'N/A'}人` },
          { label: '脱贫时间', value: data.delistingDate || 'N/A' },
        ],
      },
      {
        title: '经济指标',
        details: [
          { label: 'GDP总量', value: `${data.gdp || 'N/A'}亿元` },
          { label: '人均可支配收入', value: `${data.incomePerCapita || 'N/A'}元` },
          { label: '主导产业', value: data.mainIndustries || 'N/A' },
          { label: '投资总额', value: `${data.totalInvestment || 'N/A'}亿元` },
        ],
      },
      {
        title: '政策支持',
        details: [
          { label: '扶贫项目', value: `${data.supportProjects || 'N/A'}个` },
          { label: '资金投入', value: `${data.funding || 'N/A'}亿元` },
          { label: '产业项目', value: `${data.industrialProjects || 'N/A'}个` },
          { label: '教育支持', value: data.educationSupport || 'N/A' },
        ],
      },
    ];
  } catch (error) {
    console.error('Failed to load county data:', error);
  }
});
</script>

<style scoped>
/* 从 example.html 提取的样式 */
.county-detail {
  background: rgba(255, 255, 255, 0.7);
  padding: 35px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
}

.county-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid rgba(66, 153, 225, 0.2);
}

.county-title {
  font-size: 32px;
  color: #2c5282;
  font-weight: 700;
}

.county-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 25px;
  margin-bottom: 35px;
}

.info-card {
  background: rgba(255, 255, 255, 0.8);
  padding: 25px;
  border-radius: 12px;
  border-left: 5px solid #3182ce;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
}

.chart-container {
  background: rgba(255, 255, 255, 0.7);
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  margin-bottom: 25px;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>