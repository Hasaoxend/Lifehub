import { useState, useMemo } from 'react';
import { 
  ChevronLeft, 
  ChevronRight, 
  Plus, 
  Clock,
  LayoutGrid,
  Calendar as CalendarIcon,
  User,
  Repeat,
  Filter,
  Check
} from 'lucide-react';
import { useCalendar, CalendarEvent } from '../hooks/useCalendar';
import { useLanguage } from '../hooks/useLanguage';
import { EventDrawer } from '../components/EventDrawer';
import { convertSolarToLunar } from '../utils/lunarUtils';
import { getVietnameseHoliday } from '../utils/holidayUtils';
import './CalendarPage.css';

type ViewMode = 'month' | 'day';

export function CalendarPage() {
  const { events, loading, error, addEvent, updateEvent, deleteEvent } = useCalendar();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('month');
  const language = useLanguage();

  const t = {
    loading: language === 'vi' ? 'Đang tải lịch trình...' : 'Loading schedule...',
    error: language === 'vi' ? 'Lỗi: ' : 'Error: ',
    confirmDelete: language === 'vi' ? 'Bạn có chắc muốn xóa sự kiện này?' : 'Are you sure you want to delete this event?',
    back: language === 'vi' ? 'Quay lại' : 'Back',
    today: language === 'vi' ? 'Hôm nay' : 'Today',
    show: language === 'vi' ? 'Hiển thị' : 'View',
    lunar: language === 'vi' ? 'Lịch âm' : 'Lunar Calendar',
    holidays: language === 'vi' ? 'Ngày lễ' : 'Holidays',
    create: language === 'vi' ? 'Tạo lịch trình' : 'Create Event',
    more: language === 'vi' ? 'thêm' : 'more' 
  };
  
  // Drawer state
  const [showEventDrawer, setShowEventDrawer] = useState(false);
  const [editingEvent, setEditingEvent] = useState<CalendarEvent | null>(null);
  
  // Filter state
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState({
    lunarDates: true,
    holidays: true
  });

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // Build event map for quick lookup
  const eventMap = useMemo(() => {
    const map: Record<string, CalendarEvent[]> = {};
    events.forEach(event => {
      const startDate = new Date(event.startTime);
      const endDate = new Date(event.endTime);
      
      // Add event to all days it spans
      let current = new Date(startDate);
      current.setHours(0, 0, 0, 0);
      const end = new Date(endDate);
      end.setHours(0, 0, 0, 0);
      
      while (current <= end) {
        const key = current.toISOString().split('T')[0];
        if (!map[key]) map[key] = [];
        map[key].push(event);
        current.setDate(current.getDate() + 1);
      }
    });
    return map;
  }, [events]);

  const handleDayClick = (day: number) => {
    const date = new Date(year, month, day);
    setSelectedDate(date);
    setViewMode('day');
  };

  const handleTimeSlotClick = (hour: number) => {
    const date = selectedDate || currentDate;
    const newDate = new Date(date);
    newDate.setHours(hour, 0, 0, 0);
    setSelectedDate(newDate);
    setEditingEvent(null);
    setShowEventDrawer(true);
  };

  const handleEventClick = (event: CalendarEvent, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingEvent(event);
    setShowEventDrawer(true);
  };

  const handleSaveEvent = async (eventData: Partial<CalendarEvent>) => {
    try {
      if (editingEvent?.documentId) {
        await updateEvent(editingEvent.documentId, eventData);
      } else {
        await addEvent(eventData as any);
      }
    } catch (err) {
      console.error('Error saving event:', err);
    }
  };

  const handleDeleteEvent = async (documentId: string) => {
    if (confirm(t.confirmDelete)) {
      await deleteEvent(documentId);
    }
  };

  const navigateMonth = (direction: number) => {
    const next = new Date(currentDate);
    next.setMonth(next.getMonth() + direction);
    setCurrentDate(next);
  };

  const goToToday = () => {
    const today = new Date();
    setCurrentDate(today);
    setSelectedDate(today);
  };

  // Calendar calculations
  const firstDayOfMonth = new Date(year, month, 1).getDay();
  const adjustedFirstDay = firstDayOfMonth === 0 ? 6 : firstDayOfMonth - 1;
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const calendarDays = useMemo(() => {
    const days: (number | null)[] = [];
    for (let i = 0; i < adjustedFirstDay; i++) days.push(null);
    for (let i = 1; i <= daysInMonth; i++) days.push(i);
    return days;
  }, [adjustedFirstDay, daysInMonth]);

  const monthNames = language === 'vi' 
    ? ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12']
    : ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    
  const weekDays = language === 'vi'
    ? ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
    : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const hours = Array.from({ length: 24 }, (_, i) => i);

  const isToday = (day: number) => {
    const today = new Date();
    return today.getFullYear() === year && today.getMonth() === month && today.getDate() === day;
  };

  // Current time position for Day view
  const now = new Date();
  const currentTimeOffset = ((now.getHours() * 60 + now.getMinutes()) / (24 * 60)) * 100;
  const isSelectedToday = selectedDate && selectedDate.toDateString() === now.toDateString();

  // Mini Calendar
  const renderMiniCalendar = () => {
    const miniDays: (number | null)[] = [];
    for (let i = 0; i < adjustedFirstDay; i++) miniDays.push(null);
    for (let i = 1; i <= daysInMonth; i++) miniDays.push(i);

    return (
      <div className="mini-calendar">
        <div className="mini-calendar-header">
          <button className="mini-nav-btn" onClick={() => navigateMonth(-1)}><ChevronLeft size={16} /></button>
          <span className="mini-month-title">{monthNames[month]} {year}</span>
          <button className="mini-nav-btn" onClick={() => navigateMonth(1)}><ChevronRight size={16} /></button>
        </div>
        <div className="mini-calendar-grid">
          {weekDays.map(d => (
            <div key={d} className="mini-weekday">{d}</div>
          ))}
          {miniDays.map((day, idx) => {
            if (!day) return <div key={idx} className="mini-day empty" />;
            const dateKey = new Date(year, month, day).toISOString().split('T')[0];
            const hasEvents = eventMap[dateKey] && eventMap[dateKey].length > 0;
            const isSelected = selectedDate && selectedDate.getDate() === day && 
              selectedDate.getMonth() === month && selectedDate.getFullYear() === year;
            const isTodayMini = isToday(day);
            
            return (
              <div 
                key={idx} 
                className={`mini-day ${isTodayMini ? 'today' : ''} ${isSelected ? 'selected' : ''}`}
                onClick={() => handleDayClick(day)}
              >
                {day}
                {hasEvents && <div className="mini-event-dot" />}
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  // Month View
  const renderMonthView = () => (
    <div className="month-view-grid">
      {weekDays.map(d => <div key={d} className="month-weekday-header">{d}</div>)}
      {calendarDays.map((day, idx) => {
        if (!day) return <div key={idx} className="month-day empty" />;
        
        const date = new Date(year, month, day);
        const dateKey = date.toISOString().split('T')[0];
        const dayEvents = eventMap[dateKey] || [];
        const holiday = getVietnameseHoliday(date, language);
        const lunar = convertSolarToLunar(date);
        const isSat = idx % 7 === 5;
        const isSun = idx % 7 === 6;

        return (
          <div 
            key={idx} 
            className={`month-day ${isToday(day) ? 'today' : ''} ${isSat ? 'saturday' : ''} ${isSun ? 'sunday' : ''}`}
            onClick={() => handleDayClick(day)}
          >
            <div className="month-day-header">
              <span className={`month-day-number ${isToday(day) ? 'today-badge' : ''}`}>{day}</span>
              {filters.lunarDates && <span className="month-lunar-day">{lunar.day}/{lunar.month}</span>}
            </div>
            {filters.holidays && holiday && <div className="holiday-tag">{holiday}</div>}

            <div className="month-events">
              {dayEvents.slice(0, 3).map((event, i) => (
                <div 
                  key={event.documentId || i}
                  className="month-event-bar"
                  style={{ 
                    background: event.color,
                    borderColor: event.color 
                  }}
                  onClick={(e) => handleEventClick(event, e)}
                >
                  {event.repeatType && event.repeatType !== 'NONE' && <Repeat size={10} />}
                  <span>{event.title}</span>
                </div>
              ))}
              {dayEvents.length > 3 && <div className="month-more-events">+{dayEvents.length - 3} {t.more}</div>}
            </div>
          </div>
        );
      })}
    </div>
  );

  // Day View
  const renderDayView = () => {
    const dateKey = selectedDate ? selectedDate.toISOString().split('T')[0] : '';
    const dayEvents = eventMap[dateKey] || [];

    return (
      <div className="day-view-container">
        {renderMiniCalendar()}
        <div className="day-view-main">
          <div className="day-timeline">
            {/* Time Column */}
            <div className="time-column">
              {hours.map(hour => (
                <div key={hour} className="time-slot">
                  {hour.toString().padStart(2, '0')}:00
                </div>
              ))}
            </div>
            {/* Events Column */}
            <div className="events-column">
              {hours.map(hour => (
                <div 
                  key={hour} 
                  className="hour-slot"
                  onClick={() => handleTimeSlotClick(hour)}
                />
              ))}
              
              {/* Current Time Indicator */}
              {isSelectedToday && (
                <div className="current-time-line" style={{ top: `${currentTimeOffset}%` }}>
                  <div className="current-time-dot" />
                </div>
              )}

              {/* Events with overlap detection */}
              {(() => {
                // Calculate overlap groups
                const selectedDay = selectedDate || currentDate;
                const processedEvents = dayEvents.map((event, idx) => {
                  const start = new Date(event.startTime);
                  const end = new Date(event.endTime);
                  
                  const eventStartDay = new Date(start.getFullYear(), start.getMonth(), start.getDate());
                  const eventEndDay = new Date(end.getFullYear(), end.getMonth(), end.getDate());
                  const currentDayDate = new Date(selectedDay.getFullYear(), selectedDay.getMonth(), selectedDay.getDate());
                  const isMultiDay = eventEndDay.getTime() > eventStartDay.getTime();
                  const isStartDay = currentDayDate.getTime() === eventStartDay.getTime();
                  const isEndDay = currentDayDate.getTime() === eventEndDay.getTime();
                  const isMiddleDay = currentDayDate.getTime() > eventStartDay.getTime() && currentDayDate.getTime() < eventEndDay.getTime();
                  
                  let startMins = 0;
                  let endMins = 24 * 60;
                  
                  if (isMultiDay) {
                    if (isStartDay) {
                      startMins = start.getHours() * 60 + start.getMinutes();
                      endMins = 24 * 60;
                    } else if (isEndDay) {
                      startMins = 0;
                      endMins = end.getHours() * 60 + end.getMinutes();
                    }
                  } else {
                    startMins = start.getHours() * 60 + start.getMinutes();
                    endMins = end.getHours() * 60 + end.getMinutes();
                  }
                  
                  return { event, idx, startMins, endMins, isMultiDay, isStartDay, isEndDay, isMiddleDay, start, end };
                });
                
                // Find overlapping events and assign columns
                const columns: number[] = new Array(processedEvents.length).fill(0);
                const maxColumns: number[] = new Array(processedEvents.length).fill(1);
                
                for (let i = 0; i < processedEvents.length; i++) {
                  const ev1 = processedEvents[i];
                  let overlappingIndices = [i];
                  
                  for (let j = 0; j < processedEvents.length; j++) {
                    if (i === j) continue;
                    const ev2 = processedEvents[j];
                    
                    // Check if events overlap
                    if (ev1.startMins < ev2.endMins && ev1.endMins > ev2.startMins) {
                      overlappingIndices.push(j);
                    }
                  }
                  
                  // Assign columns to overlapping events
                  overlappingIndices.sort((a, b) => a - b);
                  const totalOverlapping = overlappingIndices.length;
                  const colIndex = overlappingIndices.indexOf(i);
                  
                  columns[i] = colIndex;
                  maxColumns[i] = totalOverlapping;
                }
                
                return processedEvents.map(({ event, idx, startMins, endMins, isMultiDay, isStartDay, isEndDay, isMiddleDay, start, end }) => {
                  const topPercent = (startMins / (24 * 60)) * 100;
                  const heightPercent = Math.max(((endMins - startMins) / (24 * 60)) * 100, 2.5);
                  
                  const column = columns[idx];
                  const totalColumns = maxColumns[idx];
                  const widthPercent = 100 / totalColumns;
                  const leftPercent = column * widthPercent;
                  
                  return (
                    <div
                      key={event.documentId || idx}
                      className={`day-event-block ${isMultiDay ? 'multi-day' : ''} ${isStartDay ? 'start-day' : ''} ${isEndDay ? 'end-day' : ''} ${isMiddleDay ? 'middle-day' : ''}`}
                      style={{
                        top: `${topPercent}%`,
                        height: `${heightPercent}%`,
                        '--event-color': event.color,
                        left: `calc(${leftPercent}% + 2px)`,
                        width: `calc(${widthPercent}% - 4px)`,
                        right: 'auto'
                      } as any}
                      onClick={(e) => handleEventClick(event, e)}
                    >
                      <div className="event-solid-edge" style={{ background: event.color }} />
                      <div className="event-content" style={{ background: isMultiDay ? `${event.color}30` : event.color }}>
                        <div className="day-event-title">
                          {event.repeatType && event.repeatType !== 'NONE' && <Repeat size={12} />}
                          {event.title}
                        </div>
                        <div className="day-event-time">
                          <Clock size={10} /> 
                          {isMultiDay ? (
                            <>
                              {start.toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US', { day: '2-digit', month: '2-digit' })} {start.toTimeString().slice(0, 5)} → {end.toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US', { day: '2-digit', month: '2-digit' })} {end.toTimeString().slice(0, 5)}
                            </>
                          ) : (
                            <>{start.toTimeString().slice(0, 5)} - {end.toTimeString().slice(0, 5)}</>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                });
              })()}


            </div>
          </div>
        </div>
      </div>
    );
  };

  if (loading && events.length === 0) {
    return <div className="page-loading"><div className="loader" /><p>{t.loading}</p></div>;
  }

  return (
    <div className="calendar-page">
      {/* Header */}
      <div className="calendar-header">
        <div className="header-left">
          {viewMode === 'day' && (
            <button className="btn btn-secondary" onClick={() => setViewMode('month')}>
              <ChevronLeft size={18} /> {t.back}
            </button>
          )}
          <button className="btn btn-outline" onClick={goToToday}>{t.today}</button>
          {viewMode === 'month' && (
            <>
              <button className="nav-btn" onClick={() => navigateMonth(-1)}><ChevronLeft size={20} /></button>
              <button className="nav-btn" onClick={() => navigateMonth(1)}><ChevronRight size={20} /></button>
              <h2 className="current-month-title">{monthNames[month]} {year}</h2>
            </>
          )}
          {viewMode === 'day' && selectedDate && (
            <h2 className="current-month-title">
              {selectedDate.toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
            </h2>
          )}
        </div>
        <div className="header-right">
          {/* Filter Dropdown */}
          <div className="filter-dropdown-container">
            <button 
              className={`btn btn-outline filter-btn ${showFilters ? 'active' : ''}`}
              onClick={() => setShowFilters(!showFilters)}
            >
              <Filter size={18} /> {t.show}
            </button>
            {showFilters && (
              <div className="filter-dropdown">
                <label className="filter-option">
                  <input 
                    type="checkbox" 
                    checked={filters.lunarDates}
                    onChange={(e) => setFilters({...filters, lunarDates: e.target.checked})}
                  />
                  <span>{t.lunar}</span>
                  {filters.lunarDates && <Check size={16} />}
                </label>
                <label className="filter-option">
                  <input 
                    type="checkbox" 
                    checked={filters.holidays}
                    onChange={(e) => setFilters({...filters, holidays: e.target.checked})}
                  />
                  <span>{t.holidays}</span>
                  {filters.holidays && <Check size={16} />}
                </label>
              </div>
            )}
          </div>
          
          <button className="btn btn-primary" onClick={() => { setEditingEvent(null); setShowEventDrawer(true); }}>
            <Plus size={18} /> {t.create}
          </button>
        </div>
      </div>



      {error && <div className="error-banner">{t.error}{error}</div>}

      {/* Main Content */}
      {viewMode === 'month' && renderMonthView()}
      {viewMode === 'day' && renderDayView()}

      {/* Event Drawer */}
      <EventDrawer
        isOpen={showEventDrawer}
        onClose={() => { setShowEventDrawer(false); setEditingEvent(null); }}
        onSave={handleSaveEvent}
        onDelete={handleDeleteEvent}
        editingEvent={editingEvent}
        initialDate={selectedDate || currentDate}
      />
    </div>
  );
}
