import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchBasicInfo as fetchBasicInfoApi } from '../api/homeService';
import Navbar from '../components/Navbar';
import TrendingEvents from '../components/engagement/TrendingEvents';
import '../styles/Home.css';

export default function Home() {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalEventsHosted: 0,
    totalStudentParticipated: 0,
    totalCollegeConnected: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBasicInfo();
  }, []);

  const fetchBasicInfo = async () => {
    try {
      const data = await fetchBasicInfoApi();
      setStats({
        totalEventsHosted: data.totalEventsHosted || 0,
        totalStudentParticipated: data.totalStudentParticipated || 0,
        totalCollegeConnected: data.totalCollegeConnected || 0,
      });
    } catch (error) {
      if (error.message === 'SESSION_EXPIRED') return;
      console.error('Failed to fetch basic info:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="home-page">
      <Navbar />
      <div className="home-container">
        <div className="home-content">
          <div className="home-hero">
            <h1 className="home-title">
              Discover. Register.
              <br />
              <span className="title-gradient">Participate. Excel.</span>
            </h1>
            
            <p className="home-description">
              Your gateway to exciting campus events, workshops, and competitions. 
              Connect, learn, and grow with your college community.
            </p>

            <div className="home-buttons">
              <button className="btn btn-primary" onClick={() => navigate('/events')}>
                <svg className="btn-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                Browse Events
              </button>
              
              <button className="btn btn-secondary" onClick={() => navigate('/signup')}>
                <svg className="btn-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                Get Started
              </button>
            </div>

            <div className="home-stats">
              <div className="stat-item">
                <div className="stat-icon stat-icon-blue">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
                <div className="stat-content">
                  <div className="stat-value">
                    {loading ? '...' : stats.totalEventsHosted.toLocaleString()}
                  </div>
                  <div className="stat-label">Events Hosted</div>
                </div>
              </div>

              <div className="stat-item">
                <div className="stat-icon stat-icon-purple">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <div className="stat-content">
                  <div className="stat-value">
                    {loading ? '...' : stats.totalStudentParticipated.toLocaleString()}
                  </div>
                  <div className="stat-label">Students Participated</div>
                </div>
              </div>

              <div className="stat-item">
                <div className="stat-icon stat-icon-pink">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                </div>
                <div className="stat-content">
                  <div className="stat-value">
                    {loading ? '...' : stats.totalCollegeConnected.toLocaleString()}
                  </div>
                  <div className="stat-label">Colleges Connected</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className="home-trending">
        <TrendingEvents limit={6} />
      </div>
    </div>
  );
}
