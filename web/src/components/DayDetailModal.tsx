import React from 'react';
import { 
  X, 
  Plus, 
  MapPin, 
  Clock, 
  Edit3, 
  Trash2,
  Calendar as CalendarIcon
} from 'lucide-react';
import { CalendarEvent } from '../hooks/useCalendar';
import { getLunarDateString } from '../utils/lunarUtils';
import { getVietnameseHoliday } from '../utils/holidayUtils';

interface DayDetailModalProps {
  date: Date;
  events: CalendarEvent[];
  onClose: () => void;
  onAddEvent: (date: Date) => void;
  onEditEvent: (event: CalendarEvent) => void;
  onDeleteEvent: (documentId: string) => void;
}

export function DayDetailModal({ 
  date, 
  events, 
  onClose, 
  onAddEvent, 
  onEditEvent, 
  onDeleteEvent 
}: DayDetailModalProps) {
  const holiday = getVietnameseHoliday(date);
  const lunarStr = getLunarDateString(date);

  const formatTime = (d: Date) => {
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal day-detail-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="day-detail-header-info">
            <h2 className="modal-title">
              {date.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })}
            </h2>
            <div className="day-detail-meta">
              <span className="lunar-info" title="Lịch âm">
                🌙 {lunarStr}
              </span>
              {holiday && (
                <span className="holiday-info" title="Ngày lễ">
                  🎉 {holiday}
                </span>
              )}
            </div>
          </div>
          <button className="modal-close" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          <div className="day-events-section">
            <div className="section-header">
              <h3 className="section-subtitle">
                <CalendarIcon size={16} />
                Sự kiện ({events.length})
              </h3>
              <button 
                className="btn btn-primary btn-sm add-inline-btn"
                onClick={() => onAddEvent(date)}
              >
                <Plus size={14} />
                Thêm
              </button>
            </div>

            {events.length === 0 ? (
              <div className="empty-events">
                <p>Không có sự kiện nào trong ngày này.</p>
              </div>
            ) : (
              <div className="events-list-vertical">
                {events.map(event => (
                  <div 
                    key={event.documentId} 
                    className="event-detail-card"
                    style={{ borderLeftColor: event.color }}
                  >
                    <div className="event-detail-main">
                      <div className="event-detail-time">
                        <Clock size={12} />
                        <span>{formatTime(event.startTime)} - {formatTime(event.endTime)}</span>
                      </div>
                      <h4 className="event-detail-title">{event.title}</h4>
                      {event.location && (
                        <div className="event-detail-location">
                          <MapPin size={12} />
                          <span>{event.location}</span>
                        </div>
                      )}
                      {event.description && (
                        <p className="event-detail-desc">{event.description}</p>
                      )}
                    </div>
                    <div className="event-detail-actions">
                      <button 
                        className="icon-btn" 
                        title="Chỉnh sửa"
                        onClick={() => onEditEvent(event)}
                      >
                        <Edit3 size={16} />
                      </button>
                      <button 
                        className="icon-btn text-danger" 
                        title="Xóa"
                        onClick={() => onDeleteEvent(event.documentId!)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary w-full" onClick={onClose}>
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
