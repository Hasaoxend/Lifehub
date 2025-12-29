import { useState, useEffect } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Shield, Lock, Check, Copy, AlertCircle, ArrowRight } from 'lucide-react';
import { generateRecoveryCode } from '../utils/encryption';
import { doc, setDoc } from 'firebase/firestore';
import { db } from '../firebase/config';

type Language = 'vi' | 'en';

export function PasscodeSetupPage() {
  const { user, needsPasscodeSetup, setupEncryption, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  
  const [step, setStep] = useState(1);
  const [passcode, setPasscode] = useState('');
  const [confirmPasscode, setConfirmPasscode] = useState('');
  const [recoveryCode, setRecoveryCode] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [confirmedSaved, setConfirmedSaved] = useState(false);
  const [language] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  const t = {
    tagline: language === 'vi' 
      ? 'Bảo mật dữ liệu của bạn bằng mã hóa cấp độ quân đội. Bạn là người duy nhất có chìa khóa.'
      : 'Secure your data with military-grade encryption. You are the only one with the key.',
    step1Title: language === 'vi' ? 'Thiết lập mã PIN bảo mật' : 'Set Up Security PIN',
    step1Desc: language === 'vi'
      ? 'LifeHub sử dụng mã hóa đầu cuối (E2EE). Bạn cần tạo một mã PIN 6 số để bảo vệ mật khẩu, ghi chú và các thông tin cá nhân khác.'
      : 'LifeHub uses end-to-end encryption (E2EE). You need to create a 6-digit PIN to protect your passwords, notes, and other personal information.',
    step1Info: language === 'vi'
      ? 'Mã PIN này độc lập với mật khẩu đăng nhập. Bạn sẽ dùng nó để mở khóa ứng dụng mỗi lần truy cập.'
      : 'This PIN is separate from your login password. You will use it to unlock the app each time you access it.',
    startSetup: language === 'vi' ? 'Bắt đầu thiết lập' : 'Start Setup',
    step2Title: language === 'vi' ? 'Tạo mã PIN 6 số' : 'Create 6-Digit PIN',
    step2Desc: language === 'vi' 
      ? 'Chọn một mã PIN khó đoán nhưng dễ nhớ với bạn.'
      : 'Choose a PIN that is hard to guess but easy for you to remember.',
    pinLabel: language === 'vi' ? 'Mã PIN (6 chữ số)' : 'PIN (6 digits)',
    confirmPinLabel: language === 'vi' ? 'Xác nhận mã PIN' : 'Confirm PIN',
    next: language === 'vi' ? 'Tiếp theo' : 'Next',
    step3Title: language === 'vi' ? 'Mã khôi phục của bạn' : 'Your Recovery Code',
    step3Desc: language === 'vi'
      ? 'Nếu bạn quên mã PIN, đây là cách DUY NHẤT để cứu dữ liệu của bạn.'
      : 'If you forget your PIN, this is the ONLY way to recover your data.',
    copied: language === 'vi' ? 'Đã sao chép' : 'Copied',
    copyCode: language === 'vi' ? 'Sao chép mã' : 'Copy code',
    confirmSaved: language === 'vi'
      ? 'Tôi xác nhận đã lưu mã khôi phục này ở nơi an toàn. Tôi hiểu rằng nếu mất mã này và quên mã PIN, dữ liệu của tôi sẽ vĩnh viễn không thể khôi phục.'
      : 'I confirm that I have saved this recovery code in a safe place. I understand that if I lose this code and forget my PIN, my data will be permanently unrecoverable.',
    completing: language === 'vi' ? 'Đang thiết lập...' : 'Setting up...',
    complete: language === 'vi' ? 'Hoàn tất & Bắt đầu' : 'Complete & Start',
    errorPinFormat: language === 'vi' ? 'Mã PIN phải bao gồm 6 chữ số' : 'PIN must be 6 digits',
    errorPinMatch: language === 'vi' ? 'Mã PIN xác nhận không khớp' : 'PINs do not match',
    errorConfirmSave: language === 'vi' ? 'Vui lòng xác nhận bạn đã lưu mã khôi phục' : 'Please confirm you have saved the recovery code',
    errorSetup: language === 'vi' ? 'Có lỗi xảy ra khi thiết lập. Vui lòng thử lại.' : 'An error occurred during setup. Please try again.'
  };

  useEffect(() => {
    if (step === 3 && !recoveryCode) {
      setRecoveryCode(generateRecoveryCode());
    }
  }, [step]);

  if (authLoading) return <div className="auth-loading"><div className="loader"></div></div>;
  if (!user) return <Navigate to="/login" replace />;
  if (!needsPasscodeSetup) return <Navigate to="/dashboard" replace />;

  const handlePasscodeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (passcode.length !== 6 || !/^\d+$/.test(passcode)) {
      setError(t.errorPinFormat);
      return;
    }
    if (passcode !== confirmPasscode) {
      setError(t.errorPinMatch);
      return;
    }
    setError('');
    setStep(3);
  };

  const handleFinalize = async () => {
    if (!confirmedSaved) {
      setError(t.errorConfirmSave);
      return;
    }
    
    setIsSubmitting(true);
    try {
      await setupEncryption(passcode);
      await setDoc(doc(db, 'users', user.uid), {
        recoveryCodePreview: recoveryCode.substring(0, 4) + '...',
        hasRecoveryCode: true
      }, { merge: true });

      navigate('/dashboard');
    } catch (err) {
      console.error('Setup error:', err);
      setError(t.errorSetup);
    } finally {
      setIsSubmitting(false);
    }
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(recoveryCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-branding">
          <div className="auth-branding-content">
            <div className="auth-logo">
              <div className="auth-logo-icon">
                <Shield size={32} />
              </div>
              <h1>LifeHub</h1>
            </div>
            <p className="auth-tagline">{t.tagline}</p>
          </div>
        </div>

        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            {step === 1 && (
              <div className="setup-step-content" style={{ textAlign: 'center' }}>
                <div className="setup-icon" style={{ color: 'var(--primary)', marginBottom: '24px' }}>
                  <Lock size={64} style={{ margin: '0 auto' }} />
                </div>
                <h2>{t.step1Title}</h2>
                <p className="auth-subtitle" style={{ color: 'var(--text-primary)', marginBottom: '32px' }}>
                  {t.step1Desc}
                </p>
                <div className="info-box" style={{ 
                  background: 'rgba(var(--primary-rgb), 0.1)', 
                  padding: '16px', 
                  borderRadius: '12px', 
                  textAlign: 'left',
                  marginBottom: '32px',
                  display: 'flex',
                  gap: '12px'
                }}>
                  <AlertCircle size={24} style={{ flexShrink: 0, color: 'var(--primary)' }} />
                  <p style={{ fontSize: '14px', margin: 0 }}>{t.step1Info}</p>
                </div>
                <button className="btn btn-primary" style={{ width: '100%' }} onClick={() => setStep(2)}>
                  {t.startSetup}
                </button>
              </div>
            )}

            {step === 2 && (
              <div className="setup-step-content">
                <h2>{t.step2Title}</h2>
                <p className="auth-subtitle">{t.step2Desc}</p>
                
                {error && <div className="auth-message error">{error}</div>}
                
                <form onSubmit={handlePasscodeSubmit}>
                  <div className="input-group">
                    <label className="input-label">{t.pinLabel}</label>
                    <input
                      type="password"
                      className="input"
                      placeholder="Enter 6 digits"
                      maxLength={6}
                      pattern="\d*"
                      inputMode="numeric"
                      value={passcode}
                      onChange={(e) => setPasscode(e.target.value)}
                      required
                    />
                  </div>
                  <div className="input-group">
                    <label className="input-label">{t.confirmPinLabel}</label>
                    <input
                      type="password"
                      className="input"
                      placeholder="Confirm 6 digits"
                      maxLength={6}
                      pattern="\d*"
                      inputMode="numeric"
                      value={confirmPasscode}
                      onChange={(e) => setConfirmPasscode(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '12px' }}>
                    {t.next} <ArrowRight size={18} style={{ marginLeft: '8px' }} />
                  </button>
                </form>
              </div>
            )}

            {step === 3 && (
              <div className="setup-step-content">
                <h2>{t.step3Title}</h2>
                <p className="auth-subtitle">{t.step3Desc}</p>
                
                <div className="recovery-code-display" style={{ 
                  background: 'var(--bg-card)', 
                  border: '2px dashed var(--primary)', 
                  padding: '24px', 
                  borderRadius: '12px',
                  textAlign: 'center',
                  marginBottom: '24px',
                  position: 'relative'
                }}>
                  <code style={{ 
                    fontSize: '20px', 
                    fontWeight: 'bold', 
                    letterSpacing: '2px',
                    color: 'var(--text-primary)',
                    display: 'block',
                    marginBottom: '16px'
                  }}>
                    {recoveryCode}
                  </code>
                  <button 
                    className="btn-text" 
                    onClick={copyToClipboard}
                    style={{ 
                      fontSize: '14px', 
                      display: 'inline-flex', 
                      alignItems: 'center', 
                      gap: '4px',
                      color: 'var(--primary)',
                      border: 'none',
                      background: 'none',
                      cursor: 'pointer'
                    }}
                  >
                    {copied ? <Check size={16} /> : <Copy size={16} />}
                    {copied ? t.copied : t.copyCode}
                  </button>
                </div>

                <div style={{ marginBottom: '24px' }}>
                  <label style={{ display: 'flex', gap: '12px', cursor: 'pointer', alignItems: 'flex-start' }}>
                    <input 
                      type="checkbox" 
                      style={{ marginTop: '4px' }}
                      checked={confirmedSaved}
                      onChange={(e) => setConfirmedSaved(e.target.checked)}
                    />
                    <span style={{ fontSize: '14px', color: 'var(--text-primary)' }}>
                      {t.confirmSaved}
                    </span>
                  </label>
                </div>

                {error && <div className="auth-message error" style={{ marginBottom: '16px' }}>{error}</div>}

                <button 
                  className="btn btn-primary" 
                  style={{ width: '100%' }} 
                  onClick={handleFinalize}
                  disabled={isSubmitting || !confirmedSaved}
                >
                  {isSubmitting ? t.completing : t.complete}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
