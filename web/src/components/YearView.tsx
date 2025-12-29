import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface YearViewProps {
  year: number;
  onYearChange: (year: number) => void;
  onMonthClick: (month: number) => void;
}

export function YearView({ year, onYearChange, onMonthClick }: YearViewProps) {
  const monthNames = [
    'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
    'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
  ];

  const renderSmallMonth = (monthIndex: number) => {
    const firstDay = new Date(year, monthIndex, 1).getDay();
    const adjustedFirstDay = firstDay === 0 ? 6 : firstDay - 1;
    const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
    
    const days = [];
    for (let i = 0; i < adjustedFirstDay; i++) days.push(null);
    for (let i = 1; i <= daysInMonth; i++) days.push(i);

    const today = new Date();
    const isCurrentMonth = today.getFullYear() === year && today.getMonth() === monthIndex;

    return (
      <div 
        key={monthIndex} 
        className="small-month-card" 
        onClick={() => onMonthClick(monthIndex)}
      >
        <h3 className="small-month-name">{monthNames[monthIndex]}</h3>
        <div className="small-month-grid">
          {['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map(d => (
            <div key={d} className="small-weekday">{d[0]}</div>
          ))}
          {days.map((day, i) => (
            <div 
              key={i} 
              className={`small-day ${day === null ? 'empty' : ''} ${isCurrentMonth && day === today.getDate() ? 'today' : ''}`}
            >
              {day}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="year-view">
      <div className="year-view-header">
        <button className="nav-btn" onClick={() => onYearChange(year - 1)}>
          <ChevronLeft size={20} />
        </button>
        <h2 className="year-display">{year}</h2>
        <button className="nav-btn" onClick={() => onYearChange(year + 1)}>
          <ChevronRight size={20} />
        </button>
      </div>
      <div className="year-grid">
        {monthNames.map((_, i) => renderSmallMonth(i))}
      </div>
    </div>
  );
}
