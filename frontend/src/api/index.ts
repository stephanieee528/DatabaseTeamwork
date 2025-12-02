// src/api/index.ts
import axios, { AxiosInstance } from 'axios';

// 创建 axios 实例
const api: AxiosInstance = axios.create({
    baseURL: '/api', // Vite 代理 /api 到后端
    timeout: 10000,
});

// 请求拦截器：每次请求自动带 JWT
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        // 确保 headers 存在
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

// 响应拦截器：401 提示重新登录
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            console.warn('未授权或 token 过期，请重新登录');
            localStorage.removeItem('token');
            localStorage.removeItem('currentUser');
            // 跳转到登录页
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ----------------------
// 登录接口
// ----------------------
export const login = (username: string, password: string) => {
    return api.post('/auth/login', { username, password }).then((res) => {
        const token = res.data.token;
        if (token) {
            localStorage.setItem('token', token);
            api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        }
        if (res.data?.user) {
            localStorage.setItem('currentUser', JSON.stringify(res.data.user));
        } else {
            localStorage.removeItem('currentUser');
        }
        return res.data;
    });
};

export const loginUser = (credentials: { username: string; password: string }) =>
  login(credentials.username, credentials.password);

export const registerUser = (userData: {
  username: string;
  password: string;
  email: string;
  role: string;
  fullname?: string;
}) => {
  return api.post('/auth/register', userData);
};

// ----------------------
// 其他接口封装
// ----------------------



export const getSummary = (year?: number) =>
    api.get(`/indicators/summary${year ? '?year=' + year : ''}`);

export const getCounties = (params?: { provinceId?: number; keyword?: string }) =>
    api.get('/counties', { params });

export const getCountyIndicators = (
    id: number,
    from?: number,
    to?: number
) =>
    api.get(
        `/counties/${id}/indicators${
            from || to ? `?from=${from || 2015}&to=${to || new Date().getFullYear()}` : ''
        }`
    );

export const getAlerts = () => api.get('/alerts');

export const scanAlerts = () => api.post('/alerts/scan');
export const getAlertScanStatus = () => api.get('/alerts/scan/status');

export const getCountyDetail = (id: number) => api.get(`/counties/${id}/detail`);

export const getRules = () => api.get('/alerts/rules');

export const createRule = (r: any) => api.post('/alerts/rules', r);

export const updateRule = (id: number, payload: any) => api.put(`/alerts/rules/${id}`, payload);

export const deleteRule = (id: number) => api.delete(`/alerts/rules/${id}`);

export const resolveAlertById = (id: number) => api.post(`/alerts/${id}/resolve`);

export const deleteUserById = (id: number) => api.delete(`/users/${id}`);

export const getUsers = () => api.get('/users');

export const getCurrentUser = () => api.get('/users/current');

export const updateCounty = (id: number, payload: any) => api.put(`/counties/${id}`, payload);

export const getCountyProjects = (id: number) => api.get(`/counties/${id}/projects`);

export const createCountyProject = (id: number, payload: any) => api.post(`/counties/${id}/projects`, payload);

export const updateCountyProject = (id: number, projectId: number, payload: any) =>
    api.put(`/counties/${id}/projects/${projectId}`, payload);

export const deleteCountyProject = (id: number, projectId: number) =>
    api.delete(`/counties/${id}/projects/${projectId}`);

export const getProvinces = () => api.get('/counties/provinces');

// ----------------------
// 获取图表数据接口
// ----------------------
export const getChartsData = (year?: number) =>
    api.get(`/dashboard/charts${year ? '?year=' + year : ''}`);

export const getAnalysisData = (year: number) => api.get(`/indicators/charts${year ? '?year=' + year : ''}`);

// 默认导出一个对象，方便在 Vue 中直接 import api from '@/api';
export default {
    login,
    getSummary,
    getCounties,
    getCountyIndicators,
    getAlerts,
    scanAlerts,
    getAlertScanStatus,
    getRules,
    createRule,
    updateRule,
    deleteRule,
    resolveAlertById,
    getCountyDetail,
    deleteUserById,
    getUsers,
    getChartsData,
    getAnalysisData,
    getCurrentUser,
    updateCounty,
    getCountyProjects,
    createCountyProject,
    updateCountyProject,
    deleteCountyProject,
    getProvinces
};
