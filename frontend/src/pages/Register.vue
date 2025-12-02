<template>
  <div class="register">
    <div class="form-container">
      <h2 class="form-title">用户注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="username" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input type="email" v-model="email" placeholder="请输入邮箱" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="password" placeholder="请输入密码" required />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input type="password" v-model="confirmPassword" placeholder="请再次输入密码" required />
        </div>
        <div class="form-group">
          <label>用户角色</label>
          <select v-model="role">
            <option value="群众">群众</option>
            <option value="数据分析师">数据分析师</option>
          </select>
        </div>
        <button type="submit" class="btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="login-link">
          已有账号？<a @click="goToLogin">点击登录</a>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { registerUser } from '@/api';
import { ElMessage } from 'element-plus';

const router = useRouter();

const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const role = ref('群众');
const loading = ref(false);

const handleRegister = async () => {
  // 表单验证
  if (password.value !== confirmPassword.value) {
    ElMessage.error('两次输入的密码不一致！');
    return;
  }

  if (password.value.length < 6) {
    ElMessage.error('密码长度不能少于6位！');
    return;
  }

  loading.value = true;

  try {
    const response = await registerUser({
      username: username.value,
      password: password.value,
      email: email.value,
      role: role.value,
      fullname: username.value
    });

    // 检查响应是否成功
    if (response.data && response.data.success) {
      ElMessage.success(response.data.message || '注册成功！');
      // 注册成功后跳转到登录页面
      setTimeout(() => {
        router.push('/login');
      }, 1500);
    } else {
      // 如果后端返回了成功字段但为false
      ElMessage.error(response.data?.message || '注册失败');
    }
    
  } catch (error) {
    console.error('Registration failed:', error);
    
    // 更详细的错误处理
    if (error.response) {
      // 服务器返回了错误状态码
      const errorData = error.response.data;
      if (errorData && errorData.message) {
        ElMessage.error(errorData.message);
      } else {
        ElMessage.error(`注册失败: ${error.response.status} ${error.response.statusText}`);
      }
    } else if (error.request) {
      // 请求发送了但没有收到响应
      ElMessage.error('网络错误，请检查网络连接');
    } else {
      // 其他错误
      ElMessage.error('注册失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
};

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
/* 保持原有样式不变 */
.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0fff4 100%);
}

.form-container {
  max-width: 450px;
  background: rgba(255, 255, 255, 0.7);
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.form-title {
  text-align: center;
  margin-bottom: 30px;
  color: #2c5282;
  font-size: 28px;
  font-weight: 700;
}

.form-group {
  margin-bottom: 25px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #4a5568;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 14px;
  border: 1px solid #cbd5e0;
  border-radius: 8px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.3);
}

.btn {
  background: linear-gradient(135deg, #2b6cb0 0%, #38a169 100%);
  color: white;
  padding: 14px 30px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
}

.btn:disabled {
  background: #a0aec0;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.login-link {
  text-align: center;
  margin-top: 15px;
  color: #4a5568;
}

.login-link a {
  color: #3182ce;
  cursor: pointer;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>