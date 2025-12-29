export interface PasswordStrength {
  score: number; // 0-4
  label: string; // 'Weak', 'Fair', 'Good', 'Strong'
  color: string; // Color code
  feedback: string[];
}

export function checkPasswordStrength(password: string): PasswordStrength {
  let score = 0;
  const feedback: string[] = [];

  if (!password) {
    return { score: 0, label: 'Trống', color: '#e0e0e0', feedback: ['Nhập mật khẩu'] };
  }

  // Length check
  if (password.length >= 8) score += 1;
  else feedback.push('Mật khẩu nên có ít nhất 8 ký tự');

  // Complexity checks
  if (/[A-Z]/.test(password)) score += 1; // Uppercase
  else feedback.push('Thêm chữ hoa');

  if (/[0-9]/.test(password)) score += 1; // Number
  else feedback.push('Thêm số');

  if (/[^A-Za-z0-9]/.test(password)) score += 1; // Special char
  else feedback.push('Thêm ký tự đặc biệt');

  // Determine label and color based on score
  let label = '';
  let color = '';

  switch (score) {
    case 0:
    case 1:
      label = 'Yếu';
      color = '#ef4444'; // Red
      break;
    case 2:
      label = 'Trung bình';
      color = '#f59e0b'; // Amber
      break;
    case 3:
      label = 'Khá';
      color = '#10b981'; // Emerald
      break;
    case 4:
      label = 'Mạnh';
      color = '#3b82f6'; // Blue
      break;
  }

  return { score, label, color, feedback };
}

export const MIN_REQUIRED_STRENGTH_SCORE = 4;
