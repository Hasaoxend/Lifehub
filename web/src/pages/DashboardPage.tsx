import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useAccounts } from '../hooks/useAccounts';
import { useTOTP } from '../hooks/useTOTP';
import { useNotes } from '../hooks/useNotes';
import { useTasks } from '../hooks/useTasks';
import { useCalendar } from '../hooks/useCalendar';
import { useLanguage } from '../hooks/useLanguage';
import { 
  Key, 
  Shield, 
  ShieldCheck,
  StickyNote, 
  CheckSquare, 
  Clock,
  TrendingUp,
  Lock,
  Calendar as CalendarIcon,
  MapPin
} from 'lucide-react';
import './DashboardPage.css';

export function DashboardPage() {
  const { user } = useAuth();
  const { accounts } = useAccounts();
  const { totpAccounts } = useTOTP();
  const { notes } = useNotes();
  const { tasks } = useTasks();
  const { events } = useCalendar();
  const language = useLanguage();

  const t = {
    accounts: language === 'vi' ? 'Tài khoản' : 'Accounts',
    authenticator: 'Authenticator',
    calendar: language === 'vi' ? 'Lịch trình' : 'Calendar',
    notes: language === 'vi' ? 'Ghi chú' : 'Notes',
    upcomingEvents: language === 'vi' ? 'Sự kiện sắp tới' : 'Upcoming Events',
    noEvents: language === 'vi' ? 'Không có sự kiện nào sắp tới' : 'No upcoming events',
    viewCalendar: language === 'vi' ? 'Xem lịch' : 'View calendar',
    pendingTasks: language === 'vi' ? 'Công việc cần làm' : 'Pending Tasks',
    allDone: language === 'vi' ? 'Đã hoàn thành tất cả công việc!' : 'All tasks completed!',
    deadline: language === 'vi' ? 'Hạn' : 'Due',
    viewAll: language === 'vi' ? 'Xem tất cả' : 'View all',
    recentNotes: language === 'vi' ? 'Ghi chú mới nhất' : 'Recent Notes',
    noNotes: language === 'vi' ? 'Chưa có ghi chú nào' : 'No notes yet',
    noTitle: language === 'vi' ? 'Không có tiêu đề' : 'No title',
    quickAccess: language === 'vi' ? 'Truy cập nhanh' : 'Quick Access',
    passwords: language === 'vi' ? 'Mật khẩu' : 'Passwords',
    securityStatus: language === 'vi' ? 'Trạng thái bảo mật' : 'Security Status',
    good: language === 'vi' ? 'Tốt' : 'Good',
    tip: language === 'vi' 
      ? 'Mẹo: Sử dụng mật khẩu mạnh và bật xác thực 2 yếu tố cho tất cả tài khoản quan trọng.'
      : 'Tip: Use strong passwords and enable 2-factor authentication for all important accounts.'
  };

  const currentHour = new Date().getHours();
  let greeting = language === 'vi' ? 'Chào buổi sáng' : 'Good morning';
  if (currentHour >= 12 && currentHour < 18) {
    greeting = language === 'vi' ? 'Chào buổi chiều' : 'Good afternoon';
  } else if (currentHour >= 18) {
    greeting = language === 'vi' ? 'Chào buổi tối' : 'Good evening';
  }

  const stats = [
    {
      label: t.accounts,
      value: accounts.length,
      icon: <Key size={24} />,
      color: 'var(--primary)',
      gradient: 'var(--gradient-primary)'
    },
    {
      label: t.authenticator,
      value: totpAccounts.length,
      icon: <ShieldCheck size={24} />,
      color: 'var(--secondary)',
      gradient: 'var(--gradient-secondary)'
    },
    {
      label: t.calendar,
      value: events.length,
      icon: <CalendarIcon size={24} />,
      color: '#8e2de2',
      gradient: 'var(--gradient-purple)'
    },
    {
      label: t.notes,
      value: notes.length,
      icon: <StickyNote size={24} />,
      color: '#f59e0b',
      gradient: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)'
    },
  ];

  const formatDate = (date?: Date) => {
    if (!date) return '';
    return new Intl.DateTimeFormat(language === 'vi' ? 'vi-VN' : 'en-US', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  };

  const upcomingEvents = events
    .filter(e => e.startTime > new Date())
    .slice(0, 3);

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-greeting">{greeting}!</h1>
          <p className="dashboard-subtitle">{user?.email}</p>
        </div>
        <div className="dashboard-time">
          <Clock size={18} />
          <span>{new Date().toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US', { 
            weekday: 'long', 
            year: 'numeric', 
            month: 'long', 
            day: 'numeric' 
          })}</span>
        </div>
      </div>

      <div className="stats-grid">
        {stats.map((stat, index) => (
          <div key={index} className="stat-card">
            <div className="stat-icon" style={{ background: stat.gradient }}>
              {stat.icon}
            </div>
            <div className="stat-content">
              <span className="stat-value">{stat.value}</span>
              <span className="stat-label">{stat.label}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="dashboard-grid">
        <div className="dashboard-section card-box">
          <h2 className="section-title">
            <CalendarIcon size={20} />
            {t.upcomingEvents}
          </h2>
          <div className="recent-list">
            {upcomingEvents.length === 0 ? (
              <p className="empty-text">{t.noEvents}</p>
            ) : (
              upcomingEvents.map(event => (
                <div key={event.documentId} className="recent-item">
                  <div className="recent-item-info">
                    <span className="recent-item-title" style={{ color: event.color }}>{event.title}</span>
                    <span className="recent-item-date">{formatDate(event.startTime)}</span>
                  </div>
                  {event.location && (
                    <p className="recent-item-snippet">
                      <MapPin size={12} style={{ display: 'inline', marginRight: '4px' }} />
                      {event.location}
                    </p>
                  )}
                </div>
              ))
            )}
          </div>
          <Link to="/calendar" className="view-all">{t.viewCalendar}</Link>
        </div>

        <div className="dashboard-section card-box">
          <h2 className="section-title">
            <CheckSquare size={20} />
            {t.pendingTasks}
          </h2>
          <div className="recent-list">
            {tasks.filter(t => !t.completed).slice(0, 5).length === 0 ? (
              <p className="empty-text">{t.allDone}</p>
            ) : (
              tasks.filter(task => !task.completed).slice(0, 5).map(task => (
                <div key={task.documentId} className="recent-item task-row">
                  <CheckSquare size={16} className="text-muted" />
                  <div className="recent-item-info">
                    <span className="recent-item-title">{task.name}</span>
                    {task.reminderTime && (
                      <span className="recent-item-deadline text-primary">{t.deadline}: {formatDate(task.reminderTime)}</span>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
          <Link to="/tasks" className="view-all">{t.viewAll}</Link>
        </div>
      </div>

      <div className="dashboard-grid mt-1.5">
        <div className="dashboard-section card-box">
          <h2 className="section-title">
            <StickyNote size={20} />
            {t.recentNotes}
          </h2>
          <div className="recent-list">
            {notes.slice(0, 3).length === 0 ? (
              <p className="empty-text">{t.noNotes}</p>
            ) : (
              notes.slice(0, 3).map(note => (
                <div key={note.documentId} className="recent-item">
                  <div className="recent-item-info">
                    <span className="recent-item-title">{note.title || t.noTitle}</span>
                    <span className="recent-item-date">{formatDate(note.lastModified)}</span>
                  </div>
                  <p className="recent-item-snippet">{note.content?.substring(0, 50)}...</p>
                </div>
              ))
            )}
          </div>
          <Link to="/notes" className="view-all">{t.viewAll}</Link>
        </div>

        <div className="dashboard-section card-box">
          <h2 className="section-title">
            <TrendingUp size={20} />
            {t.quickAccess}
          </h2>
          <div className="quick-actions" style={{ gridTemplateColumns: '1fr' }}>
            <Link to="/accounts" className="quick-action-card">
              <div className="quick-action-icon">
                <Key size={20} />
              </div>
              <div className="quick-action-content">
                <h3>{t.passwords}</h3>
              </div>
            </Link>
            <Link to="/authenticator" className="quick-action-card">
              <div className="quick-action-icon secondary">
                <Shield size={20} />
              </div>
              <div className="quick-action-content">
                <h3>{t.authenticator}</h3>
              </div>
            </Link>
            <Link to="/calendar" className="quick-action-card">
              <div className="quick-action-icon warning" style={{ background: 'var(--gradient-purple)' }}>
                <CalendarIcon size={20} />
              </div>
              <div className="quick-action-content">
                <h3>{t.calendar}</h3>
              </div>
            </Link>
          </div>
        </div>
      </div>

      <div className="dashboard-section mt-1.5">
        <h2 className="section-title">
          <Lock size={20} />
          {t.securityStatus}
        </h2>
        <div className="security-card">
          <div className="security-status good">
            <span className="security-icon">✓</span>
            <span>{t.good}</span>
          </div>
          <div className="security-info">
            <p className="security-tip">{t.tip}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
