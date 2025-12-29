import { useState, useEffect } from 'react';
import { RefreshCw, Copy, Check, X } from 'lucide-react';
import './PasswordGenerator.css';

type Language = 'vi' | 'en';

interface PasswordGeneratorProps {
  onSelect?: (password: string) => void;
  onClose?: () => void;
  isModal?: boolean;
}

const CHAR_LOWER = 'abcdefghijklmnopqrstuvwxyz';
const CHAR_UPPER = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
const NUMBERS = '0123456789';
const SYMBOLS = '!@#$%&*_-.()[]{}';

export function PasswordGenerator({ onSelect, onClose, isModal = false }: PasswordGeneratorProps) {
  const [password, setPassword] = useState('');
  const [length, setLength] = useState(16);
  const [useUppercase, setUseUppercase] = useState(true);
  const [useNumbers, setUseNumbers] = useState(true);
  const [useSymbols, setUseSymbols] = useState(true);
  const [copied, setCopied] = useState(false);
  const [language] = useState<Language>(() => {
    return (localStorage.getItem('language') as Language) || 'vi';
  });

  const t = {
    title: language === 'vi' ? 'Tạo mật khẩu' : 'Generate Password',
    length: language === 'vi' ? 'Độ dài' : 'Length',
    uppercase: language === 'vi' ? 'Chữ hoa (A-Z)' : 'Uppercase (A-Z)',
    numbers: language === 'vi' ? 'Số (0-9)' : 'Numbers (0-9)',
    symbols: language === 'vi' ? 'Ký tự đặc biệt (!@#...)' : 'Symbols (!@#...)',
    regenerate: language === 'vi' ? 'Tạo lại' : 'Regenerate',
    copy: language === 'vi' ? 'Sao chép' : 'Copy',
    copied: language === 'vi' ? 'Đã sao chép' : 'Copied',
    select: language === 'vi' ? 'Chọn mật khẩu này' : 'Use this password'
  };

  const generatePassword = () => {
    // Build character pool
    let charPool = CHAR_LOWER;
    const guaranteedSets: string[] = [CHAR_LOWER];

    if (useUppercase) {
      charPool += CHAR_UPPER;
      guaranteedSets.push(CHAR_UPPER);
    }
    if (useNumbers) {
      charPool += NUMBERS;
      guaranteedSets.push(NUMBERS);
    }
    if (useSymbols) {
      charPool += SYMBOLS;
      guaranteedSets.push(SYMBOLS);
    }

    // Generate password with guaranteed characters from each set
    const chars: string[] = [];
    
    // Add one character from each guaranteed set
    for (const set of guaranteedSets) {
      chars.push(set[Math.floor(Math.random() * set.length)]);
    }

    // Fill remaining length with random chars from pool
    for (let i = chars.length; i < length; i++) {
      chars.push(charPool[Math.floor(Math.random() * charPool.length)]);
    }

    // Shuffle the array (Fisher-Yates)
    for (let i = chars.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [chars[i], chars[j]] = [chars[j], chars[i]];
    }

    setPassword(chars.join(''));
  };

  useEffect(() => {
    generatePassword();
  }, [length, useUppercase, useNumbers, useSymbols]);

  const copyToClipboard = () => {
    navigator.clipboard.writeText(password);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSelect = () => {
    if (onSelect) {
      onSelect(password);
    }
    if (onClose) {
      onClose();
    }
  };

  // Ensure at least one option besides lowercase is always available
  const canDisableUppercase = useNumbers || useSymbols;
  const canDisableNumbers = useUppercase || useSymbols;
  const canDisableSymbols = useUppercase || useNumbers;

  const content = (
    <div className="password-generator">
      <div className="pg-header">
        <h3>{t.title}</h3>
        {isModal && onClose && (
          <button className="pg-close" onClick={onClose}>
            <X size={20} />
          </button>
        )}
      </div>

      {/* Generated Password Display */}
      <div className="pg-password-display">
        <input
          type="text"
          value={password}
          readOnly
          className="pg-password-input"
        />
        <button className="pg-icon-btn" onClick={copyToClipboard} title={t.copy}>
          {copied ? <Check size={18} /> : <Copy size={18} />}
        </button>
        <button className="pg-icon-btn" onClick={generatePassword} title={t.regenerate}>
          <RefreshCw size={18} />
        </button>
      </div>

      {/* Length Slider */}
      <div className="pg-option">
        <label className="pg-option-label">
          {t.length}: <strong>{length}</strong>
        </label>
        <input
          type="range"
          min={8}
          max={64}
          value={length}
          onChange={(e) => setLength(parseInt(e.target.value))}
          className="pg-slider"
        />
        <div className="pg-slider-labels">
          <span>8</span>
          <span>64</span>
        </div>
      </div>

      {/* Character Options */}
      <div className="pg-options">
        <label className={`pg-checkbox ${!canDisableUppercase ? 'disabled' : ''}`}>
          <input
            type="checkbox"
            checked={useUppercase}
            onChange={(e) => canDisableUppercase && setUseUppercase(e.target.checked)}
            disabled={!canDisableUppercase && useUppercase}
          />
          <span>{t.uppercase}</span>
        </label>

        <label className={`pg-checkbox ${!canDisableNumbers ? 'disabled' : ''}`}>
          <input
            type="checkbox"
            checked={useNumbers}
            onChange={(e) => canDisableNumbers && setUseNumbers(e.target.checked)}
            disabled={!canDisableNumbers && useNumbers}
          />
          <span>{t.numbers}</span>
        </label>

        <label className={`pg-checkbox ${!canDisableSymbols ? 'disabled' : ''}`}>
          <input
            type="checkbox"
            checked={useSymbols}
            onChange={(e) => canDisableSymbols && setUseSymbols(e.target.checked)}
            disabled={!canDisableSymbols && useSymbols}
          />
          <span>{t.symbols}</span>
        </label>
      </div>

      {/* Action Buttons */}
      <div className="pg-actions">
        {onSelect && (
          <button className="btn btn-primary pg-select-btn" onClick={handleSelect}>
            {t.select}
          </button>
        )}
      </div>
    </div>
  );

  if (isModal) {
    return (
      <div className="pg-modal-overlay" onClick={onClose}>
        <div className="pg-modal" onClick={(e) => e.stopPropagation()}>
          {content}
        </div>
      </div>
    );
  }

  return content;
}
