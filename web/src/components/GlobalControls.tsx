import { useState, useEffect } from 'react';
import { Sun, Moon } from 'lucide-react';
import './GlobalControls.css';

type Theme = 'light' | 'dark';
type Language = 'vi' | 'en';

export function GlobalControls() {
  const [theme, setTheme] = useState<Theme>(() => {
    return (localStorage.getItem('theme') as Theme) || 'light';
  });
  
  const [language, setLanguage] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  const toggleLanguage = () => {
    const newLang = language === 'vi' ? 'en' : 'vi';
    setLanguage(newLang);
    localStorage.setItem('language', newLang);
    // Dispatch custom event to notify all components
    window.dispatchEvent(new CustomEvent('languageChange', { detail: newLang }));
  };

  return (
    <div className="global-controls">
      <button 
        className="global-control-btn theme-btn" 
        onClick={toggleTheme} 
        title={theme === 'light' ? 'Dark mode' : 'Light mode'}
      >
        {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
      </button>
      <button 
        className="global-control-btn lang-btn" 
        onClick={toggleLanguage} 
        title={language === 'vi' ? 'English' : 'Tiếng Việt'}
      >
        {language === 'vi' ? '🇻🇳' : '🇺🇸'}
      </button>
    </div>
  );
}
