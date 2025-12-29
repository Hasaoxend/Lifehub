import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { confirmPasswordReset, verifyPasswordResetCode } from 'firebase/auth';
import { auth, db } from '../firebase/config';
import { collection, query, where, getDocs, updateDoc } from 'firebase/firestore';
import { Key, Lock, Eye, EyeOff, Check, AlertTriangle, ArrowRight, Shield } from 'lucide-react';
import { InteractiveBackground } from '../components/InteractiveBackground';
import { checkPasswordStrength, MIN_REQUIRED_STRENGTH_SCORE } from '../utils/passwordUtils';
import '../pages/LoginPage.css';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const oobCode = searchParams.get('oobCode');
  
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState('');
  const [email, setEmail] = useState('');
  const [isValidCode, setIsValidCode] = useState<boolean | null>(null);

  const language = localStorage.getItem('language') || 'vi';

  const t = {
    title: language === 'vi' ? 'Đặt lại mật khẩu' : 'Reset Password',
    subtitle: language === 'vi' ? 'Nhập mật khẩu mới cho tài khoản của bạn' : 'Enter a new password for your account',
    newPassword: language === 'vi' ? 'Mật khẩu mới' : 'New Password',
    confirmPassword: language === 'vi' ? 'Xác nhận mật khẩu' : 'Confirm Password',
    reset: language === 'vi' ? 'Đặt lại mật khẩu' : 'Reset Password',
    resetting: language === 'vi' ? 'Đang cập nhật...' : 'Updating...',
    success: language === 'vi' ? 'Mật khẩu đã được đặt lại thành công!' : 'Password reset successfully!',
    backToLogin: language === 'vi' ? 'Quay lại đăng nhập' : 'Back to Login',
    invalidCode: language === 'vi' ? 'Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.' : 'Invalid or expired reset code.',
    verifying: language === 'vi' ? 'Đang xác minh mã...' : 'Verifying code...',
    mismatch: language === 'vi' ? 'Mật khẩu xác nhận không khớp' : 'Passwords do not match',
    tooShort: language === 'vi' ? 'Mật khẩu phải từ 8 ký tự trở lên' : 'Password must be at least 8 characters',
    weak: language === 'vi' ? 'Mật khẩu không đủ mạnh' : 'Password is not strong enough',
    strength: language === 'vi' ? 'Độ mạnh:' : 'Strength:'
  };

  useEffect(() => {
    if (!oobCode) {
      setIsValidCode(false);
      return;
    }

    const verify = async () => {
      try {
        const emailAddress = await verifyPasswordResetCode(auth, oobCode);
        setEmail(emailAddress);
        setIsValidCode(true);
      } catch (err) {
        console.error('Verify code error:', err);
        setIsValidCode(false);
      }
    };
    verify();
  }, [oobCode]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!oobCode) return;
    
    setError('');
    
    if (newPassword !== confirmPassword) {
      setError(t.mismatch);
      return;
    }

    const strength = checkPasswordStrength(newPassword);
    if (strength.score < MIN_REQUIRED_STRENGTH_SCORE) {
      setError(t.weak);
      return;
    }

    setIsSubmitting(true);
    try {
      await confirmPasswordReset(auth, oobCode, newPassword);

      // Successfully reset - mark password as strong if we have the email
      if (email) {
        try {
          const usersRef = collection(db, 'users');
          const q = query(usersRef, where('email', '==', email));
          const querySnapshot = await getDocs(q);
          
          if (!querySnapshot.empty) {
            const userDoc = querySnapshot.docs[0];
            await updateDoc(userDoc.ref, {
              passwordStrengthVerified: true
            });
          }
        } catch (err) {
          console.error('Error marking password as strong in Firestore:', err);
        }
      }

      setIsSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
    } catch (err: any) {
      console.error('Reset password error:', err);
      setError(language === 'vi' ? 'Có lỗi xảy ra. Vui lòng thử lại.' : 'An error occurred. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isValidCode === null) {
    return (
      <div className="auth-loading">
        <div className="loader"></div>
        <p style={{ marginTop: '16px', color: 'var(--text-secondary)' }}>{t.verifying}</p>
      </div>
    );
  }

  if (isValidCode === false) {
    return (
      <div className="auth-page">
        <InteractiveBackground />
        <div className="auth-container">
          <div className="auth-form-container" style={{ flex: 1 }}>
            <div className="auth-form-wrapper" style={{ textAlign: 'center' }}>
              <div style={{ color: 'var(--error)', marginBottom: '24px', display: 'flex', justifyContent: 'center' }}>
                <AlertTriangle size={64} />
              </div>
              <h2 style={{ marginBottom: '16px' }}>{language === 'vi' ? 'Lỗi' : 'Error'}</h2>
              <p className="auth-subtitle" style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '32px' }}>
                {t.invalidCode}
              </p>
              <button className="btn btn-primary" onClick={() => navigate('/login')} style={{ width: '100%' }}>
                {t.backToLogin}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (isSuccess) {
    return (
      <div className="auth-page">
        <InteractiveBackground />
        <div className="auth-container">
          <div className="auth-form-container" style={{ flex: 1 }}>
            <div className="auth-form-wrapper" style={{ textAlign: 'center' }}>
              <div style={{ color: 'var(--success)', marginBottom: '24px', display: 'flex', justifyContent: 'center' }}>
                <Check size={64} />
              </div>
              <h2 style={{ marginBottom: '16px' }}>{language === 'vi' ? 'Thành công!' : 'Success!'}</h2>
              <p className="auth-subtitle" style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '32px' }}>
                {t.success}
              </p>
              <button className="btn btn-primary" onClick={() => navigate('/login')} style={{ width: '100%' }}>
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
      <InteractiveBackground />
      <div className="auth-container">
        <div className="auth-branding">
          <div className="auth-branding-content">
            <div className="auth-logo">
              <div className="auth-logo-icon">
                <Key size={32} />
              </div>
              <h1>LifeHub</h1>
            </div>
            <p className="auth-tagline">{email ? `${language === 'vi' ? 'Đặt lại mật khẩu cho' : 'Reset password for'} ${email}` : t.subtitle}</p>
          </div>
        </div>

        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            <h2>{t.title}</h2>
            <p className="auth-subtitle">{t.subtitle}</p>

            {error && (
              <div className="auth-error">{error}</div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="input-group">
                <label className="input-label">{t.newPassword}</label>
                <div className="input-with-icon">
                  <Lock size={18} className="input-icon" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className="input"
                    placeholder="••••••••"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
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
                  {newPassword && (
                    <div className="password-strength" style={{ marginTop: '12px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '13px' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{t.strength}</span>
                        <span style={{ color: checkPasswordStrength(newPassword).color, fontWeight: 600 }}>
                          {checkPasswordStrength(newPassword).label}
                        </span>
                      </div>
                      <div className="strength-bar" style={{ height: '6px', background: 'var(--bg-card)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div
                          className="strength-fill"
                          style={{
                            height: '100%',
                            width: `${(checkPasswordStrength(newPassword).score / 4) * 100}%`,
                            backgroundColor: checkPasswordStrength(newPassword).color,
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
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: newPassword.length >= 8 ? 'var(--success)' : 'var(--text-secondary)' }}>
                            {newPassword.length >= 8 ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                            {language === 'vi' ? 'Tối thiểu 8 ký tự' : 'At least 8 characters'}
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: /[A-Z]/.test(newPassword) ? 'var(--success)' : 'var(--text-secondary)' }}>
                            {/[A-Z]/.test(newPassword) ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                            {language === 'vi' ? 'Chứa chữ hoa (A-Z)' : 'Contains uppercase (A-Z)'}
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: /[0-9]/.test(newPassword) ? 'var(--success)' : 'var(--text-secondary)' }}>
                            {/[0-9]/.test(newPassword) ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
                            {language === 'vi' ? 'Chứa số (0-9)' : 'Contains number (0-9)'}
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: /[^A-Za-z0-9]/.test(newPassword) ? 'var(--success)' : 'var(--text-secondary)' }}>
                            {/[^A-Za-z0-9]/.test(newPassword) ? <Check size={12} /> : <div style={{ width: 12, height: 12, borderRadius: '50%', border: '1px solid currentColor' }} />}
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
                </div>
              </div>

              <button 
                type="submit" 
                className="btn btn-primary btn-login"
                disabled={isSubmitting || !newPassword}
              >
                {isSubmitting ? (
                  <span className="btn-loading">{t.resetting}</span>
                ) : (
                  <>
                    {t.reset}
                    <ArrowRight size={18} />
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
