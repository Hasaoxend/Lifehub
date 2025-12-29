import { useState } from 'react';
import { useNotes, NoteEntry } from '../hooks/useNotes';
import { useLanguage } from '../hooks/useLanguage';
import { 
  FileText, 
  Plus, 
  Search, 
  Trash2, 
  Edit3,
  X,
  Clock
} from 'lucide-react';
import './NotesPage.css';

export function NotesPage() {
  const { notes, loading, error, addNote, updateNote, deleteNote } = useNotes();
  const language = useLanguage();
  const [showModal, setShowModal] = useState(false);
  const [editingNote, setEditingNote] = useState<NoteEntry | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [viewingNote, setViewingNote] = useState<NoteEntry | null>(null);

  const t = {
    title: language === 'vi' ? 'Ghi chú' : 'Notes',
    subtitle: language === 'vi' ? 'Quản lý và tổ chức các ghi chú của bạn' : 'Manage and organize your notes',
    search: language === 'vi' ? 'Tìm kiếm ghi chú...' : 'Search notes...',
    addNote: language === 'vi' ? 'Thêm ghi chú' : 'Add Note',
    noResults: language === 'vi' ? 'Không tìm thấy ghi chú' : 'No notes found',
    noNotes: language === 'vi' ? 'Chưa có ghi chú nào' : 'No notes yet',
    tryOther: language === 'vi' ? 'Thử từ khóa khác' : 'Try a different search',
    startAdd: language === 'vi' ? 'Tạo ghi chú đầu tiên của bạn' : 'Create your first note',
    loading: language === 'vi' ? 'Đang tải...' : 'Loading...',
    errorLabel: language === 'vi' ? 'Lỗi' : 'Error',
    reload: language === 'vi' ? 'Tải lại trang' : 'Reload page',
    editNote: language === 'vi' ? 'Chỉnh sửa ghi chú' : 'Edit Note',
    newNote: language === 'vi' ? 'Thêm ghi chú mới' : 'Add New Note',
    noteTitle: language === 'vi' ? 'Tiêu đề' : 'Title',
    noteTitlePlaceholder: language === 'vi' ? 'Nhập tiêu đề...' : 'Enter title...',
    content: language === 'vi' ? 'Nội dung' : 'Content',
    contentPlaceholder: language === 'vi' ? 'Nhập nội dung...' : 'Enter content...',
    reminder: language === 'vi' ? 'Thời gian kết thúc / Nhắc nhở' : 'Due Date / Reminder',
    cancel: language === 'vi' ? 'Hủy' : 'Cancel',
    save: language === 'vi' ? 'Lưu' : 'Save',
    update: language === 'vi' ? 'Cập nhật' : 'Update',
    add: language === 'vi' ? 'Thêm' : 'Add',
    confirmDelete: language === 'vi' ? 'Bạn có chắc muốn xóa ghi chú này?' : 'Are you sure you want to delete this note?',
    noTitle: language === 'vi' ? 'Không có tiêu đề' : 'No title',
    noContent: language === 'vi' ? 'Không có nội dung' : 'No content',
    edited: language === 'vi' ? 'Sửa' : 'Edited',
    due: language === 'vi' ? 'Hạn' : 'Due'
  };
  
  // Form state
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    reminderTime: ''
  });

  const handleOpenModal = (note?: NoteEntry) => {
    if (note) {
      setEditingNote(note);
      setFormData({
        title: note.title,
        content: note.content,
        reminderTime: note.reminderTime 
          ? new Date(note.reminderTime.getTime() - note.reminderTime.getTimezoneOffset() * 60000).toISOString().slice(0, 16) 
          : ''
      });
    } else {
      setEditingNote(null);
      setFormData({ title: '', content: '', reminderTime: '' });
    }
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingNote(null);
    setFormData({ title: '', content: '', reminderTime: '' });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title.trim()) {
      alert(language === 'vi' ? 'Vui lòng nhập tiêu đề' : 'Please enter a title');
      return;
    }

    try {
      const dataToSave = {
        ...formData,
        reminderTime: formData.reminderTime ? new Date(formData.reminderTime) : null
      };

      if (editingNote?.documentId) {
        await updateNote(editingNote.documentId, dataToSave);
      } else {
        await addNote(dataToSave);
      }
      handleCloseModal();
    } catch (error) {
      console.error('Error saving note:', error);
    }
  };

  const handleDelete = async (documentId: string) => {
    if (confirm(t.confirmDelete)) {
      await deleteNote(documentId);
    }
  };

  const filteredNotes = notes.filter(note => {
    const query = searchQuery.toLowerCase();
    return note.title.toLowerCase().includes(query) || 
           note.content.toLowerCase().includes(query);
  });

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

  if (loading) {
    return (
      <div className="page-loading">
        <div className="loader"></div>
        <p>{t.loading}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-error">
        <p>{t.errorLabel}: {error}</p>
        <button className="btn btn-primary" onClick={() => window.location.reload()}>{t.reload}</button>
      </div>
    );
  }

  return (
    <div className="notes-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">{t.title} ({notes.length})</h1>
          <p className="page-subtitle">{t.subtitle}</p>
        </div>
      </div>

      {/* Search & Actions */}
      <div className="page-toolbar">
        <div className="search-box">
          <Search size={18} />
          <input
            type="text"
            placeholder={t.search}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="btn btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={18} />
          {t.addNote}
        </button>
      </div>

      {/* Notes Grid */}
      {filteredNotes.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">
            <FileText size={40} />
          </div>
          <h3 className="empty-title">
            {searchQuery ? t.noResults : t.noNotes}
          </h3>
          <p className="empty-description">
            {searchQuery ? t.tryOther : t.startAdd}
          </p>
          {!searchQuery && (
            <button className="btn btn-primary" onClick={() => handleOpenModal()}>
              <Plus size={18} />
              {t.addNote}
            </button>
          )}
        </div>
      ) : (
        <div className="notes-grid">
          {filteredNotes.map((note) => (
            <div 
              key={note.documentId} 
              className="note-card clickable"
              onClick={() => setViewingNote(note)}
            >
              <div className="note-header">
                <h3 className="note-title">{note.title || 'Không có tiêu đề'}</h3>
                <div className="note-actions" onClick={(e) => e.stopPropagation()}>
                  <button 
                    className="action-btn"
                    onClick={() => handleOpenModal(note)}
                    title="Chỉnh sửa"
                  >
                    <Edit3 size={16} />
                  </button>
                  <button 
                    className="action-btn danger"
                    onClick={() => handleDelete(note.documentId!)}
                    title="Xóa"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
              
              <p className="note-content">
                {note.content || 'Không có nội dung'}
              </p>
              
              <div className="note-footer">
                <div className="note-time">
                  <Clock size={12} />
                  <span>Sửa: {formatDate(note.lastModified)}</span>
                </div>

                {note.reminderTime && (
                  <div className="note-reminder">
                    <Clock size={12} className="text-primary" />
                    <span className="text-primary">Hạn: {formatDate(note.reminderTime)}</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {editingNote ? t.editNote : t.newNote}
              </h2>
              <button className="modal-close" onClick={handleCloseModal}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="modal-form">
              <div className="input-group">
                <label className="input-label">{t.noteTitle}</label>
                <input
                  type="text"
                  className="input"
                  placeholder={t.noteTitlePlaceholder}
                  value={formData.title}
                  onChange={(e) => setFormData({...formData, title: e.target.value})}
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">{t.reminder}</label>
                <input
                  type="datetime-local"
                  className="input"
                  value={formData.reminderTime}
                  onChange={(e) => setFormData({...formData, reminderTime: e.target.value})}
                />
              </div>

              <div className="input-group">
                <label className="input-label">{t.content}</label>
                <textarea
                  className="input textarea"
                  placeholder={t.contentPlaceholder}
                  rows={8}
                  value={formData.content}
                  onChange={(e) => setFormData({...formData, content: e.target.value})}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={handleCloseModal}>
                  {t.cancel}
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingNote ? t.update : t.add}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View Detail Modal */}
      {viewingNote && (
        <div className="modal-overlay" onClick={() => setViewingNote(null)}>
          <div className="modal detail-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {viewingNote.title || t.noTitle}
              </h2>
              <button className="modal-close" onClick={() => setViewingNote(null)}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body">
              <div className="detail-group">
                <label className="detail-label">{t.content}</label>
                <div className="detail-content-text">
                  {viewingNote.content || t.noContent}
                </div>
              </div>

              <div className="detail-meta-group">
                <div className="detail-meta-item">
                  <Clock size={14} />
                  <span>{t.edited}: {formatDate(viewingNote.lastModified)}</span>
                </div>
                {viewingNote.reminderTime && (
                  <div className="detail-meta-item important">
                    <Clock size={14} />
                    <span>{t.due}: {formatDate(viewingNote.reminderTime)}</span>
                  </div>
                )}
              </div>
            </div>

            <div className="modal-actions">
              <button 
                className="btn btn-secondary" 
                onClick={() => {
                  navigator.clipboard.writeText(viewingNote.content);
                  // Optional: add a tiny toast here if available globally
                }}
              >
                {language === 'vi' ? 'Sao chép nội dung' : 'Copy Content'}
              </button>
              <button className="btn btn-primary" onClick={() => setViewingNote(null)}>
                {language === 'vi' ? 'Đóng' : 'Close'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
