import React, { useState } from 'react';
import { createUser, getUser, deleteUser } from '../../api/userApi';

const UsersPage = () => {
  // Create User state
  const [createName, setCreateName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createResult, setCreateResult] = useState(null);

  // Lookup User state
  const [lookupId, setLookupId] = useState('');
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState('');
  const [lookupResult, setLookupResult] = useState(null);

  // Delete User state
  const [deleteId, setDeleteId] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [deleteMessage, setDeleteMessage] = useState('');

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setCreateError('');
    setCreateResult(null);
    try {
      const res = await createUser({ name: createName, email: createEmail });
      setCreateResult(res.data);
      setCreateName('');
      setCreateEmail('');
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleLookupUser = async (e) => {
    e.preventDefault();
    setLookupLoading(true);
    setLookupError('');
    setLookupResult(null);
    try {
      const res = await getUser(lookupId);
      setLookupResult(res.data);
    } catch (err) {
      setLookupError(err.message);
    } finally {
      setLookupLoading(false);
    }
  };

  const handleDeleteUser = async (e) => {
    e.preventDefault();
    setDeleteLoading(true);
    setDeleteError('');
    setDeleteMessage('');
    try {
      await deleteUser(deleteId);
      setDeleteMessage(`User ${deleteId} deleted successfully.`);
      setDeleteId('');
    } catch (err) {
      setDeleteError(err.message);
    } finally {
      setDeleteLoading(false);
    }
  };

  return (
    <div>
      <h1>Users</h1>

      <div className="section">
        <h2>Create User</h2>
        <form className="form" onSubmit={handleCreateUser}>
          <div className="form-group">
            <label>Name</label>
            <input
              type="text"
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input
              type="text"
              value={createEmail}
              onChange={(e) => setCreateEmail(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={createLoading}>
            {createLoading ? 'Creating...' : 'Create User'}
          </button>
        </form>
        {createError && <div className="error">{createError}</div>}
        {createResult && (
          <div className="result-card">
            <h3>Created User</h3>
            <p><strong>ID:</strong> {createResult.id}</p>
            <p><strong>Name:</strong> {createResult.name}</p>
            <p><strong>Email:</strong> {createResult.email}</p>
          </div>
        )}
      </div>

      <div className="section">
        <h2>Lookup User</h2>
        <form className="form" onSubmit={handleLookupUser}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={lookupId}
              onChange={(e) => setLookupId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={lookupLoading}>
            {lookupLoading ? 'Looking up...' : 'Get User'}
          </button>
        </form>
        {lookupError && <div className="error">{lookupError}</div>}
        {lookupResult && (
          <div className="result-card">
            <h3>User Details</h3>
            <p><strong>ID:</strong> {lookupResult.id}</p>
            <p><strong>Name:</strong> {lookupResult.name}</p>
            <p><strong>Email:</strong> {lookupResult.email}</p>
          </div>
        )}
      </div>

      <div className="section">
        <h2>Delete User</h2>
        <form className="form" onSubmit={handleDeleteUser}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={deleteId}
              onChange={(e) => setDeleteId(e.target.value)}
              required
            />
          </div>
          <button className="btn btn-danger" type="submit" disabled={deleteLoading}>
            {deleteLoading ? 'Deleting...' : 'Delete User'}
          </button>
        </form>
        {deleteError && <div className="error">{deleteError}</div>}
        {deleteMessage && <div className="success">{deleteMessage}</div>}
      </div>
    </div>
  );
};

export default UsersPage;
