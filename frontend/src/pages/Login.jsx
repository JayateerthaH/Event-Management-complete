import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../context/AuthContext';
import '../styles/Auth.css';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    rememberMe: false
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) navigate('/dashboard');
  }, [user, navigate]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    // Clear errors when user starts typing
    if (errors[name] || errors.submit) {
      setErrors(prev => ({ ...prev, [name]: '', submit: '' }));
    }
  };

  const validate = () => {
    const newErrors = {};
    
    // Email validation
    if (!formData.username.trim()) {
      newErrors.username = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.username)) {
      newErrors.username = 'Please enter a valid email address';
    }
    
    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;

    setLoading(true);
    setErrors({}); // Clear all previous errors
    
    try {
      await login({
        username: formData.username,
        password: formData.password
      });
      console.log('Login successful, redirecting to /dashboard');
      // Navigate happens automatically through AuthContext
    } catch (error) {
      console.error('Login failed:', error.message);
      setErrors({ submit: error.message || 'Login failed. Please check your credentials.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-split-layout">
        {/* Left Panel */}
        <div className="auth-left-panel">
          <div className="auth-left-content">
            <div className="brand-header">
              <span className="brand-icon">🎫</span>
              <span className="brand-name">CampusEvents</span>
            </div>
            <h1 className="hero-title">Welcome Back!</h1>
            <p className="hero-subtitle">
              Login to discover amazing events, participate in<br/>
              competitions, and connect with your campus community.
            </p>
            <img 
              src="https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=600&auto=format&fit=crop"
              alt="Campus Event"
              className="hero-image"
            />
          </div>
        </div>

        {/* Right Panel */}
        <div className="auth-right-panel">
          <div className="auth-form-container">
            <h2 className="form-title">Login to Your Account</h2>
            <p className="form-subtitle">Enter your credentials to access your dashboard</p>

            {/* Temporarily hidden until Google OAuth is configured
            <div className="oauth-buttons">
              <GoogleLogin
                onSuccess={}
                onError={() => console.error('Login failed')}
                text="continue_with"
                width="100%"
              />
            </div>

            <div className="auth-divider">
              <span>Or login with email</span>
            </div>
            */}

            <h3 className="form-section-title">Login with your credentials</h3>

            <form onSubmit={handleSubmit} className="auth-form">
              {/* Email */}
              <div className="form-group">
                <label htmlFor="username">Email Address *</label>
                <div className="input-with-icon">
                  <span className="input-icon">✉</span>
                  <input
                    type="email"
                    id="username"
                    name="username"
                    value={formData.username}
                    onChange={handleChange}
                    placeholder="your.email@example.com"
                    className={errors.username ? 'error' : ''}
                  />
                </div>
                {errors.username && <span className="error-text">{errors.username}</span>}
              </div>

              {/* Password */}
              <div className="form-group">
                <label htmlFor="password">Password *</label>
                <div className="input-with-icon">
                  <span className="input-icon">🔒</span>
                  <input
                    type="password"
                    id="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="Enter your password"
                    className={errors.password ? 'error' : ''}
                  />
                </div>
                {errors.password && <span className="error-text">{errors.password}</span>}
              </div>

              {/* Remember Me & Forgot Password */}
              <div className="form-row space-between">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="rememberMe"
                    checked={formData.rememberMe}
                    onChange={handleChange}
                  />
                  <span>Remember me</span>
                </label>
                {/* Password reset is not implemented on the backend; the link pointed at a missing route */}
              </div>

              {errors.submit && (
                <div className="error-message">{errors.submit}</div>
              )}

              <button type="submit" className="submit-btn" disabled={loading}>
                {loading ? 'Logging in...' : '→ Login'}
              </button>
            </form>

            <p className="auth-switch">
              Don&apos;t have an account? <Link to="/signup" className="auth-link">Sign Up</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
