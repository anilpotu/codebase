import api from './axios';

export const createHealthRecord = (data) => api.post('/api/health-records', data);
export const getHealthRecordByUserId = (userId) => api.get(`/api/health-records/user/${userId}`);
export const recordVital = (data) => api.post('/api/vitals', data);
export const getVitalsByUserId = (userId) => api.get(`/api/vitals/user/${userId}`);
export const getLatestVital = (userId) => api.get(`/api/vitals/user/${userId}/latest`);
