<template>
  <div class="users-page">
    <div class="header">
      <h2>👥 用户管理</h2>
      <p>查看和管理所有用户信息。</p>
    </div>

    <div class="users-section">
      <table class="users-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>角色</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.role }}</td>
            <td>
              <button class="btn" @click="deleteUser(user.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getUsers, deleteUserById } from '@/api';

const users = ref([]);

const deleteUser = async (id) => {
  try {
    await deleteUserById(id);
    users.value = users.value.filter((user) => user.id !== id);
    alert('用户已删除');
  } catch (error) {
    console.error('Failed to delete user:', error);
    alert('删除用户失败');
  }
};

onMounted(async () => {
  try {
    const response = await getUsers();
    users.value = response.data;
  } catch (error) {
    console.error('Failed to load users:', error);
  }
});
</script>

<style scoped>
.users-page {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.header {
  text-align: center;
  margin-bottom: 20px;
}

.header h2 {
  font-size: 28px;
  color: #2c5282;
}

.users-section {
  margin-top: 20px;
}

.users-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.users-table th,
.users-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.users-table th {
  background: #edf2f7;
  color: #4a5568;
  font-weight: bold;
}

.users-table tr:hover {
  background: #f7fafc;
}

.btn {
  background: #e53e3e;
  color: #fff;
  border: none;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.btn:hover {
  background: #c53030;
}
</style>