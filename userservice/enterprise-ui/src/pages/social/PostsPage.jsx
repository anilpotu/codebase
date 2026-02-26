import React, { useState } from 'react';
import { createPost, getPostsByUserId } from '../../api/socialApi';

const PostsPage = () => {
  // Create Post state
  const [postForm, setPostForm] = useState({ userId: '', content: '' });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createResult, setCreateResult] = useState(null);

  // List Posts state
  const [listUserId, setListUserId] = useState('');
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [listResult, setListResult] = useState(null);

  const handlePostFormChange = (e) => {
    const { name, value } = e.target;
    setPostForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreatePost = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setCreateError('');
    try {
      const res = await createPost({
        userId: Number(postForm.userId),
        content: postForm.content,
      });
      setCreateResult(res.data);
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleListPosts = async (e) => {
    e.preventDefault();
    setListLoading(true);
    setListError('');
    try {
      const res = await getPostsByUserId(Number(listUserId));
      setListResult(res.data);
    } catch (err) {
      setListError(err.message);
    } finally {
      setListLoading(false);
    }
  };

  return (
    <div>
      <h1>Posts</h1>

      <div className="section">
        <h2>Create Post</h2>
        <form className="form" onSubmit={handleCreatePost}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              name="userId"
              value={postForm.userId}
              onChange={handlePostFormChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Content</label>
            <textarea
              name="content"
              value={postForm.content}
              onChange={handlePostFormChange}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={createLoading}>
            {createLoading ? 'Posting...' : 'Create Post'}
          </button>
        </form>
        {createError && <div className="error">{createError}</div>}
        {createResult && (
          <div className="result-card">
            <h3>Post Created</h3>
            <p><strong>Post ID:</strong> {createResult.id}</p>
            <p><strong>User ID:</strong> {createResult.userId}</p>
            <p><strong>Content:</strong> {createResult.content}</p>
            <p><strong>Likes:</strong> {createResult.likesCount}</p>
            <p><strong>Comments:</strong> {createResult.commentsCount}</p>
            <p><strong>Created:</strong> {createResult.createdAt}</p>
          </div>
        )}
      </div>

      <div className="section">
        <h2>List Posts by User</h2>
        <form className="form" onSubmit={handleListPosts}>
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
            {listLoading ? 'Loading...' : 'List Posts'}
          </button>
        </form>
        {listError && <div className="error">{listError}</div>}
        {listResult && (
          <div>
            {listResult.length === 0 && <p>No posts found.</p>}
            {listResult.map((post) => (
              <div className="result-card" key={post.id}>
                <p>{post.content}</p>
                <p><strong>Likes:</strong> {post.likesCount} | <strong>Comments:</strong> {post.commentsCount}</p>
                <p><strong>Created:</strong> {post.createdAt}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PostsPage;
