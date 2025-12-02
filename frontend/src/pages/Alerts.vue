<template>
  <div class="alerts-page">
    <div class="hero">
      <div>
        <h2>⚠️ 警告中心</h2>
        <p>管理员可配置监测规则、触发数据扫描，并跟踪每一条警告的处理状态。</p>
        <div class="hero-stats">
          <span>当前规则：{{ rules.length }} 条</span>
          <span>待处理警告：{{ pendingCount }} 条</span>
        </div>
    </div>
      <div class="hero-actions">
        <el-button type="primary" :icon="Plus" @click="openRuleDialog()">新建规则</el-button>
        <el-button type="success" plain :icon="Refresh" :loading="scanning" @click="triggerScan">
          立即扫描
        </el-button>
      </div>
    </div>

    <div class="content-grid">
      <el-card shadow="hover" class="rule-card">
        <template #header>
          <div class="card-header">
            <div>
              <h3>规则配置</h3>
              <p>定义监测指标、阈值及持续周期，系统将自动巡检。</p>
            </div>
            <el-button type="primary" link :loading="loadingRules" @click="loadRules">刷新</el-button>
          </div>
        </template>
        <el-table
          :data="rules"
          v-loading="loadingRules"
          empty-text="暂无警告规则，点击右上角新建"
          border
        >
          <el-table-column prop="ruleName" label="规则名称" min-width="160" />
          <el-table-column label="监测指标" min-width="160">
            <template #default="{ row }">{{ formatMetric(row.metricKey) }}</template>
          </el-table-column>
          <el-table-column label="阈值条件" min-width="160">
            <template #default="{ row }">
              {{ formatComparator(row.comparator) }} {{ formatThreshold(row) }}
            </template>
          </el-table-column>
          <el-table-column label="持续年限" width="110">
            <template #default="{ row }">{{ row.durationYears || 1 }} 年</template>
          </el-table-column>
          <el-table-column label="启用" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                @change="(val) => toggleRuleStatus(row, Boolean(val))"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRuleDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="hover" class="event-card">
        <template #header>
          <div class="card-header">
            <div>
              <h3>警告事件</h3>
              <p>展示最新触发的警告，可直接标记处理完成。</p>
            </div>
            <el-button type="primary" link :loading="loadingEvents" @click="loadEvents">刷新</el-button>
          </div>
        </template>
        <el-table
          :data="events"
          v-loading="loadingEvents"
          border
          empty-text="暂无警告事件，尝试触发扫描"
        >
          <el-table-column label="规则" min-width="180">
            <template #default="{ row }">{{ row.rule?.ruleName || '-' }}</template>
          </el-table-column>
          <el-table-column label="县区" min-width="180">
            <template #default="{ row }">
              {{ row.county?.countyName || '-' }}
              <span class="province-label">{{ row.county?.province?.provinceName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="触发年份" width="120" prop="year" />
          <el-table-column label="指标值" min-width="140">
            <template #default="{ row }">{{ formatMetricValue(row) }}</template>
          </el-table-column>
          <el-table-column label="触发时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.triggeredAt) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.acknowledgedAt ? 'success' : 'danger'">
                {{ row.acknowledgedAt ? '已处理' : '待处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                type="success"
                link
                :disabled="Boolean(row.acknowledgedAt)"
                @click="resolveEvent(row)"
              >
                标记已处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog
      v-model="ruleDialogVisible"
      :title="ruleForm.ruleId ? '编辑规则' : '新建规则'"
      width="520px"
      @closed="resetRuleForm"
    >
      <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="ruleFormRules"
        label-width="100px"
      >
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="例如：GDP增速连续下降" />
        </el-form-item>
        <el-form-item label="监测指标" prop="metricKey">
          <el-select v-model="ruleForm.metricKey">
            <el-option
              v-for="item in metricOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="条件" prop="comparator">
          <el-select v-model="ruleForm.comparator">
            <el-option
              v-for="item in comparatorOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="threshold">
          <el-input-number
            v-model="ruleForm.threshold"
            :precision="metricPrecision(ruleForm.metricKey)"
            :step="metricStep(ruleForm.metricKey)"
          />
          <span class="unit">{{ metricUnits[ruleForm.metricKey] || '' }}</span>
        </el-form-item>
        <el-form-item label="持续年限" prop="durationYears">
          <el-input-number v-model="ruleForm.durationYears" :min="1" :max="5" />
          <span class="unit">年</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRule" @click="submitRule">
          {{ ruleForm.ruleId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getAlerts,
  resolveAlertById,
  getRules,
  createRule,
  updateRule,
  deleteRule,
  scanAlerts,
  getAlertScanStatus,
} from '@/api';
import { Plus, Refresh } from '@element-plus/icons-vue';

interface AlertRuleRecord {
  ruleId?: number;
  ruleName?: string;
  metricKey?: string;
  comparator?: string;
  threshold?: number;
  durationYears?: number;
  enabled?: boolean;
}

interface AlertEventRecord {
  eventId: number;
  rule?: AlertRuleRecord;
  county?: { countyName?: string; province?: { provinceName?: string } };
  year?: number;
  metricValue?: number;
  triggeredAt?: string;
  acknowledgedAt?: string;
}

const rules = ref<AlertRuleRecord[]>([]);
const events = ref<AlertEventRecord[]>([]);
const loadingRules = ref(false);
const loadingEvents = ref(false);
const savingRule = ref(false);
const scanning = ref(false);
const scanStatus = ref({
  state: 'IDLE',
  message: '等待触发',
  startedAt: null as string | null,
  finishedAt: null as string | null,
  durationMs: null as number | null,
});
const scanPollTimer = ref<number | null>(null);
const SCAN_POLL_INTERVAL = 2000;

const pendingCount = computed(() => events.value.filter((item) => !item.acknowledgedAt).length);

const ruleDialogVisible = ref(false);
const ruleFormRef = ref<FormInstance>();
const ruleForm = reactive({
  ruleId: null as number | null,
  ruleName: '',
  metricKey: 'gdpYoy',
  comparator: 'lt',
  threshold: 0,
  durationYears: 1,
  enabled: true,
});

const metricOptions = [
  { label: 'GDP 增速(%)', value: 'gdpYoy' },
  { label: '地区生产总值 (亿元)', value: 'gdp' },
  { label: '农村可支配收入 (元)', value: 'ruralDisposableIncome' },
  { label: '财政收入增速(%)', value: 'fiscalRevenueYoy' },
];

const metricUnits: Record<string, string> = {
  gdpYoy: '%',
  gdp: '亿元',
  ruralDisposableIncome: '元',
  fiscalRevenueYoy: '%',
};

const comparatorOptions = [
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'lte' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'gte' },
];

const comparatorLabels: Record<string, string> = {
  lt: '小于',
  lte: '不超过',
  gt: '大于',
  gte: '不少于',
};

const metricLabelMap = metricOptions.reduce((acc, option) => {
  acc[option.value] = option.label;
  return acc;
}, {} as Record<string, string>);

const ruleFormRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metricKey: [{ required: true, message: '请选择监测指标', trigger: 'change' }],
  comparator: [{ required: true, message: '请选择条件', trigger: 'change' }],
  threshold: [{ required: true, message: '请输入阈值', trigger: 'change' }],
  durationYears: [{ required: true, message: '请输入持续年限', trigger: 'change' }],
};

const loadRules = async () => {
  loadingRules.value = true;
  try {
    const res = await getRules();
    rules.value = res.data || [];
  } catch (error) {
    console.error('加载警告规则失败', error);
    ElMessage.error('加载规则失败');
  } finally {
    loadingRules.value = false;
  }
};

const loadEvents = async () => {
  loadingEvents.value = true;
  try {
    const res = await getAlerts();
    events.value = res.data || [];
  } catch (error) {
    console.error('加载警告事件失败', error);
    ElMessage.error('加载警告失败');
  } finally {
    loadingEvents.value = false;
  }
};

const resetRuleForm = () => {
  ruleForm.ruleId = null;
  ruleForm.ruleName = '';
  ruleForm.metricKey = 'gdpYoy';
  ruleForm.comparator = 'lt';
  ruleForm.threshold = 0;
  ruleForm.durationYears = 1;
  ruleForm.enabled = true;
  ruleFormRef.value?.clearValidate();
};

const openRuleDialog = (rule?: AlertRuleRecord) => {
  if (rule) {
    ruleForm.ruleId = rule.ruleId ?? null;
    ruleForm.ruleName = rule.ruleName || '';
    ruleForm.metricKey = rule.metricKey || 'gdpYoy';
    ruleForm.comparator = rule.comparator || 'lt';
    ruleForm.threshold = rule.threshold ?? 0;
    ruleForm.durationYears = rule.durationYears ?? 1;
    ruleForm.enabled = Boolean(rule.enabled);
  } else {
    resetRuleForm();
  }
  ruleDialogVisible.value = true;
};

const buildRulePayload = () => ({
  ruleName: ruleForm.ruleName.trim(),
  metricKey: ruleForm.metricKey,
  comparator: ruleForm.comparator,
  threshold: ruleForm.threshold,
  durationYears: ruleForm.durationYears,
  enabled: ruleForm.enabled,
});

const submitRule = async () => {
  if (!ruleFormRef.value) return;
  try {
    await ruleFormRef.value.validate();
  } catch {
    return;
  }
  savingRule.value = true;
  try {
    const payload = buildRulePayload();
    if (ruleForm.ruleId) {
      await updateRule(ruleForm.ruleId, payload);
      ElMessage.success('规则已更新');
    } else {
      await createRule(payload);
      ElMessage.success('规则已创建');
    }
    ruleDialogVisible.value = false;
    await loadRules();
  } catch (error) {
    console.error('保存规则失败', error);
    ElMessage.error('保存规则失败，请稍后再试');
  } finally {
    savingRule.value = false;
  }
};

const toggleRuleStatus = async (rule: AlertRuleRecord, value: boolean) => {
  if (!rule.ruleId) return;
  const original = rule.enabled;
  rule.enabled = value;
  try {
    await updateRule(rule.ruleId, { enabled: value });
    ElMessage.success(value ? '规则已启用' : '规则已停用');
  } catch (error) {
    rule.enabled = original;
    console.error('切换规则状态失败', error);
    ElMessage.error('切换状态失败');
  }
};

const removeRule = async (rule: AlertRuleRecord) => {
  if (!rule.ruleId) return;
  try {
    await ElMessageBox.confirm(`确定删除规则「${rule.ruleName}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await deleteRule(rule.ruleId);
    ElMessage.success('规则已删除');
    await loadRules();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除规则失败', error);
      ElMessage.error('删除规则失败');
    }
  }
};

const resolveEvent = async (event: AlertEventRecord) => {
  if (!event.eventId || event.acknowledgedAt) return;
  try {
    await resolveAlertById(event.eventId);
    ElMessage.success('警告已标记为已处理');
    await loadEvents();
  } catch (error) {
    console.error('标记警告失败', error);
    ElMessage.error('标记失败');
  }
};

const triggerScan = async () => {
  scanning.value = true;
  try {
    await scanAlerts();
    ElMessage.success('已触发扫描任务');
    startScanPolling();
  } catch (error) {
    console.error('触发扫描失败', error);
    ElMessage.error('扫描失败');
  } finally {
    scanning.value = false;
  }
};

const startScanPolling = () => {
  stopScanPolling();
  pollScanStatus();
  scanPollTimer.value = window.setInterval(pollScanStatus, SCAN_POLL_INTERVAL);
};

const stopScanPolling = () => {
  if (scanPollTimer.value !== null) {
    clearInterval(scanPollTimer.value);
    scanPollTimer.value = null;
  }
};

const pollScanStatus = async () => {
  try {
    const res = await getAlertScanStatus();
    const data = res.data || {};
    scanStatus.value = {
      state: data.state || 'IDLE',
      message: data.message || '',
      startedAt: data.startedAt || null,
      finishedAt: data.finishedAt || null,
      durationMs: data.durationMs || null,
    };
    handleScanStatusChange(scanStatus.value);
  } catch (error) {
    console.error('获取扫描状态失败', error);
  }
};

const handleScanStatusChange = (status: typeof scanStatus.value) => {
  if (status.state === 'COMPLETED') {
    stopScanPolling();
    ElMessage.success(status.message || '扫描已完成');
    loadEvents();
  } else if (status.state === 'FAILED') {
    stopScanPolling();
    ElMessage.error(status.message || '扫描失败');
  }
};

const formatMetric = (key?: string) => metricLabelMap[key || ''] || key || '-';

const formatComparator = (value?: string) => comparatorLabels[value || ''] || value || '-';

const formatThreshold = (rule: AlertRuleRecord) => {
  if (rule.threshold === null || rule.threshold === undefined) return '-';
  const unit = metricUnits[rule.metricKey || ''] || '';
  return `${rule.threshold}${unit}`;
};

const formatMetricValue = (event: AlertEventRecord) => {
  if (event.metricValue === null || event.metricValue === undefined) return '-';
  const unit = metricUnits[event.rule?.metricKey || ''] || '';
  return `${event.metricValue}${unit}`;
};

const metricPrecision = (key?: string) => (key === 'ruralDisposableIncome' ? 0 : 2);

const metricStep = (key?: string) => (key === 'ruralDisposableIncome' ? 100 : 0.1);

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
};

onMounted(async () => {
  await Promise.all([loadRules(), loadEvents()]);
  await pollScanStatus();
  if (scanStatus.value.state === 'RUNNING') {
    startScanPolling();
  }
});

onBeforeUnmount(() => {
  stopScanPolling();
});
</script>

<style scoped>
.alerts-page {
  min-height: calc(100vh - 120px);
  padding: 24px;
  background: linear-gradient(135deg, #fef3c7 0%, #f7fafc 100%);
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: #fffaf0;
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.hero h2 {
  font-size: 28px;
  margin-bottom: 8px;
  color: #c05621;
}

.hero p {
  margin: 0;
  color: #744210;
  font-size: 15px;
}

.hero-stats {
  margin-top: 12px;
  display: flex;
  gap: 20px;
  color: #2d3748;
  font-weight: 600;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #2d3748;
}

.card-header p {
  margin: 4px 0 0;
  color: #718096;
  font-size: 13px;
}

.rule-card,
.event-card {
  border-radius: 16px;
}

.province-label {
  display: inline-block;
  margin-left: 4px;
  color: #718096;
  font-size: 12px;
}

.unit {
  margin-left: 10px;
  color: #a0aec0;
}

@media (min-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
}

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>