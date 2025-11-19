<template>
  <div class="login-page">
    <div class="login-container">
      <h2>🔒 用户登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input id="username" v-model="username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input id="password" v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <button class="btn" type="submit">登录</button>
        <p class="register-link">
          还没有账号？<a @click="goToRegister">点击注册</a>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { loginUser } from '@/api';
import { ElMessage } from 'element-plus';
import axios from 'axios'; // 添加这行导入
const username = ref('');
const password = ref('');
const loading = ref(false);
const router = useRouter();

// 登录处理
const handleLogin = async () => {
  loading.value = true;
  
  try {
    const response = await loginUser({ 
      username: username.value, 
      password: password.value 
    });
    
    // 保存 Token 到本地存储
    const token = response.data.token;
    if (token) {
      localStorage.setItem('token', token);
      // 设置全局 axios 请求头
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
    
    ElMessage.success('登录成功！');
    router.push('/home');
    
  } catch (error) {
    console.error('Login failed:', error);
    const errorMessage = error.response?.data?.message || '登录失败，请检查用户名或密码。';
    ElMessage.error(errorMessage);
  } finally {
    loading.value = false;
  }
};

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register');
};
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #edf2f7;
}

.login-container {
  background: #fff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

h2 {
  text-align: center;
  color: #2c5282;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
  color: #4a5568;
}

input {
  width: 100%;
  padding: 10px;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
  font-size: 14px;
}

input:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.3);
}

.btn {
  width: 100%;
  padding: 10px;
  background: #3182ce;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn:hover {
  background: #2b6cb0;
}

/* 保留原有样式，并添加注册链接样式 */
.register-link {
  text-align: center;
  margin-top: 15px;
  color: #4a5568;
}

.register-link a {
  color: #3182ce;
  cursor: pointer;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}

</style>

