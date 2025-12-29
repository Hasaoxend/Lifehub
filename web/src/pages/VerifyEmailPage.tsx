import { useState, useEffect } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Mail, RefreshCw, LogOut, ShieldCheck } from 'lucide-react';
import { sendEmailVerification } from 'firebase/auth';

type Language = 'vi' | 'en';

export function VerifyEmailPage() {
  const { user, signOut, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
  const [countdown, setCountdown] = useState(0);
  const [language] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  const t = {
    tagline: language === 'vi' 
      ? 'Chúng tôi bảo vệ dữ liệu của bạn bằng mã hóa đầu cuối. Xác thực email là bước đầu tiên để đảm bảo tài khoản của bạn an toàn.'
      : 'We protect your data with end-to-end encryption. Email verification is the first step to ensure your account is secure.',
    title: language === 'vi' ? 'Xác thực email của bạn' : 'Verify your email',
    subtitle: language === 'vi'
      ? 'Vui lòng nhấp vào liên kết trong email chúng tôi đã gửi đến'
      : 'Please click the link in the email we sent to',
    toActivate: language === 'vi' ? 'để kích hoạt tài khoản.' : 'to activate your account.',
    verified: language === 'vi' ? 'Tôi đã xác thực' : "I've verified",
    resendAfter: language === 'vi' ? 'Gửi lại sau' : 'Resend after',
    resend: language === 'vi' ? 'Gửi lại email xác thực' : 'Resend verification email',
    signOut: language === 'vi' ? 'Đăng xuất' : 'Sign out',
    notVerified: language === 'vi' 
      ? 'Email vẫn chưa được xác thực. Vui lòng kiểm tra hộp thư của bạn.'
      : 'Email not verified yet. Please check your inbox.',
    error: language === 'vi' ? 'Có lỗi xảy ra. Vui lòng thử lại.' : 'An error occurred. Please try again.',
    sent: language === 'vi' ? 'Email xác thực mới đã được gửi!' : 'New verification email sent!',
    tooMany: language === 'vi' 
      ? 'Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau.'
      : 'Too many requests. Please try again later.',
    cantSend: language === 'vi' 
      ? 'Không thể gửi email. Vui lòng thử lại sau.'
      : 'Could not send email. Please try again later.'
  };

  useEffect(() => {
    let timer: number;
    if (countdown > 0) {
      timer = window.setTimeout(() => setCountdown(countdown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [countdown]);

  if (authLoading) return <div className="auth-loading"><div className="loader"></div></div>;
  if (!user) return <Navigate to="/login" replace />;
  if (user.emailVerified) return <Navigate to="/dashboard" replace />;

  const handleRefresh = async () => {
    setLoading(true);
    try {
      await user.reload();
      if (user.emailVerified) {
        navigate('/dashboard');
      } else {
        setMessage({ type: 'error', text: t.notVerified });
      }
    } catch (err) {
      console.error('Error reloading user:', err);
      setMessage({ type: 'error', text: t.error });
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0) return;
    
    setLoading(true);
    try {
      await sendEmailVerification(user);
      setMessage({ type: 'success', text: t.sent });
      setCountdown(60);
    } catch (err: any) {
      console.error('Error resending verification:', err);
      if (err.code === 'auth/too-many-requests') {
        setMessage({ type: 'error', text: t.tooMany });
      } else {
        setMessage({ type: 'error', text: t.cantSend });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-branding">
          <div className="auth-branding-content">
            <div className="auth-logo">
              <div className="auth-logo-icon">
                <ShieldCheck size={32} />
              </div>
              <h1>LifeHub</h1>
            </div>
            <p className="auth-tagline">
              {t.tagline}
            </p>
          </div>
        </div>

        <div className="auth-form-container">
          <div className="auth-form-wrapper" style={{ textAlign: 'center' }}>
            <div className="success-icon-large" style={{ color: 'var(--primary)', marginBottom: '24px', display: 'flex', justifyContent: 'center' }}>
              <Mail size={64} />
            </div>
            <h2 style={{ marginBottom: '16px' }}>{t.title}</h2>
            <p className="auth-subtitle" style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '32px' }}>
              {t.subtitle} <strong>{user.email}</strong> {t.toActivate}
            </p>

            {message && (
              <div className={`auth-message ${message.type}`} style={{ marginBottom: '24px' }}>
                {message.text}
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button 
                className="btn btn-primary"
                onClick={handleRefresh}
                disabled={loading}
                style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
              >
                {loading ? <RefreshCw className="spin" size={18} /> : <RefreshCw size={18} />}
                {t.verified}
              </button>

              <button 
                className="btn btn-outline"
                onClick={handleResend}
                disabled={loading || countdown > 0}
                style={{ width: '100%' }}
              >
                {countdown > 0 ? `${t.resendAfter} (${countdown}s)` : t.resend}
              </button>

              <button 
                className="btn-text"
                onClick={() => signOut()}
                style={{ 
                  marginTop: '12px', 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'center', 
                  gap: '8px',
                  color: 'var(--text-muted)',
                  border: 'none',
                  background: 'none',
                  cursor: 'pointer'
                }}
              >
                <LogOut size={16} />
                {t.signOut}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
