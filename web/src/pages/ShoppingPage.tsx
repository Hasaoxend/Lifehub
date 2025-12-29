import { useState } from 'react';
import { useShopping } from '../hooks/useShopping';
import { useLanguage } from '../hooks/useLanguage';
import { 
  ShoppingCart, 
  Plus, 
  Trash2,
  Check,
  Circle,
  Trash
} from 'lucide-react';
import './ShoppingPage.css';

export function ShoppingPage() {
  const { 
    items, 
    loading, 
    error,
    addItem, 
    toggleItemCompletion, 
    deleteItem, 
    clearCompleted,
    totalItems,
    completedCount,
    pendingCount
  } = useShopping();
  
  const [newItemName, setNewItemName] = useState('');
  const language = useLanguage();

  const t = {
    loading: language === 'vi' ? 'Đang tải...' : 'Loading...',
    error: language === 'vi' ? 'Lỗi: ' : 'Error: ',
    reload: language === 'vi' ? 'Tải lại trang' : 'Reload Page',
    title: language === 'vi' ? 'Danh sách mua sắm' : 'Shopping List',
    subtitle: language === 'vi' ? 'Quản lý những gì bạn cần mua' : 'Manage what you need to buy',
    pending: language === 'vi' ? 'cần mua' : 'pending',
    completed: language === 'vi' ? 'đã mua' : 'bought',
    placeholder: language === 'vi' ? 'Thêm món cần mua...' : 'Add item to buy...',
    add: language === 'vi' ? 'Thêm' : 'Add',
    emptyTitle: language === 'vi' ? 'Danh sách trống' : 'Empty List',
    emptyDesc: language === 'vi' ? 'Thêm những gì bạn cần mua vào danh sách' : 'Add items you need to buy to the list',
    bought: language === 'vi' ? 'Đã mua' : 'Bought',
    clearAll: language === 'vi' ? 'Xóa tất cả' : 'Clear All'
  };

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newItemName.trim()) return;
    
    try {
      await addItem(newItemName.trim());
      setNewItemName('');
    } catch (error) {
      console.error('Error adding item:', error);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleAddItem(e);
    }
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
        <p>{t.error}{error}</p>
        <button className="btn btn-primary" onClick={() => window.location.reload()}>{t.reload}</button>
      </div>
    );
  }

  return (
    <div className="shopping-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">{t.title} ({items.length})</h1>
          <p className="page-subtitle">
            {t.subtitle}
          </p>
        </div>
        {totalItems > 0 && (
          <div className="shopping-stats">
            <div className="stat-item">
              <span className="stat-value pending">{pendingCount}</span>
              <span className="stat-label">{t.pending}</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value completed">{completedCount}</span>
              <span className="stat-label">{t.completed}</span>
            </div>
          </div>
        )}
      </div>

      {/* Quick Add Form */}
      <form className="quick-add-form" onSubmit={handleAddItem}>
        <div className="quick-add-input-wrapper">
          <ShoppingCart size={20} className="input-icon" />
          <input
            type="text"
            className="quick-add-input"
            placeholder={t.placeholder}
            value={newItemName}
            onChange={(e) => setNewItemName(e.target.value)}
            onKeyPress={handleKeyPress}
          />
        </div>
        <button type="submit" className="btn btn-primary">
          <Plus size={18} />
          {t.add}
        </button>
      </form>

      {/* Shopping List */}
      {items.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">
            <ShoppingCart size={40} />
          </div>
          <h3 className="empty-title">{t.emptyTitle}</h3>
          <p className="empty-description">
            {t.emptyDesc}
          </p>
        </div>
      ) : (
        <>
          <div className="shopping-list">
            {/* Pending items */}
            {items.filter(i => !i.completed).map(item => (
              <div key={item.documentId} className="shopping-item">
                <button 
                  className="item-checkbox"
                  onClick={() => toggleItemCompletion(item.documentId!, item.completed)}
                >
                  <Circle size={22} />
                </button>
                <span className="item-name">{item.name}</span>
                <button 
                  className="item-delete"
                  onClick={() => deleteItem(item.documentId!)}
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
            
            {/* Completed items */}
            {completedCount > 0 && (
              <>
                <div className="completed-divider">
                  <span>{t.bought} ({completedCount})</span>
                  <button 
                    className="clear-completed-btn"
                    onClick={clearCompleted}
                  >
                    <Trash size={14} />
                    {t.clearAll}
                  </button>
                </div>
                
                {items.filter(i => i.completed).map(item => (
                  <div key={item.documentId} className="shopping-item completed">
                    <button 
                      className="item-checkbox checked"
                      onClick={() => toggleItemCompletion(item.documentId!, item.completed)}
                    >
                      <Check size={18} />
                    </button>
                    <span className="item-name">{item.name}</span>
                    <button 
                      className="item-delete"
                      onClick={() => deleteItem(item.documentId!)}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
              </>
            )}
          </div>
        </>
      )}
    </div>
  );
}
