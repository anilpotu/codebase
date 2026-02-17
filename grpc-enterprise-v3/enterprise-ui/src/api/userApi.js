import api from './axios';

export const createUser = (data) => api.post('/api/users', data);
export const getUser = (id) => api.get(`/api/users/${id}`);
export const deleteUser = (id) => api.delete(`/api/users/${id}`);
