import React, { useState } from 'react';
import { createProfile, getProfileByUserId } from '../../api/socialApi';

const ProfilesPage = () => {
  // Create/Update Profile state
  const [profileForm, setProfileForm] = useState({
    userId: '',
    displayName: '',
    bio: '',
    location: '',
    website: '',
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createResult, setCreateResult] = useState(null);

  // Lookup Profile state
  const [lookupUserId, setLookupUserId] = useState('');
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState('');
  const [lookupResult, setLookupResult] = useState(null);

  const handleProfileFormChange = (e) => {
    const { name, value } = e.target;
    setProfileForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreateProfile = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setCreateError('');
    try {
      const res = await createProfile({
        userId: Number(profileForm.userId),
        displayName: profileForm.displayName,
        bio: profileForm.bio,
        location: profileForm.location,
        website: profileForm.website,
      });
      setCreateResult(res.data);
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleLookupProfile = async (e) => {
    e.preventDefault();
    setLookupLoading(true);
    setLookupError('');
    try {
      const res = await getProfileByUserId(Number(lookupUserId));
      setLookupResult(res.data);
    } catch (err) {
      setLookupError(err.message);
    } finally {
      setLookupLoading(false);
    }
  };

  return (
    <div>
      <h1>Profiles</h1>

      <div className="section">
        <h2>Create / Update Profile</h2>
        <form className="form" onSubmit={handleCreateProfile}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              name="userId"
              value={profileForm.userId}
              onChange={handleProfileFormChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Display Name</label>
            <input
              type="text"
              name="displayName"
              value={profileForm.displayName}
              onChange={handleProfileFormChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Bio</label>
            <textarea
              name="bio"
              value={profileForm.bio}
              onChange={handleProfileFormChange}
            />
          </div>
          <div className="form-group">
            <label>Location</label>
            <input
              type="text"
              name="location"
              value={profileForm.location}
              onChange={handleProfileFormChange}
            />
          </div>
          <div className="form-group">
            <label>Website</label>
            <input
              type="text"
              name="website"
              value={profileForm.website}
              onChange={handleProfileFormChange}
            />
          </div>
          <button className="btn" type="submit" disabled={createLoading}>
            {createLoading ? 'Saving...' : 'Save Profile'}
          </button>
        </form>
        {createError && <div className="error">{createError}</div>}
        {createResult && (
          <div className="success">
            Profile saved for user {createResult.userId} ({createResult.displayName})
          </div>
        )}
      </div>

      <div className="section">
        <h2>Lookup Profile by User</h2>
        <form className="form" onSubmit={handleLookupProfile}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={lookupUserId}
              onChange={(e) => setLookupUserId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={lookupLoading}>
            {lookupLoading ? 'Loading...' : 'Lookup'}
          </button>
        </form>
        {lookupError && <div className="error">{lookupError}</div>}
        {lookupResult && (
          <div className="result-card">
            <h3>{lookupResult.displayName}</h3>
            <p><strong>User ID:</strong> {lookupResult.userId}</p>
            <p><strong>Bio:</strong> {lookupResult.bio}</p>
            <p><strong>Location:</strong> {lookupResult.location}</p>
            <p><strong>Website:</strong> {lookupResult.website}</p>
            <p><strong>Followers:</strong> {lookupResult.followersCount}</p>
            <p><strong>Following:</strong> {lookupResult.followingCount}</p>
            <p><strong>Created:</strong> {lookupResult.createdAt}</p>
            <p><strong>Updated:</strong> {lookupResult.updatedAt}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfilesPage;
