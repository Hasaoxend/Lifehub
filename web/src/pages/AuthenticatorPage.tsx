import { useState } from 'react';
import { useTOTP } from '../hooks/useTOTP';
import { useLanguage } from '../hooks/useLanguage';
import { parseOtpAuthUri } from '../utils/totp';
import { 
  Shield, 
  Plus, 
  Copy, 
  Trash2,
  QrCode,
  X,
  Clock
} from 'lucide-react';
import './AuthenticatorPage.css';

export function AuthenticatorPage() {
  const { totpAccounts, loading, addTotpAccount, deleteTotpAccount } = useTOTP();
  const language = useLanguage();
  const [showModal, setShowModal] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const t = {
    title: 'Authenticator',
    subtitle: language === 'vi' ? 'Quản lý mã xác thực 2 yếu tố (TOTP) của bạn' : 'Manage your 2-factor authentication (TOTP) codes',
    addAccount: language === 'vi' ? 'Thêm tài khoản' : 'Add Account',
    noAccounts: language === 'vi' ? 'Chưa có tài khoản 2FA nào' : 'No 2FA accounts yet',
    noAccountsDesc: language === 'vi' ? 'Thêm tài khoản 2FA để bảo vệ các dịch vụ của bạn tốt hơn' : 'Add 2FA accounts to better protect your services',
    loading: language === 'vi' ? 'Đang tải...' : 'Loading...',
    addModalTitle: language === 'vi' ? 'Thêm tài khoản 2FA' : 'Add 2FA Account',
    hint: language === 'vi' ? 'Dán otpauth:// URI hoặc nhập thủ công secret key' : 'Paste otpauth:// URI or enter secret key manually',
    secretLabel: language === 'vi' ? 'Secret Key hoặc OTPAuth URI *' : 'Secret Key or OTPAuth URI *',
    secretPlaceholder: 'otpauth://totp/... ' + (language === 'vi' ? 'hoặc' : 'or') + ' JBSWY3DPEHPK3PXP',
    nameLabel: language === 'vi' ? 'Tên tài khoản *' : 'Account Name *',
    issuerLabel: language === 'vi' ? 'Issuer (Tên dịch vụ)' : 'Issuer (Service Name)',
    cancel: language === 'vi' ? 'Hủy' : 'Cancel',
    add: language === 'vi' ? 'Thêm' : 'Add',
    confirmDelete: language === 'vi' ? 'Bạn có chắc muốn xóa tài khoản 2FA này?' : 'Are you sure you want to delete this 2FA account?',
    invalidUri: language === 'vi' ? 'URI không hợp lệ' : 'Invalid URI',
    enterName: language === 'vi' ? 'Vui lòng nhập tên tài khoản' : 'Please enter account name',
    addError: language === 'vi' ? 'Không thể thêm tài khoản. Kiểm tra lại secret key.' : 'Cannot add account. Check secret key.'
  };
  
  // Form state
  const [formData, setFormData] = useState({
    secretKey: '',
    accountName: '',
    issuer: ''
  });
  const [formError, setFormError] = useState('');

  const handleCopy = async (code: string, id: string) => {
    await navigator.clipboard.writeText(code);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    
    try {
      // Check if it's an otpauth:// URI
      if (formData.secretKey.startsWith('otpauth://')) {
        const parsed = parseOtpAuthUri(formData.secretKey);
        if (!parsed) {
          setFormError(t.invalidUri);
          return;
        }
        await addTotpAccount({
          secretKey: parsed.secret,
          accountName: parsed.label,
          issuer: parsed.issuer,
          digits: parsed.digits,
          period: parsed.period
        });
      } else {
        // Manual entry
        if (!formData.accountName.trim()) {
          setFormError(t.enterName);
          return;
        }
        await addTotpAccount({
          secretKey: formData.secretKey.replace(/\s/g, '').toUpperCase(),
          accountName: formData.accountName,
          issuer: formData.issuer || undefined
        });
      }
      
      setShowModal(false);
      setFormData({ secretKey: '', accountName: '', issuer: '' });
    } catch (error) {
      console.error('Error adding TOTP:', error);
      setFormError(t.addError);
    }
  };

  const handleDelete = async (id: string) => {
    if (confirm(t.confirmDelete)) {
      await deleteTotpAccount(id);
    }
  };

  if (loading) {
    return (
      <div className="page-loading">
        <div className="loader"></div>
        <p>{t.loading}</p>
      </div>
    );
  }

  return (
    <div className="authenticator-page">
      {/* Header */}
      <div className="page-header">
        <h1 className="page-title">{t.title}</h1>
        <p className="page-subtitle">{t.subtitle}</p>
      </div>

      {/* Actions */}
      <div className="page-actions">
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <Plus size={18} />
          {t.addAccount}
        </button>
      </div>

      {/* TOTP List */}
      {totpAccounts.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">
            <Shield size={40} />
          </div>
          <h3 className="empty-title">{t.noAccounts}</h3>
          <p className="empty-description">{t.noAccountsDesc}</p>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            <Plus size={18} />
            {t.addAccount}
          </button>
        </div>
      ) : (
        <div className="totp-grid">
          {totpAccounts.map((account) => (
            <div key={account.documentId} className="totp-card">
              <div className="totp-header">
                <div className="totp-info">
                  <h3 className="totp-name">
                    {account.issuer || account.accountName}
                  </h3>
                  {account.issuer && (
                    <p className="totp-account">{account.accountName}</p>
                  )}
                </div>
                <button 
                  className="action-btn danger"
                  onClick={() => handleDelete(account.documentId!)}
                  title="Xóa"
                >
                  <Trash2 size={16} />
                </button>
              </div>

              <div className="totp-code-container">
                <div 
                  className="totp-code"
                  onClick={() => handleCopy(account.currentCode, account.documentId!)}
                >
                  <span className="code-digits">
                    {account.currentCode.slice(0, 3)}
                  </span>
                  <span className="code-separator">·</span>
                  <span className="code-digits">
                    {account.currentCode.slice(3)}
                  </span>
                  
                  {copiedId === account.documentId && (
                    <span className="copied-badge">Đã sao chép!</span>
                  )}
                </div>
                
                <button 
                  className="copy-btn"
                  onClick={() => handleCopy(account.currentCode, account.documentId!)}
                >
                  <Copy size={16} />
                </button>
              </div>

              <div className="totp-timer">
                <Clock size={14} />
                <span>{account.remainingSeconds}s</span>
                <div className="timer-progress">
                  <div 
                    className="timer-bar"
                    style={{ width: `${account.progress}%` }}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">{t.addModalTitle}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-tabs">
              <div className="tab-hint">
                <QrCode size={20} />
                <span>{t.hint}</span>
              </div>
            </div>

            {formError && (
              <div className="form-error">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="modal-form">
              <div className="input-group">
                <label className="input-label">{t.secretLabel}</label>
                <textarea
                  className="input"
                  placeholder={t.secretPlaceholder}
                  rows={3}
                  value={formData.secretKey}
                  onChange={(e) => setFormData({...formData, secretKey: e.target.value})}
                  required
                />
              </div>

              {!formData.secretKey.startsWith('otpauth://') && (
                <>
                  <div className="input-group">
                    <label className="input-label">{t.nameLabel}</label>
                    <input
                      type="text"
                      className="input"
                      placeholder="example@email.com"
                      value={formData.accountName}
                      onChange={(e) => setFormData({...formData, accountName: e.target.value})}
                    />
                  </div>

                  <div className="input-group">
                    <label className="input-label">{t.issuerLabel}</label>
                    <input
                      type="text"
                      className="input"
                      placeholder="Google, Facebook, ..."
                      value={formData.issuer}
                      onChange={(e) => setFormData({...formData, issuer: e.target.value})}
                    />
                  </div>
                </>
              )}

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                  {t.cancel}
                </button>
                <button type="submit" className="btn btn-primary">
                  {t.add}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
