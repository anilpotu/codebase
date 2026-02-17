import React, { useState } from 'react';
import { createTransaction, getTransactionsByAccountId } from '../../api/financialApi';

const TransactionsPage = () => {
  // Create Transaction state
  const [createAccountId, setCreateAccountId] = useState('');
  const [createType, setCreateType] = useState('DEPOSIT');
  const [createAmount, setCreateAmount] = useState('');
  const [createDescription, setCreateDescription] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createResult, setCreateResult] = useState(null);

  // List Transactions state
  const [listAccountId, setListAccountId] = useState('');
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [listResult, setListResult] = useState(null);

  const handleCreateTransaction = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setCreateError('');
    setCreateResult(null);
    try {
      const res = await createTransaction({
        accountId: Number(createAccountId),
        transactionType: createType,
        amount: Number(createAmount),
        description: createDescription,
      });
      setCreateResult(res.data);
      setCreateAccountId('');
      setCreateType('DEPOSIT');
      setCreateAmount('');
      setCreateDescription('');
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleListTransactions = async (e) => {
    e.preventDefault();
    setListLoading(true);
    setListError('');
    setListResult(null);
    try {
      const res = await getTransactionsByAccountId(listAccountId);
      setListResult(res.data);
    } catch (err) {
      setListError(err.message);
    } finally {
      setListLoading(false);
    }
  };

  return (
    <div>
      <h1>Transactions</h1>

      <div className="section">
        <h2>Create Transaction</h2>
        <form className="form" onSubmit={handleCreateTransaction}>
          <div className="form-group">
            <label>Account ID</label>
            <input
              type="number"
              value={createAccountId}
              onChange={(e) => setCreateAccountId(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Transaction Type</label>
            <select
              value={createType}
              onChange={(e) => setCreateType(e.target.value)}
              required
            >
              <option value="DEPOSIT">DEPOSIT</option>
              <option value="WITHDRAWAL">WITHDRAWAL</option>
              <option value="TRANSFER">TRANSFER</option>
            </select>
          </div>
          <div className="form-group">
            <label>Amount</label>
            <input
              type="number"
              value={createAmount}
              onChange={(e) => setCreateAmount(e.target.value)}
              step="0.01"
              min="0"
              required
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <input
              type="text"
              value={createDescription}
              onChange={(e) => setCreateDescription(e.target.value)}
            />
          </div>
          <button className="btn" type="submit" disabled={createLoading}>
            {createLoading ? 'Creating...' : 'Create Transaction'}
          </button>
        </form>
        {createError && <div className="error">{createError}</div>}
        {createResult && (
          <div className="result-card">
            <h3>Created Transaction</h3>
            <p><strong>ID:</strong> {createResult.id}</p>
            <p><strong>Type:</strong> {createResult.transactionType}</p>
            <p><strong>Amount:</strong> {createResult.amount}</p>
            <p><strong>Description:</strong> {createResult.description}</p>
            <p><strong>Date:</strong> {createResult.transactionDate}</p>
          </div>
        )}
      </div>

      <div className="section">
        <h2>List Transactions</h2>
        <form className="form" onSubmit={handleListTransactions}>
          <div className="form-group">
            <label>Account ID</label>
            <input
              type="number"
              value={listAccountId}
              onChange={(e) => setListAccountId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={listLoading}>
            {listLoading ? 'Loading...' : 'List Transactions'}
          </button>
        </form>
        {listError && <div className="error">{listError}</div>}
        {listResult && listResult.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Description</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {listResult.map((tx) => (
                <tr key={tx.id}>
                  <td>{tx.id}</td>
                  <td>{tx.transactionType}</td>
                  <td>{tx.amount}</td>
                  <td>{tx.description}</td>
                  <td>{tx.transactionDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {listResult && listResult.length === 0 && (
          <div className="result-card">No transactions found for this account.</div>
        )}
      </div>
    </div>
  );
};

export default TransactionsPage;
