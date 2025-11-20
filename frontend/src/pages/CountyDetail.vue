<template>
  <div class="county-detail-page">
    <div class="county-detail">
      <div class="county-header">
        <h2 class="county-title">📍 {{ countyData.name }} - 详细信息</h2>
        <button class="btn" style="width: auto;" @click="followCounty">+ 关注该县</button>
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

      <div class="additional-charts">
        <div class="chart-container" style="height: 300px;">
          <div class="chart-placeholder">
            📊 人口结构分析<br />
            <small>年龄分布、就业情况等</small>
          </div>
        </div>
        <div class="chart-container" style="height: 300px;">
          <div class="chart-placeholder">
            🏗️ 基础设施状况<br />
            <small>交通、教育、医疗设施</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const countyData = ref({ 
  name: '示例县',
  province: '示例省',
  level: '县级',
  population: '45.6',
  area: '2,845',
  povertyRate: '3.2',
  delistedHouseholds: '12,456',
  delistedPopulation: '48,234',
  delistedDate: '2020年12月',
  gdp: '128.5',
  income: '18,245',
  industries: ['农业', '旅游业', '制造业'],
  investment: '15.8',
  projects: '156',
  funding: '8.9',
  industryProjects: '45',
  educationSupport: '全覆盖'
});

const countyInfo = ref([]);

const followCounty = () => {
  alert('已添加到关注列表');
};

onMounted(async () => {
  try {
    // 模拟API调用
    setTimeout(() => {
      countyInfo.value = [
        {
          title: '基本信息',
          details: {
            所属省份: countyData.value.province,
            行政级别: countyData.value.level,
            人口数量: `${countyData.value.population}万人`,
            面积: `${countyData.value.area}平方公里`,
          },
        },
        {
          title: '贫困指标',
          details: {
            贫困发生率: `${countyData.value.povertyRate}%`,
            脱贫户数: `${countyData.value.delistedHouseholds}户`,
            脱贫人口: `${countyData.value.delistedPopulation}人`,
            脱贫时间: countyData.value.delistedDate,
          },
        },
        {
          title: '经济指标',
          details: {
            GDP总量: `${countyData.value.gdp}亿元`,
            人均可支配收入: `${countyData.value.income}元`,
            主导产业: countyData.value.industries.join('、'),
            投资总额: `${countyData.value.investment}亿元`,
          },
        },
        {
          title: '政策支持',
          details: {
            扶贫项目: `${countyData.value.projects}个`,
            资金投入: `${countyData.value.funding}亿元`,
            产业项目: `${countyData.value.industryProjects}个`,
            教育支持: countyData.value.educationSupport,
          },
        },
      ];
    }, 500);
  } catch (error) {
    console.error('Failed to fetch county details:', error);
  }
});
</script>

<style scoped>
.county-detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0fff4 100%);
  padding: 20px;
}

.county-detail {
  background: rgba(255, 255, 255, 0.7);
  padding: 35px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
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
  box-shadow: 0 4px 10px rgba(0,0,0,0.03);
  transition: all 0.3s ease;
}

.info-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 15px rgba(0,0,0,0.05);
}

.info-card h3 {
  color: #2c5282;
  margin-bottom: 15px;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-card p {
  margin-bottom: 8px;
  color: #4a5568;
  line-height: 1.5;
}

.info-card strong {
  color: #2d3748;
}

.chart-container {
  background: rgba(255, 255, 255, 0.7);
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  margin-bottom: 25px;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
}

.chart-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(230, 247, 255, 0.6) 0%, rgba(240, 255, 244, 0.6) 100%);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #4a5568;
  font-size: 20px;
  border: 2px dashed rgba(56, 161, 105, 0.3);
  position: relative;
  overflow: hidden;
  text-align: center;
}

.chart-placeholder::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" preserveAspectRatio="none"><path d="M0,0 L100,0 L100,100 Z" fill="rgba(255,255,255,0.2)"/></svg>');
  background-size: cover;
  pointer-events: none;
}

.additional-charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 25px;
}

.btn {
  background: linear-gradient(135deg, #2b6cb0 0%, #38a169 100%);
  color: white;
  padding: 14px 30px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.15);
}

@media (max-width: 768px) {
  .county-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .county-info {
    grid-template-columns: 1fr;
  }
  
  .additional-charts {
    grid-template-columns: 1fr;
  }
}
</style>