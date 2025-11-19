<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <h1>832 工程贫困县脱贫情况可视化分析系统</h1>
      <div class="search-container">
        <el-input 
          v-model="searchCounty" 
          placeholder="搜索贫困县..." 
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch" icon="Search" />
          </template>
        </el-input>
      </div>
    </header>

    <div class="timeline-container">
      <el-timeline :reverse="false" mode="center">
        <el-timeline-item 
          v-for="year in years" 
          :key="year"
          :timestamp="year + '年'"
          :color="currentYear === year ? '#409EFF' : ''"
          @click="changeYear(year)"
        >
        </el-timeline-item>
      </el-timeline>
    </div>

    <main class="dashboard-main">
      <div class="map-container">
        <el-card>
          <div slot="header">
            <h2>贫困县分布与摘帽状态 ({{ currentYear }})</h2>
          </div>
          <!-- 加载状态提示 -->
          <div v-if="isMapLoading" class="loading-container">
            <el-loading-spinner size="large" />
            <p>地图数据加载中...</p>
          </div>
          <!-- 加载失败提示 -->
          <div v-else-if="mapLoadError" class="error-container">
            <el-icon class="error-icon"><WarningFilled /></el-icon>
            <p>{{ errorMsg }}</p>
            <el-button size="small" @click="retryLoadMap">重试</el-button>
          </div>
          <!-- 地图容器 -->
          <div v-else id="china-map" class="chart-container"></div>
        </el-card>
      </div>

      <div class="province-chart-container">
        <el-card>
          <div slot="header">
            <h2>各省贫困县数量对比</h2>
          </div>
          <div v-if="!povertyData.length" class="loading-container">
            <p>暂无数据，请稍后再试。</p>
          </div>
          <div v-else id="province-bar" class="chart-container"></div>
        </el-card>
      </div>

      <div class="trend-chart-container">
        <el-card>
          <div slot="header">
            <h2>贫困县摘帽趋势 (2016-2020)</h2>
          </div>
          <div v-if="!delistingTrendData.data.length" class="loading-container">
            <p>暂无数据，请稍后再试。</p>
          </div>
          <div v-else id="delisting-trend" class="chart-container"></div>
        </el-card>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import * as echarts from 'echarts';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { WarningFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getChartsData } from '@/api';

// 年份范围
const years = [2016, 2017, 2018, 2019, 2020];
const currentYear = ref(2020);
const searchCounty = ref('');
const router = useRouter();

// 地图相关状态
const isMapLoading = ref(true); // 地图加载中状态
const mapLoadError = ref(false); // 地图加载失败状态
const chinaMapData = ref<any>(null); // 存储接口返回的地图数据
const errorMsg = ref('地图数据加载失败，请刷新页面重试'); // 错误提示信息
const mapApiUrl = 'https://geojson.cn/api/china/_meta.json'; // 用户提供的接口地址

// 模拟贫困县数据 - 实际项目中替换为后端832工程数据接口
const povertyData = ref([
  { name: '河北省', count: 28, delisted: 20, year: 2020 },
  { name: '山西省', count: 35, delisted: 25, year: 2020 },
  { name: '河南省', count: 38, delisted: 30, year: 2020 },
  { name: '四川省', count: 66, delisted: 50, year: 2020 },
  { name: '贵州省', count: 66, delisted: 55, year: 2020 },
  { name: '云南省', count: 88, delisted: 75, year: 2020 },
  { name: '甘肃省', count: 58, delisted: 45, year: 2020 },
]);

// 摘帽趋势数据 - 实际项目中替换为后端接口
const delistingTrendData = ref({
  years: [2016, 2017, 2018, 2019, 2020],
  data: [120, 180, 220, 150, 832] // 每年摘帽县数量（832为总贫困县数）
});

// 图表数据
const chartsData = ref({});

// 地图数据格式化：将GeoJSON转换为ECharts所需格式（关联贫困县数据）
const formatMapData = (geojson: any) => {
  // 遍历GeoJSON的省份，匹配贫困县数据
  return geojson.features.map((region: any) => {
    const provinceName = region.properties.name;
    // 查找该省份的贫困县数据（匹配省份名称）
    const provincePoverty = povertyData.value.find(item => item.name === provinceName);
    
    return {
      name: provinceName,
      value: provincePoverty ? provincePoverty.count : 0, // 剩余贫困县数量
      delisted: provincePoverty ? provincePoverty.delisted : 0, // 已摘帽县数量
      adcode: region.properties.adcode || '', // 保留行政区划代码（便于后续扩展）
    };
  });
};

// 图表实例
let chinaMapChart: echarts.ECharts;
let provinceBarChart: echarts.ECharts;
let delistingTrendChart: echarts.ECharts;

// 检查接口返回数据是否为标准GeoJSON
const isGeoJSON = (data: any) => {
  return data.type === 'FeatureCollection' && Array.isArray(data.features);
};

// 从用户提供的接口请求地图数据（处理元数据二次请求逻辑）
const fetchChinaMapData = async () => {
  try {
    isMapLoading.value = true;
    mapLoadError.value = false;
    
    // 第一步：请求用户提供的接口
    const response = await axios.get(mapApiUrl, {
      timeout: 10000, // 10秒超时
      headers: {
        'Accept': 'application/json'
      }
    });
    const resData = response.data;

    // 处理两种情况：1. 直接返回GeoJSON 2. 返回元数据（包含真实GeoJSON地址）
    let finalGeoJSON: any;
    if (isGeoJSON(resData)) {
      // 情况1：直接返回GeoJSON，直接使用
      finalGeoJSON = resData;
    } else if (resData.url && typeof resData.url === 'string') {
      // 情况2：返回元数据（含真实地图数据地址），二次请求
      const geoResponse = await axios.get(resData.url, { timeout: 10000 });
      if (isGeoJSON(geoResponse.data)) {
        finalGeoJSON = geoResponse.data;
      } else {
        throw new Error('元数据指向的地址未返回有效GeoJSON');
      }
    } else {
      throw new Error('接口返回数据格式不是标准GeoJSON或元数据');
    }

    // 存储地图数据并初始化地图
    chinaMapData.value = finalGeoJSON;
    isMapLoading.value = false;

  } catch (error: any) {
    console.error('地图数据请求失败:', error);
    isMapLoading.value = false;
    mapLoadError.value = true;
    
    // 区分错误类型，给出精准提示
    if (error.message.includes('timeout')) {
      errorMsg.value = '地图数据请求超时，请检查网络连接';
    } else if (error.message.includes('404')) {
      errorMsg.value = '地图接口地址不存在，请确认接口有效性';
    } else if (error.message.includes('CORS')) {
      errorMsg.value = '跨域访问被拒绝，请配置后端代理或接口跨域许可';
    } else {
      errorMsg.value = `地图加载失败：${error.message}`;
    }
    ElMessage.error(errorMsg.value);
  }
};

// 初始化地图（依赖接口返回的GeoJSON数据）
const initChinaMap = () => {
  if (!chinaMapData.value) return;
  
  const mapDom = document.getElementById('china-map');
  if (mapDom) {
    chinaMapChart = echarts.init(mapDom);
    
    // 注册中国地图（使用接口返回的GeoJSON数据）
    echarts.registerMap('china', chinaMapData.value);
    
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: function(params: any) {
          return `
            <div style="padding: 4px 8px;">
              <div style="font-weight: 600;">${params.name}</div>
              <div>剩余贫困县：${params.value} 个</div>
              <div>已摘帽县：${params.data.delisted} 个</div>
              <div>脱贫完成率：${params.value + params.data.delisted === 0 ? '0%' : Math.round((params.data.delisted / (params.value + params.data.delisted)) * 100)}%</div>
            </div>
          `;
        },
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderColor: '#409EFF',
        borderWidth: 1,
        textStyle: { fontSize: 12 }
      },
      visualMap: {
        type: 'piecewise',
        pieces: [
          { value: 0, label: '0个（已脱贫）', color: '#e6f7e6' },
          { min: 1, max: 5, label: '1-5个', color: '#fff3cd' },
          { min: 6, max: 10, label: '6-10个', color: '#ffd9b3' },
          { min: 11, max: 20, label: '11-20个', color: '#ffb3b3' },
          { min: 21, label: '21个以上', color: '#ff8080' }
        ],
        orient: 'horizontal',
        left: 'center',
        bottom: 20,
        textStyle: { fontSize: 11 },
        itemWidth: 15,
        itemHeight: 10
      },
      series: [
        {
          name: '贫困县数量',
          type: 'map',
          mapType: 'china',
          roam: true, // 允许缩放和拖拽
          zoom: 1.2, // 初始缩放比例
          label: {
            show: true,
            fontSize: 9,
            color: '#333'
          },
          emphasis: { // 鼠标hover高亮样式
            label: { color: '#409EFF', fontSize: 10, fontWeight: 600 },
            itemStyle: { areaColor: '#e6f7ff' }
          },
          data: formatMapData(chinaMapData.value)
        }
      ]
    };
    
    chinaMapChart.setOption(option);
    
    // 点击省份事件：跳转至该省份贫困县列表（预留路由）
    chinaMapChart.on('click', (params: any) => {
      console.log('点击省份：', params.name, '行政区划代码：', params.data.adcode);
      // 实际项目中可跳转至省份详情页：router.push(`/province/${params.data.adcode}`)
      ElMessage.info(`已选择省份：${params.name}`);
    });
  }
};

// 初始化省份对比柱状图
const initProvinceBar = () => {
  const barDom = document.getElementById('province-bar');
  if (barDom) {
    provinceBarChart = echarts.init(barDom);
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderColor: '#409EFF',
        borderWidth: 1
      },
      legend: {
        data: ['剩余贫困县', '已摘帽县'],
        top: 0
      },
      grid: {
        left: '5%',
        right: '4%',
        bottom: '8%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: povertyData.value.map(item => item.name),
        axisLabel: {
          rotate: 30,
          fontSize: 11
        }
      },
      yAxis: {
        type: 'value',
        name: '县数量（个）',
        nameTextStyle: { fontSize: 11 }
      },
      series: [
        {
          name: '剩余贫困县',
          type: 'bar',
          data: povertyData.value.map(item => item.count),
          itemStyle: { color: '#ff9999' },
          barWidth: '30%'
        },
        {
          name: '已摘帽县',
          type: 'bar',
          data: povertyData.value.map(item => item.delisted),
          itemStyle: { color: '#66cc99' },
          barWidth: '30%'
        }
      ]
    };
    
    provinceBarChart.setOption(option);
  }
};

// 初始化摘帽趋势折线图
const initDelistingTrend = () => {
  const trendDom = document.getElementById('delisting-trend');
  if (trendDom) {
    delistingTrendChart = echarts.init(trendDom);
    
    const option = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderColor: '#409EFF',
        borderWidth: 1
      },
      legend: {
        data: ['年度摘帽县数量'],
        top: 0
      },
      grid: {
        left: '5%',
        right: '4%',
        bottom: '8%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: delistingTrendData.value.years
      },
      yAxis: {
        type: 'value',
        name: '摘帽县数量（个）',
        nameTextStyle: { fontSize: 11 }
      },
      series: [
        {
          name: '年度摘帽县数量',
          type: 'line',
          stack: 'Total',
          data: delistingTrendData.value.data,
          itemStyle: { color: '#409EFF' },
          lineStyle: { width: 3 },
          symbol: 'circle',
          symbolSize: 6,
          emphasis: { symbolSize: 8 },
          markPoint: {
            data: [
              { type: 'max', name: '最多' },
              { type: 'min', name: '最少' }
            ],
            itemStyle: { color: '#ff6666' }
          },
          markLine: {
            data: [{ type: 'average', name: '年均摘帽' }],
            lineStyle: { type: 'dashed', color: '#999' }
          }
        }
      ]
    };
    
    delistingTrendChart.setOption(option);
  }
};

// 年份变更处理：同步请求对应年份的贫困县数据
const changeYear = async (year: number) => {
  currentYear.value = year;
  isMapLoading.value = true;
  
  try {
    // 实际项目中：替换为后端按年份查询数据的接口
    // const res = await axios.get(`/api/832/poverty-data?year=${year}`);
    // povertyData.value = res.data.provinceData;
    // delistingTrendData.value.data = res.data.trendData;
    
    // 模拟接口请求延迟（实际项目中删除）
    setTimeout(() => {
      // 模拟年份数据变化（示例：2016年摘帽县更少）
      if (year === 2016) {
        povertyData.value = [
          { name: '河北省', count: 28, delisted: 5, year: 2016 },
          { name: '山西省', count: 35, delisted: 8, year: 2016 },
          { name: '河南省', count: 38, delisted: 10, year: 2016 },
          { name: '四川省', count: 66, delisted: 15, year: 2016 },
          { name: '贵州省', count: 66, delisted: 12, year: 2016 },
          { name: '云南省', count: 88, delisted: 20, year: 2016 },
          { name: '甘肃省', count: 58, delisted: 10, year: 2016 },
        ];
      } else if (year === 2018) {
        povertyData.value = [
          { name: '河北省', count: 15, delisted: 13, year: 2018 },
          { name: '山西省', count: 20, delisted: 15, year: 2018 },
          { name: '河南省', count: 22, delisted: 16, year: 2018 },
          { name: '四川省', count: 35, delisted: 31, year: 2018 },
          { name: '贵州省', count: 30, delisted: 36, year: 2018 },
          { name: '云南省', count: 45, delisted: 43, year: 2018 },
          { name: '甘肃省', count: 30, delisted: 28, year: 2018 },
        ];
      }

      // 更新所有图表
      isMapLoading.value = false;
      if (chinaMapData.value) initChinaMap();
      initProvinceBar();
      initDelistingTrend();
    }, 600);

  } catch (error) {
    console.error(`年份${year}数据更新失败:', error`);
    isMapLoading.value = false;
    ElMessage.error(`更新${year}年数据失败，请重试`);
  }
};

// 搜索贫困县：跳转至详情页
const handleSearch = () => {
  const countyName = searchCounty.value.trim();
  if (countyName) {
    router.push(`/county/${encodeURIComponent(countyName)}`); // 编码特殊字符
    searchCounty.value = ''; // 清空搜索框
  } else {
    ElMessage.warning('请输入贫困县名称');
  }
};

// 重试加载地图
const retryLoadMap = () => {
  fetchChinaMapData();
};

// 获取图表数据
const fetchChartsData = async () => {
  try {
    const response = await getChartsData(currentYear.value);
    chartsData.value = response.data;
    console.log('Charts data loaded:', chartsData.value);
  } catch (error) {
    console.error('Failed to load charts data:', error);
    ElMessage.error('图表数据加载失败，请稍后重试');
  }
};

// 初始化：请求地图数据 + 初始化其他图表
onMounted(() => {
  // 1. 优先请求地图数据（异步）
  fetchChinaMapData();
  // 2. 初始化不依赖地图的图表
  initProvinceBar();
  initDelistingTrend();
  // 3. 获取图表数据
  fetchChartsData();
  
  // 窗口大小变化时重绘图表
  window.addEventListener('resize', () => {
    chinaMapChart?.resize();
    provinceBarChart?.resize();
    delistingTrendChart?.resize();
  });
});

// 监听地图数据变化，重新初始化地图
watch(chinaMapData, (newVal) => {
  if (newVal) initChinaMap();
});

// 监听贫困县数据变化，更新地图（年份切换时触发）
watch(povertyData, () => {
  if (chinaMapData.value && chinaMapChart) {
    chinaMapChart.setOption({
      series: [{ data: formatMapData(chinaMapData.value) }]
    });
  }
});

// 监听年份变化，重新获取图表数据
watch(currentYear, () => {
  fetchChartsData();
});
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 0 20px 20px;
  box-sizing: border-box;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  flex-wrap: wrap;
  gap: 16px;
}

.dashboard-header h1 {
  color: #1f2329;
  margin: 0;
  font-size: 24px;
}

.search-container {
  width: 300px;
  min-width: 200px;
}

.timeline-container {
  margin: 20px 0;
  padding: 15px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.dashboard-main {
  display: grid;
  grid-template-columns: 2fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 20px;
  height: calc(100vh - 240px);
}

@media (max-width: 1200px) {
  .dashboard-main {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
    height: auto;
  }
}

.map-container, .province-chart-container, .trend-chart-container {
  height: 100%;
  min-height: 300px;
}

.chart-container {
  width: 100%;
  height: calc(100% - 50px);
  position: relative;
}

.el-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.el-card__header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.el-card__header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2329;
}

/* 加载状态样式 */
.loading-container {
  width: 100%;
  height: calc(100% - 50px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #fff;
}

.loading-container p {
  margin-top: 16px;
  color: #606266;
  font-size: 14px;
}

/* 错误状态样式 */
.error-container {
  width: 100%;
  height: calc(100% - 50px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #fff;
  color: #f56c6c;
  padding: 20px;
  text-align: center;
}

.error-icon {
  font-size: 32px;
  margin-bottom: 16px;
}

.error-container p {
  margin: 0 0 16px 0;
  font-size: 14px;
  line-height: 1.5;
}
</style>