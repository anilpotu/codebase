import { useState } from 'react';
import {
  deleteProfileById,
  getMyProfile,
  getProfileById,
  getProfiles,
  updateMyProfile,
  updateProfileById,
} from '../../api/userProfileApi';

const unwrap = (res) => res?.data?.data ?? res?.data;

export default function UserProfilesPage() {
  const [profiles, setProfiles] = useState(null);
  const [lookupId, setLookupId] = useState('');
  const [lookupResult, setLookupResult] = useState(null);
  const [updateId, setUpdateId] = useState('');
  const [updateForm, setUpdateForm] = useState({ firstName: '', lastName: '', phoneNumber: '', address: '' });
  const [deleteId, setDeleteId] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const resetFeedback = () => {
    setMessage('');
    setError('');
  };

  const onGetAll = async () => {
    resetFeedback();
    try {
      const res = await getProfiles();
      setProfiles(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onGetById = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await getProfileById(Number(lookupId));
      setLookupResult(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onGetMe = async () => {
    resetFeedback();
    try {
      const res = await getMyProfile();
      setLookupResult(unwrap(res));
      setMessage('Loaded /users/me');
    } catch (err) {
      setError(err.message);
    }
  };

  const onUpdateById = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await updateProfileById(Number(updateId), updateForm);
      setLookupResult(unwrap(res));
      setMessage('Profile updated');
    } catch (err) {
      setError(err.message);
    }
  };

  const onUpdateMe = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await updateMyProfile(updateForm);
      setLookupResult(unwrap(res));
      setMessage('Current user profile updated');
    } catch (err) {
      setError(err.message);
    }
  };

  const onDelete = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      await deleteProfileById(Number(deleteId));
      setMessage(`Deleted profile ${deleteId}`);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1>User Service - Profiles</h1>

      <div className="section">
        <h2>Read Profiles</h2>
        <button className="btn" type="button" onClick={onGetAll}>Get All Profiles</button>
        <button className="btn" type="button" onClick={onGetMe} style={{ marginLeft: 10 }}>Get /users/me</button>
        <form className="form" onSubmit={onGetById} style={{ marginTop: 12 }}>
          <div className="form-group">
            <label>Profile ID</label>
            <input type="number" value={lookupId} onChange={(e) => setLookupId(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Get By ID</button>
        </form>
      </div>

      <div className="section">
        <h2>Update Profile</h2>
        <form className="form" onSubmit={onUpdateById}>
          <div className="form-group">
            <label>Profile ID</label>
            <input type="number" value={updateId} onChange={(e) => setUpdateId(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>First Name</label>
            <input value={updateForm.firstName} onChange={(e) => setUpdateForm((s) => ({ ...s, firstName: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Last Name</label>
            <input value={updateForm.lastName} onChange={(e) => setUpdateForm((s) => ({ ...s, lastName: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Phone</label>
            <input value={updateForm.phoneNumber} onChange={(e) => setUpdateForm((s) => ({ ...s, phoneNumber: e.target.value }))} />
          </div>
          <div className="form-group">
            <label>Address</label>
            <input value={updateForm.address} onChange={(e) => setUpdateForm((s) => ({ ...s, address: e.target.value }))} />
          </div>
          <button className="btn" type="submit">Update By ID</button>
          <button className="btn" type="button" onClick={onUpdateMe} style={{ marginLeft: 10 }}>Update /users/me</button>
        </form>
      </div>

      <div className="section">
        <h2>Delete Profile</h2>
        <form className="form" onSubmit={onDelete}>
          <div className="form-group">
            <label>Profile ID</label>
            <input type="number" value={deleteId} onChange={(e) => setDeleteId(e.target.value)} required />
          </div>
          <button className="btn btn-danger" type="submit">Delete</button>
        </form>
      </div>

      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}

      {Array.isArray(profiles) && (
        <div className="section">
          <h2>All Profiles</h2>
          {profiles.length === 0 ? <p>No profiles found.</p> : (
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>User ID</th>
                  <th>Name</th>
                  <th>Phone</th>
                  <th>Address</th>
                </tr>
              </thead>
              <tbody>
                {profiles.map((p) => (
                  <tr key={p.id}>
                    <td>{p.id}</td>
                    <td>{p.userId}</td>
                    <td>{p.firstName} {p.lastName}</td>
                    <td>{p.phoneNumber}</td>
                    <td>{p.address}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {lookupResult && (
        <div className="result-card">
          <h3>Profile Details</h3>
          <p><strong>ID:</strong> {lookupResult.id}</p>
          <p><strong>User ID:</strong> {lookupResult.userId}</p>
          <p><strong>Name:</strong> {lookupResult.firstName} {lookupResult.lastName}</p>
          <p><strong>Phone:</strong> {lookupResult.phoneNumber}</p>
          <p><strong>Address:</strong> {lookupResult.address}</p>
        </div>
      )}
    </div>
  );
}
