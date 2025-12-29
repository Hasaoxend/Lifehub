import { useState } from 'react';
import { useTasks, TaskEntry, ProjectEntry } from '../hooks/useTasks';
import { useLanguage } from '../hooks/useLanguage';
import { 
  CheckSquare, 
  Square,
  Plus, 
  Search, 
  Trash2,
  Folder,
  FolderOpen,
  ChevronRight,
  X
} from 'lucide-react';
import './TasksPage.css';

export function TasksPage() {
  const { 
    tasks, 
    projects, 
    loading, 
    error,
    addTask, 
    toggleTaskCompletion, 
    deleteTask,
    addProject,
    deleteProject,
    getTasksByProject
  } = useTasks();
  
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedProjects, setExpandedProjects] = useState<Set<string>>(new Set(['root']));
  const language = useLanguage();

  const t = {
    title: language === 'vi' ? 'Quản lý công việc' : 'Task Manager',
    subtitle: language === 'vi' ? 'Tổ chức và theo dõi công việc của bạn' : 'Organize and track your tasks',
    search: language === 'vi' ? 'Tìm kiếm công việc...' : 'Search tasks...',
    addTask: language === 'vi' ? 'Thêm công việc' : 'Add Task',
    addProject: language === 'vi' ? 'Thêm dự án' : 'Add Project',
    noResults: language === 'vi' ? 'Không tìm thấy công việc' : 'No tasks found',
    noTasks: language === 'vi' ? 'Chưa có công việc nào' : 'No tasks yet',
    loading: language === 'vi' ? 'Đang tải...' : 'Loading...',
    errorLabel: language === 'vi' ? 'Lỗi' : 'Error',
    reload: language === 'vi' ? 'Tải lại trang' : 'Reload page',
    taskName: language === 'vi' ? 'Tên công việc' : 'Task name',
    taskNamePlaceholder: language === 'vi' ? 'Nhập tên công việc...' : 'Enter task name...',
    project: language === 'vi' ? 'Dự án' : 'Project',
    noProject: language === 'vi' ? 'Không có dự án' : 'No project',
    reminder: language === 'vi' ? 'Nhắc nhở' : 'Reminder',
    cancel: language === 'vi' ? 'Hủy' : 'Cancel',
    add: language === 'vi' ? 'Thêm' : 'Add',
    projectName: language === 'vi' ? 'Tên dự án' : 'Project name',
    projectNamePlaceholder: language === 'vi' ? 'Nhập tên dự án...' : 'Enter project name...',
    generalTasks: language === 'vi' ? 'Công việc chung' : 'General Tasks',
    confirmDeleteTask: language === 'vi' ? 'Bạn có chắc muốn xóa công việc này?' : 'Are you sure you want to delete this task?',
    confirmDeleteProject: language === 'vi' ? 'Bạn có chắc muốn xóa dự án này?' : 'Are you sure you want to delete this project?',
    cannotDeleteProject: language === 'vi' ? 'Không thể xóa project có chứa công việc. Vui lòng xóa hoặc di chuyển các công việc trước.' : 'Cannot delete project with tasks. Please delete or move tasks first.',
    completed: language === 'vi' ? 'hoàn thành' : 'completed',
    due: language === 'vi' ? 'Hạn' : 'Due',
    tasks: language === 'vi' ? 'Công việc' : 'Tasks',
    searchPlaceholder: language === 'vi' ? 'Tìm kiếm công việc...' : 'Search tasks...',
    addProjectBtn: language === 'vi' ? 'Thêm Project' : 'Add Project',
    addTaskBtn: language === 'vi' ? 'Thêm công việc' : 'Add Task',
    addTaskModalTitle: language === 'vi' ? 'Thêm công việc mới' : 'Add New Task',
    taskNameRequired: language === 'vi' ? 'Tên công việc *' : 'Task Name *',
    deadlineOptional: language === 'vi' ? 'Hạn hoàn thành (tùy chọn)' : 'Due Date (Optional)',
    projectOptional: language === 'vi' ? 'Project (tùy chọn)' : 'Project (Optional)',
    noProjectOption: language === 'vi' ? '-- Không có project --' : '-- No project --',
    addProjectModalTitle: language === 'vi' ? 'Thêm Project mới' : 'Add New Project',
    projectNameRequired: language === 'vi' ? 'Tên Project *' : 'Project Name *'
  };
  
  // Form state
  const [taskName, setTaskName] = useState('');
  const [taskProjectId, setTaskProjectId] = useState<string | null>(null);
  const [taskReminderTime, setTaskReminderTime] = useState('');
  const [projectName, setProjectName] = useState('');

  const toggleProject = (projectId: string) => {
    const newExpanded = new Set(expandedProjects);
    if (newExpanded.has(projectId)) {
      newExpanded.delete(projectId);
    } else {
      newExpanded.add(projectId);
    }
    setExpandedProjects(newExpanded);
  };

  const handleAddTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskName.trim()) return;
    
    try {
      await addTask({
        name: taskName,
        completed: false,
        taskType: 0,
        projectId: taskProjectId,
        reminderTime: taskReminderTime ? new Date(taskReminderTime) : null
      });
      setTaskName('');
      setTaskProjectId(null);
      setTaskReminderTime('');
      setShowTaskModal(false);
    } catch (error) {
      console.error('Error adding task:', error);
    }
  };

  const handleAddProject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim()) return;
    
    try {
      await addProject(projectName);
      setProjectName('');
      setShowProjectModal(false);
    } catch (error) {
      console.error('Error adding project:', error);
    }
  };

  const handleDeleteProject = async (projectId: string) => {
    const projectTasks = getTasksByProject(projectId);
    if (projectTasks.length > 0) {
      alert(t.cannotDeleteProject);
      return;
    }
    if (confirm(t.confirmDeleteProject)) {
      await deleteProject(projectId);
    }
  };

  const filteredTasks = tasks.filter(task => 
    task.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const rootTasks = filteredTasks.filter(t => !t.projectId);
  
  const getProjectTasks = (projectId: string) => 
    filteredTasks.filter(t => t.projectId === projectId);

  // Stats
  const totalTasks = tasks.length;
  const completedTasks = tasks.filter(t => t.completed).length;

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
    <div className="tasks-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">{t.tasks} ({tasks.length})</h1>
          <p className="page-subtitle">{t.subtitle}</p>
        </div>
        <div className="stats-badge">
          <span className="stats-completed">{completedTasks}</span>
          <span className="stats-divider">/</span>
          <span className="stats-total">{totalTasks}</span>
          <span className="stats-label">{t.completed}</span>
        </div>
      </div>

      {/* Toolbar */}
      <div className="page-toolbar">
        <div className="search-box">
          <Search size={18} />
          <input
            type="text"
            placeholder={t.searchPlaceholder}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="btn btn-secondary" onClick={() => setShowProjectModal(true)}>
          <Folder size={18} />
          {t.addProjectBtn}
        </button>
        <button className="btn btn-primary" onClick={() => setShowTaskModal(true)}>
          <Plus size={18} />
          {t.addTaskBtn}
        </button>
      </div>

      {/* Task List */}
      <div className="task-list">
        {/* Root Level Tasks */}
        <div className="task-section">
          <div 
            className="section-header"
            onClick={() => toggleProject('root')}
          >
            <ChevronRight 
              size={18} 
              className={`chevron ${expandedProjects.has('root') ? 'expanded' : ''}`}
            />
            <FolderOpen size={18} />
            <span className="section-title">{t.generalTasks}</span>
            <span className="section-count">{rootTasks.length}</span>
          </div>
          
          {expandedProjects.has('root') && (
            <div className="section-content">
              {rootTasks.length === 0 ? (
                <p className="empty-section">{t.noTasks}</p>
              ) : (
                rootTasks.map(task => (
                  <TaskItem 
                    key={task.documentId}
                    task={task}
                    onToggle={() => toggleTaskCompletion(task.documentId!, task.completed)}
                    onDelete={() => deleteTask(task.documentId!)}
                    formatDate={formatDate}
                  />
                ))
              )}
            </div>
          )}
        </div>

        {/* Project Sections */}
        {projects.map(project => {
          const projectTasks = getProjectTasks(project.documentId!);
          const isExpanded = expandedProjects.has(project.documentId!);
          
          return (
            <div key={project.documentId} className="task-section">
              <div 
                className="section-header"
                onClick={() => toggleProject(project.documentId!)}
              >
                <ChevronRight 
                  size={18} 
                  className={`chevron ${isExpanded ? 'expanded' : ''}`}
                />
                <Folder size={18} />
                <span className="section-title">{project.name}</span>
                <span className="section-count">{projectTasks.length}</span>
                <button 
                  className="section-delete"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteProject(project.documentId!);
                  }}
                >
                  <Trash2 size={14} />
                </button>
              </div>
              
              {isExpanded && (
                <div className="section-content">
                  {projectTasks.length === 0 ? (
                    <p className="empty-section">{t.noTasks}</p>
                  ) : (
                    projectTasks.map(task => (
                      <TaskItem 
                        key={task.documentId}
                        task={task}
                        onToggle={() => toggleTaskCompletion(task.documentId!, task.completed)}
                        onDelete={() => deleteTask(task.documentId!)}
                        formatDate={formatDate}
                      />
                    ))
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Add Task Modal */}
      {showTaskModal && (
        <div className="modal-overlay" onClick={() => setShowTaskModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">{t.addTaskModalTitle}</h2>
              <button className="modal-close" onClick={() => setShowTaskModal(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleAddTask} className="modal-form">
              <div className="input-group">
                <label className="input-label">{t.taskNameRequired}</label>
                <input
                  type="text"
                  className="input"
                  placeholder={t.taskNamePlaceholder}
                  value={taskName}
                  onChange={(e) => setTaskName(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              <div className="input-group">
                <label className="input-label">{t.deadlineOptional}</label>
                <input
                  type="datetime-local"
                  className="input"
                  value={taskReminderTime}
                  onChange={(e) => setTaskReminderTime(e.target.value)}
                />
              </div>

              <div className="input-group">
                <label className="input-label">{t.projectOptional}</label>
                <select
                  className="input"
                  value={taskProjectId || ''}
                  onChange={(e) => setTaskProjectId(e.target.value || null)}
                >
                  <option value="">{t.noProjectOption}</option>
                  {projects.map(p => (
                    <option key={p.documentId} value={p.documentId}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowTaskModal(false)}>
                  {t.cancel}
                </button>
                <button type="submit" className="btn btn-primary">
                  {t.add}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Project Modal */}
      {showProjectModal && (
        <div className="modal-overlay" onClick={() => setShowProjectModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">{t.addProjectModalTitle}</h2>
              <button className="modal-close" onClick={() => setShowProjectModal(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleAddProject} className="modal-form">
              <div className="input-group">
                <label className="input-label">{t.projectNameRequired}</label>
                <input
                  type="text"
                  className="input"
                  placeholder={t.projectNamePlaceholder}
                  value={projectName}
                  onChange={(e) => setProjectName(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowProjectModal(false)}>
                  {t.cancel}
                </button>
                <button type="submit" className="btn btn-primary">
                  {t.add}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// Task Item Component
function TaskItem({ 
  task, 
  onToggle, 
  onDelete,
  formatDate
}: { 
  task: TaskEntry; 
  onToggle: () => void; 
  onDelete: () => void;
  formatDate: (date?: Date) => string;
}) {
  return (
    <div className={`task-item ${task.completed ? 'completed' : ''}`}>
      <button className="task-checkbox" onClick={onToggle}>
        {task.completed ? <CheckSquare size={20} /> : <Square size={20} />}
      </button>
      <div className="task-content-wrapper">
        <span className="task-name">{task.name}</span>
        {task.reminderTime && (
          <span className="task-deadline">Hạn: {formatDate(task.reminderTime)}</span>
        )}
      </div>
      <button className="task-delete" onClick={onDelete}>
        <Trash2 size={16} />
      </button>
    </div>
  );
}
