import { useState } from 'react';
import { useAccounts } from '../hooks/useAccounts';
import { useLanguage } from '../hooks/useLanguage';
import { 
  Key, 
  Plus, 
  Search, 
  Eye, 
  EyeOff, 
  Copy, 
  Edit, 
  Trash2,
  ExternalLink,
  Globe,
  X,
  User,
  ShieldCheck,
  Wand2,
  PlusCircle,
  Lock,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { checkPasswordStrength, PasswordStrength } from '../utils/passwordUtils';
import { PasswordGenerator } from '../components/PasswordGenerator';
import './AccountsPage.css';

interface AccountEntry {
  documentId?: string;
  serviceName: string;
  username: string;
  password: string;
  websiteUrl?: string;
  notes?: string;
  userOwnerId: string;
  lastModified?: Date;
  isEncryptionError?: boolean;
}

export function AccountsPage() {
  const { accounts, loading, addAccount, updateAccount, deleteAccount } = useAccounts();
  const [searchQuery, setSearchQuery] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingAccount, setEditingAccount] = useState<AccountEntry | null>(null);
  const [visiblePasswords, setVisiblePasswords] = useState<Set<string>>(new Set());
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showPasswordGenerator, setShowPasswordGenerator] = useState(false);
  const [expandedCardId, setExpandedCardId] = useState<string | null>(null);
  const language = useLanguage();

  // Custom fields state
  interface CustomFieldItem {
    key: string;
    value: string;
    type: 'text' | 'password';
  }
  const [customFields, setCustomFields] = useState<CustomFieldItem[]>([]);
  const [visibleCustomFields, setVisibleCustomFields] = useState<Set<number>>(new Set());

  const t = {
    title: language === 'vi' ? 'Quản lý tài khoản' : 'Account Manager',
    subtitle: language === 'vi' ? 'Lưu trữ và quản lý mật khẩu của bạn một cách an toàn' : 'Store and manage your passwords securely',
    search: language === 'vi' ? 'Tìm kiếm tài khoản...' : 'Search accounts...',
    addAccount: language === 'vi' ? 'Thêm tài khoản' : 'Add Account',
    noResults: language === 'vi' ? 'Không tìm thấy kết quả' : 'No results found',
    noAccounts: language === 'vi' ? 'Chưa có tài khoản nào' : 'No accounts yet',
    tryOther: language === 'vi' ? 'Thử tìm kiếm với từ khóa khác' : 'Try a different search term',
    startAdd: language === 'vi' ? 'Bắt đầu bằng cách thêm tài khoản đầu tiên của bạn' : 'Start by adding your first account',
    password: language === 'vi' ? 'Mật khẩu' : 'Password',
    needsUpdate: language === 'vi' ? 'Cần cập nhật trên Android' : 'Needs update on Android',
    copied: language === 'vi' ? 'Đã chép!' : 'Copied!',
    copiedFull: language === 'vi' ? 'Đã sao chép!' : 'Copied!',
    editAccount: language === 'vi' ? 'Chỉnh sửa tài khoản' : 'Edit Account',
    addNew: language === 'vi' ? 'Thêm tài khoản mới' : 'Add New Account',
    serviceName: language === 'vi' ? 'Tên dịch vụ' : 'Service Name',
    username: 'Username / Email',
    yourPassword: language === 'vi' ? 'Mật khẩu của bạn' : 'Your password',
    websiteUrl: 'Website URL',
    notes: language === 'vi' ? 'Ghi chú' : 'Notes',
    cancel: language === 'vi' ? 'Hủy' : 'Cancel',
    update: language === 'vi' ? 'Cập nhật' : 'Update',
    add: language === 'vi' ? 'Thêm mới' : 'Add',
    confirmDelete: language === 'vi' ? 'Bạn có chắc muốn xóa tài khoản này?' : 'Are you sure you want to delete this account?',
    loading: language === 'vi' ? 'Đang tải tài khoản...' : 'Loading accounts...'
  };

  // Form state
  const [formData, setFormData] = useState({
    serviceName: '',
    username: '',
    password: '',
    websiteUrl: '',
    notes: ''
  });

  const [passwordStrength, setPasswordStrength] = useState<PasswordStrength | null>(null);

  // Update password strength when typing
  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newPassword = e.target.value;
    setFormData({...formData, password: newPassword});
    setPasswordStrength(checkPasswordStrength(newPassword));
  };

  const filteredAccounts = accounts.filter(account =>
    account.serviceName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    account.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const togglePasswordVisibility = (id: string) => {
    const newSet = new Set(visiblePasswords);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setVisiblePasswords(newSet);
  };

  const handleCopy = async (text: string, id: string) => {
    await navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const openAddModal = () => {
    setEditingAccount(null);
    setFormData({
      serviceName: '',
      username: '',
      password: '',
      websiteUrl: '',
      notes: ''
    });
    setCustomFields([]);
    setShowModal(true);
  };

  const openEditModal = (account: AccountEntry) => {
    setEditingAccount(account);
    setFormData({
      serviceName: account.serviceName,
      username: account.username,
      password: account.password || '',
      websiteUrl: account.websiteUrl || '',
      notes: account.notes || ''
    });
    // Load custom fields if any
    const existingFields = (account as any).customFields || {};
    const fieldsArray: CustomFieldItem[] = Object.entries(existingFields).map(([key, val]: [string, any]) => ({
      key,
      value: val.value || '',
      type: val.type === 1 ? 'password' : 'text'
    }));
    setCustomFields(fieldsArray);
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Convert custom fields array to object format
    const customFieldsObj: Record<string, { value: string; type: number }> = {};
    customFields.forEach(field => {
      if (field.key.trim()) {
        customFieldsObj[field.key.trim()] = {
          value: field.value,
          type: field.type === 'password' ? 1 : 0
        };
      }
    });
    
    // Build data object - only include customFields if not empty
    const dataToSave: any = { ...formData };
    if (Object.keys(customFieldsObj).length > 0) {
      dataToSave.customFields = customFieldsObj;
    }
    
    try {
      if (editingAccount) {
        await updateAccount(editingAccount.documentId!, dataToSave);
      } else {
        await addAccount(dataToSave);
      }
      setShowModal(false);
    } catch (error) {
      console.error('Error saving account:', error);
    }
  };

  const handleDelete = async (id: string) => {
    if (confirm(t.confirmDelete)) {
      await deleteAccount(id);
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
    <div className="accounts-page">
      {/* Header */}
      <div className="page-header">
        <h1 className="page-title">{t.title}</h1>
        <p className="page-subtitle">{t.subtitle}</p>
      </div>

      {/* Actions */}
      <div className="page-actions">
        <div className="search-container">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="input search-input"
            placeholder={t.search}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="btn btn-primary" onClick={openAddModal}>
          <Plus size={18} />
          {t.addAccount}
        </button>
      </div>

      {/* Accounts List */}
      {filteredAccounts.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">
            <Key size={40} />
          </div>
          <h3 className="empty-title">
            {searchQuery ? t.noResults : t.noAccounts}
          </h3>
          <p className="empty-description">
            {searchQuery ? t.tryOther : t.startAdd}
          </p>
          {!searchQuery && (
            <button className="btn btn-primary" onClick={openAddModal}>
              <Plus size={18} />
              {t.addAccount}
            </button>
          )}
        </div>
      ) : (
        <div className="accounts-grid">
          {filteredAccounts.map((account) => (
            <div key={account.documentId} className="account-card">
              <div className="account-header">
                <div className="account-icon">
                  {account.websiteUrl ? (
                    <img 
                      src={`https://www.google.com/s2/favicons?domain=${account.websiteUrl}&sz=32`}
                      alt=""
                      onError={(e) => {
                        (e.target as HTMLImageElement).style.display = 'none';
                      }}
                    />
                  ) : null}
                  <Globe size={20} className="fallback-icon" />
                </div>
                <div className="account-info">
                  <div className="account-name-row">
                    <h3 className="account-name">{account.serviceName}</h3>
                    {(account as any).customFields && Object.keys((account as any).customFields).length > 0 && (
                      <span className="custom-fields-badge" title="Có trường tùy chỉnh">
                        +{Object.keys((account as any).customFields).length} trường
                      </span>
                    )}
                  </div>
                  <div className="account-username-row">
                    <User size={14} className="username-icon" />
                    <span className="account-username">{account.username}</span>
                    <button 
                      className="copy-btn-mini"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopy(account.username, `user-${account.documentId}`);
                      }}
                      title="Sao chép tên đăng nhập"
                    >
                      <Copy size={12} />
                      {copiedId === `user-${account.documentId}` && (
                        <span className="copied-tooltip-mini">Đã chép!</span>
                      )}
                    </button>
                  </div>
                </div>
                <div className="account-actions">
                  <button 
                    className="action-btn"
                    onClick={() => openEditModal(account)}
                    title="Chỉnh sửa"
                  >
                    <Edit size={16} />
                  </button>
                  <button 
                    className="action-btn danger"
                    onClick={() => handleDelete(account.documentId!)}
                    title="Xóa"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>

              <div className="account-credentials">
                <div className="credential-row">
                  <span className="credential-label">Mật khẩu</span>
                  <div className="credential-value">
                    <span className="password-text">
                      {account.isEncryptionError ? (
                         <span style={{color: 'var(--error)', fontStyle: 'italic', fontSize: '13px'}}>
                           🔒 Cần cập nhật trên Android
                         </span>
                      ) : (
                        visiblePasswords.has(account.documentId!) 
                          ? account.password 
                          : '••••••••'
                      )}
                    </span>
                    {!account.isEncryptionError && (
                      <>
                        <button
                          className="credential-btn"
                          onClick={() => togglePasswordVisibility(account.documentId!)}
                        >
                          {visiblePasswords.has(account.documentId!) 
                            ? <EyeOff size={14} />
                            : <Eye size={14} />
                          }
                        </button>
                        <button
                          className="credential-btn"
                          onClick={() => handleCopy(account.password || '', `pwd-${account.documentId}`)}
                        >
                          <Copy size={14} />
                          {copiedId === `pwd-${account.documentId}` && (
                            <span className="copied-tooltip">Đã sao chép!</span>
                          )}
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {account.websiteUrl && (
                  <div className="credential-row">
                    <span className="credential-label">Website</span>
                    <a 
                      href={account.websiteUrl} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="website-link"
                    >
                      {account.websiteUrl}
                      <ExternalLink size={12} />
                    </a>
                  </div>
                )}

                {/* Custom Fields Display - Collapsible */}
                {(account as any).customFields && Object.keys((account as any).customFields).length > 0 && (
                  <div className="custom-fields-section">
                    <button 
                      className="custom-fields-toggle"
                      onClick={(e) => {
                        e.stopPropagation();
                        setExpandedCardId(expandedCardId === account.documentId ? null : account.documentId!);
                      }}
                    >
                      <span>Trường tùy chỉnh ({Object.keys((account as any).customFields).length})</span>
                      {expandedCardId === account.documentId ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                    </button>
                    
                    {expandedCardId === account.documentId && (
                      <>
                        <div className="custom-fields-overlay-backdrop" onClick={() => setExpandedCardId(null)} />
                        <div className="custom-fields-overlay">
                          {Object.entries((account as any).customFields).map(([key, field]: [string, any]) => (
                            <div key={key} className="credential-row">
                              <span className="credential-label">{key}</span>
                              <div className="credential-value">
                                <span className="password-text">
                                  {field.type === 1 
                                    ? (visiblePasswords.has(`${account.documentId}-${key}`) ? field.value : '••••••••')
                                    : field.value}
                                </span>
                                {field.type === 1 && (
                                  <>
                                    <button
                                      className="credential-btn"
                                      onClick={() => togglePasswordVisibility(`${account.documentId}-${key}`)}
                                    >
                                      {visiblePasswords.has(`${account.documentId}-${key}`) 
                                        ? <EyeOff size={14} />
                                        : <Eye size={14} />
                                      }
                                    </button>
                                    <button
                                      className="credential-btn"
                                      onClick={() => handleCopy(field.value, `cf-${account.documentId}-${key}`)}
                                    >
                                      <Copy size={14} />
                                      {copiedId === `cf-${account.documentId}-${key}` && (
                                        <span className="copied-tooltip">Đã sao chép!</span>
                                      )}
                                    </button>
                                  </>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {editingAccount ? 'Chỉnh sửa tài khoản' : 'Thêm tài khoản mới'}
              </h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="modal-form">
              <div className="input-group">
                <label className="input-label">Tên dịch vụ *</label>
                <input
                  type="text"
                  className="input"
                  placeholder="VD: Gmail, Facebook, ..."
                  value={formData.serviceName}
                  onChange={(e) => setFormData({...formData, serviceName: e.target.value})}
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Username / Email *</label>
                <input
                  type="text"
                  className="input"
                  placeholder="example@email.com"
                  value={formData.username}
                  onChange={(e) => setFormData({...formData, username: e.target.value})}
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Mật khẩu *</label>
                <div className="password-input-wrapper">
                  <input
                    type="text"
                    className="input"
                    placeholder="Mật khẩu của bạn"
                    value={formData.password}
                    onChange={handlePasswordChange}
                    required
                  />
                  <button
                    type="button"
                    className="generate-btn"
                    onClick={() => setShowPasswordGenerator(true)}
                    title="Tạo mật khẩu mạnh ngẫu nhiên"
                  >
                    <Wand2 size={16} />
                  </button>
                </div>
                
                {/* Password Strength Indicator */}
                {formData.password && passwordStrength && (
                  <div className="password-strength-container">
                    <div className="strength-bar-bg">
                      <div 
                        className="strength-bar-fill" 
                        style={{ 
                          width: `${(passwordStrength.score / 4) * 100}%`,
                          backgroundColor: passwordStrength.color
                        }}
                      />
                    </div>
                    <div className="strength-text" style={{ color: passwordStrength.color }}>
                      <ShieldCheck size={12} style={{ marginRight: 4 }} />
                      {passwordStrength.label}
                    </div>
                  </div>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">Website URL</label>
                <input
                  type="text"
                  className="input"
                  placeholder="example.com"
                  value={formData.websiteUrl}
                  onChange={(e) => setFormData({...formData, websiteUrl: e.target.value})}
                />
              </div>

              {/* Custom Fields Section */}
              <div className="custom-fields-section">
                <div className="custom-fields-header">
                  <label className="input-label">Trường tùy chỉnh</label>
                  <button
                    type="button"
                    className="add-field-btn"
                    onClick={() => setCustomFields([...customFields, { key: '', value: '', type: 'text' }])}
                  >
                    <PlusCircle size={16} />
                    Thêm trường
                  </button>
                </div>
                
                {customFields.map((field, index) => (
                  <div key={index} className="custom-field-item">
                    <div className="custom-field-row-top">
                      <input
                        type="text"
                        className="input"
                        placeholder="Tên trường (VD: PIN, Câu hỏi bảo mật...)"
                        value={field.key}
                        onChange={(e) => {
                          const updated = [...customFields];
                          updated[index].key = e.target.value;
                          setCustomFields(updated);
                        }}
                      />
                      <div className="custom-field-actions">
                        <button
                          type="button"
                          className={`field-type-btn ${field.type === 'password' ? 'active' : ''}`}
                          onClick={() => {
                            const updated = [...customFields];
                            updated[index].type = field.type === 'password' ? 'text' : 'password';
                            setCustomFields(updated);
                          }}
                          title={field.type === 'password' ? 'Đây là trường mật khẩu' : 'Click để đánh dấu là mật khẩu'}
                        >
                          <Lock size={14} />
                        </button>
                        <button
                          type="button"
                          className="remove-field-btn"
                          onClick={() => {
                            setCustomFields(customFields.filter((_, i) => i !== index));
                          }}
                        >
                          <X size={14} />
                        </button>
                      </div>
                    </div>
                    <div className="custom-field-row-bottom">
                      <input
                        type={field.type === 'password' && !visibleCustomFields.has(index) ? 'password' : 'text'}
                        className="input"
                        placeholder="Giá trị"
                        value={field.value}
                        onChange={(e) => {
                          const updated = [...customFields];
                          updated[index].value = e.target.value;
                          setCustomFields(updated);
                        }}
                      />
                      {field.type === 'password' && (
                        <button
                          type="button"
                          className="field-visibility-btn"
                          onClick={() => {
                            const newSet = new Set(visibleCustomFields);
                            if (newSet.has(index)) {
                              newSet.delete(index);
                            } else {
                              newSet.add(index);
                            }
                            setVisibleCustomFields(newSet);
                          }}
                        >
                          {visibleCustomFields.has(index) ? <EyeOff size={14} /> : <Eye size={14} />}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <div className="input-group">
                <label className="input-label">Ghi chú</label>
                <textarea
                  className="input"
                  placeholder="Ghi chú thêm..."
                  rows={3}
                  value={formData.notes}
                  onChange={(e) => setFormData({...formData, notes: e.target.value})}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                  Hủy
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingAccount ? 'Cập nhật' : 'Thêm mới'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Password Generator Modal */}
      {showPasswordGenerator && (
        <PasswordGenerator
          isModal={true}
          onClose={() => setShowPasswordGenerator(false)}
          onSelect={(password) => {
            setFormData({...formData, password});
            setPasswordStrength(checkPasswordStrength(password));
            setShowPasswordGenerator(false);
          }}
        />
      )}
    </div>
  );
}
