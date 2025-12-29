import { useState, useEffect } from 'react';

import { useNavigate, useSearchParams } from 'react-router-dom';

import { useAuth } from '../hooks/useAuth';

import { checkPasswordStrength, MIN_REQUIRED_STRENGTH_SCORE } from '../utils/passwordUtils';
import { useLanguage } from '../hooks/useLanguage';
import { reauthenticateWithCredential, EmailAuthProvider, updatePassword } from 'firebase/auth';
import { auth } from '../firebase/config';
import { Lock, Eye, EyeOff, Check, AlertTriangle, ArrowLeft, ArrowRight, Shield, LogOut } from 'lucide-react';
import { InteractiveBackground } from '../components/InteractiveBackground';
import '../pages/LoginPage.css';

export function ChangePasswordPage() {
  const { user, markPasswordAsStrong, signOut } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isForced = searchParams.get('forced') === 'true';
  const language = useLanguage();

  const [step, setStep] = useState<'verify' | 'newPassword'>(isForced ? 'newPassword' : 'verify');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  const t = {
    title: language === 'vi' ? 'Đổi mật khẩu' : 'Change Password',
    forcedTitle: language === 'vi' ? 'Yêu cầu đổi mật khẩu' : 'Password Change Required',
    forcedSubtitle: language === 'vi' ? 'Mật khẩu hiện tại của bạn không đủ mạnh. Vui lòng cập nhật để tiếp tục.' : 'Your current password is not strong enough. Please update to continue.',
    verifySubtitle: language === 'vi' ? 'Nhập mật khẩu hiện tại để xác thực' : 'Enter current password to verify',
    newSubtitle: language === 'vi' ? 'Nhập mật khẩu mới (Mạnh)' : 'Enter new password (Strong)',
    currentPass: language === 'vi' ? 'Mật khẩu hiện tại' : 'Current Password',
    newPass: language === 'vi' ? 'Mật khẩu mới' : 'New Password',
    confirmPass: language === 'vi' ? 'Xác nhận mật khẩu' : 'Confirm Password',
    update: language === 'vi' ? 'Cập nhật mật khẩu' : 'Update Password',
    updating: language === 'vi' ? 'Đang cập nhật...' : 'Updating...',
    verifying: language === 'vi' ? 'Đang xác thực...' : 'Verifying...',
    verify: language === 'vi' ? 'Xác thực' : 'Verify',
    successTitle: language === 'vi' ? 'Thành công!' : 'Success!',
    successDesc: language === 'vi' ? 'Mật khẩu đã được cập nhật. Bạn sẽ được đăng xuất để đăng nhập lại.' : 'Password updated. You will be logged out to sign in again.',
    mismatch: language === 'vi' ? 'Mật khẩu không khớp' : 'Passwords do not match',
    tooWeak: language === 'vi' ? 'Mật khẩu phải đạt độ mạnh tối đa' : 'Password must meet maximum strength requirements',
    wrongPass: language === 'vi' ? 'Mật khẩu hiện tại không chính xác' : 'Incorrect current password',
    generalError: language === 'vi' ? 'Có lỗi xảy ra. Vui lòng thử lại.' : 'An error occurred. Please try again.',
    strength: language === 'vi' ? 'Độ mạnh:' : 'Strength:'
  };

  const strength = checkPasswordStrength(newPassword);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      const credential = EmailAuthProvider.credential(user?.email!, currentPassword);
      await reauthenticateWithCredential(auth.currentUser!, credential);
      setStep('newPassword');
    } catch (err: any) {
      console.error('Verify error:', err);
      setError(t.wrongPass);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (newPassword !== confirmPassword) {
      setError(t.mismatch);
      return;
    }

    if (strength.score < MIN_REQUIRED_STRENGTH_SCORE) {
      setError(t.tooWeak);
      return;
    }

    setIsSubmitting(true);
    try {
      await updatePassword(auth.currentUser!, newPassword);
      await markPasswordAsStrong();
      setIsSuccess(true);
      setTimeout(async () => {
        await signOut();
        navigate('/login');
      }, 3000);
    } catch (err: any) {
      console.error('Update password error:', err);
      setError(t.generalError);
    } finally {
      setIsSubmitting(false);
    }
  };

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
              <h2 style={{ marginBottom: '16px' }}>{t.successTitle}</h2>
              <p className="auth-subtitle" style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '32px' }}>
                {t.successDesc}
              </p>
              <div className="loader" style={{ margin: '0 auto' }}></div>
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
                <Shield size={32} />
              </div>
              <h1>LifeHub</h1>
            </div>
            <p className="auth-tagline">
              {isForced ? t.forcedSubtitle : (step === 'verify' ? t.verifySubtitle : t.newSubtitle)}
            </p>
          </div>
        </div>

        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            {!isForced && step === 'verify' && (
               <button className="back-to-settings" onClick={() => navigate('/settings')} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', marginBottom: '24px' }}>
                 <ArrowLeft size={18} />
                 {language === 'vi' ? 'Quay lại' : 'Back'}
               </button>
            )}
            
            <h2>{isForced ? t.forcedTitle : t.title}</h2>
            <p className="auth-subtitle">
              {step === 'verify' ? t.verifySubtitle : t.newSubtitle}
            </p>

            {error && (
              <div className="auth-error">{error}</div>
            )}

            {step === 'verify' ? (
              <form onSubmit={handleVerify} className="auth-form">
                <div className="input-group">
                  <label className="input-label">{t.currentPass}</label>
                  <div className="input-with-icon">
                    <Lock size={18} className="input-icon" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      className="input"
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
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
                <button type="submit" className="btn btn-primary btn-login" disabled={isSubmitting}>
                   {isSubmitting ? t.verifying : t.verify}
                   <ArrowRight size={18} />
                </button>
              </form>
            ) : (
              <form onSubmit={handleChangePassword} className="auth-form">
                <div className="input-group">
                  <label className="input-label">{t.newPass}</label>
                  <div className="input-with-icon">
                    <Lock size={18} className="input-icon" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      className="input"
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
                        <span style={{ color: strength.color, fontWeight: 600 }}>
                          {strength.label}
                        </span>
                      </div>
                      <div className="strength-bar" style={{ height: '6px', background: 'var(--bg-card)', borderRadius: '3px', overflow: 'hidden' }}>
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
                  <label className="input-label">{t.confirmPass}</label>
                  <div className="input-with-icon">
                    <Lock size={18} className="input-icon" />
                    <input
                      type="password"
                      className="input"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                </div>

                <button type="submit" className="btn btn-primary btn-login" disabled={isSubmitting || strength.score < MIN_REQUIRED_STRENGTH_SCORE}>
                   {isSubmitting ? t.updating : t.update}
                   <LogOut size={18} />
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
