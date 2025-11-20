<template>
  <div class="analysis-page">
    <div class="header">
      <h2>📊 数据分析中心</h2>
      <p>深入了解经济和贫困指标的趋势</p>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="选择年份">
          <el-select v-model="filterForm.year" placeholder="选择年份" @change="loadAnalysisData">
            <el-option label="2022" value="2022" />
            <el-option label="2021" value="2021" />
            <el-option label="2020" value="2020" />
            <el-option label="2019" value="2019" />
            <el-option label="2018" value="2018" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="指标类型">
          <el-select v-model="filterForm.indicatorType" placeholder="选择指标" @change="updateCharts">
            <el-option label="GDP与贫困率" value="gdpPoverty" />
            <el-option label="年度趋势对比" value="yearComparison" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="loadAnalysisData" :loading="loading">
            {{ loading ? '加载中...' : '查询数据' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="charts-container">
      <!-- GDP与贫困率散点图 -->
      <div class="chart-card" v-if="filterForm.indicatorType === 'gdpPoverty'">
        <h3>GDP与贫困率关联分析</h3>
        <div ref="scatterChart" style="height: 500px;"></div>
      </div>
      
      <!-- 年度趋势对比图 -->
      <div class="chart-card" v-if="filterForm.indicatorType === 'yearComparison'">
        <h3>年度趋势对比分析</h3>
        <div ref="trendChart" style="height: 500px;"></div>
      </div>
      
      <!-- 数据表格 -->
      <div class="data-table">
        <h3>详细数据</h3>
        <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
          <el-table-column prop="county" label="县名称" width="150" />
          <el-table-column prop="year" label="年份" width="100" />
          <el-table-column prop="gdp" label="GDP(亿元)" width="120">
            <template #default="scope">
              {{ scope.row.gdp?.toFixed(1) }}
            </template>
          </el-table-column>
          <el-table-column prop="povertyRate" label="贫困率(%)" width="120">
            <template #default="scope">
              {{ scope.row.povertyRate?.toFixed(1) }}
            </template>
          </el-table-column>
          <el-table-column prop="ruralIncome" label="农村收入(元)" width="130">
            <template #default="scope">
              {{ scope.row.ruralIncome?.toFixed(0) }}
            </template>
          </el-table-column>
          <el-table-column prop="gdpPerCapita" label="人均GDP(元)" width="130">
            <template #default="scope">
              {{ scope.row.gdpPerCapita?.toFixed(0) }}
            </template>
          </el-table-column>
          <el-table-column prop="fiscalRevenue" label="财政收入(亿元)" width="130">
            <template #default="scope">
              {{ scope.row.fiscalRevenue?.toFixed(1) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getAnalysisData } from '@/api';
import * as echarts from 'echarts';

const filterForm = ref({
  year: '2022',
  indicatorType: 'gdpPoverty'
});

const loading = ref(false);
const analysisData = ref({});
const tableData = ref([]);

let scatterChart = null;
let trendChart = null;

const loadAnalysisData = async () => {
  loading.value = true;
  try {
    const response = await getAnalysisData(filterForm.value.year);
    analysisData.value = response.data;
    updateTableData();
    updateCharts();
  } catch (error) {
    console.error('Failed to load analysis data:', error);
    ElMessage.error('数据加载失败');
  } finally {
    loading.value = false;
  }
};

const updateTableData = () => {
  if (filterForm.value.indicatorType === 'gdpPoverty') {
    tableData.value = analysisData.value.gdpPovertyRelation?.map(item => ({
      county: item.county,
      year: filterForm.value.year,
      gdp: item.gdp,
      povertyRate: item.poverty,
      ruralIncome: Math.round(item.gdp * 450), // 模拟计算
      gdpPerCapita: Math.round(item.gdp * 1000000 / 45000), // 模拟计算，假设人口45万
      fiscalRevenue: Math.round(item.gdp * 0.05) // 模拟计算
    })) || [];
  } else {
    // 对于年度对比，显示汇总数据
    const yearComparison = analysisData.value.yearComparison;
    if (yearComparison) {
      tableData.value = yearComparison.years.map((year, index) => ({
        county: '全国平均',
        year: year,
        gdp: null,
        povertyRate: yearComparison.avgPovertyRate[index],
        ruralIncome: yearComparison.avgIncome[index],
        gdpPerCapita: null,
        fiscalRevenue: null
      }));
    }
  }
};

const updateCharts = () => {
  if (!analysisData.value) return;

  if (filterForm.value.indicatorType === 'gdpPoverty' && scatterChart) {
    const scatterData = analysisData.value.gdpPovertyRelation || [];
    
    scatterChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          return `${params.data.county}<br/>GDP: ${params.data.gdp}亿元<br/>贫困率: ${params.data.poverty}%`;
        }
      },
      xAxis: {
        type: 'value',
        name: 'GDP(亿元)',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: {
        type: 'value',
        name: '贫困率(%)',
        inverse: true,
        nameLocation: 'middle',
        nameGap: 30
      },
      series: [{
        type: 'scatter',
        data: scatterData,
        symbolSize: (data) => {
          return Math.sqrt(data.gdp) * 2;
        },
        itemStyle: {
          color: '#2b6cb0'
        },
        emphasis: {
          focus: 'series'
        }
      }],
      grid: {
        left: '10%',
        right: '10%',
        bottom: '15%',
        top: '15%',
        containLabel: true
      }
    });
  }

  if (filterForm.value.indicatorType === 'yearComparison' && trendChart) {
    const yearComparison = analysisData.value.yearComparison;
    if (!yearComparison) return;

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      legend: {
        data: ['平均贫困率', '平均收入']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: yearComparison.years,
        axisPointer: {
          type: 'shadow'
        }
      },
      yAxis: [
        {
          type: 'value',
          name: '贫困率(%)',
          position: 'left',
          axisLine: {
            show: true,
            lineStyle: {
              color: '#e53e3e'
            }
          },
          axisLabel: {
            formatter: '{value}%'
          }
        },
        {
          type: 'value',
          name: '收入(元)',
          position: 'right',
          axisLine: {
            show: true,
            lineStyle: {
              color: '#38a169'
            }
          },
          axisLabel: {
            formatter: '{value}'
          }
        }
      ],
      series: [
        {
          name: '平均贫困率',
          type: 'line',
          yAxisIndex: 0,
          data: yearComparison.avgPovertyRate,
          lineStyle: { 
            color: '#e53e3e',
            width: 3
          },
          itemStyle: {
            color: '#e53e3e'
          },
          smooth: true
        },
        {
          name: '平均收入',
          type: 'line',
          yAxisIndex: 1,
          data: yearComparison.avgIncome,
          lineStyle: { 
            color: '#38a169',
            width: 3
          },
          itemStyle: {
            color: '#38a169'
          },
          smooth: true
        }
      ]
    });
  }
};

onMounted(() => {
  // 初始化图表
  scatterChart = echarts.init(scatterChart.value);
  trendChart = echarts.init(trendChart.value);
  
  // 加载数据
  loadAnalysisData();
});

onUnmounted(() => {
  if (scatterChart) scatterChart.dispose();
  if (trendChart) trendChart.dispose();
});
</script>

<style scoped>
.analysis-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0fff4 100%);
  padding: 20px;
}

.header {
  text-align: center;
  margin-bottom: 30px;
  background: rgba(255, 255, 255, 0.7);
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
}

.header h2 {
  font-size: 32px;
  color: #2c5282;
  margin-bottom: 10px;
}

.header p {
  font-size: 18px;
  color: #4a5568;
}

.filter-bar {
  background: rgba(255, 255, 255, 0.7);
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 25px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
}

.charts-container {
  display: flex;
  flex-direction: column;
  gap: 25px;
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
  font-size: 20px;
  font-weight: 600;
  text-align: center;
}

.data-table {
  background: rgba(255, 255, 255, 0.7);
  padding: 25px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
}

.data-table h3 {
  margin-bottom: 20px;
  color: #2d3748;
  font-size: 20px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
  }
  
  .chart-card {
    padding: 15px;
  }
}
</style>