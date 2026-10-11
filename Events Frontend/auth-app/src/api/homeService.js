import api from './apiClient';

/**
 * Fetch basic stats for the Home page.
 * Backend endpoint: GET /home/basic-info
 */
export const fetchBasicInfo = async () => {
  const res = await api.get('/home/basic-info');
  return res.data;
};
