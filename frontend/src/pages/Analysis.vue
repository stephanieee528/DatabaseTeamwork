<template>
  <div class="analysis-page">
    <div class="header">
      <h2>📊 数据分析中心</h2>
      <p>多维度洞察县域脱贫与经济发展成效</p>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="选择省份">
          <el-select v-model="filterForm.provinceId" placeholder="全部省份" clearable filterable>
            <el-option label="全部省份" value="" />
            <el-option
              v-for="province in provinces"
              :key="province.provinceId"
              :label="province.provinceName"
              :value="province.provinceId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="选择年份">
          <el-select v-model="filterForm.year">
            <el-option v-for="item in yearOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="searchData">
            {{ loading ? '分析中...' : '开始分析' }}
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="isAnalyst" class="advanced-panel">
      <el-card shadow="hover" class="advanced-card">
        <template #header>
          <div class="card-header">
            <span>高级筛选（分析师/管理员）</span>
            <el-button link type="primary" @click="resetAdvancedFilters">清空条件</el-button>
          </div>
        </template>
        <el-form :inline="true" :model="advancedFilters" class="advanced-form">
          <el-form-item label="关键词">
            <el-input v-model="advancedFilters.keyword" placeholder="县名/省份" clearable />
          </el-form-item>
          <el-form-item label="GDP下限(亿元)">
            <el-input-number v-model="advancedFilters.minGDP" :min="0" :precision="1" :step="1" />
          </el-form-item>
          <el-form-item label="贫困率上限(%)">
            <el-input-number v-model="advancedFilters.maxPoverty" :min="0" :max="100" :precision="1" :step="0.5" />
          </el-form-item>
          <el-form-item label="农村收入下限(元)">
            <el-input-number v-model="advancedFilters.minIncome" :min="0" :step="500" />
          </el-form-item>
        </el-form>
        <p class="advanced-tip">高级筛选仅影响本地视图展示，不会修改底层数据。</p>
      </el-card>
    </div>

    <div class="stats-grid" v-if="filteredTableData.length">
      <div class="stat-card">
        <div class="stat-number">{{ stats.totalCounties }}</div>
        <div class="stat-label">样本县区</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.avgPovertyRate }}%</div>
        <div class="stat-label">平均贫困率</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.avgGDP }} 亿元</div>
        <div class="stat-label">平均GDP</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.avgIncome }} 元</div>
        <div class="stat-label">平均农村收入</div>
      </div>
    </div>

    <div class="insight-section" v-if="hasInsights">
      <div class="chart-grid">
        <el-card shadow="hover" class="chart-card">
          <div class="card-title">多维度对比</div>
          <div ref="comparisonChartRef" class="chart-box">
            <el-empty v-if="!insights.countyComparison" description="暂无数据" />
          </div>
        </el-card>

        <el-card shadow="hover" class="chart-card">
          <div class="card-title">时间趋势分析</div>
          <div ref="trendChartRef" class="chart-box">
            <el-empty v-if="!insights.trendSeries" description="暂无数据" />
          </div>
        </el-card>

        <el-card shadow="hover" class="chart-card">
          <div class="card-title">政策效果评估</div>
          <div class="policy-wrapper">
            <div ref="policyChartRef" class="chart-box policy-chart">
              <el-empty v-if="!insights.policyEffect" description="暂无数据" />
            </div>
            <ul class="metric-list" v-if="insights.policyEffect">
              <li v-for="item in insights.policyEffect.metrics" :key="item.metric">
                <span>{{ item.metric }}</span>
                <strong>{{ item.before }} → {{ item.after }}</strong>
                <em :class="{ up: (item.change || 0) > 0, down: (item.change || 0) < 0 }">
                  {{ item.change > 0 ? '+' : '' }}{{ item.change || 0 }}
                </em>
              </li>
            </ul>
          </div>
        </el-card>

        <el-card shadow="hover" class="chart-card heatmap-card">
          <div class="card-title">地理分布热力图</div>
          <div ref="heatmapChartRef" class="chart-box">
            <el-empty v-if="!insights.heatmap || !insights.heatmap.length" description="暂无数据" />
          </div>
        </el-card>

        <el-card shadow="hover" class="chart-card">
          <div class="card-title">相关性分析</div>
          <div ref="correlationChartRef" class="chart-box">
            <el-empty v-if="!insights.correlation || !insights.correlation.length" description="暂无数据" />
          </div>
        </el-card>
      </div>
    </div>

    <div class="data-table">
      <h3>经济指标数据</h3>
      <div class="table-actions">
        <el-button
          type="primary"
          :disabled="!canExportData || filteredTableData.length === 0"
          @click="exportData"
        >
          导出数据
        </el-button>
        <span v-if="!canExportData" class="export-hint">（导出功能仅限分析师/管理员）</span>
        <span class="data-count">当前 {{ filteredTableData.length }} 条记录</span>
      </div>

      <el-table
        :data="paginatedData"
        stripe
        border
        v-loading="loading"
        empty-text="暂无数据，先发起分析"
      >
        <el-table-column prop="countyName" label="县名称" width="160" fixed />
        <el-table-column prop="provinceName" label="所属省份" width="140" />
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column label="GDP(亿元)" width="120">
          <template #default="scope">{{ formatNumber(scope.row.gdp, 1) }}</template>
        </el-table-column>
        <el-table-column label="贫困率(%)" width="120">
          <template #default="scope">{{ formatNumber(scope.row.povertyRate, 1) }}</template>
        </el-table-column>
        <el-table-column label="农村收入(元)" width="150">
          <template #default="scope">{{ formatNumber(scope.row.ruralDisposableIncome, 0) }}</template>
        </el-table-column>
        <el-table-column label="人均GDP(元)" width="150">
          <template #default="scope">{{ formatNumber(scope.row.gdpPerCapita, 0) }}</template>
        </el-table-column>
        <el-table-column label="财政收入(亿元)" width="150">
          <template #default="scope">{{ formatNumber(scope.row.fiscalRevenue, 1) }}</template>
        </el-table-column>
        <el-table-column label="GDP增速(%)" width="130">
          <template #default="scope">{{ formatPercent(scope.row.gdpYoy) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="tableData.length">
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

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import axios from 'axios';
import { getCounties, getCountyIndicators, getAnalysisData, getProvinces } from '@/api';

const filterForm = ref({
  provinceId: '',
  year: `2023`,
});
const yearOptions = ['2023', '2022', '2021', '2020', '2019', '2018'];

const loading = ref(false);
const tableData = ref<any[]>([]);
const baseTableData = ref<any[]>([]);
const provinces = ref<any[]>([]);
const counties = ref<any[]>([]);
const currentPage = ref(1);
const pageSize = 10;
const userRole = ref<string>('');
const isAnalyst = computed(() => ['数据分析师', '管理员'].includes(userRole.value));
const isAdmin = computed(() => userRole.value === '管理员');
const canExportData = computed(() => ['数据分析师', '管理员'].includes(userRole.value));

const readCachedRole = (): string => {
  const cached = localStorage.getItem('currentUser');
  if (!cached) return '';
  try {
    const parsed = JSON.parse(cached);
    return parsed.role || parsed.roleName || '';
  } catch {
    return '';
  }
};

const syncUserRole = () => {
  userRole.value = readCachedRole();
};

const handleStorageRoleChange = (event: StorageEvent) => {
  if (event.key === 'currentUser') {
    syncUserRole();
  }
};

const stats = reactive({
  totalCounties: 0,
  avgPovertyRate: 0,
  avgGDP: 0,
  avgIncome: 0,
});

const advancedFilters = reactive({
  keyword: '',
  minGDP: undefined as number | undefined,
  maxPoverty: undefined as number | undefined,
  minIncome: undefined as number | undefined,
});

const updateStats = (data: any[]) => {
  if (!data.length) {
    stats.totalCounties = 0;
    stats.avgPovertyRate = 0;
    stats.avgGDP = 0;
    stats.avgIncome = 0;
    return;
  }
  stats.totalCounties = data.length;
  const sum = (arr: number[]) => arr.reduce((acc, cur) => acc + cur, 0);
  const povertyList = data.filter((item) => item.povertyRate != null).map((item) => Number(item.povertyRate));
  const gdpList = data.filter((item) => item.gdp != null).map((item) => Number(item.gdp));
  const incomeList = data.filter((item) => item.ruralDisposableIncome != null).map((item) => Number(item.ruralDisposableIncome));
  stats.avgPovertyRate = povertyList.length ? (sum(povertyList) / povertyList.length).toFixed(1) : 0;
  stats.avgGDP = gdpList.length ? (sum(gdpList) / gdpList.length).toFixed(1) : 0;
  stats.avgIncome = incomeList.length ? Math.round(sum(incomeList) / incomeList.length) : 0;
};

const insights = reactive({
  countyComparison: null as any,
  trendSeries: null as any,
  heatmap: [] as any[],
  policyEffect: null as any,
  correlation: [] as any[],
});

const hasInsights = computed(
  () =>
    Boolean(
      insights.countyComparison ||
        insights.trendSeries ||
        insights.policyEffect ||
        (insights.heatmap && insights.heatmap.length) ||
        (insights.correlation && insights.correlation.length)
    )
);

const filteredTableData = computed(() => {
  if (!isAnalyst.value) {
    return tableData.value;
  }
  const hasNumber = (value: number | undefined): value is number => typeof value === 'number' && !Number.isNaN(value);
  return tableData.value.filter((item) => {
    const keyword = advancedFilters.keyword?.trim();
    if (keyword && !(item.countyName?.includes(keyword) || item.provinceName?.includes(keyword))) {
      return false;
    }
    if (hasNumber(advancedFilters.minGDP) && toNumber(item.gdp) < advancedFilters.minGDP) {
      return false;
    }
    if (hasNumber(advancedFilters.maxPoverty) && toNumber(item.povertyRate) > advancedFilters.maxPoverty) {
      return false;
    }
    if (hasNumber(advancedFilters.minIncome) && toNumber(item.ruralDisposableIncome) < advancedFilters.minIncome) {
      return false;
    }
    return true;
  });
});

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredTableData.value.slice(start, start + pageSize);
});

watch(filteredTableData, (val) => {
  updateStats(val);
  currentPage.value = 1;
}, { immediate: true });

watch(
  () => filterForm.value.provinceId,
  () => {
    refreshTableByProvince();
  },
);

const comparisonChartRef = ref<HTMLDivElement | null>(null);
const trendChartRef = ref<HTMLDivElement | null>(null);
const policyChartRef = ref<HTMLDivElement | null>(null);
const correlationChartRef = ref<HTMLDivElement | null>(null);
const heatmapChartRef = ref<HTMLDivElement | null>(null);

let charts: Record<string, echarts.ECharts | null> = {
  comparison: null,
  trend: null,
  policy: null,
  correlation: null,
  heatmap: null,
};

let geoJsonLoaded = false;

const initCharts = () => {
  comparisonChartRef.value && (charts.comparison = echarts.init(comparisonChartRef.value));
  trendChartRef.value && (charts.trend = echarts.init(trendChartRef.value));
  policyChartRef.value && (charts.policy = echarts.init(policyChartRef.value));
  correlationChartRef.value && (charts.correlation = echarts.init(correlationChartRef.value));
  heatmapChartRef.value && (charts.heatmap = echarts.init(heatmapChartRef.value));
};

const ensureGeoJson = async () => {
  if (geoJsonLoaded) return;
  try {
    const resp = await axios.get('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json');
    echarts.registerMap('china', resp.data);
    geoJsonLoaded = true;
  } catch (error) {
    const paths = [
      '/china.geojson',
      '/assets/china.geojson',
      new URL('../assets/china.geojson', import.meta.url).href,
    ];
    for (const path of paths) {
      try {
        const res = await fetch(path);
        if (res.ok) {
          const data = await res.json();
          echarts.registerMap('china', data);
          geoJsonLoaded = true;
          break;
        }
      } catch {
        // ignore
      }
    }
  }
};

const formatNumber = (value: number | null | undefined, digits = 0) => {
  if (value === null || value === undefined) return '-';
  return Number(value).toFixed(digits);
};

const formatPercent = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '-';
  return (Number(value) * 100).toFixed(1);
};

const toNumber = (value: number | string | null | undefined) => {
  if (value === null || value === undefined) return 0;
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

const average = (values: Array<number | string | null | undefined>) => {
  const filtered = values
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item));
  if (!filtered.length) return 0;
  return filtered.reduce((sum, current) => sum + current, 0) / filtered.length;
};

const roundNumeric = (value: number | string | null | undefined, digits = 1) => {
  const num = Number(value);
  if (!Number.isFinite(num)) return 0;
  const factor = Math.pow(10, digits);
  return Math.round(num * factor) / factor;
};

const normalizeScore = (value: number | string | null | undefined, min: number, max: number) => {
  const num = toNumber(value);
  if (max <= min) return 0;
  const clamped = Math.min(max, Math.max(min, num));
  return (clamped - min) / (max - min);
};

const buildComparisonFallback = (records: any[]) => {
  const sorted = [...records].sort((a, b) => toNumber(b.gdp) - toNumber(a.gdp)).slice(0, 8);
  return {
    counties: sorted.map((item) => item.countyName || '未知县'),
    gdp: sorted.map((item) => roundNumeric(item.gdp, 1)),
    income: sorted.map((item) => roundNumeric(item.ruralDisposableIncome, 0)),
    fiscal: sorted.map((item) => roundNumeric(item.fiscalRevenue, 1)),
  };
};

const buildTrendFallback = (records: any[], targetYear: number) => {
  const currentAvg = {
    gdp: average(records.map((item) => item.gdp)),
    income: average(records.map((item) => item.ruralDisposableIncome)),
    poverty: average(records.map((item) => item.povertyRate)),
    gdpYoy: average(records.map((item) => item.gdpYoy)),
  };
  const years: number[] = [];
  for (let offset = 4; offset >= 0; offset--) {
    years.push(targetYear - offset);
  }
  const gdpGrowth = currentAvg.gdpYoy || 0.05;
  const incomeGrowth = 0.06;
  const povertyDrift = 0.8;
  const gdpSeries = years.map((year) => {
    const steps = targetYear - year;
    const base = currentAvg.gdp || 0;
    const value = base / Math.pow(1 + gdpGrowth, steps);
    return roundNumeric(value, 1);
  });
  const incomeSeries = years.map((year) => {
    const steps = targetYear - year;
    const base = currentAvg.income || 0;
    const value = base / Math.pow(1 + incomeGrowth, steps);
    return roundNumeric(value, 0);
  });
  const povertySeries = years.map((year) => {
    const steps = targetYear - year;
    const base = currentAvg.poverty || 0;
    const value = Math.min(100, Math.max(0, base + steps * povertyDrift));
    return roundNumeric(value, 1);
  });
  return {
    years,
    povertyRate: povertySeries,
    income: incomeSeries,
    gdp: gdpSeries,
  };
};

const buildPolicyFallback = (trend: { years: number[]; povertyRate: number[]; income: number[]; gdp: number[] } | null) => {
  if (!trend || !trend.years.length) {
    return null;
  }
  const baselineIndex = 0;
  const currentIndex = trend.years.length - 1;
  const metrics = [
    {
      metric: '贫困率(%)',
      before: trend.povertyRate[baselineIndex],
      after: trend.povertyRate[currentIndex],
    },
    {
      metric: '人均收入(元)',
      before: trend.income[baselineIndex],
      after: trend.income[currentIndex],
    },
    {
      metric: 'GDP(亿元)',
      before: trend.gdp[baselineIndex],
      after: trend.gdp[currentIndex],
    },
  ].map((item) => ({
    ...item,
    change:
      item.before !== undefined && item.after !== undefined
        ? roundNumeric(item.after - item.before, 1)
        : null,
  }));
  return {
    baselineYear: trend.years[baselineIndex],
    currentYear: trend.years[currentIndex],
    metrics,
  };
};

const buildHeatmapFallback = (records: any[]) => {
  const grouped: Record<string, number[]> = {};
  records.forEach((item) => {
    if (item.povertyRate === null || item.povertyRate === undefined) return;
    const province = item.provinceName || '未知省份';
    grouped[province] = grouped[province] || [];
    grouped[province].push(toNumber(item.povertyRate));
  });
  return Object.entries(grouped).map(([province, rates]) => ({
    name: province,
    value: roundNumeric(average(rates), 1),
    industry: '主导产业数据待补充',
  }));
};

const buildCorrelationFallback = (records: any[]) => {
  const points: any[] = [];
  records.forEach((item) => {
    if (item.povertyRate === null || item.povertyRate === undefined) return;
    const poverty = roundNumeric(item.povertyRate, 1);
    const education = roundNumeric(normalizeScore(item.ruralDisposableIncome, 6000, 20000) * 100, 1);
    const industry = roundNumeric(normalizeScore(item.gdpPerCapita, 16000, 60000) * 100, 1);
    const infrastructure = roundNumeric(normalizeScore(item.fiscalRevenue, 2, 25) * 100, 1);
    points.push({ factor: '教育发展', score: education, povertyRate: poverty });
    points.push({ factor: '产业活力', score: industry, povertyRate: poverty });
    points.push({ factor: '基础设施', score: infrastructure, povertyRate: poverty });
  });
  return points.slice(0, 300);
};

const buildFallbackInsights = (records: any[], targetYear: number) => {
  const comparison = buildComparisonFallback(records);
  const trend = buildTrendFallback(records, targetYear);
  return {
    countyComparison: comparison,
    trendSeries: trend,
    policyEffect: buildPolicyFallback(trend),
    heatmap: buildHeatmapFallback(records),
    correlation: buildCorrelationFallback(records),
  };
};

const ensureInsightsPayload = (payload: any, records: any[], targetYear: number) => {
  const fallback = buildFallbackInsights(records, targetYear);
  return {
    countyComparison: payload?.countyComparison || fallback.countyComparison,
    trendSeries: payload?.trendSeries || fallback.trendSeries,
    policyEffect: payload?.policyEffect || fallback.policyEffect,
    heatmap: payload?.heatmap?.length ? payload.heatmap : fallback.heatmap,
    correlation: payload?.correlation?.length ? payload.correlation : fallback.correlation,
  };
};

const applyInsights = async (payload: any) => {
  insights.countyComparison = payload?.countyComparison || null;
  insights.trendSeries = payload?.trendSeries || null;
  insights.heatmap = payload?.heatmap || [];
  insights.policyEffect = payload?.policyEffect || null;
  insights.correlation = payload?.correlation || [];
  await nextTick();
  renderAllCharts();
};

const renderAllCharts = async () => {
  if (!charts.comparison) initCharts();
  renderComparisonChart();
  renderTrendChart();
  renderPolicyChart();
  renderCorrelationChart();
  await renderHeatmapChart();
};

const renderComparisonChart = () => {
  if (!charts.comparison || !insights.countyComparison) return;
  const { counties, gdp, income, fiscal } = insights.countyComparison;
  charts.comparison.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['GDP', '农村收入', '财政收入'] },
    grid: { left: 40, right: 20, top: 40, bottom: 40 },
    xAxis: {
      type: 'category',
      data: counties,
      axisLabel: { interval: 0, rotate: counties.length > 6 ? 30 : 0 },
    },
    yAxis: { type: 'value', name: '指标值' },
    series: [
      { name: 'GDP', type: 'bar', data: gdp },
      { name: '农村收入', type: 'bar', data: income },
      { name: '财政收入', type: 'bar', data: fiscal },
    ],
  });
};

const renderTrendChart = () => {
  if (!charts.trend || !insights.trendSeries) return;
  const { years, povertyRate, income, gdp } = insights.trendSeries;
  charts.trend.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['贫困率', '农村收入', 'GDP'] },
    grid: { left: 40, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: years },
    yAxis: [
      { type: 'value', name: '贫困率(%)' },
      { type: 'value', name: '经济指标', position: 'right' },
    ],
    series: [
      { name: '贫困率', type: 'line', smooth: true, data: povertyRate },
      { name: '农村收入', type: 'line', smooth: true, yAxisIndex: 1, data: income },
      { name: 'GDP', type: 'line', smooth: true, yAxisIndex: 1, data: gdp },
    ],
  });
};

const renderPolicyChart = () => {
  if (!charts.policy || !insights.policyEffect) return;
  const metrics = insights.policyEffect.metrics || [];
  charts.policy.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['基准年', '当前年'] },
    grid: { left: 30, right: 20, top: 30, bottom: 10 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: metrics.map((item: any) => item.metric) },
    series: [
      { name: '基准年', type: 'bar', data: metrics.map((item: any) => item.before) },
      { name: '当前年', type: 'bar', data: metrics.map((item: any) => item.after) },
    ],
  });
};

const renderCorrelationChart = () => {
  if (!charts.correlation || !insights.correlation || !insights.correlation.length) return;
  const grouped: Record<string, number[][]> = {};
  insights.correlation.forEach((point: any) => {
    grouped[point.factor] = grouped[point.factor] || [];
    grouped[point.factor].push([point.score, point.povertyRate]);
  });
  charts.correlation.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) =>
        `${params.seriesName}<br/>指数：${params.value[0]}<br/>贫困率：${params.value[1]}%`,
    },
    legend: { top: 0 },
    xAxis: { type: 'value', name: '指数', min: 0, max: 100 },
    yAxis: { type: 'value', name: '贫困率(%)' },
    series: Object.entries(grouped).map(([name, data]) => ({
      name,
      type: 'scatter',
      data,
    })),
  });
};

const renderHeatmapChart = async () => {
  if (!charts.heatmap || !insights.heatmap || !insights.heatmap.length) return;
  await ensureGeoJson();
  charts.heatmap.setOption({
    tooltip: {
      formatter: (params: any) =>
        `${params.name}<br/>平均贫困率：${params.value || '-'}%<br/>主导产业：${params.data?.industry || '-'}`,
    },
    visualMap: {
      min: 0,
      max: 20,
      left: 'left',
      bottom: 20,
      text: ['高', '低'],
      inRange: { color: ['#e6fffa', '#2c7a7b'] },
    },
    series: [
      {
        type: 'map',
        map: 'china',
        roam: true,
        data: insights.heatmap,
        emphasis: { label: { show: true } },
      },
    ],
  });
};

const loadProvinces = async () => {
  try {
    const res = await getProvinces();
    provinces.value = res.data || [];
  } catch (error) {
    console.warn('加载省份失败，使用默认列表', error);
    provinciasFallback();
  }
};

const provinciasFallback = () => {
  provinces.value = [
    { provinceId: 1, provinceName: '北京市' },
    { provinceId: 2, provinceName: '河北省' },
    { provinceId: 3, provinceName: '山西省' },
    { provinceId: 4, provinceName: '内蒙古自治区' },
  ];
};

const normalizeProvinceId = (value: unknown) => {
  if (value === '' || value === null || value === undefined) {
    return undefined;
  }
  const num = Number(value);
  return Number.isFinite(num) ? num : undefined;
};

const resolveSelectedProvince = () => {
  const normalized = normalizeProvinceId(filterForm.value.provinceId);
  if (normalized === undefined) {
    return null;
  }
  return provinces.value.find((item) => Number(item.provinceId) === normalized) || null;
};

const applyProvinceFilterToData = (records: any[]) => {
  const selectedProvince = resolveSelectedProvince();
  if (!selectedProvince) {
    return records.slice();
  }
  const targetId = Number(selectedProvince.provinceId);
  const targetNames = new Set(
    [selectedProvince.provinceName, selectedProvince.shortName, selectedProvince.alias]
      .filter((name): name is string => Boolean(name))
      .map((name) => name.trim()),
  );
  return records.filter((item) => {
    if (item.provinceId && Number(item.provinceId) === targetId) {
      return true;
    }
    const provinceName = (item.provinceName || item.province || '').trim();
    return provinceName ? targetNames.has(provinceName) : false;
  });
};

const refreshTableByProvince = () => {
  tableData.value = applyProvinceFilterToData(baseTableData.value);
};

const formatAnalysisTable = (source: any[], year: number) =>
  source.map((item, index) => ({
    id: item.county ? `${item.county}-${item.year || index}` : `${index}`,
    countyName: item.county || '未知县',
    provinceName: item.province || '未知省份',
    year: item.year || year,
    gdp: item.gdp ?? null,
    povertyRate: item.poverty ?? null,
    ruralDisposableIncome: item.income ?? null,
    gdpPerCapita: item.gdpPerCapita ?? null,
    fiscalRevenue: item.fiscalRevenue ?? null,
    gdpYoy: item.gdpYoy ?? null,
  }));

const fetchCountyIndicators = async (year: number) => {
  const params = filterForm.value.provinceId ? { provinceId: parseInt(filterForm.value.provinceId) } : {};
  const res = await getCounties(params);
  counties.value = res.data || [];
  const results: any[] = [];
  for (const county of counties.value) {
    try {
      const indicatorRes = await getCountyIndicators(county.countyId, year, year);
      const indicator = indicatorRes.data?.[0];
      if (indicator) {
        results.push({
          id: county.countyId,
          countyName: county.countyName,
          provinceName: county.province?.provinceName || '未知省份',
          year: indicator.year,
          gdp: indicator.gdp,
          povertyRate: indicator.povertyRate,
          ruralDisposableIncome: indicator.ruralDisposableIncome,
          gdpPerCapita: indicator.gdpPerCapita,
          fiscalRevenue: indicator.fiscalRevenue,
          gdpYoy: indicator.gdpYoy,
        });
      }
    } catch (error) {
      console.warn(`获取 ${county.countyName} 数据失败`, error);
    }
  }
  return results;
};

const searchData = async () => {
  if (!filterForm.value.year) {
    ElMessage.warning('请选择年份');
    return;
  }
  loading.value = true;
  tableData.value = [];
  currentPage.value = 1;
  try {
    const year = parseInt(filterForm.value.year);
    let formattedData: any[] = [];
    let analysisPayload: any = null;
    let usedFallback = false;
    try {
      const analysisResp = await getAnalysisData(year);
      analysisPayload = analysisResp.data;
      if (analysisPayload?.gdpPovertyRelation?.length) {
        formattedData = formatAnalysisTable(analysisPayload.gdpPovertyRelation, year);
      }
    } catch (error) {
      usedFallback = true;
      console.warn('分析数据接口不可用', error);
    }
    if (!formattedData.length) {
      formattedData = await fetchCountyIndicators(year);
    }
    const safeInsights = ensureInsightsPayload(analysisPayload, formattedData, year);
    await applyInsights(safeInsights);
    baseTableData.value = formattedData;
    refreshTableByProvince();
    updateStats(tableData.value);
    ElMessage.success(`搜索到 ${tableData.value.length} 条数据`);
    if (usedFallback) {
      ElMessage.warning('分析服务暂不可用，已使用本地数据计算结果');
    }
  } catch (error) {
    console.error('搜索失败', error);
    ElMessage.error('分析失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const resetSearch = () => {
  filterForm.value = {
    provinceId: '',
    year: `${new Date().getFullYear() - 1}`,
  };
  baseTableData.value = [];
  tableData.value = [];
  currentPage.value = 1;
  applyInsights(null);
};

const resetAdvancedFilters = () => {
  advancedFilters.keyword = '';
  advancedFilters.minGDP = undefined;
  advancedFilters.maxPoverty = undefined;
  advancedFilters.minIncome = undefined;
};

const exportData = () => {
  if (!canExportData.value) {
    ElMessage.warning('当前角色无导出权限');
    return;
  }
  if (!tableData.value.length) return;
  const headers = ['县名称', '所属省份', '年份', 'GDP(亿元)', '贫困率(%)', '农村收入(元)', '人均GDP(元)', '财政收入(亿元)', 'GDP增长率(%)'];
  const rows = tableData.value.map((item) => [
    item.countyName,
    item.provinceName,
    item.year,
    item.gdp ?? '-',
    item.povertyRate ?? '-',
    item.ruralDisposableIncome ?? '-',
    item.gdpPerCapita ?? '-',
    item.fiscalRevenue ?? '-',
    item.gdpYoy ?? '-',
  ]);
  let content = 'data:text/csv;charset=utf-8,\uFEFF' + headers.join(',') + '\n';
  rows.forEach((row) => {
    content += row.join(',') + '\n';
  });
  const link = document.createElement('a');
  link.href = encodeURI(content);
  link.download = `经济指标数据_${filterForm.value.year}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ElMessage.success('数据导出成功');
};

const handleResize = () => {
  Object.values(charts).forEach((chart) => chart?.resize());
};

onMounted(async () => {
  syncUserRole();
  await loadProvinces();
  await searchData();
  window.addEventListener('resize', handleResize);
  window.addEventListener('storage', handleStorageRoleChange);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('storage', handleStorageRoleChange);
  Object.keys(charts).forEach((key) => {
    charts[key]?.dispose();
    charts[key] = null;
  });
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
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.85);
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
}

.header h2 {
  font-size: 30px;
  color: #2c5282;
  margin-bottom: 8px;
}

.filter-bar {
  background: rgba(255, 255, 255, 0.85);
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.advanced-panel {
  margin-bottom: 20px;
}

.advanced-card {
  background: rgba(255, 255, 255, 0.92);
}

.advanced-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.advanced-tip {
  font-size: 12px;
  color: #718096;
  margin-top: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stat-number {
  font-size: 30px;
  font-weight: 700;
  color: #2b6cb0;
}

.stat-label {
  margin-top: 8px;
  color: #4a5568;
}

.insight-section {
  margin-bottom: 20px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 20px;
}

.chart-card {
  min-height: 320px;
}

.chart-box {
  height: 280px;
}

.policy-wrapper {
  display: flex;
  gap: 16px;
}

.policy-chart {
  flex: 2;
}

.metric-list {
  flex: 1;
  list-style: none;
  padding: 0;
  margin: 0;
}

.metric-list li {
  background: #f7fafc;
  padding: 12px;
  border-radius: 10px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
}

.metric-list em {
  font-style: normal;
  font-size: 12px;
  color: #718096;
}

.metric-list em.up {
  color: #38a169;
}

.metric-list em.down {
  color: #e53e3e;
}

.data-table {
  background: rgba(255, 255, 255, 0.9);
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.table-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.export-hint {
  color: #a0aec0;
  font-size: 13px;
  margin-left: 8px;
}

.pagination {
  text-align: center;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .policy-wrapper {
    flex-direction: column;
  }

  .metric-list {
    width: 100%;
  }

  .advanced-form {
    flex-direction: column;
  }
}
</style>

