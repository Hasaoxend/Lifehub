import { useState, useEffect } from 'react';
import { Link, useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { sendPasswordResetEmail } from 'firebase/auth';
import { auth, db } from '../firebase/config';
import { collection, query, where, getDocs, updateDoc } from 'firebase/firestore';
import { Key, Mail, Lock, Eye, EyeOff, ArrowRight, ArrowLeft, Sun, Moon } from 'lucide-react';
import { InteractiveBackground } from '../components/InteractiveBackground';
import './LoginPage.css';

type Theme = 'light' | 'dark';
type Language = 'vi' | 'en';
type LoginStep = 'email' | 'password';

export function LoginPage() {
  const { user, signIn, loading } = useAuth();
  const navigate = useNavigate();
  
  const [step, setStep] = useState<LoginStep>('email');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Forgot password state
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotMessage, setForgotMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  
  // Theme state
  const [theme, setTheme] = useState<Theme>(() => {
    return (localStorage.getItem('theme') as Theme) || 'light';
  });
  
  // Language state  
  const [language, setLanguage] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  const toggleLanguage = () => {
    const newLang = language === 'vi' ? 'en' : 'vi';
    setLanguage(newLang);
    localStorage.setItem('language', newLang);
  };

  const handleContinue = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setForgotMessage(null);
    
    if (!email || !email.includes('@')) {
      setError(language === 'vi' ? 'Vui lòng nhập email hợp lệ' : 'Please enter a valid email');
      return;
    }
    
    setStep('password');
  };

  const handleBackToEmail = () => {
    setStep('email');
    setPassword('');
    setError('');
    setForgotMessage(null);
  };

  const handleForgotPassword = async () => {
    if (!email) {
      setForgotMessage({ 
        type: 'error', 
        text: language === 'vi' ? 'Vui lòng nhập email trước' : 'Please enter your email first' 
      });
      return;
    }
    
    try {
      setForgotLoading(true);
      setForgotMessage(null);
      
      const actionCodeSettings = {
        url: `${window.location.origin}/reset-password`,
        handleCodeInApp: true,
      };

      await sendPasswordResetEmail(auth, email, actionCodeSettings);

      // Reset the flag for this email so they are forced to change it even if they log back in with old password
      try {
        const usersRef = collection(db, 'users');
        const q = query(usersRef, where('email', '==', email));
        const querySnapshot = await getDocs(q);
        
        if (!querySnapshot.empty) {
          const userDoc = querySnapshot.docs[0];
          await updateDoc(userDoc.ref, {
            passwordStrengthVerified: false
          });
        }
      } catch (err) {
        console.error('Error resetting password flag in Firestore:', err);
      }

      setForgotMessage({ 
        type: 'success', 
        text: language === 'vi' ? `Email đặt lại mật khẩu đã gửi đến ${email}` : `Password reset email sent to ${email}` 
      });
    } catch (err: any) {
      console.error('Forgot password error:', err);
      if (err.code === 'auth/user-not-found') {
        setForgotMessage({ type: 'error', text: language === 'vi' ? 'Email không tồn tại trong hệ thống' : 'Email not found' });
      } else if (err.code === 'auth/invalid-email') {
        setForgotMessage({ type: 'error', text: language === 'vi' ? 'Email không hợp lệ' : 'Invalid email' });
      } else {
        setForgotMessage({ type: 'error', text: language === 'vi' ? 'Có lỗi xảy ra. Vui lòng thử lại.' : 'An error occurred. Please try again.' });
      }
    } finally {
      setForgotLoading(false);
    }
  };

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
    setForgotMessage(null);
    setIsSubmitting(true);

    try {
      await signIn(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      console.error('Login error:', err);
      if (err.code === 'auth/user-not-found') {
        setError(language === 'vi' ? 'Tài khoản không tồn tại' : 'Account not found');
      } else if (err.code === 'auth/wrong-password') {
        setError(language === 'vi' ? 'Mật khẩu không chính xác' : 'Incorrect password');
      } else if (err.code === 'auth/invalid-email') {
        setError(language === 'vi' ? 'Email không hợp lệ' : 'Invalid email');
      } else if (err.code === 'auth/too-many-requests') {
        setError(language === 'vi' ? 'Quá nhiều lần thử. Vui lòng thử lại sau' : 'Too many attempts. Please try again later');
      } else {
        setError(language === 'vi' ? 'Đăng nhập thất bại. Vui lòng thử lại' : 'Login failed. Please try again');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const t = {
    title: language === 'vi' ? 'Đăng nhập' : 'Sign In',
    subtitleEmail: language === 'vi' ? 'Nhập email của bạn để tiếp tục' : 'Enter your email to continue',
    subtitlePassword: language === 'vi' ? 'Nhập mật khẩu để đăng nhập' : 'Enter your password to sign in',
    email: 'Email',
    password: language === 'vi' ? 'Mật khẩu' : 'Password',
    continue: language === 'vi' ? 'Tiếp tục' : 'Continue',
    forgotPassword: language === 'vi' ? 'Quên mật khẩu?' : 'Forgot password?',
    sendingReset: language === 'vi' ? 'Đang gửi...' : 'Sending...',
    login: language === 'vi' ? 'Đăng nhập' : 'Sign In',
    loggingIn: language === 'vi' ? 'Đang đăng nhập...' : 'Signing in...',
    noAccount: language === 'vi' ? 'Chưa có tài khoản?' : "Don't have an account?",
    register: language === 'vi' ? 'Đăng ký ngay' : 'Register now',
    changeEmail: language === 'vi' ? 'Đổi email' : 'Change email',
    tagline: language === 'vi' 
      ? 'Quản lý mật khẩu và năng suất cá nhân của bạn một cách an toàn và hiệu quả.'
      : 'Manage your passwords and personal productivity securely and efficiently.',
    featureAES: language === 'vi' ? 'Mã hóa AES-256' : 'AES-256 Encryption',
    featureSync: language === 'vi' ? 'Đồng bộ đa nền tảng' : 'Cross-platform sync',
    feature2FA: 'Authenticator 2FA'
  };


  return (
    <div className="auth-page">
      {/* Top Right Controls */}
      <div className="auth-top-controls">
        <button className="control-btn theme-btn" onClick={toggleTheme} title={theme === 'light' ? 'Dark mode' : 'Light mode'}>

          {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
        </button>
        <button className="control-btn lang-btn" onClick={toggleLanguage} title={language === 'vi' ? 'English' : 'Tiếng Việt'}>
          {language === 'vi' ? '🇻🇳' : '🇺🇸'}
        </button>
      </div>
      
      <InteractiveBackground />
      <div className="auth-container">

        {/* Left Side - Branding */}
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
                <span>{t.featureAES}</span>
              </div>
              <div className="feature-item">
                <span className="feature-icon">•</span>
                <span>{t.featureSync}</span>
              </div>
              <div className="feature-item">
                <span className="feature-icon">•</span>
                <span>{t.feature2FA}</span>
              </div>
            </div>
          </div>
        </div>



        {/* Right Side - Form */}
        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            <h2>{t.title}</h2>
            <p className="auth-subtitle">
              {step === 'email' ? t.subtitleEmail : t.subtitlePassword}
            </p>

            {error && (
              <div className="auth-error">
                {error}
              </div>
            )}

            {/* Step 1: Email */}
            {step === 'email' && (
              <form onSubmit={handleContinue} className="auth-form">
                <div className="input-group">
                  <label className="input-label">{t.email}</label>
                  <div className="input-with-icon">
                    <Mail size={18} className="input-icon" />
                    <input
                      type="email"
                      className="input"
                      placeholder="example@email.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>
                </div>

                <button 
                  type="submit" 
                  className="btn btn-primary btn-login"
                >
                  {t.continue}
                  <ArrowRight size={18} />
                </button>

                <p className="auth-footer">
                  {t.noAccount}{' '}
                  <Link to="/register">{t.register}</Link>
                </p>
              </form>
            )}

            {/* Step 2: Password */}
            {step === 'password' && (
              <form onSubmit={handleSubmit} className="auth-form">
                {/* Show current email with change button */}
                <div className="email-display">
                  <div className="email-info">
                    <Mail size={16} />
                    <span>{email}</span>
                  </div>
                  <button 
                    type="button" 
                    className="change-email-btn"
                    onClick={handleBackToEmail}
                  >
                    {t.changeEmail}
                  </button>
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
                      autoFocus
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

                <div className="auth-options">
                  <button 
                    type="button" 
                    className="forgot-link"
                    onClick={handleForgotPassword}
                    disabled={forgotLoading}
                  >
                    {forgotLoading ? t.sendingReset : t.forgotPassword}
                  </button>
                </div>
                
                {forgotMessage && (
                  <div className={`auth-message ${forgotMessage.type}`}>
                    {forgotMessage.text}
                  </div>
                )}

                <button 
                  type="submit" 
                  className="btn btn-primary btn-login"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <span className="btn-loading">{t.loggingIn}</span>
                  ) : (
                    <>
                      {t.login}
                      <ArrowRight size={18} />
                    </>
                  )}
                </button>
              </form>
            )}
          </div>
        </div>


      </div>
    </div>
  );
}
