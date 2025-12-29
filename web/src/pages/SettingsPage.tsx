import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { checkPasswordStrength, MIN_REQUIRED_STRENGTH_SCORE } from '../utils/passwordUtils';
import { 
  updatePassword, 
  EmailAuthProvider, 
  reauthenticateWithCredential,
  sendPasswordResetEmail,
  createUserWithEmailAndPassword,
  fetchSignInMethodsForEmail,
  sendEmailVerification
} from 'firebase/auth';
import { auth, db } from '../firebase/config';
import { doc, setDoc } from 'firebase/firestore';
import { 
  Key, 
  Globe, 
  Lock, 
  Mail, 
  Eye, 
  EyeOff, 
  Check, 
  AlertCircle,
  ChevronRight,
  UserPlus,
  ArrowLeft,
  Shield,
  Sun,
  Moon,
  Monitor
} from 'lucide-react';
import './SettingsPage.css';

type Language = 'vi' | 'en';
type Theme = 'light' | 'dark';
type PasswordStep = 'idle' | 'verify' | 'newPassword';
type RegisterStep = 'idle' | 'email' | 'password' | 'success';

// Redundant local function removed

export function SettingsPage() {
  const { user, changePasscode, signOut, resetPassword } = useAuth();
  const navigate = useNavigate();
  
  // Passcode change state
  const [showChangePasscode, setShowChangePasscode] = useState(false);
  const [oldPasscode, setOldPasscode] = useState('');
  const [newPasscode, setNewPasscode] = useState('');
  const [confirmPasscode, setConfirmPasscode] = useState('');
  const [passcodeLoading, setPasscodeLoading] = useState(false);
  const [passcodeMessage, setPasscodeMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  
  // Password change state

  // Register state
  const [registerStep, setRegisterStep] = useState<RegisterStep>('idle');
  const [registerEmail, setRegisterEmail] = useState('');
  const [registerPassword, setRegisterPassword] = useState('');
  const [registerConfirmPassword, setRegisterConfirmPassword] = useState('');
  const [registerLoading, setRegisterLoading] = useState(false);
  const [registerMessage, setRegisterMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  
  // Forgot password state
  const [forgotPasswordLoading, setForgotPasswordLoading] = useState(false);
  const [forgotPasswordMessage, setForgotPasswordMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  
  // Theme state
  // Theme state
  const [theme, setTheme] = useState<Theme>(() => {
    return (localStorage.getItem('theme') as Theme) || 'light';
  });
  
  // Language state
  const language = useLanguage();

  const t = {
    title: language === 'vi' ? 'Cài đặt hệ thống' : 'System Settings',
    subtitle: language === 'vi' ? 'Quản lý tài khoản và tùy chọn ứng dụng' : 'Manage account and application preferences',
    security: language === 'vi' ? 'Bảo mật' : 'Security',
    theme: language === 'vi' ? 'Giao diện' : 'Appearance',
    language: language === 'vi' ? 'Ngôn ngữ' : 'Language',
    changePin: language === 'vi' ? 'Đổi mã PIN' : 'Change Security PIN',
    changePinDesc: language === 'vi' ? 'Thay đổi mã PIN dùng để truy cập các tính năng bảo mật' : 'Change PIN code used to access security features',
    changePass: language === 'vi' ? 'Đổi mật khẩu' : 'Change Password',
    changePassDesc: language === 'vi' ? 'Thay đổi mật khẩu đăng nhập tài khoản' : 'Change login password',
    register: language === 'vi' ? 'Đăng ký tài khoản' : 'Register Account',
    registerDesc: language === 'vi' ? 'Tạo tài khoản mới để đồng bộ dữ liệu' : 'Create new account to sync data',
    forgotPass: language === 'vi' ? 'Quên mật khẩu' : 'Forgot Password',
    forgotPassDesc: language === 'vi' ? 'Khôi phục mật khẩu đăng nhập' : 'Reset login password',
    lightMode: language === 'vi' ? 'Chế độ sáng' : 'Light Mode',
    darkMode: language === 'vi' ? 'Chế độ tối' : 'Dark Mode',
    systemMode: language === 'vi' ? 'Hệ thống' : 'System Default',
    currentPin: language === 'vi' ? 'Mã PIN hiện tại' : 'Current PIN',
    newPin: language === 'vi' ? 'Mã PIN mới (6 số)' : 'New PIN (6 digits)',
    confirmPin: language === 'vi' ? 'Xác nhận mã PIN mới' : 'Confirm New PIN',
    cancel: language === 'vi' ? 'Hủy' : 'Cancel',
    save: language === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
    loading: language === 'vi' ? 'Đang xử lý...' : 'Processing...',
    pinError: language === 'vi' ? 'Mã PIN mới phải là 6 chữ số' : 'New PIN must be 6 digits',
    pinMismatch: language === 'vi' ? 'Mã PIN xác nhận không khớp' : 'Confirmation PIN does not match',
    pinSuccess: language === 'vi' ? 'Đã đổi mã PIN thành công!' : 'PIN changed successfully!',
    currentPass: language === 'vi' ? 'Mật khẩu hiện tại' : 'Current Password',
    newPass: language === 'vi' ? 'Mật khẩu mới' : 'New Password',
    confirmPass: language === 'vi' ? 'Xác nhận mật khẩu mới' : 'Confirm New Password',
    passMismatch: language === 'vi' ? 'Mật khẩu mới không khớp' : 'New password does not match',
    passShort: language === 'vi' ? 'Mật khẩu phải có ít nhất 6 ký tự' : 'Password must be at least 6 characters',
    passWeak: language === 'vi' ? 'Mật khẩu không đủ mạnh' : 'Password is not strong enough',
    passSuccess: language === 'vi' ? 'Đổi mật khẩu thành công!' : 'Password changed successfully!',
    verify: language === 'vi' ? 'Xác thực' : 'Verify',
    update: language === 'vi' ? 'Cập nhật' : 'Update',
    weak: language === 'vi' ? 'Yếu' : 'Weak',
    medium: language === 'vi' ? 'Trung bình' : 'Medium',
    good: language === 'vi' ? 'Khá' : 'Good',
    strong: language === 'vi' ? 'Mạnh' : 'Strong',
    email: language === 'vi' ? 'Email' : 'Email',
    sendReset: language === 'vi' ? 'Gửi email khôi phục' : 'Send Reset Email',
    resetSuccess: language === 'vi' ? 'Đã gửi email khôi phục mật khẩu!' : 'Password reset email sent!',
    registerSuccess: language === 'vi' ? 'Đăng ký thành công! Vui lòng kiểm tra email để xác thực.' : 'Registration successful! Please check your email to verify.',
    pinPlaceholder: '******',
    passPlaceholder: '••••••'
  };

  // Apply theme to document
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);


  const handlePasscodeChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasscodeMessage(null);

    if (newPasscode.length !== 6 || !/^\d+$/.test(newPasscode)) {
      setPasscodeMessage({ type: 'error', text: 'Mã PIN mới phải là 6 chữ số' });
      return;
    }

    if (newPasscode !== confirmPasscode) {
      setPasscodeMessage({ type: 'error', text: t.pinMismatch });
      return;
    }

    setPasscodeLoading(true);
    try {
      await changePasscode(oldPasscode, newPasscode);
      setPasscodeMessage({ type: 'success', text: 'Đã đổi mã PIN thành công!' });
      setOldPasscode('');
      setNewPasscode('');
      setConfirmPasscode('');
      setTimeout(() => setShowChangePasscode(false), 2000);
    } catch (err: any) {
      console.error('Passcode change error:', err);
      setPasscodeMessage({ type: 'error', text: err.message || 'Mã PIN cũ không chính xác' });
    } finally {
      setPasscodeLoading(false);
    }
  };

  // Handlers for password change removed (moved to dedicated page)

  // Register Step 1: Check email
  const handleCheckEmail = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegisterMessage(null);
    
    if (!registerEmail || !registerEmail.includes('@')) {
      setRegisterMessage({ type: 'error', text: 'Vui lòng nhập email hợp lệ' });
      return;
    }
    
    try {
      setRegisterLoading(true);
      const methods = await fetchSignInMethodsForEmail(auth, registerEmail);
      
      if (methods.length > 0) {
        setRegisterMessage({ type: 'error', text: 'Email này đã được sử dụng' });
      } else {
        // Email available - move to password step
        setRegisterStep('password');
        setRegisterMessage(null);
      }
    } catch (error: any) {
      console.error('Error checking email:', error);
      // Firebase may throw error for invalid email format
      if (error.code === 'auth/invalid-email') {
        setRegisterMessage({ type: 'error', text: 'Email không hợp lệ' });
      } else {
        // If error, assume email is available (Firebase sometimes doesn't allow enumeration)
        setRegisterStep('password');
        setRegisterMessage(null);
      }
    } finally {
      setRegisterLoading(false);
    }
  };

  // Register Step 2: Create account
  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegisterMessage(null);
    
    if (registerPassword !== registerConfirmPassword) {
      setRegisterMessage({ type: 'error', text: t.passMismatch });
      return;
    }
    
    if (registerPassword.length < 6) {
      setRegisterMessage({ type: 'error', text: t.passShort });
      return;
    }
    
    const strength = checkPasswordStrength(registerPassword);
    if (strength.score < MIN_REQUIRED_STRENGTH_SCORE) {
      setRegisterMessage({ 
        type: 'error', 
        text: language === 'vi' ? 'Mật khẩu phải đạt độ mạnh tối đa' : 'Password must meet maximum strength requirements' 
      });
      return;
    }
    
    try {
      setRegisterLoading(true);
      const userCredential = await createUserWithEmailAndPassword(auth, registerEmail, registerPassword);
      
      // Send verification email
      await sendEmailVerification(userCredential.user);
      
      setRegisterStep('success');
      setRegisterMessage({ type: 'success', text: language === 'vi' ? `Tài khoản đã tạo! Email xác nhận đã gửi đến ${registerEmail}` : `Account created! Verification email sent to ${registerEmail}` });
    } catch (error: any) {
      console.error('Error creating account:', error);
      if (error.code === 'auth/email-already-in-use') {
        setRegisterMessage({ type: 'error', text: 'Email này đã được sử dụng' });
      } else if (error.code === 'auth/weak-password') {
        setRegisterMessage({ type: 'error', text: 'Mật khẩu quá yếu' });
      } else {
        setRegisterMessage({ type: 'error', text: 'Có lỗi xảy ra. Vui lòng thử lại.' });
      }
    } finally {
      setRegisterLoading(false);
    }
  };

  const resetRegisterState = () => {
    setRegisterStep('idle');
    setRegisterEmail('');
    setRegisterPassword('');
    setRegisterConfirmPassword('');
    setRegisterMessage(null);
  };

  const handleForgotPassword = async () => {
    if (!user?.email) {
      setForgotPasswordMessage({ type: 'error', text: 'Không tìm thấy email' });
      return;
    }
    
    try {
      setForgotPasswordLoading(true);
      
      const actionCodeSettings = {
        url: `${window.location.origin}/reset-password`,
        handleCodeInApp: true,
      };

      // Use the centralized resetPassword from useAuth
      await resetPassword(user.email, actionCodeSettings);

      setForgotPasswordMessage({ 
        type: 'success', 
        text: language === 'vi' ? `Email đặt lại mật khẩu đã gửi đến ${user.email}. Đang chuyển hướng...` : `Reset password email sent to ${user.email}. Redirecting...`
      });
      setTimeout(async () => {
        await signOut();
        navigate('/login');
      }, 2000);
    } catch (error: any) {
      console.error('Error sending reset email:', error);
      setForgotPasswordMessage({ type: 'error', text: 'Có lỗi xảy ra. Vui lòng thử lại.' });
    } finally {
      setForgotPasswordLoading(false);
    }
  };


  const passwordStrength = checkPasswordStrength(registerPassword);

  return (
    <div className="settings-page">
      <div className="settings-header">
        <h1>{t.title}</h1>
      </div>

      <div className="settings-content">
        {/* Account Section */}
        <div className="settings-section">
          <div className="section-header">
            <Mail size={20} />
            <h2>{t.title}</h2>
          </div>
          <div className="section-body">
            <div className="setting-item">
              <span className="setting-label">{t.email}</span>
              <span className="setting-value">{user?.email}</span>
            </div>
          </div>
        </div>

        {/* Password Section */}
        <div className="settings-section">
          <div className="section-header">
            <Lock size={20} />
            <h2>{t.changePass}</h2>
          </div>
          <div className="section-body">
            <button 
                className="setting-action-btn"
                onClick={() => navigate('/change-password')}
              >
                {t.changePass}
                <ChevronRight size={18} />
              </button>
            
            {/* Password steps block removed */}
            
            {/* Forgot Password */}
            <div className="forgot-password-section">
              <button 
                className="forgot-password-btn"
                onClick={handleForgotPassword}
                disabled={forgotPasswordLoading}
              >
                <Key size={16} />
                {forgotPasswordLoading ? t.loading : t.forgotPass + '?'}
              </button>
              {forgotPasswordMessage && (
                <div className={`message ${forgotPasswordMessage.type}`}>
                  {forgotPasswordMessage.type === 'success' ? <Check size={16} /> : <AlertCircle size={16} />}
                  {forgotPasswordMessage.text}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Passcode (PIN) Section */}
        <div className="settings-section">
          <div className="section-header">
            <Shield size={20} />
            <h2>{t.changePin}</h2>
          </div>
          <div className="section-body">
            {!showChangePasscode ? (
              <button 
                className="setting-action-btn"
                onClick={() => setShowChangePasscode(true)}
              >
                {t.changePin} (6 {language === 'vi' ? 'số' : 'digits'})
                <ChevronRight size={18} />
              </button>
            ) : (
              <form onSubmit={handlePasscodeChange} className="password-form">
                <div className="step-header">
                  <button type="button" className="back-btn" onClick={() => setShowChangePasscode(false)}>
                    <ArrowLeft size={18} />
                  </button>
                  <span>{t.changePin}</span>
                </div>
                
                <div className="form-group">
                  <label>{t.currentPin}</label>
                  <input
                    type="password"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    pattern="[0-9]*"
                    maxLength={6}
                    value={oldPasscode}
                    onChange={(e) => setOldPasscode(e.target.value.replace(/\D/g, ''))}
                    placeholder={t.pinPlaceholder}
                    required
                    autoFocus
                  />
                </div>

                <div className="form-group">
                  <label>{t.newPin}</label>
                  <input
                    type="password"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={6}
                    value={newPasscode}
                    onChange={(e) => setNewPasscode(e.target.value.replace(/\D/g, ''))}
                    placeholder={t.pinPlaceholder}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>{t.confirmPin}</label>
                  <input
                    type="password"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={6}
                    value={confirmPasscode}
                    onChange={(e) => setConfirmPasscode(e.target.value.replace(/\D/g, ''))}
                    placeholder={t.pinPlaceholder}
                    required
                  />
                </div>
                
                {passcodeMessage && (
                  <div className={`message ${passcodeMessage.type}`}>
                    {passcodeMessage.type === 'success' ? <Check size={16} /> : <AlertCircle size={16} />}
                    {passcodeMessage.text}
                  </div>
                )}
                
                <div className="form-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => setShowChangePasscode(false)}>
                    Hủy
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={passcodeLoading}>
                    {passcodeLoading ? 'Đang xử lý...' : 'Đổi mã PIN'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>


      </div>
    </div>
  );
}

