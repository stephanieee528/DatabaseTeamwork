<template>
  <div class="analysis-page">
    <div class="header">
      <h2>📊 数据分析中心</h2>
      <p>基于真实数据库的经济和贫困指标分析</p>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="选择省份">
          <el-select v-model="filterForm.provinceId" placeholder="选择省份" clearable>
            <el-option label="全部省份" value="" />
            <el-option v-for="province in provinces" :key="province.provinceId" 
                       :label="province.provinceName" :value="province.provinceId" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择年份">
          <el-select v-model="filterForm.year" placeholder="选择年份">
            <el-option label="2022" value="2022" />
            <el-option label="2021" value="2021" />
            <el-option label="2020" value="2020" />
            <el-option label="2019" value="2019" />
            <el-option label="2018" value="2018" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="searchData" :loading="loading">
            {{ loading ? '搜索中...' : '搜索数据' }}
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-if="tableData.length > 0">
      <div class="stat-card">
        <div class="stat-number">{{ stats.totalCounties }}</div>
        <div class="stat-label">县区数量</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.avgPovertyRate }}%</div>
        <div class="stat-label">平均贫困率</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.avgGDP }}亿元</div>
        <div class="stat-label">平均GDP</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.avgIncome }}元</div>
        <div class="stat-label">平均农村收入</div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="data-table">
      <h3>经济指标数据</h3>
      <div class="table-actions">
        <el-button type="primary" @click="exportData" :disabled="tableData.length === 0">
          导出数据
        </el-button>
        <span class="data-count">共 {{ tableData.length }} 条记录</span>
      </div>
      
      <el-table 
        :data="paginatedData" 
        stripe 
        style="width: 100%" 
        v-loading="loading"
        empty-text="暂无数据，请先搜索">
        <el-table-column prop="countyName" label="县名称" width="150" fixed />
        <el-table-column prop="provinceName" label="所属省份" width="120" />
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column prop="gdp" label="GDP(亿元)" width="120">
          <template #default="scope">
            {{ formatNumber(scope.row.gdp, 1) }}
          </template>
        </el-table-column>
        <el-table-column prop="povertyRate" label="贫困率(%)" width="120">
          <template #default="scope">
            {{ formatNumber(scope.row.povertyRate, 1) }}
          </template>
        </el-table-column>
        <el-table-column prop="ruralDisposableIncome" label="农村收入(元)" width="130">
          <template #default="scope">
            {{ formatNumber(scope.row.ruralDisposableIncome, 0) }}
          </template>
        </el-table-column>
        <el-table-column prop="gdpPerCapita" label="人均GDP(元)" width="130">
          <template #default="scope">
            {{ formatNumber(scope.row.gdpPerCapita, 0) }}
          </template>
        </el-table-column>
        <el-table-column prop="fiscalRevenue" label="财政收入(亿元)" width="130">
          <template #default="scope">
            {{ formatNumber(scope.row.fiscalRevenue, 1) }}
          </template>
        </el-table-column>
        <el-table-column prop="gdpYoy" label="GDP增长率(%)" width="130">
          <template #default="scope">
            {{ formatNumber(scope.row.gdpYoy, 1) }}
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination" v-if="tableData.length > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="tableData.length"
          :page-size="pageSize"
          v-model:current-page="currentPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getCounties, getCountyIndicators, getAnalysisData } from '@/api';

const filterForm = ref({
  provinceId: '',
  year: '2022'
});

const loading = ref(false);
const tableData = ref([]);
const provinces = ref([]);
const counties = ref([]);

// 分页相关
const currentPage = ref(1);
const pageSize = 10;

// 统计数据
const stats = reactive({
  totalCounties: 0,
  avgPovertyRate: 0,
  avgGDP: 0,
  avgIncome: 0
});

// 分页后的数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  const end = start + pageSize;
  return tableData.value.slice(start, end);
});

// 格式化数字显示
const formatNumber = (value, decimals = 0) => {
  if (value === null || value === undefined) return '-';
  return Number(value).toFixed(decimals);
};

// 加载省份数据 - 暂时使用模拟数据，需要后端提供API
const loadProvinces = async () => {
  try {
    // 这里需要调用获取省份的API
    // 暂时使用模拟数据，您需要根据实际API调整
    provinces.value = [
      { provinceId: 1, provinceName: '北京市' },
      { provinceId: 2, provinceName: '河北省' },
      { provinceId: 3, provinceName: '山西省' },
      { provinceId: 4, provinceName: '内蒙古自治区' },
      { provinceId: 5, provinceName: '辽宁省' },
      { provinceId: 6, provinceName: '吉林省' },
      { provinceId: 7, provinceName: '黑龙江省' },
      { provinceId: 8, provinceName: '安徽省' },
      { provinceId: 9, provinceName: '江西省' },
      { provinceId: 10, provinceName: '河南省' },
      { provinceId: 11, provinceName: '湖北省' },
      { provinceId: 12, provinceName: '湖南省' },
      { provinceId: 13, provinceName: '广西壮族自治区' },
      { provinceId: 14, provinceName: '海南省' },
      { provinceId: 15, provinceName: '重庆市' },
      { provinceId: 16, provinceName: '四川省' },
      { provinceId: 17, provinceName: '贵州省' },
      { provinceId: 18, provinceName: '云南省' },
      { provinceId: 19, provinceName: '西藏自治区' },
      { provinceId: 20, provinceName: '陕西省' },
      { provinceId: 21, provinceName: '甘肃省' },
      { provinceId: 22, provinceName: '青海省' },
      { provinceId: 23, provinceName: '宁夏回族自治区' },
      { provinceId: 24, provinceName: '新疆维吾尔自治区' }
    ];
  } catch (error) {
    console.error('加载省份数据失败:', error);
    ElMessage.error('省份数据加载失败');
  }
};

// 搜索数据
const searchData = async () => {
  if (!filterForm.value.year) {
    ElMessage.warning('请选择年份');
    return;
  }

  loading.value = true;
  tableData.value = [];
  
  try {
    // 先尝试使用分析数据API
    try {
      const analysisResponse = await getAnalysisData(parseInt(filterForm.value.year));
      const analysisData = analysisResponse.data;
      
      // 如果有分析数据，直接使用
      if (analysisData && analysisData.gdpPovertyRelation) {
        const formattedData = analysisData.gdpPovertyRelation.map(item => ({
          id: item.id || Math.random(),
          countyName: item.county || '未知县',
          provinceName: item.province || '未知省份',
          year: parseInt(filterForm.value.year),
          gdp: item.gdp,
          povertyRate: item.poverty,
          ruralDisposableIncome: item.income || 0,
          gdpPerCapita: item.gdpPerCapita || 0,
          fiscalRevenue: item.fiscalRevenue || 0,
          gdpYoy: item.gdpYoy || 0
        }));
        
        tableData.value = formattedData;
        updateStats(formattedData);
        ElMessage.success(`搜索到 ${formattedData.length} 条数据`);
        return;
      }
    } catch (error) {
      console.log('分析数据API不可用，使用备选方案:', error);
    }

    // 备选方案：获取县列表然后获取每个县的指标
    const params = filterForm.value.provinceId ? { provinceId: parseInt(filterForm.value.provinceId) } : {};
    const countiesResponse = await getCounties(params);
    counties.value = countiesResponse.data || [];
    
    if (counties.value.length === 0) {
      ElMessage.warning('未找到符合条件的县数据');
      return;
    }

    // 为每个县获取经济指标数据
    const allData = [];
    const year = parseInt(filterForm.value.year);
    
    for (const county of counties.value) {
      try {
        const indicatorsResponse = await getCountyIndicators(
          county.countyId, 
          year, 
          year
        );
        
        const indicators = indicatorsResponse.data;
        if (indicators && indicators.length > 0) {
          const indicator = indicators[0];
          allData.push({
            id: county.countyId,
            countyName: county.countyName,
            provinceName: county.province ? county.province.provinceName : '未知省份',
            year: indicator.year,
            gdp: indicator.gdp,
            povertyRate: indicator.povertyRate,
            ruralDisposableIncome: indicator.ruralDisposableIncome,
            gdpPerCapita: indicator.gdpPerCapita,
            fiscalRevenue: indicator.fiscalRevenue,
            gdpYoy: indicator.gdpYoy
          });
        }
      } catch (error) {
        console.error(`获取县 ${county.countyName} 数据失败:`, error);
      }
    }
    
    tableData.value = allData;
    updateStats(allData);
    ElMessage.success(`搜索到 ${allData.length} 条数据`);
  } catch (error) {
    console.error('搜索数据失败:', error);
    ElMessage.error('数据搜索失败');
  } finally {
    loading.value = false;
  }
};

// 更新统计数据
const updateStats = (data) => {
  if (data.length === 0) {
    stats.totalCounties = 0;
    stats.avgPovertyRate = 0;
    stats.avgGDP = 0;
    stats.avgIncome = 0;
    return;
  }
  
  stats.totalCounties = data.length;
  
  // 计算平均贫困率
  const validPovertyRates = data.filter(item => item.povertyRate != null).map(item => Number(item.povertyRate));
  stats.avgPovertyRate = validPovertyRates.length > 0 
    ? (validPovertyRates.reduce((sum, rate) => sum + rate, 0) / validPovertyRates.length).toFixed(1)
    : 0;
  
  // 计算平均GDP
  const validGDPs = data.filter(item => item.gdp != null).map(item => Number(item.gdp));
  stats.avgGDP = validGDPs.length > 0 
    ? (validGDPs.reduce((sum, gdp) => sum + gdp, 0) / validGDPs.length).toFixed(1)
    : 0;
  
  // 计算平均农村收入
  const validIncomes = data.filter(item => item.ruralDisposableIncome != null).map(item => Number(item.ruralDisposableIncome));
  stats.avgIncome = validIncomes.length > 0 
    ? Math.floor(validIncomes.reduce((sum, income) => sum + income, 0) / validIncomes.length)
    : 0;
};

// 重置搜索
const resetSearch = () => {
  filterForm.value = {
    provinceId: '',
    year: '2022'
  };
  tableData.value = [];
  currentPage.value = 1;
};

// 导出数据
const exportData = () => {
  const headers = ['县名称', '所属省份', '年份', 'GDP(亿元)', '贫困率(%)', '农村收入(元)', '人均GDP(元)', '财政收入(亿元)', 'GDP增长率(%)'];
  const csvData = tableData.value.map(item => [
    item.countyName,
    item.provinceName,
    item.year,
    item.gdp || '-',
    item.povertyRate || '-',
    item.ruralDisposableIncome || '-',
    item.gdpPerCapita || '-',
    item.fiscalRevenue || '-',
    item.gdpYoy || '-'
  ]);
  
  let csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + headers.join(',') + '\n';
  csvData.forEach(row => {
    csvContent += row.join(',') + '\n';
  });
  
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `经济指标数据_${filterForm.value.year}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  ElMessage.success('数据导出成功');
};

onMounted(async () => {
  // 加载省份数据
  await loadProvinces();
  // 页面加载时自动搜索当前年份的数据
  await searchData();
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.7);
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #2b6cb0;
  margin-bottom: 8px;
}

.stat-label {
  color: #4a5568;
  font-size: 16px;
  font-weight: 500;
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

.table-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.data-count {
  color: #718096;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .table-actions {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
}
</style>