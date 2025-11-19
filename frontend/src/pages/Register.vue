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
            <option value="普通用户">普通用户</option>
            <option value="数据分析师">数据分析师</option>
          </select>
        </div>
        <button type="submit" class="btn">注册</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const role = ref('普通用户');

const handleRegister = () => {
  if (password.value !== confirmPassword.value) {
    alert('两次输入的密码不一致！');
    return;
  }
  alert(`注册成功！用户名：${username.value}，邮箱：${email.value}，角色：${role.value}`);
};
</script>

<style scoped>
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

.btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
}
</style>
