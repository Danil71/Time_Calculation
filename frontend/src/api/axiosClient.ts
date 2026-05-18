import axios from 'axios';

export const api = axios.create({
  baseURL: '/api', // Адрес Java бэкенда
  headers: {
    'Content-Type': 'application/json',
  },
});

// Перехватчик для добавления JWT токена
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => Promise.reject(error));

// Перехватчик для отлова ошибки 401 (токен протух)
api.interceptors.response.use((response) => response, (error) => {
  if (error.response && error.response.status === 401) {
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
  return Promise.reject(error);
});