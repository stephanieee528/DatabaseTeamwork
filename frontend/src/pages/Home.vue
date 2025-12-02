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

      <div class="map-container">
        <div id="map" style="height: 100%; width: 100%;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { getSummary } from '@/api';
import * as echarts from 'echarts';
import axios from 'axios';

const router = useRouter();
const stats = ref([
  { label: '贫困县总数', value: '加载中...' },
  { label: '已脱贫县数', value: '加载中...' },
  { label: '扶贫覆盖率', value: '加载中...' },
  { label: '扶贫资金投入', value: '加载中...' },
]);
let summaryRetries = 0;

const formatCount = (value) => {
  if (value === null || value === undefined) return '暂无数据';
  return `${Intl.NumberFormat('zh-CN').format(value)} 个`;
};

const formatPercent = (value) => {
  if (value === null || value === undefined) return '暂无数据';
  return `${Number(value).toFixed(1)}%`;
};

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '暂无数据';
  return `${Number(value).toFixed(1)} 亿元`;
};

let myChart = null;

// 贫困县分布数据
const povertyData = [
  { name: '云南省', value: 73 },
  { name: '贵州省', value: 66 },
  { name: '四川省', value: 45 },
  { name: '甘肃省', value: 58 },
  { name: '陕西省', value: 50 },
  { name: '河北省', value: 45 },
  { name: '山西省', value: 36 },
  { name: '内蒙古自治区', value: 31 },
  { name: '辽宁省', value: 15 },
  { name: '吉林省', value: 8 },
  { name: '黑龙江省', value: 14 },
  { name: '安徽省', value: 20 },
  { name: '江西省', value: 24 },
  { name: '河南省', value: 38 },
  { name: '湖北省', value: 28 },
  { name: '湖南省', value: 40 },
  { name: '广西壮族自治区', value: 54 },
  { name: '海南省', value: 5 },
  { name: '重庆市', value: 14 },
  { name: '青海省', value: 42 },
  { name: '宁夏回族自治区', value: 8 },
  { name: '新疆维吾尔自治区', value: 35 },
  { name: '西藏自治区', value: 74 }
];

// 获取GeoJSON数据
const getGeoJson = async (jsonName) => {
  try {
    // 使用阿里云DataV的GeoJSON数据
    const response = await axios.get(`https://geo.datav.aliyun.com/areas_v3/bound/${jsonName}`);
    return response.data;
  } catch (error) {
    console.error('获取GeoJSON数据失败:', error);
    throw error;
  }
};

// 初始化图表
const initChart = async () => {
  const chartDom = document.getElementById('map');
  if (!chartDom) {
    console.error('地图容器未找到');
    return;
  }
  
  myChart = echarts.init(chartDom);
  
  try {
    // 显示加载动画
    myChart.showLoading();
    
    // 获取中国地图的GeoJSON数据
    const chinaGeoJson = await getGeoJson('100000_full.json');
    
    // 注册地图
    echarts.registerMap('china', chinaGeoJson);
    
    // 初始化ECharts
    initEcharts();
    
  } catch (error) {
    console.error('初始化地图失败:', error);
    myChart.hideLoading();
    
    // 如果在线数据获取失败，尝试使用本地数据
    try {
      await initWithLocalGeoJson();
    } catch (localError) {
      console.error('本地地图数据也失败:', localError);
      showErrorMap();
    }
  }
};

// 使用本地GeoJSON数据初始化
const initWithLocalGeoJson = async () => {
  // 尝试不同的本地路径
  const paths = [
    '/china.geojson',
    '/assets/china.geojson',
    './china.geojson',
    './assets/china.geojson'
  ];
  
  for (const path of paths) {
    try {
      const response = await fetch(path);
      if (response.ok) {
        const geoJson = await response.json();
        echarts.registerMap('china', geoJson);
        initEcharts();
        return;
      }
    } catch (error) {
      console.warn(`路径 ${path} 加载失败:`, error);
    }
  }
  
  throw new Error('所有本地路径都失败');
};

// 初始化ECharts配置
const initEcharts = () => {
  myChart.hideLoading();
  
  const option = {
    title: {
      text: '832工程贫困县分布图',
      subtext: '全国贫困县数量可视化分析',
      left: 'center',
      textStyle: {
        color: '#2c5282',
        fontSize: 18
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        if (params.componentType === 'series') {
          const value = params.value || 0;
          const percentage = ((value / 832) * 100).toFixed(1);
          return `${params.name}<br/>贫困县数量: ${value}个<br/>占全国比例: ${percentage}%`;
        }
        return params.name;
      }
    },
    toolbox: {
      show: true,
      orient: 'vertical',
      left: 'right',
      top: 'center',
      feature: {
        dataView: { 
          readOnly: false,
          title: '数据视图',
          lang: ['数据视图', '关闭', '刷新']
        },
        restore: { 
          title: '还原'
        },
        saveAsImage: { 
          title: '保存为图片',
          pixelRatio: 2
        }
      }
    },
    visualMap: {
      type: 'piecewise',
      pieces: [
        {min: 60, label: '60+ 个', color: '#a50f15'},
        {min: 40, max: 59, label: '40-59 个', color: '#de2d26'},
        {min: 20, max: 39, label: '20-39 个', color: '#fb6a4a'},
        {min: 1, max: 19, label: '1-19 个', color: '#fcae91'}
      ],
      left: 'left',
      top: 'bottom',
      textStyle: {
        color: '#4a5568'
      },
      orient: 'vertical'
    },
    series: [
      {
        name: '贫困县数量',
        type: 'map',
        map: 'china',
        roam: true,
        zoom: 1.2,
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            areaColor: '#63b3ed',
            borderColor: '#3182ce',
            borderWidth: 2
          }
        },
        itemStyle: {
          areaColor: '#ebf8ff',
          borderColor: '#3182ce',
          borderWidth: 1
        },
        data: povertyData,
        // 名称映射，确保GeoJSON中的名称与数据匹配
        nameMap: {
          '新疆': '新疆维吾尔自治区',
          '广西': '广西壮族自治区',
          '西藏': '西藏自治区',
          '宁夏': '宁夏回族自治区',
          '内蒙古': '内蒙古自治区'
        }
      }
    ]
  };
  
  myChart.setOption(option);
  
  // 添加窗口大小变化监听
  window.addEventListener('resize', function() {
    myChart.resize();
  });
  
  // 添加点击事件
  myChart.on('click', function(params) {
    if (params.componentType === 'series' && params.componentSubType === 'map') {
      const provinceName = params.name;
      const provinceData = povertyData.find(p => p.name === provinceName);
      if (provinceData) {
        console.log(`点击了 ${provinceName}，贫困县数量: ${provinceData.value}个`);
        router.push({
          path: '/county',
          query: { province: provinceName },
        });
      }
    }
  });
};

// 显示错误地图
const showErrorMap = () => {
  const option = {
    title: {
      text: '832工程贫困县分布图',
      subtext: '地图数据加载失败，显示简化版本',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>贫困县数量: {c}个'
    },
    visualMap: {
      type: 'piecewise',
      pieces: [
        {min: 60, label: '60+ 个', color: '#a50f15'},
        {min: 40, max: 59, label: '40-59 个', color: '#de2d26'},
        {min: 20, max: 39, label: '20-39 个', color: '#fb6a4a'},
        {min: 1, max: 19, label: '1-19 个', color: '#fcae91'}
      ],
      left: 'left',
      top: 'bottom'
    },
    series: [
      {
        name: '贫困县数量',
        type: 'scatter',
        data: povertyData.map(item => ({
          name: item.name,
          value: [getProvinceLng(item.name), getProvinceLat(item.name), item.value],
          itemStyle: {
            color: getColorByCount(item.value)
          }
        })),
        symbolSize: function(val) {
          return Math.max(val[2] / 3, 8);
        },
        label: {
          show: true,
          formatter: '{b}\n{c}个',
          position: 'top'
        },
        emphasis: {
          label: {
            show: true,
            fontWeight: 'bold'
          }
        }
      }
    ]
  };
  
  myChart.setOption(option);
};

// 获取省份的经度（简化版）
const getProvinceLng = (name) => {
  const lngMap = {
    '北京': 116.4, '天津': 117.2, '上海': 121.47, '重庆': 106.54,
    '河北': 114.48, '山西': 112.53, '内蒙古': 111.65, '辽宁': 123.38,
    '吉林': 125.35, '黑龙江': 126.63, '江苏': 118.78, '浙江': 120.19,
    '安徽': 117.27, '福建': 119.3, '江西': 115.89, '山东': 117.0,
    '河南': 113.65, '湖北': 114.31, '湖南': 112.91, '广东': 113.23,
    '广西': 108.33, '海南': 110.35, '四川': 104.06, '贵州': 106.71,
    '云南': 102.73, '西藏': 91.11, '陕西': 108.95, '甘肃': 103.82,
    '青海': 101.74, '宁夏': 106.27, '新疆': 87.62
  };
  return lngMap[name] || 116.4;
};

// 获取省份的纬度（简化版）
const getProvinceLat = (name) => {
  const latMap = {
    '北京': 39.9, '天津': 39.13, '上海': 31.23, '重庆': 29.59,
    '河北': 38.03, '山西': 37.87, '内蒙古': 40.82, '辽宁': 41.8,
    '吉林': 43.88, '黑龙江': 45.75, '江苏': 32.04, '浙江': 30.26,
    '安徽': 31.86, '福建': 26.08, '江西': 28.68, '山东': 36.65,
    '河南': 34.76, '湖北': 30.52, '湖南': 28.21, '广东': 23.16,
    '广西': 22.84, '海南': 20.02, '四川': 30.67, '贵州': 26.57,
    '云南': 25.04, '西藏': 29.97, '陕西': 34.27, '甘肃': 36.06,
    '青海': 36.56, '宁夏': 38.47, '新疆': 43.82
  };
  return latMap[name] || 39.9;
};

// 根据数量获取颜色
const getColorByCount = (count) => {
  if (count >= 60) return '#a50f15';
  if (count >= 40) return '#de2d26';
  if (count >= 20) return '#fb6a4a';
  if (count >= 1) return '#fcae91';
  return '#f0f0f0';
};

const loadData = async () => {
  try {
    // 从后端获取统计数据
    const response = await getSummary();
    const data = response.data;

    stats.value = [
      { label: '贫困县总数', value: formatCount(data.totalCounties) },
      { label: '已脱贫县数', value: formatCount(data.delistedCounties) },
      { label: '扶贫覆盖率', value: formatPercent(data.coverageRate) },
      { label: '扶贫资金投入', value: formatCurrency(data.funding) },
    ];
    summaryRetries = 0;
  } catch (error) {
    console.error('Failed to load data:', error);
    if (error?.response?.status === 401 && summaryRetries < 3 && localStorage.getItem('token')) {
      summaryRetries += 1;
      setTimeout(loadData, 600);
      return;
    }
    stats.value = [
      { label: '贫困县总数', value: '832 个' },
      { label: '已脱贫县数', value: '780 个' },
      { label: '扶贫覆盖率', value: '93.8%' },
      { label: '扶贫资金投入', value: '156.8 亿元' },
    ];
  }
};

onMounted(() => {
  // 等待DOM完全渲染后初始化地图
  setTimeout(() => {
    initChart();
  }, 100);
  
  // 加载统计数据
  loadData();
});

onUnmounted(() => {
  if (myChart) {
    myChart.dispose();
    myChart = null;
  }
});
</script>

<style scoped>
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
  height: 600px;
  position: relative;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
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
  
  .map-container {
    height: 400px;
    padding: 15px;
  }
}
</style>