<template>
  <div class="county-page">
    <div class="county-header">
      <div class="title-block">
        <h2>{{ countyInfo.countyName || '📍 县详情' }}</h2>
        <p class="subtitle">
          {{ countyInfo.province || '加载中' }} · 贫困等级：{{ countyInfo.povertyLevel || '待维护' }}
        </p>
        <div class="province-chip">
          <span>当前省份分类：</span>
          <el-tag type="info" effect="plain" size="small">{{ selectedProvinceName }}</el-tag>
        </div>
      </div>
      <div class="header-actions">
        <el-select
          v-model="selectedProvinceId"
          placeholder="按省筛选"
          class="province-select"
          @change="handleProvinceChange"
        >
          <el-option :label="'全部省份'" :value="ALL_PROVINCE_VALUE" />
          <el-option
            v-for="prov in provinces"
            :key="prov.provinceId"
            :label="prov.provinceName"
            :value="prov.provinceId"
          />
        </el-select>
        <el-select
          v-model="selectedCountyId"
          filterable
          placeholder="选择县区"
          style="width: 220px"
          :disabled="!countyOptions.length"
          @change="handleCountyChange"
        >
          <el-option
            v-for="county in countyOptions"
            :key="county.countyId"
            :label="county.countyName"
            :value="county.countyId"
          />
        </el-select>
        <el-button @click="refreshData">刷新</el-button>
        <el-button v-if="canExportCounty" type="success" @click="exportCountyData">导出县数据</el-button>
        <el-button v-if="canEditCounty" type="primary" @click="openEditDialog">编辑基础信息</el-button>
      </div>
    </div>

    <el-row :gutter="20" class="info-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="card-title">基础信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="所属省份">{{ countyInfo.province || '-' }}</el-descriptions-item>
            <el-descriptions-item label="脱贫年份">{{ countyInfo.delistingYear || '持续推进中' }}</el-descriptions-item>
            <el-descriptions-item label="常住人口">{{ formatNumber(countyInfo.population, '万人') }}</el-descriptions-item>
            <el-descriptions-item label="面积">{{ formatNumber(countyInfo.area, '平方公里') }}</el-descriptions-item>
            <el-descriptions-item label="主导产业">{{ countyInfo.mainIndustries || '-' }}</el-descriptions-item>
            <el-descriptions-item label="教育支持">{{ countyInfo.educationSupport || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="card-title">扶贫项目概览</div>
          <div class="project-stats">
            <div class="stat-item">
              <div class="label">项目数量</div>
              <div class="value">{{ projectOverview.projectCount || 0 }} 个</div>
            </div>
            <div class="stat-item">
              <div class="label">总投资</div>
              <div class="value">{{ formatNumber(projectOverview.totalInvestment, '亿元') }}</div>
            </div>
            <div class="stat-item">
              <div class="label">覆盖人口</div>
              <div class="value">{{ formatInteger(projectOverview.coveragePopulation, '人') }}</div>
            </div>
            <div class="stat-item">
              <div class="label">受益群众</div>
              <div class="value">{{ formatInteger(projectOverview.totalBeneficiaries, '人') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="kpi-grid">
      <KpiCard v-for="card in kpiCards" :key="card.title" :title="card.title" :value="card.value" />
    </div>

    <el-card shadow="hover" class="chart-card">
      <div class="card-title">经济指标趋势</div>
      <div ref="indicatorChartRef" class="chart-canvas">
        <el-empty v-if="!indicatorTrend.length" description="暂无趋势数据" />
      </div>
    </el-card>

    <el-card shadow="hover" class="project-card">
      <div class="card-header">
        <div>
          <h3>扶贫项目管理</h3>
          <p>管理项目进度、资金与受益人群</p>
        </div>
        <el-button v-if="canManageProjects" type="primary" @click="openProjectDialog()">新增项目</el-button>
    </div>
      <el-table :data="projects" v-loading="detailLoading" stripe>
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column label="投资(亿元)" width="130">
          <template #default="{ row }">{{ formatNumber(row.investment, '亿') }}</template>
        </el-table-column>
        <el-table-column label="覆盖人口" width="120">
          <template #default="{ row }">{{ formatInteger(row.beneficiaries, '人') }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status || '未设置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="row.progress === 100 ? 'success' : undefined" />
          </template>
        </el-table-column>
        <el-table-column prop="leadUnit" label="牵头单位" width="160" />
        <el-table-column v-if="canManageProjects" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openProjectDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑县信息 -->
    <el-dialog v-model="editDialogVisible" title="编辑县基础信息" width="640px">
      <el-form :model="countyForm" label-width="120px">
        <el-form-item label="县名称">
          <el-input v-model="countyForm.countyName" />
        </el-form-item>
        <el-form-item label="所属省份">
          <el-select v-model="countyForm.provinceId" placeholder="请选择">
            <el-option v-for="item in provinces" :key="item.provinceId" :label="item.provinceName" :value="item.provinceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="常住人口(万人)">
          <el-input-number v-model="countyForm.population" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="面积(平方公里)">
          <el-input-number v-model="countyForm.area" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="贫困等级">
          <el-select v-model="countyForm.povertyLevel" placeholder="请选择">
            <el-option label="Ⅰ类重点县" value="Ⅰ类重点县" />
            <el-option label="Ⅱ类巩固县" value="Ⅱ类巩固县" />
            <el-option label="Ⅲ类监测县" value="Ⅲ类监测县" />
          </el-select>
        </el-form-item>
        <el-form-item label="脱贫成效(%)">
          <el-input-number v-model="countyForm.povertyAlleviationRate" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="经济增速(%)">
          <el-input-number v-model="countyForm.economicGrowthRate" :min="-20" :max="30" :precision="1" />
        </el-form-item>
        <el-form-item label="就业率(%)">
          <el-input-number v-model="countyForm.employmentRate" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="覆盖人口(人)">
          <el-input-number v-model="countyForm.coveragePopulation" :min="0" />
        </el-form-item>
        <el-form-item label="总投资(亿元)">
          <el-input-number v-model="countyForm.totalInvestment" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="主导产业">
          <el-input v-model="countyForm.mainIndustries" />
        </el-form-item>
        <el-form-item label="教育支持">
          <el-input v-model="countyForm.educationSupport" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCountyForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 项目信息 -->
    <el-dialog v-model="projectDialogVisible" :title="projectForm.projectId ? '编辑项目' : '新增项目'" width="600px">
      <el-form :model="projectForm" :rules="projectRules" ref="projectFormRef" label-width="110px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="projectForm.projectName" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="projectForm.category">
            <el-option v-for="item in projectCategories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="投资规模(亿元)" prop="investment">
          <el-input-number v-model="projectForm.investment" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="覆盖人口(人)" prop="beneficiaries">
          <el-input-number v-model="projectForm.beneficiaries" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="projectForm.status">
            <el-option v-for="item in projectStatuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="进度(%)" prop="progress">
          <el-input-number v-model="projectForm.progress" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="开始年份">
          <el-input-number v-model="projectForm.startYear" :min="2015" :max="2030" />
        </el-form-item>
        <el-form-item label="结束年份">
          <el-input-number v-model="projectForm.endYear" :min="2015" :max="2035" />
        </el-form-item>
        <el-form-item label="牵头单位">
          <el-input v-model="projectForm.leadUnit" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProjectForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';
import * as echarts from 'echarts';
import KpiCard from '@/components/KpiCard.vue';
import {
  getCountyDetail,
  getCounties,
  updateCounty,
  createCountyProject,
  updateCountyProject,
  deleteCountyProject,
  getProvinces,
} from '@/api';

const ALL_PROVINCE_VALUE = 0;
const route = useRoute();
const router = useRouter();

const selectedProvinceId = ref<number>(ALL_PROVINCE_VALUE);
const selectedCountyId = ref<number | null>(null);
const countyOptions = ref<any[]>([]);
const provinces = ref<any[]>([]);
const detailLoading = ref(false);
const userRole = ref<string>('');
const isAdmin = computed(() => userRole.value === '管理员');
const isAnalyst = computed(() => ['数据分析师', '管理员'].includes(userRole.value));
const canEditCounty = isAdmin;
const canManageProjects = isAdmin;
const canExportCounty = computed(() => isAnalyst.value);
const selectedProvinceName = computed(() => {
  if (selectedProvinceId.value === ALL_PROVINCE_VALUE) {
    return '全部省份';
  }
  const match = provinces.value.find(
    (item) => item.provinceId === selectedProvinceId.value,
  );
  return match?.provinceName || '全部省份';
});

const countyInfo = reactive<any>({});
const projectOverview = reactive({
  projectCount: 0,
  totalInvestment: 0,
  totalBeneficiaries: 0,
  coveragePopulation: 0,
});
const kpiCards = ref<{ title: string; value: string }[]>([
  { title: '脱贫成效', value: '加载中' },
  { title: '经济增速', value: '加载中' },
  { title: '就业率', value: '加载中' },
  { title: '贫困发生率', value: '加载中' },
]);
const indicatorTrend = ref<any[]>([]);
const projects = ref<any[]>([]);

const countyForm = reactive<any>({
  countyName: '',
  provinceId: null,
  population: null,
  area: null,
  povertyLevel: '',
  povertyAlleviationRate: null,
  economicGrowthRate: null,
  employmentRate: null,
  coveragePopulation: null,
  totalInvestment: null,
  mainIndustries: '',
  educationSupport: '',
});
const editDialogVisible = ref(false);

const projectFormRef = ref<FormInstance>();
const projectDialogVisible = ref(false);
const projectForm = reactive<any>({
  projectId: null,
  projectName: '',
  category: '产业扶贫',
  investment: 1,
  beneficiaries: 200,
  status: '进行中',
  progress: 60,
  startYear: 2021,
  endYear: 2023,
  leadUnit: '',
  description: '',
});
const projectCategories = ['产业扶贫', '教育扶智', '基础设施', '生态旅游', '数字乡村'];
const projectStatuses = ['进行中', '已完成', '筹备阶段'];
const projectRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  investment: [{ required: true, message: '请输入投资额', trigger: 'change' }],
  beneficiaries: [{ required: true, message: '请输入覆盖人口', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  progress: [{ type: 'number', min: 0, max: 100, message: '0-100之间', trigger: 'change' }],
};

const indicatorChartRef = ref<HTMLDivElement | null>(null);
let indicatorChart: echarts.ECharts | null = null;

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

const initChart = () => {
  if (indicatorChartRef.value) {
    indicatorChart = echarts.init(indicatorChartRef.value);
  }
};

const renderIndicatorChart = () => {
  if (!indicatorChartRef.value) return;
  if (!indicatorChart) {
    initChart();
    if (!indicatorChart) return;
  }
  if (!indicatorTrend.value.length) {
    indicatorChart.clear();
    return;
  }
  indicatorChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['GDP', '农村收入', '贫困率'], top: 10 },
    grid: { left: 40, right: 40, top: 60, bottom: 30 },
    xAxis: {
      type: 'category',
      data: indicatorTrend.value.map((item) => item.year),
    },
    yAxis: [
      { type: 'value', name: '亿元', axisLabel: { formatter: '{value}' } },
      { type: 'value', name: '贫困率(%)', position: 'right', axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      {
        name: 'GDP',
        type: 'line',
        smooth: true,
        data: indicatorTrend.value.map((item) => item.gdp ?? null),
      },
      {
        name: '农村收入',
        type: 'line',
        smooth: true,
        data: indicatorTrend.value.map((item) => item.ruralDisposableIncome ?? null),
      },
      {
        name: '贫困率',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: indicatorTrend.value.map((item) => item.povertyRate ?? null),
      },
    ],
  });
};

const formatNumber = (value: number | null | undefined, suffix = '') => {
  if (value === null || value === undefined) return '-';
  return `${Number(value).toLocaleString()}${suffix}`;
};

const formatInteger = (value: number | null | undefined, suffix = '') => {
  if (value === null || value === undefined) return '-';
  return `${Math.round(Number(value)).toLocaleString()}${suffix}`;
};

const statusType = (status: string) => {
  if (status === '已完成') return 'success';
  if (status === '进行中') return 'warning';
  return 'info';
};

const buildKpiCards = (kpis: any = {}) => [
  { title: '脱贫成效', value: formatNumber(kpis.povertyAlleviationRate, '%') },
  { title: '经济增速', value: formatNumber(kpis.economicGrowthRate, '%') },
  { title: '就业率', value: formatNumber(kpis.employmentRate, '%') },
  { title: '贫困率', value: kpis.povertyRate !== undefined ? formatNumber(kpis.povertyRate, '%') : '-' },
];

const getProvinceFilterValue = () => {
  if (selectedProvinceId.value === ALL_PROVINCE_VALUE) {
    return undefined;
  }
  const exists = provinces.value.some(
    (item) => item.provinceId === selectedProvinceId.value,
  );
  return exists ? selectedProvinceId.value : undefined;
};

const loadCountyList = async (options?: { preserveSelection?: boolean }) => {
  try {
    const provinceFilter = getProvinceFilterValue();
    const res = await getCounties(
      provinceFilter ? { provinceId: provinceFilter } : undefined,
    );
    countyOptions.value = res.data || [];
    if (!countyOptions.value.length) {
      selectedCountyId.value = null;
      return;
    }
    const shouldPreserve = options?.preserveSelection;
    if (
      shouldPreserve &&
      selectedCountyId.value != null &&
      countyOptions.value.some((item) => item.countyId === selectedCountyId.value)
    ) {
      return;
    }
    selectedCountyId.value = countyOptions.value[0].countyId;
  } catch (error) {
    console.error('加载县列表失败', error);
  }
};

const extractQueryValue = (value: unknown): string | undefined => {
  if (Array.isArray(value)) {
    return value.length ? String(value[0]) : undefined;
  }
  return typeof value === 'string' ? value : undefined;
};

const applyProvinceFromRoute = () => {
  const provinceIdRaw = extractQueryValue(route.query.provinceId);
  if (provinceIdRaw) {
    const parsed = Number(provinceIdRaw);
    if (
      !Number.isNaN(parsed) &&
      provinces.value.some((item) => item.provinceId === parsed)
    ) {
      selectedProvinceId.value = parsed;
      return;
    }
  }
  const provinceNameRaw = extractQueryValue(route.query.province);
  if (provinceNameRaw) {
    const match = provinces.value.find(
      (item) => item.provinceName === provinceNameRaw.trim(),
    );
    if (match?.provinceId) {
      selectedProvinceId.value = match.provinceId;
    }
  }
};

const syncProvinceQuery = () => {
  const query = { ...route.query };
  const provinceFilter = getProvinceFilterValue();
  if (provinceFilter) {
    query.provinceId = String(provinceFilter);
  } else {
    delete query.provinceId;
    delete query.province;
  }
  router.replace({ path: route.path, query });
};

const handleProvinceChange = async () => {
  await loadCountyList();
  syncProvinceQuery();
  if (!countyOptions.value.length) {
    ElMessage.info(`${selectedProvinceName.value}暂无县域数据`);
    return;
  }
  if (selectedCountyId.value != null) {
    await loadCountyDetail();
  }
};

const loadProvinces = async () => {
  try {
    const res = await getProvinces();
    provinces.value = res.data || [];
  } catch (error) {
    console.warn('加载省份失败，使用内置数据', error);
    provinces.value = [
      { provinceId: 1, provinceName: '云南省' },
      { provinceId: 2, provinceName: '贵州省' },
      { provinceId: 3, provinceName: '四川省' },
    ];
  }
};

const loadCountyDetail = async () => {
  if (selectedCountyId.value == null) return;
  detailLoading.value = true;
  try {
    const response = await getCountyDetail(selectedCountyId.value);
    const payload = response.data || {};
    Object.assign(countyInfo, payload.county || {});
    Object.assign(projectOverview, payload.projectOverview || {});
    indicatorTrend.value = payload.indicatorTrend || payload.indicators || [];
    projects.value = payload.projects || [];
    kpiCards.value = buildKpiCards(payload.kpis);
    nextTick(() => renderIndicatorChart());
  } catch (error) {
    console.error('加载县详情失败', error);
    ElMessage.error('县详情数据加载失败');
  } finally {
    detailLoading.value = false;
  }
};

const handleCountyChange = () => {
  if (selectedCountyId.value == null) return;
  loadCountyDetail();
};

const refreshData = () => {
  if (selectedCountyId.value == null) return;
  loadCountyDetail();
};

const openEditDialog = () => {
  if (selectedCountyId.value == null) {
    ElMessage.warning('请选择县区后再编辑');
    return;
  }
  if (!canEditCounty.value) {
    ElMessage.warning('当前角色无权编辑县基础信息');
    return;
  }
  Object.assign(countyForm, {
    countyName: countyInfo.countyName,
    provinceId: countyInfo.provinceId,
    population: countyInfo.population,
    area: countyInfo.area,
    povertyLevel: countyInfo.povertyLevel,
    povertyAlleviationRate: countyInfo.povertyAlleviationRate,
    economicGrowthRate: countyInfo.economicGrowthRate,
    employmentRate: countyInfo.employmentRate,
    coveragePopulation: countyInfo.coveragePopulation,
    totalInvestment: countyInfo.totalInvestment,
    mainIndustries: countyInfo.mainIndustries,
    educationSupport: countyInfo.educationSupport,
  });
  editDialogVisible.value = true;
};

const submitCountyForm = async () => {
  if (selectedCountyId.value == null) {
    ElMessage.warning('请选择县区后再保存');
    return;
  }
  try {
    await updateCounty(selectedCountyId.value, countyForm);
    ElMessage.success('县信息已更新');
    editDialogVisible.value = false;
    loadCountyDetail();
  } catch (error) {
    console.error('更新县信息失败', error);
    ElMessage.error('更新失败');
  }
};

const resetProjectForm = () => {
  Object.assign(projectForm, {
    projectId: null,
    projectName: '',
    category: '产业扶贫',
    investment: 1,
    beneficiaries: 200,
    status: '进行中',
    progress: 60,
    startYear: 2021,
    endYear: 2023,
    leadUnit: '',
    description: '',
  });
};

const openProjectDialog = (project?: any) => {
  if (selectedCountyId.value == null) {
    ElMessage.warning('请选择县区后再管理项目');
    return;
  }
  if (!canManageProjects.value) {
    ElMessage.warning('当前角色无权管理项目');
    return;
  }
  if (project) {
    Object.assign(projectForm, project);
  } else {
    resetProjectForm();
  }
  projectDialogVisible.value = true;
};

const submitProjectForm = () => {
  if (selectedCountyId.value == null) {
    ElMessage.warning('请选择县区后再保存项目');
    return;
  }
  if (!canManageProjects.value) {
    ElMessage.warning('当前角色无权保存项目');
    return;
  }
  projectFormRef.value?.validate(async (valid) => {
    if (!valid) return;
    try {
      const payload = { ...projectForm };
      if (projectForm.projectId) {
        await updateCountyProject(selectedCountyId.value, projectForm.projectId, payload);
        ElMessage.success('项目已更新');
      } else {
        await createCountyProject(selectedCountyId.value, payload);
        ElMessage.success('项目已创建');
      }
      projectDialogVisible.value = false;
      loadCountyDetail();
    } catch (error) {
      console.error('保存项目失败', error);
      ElMessage.error('项目保存失败');
    }
  });
};

const confirmDelete = (project: any) => {
  if (selectedCountyId.value == null) {
    ElMessage.warning('请选择县区后再删除项目');
    return;
  }
  if (!canManageProjects.value) {
    ElMessage.warning('当前角色无权删除项目');
    return;
  }
  ElMessageBox.confirm(`确认删除【${project.projectName}】?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCountyProject(selectedCountyId.value, project.projectId);
      ElMessage.success('已删除项目');
      loadCountyDetail();
    })
    .catch(() => null);
};

const handleResize = () => {
  indicatorChart?.resize();
};

const exportCountyData = () => {
  if (!canExportCounty.value) {
    ElMessage.warning('当前角色无权导出数据');
    return;
  }
  if (selectedCountyId.value == null) {
    ElMessage.warning('请选择县区后再导出');
    return;
  }
  const payload = {
    county: { ...countyInfo },
    projectOverview: { ...projectOverview },
    projects: projects.value,
    indicators: indicatorTrend.value,
  };
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = `${countyInfo.countyName || 'county'}_detail.json`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(link.href);
  ElMessage.success('县域数据导出成功');
};

watch(indicatorTrend, () => {
  nextTick(() => renderIndicatorChart());
});

onMounted(async () => {
  syncUserRole();
  await loadProvinces();
  applyProvinceFromRoute();
  syncProvinceQuery();
  await loadCountyList();
  if (selectedCountyId.value != null) {
    await loadCountyDetail();
  }
  window.addEventListener('resize', handleResize);
  window.addEventListener('storage', handleStorageRoleChange);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('storage', handleStorageRoleChange);
  if (indicatorChart) {
    indicatorChart.dispose();
    indicatorChart = null;
  }
});
</script>

<style scoped>
.county-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(120deg, #e6f7ff 0%, #f7fafc 100%);
}

.county-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.title-block h2 {
  margin: 0;
  font-size: 28px;
  color: #1a365d;
}

.subtitle {
  margin-top: 4px;
  color: #4a5568;
  font-size: 14px;
}

.province-chip {
  margin-top: 6px;
  font-size: 13px;
  color: #4a5568;
  display: flex;
  align-items: center;
  gap: 8px;
}

.province-select {
  width: 180px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.info-row {
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #2d3748;
}

.project-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.stat-item {
  background: #f7fafc;
  padding: 12px;
  border-radius: 10px;
}

.stat-item .label {
  color: #718096;
  font-size: 13px;
}

.stat-item .value {
  font-size: 20px;
  font-weight: 600;
  margin-top: 6px;
  color: #2b6cb0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.chart-card,
.project-card {
  margin-bottom: 20px;
}

.chart-canvas {
  height: 360px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #2d3748;
}

@media (max-width: 768px) {
  .county-header {
    flex-direction: column;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .project-stats {
    grid-template-columns: 1fr;
  }
}
</style>