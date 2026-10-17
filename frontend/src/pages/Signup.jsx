import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import { GOOGLE_ENABLED } from '../config/google';
import { useAuth } from '../context/AuthContext';
import '../styles/Auth.css';

export default function Signup() {
  const { user, signup, loginWithGoogle } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    role: 'STUDENT',
    agreeToTerms: false
  });

  

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  // Password strength checker
  const getPasswordStrength = (password) => {
    if (!password) return { strength: 0, label: '', color: '' };
    
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/\d/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;
    
    if (strength <= 2) return { strength, label: 'Weak', color: '#ef4444' };
    if (strength === 3) return { strength, label: 'Fair', color: '#f59e0b' };
    if (strength === 4) return { strength, label: 'Good', color: '#10b981' };
    return { strength, label: 'Strong', color: '#10b981' };
  };

  useEffect(() => {
    if (user) navigate('/dashboard');
  }, [user, navigate]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    const newValue = type === 'checkbox' ? checked : value;
    
    // Clear submit error when user starts typing
    if (errors.submit) {
      setErrors(prev => ({ ...prev, submit: '' }));
    }
    
    setFormData(prev => ({
      ...prev,
      [name]: newValue
    }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
    
    // Real-time validation for specific fields
    if (name === 'username' && value) {
      // Validate email format on blur/change
      if (!/\S+@\S+\.\S+/.test(value)) {
        setErrors(prev => ({ ...prev, username: 'Email is invalid' }));
      }
    }
    
    // Check password match in real-time
    if (name === 'password' && formData.confirmPassword) {
      if (value !== formData.confirmPassword) {
        setErrors(prev => ({ ...prev, confirmPassword: 'Passwords do not match' }));
      } else {
        setErrors(prev => ({ ...prev, confirmPassword: '' }));
      }
    }
    
    if (name === 'confirmPassword' && value) {
      if (value !== formData.password) {
        setErrors(prev => ({ ...prev, confirmPassword: 'Passwords do not match' }));
      } else {
        setErrors(prev => ({ ...prev, confirmPassword: '' }));
      }
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
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    } else if (!/(?=.*[a-z])/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one lowercase letter';
    } else if (!/(?=.*[A-Z])/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one uppercase letter';
    } else if (!/(?=.*\d)/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one number';
    }
    
    // Confirm Password validation
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    // Terms agreement validation
    if (!formData.agreeToTerms) {
      newErrors.agreeToTerms = 'You must agree to the terms';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Signing up with Google uses the role chosen above: Organizer files an organizer request
  const handleGoogle = async (credentialResponse) => {
    setErrors({});
    try {
      const roles = formData.role === 'STUDENT' ? ['STUDENT'] : [formData.role, 'STUDENT'];
      await loginWithGoogle(credentialResponse, roles);
      navigate('/dashboard');
    } catch (error) {
      setErrors({ submit: error.message });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;

    setLoading(true);
    setErrors({}); // Clear all previous errors
    
    try {
      // Prepare roles array - if STUDENT is selected, only send STUDENT
      // Otherwise, send selected role + STUDENT
      const roles = formData.role === 'STUDENT' 
        ? ['STUDENT'] 
        : [formData.role, 'STUDENT'];

      console.log('Submitting signup with roles:', roles);

      await signup({
        username: formData.username,
        password: formData.password,
        roles: roles
      });
      
      console.log('Signup successful, redirecting to /dashboard');
      // Navigate happens automatically through AuthContext
    } catch (error) {
      console.error('Signup failed:', error.message);
      setErrors({ submit: error.message || 'Signup failed. Please try again.' });
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
            <h1 className="hero-title">Join CampusEvents<br/>Today!</h1>
            <p className="hero-subtitle">
              Create your account and start exploring amazing campus<br/>
              events, competitions, and opportunities.
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
            <h2 className="form-title">Create Your Account</h2>
            <p className="form-subtitle">Fill in your details to get started</p>

            <h3 className="form-section-title">Create your account</h3>

            <form onSubmit={handleSubmit} className="auth-form">
              {/* Role Selection */}
              <div className="form-group">
                <label>I am a *</label>
                <div className="role-selector">
                  <div 
                    className={`role-option ${formData.role === 'STUDENT' ? 'selected' : ''}`}
                    onClick={() => setFormData(prev => ({ ...prev, role: 'STUDENT' }))}
                  >
                    <span className="role-icon">🎓</span>
                    <span>Student</span>
                  </div>
                  <div 
                    className={`role-option ${formData.role === 'ORGANIZER' ? 'selected' : ''}`}
                    onClick={() => setFormData(prev => ({ ...prev, role: 'ORGANIZER' }))}
                  >
                    <span className="role-icon">👥</span>
                    <span>Organizer</span>
                  </div>
                  <div 
                    className={`role-option ${formData.role === 'ADMIN' ? 'selected' : ''}`}
                    onClick={() => setFormData(prev => ({ ...prev, role: 'ADMIN' }))}
                  >
                    <span className="role-icon">🛡️</span>
                    <span>Admin</span>
                  </div>
                </div>
              </div>

                {GOOGLE_ENABLED && (
                  <>
                    <div className="oauth-buttons">
                      <GoogleLogin
                        onSuccess={handleGoogle}
                        onError={() => setErrors({ submit: "Google sign-up didn't complete. If no Google window opened, allow pop-ups for this site (or turn off pop-up blocking extensions) and try again." })}
                        text="signup_with"
                        width={320}
                        use_fedcm_for_button
                        /* Chrome's own account chooser instead of a popup window that blockers can stop; other browsers keep the popup */
                      />
                    </div>
                    <div className="auth-divider">
                      <span>Or sign up with email</span>
                    </div>
                  </>
                )}

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
                    placeholder="Create a strong password"
                    className={errors.password ? 'error' : ''}
                  />
                </div>
                {errors.password && <span className="error-text">{errors.password}</span>}
                {formData.password && !errors.password && (
                  <div className="password-strength">
                    <div className="strength-bar-container">
                      <div 
                        className="strength-bar" 
                        style={{
                          width: `${(getPasswordStrength(formData.password).strength / 5) * 100}%`,
                          backgroundColor: getPasswordStrength(formData.password).color
                        }}
                      ></div>
                    </div>
                    <span 
                      className="strength-label" 
                      style={{ color: getPasswordStrength(formData.password).color }}
                    >
                      {getPasswordStrength(formData.password).label}
                    </span>
                  </div>
                )}
                <small className="hint-text">Must be at least 8 characters with uppercase, lowercase, and numbers</small>
              </div>

              {/* Confirm Password */}
              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm Password *</label>
                <div className="input-with-icon">
                  <span className="input-icon">🔒</span>
                  <input
                    type="password"
                    id="confirmPassword"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    placeholder="Re-enter your password"
                    className={errors.confirmPassword ? 'error' : ''}
                  />
                </div>
                {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
              </div>

              {/* Terms Checkbox */}
              <div className="form-group checkbox-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="agreeToTerms"
                    checked={formData.agreeToTerms}
                    onChange={handleChange}
                  />
                  <span>
                    I agree to the <Link to="/terms" className="auth-link">Terms of Service</Link> and{' '}
                    <Link to="/privacy" className="auth-link">Privacy Policy</Link>
                  </span>
                </label>
                {errors.agreeToTerms && <span className="error-text">{errors.agreeToTerms}</span>}
              </div>

              {errors.submit && (
                <div className="error-message">{errors.submit}</div>
              )}

              <button type="submit" className="submit-btn" disabled={loading}>
                {loading ? 'Creating Account...' : '→ Sign Up'}
              </button>
            </form>

            <p className="auth-switch">
              Already have an account? <Link to="/login" className="auth-link">Login</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
