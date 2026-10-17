import axios from 'axios';
import { jwtDecode } from 'jwt-decode';
import { saveAccessToken, clearTokens, AUTH_BASE_URL, apiErrorMessage } from './apiClient';

const BASE_URL = AUTH_BASE_URL;

// Use plain axios (no interceptors) for auth endpoints so we never get
// caught in a refresh loop during login / signup
const authAxios = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

/**
 * Login with email + password.
 * The backend sets the refresh token as an HttpOnly cookie automatically.
 * Returns { userData, jwt }
 */
export const loginRequest = async ({ username, password }) => {
  let res;
  try {
    res = await authAxios.post('/auth/login', { username, password });
  } catch (err) {
    if (err.response) {
      const msg = err.response.data?.message || err.response.data?.error;
      if (err.response.status === 401) throw new Error('Invalid email or password');
      if (err.response.status === 404) throw new Error('User not found');
      if (err.response.status >= 500)  throw new Error(apiErrorMessage(err));
      throw new Error(msg || 'Login failed');
    }
    throw new Error(apiErrorMessage(err));
  }

  const data = res.data;
  const jwt    = data.accessToken || data.jwt || data.token;
  const userId = data.userId;

  if (!jwt)    throw new Error('Server did not return an access token');
  if (!userId) throw new Error('Server did not return a user ID');

  const decoded  = jwtDecode(jwt);
  const userData = {
    userId,
    email: decoded.sub || username,
    roles: decoded.roles || data.roles || [],
    ...decoded,
  };

  saveAccessToken(jwt);
  localStorage.setItem('user', JSON.stringify(userData));

  return { userData, jwt };
};

/**
 * Signup.
 * The backend sets the refresh token as an HttpOnly cookie automatically.
 * Returns { userData, jwt }
 */
export const signupRequest = async (signupData) => {
  let res;
  try {
    res = await authAxios.post('/auth/signup', signupData);
  } catch (err) {
    if (err.response) {
      const msg = err.response.data?.message || err.response.data?.error;
      if (err.response.status === 400) throw new Error(msg || 'Invalid signup data.');
      if (err.response.status === 409) throw new Error(msg || 'Email already exists. Use a different email.');
      if (err.response.status >= 500)  throw new Error(apiErrorMessage(err));
      throw new Error(msg || 'Signup failed');
    }
    throw new Error(apiErrorMessage(err));
  }

  const data   = res.data;
  const jwt    = data.accessToken || data.jwt || data.token;
  const userId = data.userId;

  if (jwt && userId) {
    const decoded  = jwtDecode(jwt);
    const userData = {
      userId,
      email: decoded.sub || signupData.username,
      roles: decoded.roles || data.roles || signupData.roles || [],
      ...decoded,
    };

    saveAccessToken(jwt);
    localStorage.setItem('user', JSON.stringify(userData));

    return { userData, jwt };
  }

  // Signup didn't return a token — fall back to login
  return loginRequest({ username: signupData.username, password: signupData.password });
};

/**
 * Logout — clears all local tokens.
 */
export const logoutRequest = () => {
  clearTokens();
};
