import React, { useState, useEffect } from 'react';
import { useLanguage } from '../hooks/useLanguage';
import { X, Clock, MapPin, Repeat, Save, Zap } from 'lucide-react';
import { CalendarEvent } from '../hooks/useCalendar';

interface EventDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (event: Partial<CalendarEvent>) => void;
  onDelete?: (documentId: string) => void;
  editingEvent?: CalendarEvent | null;
  initialDate?: Date;
}



const COLORS = ['#4285f4', '#ea4335', '#34a853', '#fbbc04', '#9c27b0', '#00bcd4', '#ff5722', '#607d8b', '#e91e63', '#795548'];

// Generate random color
function getRandomColor(): string {
  return COLORS[Math.floor(Math.random() * COLORS.length)];
}

// Get current date/time strings
function getNowStrings() {
  const now = new Date();
  const endTime = new Date(now);
  endTime.setHours(endTime.getHours() + 1);
  
  return {
    startDate: now.toISOString().split('T')[0],
    startTime: now.toTimeString().slice(0, 5),
    endDate: endTime.toISOString().split('T')[0],
    endTime: endTime.toTimeString().slice(0, 5)
  };
}

export function EventDrawer({ isOpen, onClose, onSave, onDelete, editingEvent, initialDate }: EventDrawerProps) {
  const language = useLanguage();

  const t = {
    work: language === 'vi' ? 'Công việc' : 'Work',
    meeting: language === 'vi' ? 'Họp' : 'Meeting',
    event: language === 'vi' ? 'Sự kiện' : 'Event',
    personal: language === 'vi' ? 'Cá nhân' : 'Personal',
    none: language === 'vi' ? 'Không lặp' : 'No repeat',
    daily: language === 'vi' ? 'Hàng ngày' : 'Daily',
    weekly: language === 'vi' ? 'Hàng tuần' : 'Weekly',
    monthly: language === 'vi' ? 'Hàng tháng' : 'Monthly',
    yearly: language === 'vi' ? 'Hàng năm' : 'Yearly',
    editTitle: language === 'vi' ? 'Chỉnh sửa lịch trình' : 'Edit Event',
    createTitle: language === 'vi' ? 'Tạo lịch trình mới' : 'New Event',
    type: language === 'vi' ? 'Loại' : 'Type',
    titleLabel: language === 'vi' ? 'Tiêu đề' : 'Title',
    titlePlaceholder: language === 'vi' ? 'Nhập tiêu đề...' : 'Enter title...',
    setNow: language === 'vi' ? 'Đặt thời gian hiện tại' : 'Set to current time',
    startTime: language === 'vi' ? 'Thời gian bắt đầu' : 'Start Time',
    endTime: language === 'vi' ? 'Thời gian kết thúc' : 'End Time',
    locationLabel: language === 'vi' ? 'Địa điểm' : 'Location',
    locationPlaceholder: language === 'vi' ? 'Thêm địa điểm...' : 'Add location...',
    repeatLabel: language === 'vi' ? 'Lặp lại' : 'Repeat',
    notesLabel: language === 'vi' ? 'Ghi chú' : 'Notes',
    notesPlaceholder: language === 'vi' ? 'Thêm ghi chú...' : 'Add notes...',
    delete: language === 'vi' ? 'Xóa' : 'Delete',
    cancel: language === 'vi' ? 'Hủy' : 'Cancel',
    save: language === 'vi' ? 'Lưu' : 'Save',
    current: language === 'vi' ? 'Hiện tại' : 'Current',
    alertTitle: language === 'vi' ? 'Vui lòng nhập tiêu đề' : 'Please enter a title',
    alertTime: language === 'vi' ? 'Thời gian kết thúc phải sau thời gian bắt đầu' : 'End time must be after start time'
  };

  const EVENT_TYPES = [
    { value: 'work', label: t.work },
    { value: 'meeting', label: t.meeting },
    { value: 'event', label: t.event },
    { value: 'personal', label: t.personal },
  ];

  const REPEAT_OPTIONS = [
    { value: 'NONE', label: t.none },
    { value: 'DAILY', label: t.daily },
    { value: 'WEEKLY', label: t.weekly },
    { value: 'MONTHLY', label: t.monthly },
    { value: 'YEARLY', label: t.yearly },
  ];

  const [formData, setFormData] = useState({
    eventType: 'work',
    title: '',
    startDate: '',
    startTime: '09:00',
    endDate: '',
    endTime: '10:00',
    location: '',
    repeatType: 'NONE' as any,
    color: '#4285f4',
    description: ''
  });

  useEffect(() => {
    if (isOpen) {
      if (editingEvent) {
        setFormData({
          eventType: editingEvent.titleType || 'work',
          title: editingEvent.title,
          startDate: editingEvent.startTime.toISOString().split('T')[0],
          startTime: editingEvent.startTime.toTimeString().slice(0, 5),
          endDate: editingEvent.endTime.toISOString().split('T')[0],
          endTime: editingEvent.endTime.toTimeString().slice(0, 5),
          location: editingEvent.location || '',
          repeatType: editingEvent.repeatType || 'NONE',
          color: editingEvent.color || getRandomColor(),
          description: editingEvent.description || ''
        });
      } else {
        // Always use current time for new events
        const now = getNowStrings();
        
        setFormData({
          eventType: 'work',
          title: '',
          startDate: now.startDate,
          startTime: now.startTime,
          endDate: now.endDate,
          endTime: now.endTime,
          location: '',
          repeatType: 'NONE',
          color: getRandomColor(), // Random color
          description: ''
        });
      }
    }
  }, [isOpen, editingEvent]);

  // "Now" button handler
  const handleSetNow = () => {
    const now = getNowStrings();
    setFormData(prev => ({
      ...prev,
      startDate: now.startDate,
      startTime: now.startTime,
      endDate: now.endDate,
      endTime: now.endTime
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title.trim()) {
      alert(t.alertTitle);
      return;
    }
    
    const startTime = new Date(`${formData.startDate}T${formData.startTime}:00`);
    const endTime = new Date(`${formData.endDate}T${formData.endTime}:00`);

    if (endTime <= startTime) {
      alert(t.alertTime);
      return;
    }

    const eventData: Partial<CalendarEvent> = {
      title: formData.title,
      titleType: formData.eventType,
      startTime,
      endTime,
      location: formData.location,
      repeatType: formData.repeatType,
      color: formData.color,
      description: formData.description
    };

    onSave(eventData);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="drawer-overlay" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <h2>{editingEvent ? t.editTitle : t.createTitle}</h2>
          <button className="drawer-close" onClick={onClose}><X size={20} /></button>
        </div>

        <form onSubmit={handleSubmit} className="drawer-body">
          {/* Event Type Pills */}
          <div className="form-group">
            <label>{t.type}</label>
            <div className="type-pills">
              {EVENT_TYPES.map(type => (
                <button
                  key={type.value}
                  type="button"
                  className={`type-pill ${formData.eventType === type.value ? 'active' : ''}`}
                  style={{ '--pill-color': formData.color } as any}
                  onClick={() => setFormData({ ...formData, eventType: type.value })}
                >
                  {type.label}
                </button>
              ))}
            </div>
          </div>

          {/* Title - Required */}
          <div className="form-group">
            <label>{t.titleLabel} <span style={{ color: '#ea4335' }}>*</span></label>
            <input
              type="text"
              className="input"
              placeholder={t.titlePlaceholder}
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              required
              autoFocus
            />
          </div>

          {/* "Now" Button */}
          <div className="form-group">
            <button type="button" className="btn btn-now" onClick={handleSetNow}>
              <Zap size={16} /> {t.setNow}
            </button>
          </div>

          {/* Start Date & Time */}
          <div className="form-group">
            <label><Clock size={14} /> {t.startTime}</label>
            <div className="datetime-row">
              <input type="date" className="input" value={formData.startDate} onChange={(e) => setFormData({ ...formData, startDate: e.target.value })} required />
              <input type="time" className="input" value={formData.startTime} onChange={(e) => setFormData({ ...formData, startTime: e.target.value })} required />
            </div>
          </div>

          {/* End Date & Time */}
          <div className="form-group">
            <label><Clock size={14} /> {t.endTime}</label>
            <div className="datetime-row">
              <input type="date" className="input" value={formData.endDate} onChange={(e) => setFormData({ ...formData, endDate: e.target.value })} required />
              <input type="time" className="input" value={formData.endTime} onChange={(e) => setFormData({ ...formData, endTime: e.target.value })} required />
            </div>
          </div>

          {/* Location */}
          <div className="form-group">
            <label><MapPin size={14} /> {t.locationLabel}</label>
            <input
              type="text"
              className="input"
              placeholder={t.locationPlaceholder}
              value={formData.location}
              onChange={(e) => setFormData({ ...formData, location: e.target.value })}
            />
          </div>

          {/* Repeat */}
          <div className="form-group">
            <label><Repeat size={14} /> {t.repeatLabel}</label>
            <select className="input" value={formData.repeatType} onChange={(e) => setFormData({ ...formData, repeatType: e.target.value })}>
              {REPEAT_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          {/* Notes */}
          <div className="form-group">
            <label>{t.notesLabel}</label>
            <textarea
              className="input"
              rows={3}
              placeholder={t.notesPlaceholder}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>
        </form>

        <div className="drawer-footer">
          {editingEvent && onDelete && (
            <button type="button" className="btn btn-danger" onClick={() => { onDelete(editingEvent.documentId!); onClose(); }}>
              {t.delete}
            </button>
          )}
          <div style={{ flex: 1 }} />
          <button type="button" className="btn btn-secondary" onClick={onClose}>{t.cancel}</button>
          <button type="submit" className="btn btn-primary" onClick={handleSubmit}>
            <Save size={16} /> {t.save}
          </button>
        </div>
      </div>
    </div>
  );
}
