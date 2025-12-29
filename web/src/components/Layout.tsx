import { useNavigate, Outlet, Navigate } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { MasterPasswordPrompt } from './MasterPasswordPrompt';
import { ForcePasswordChange } from './ForcePasswordChange';
import { GlobalControls } from './GlobalControls';
import { useAuth } from '../hooks/useAuth';
import './Layout.css';
import { useEffect, useState } from 'react';
import { Menu } from 'lucide-react';

export function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { 
    user, 
    loading: authLoading, 
    isUnlocked, 
    needsPasscodeSetup,
    requiresPasswordChange,
    unlockWithPasscode 
  } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (needsPasscodeSetup) {
      navigate('/passcode-setup');
    }
  }, [needsPasscodeSetup, navigate]);

  if (authLoading) {
    const lang = localStorage.getItem('language') || 'vi';
    return (
      <div className="loading-screen">
        <div className="loader"></div>
        <p>{lang === 'vi' ? 'Đang tải...' : 'Loading...'}</p>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Enforce email verification
  if (!user.emailVerified) {
    return <Navigate to="/verify-email" replace />;
  }


  // Show unlock prompt if user is logged in but encryption key is missing
  // AND they already have a setup (no needsPasscodeSetup)
  // AND they are not already on the setup page
  if (!isUnlocked && !needsPasscodeSetup && window.location.pathname !== '/passcode-setup') {
    return (
      <MasterPasswordPrompt 
        onUnlock={(passcode) => unlockWithPasscode(passcode)} 
      />
    );
  }

  // After passcode unlock, check if password strength verification is required
  if (isUnlocked && requiresPasswordChange && window.location.pathname !== '/change-password') {
    return <Navigate to="/change-password?forced=true" replace />;
  }


  return (
    <div className="layout">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      {sidebarOpen && (
        <div 
          className="sidebar-overlay" 
          onClick={() => setSidebarOpen(false)}
        />
      )}
      <main className="main-content">
        <button 
          className="mobile-menu-btn"
          onClick={() => setSidebarOpen(true)}
        >
          <Menu size={24} />
        </button>
        <GlobalControls />
        <Outlet />
      </main>
    </div>
  );
}

