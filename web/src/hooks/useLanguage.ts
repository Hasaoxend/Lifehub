import { useState, useEffect } from 'react';

type Language = 'vi' | 'en';

/**
 * Hook that provides reactive language state.
 * Listens for 'languageChange' custom events from GlobalControls.
 */
export function useLanguage(): Language {
  const [language, setLanguage] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  useEffect(() => {
    const handleLanguageChange = (event: CustomEvent<Language>) => {
      setLanguage(event.detail);
    };

    window.addEventListener('languageChange', handleLanguageChange as EventListener);
    
    return () => {
      window.removeEventListener('languageChange', handleLanguageChange as EventListener);
    };
  }, []);

  return language;
}
