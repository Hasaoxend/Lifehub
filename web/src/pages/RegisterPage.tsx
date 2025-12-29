import { useState } from 'react';
import { Link, useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Key, Mail, Lock, Eye, EyeOff, ArrowRight, Check, AlertTriangle } from 'lucide-react';
import { checkPasswordStrength, MIN_REQUIRED_STRENGTH_SCORE } from '../utils/passwordUtils';
import './LoginPage.css';

type Language = 'vi' | 'en';

export function RegisterPage() {
  const { user, signUp, loading } = useAuth();
  const navigate = useNavigate();
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [language] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  const t = {
    tagline: language === 'vi'
      ? 'Tạo tài khoản miễn phí và bắt đầu quản lý mật khẩu của bạn an toàn hơn.'
      : 'Create a free account and start managing your passwords more securely.',
    featureFree: language === 'vi' ? 'Hoàn toàn miễn phí' : 'Completely free',
    featureE2E: language === 'vi' ? 'Bảo mật end-to-end' : 'End-to-end security',
    featureSync: language === 'vi' ? 'Đồng bộ với Android' : 'Sync with Android',
    createAccount: language === 'vi' ? 'Tạo tài khoản' : 'Create Account',
    subtitle: language === 'vi' ? 'Điền thông tin bên dưới để bắt đầu.' : 'Fill in the information below to get started.',
    password: language === 'vi' ? 'Mật khẩu' : 'Password',
    confirmPassword: language === 'vi' ? 'Xác nhận mật khẩu' : 'Confirm Password',
    register: language === 'vi' ? 'Đăng ký' : 'Register',
    registering: language === 'vi' ? 'Đang tạo tài khoản...' : 'Creating account...',
    haveAccount: language === 'vi' ? 'Đã có tài khoản?' : 'Already have an account?',
    login: language === 'vi' ? 'Đăng nhập' : 'Sign In',
    successTitle: language === 'vi' ? 'Đăng ký thành công!' : 'Registration Successful!',
    successDesc: language === 'vi'
      ? 'Chúng tôi đã gửi một email xác thực đến'
      : 'We have sent a verification email to',
    successDesc2: language === 'vi'
      ? 'Vui lòng kiểm tra hộp thư (và cả thư rác) để kích hoạt tài khoản của bạn.'
      : 'Please check your inbox (and spam folder) to activate your account.',
    backToLogin: language === 'vi' ? 'Quay lại đăng nhập' : 'Back to Login',
    req8Chars: language === 'vi' ? 'Ít nhất 8 ký tự' : 'At least 8 characters',
    reqUpper: language === 'vi' ? 'Có chữ hoa' : 'Has uppercase',
    reqLower: language === 'vi' ? 'Có chữ thường' : 'Has lowercase',
    reqNumber: language === 'vi' ? 'Có số' : 'Has number',
    reqSpecial: language === 'vi' ? 'Có ký tự đặc biệt' : 'Has special character',
    errorMismatch: language === 'vi' ? 'Mật khẩu xác nhận không khớp' : 'Passwords do not match',
    errorWeak: language === 'vi' ? 'Mật khẩu không đủ mạnh' : 'Password is not strong enough',
    errorInUse: language === 'vi' ? 'Email đã được sử dụng' : 'Email already in use',
    errorInvalid: language === 'vi' ? 'Email không hợp lệ' : 'Invalid email',
    errorWeakFirebase: language === 'vi' ? 'Mật khẩu quá yếu' : 'Password too weak',
    errorGeneral: language === 'vi' ? 'Đăng ký thất bại. Vui lòng thử lại' : 'Registration failed. Please try again'
  };

  const strength = checkPasswordStrength(password);
  const allRequirementsMet = strength.score >= MIN_REQUIRED_STRENGTH_SCORE;

  if (loading) {
    return (
      <div className="auth-loading">
        <div className="loader"></div>
      </div>
    );
  }

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError(t.errorMismatch);
      return;
    }

    if (!allRequirementsMet) {
      setError(t.errorWeak);
      return;
    }

    setIsSubmitting(true);

    try {
      await signUp(email, password);
      setIsSuccess(true);
    } catch (err: any) {
      console.error('Register error:', err);
      if (err.code === 'auth/email-already-in-use') {
        setError(t.errorInUse);
      } else if (err.code === 'auth/invalid-email') {
        setError(t.errorInvalid);
      } else if (err.code === 'auth/weak-password') {
        setError(t.errorWeakFirebase);
      } else {
        setError(t.errorGeneral);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="auth-page">
        <div className="auth-container">
          <div className="auth-form-container" style={{ flex: 1 }}>
            <div className="auth-form-wrapper" style={{ textAlign: 'center' }}>
              <div className="success-icon-large" style={{ color: 'var(--success)', marginBottom: '24px', display: 'flex', justifyContent: 'center' }}>
                <Check size={64} />
              </div>
              <h2 style={{ marginBottom: '16px' }}>{t.successTitle}</h2>
              <p className="auth-subtitle" style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '32px' }}>
                {t.successDesc} <strong>{email}</strong>. {t.successDesc2}
              </p>
              <button 
                className="btn btn-primary"
                onClick={() => navigate('/login')}
                style={{ width: '100%' }}
              >
                {t.backToLogin}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-branding">
          <div className="auth-branding-content">
            <div className="auth-logo">
              <div className="auth-logo-icon">
                <Key size={32} />
              </div>
              <h1>LifeHub</h1>
            </div>
            <p className="auth-tagline">{t.tagline}</p>
            <div className="auth-features">
              <div className="feature-item">
                <span className="feature-icon">•</span>
                <span>{t.featureFree}</span>
              </div>
              <div className="feature-item">
                <span className="feature-icon">•</span>
                <span>{t.featureE2E}</span>
              </div>
              <div className="feature-item">
                <span className="feature-icon">•</span>
                <span>{t.featureSync}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            <h2>{t.createAccount}</h2>
            <p className="auth-subtitle">{t.subtitle}</p>

            {error && (
              <div className="auth-error">{error}</div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="input-group">
                <label className="input-label">Email</label>
                <div className="input-with-icon">
                  <Mail size={18} className="input-icon" />
                  <input
                    type="email"
                    className="input"
                    placeholder="example@email.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    disabled={isSubmitting}
                  />
                </div>
              </div>

              <div className="input-group">
                <label className="input-label">{t.password}</label>
                <div className="input-with-icon">
                  <Lock size={18} className="input-icon" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className="input"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    disabled={isSubmitting}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                
                {password && (
                  <div className="password-strength" style={{ marginTop: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '12px' }}>
                      <span style={{ color: 'var(--text-secondary)' }}>
                        {language === 'vi' ? 'Độ mạnh:' : 'Strength:'}
                      </span>
                      <span style={{ color: strength.color, fontWeight: 600 }}>
                        {strength.label}
                      </span>
                    </div>
                    <div className="strength-bar" style={{ height: '4px', background: 'var(--bg-card)', borderRadius: '2px', overflow: 'hidden' }}>
                      <div
                        className="strength-fill"
                        style={{
                          height: '100%',
                          width: `${(strength.score / 4) * 100}%`,
                          backgroundColor: strength.color,
                          transition: 'all 0.3s ease'
                        }}
                      />
                    </div>
                    
                    {/* Password Requirements Checklist */}
                    <div className="password-requirements" style={{ marginTop: '12px', fontSize: '12px', background: 'rgba(255,255,255,0.05)', padding: '10px', borderRadius: '8px' }}>
                      <div style={{ fontWeight: 600, marginBottom: '8px', color: 'var(--text-primary)', fontSize: '11px', textTransform: 'uppercase', opacity: 0.7 }}>
                        {language === 'vi' ? 'Điều kiện mật khẩu:' : 'Password Requirements:'}
                      </div>
                      <div style={{ display: 'grid', gap: '6px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: password.length >= 8 ? 'var(--success)' : 'var(--text-secondary)' }}>
                          {password.length >= 8 ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                          {language === 'vi' ? 'Tối thiểu 8 ký tự' : 'At least 8 characters'}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: /[A-Z]/.test(password) ? 'var(--success)' : 'var(--text-secondary)' }}>
                          {/[A-Z]/.test(password) ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                          {language === 'vi' ? 'Chứa chữ hoa (A-Z)' : 'Contains uppercase (A-Z)'}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: /[0-9]/.test(password) ? 'var(--success)' : 'var(--text-secondary)' }}>
                          {/[0-9]/.test(password) ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                          {language === 'vi' ? 'Chứa số (0-9)' : 'Contains number (0-9)'}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: /[^A-Za-z0-9]/.test(password) ? 'var(--success)' : 'var(--text-secondary)' }}>
                          {/[^A-Za-z0-9]/.test(password) ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                          {language === 'vi' ? 'Chứa ký tự đặc biệt (!@#...)' : 'Contains special character (!@#...)'}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">{t.confirmPassword}</label>
                <div className="input-with-icon">
                  <Lock size={18} className="input-icon" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className="input"
                    placeholder="••••••••"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    disabled={isSubmitting}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <button 
                type="submit" 
                className="btn btn-primary btn-login"
                disabled={isSubmitting || !allRequirementsMet}
              >
                {isSubmitting ? (
                  <span className="btn-loading">{t.registering}</span>
                ) : (
                  <>
                    {t.register}
                    <ArrowRight size={18} />
                  </>
                )}
              </button>
            </form>

            <p className="auth-footer">
              {t.haveAccount}{' '}
              <Link to="/login">{t.login}</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
