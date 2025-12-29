import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { 
  Key, 
  StickyNote, 
  CheckSquare, 
  Calendar, 
  Settings, 
  LogOut,
  Shield,
  LayoutDashboard,
  ShoppingCart,
  ChevronDown,
  ChevronRight,
  Briefcase
} from 'lucide-react';
import { useState } from 'react';
import './Sidebar.css';

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user, signOut } = useAuth();
  const language = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [expandedSections, setExpandedSections] = useState({
    security: false,
    tasks: false
  });

  const t = {
    dashboard: 'Dashboard',
    security: language === 'vi' ? 'Bảo mật' : 'Security',
    accounts: language === 'vi' ? 'Tài khoản' : 'Accounts',
    authenticator: 'Authenticator',
    calendar: language === 'vi' ? 'Quản lý lịch trình' : 'Calendar',
    notes: language === 'vi' ? 'Quản lý ghi chú' : 'Notes',
    work: language === 'vi' ? 'Quản lý công việc' : 'Tasks',
    tasks: language === 'vi' ? 'Công việc' : 'Tasks',
    shopping: language === 'vi' ? 'Mua sắm' : 'Shopping',
    settings: language === 'vi' ? 'Cài đặt' : 'Settings',
    online: 'Online'
  };

  const toggleSection = (section: 'security' | 'tasks') => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const handleSignOut = async () => {
    try {
      await signOut();
      navigate('/login');
    } catch (error) {
      console.error('Error signing out:', error);
    }
  };

  const isSecurityActive = location.pathname === '/accounts' || location.pathname === '/authenticator';
  const isTasksActive = location.pathname === '/tasks' || location.pathname === '/shopping';

  return (
    <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-header">
        <div className="logo">
          <div className="logo-icon">
            <Key size={24} />
          </div>
          <span className="logo-text">LifeHub</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <span className="nav-icon"><LayoutDashboard size={20} /></span>
          <span className="nav-label">{t.dashboard}</span>
        </NavLink>

        <div className="nav-section">
          <div 
            className={`nav-item expandable ${isSecurityActive ? 'active' : ''}`}
            onClick={() => toggleSection('security')}
          >
            <span className="nav-icon"><Shield size={20} /></span>
            <span className="nav-label">{t.security}</span>
            <span className="nav-chevron">
              {expandedSections.security ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            </span>
          </div>
          {expandedSections.security && (
            <div className="nav-sub-items">
              <NavLink to="/accounts" className={({ isActive }) => `nav-sub-item ${isActive ? 'active' : ''}`}>
                <Key size={16} />
                <span>{t.accounts}</span>
              </NavLink>
              <NavLink to="/authenticator" className={({ isActive }) => `nav-sub-item ${isActive ? 'active' : ''}`}>
                <Shield size={16} />
                <span>{t.authenticator}</span>
              </NavLink>
            </div>
          )}
        </div>

        <NavLink to="/calendar" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <span className="nav-icon"><Calendar size={20} /></span>
          <span className="nav-label">{t.calendar}</span>
        </NavLink>

        <NavLink to="/notes" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <span className="nav-icon"><StickyNote size={20} /></span>
          <span className="nav-label">{t.notes}</span>
        </NavLink>

        <div className="nav-section">
          <div 
            className={`nav-item expandable ${isTasksActive ? 'active' : ''}`}
            onClick={() => toggleSection('tasks')}
          >
            <span className="nav-icon"><Briefcase size={20} /></span>
            <span className="nav-label">{t.work}</span>
            <span className="nav-chevron">
              {expandedSections.tasks ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            </span>
          </div>
          {expandedSections.tasks && (
            <div className="nav-sub-items">
              <NavLink to="/tasks" className={({ isActive }) => `nav-sub-item ${isActive ? 'active' : ''}`}>
                <CheckSquare size={16} />
                <span>{t.tasks}</span>
              </NavLink>
              <NavLink to="/shopping" className={({ isActive }) => `nav-sub-item ${isActive ? 'active' : ''}`}>
                <ShoppingCart size={16} />
                <span>{t.shopping}</span>
              </NavLink>
            </div>
          )}
        </div>

        <NavLink to="/settings" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <span className="nav-icon"><Settings size={20} /></span>
          <span className="nav-label">{t.settings}</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <div className="user-info">
          <div className="user-avatar">
            {user?.email?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="user-details">
            <span className="user-email">{user?.email}</span>
            <span className="user-status">{t.online}</span>
          </div>
        </div>
        <button className="logout-btn" onClick={handleSignOut}>
          <LogOut size={18} />
        </button>
      </div>
    </aside>
  );
}
