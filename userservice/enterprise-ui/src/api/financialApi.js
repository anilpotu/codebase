import api from './axios';

export const createAccount = (data) => api.post('/api/accounts', data);
export const getAccountsByUserId = (userId) => api.get(`/api/accounts/user/${userId}`);
export const getAccountById = (id) => api.get(`/api/accounts/${id}`);
export const createTransaction = (data) => api.post('/api/transactions', data);
export const getTransactionsByAccountId = (accountId) => api.get(`/api/transactions/account/${accountId}`);
