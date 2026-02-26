import React, { useState } from 'react';
import { createAccount, getAccountsByUserId, getAccountById } from '../../api/financialApi';

const AccountsPage = () => {
  // Create Account state
  const [createUserId, setCreateUserId] = useState('');
  const [createAccountType, setCreateAccountType] = useState('');
  const [createCurrency, setCreateCurrency] = useState('USD');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createResult, setCreateResult] = useState(null);

  // List Accounts by User state
  const [listUserId, setListUserId] = useState('');
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [listResult, setListResult] = useState(null);

  // Get Account by ID state
  const [accountId, setAccountId] = useState('');
  const [accountLoading, setAccountLoading] = useState(false);
  const [accountError, setAccountError] = useState('');
  const [accountResult, setAccountResult] = useState(null);

  const handleCreateAccount = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setCreateError('');
    setCreateResult(null);
    try {
      const res = await createAccount({
        userId: Number(createUserId),
        accountType: createAccountType,
        currency: createCurrency,
      });
      setCreateResult(res.data);
      setCreateUserId('');
      setCreateAccountType('');
      setCreateCurrency('USD');
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleListAccounts = async (e) => {
    e.preventDefault();
    setListLoading(true);
    setListError('');
    setListResult(null);
    try {
      const res = await getAccountsByUserId(listUserId);
      setListResult(res.data);
    } catch (err) {
      setListError(err.message);
    } finally {
      setListLoading(false);
    }
  };

  const handleGetAccount = async (e) => {
    e.preventDefault();
    setAccountLoading(true);
    setAccountError('');
    setAccountResult(null);
    try {
      const res = await getAccountById(accountId);
      setAccountResult(res.data);
    } catch (err) {
      setAccountError(err.message);
    } finally {
      setAccountLoading(false);
    }
  };

  return (
    <div>
      <h1>Accounts</h1>

      <div className="section">
        <h2>Create Account</h2>
        <form className="form" onSubmit={handleCreateAccount}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={createUserId}
              onChange={(e) => setCreateUserId(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Account Type</label>
            <input
              type="text"
              value={createAccountType}
              onChange={(e) => setCreateAccountType(e.target.value)}
              placeholder="CHECKING / SAVINGS"
              required
            />
          </div>
          <div className="form-group">
            <label>Currency</label>
            <input
              type="text"
              value={createCurrency}
              onChange={(e) => setCreateCurrency(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={createLoading}>
            {createLoading ? 'Creating...' : 'Create Account'}
          </button>
        </form>
        {createError && <div className="error">{createError}</div>}
        {createResult && (
          <div className="result-card">
            <h3>Created Account</h3>
            <p><strong>ID:</strong> {createResult.id}</p>
            <p><strong>Account Type:</strong> {createResult.accountType}</p>
            <p><strong>Account Number:</strong> {createResult.accountNumber}</p>
            <p><strong>Balance:</strong> {createResult.balance}</p>
            <p><strong>Currency:</strong> {createResult.currency}</p>
          </div>
        )}
      </div>

      <div className="section">
        <h2>List Accounts by User</h2>
        <form className="form" onSubmit={handleListAccounts}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={listUserId}
              onChange={(e) => setListUserId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={listLoading}>
            {listLoading ? 'Loading...' : 'List Accounts'}
          </button>
        </form>
        {listError && <div className="error">{listError}</div>}
        {listResult && listResult.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Account Type</th>
                <th>Account Number</th>
                <th>Balance</th>
                <th>Currency</th>
              </tr>
            </thead>
            <tbody>
              {listResult.map((account) => (
                <tr key={account.id}>
                  <td>{account.id}</td>
                  <td>{account.accountType}</td>
                  <td>{account.accountNumber}</td>
                  <td>{account.balance}</td>
                  <td>{account.currency}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {listResult && listResult.length === 0 && (
          <div className="result-card">No accounts found for this user.</div>
        )}
      </div>

      <div className="section">
        <h2>Get Account by ID</h2>
        <form className="form" onSubmit={handleGetAccount}>
          <div className="form-group">
            <label>Account ID</label>
            <input
              type="number"
              value={accountId}
              onChange={(e) => setAccountId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={accountLoading}>
            {accountLoading ? 'Loading...' : 'Get Account'}
          </button>
        </form>
        {accountError && <div className="error">{accountError}</div>}
        {accountResult && (
          <div className="result-card">
            <h3>Account Details</h3>
            <p><strong>ID:</strong> {accountResult.id}</p>
            <p><strong>Account Type:</strong> {accountResult.accountType}</p>
            <p><strong>Account Number:</strong> {accountResult.accountNumber}</p>
            <p><strong>Balance:</strong> {accountResult.balance}</p>
            <p><strong>Currency:</strong> {accountResult.currency}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default AccountsPage;
