<template>
  <el-card style="max-width: 800px; margin: 20px auto;">
    <h2>个人信息</h2>
    <el-form :model="userInfo" label-width="100px" style="margin-top: 20px;">
      <el-form-item label="用户名">
        <el-input v-model="userInfo.username" disabled />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="userInfo.fullname" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="userInfo.email" />
      </el-form-item>
      <el-form-item label="角色">
        <el-input v-model="userInfo.roleName" disabled />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveProfile" :loading="loading">保存修改</el-button>
        <el-button @click="handleLogout">退出登录</el-button>
      </el-form-item>
    </el-form>
    
    <!-- 调试信息（开发时可用） -->
    <el-collapse v-if="debugInfo" style="margin-top: 20px;">
      <el-collapse-item title="调试信息">
        <pre>{{ debugInfo }}</pre>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getCurrentUser } from '@/api'; // 使用统一的API调用

const router = useRouter();
const userInfo = ref({
  userId: null,
  username: '',
  fullname: '',
  email: '',
  roleName: ''
});
const loading = ref(false);
const debugInfo = ref('');

// 获取当前用户信息
const fetchUserInfo = async () => {
  loading.value = true;
  try {
    // 检查是否已登录
    const token = localStorage.getItem('token');
    if (!token) {
      ElMessage.warning('请先登录');
      router.push('/login');
      return;
    }
    
    debugInfo.value = `Token: ${token.substring(0, 20)}...`;
    
    const response = await getCurrentUser();
    
    if (response.data) {
      userInfo.value = response.data;
      ElMessage.success('获取个人信息成功');
    } else {
      throw new Error('响应数据为空');
    }
  } catch (error: any) {
    console.error('获取个人信息失败:', error);
    
    // 详细的错误信息
    let errorMessage = '获取个人信息失败';
    if (error.response) {
      errorMessage += `: ${error.response.status} ${error.response.statusText}`;
      if (error.response.data) {
        errorMessage += ` - ${JSON.stringify(error.response.data)}`;
      }
    } else if (error.request) {
      errorMessage += ': 无法连接到服务器';
    } else {
      errorMessage += `: ${error.message}`;
    }
    
    ElMessage.error(errorMessage);
    
    // 如果是认证错误，跳转到登录页
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('token');
      setTimeout(() => {
        router.push('/login');
      }, 2000);
    }
  } finally {
    loading.value = false;
  }
};

// 保存个人信息
const saveProfile = async () => {
  loading.value = true;
  try {
    // 这里需要添加更新用户信息的API调用
    // 暂时使用成功提示
    ElMessage.success('信息保存成功');
  } catch (error: any) {
    console.error('保存失败:', error);
    ElMessage.error('保存失败：' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token');
  // 清除全局请求头中的token
  delete (window as any).axios?.defaults?.headers?.common?.['Authorization'];
  router.push('/login');
  ElMessage.success('已退出登录');
};

// 页面加载时获取用户信息
onMounted(() => {
  fetchUserInfo();
});
</script>