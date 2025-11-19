<template>
  <div class="county-detail">
    <div class="county-header">
      <h2 class="county-title">📍 {{ countyData.name }} - 详细信息</h2>
      <button class="btn" @click="followCounty">+ 关注该县</button>
    </div>

    <div class="county-info">
      <div class="info-card" v-for="info in countyInfo" :key="info.title">
        <h3>{{ info.title }}</h3>
        <p v-for="(value, key) in info.details" :key="key">
          <strong>{{ key }}：</strong>{{ value }}
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

const countyData = ref({ name: '示例县' });
const countyInfo = ref([]);

const followCounty = () => {
  alert('已添加到关注列表');
};

onMounted(async () => {
  try {
    const response = await fetch('/api/county-detail');
    const data = await response.json();
    countyData.value = data;
    countyInfo.value = [
      {
        title: '基本信息',
        details: {
          所属省份: data.province,
          行政级别: data.level,
          人口数量: `${data.population}万人`,
          面积: `${data.area}平方公里`,
        },
      },
      {
        title: '贫困指标',
        details: {
          贫困发生率: `${data.povertyRate}%`,
          脱贫户数: `${data.delistedHouseholds}户`,
          脱贫人口: `${data.delistedPopulation}人`,
          脱贫时间: data.delistedDate,
        },
      },
      {
        title: '经济指标',
        details: {
          GDP总量: `${data.gdp}亿元`,
          人均可支配收入: `${data.income}元`,
          主导产业: data.industries.join('、'),
          投资总额: `${data.investment}亿元`,
        },
      },
      {
        title: '政策支持',
        details: {
          扶贫项目: `${data.projects}个`,
          资金投入: `${data.funding}亿元`,
          产业项目: `${data.industryProjects}个`,
          教育支持: data.educationSupport,
        },
      },
    ];
  } catch (error) {
    console.error('Failed to fetch county details:', error);
  }
});
</script>

<style scoped>
.county-detail {
  background: rgba(255, 255, 255, 0.7);
  padding: 35px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
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
  transition: all 0.3s ease;
}

.info-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 15px rgba(0, 0, 0, 0.05);
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

.chart-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(
    135deg,
    rgba(230, 247, 255, 0.6) 0%,
    rgba(240, 255, 244, 0.6) 100%
  );
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a5568;
  font-size: 20px;
  border: 2px dashed rgba(56, 161, 105, 0.3);
}
</style>