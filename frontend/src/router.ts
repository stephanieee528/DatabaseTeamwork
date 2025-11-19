import { createRouter, createWebHistory } from 'vue-router';
import Home from '@/pages/Home.vue';
import County from '@/pages/County.vue';
import Analysis from '@/pages/Analysis.vue';
import Alerts from '@/pages/Alerts.vue';
import Login from '@/pages/Login.vue';
import Users from '@/pages/Users.vue';
import Dashboard from '@/pages/Dashboard.vue';

const routes = [
  { path: '/home', component: Home },
  { path: '/county', component: County },
  { path: '/analysis', component: Analysis },
  { path: '/alerts', component: Alerts },
  { path: '/login', component: Login },
  { path: '/users', component: Users },
  { path: '/dashboard', component: Dashboard },
  { path: '/', redirect: '/home' },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
