<template>
  <div class="home">
    <header class="header">
      <div class="nav-container">
        <div class="logo">832工程贫困县可视化分析系统</div>
        <nav>
          <ul class="nav-menu">
            <li><router-link to="/home" class="nav-link active">首页</router-link></li>
            <li><router-link to="/county" class="nav-link">县详情</router-link></li>
            <li><router-link to="/analysis" class="nav-link">数据分析</router-link></li>
            <li><router-link to="/alerts" class="nav-link">警报管理</router-link></li>
            <li><router-link to="/login" class="nav-link">登录/注册</router-link></li>
            <li><router-link to="/users" class="nav-link">用户管理</router-link></li>
          </ul>
        </nav>
      </div>
    </header>

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
        <div class="map-placeholder">
          🗺️ 交互式地图加载中...<br />
          <small>显示全国832个贫困县分布情况</small>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getChartsData } from '@/api';

const stats = ref([
  { label: '贫困县总数', value: '加载中...' },
  { label: '已脱贫县数', value: '加载中...' },
  { label: '扶贫覆盖率', value: '加载中...' },
  { label: '扶贫资金投入（元）', value: '加载中...' },
]);

onMounted(async () => {
  try {
    const response = await getChartsData();
    const data = response.data;

    stats.value = [
      { label: '贫困县总数', value: data.totalCounties || 'N/A' },
      { label: '已脱贫县数', value: data.delistedCounties || 'N/A' },
      { label: '扶贫覆盖率', value: `${data.coverageRate || 'N/A'}%` },
      { label: '扶贫资金投入（元）', value: `${data.funding || 'N/A'}亿` },
    ];
  } catch (error) {
    console.error('Failed to load stats:', error);
  }
});
</script>

<style scoped>
/* 从 example.html 提取的样式 */
.header {
  background: linear-gradient(135deg, #2c5282 0%, #3182ce 100%);
  color: white;
  padding: 20px 0;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-menu {
  display: flex;
  gap: 30px;
  list-style: none;
}

.nav-menu a {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  transition: all 0.3s ease;
  padding: 8px 12px;
  border-radius: 20px;
  font-weight: 500;
}

.nav-menu a:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.welcome-section {
  background: linear-gradient(
    135deg,
    rgba(44, 82, 130, 0.85) 0%,
    rgba(49, 130, 206, 0.9) 100%
  );
  color: white;
  padding: 50px 40px;
  border-radius: 16px;
  margin-bottom: 40px;
  text-align: center;
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
  text-align: center;
}

.map-container {
  background: rgba(255, 255, 255, 0.7);
  padding: 30px;
  border-radius: 16px;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>