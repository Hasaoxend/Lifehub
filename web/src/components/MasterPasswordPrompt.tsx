import { useState, useEffect } from 'react';
import { Shield, LogOut, Timer, AlertTriangle } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import './MasterPasswordPrompt.css';

type Language = 'vi' | 'en';

interface MasterPasswordPromptProps {
  onUnlock: (passcode: string) => Promise<any>;
}

export function MasterPasswordPrompt({ onUnlock }: MasterPasswordPromptProps) {
  const { signOut } = useAuth();
  const [passcode, setPasscode] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [attempts, setAttempts] = useState(0);
  const [lockoutUntil, setLockoutUntil] = useState<number | null>(null);
  const [timeLeft, setTimeLeft] = useState(0);
  const [language] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  const t = {
    signOut: language === 'vi' ? 'Đăng xuất' : 'Sign out',
    tagline: language === 'vi' ? 'Dữ liệu được bảo vệ bằng mã hóa PIN' : 'Data protected with PIN encryption',
    unlock: language === 'vi' ? 'Mở khóa ứng dụng' : 'Unlock App',
    description: language === 'vi' 
      ? 'Nhập mã PIN 6 số của bạn để truy cập dữ liệu đã mã hóa.'
      : 'Enter your 6-digit PIN to access your encrypted data.',
    lockedOut: language === 'vi' 
      ? 'Bạn đã nhập sai quá 10 lần. Vui lòng chờ 5 phút.'
      : 'Too many failed attempts. Please wait 5 minutes.',
    wrongPin: language === 'vi' ? 'Mã PIN không đúng. Bạn còn' : 'Incorrect PIN.',
    attemptsLeft: language === 'vi' ? 'lần thử.' : 'attempts left.',
    temporaryLocked: language === 'vi' ? 'Tạm thời bị khóa' : 'Temporarily Locked',
    tryAgain: language === 'vi' ? 'Vui lòng thử lại sau:' : 'Please try again in:',
    unlocking: language === 'vi' ? 'Đang mở khóa...' : 'Unlocking...',
    unlockNow: language === 'vi' ? 'Mở khóa ngay' : 'Unlock Now',
    warning: language === 'vi' 
      ? 'Dữ liệu của bạn được lưu trữ an toàn với mã hóa AES-256. Mã PIN không bao giờ được gửi lên máy chủ.'
      : 'Your data is securely stored with AES-256 encryption. Your PIN is never sent to the server.'
  };

  useEffect(() => {
    const savedLockout = localStorage.getItem('lifehub_lockout_until');
    const savedAttempts = localStorage.getItem('lifehub_failed_attempts');
    
    if (savedLockout) {
      const until = parseInt(savedLockout, 10);
      if (until > Date.now()) {
        setLockoutUntil(until);
        setTimeLeft(Math.ceil((until - Date.now()) / 1000));
      } else {
        localStorage.removeItem('lifehub_lockout_until');
      }
    }
    
    if (savedAttempts) {
      setAttempts(parseInt(savedAttempts, 10));
    }
  }, []);

  useEffect(() => {
    let timer: number;
    if (lockoutUntil && timeLeft > 0) {
      timer = window.setInterval(() => {
        const remaining = Math.ceil((lockoutUntil - Date.now()) / 1000);
        if (remaining <= 0) {
          setLockoutUntil(null);
          setTimeLeft(0);
          localStorage.removeItem('lifehub_lockout_until');
          localStorage.setItem('lifehub_failed_attempts', '0');
          setAttempts(0);
        } else {
          setTimeLeft(remaining);
        }
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [lockoutUntil, timeLeft]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (lockoutUntil) return;
    if (passcode.length !== 6) return;

    setError('');
    setIsSubmitting(true);

    try {
      await onUnlock(passcode);
      localStorage.setItem('lifehub_failed_attempts', '0');
      setAttempts(0);
    } catch (err: any) {
      const newAttempts = attempts + 1;
      setAttempts(newAttempts);
      localStorage.setItem('lifehub_failed_attempts', newAttempts.toString());

      if (newAttempts >= 10) {
        const until = Date.now() + 5 * 60 * 1000;
        setLockoutUntil(until);
        setTimeLeft(300);
        localStorage.setItem('lifehub_lockout_until', until.toString());
        setError(t.lockedOut);
      } else {
        setError(`${t.wrongPin} ${10 - newAttempts} ${t.attemptsLeft}`);
      }
      setPasscode('');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="master-password-page">
      <div className="master-password-container">
        <div className="mp-header">
          <button className="mp-logout-btn" onClick={() => signOut()} title={t.signOut}>
            <LogOut size={20} />
            <span>{t.signOut}</span>
          </button>
          <div className="mp-logo">
            <Shield size={48} />
          </div>
          <h1>LifeHub</h1>
          <p>{t.tagline}</p>
        </div>

        <div className="mp-card">
          <h2>{t.unlock}</h2>
          <p className="mp-description">{t.description}</p>

          {error && (
            <div className={`mp-error ${lockoutUntil ? 'critical' : ''}`}>
              <AlertTriangle size={16} />
              {error}
            </div>
          )}

          {lockoutUntil ? (
            <div className="lockout-display">
              <Timer size={48} className="spin-slow" />
              <h3>{t.temporaryLocked}</h3>
              <p>{t.tryAgain} <strong>{formatTime(timeLeft)}</strong></p>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="pin-input-container">
                <input
                  type="password"
                  className="pin-hidden-input"
                  maxLength={6}
                  pattern="\d*"
                  inputMode="numeric"
                  value={passcode}
                  onChange={(e) => {
                    const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                    setPasscode(val);
                  }}
                  autoFocus
                  disabled={isSubmitting}
                />
                <div className="pin-dots">
                  {[...Array(6)].map((_, i) => (
                    <div 
                      key={i} 
                      className={`pin-dot ${passcode.length > i ? 'active' : ''} ${passcode.length === i ? 'current' : ''}`}
                    />
                  ))}
                </div>
              </div>

              <button 
                type="submit" 
                className="btn btn-primary btn-unlock"
                disabled={isSubmitting || passcode.length !== 6}
                style={{ marginTop: '32px' }}
              >
                {isSubmitting ? t.unlocking : t.unlockNow}
              </button>
            </form>
          )}

          <p className="mp-warning">{t.warning}</p>
        </div>
      </div>
    </div>
  );
}
