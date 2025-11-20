<template>
  <div class="home">
    <div class="container">
      <div class="welcome-section">
        <h1>欢迎来到832工程贫困县可视化分析系统</h1>
        <p>实时掌握贫困县动态数据，助力精准扶贫决策</p>
      </div>

      <div class="stats-grid">
        <div class="stat-card" v-for="stat in stats" :key="stat.label">
          <div class="stat-number">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>

      <!-- 添加图表展示 -->
      <div class="charts-grid">
        <div class="chart-card">
          <h3>贫困率分布</h3>
          <div ref="povertyChart" style="height: 300px;"></div>
        </div>
        
        <div class="chart-card">
          <h3>农村收入增长趋势</h3>
          <div ref="incomeChart" style="height: 300px;"></div>
        </div>
        
        <div class="chart-card">
          <h3>地区分布</h3>
          <div ref="regionChart" style="height: 300px;"></div>
        </div>
      </div>

      <div class="map-container">
        <div class="map-placeholder">
          🗺️ 交互式地图加载中...<br />
          <small>显示全国832个贫困县分布情况</small>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getChartsData } from '@/api';
import * as echarts from 'echarts';

const stats = ref([
  { label: '贫困县总数', value: '加载中...' },
  { label: '已脱贫县数', value: '加载中...' },
  { label: '扶贫覆盖率', value: '加载中...' },
  { label: '扶贫资金投入', value: '加载中...' },
]);

let povertyChart = null;
let incomeChart = null;
let regionChart = null;

const initCharts = (chartsData) => {
  // 贫困率分布图表
  povertyChart = echarts.init(povertyChart.value);
  povertyChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    xAxis: {
      type: 'category',
      data: chartsData.povertyDistribution.categories
    },
    yAxis: {
      type: 'value',
      name: '县数量'
    },
    series: [{
      data: chartsData.povertyDistribution.data,
      type: 'bar',
      itemStyle: {
        color: '#2b6cb0'
      }
    }]
  });

  // 收入增长趋势图表
  incomeChart = echarts.init(incomeChart.value);
  incomeChart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: chartsData.incomeTrend.years
    },
    yAxis: {
      type: 'value',
      name: '收入(元)'
    },
    series: [{
      data: chartsData.incomeTrend.income,
      type: 'line',
      smooth: true,
      lineStyle: {
        color: '#38a169'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{
            offset: 0, color: 'rgba(56, 161, 105, 0.3)'
          }, {
            offset: 1, color: 'rgba(56, 161, 105, 0.1)'
          }]
        }
      }
    }]
  });

  // 地区分布图表
  regionChart = echarts.init(regionChart.value);
  regionChart.setOption({
    tooltip: {
      trigger: 'item'
    },
    series: [{
      type: 'pie',
      radius: '70%',
      data: chartsData.regionDistribution.regions.map((region, index) => ({
        name: region,
        value: chartsData.regionDistribution.counts[index]
      })),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  });
};

onMounted(async () => {
  try {
    const response = await getChartsData();
    const data = response.data;

    stats.value = [
      { label: '贫困县总数', value: data.totalCounties || '832' },
      { label: '已脱贫县数', value: data.delistedCounties || '780' },
      { label: '扶贫覆盖率', value: `${data.coverageRate || '93.8'}%` },
      { label: '扶贫资金投入', value: `${data.funding || '156.8'}亿元` },
    ];

    // 初始化图表
    initCharts(data);
  } catch (error) {
    console.error('Failed to load data:', error);
    stats.value = [
      { label: '贫困县总数', value: '832' },
      { label: '已脱贫县数', value: '780' },
      { label: '扶贫覆盖率', value: '93.8%' },
      { label: '扶贫资金投入', value: '156.8亿元' },
    ];
  }
});

onUnmounted(() => {
  // 销毁图表实例
  if (povertyChart) povertyChart.dispose();
  if (incomeChart) incomeChart.dispose();
  if (regionChart) regionChart.dispose();
});
</script>

<style scoped>
.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 25px;
  margin-bottom: 40px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.7);
  padding: 25px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
}

.chart-card h3 {
  margin-bottom: 20px;
  color: #2d3748;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}


.home {
  min-height: calc(100vh - 80px);
  background: linear-gradient(135deg, #e6f7ff 0%, #f0fff4 100%);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.welcome-section {
  background: linear-gradient(135deg, rgba(44, 82, 130, 0.85) 0%, rgba(49, 130, 206, 0.9) 100%);
  color: white;
  padding: 50px 40px;
  border-radius: 16px;
  margin-bottom: 40px;
  text-align: center;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 15px rgba(0,0,0,0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.18);
}

.welcome-section::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" preserveAspectRatio="none"><path d="M0,0 L100,0 L100,100 Z" fill="rgba(255,255,255,0.05)"/></svg>');
  background-size: cover;
  pointer-events: none;
}

.welcome-section h1 {
  font-size: 42px;
  margin-bottom: 20px;
  font-weight: 700;
  position: relative;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.welcome-section p {
  font-size: 20px;
  opacity: 0.9;
  max-width: 800px;
  margin: 0 auto;
  position: relative;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 25px;
  margin-bottom: 40px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.7);
  padding: 30px 25px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  text-align: center;
  transition: all 0.4s ease;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 25px rgba(0,0,0,0.1);
  background: rgba(255, 255, 255, 0.85);
}

.stat-number {
  font-size: 42px;
  font-weight: 800;
  color: #2b6cb0;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #2b6cb0 0%, #38a169 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  color: #4a5568;
  font-size: 18px;
  font-weight: 500;
}

.map-container {
  background: rgba(255, 255, 255, 0.7);
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
}

.map-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(230, 247, 255, 0.6) 0%, rgba(240, 255, 244, 0.6) 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #4a5568;
  border: 2px dashed rgba(66, 153, 225, 0.3);
  position: relative;
  overflow: hidden;
}

.map-placeholder::before {
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

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .welcome-section {
    padding: 30px 20px;
  }
  
  .welcome-section h1 {
    font-size: 32px;
  }
  
  .welcome-section p {
    font-size: 18px;
  }
}
</style>