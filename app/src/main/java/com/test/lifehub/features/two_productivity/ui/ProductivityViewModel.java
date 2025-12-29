package com.test.lifehub.features.two_productivity.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.features.two_productivity.data.NoteEntry;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ProductivityViewModel - ViewModel cho ProductivityFragment
 * 
 * === MỤC ĐÍCH ===
 * Quản lý UI state và business logic cho màn hình Productivity.
 * Expose LiveData cho Fragment để hiển thị 3 loại dữ liệu:
 * 1. Notes (Ghi chú)
 * 2. Tasks (Công việc) - Có thể nhóm theo Projects
 * 3. Shopping Items (Danh sách mua sắm)
 *   
 * === KIẾN TRÚC MVVM ===
 * ```
 * ProductivityFragment (View Layer)
 *        |
 *        | observe()
 *        v
 * ProductivityViewModel (ViewModel Layer) <- ĐÂY
 *        |
 *        | delegate to
 *        v
 * ProductivityRepository (Data Layer)
 *        |
 *        | Firestore Snapshot Listener
 *        v
 * Firestore Database
 * ```
 * 
 * === TÍNH NĂNG NỔI BẬT ===
 * 
 * 1. HIERARCHICAL FILTERING (Lọc theo cấp bậc):
 *    - Root Level: Tasks/Projects không thuộc project nào (projectId=null)
 *    - Project Detail: Tasks/Projects thuộc project đang xem (projectId=mCurrentProjectId)
 * 
 *    Cấu trúc:
 *    ```
 *    Root
 *    ├─ Task 1 (projectId=null)
 *    ├─ Task 2 (projectId=null)
 *    ├─ Project A (projectId=null)
 *    │  ├─ Task 3 (projectId="projectA_id")
 *    │  └─ SubProject B (projectId="projectA_id")
 *    └─ Project C (projectId=null)
 *    ```
 * 
 * 2. MEDIATOR LIVEDATA PATTERN:
 *    - tasksInRoot: Tự động lọc tasks ở root khi allTasks thay đổi
 *    - projectsInRoot: Tự động lọc projects ở root khi allProjects thay đổi
 *    - tasksInProject: Lọc tasks thuộc mCurrentProjectId
 *    - projectsInProject: Lọc subprojects thuộc mCurrentProjectId
 * 
 *    Lý do dùng MediatorLiveData:
 *    - Kết hợp nhiều LiveData sources (allTasks + allProjects)
 *    - Tự động cập nhật khi bất kỳ source nào thay đổi
 *    - Tránh phải refresh thủ công
 * 
 * 3. STREAM API FILTERING:
 *    ```java
 *    tasks.stream()
 *         .filter(t -> t.getProjectId() == null)  // Chỉ lấy root tasks
 *         .collect(Collectors.toList())
 *    ```
 * 
 * === LIVEDATA HIERARCHY ===
 * 
 * Repository Layer (Source):
 * - allNotes: Tất cả notes
 * - allTasks: Tất cả tasks (task + shopping items)
 * - allShoppingItems: Chỉ shopping items (taskType=1)
 * - allProjects: Tất cả projects
 * 
 * ViewModel Layer (Transformed):
 * - tasksInRoot: Tasks ở root level (projectId=null)
 * - projectsInRoot: Projects ở root level (projectId=null)
 * - tasksInProject: Tasks thuộc mCurrentProjectId
 * - projectsInProject: Subprojects thuộc mCurrentProjectId
 * 
 * Fragment Layer (Observe):
 * ```java
 * viewModel.getTasksInRoot().observe(this, tasks -> {
 *     adapter.submitList(tasks);
 * });
 * ```
 * 
 * === NAVIGATION FLOW ===
 * 1. User mở ProductivityFragment -> Hiển thị root level
 * 2. User click vào Project A -> setCurrentProject("projectA_id")
 * 3. ViewModel lọc tasksInProject và projectsInProject
 * 4. Fragment hiển thị nội dung Project A
 * 
 * === SCOPE ===
 * @HiltViewModel:
 * - Tự động inject ProductivityRepository
 * - Lifecycle gắn với Fragment (destroy khi Fragment destroy)
 * - Mỗi Fragment có 1 ViewModel instance riêng
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * ```java
 * // Fragment
 * @AndroidEntryPoint
 * public class ProductivityFragment extends Fragment {
 *     private ProductivityViewModel viewModel;
 * 
 *     @Override
 *     public void onViewCreated(View view, Bundle savedInstanceState) {
 *         // 1. Inject ViewModel
 *         viewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);
 * 
 *         // 2. Observe root level data
 *         viewModel.getTasksInRoot().observe(getViewLifecycleOwner(), tasks -> {
 *             taskAdapter.submitList(tasks);
 *         });
 * 
 *         // 3. Navigate vào project
 *         projectAdapter.setOnItemClickListener(project -> {
 *             viewModel.setCurrentProject(project.documentId);
 *             // Fragment tự động cập nhật nhờ observe tasksInProject
 *         });
 * 
 *         // 4. Insert task mới
 *         viewModel.insertTask(newTask);
 *     }
 * }
 * ```
 * 
 * === LƯU Ý QUAN TRỌNG ===
 * 1. ViewModel KHÔNG GIỮNG REFERENCE ĐẾN VIEW (Fragment/Activity)
 * 2. Chỉ expose LiveData, không expose MutableLiveData ra ngoài
 * 3. Business logic nặng nề để ở Repository, ViewModel chỉ transform data
 * 4. Dùng Objects.equals() thay vì == để so sánh String (tránh NullPointerException)
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm search/filter tasks theo keyword
 * TODO: Hỗ trợ sort tasks (theo ngày, tên, priority)
 * TODO: Thêm batch operations (delete nhiều tasks)
 * TODO: Hỗ trợ drag-and-drop để sắp xếp tasks
 * FIXME: Hiệu suất khi danh sách tasks quá dài (1000+ items)
 * 
 * @see ProductivityRepository Data source
 * @see ProductivityFragment UI layer
 * @see NoteEntry, TaskEntry, ProjectEntry Data models
 */
@HiltViewModel
public class ProductivityViewModel extends ViewModel {

    private final ProductivityRepository mRepository;

    private final LiveData<List<NoteEntry>> allNotes;
    private final LiveData<List<TaskEntry>> allTasks;
    private final LiveData<List<TaskEntry>> allShoppingItems;
    private final LiveData<List<ProjectEntry>> allProjects;

    private final MediatorLiveData<List<TaskEntry>> tasksInRoot = new MediatorLiveData<>();
    private final MediatorLiveData<List<ProjectEntry>> projectsInRoot = new MediatorLiveData<>();

    private final MediatorLiveData<List<TaskEntry>> tasksInProject = new MediatorLiveData<>();
    private final MediatorLiveData<List<ProjectEntry>> projectsInProject = new MediatorLiveData<>();

    private String mCurrentProjectId = null;

    @Inject
    public ProductivityViewModel(ProductivityRepository repository) {
        this.mRepository = repository;

        allNotes = mRepository.getAllNotes();
        allTasks = mRepository.getAllTasks();
        allShoppingItems = mRepository.getAllShoppingItems();
        allProjects = mRepository.getAllProjects();

        // --- Logic lọc ---
        tasksInRoot.addSource(allTasks, tasks -> filterRootLists(tasks, allProjects.getValue()));
        projectsInRoot.addSource(allProjects, projects -> filterRootLists(allTasks.getValue(), projects));

        tasksInProject.addSource(allTasks, tasks -> filterProjectDetailLists(tasks, allProjects.getValue()));
        projectsInProject.addSource(allProjects, projects -> filterProjectDetailLists(allTasks.getValue(), projects));
    }

    // --- Logic lọc (Filtering Logic) ---

    private void filterRootLists(List<TaskEntry> tasks, List<ProjectEntry> projects) {
        if (tasks != null) {
            tasksInRoot.setValue(tasks.stream()
                    .filter(t -> t.getProjectId() == null) // Chỉ lấy task ở root
                    .collect(Collectors.toList()));
        }
        if (projects != null) {
            projectsInRoot.setValue(projects.stream()
                    .filter(p -> p.getProjectId() == null) // Chỉ lấy project ở root
                    .collect(Collectors.toList()));
        }
    }

    private void filterProjectDetailLists(List<TaskEntry> tasks, List<ProjectEntry> projects) {
        if (mCurrentProjectId == null) {
            tasksInProject.setValue(null);
            projectsInProject.setValue(null);
            return;
        }

        if (tasks != null) {
            tasksInProject.setValue(tasks.stream()
                    .filter(t -> Objects.equals(t.getProjectId(), mCurrentProjectId)) // Dùng Objects.equals
                    .collect(Collectors.toList()));
        }
        if (projects != null) {
            projectsInProject.setValue(projects.stream()
                    .filter(p -> Objects.equals(p.getProjectId(), mCurrentProjectId)) // Dùng Objects.equals
                    .collect(Collectors.toList()));
        }
    }

    public void setCurrentProjectId(String projectId) {
        mCurrentProjectId = projectId;
        // Kích hoạt re-filter
        filterRootLists(allTasks.getValue(), allProjects.getValue());
        filterProjectDetailLists(allTasks.getValue(), allProjects.getValue());
    }

    // --- Getters cho UI ---

    public LiveData<List<TaskEntry>> getTasksInRoot() { return tasksInRoot; }
    public LiveData<List<ProjectEntry>> getProjectsInRoot() { return projectsInRoot; }

    public LiveData<List<TaskEntry>> getTasksInProject() { return tasksInProject; }
    public LiveData<List<ProjectEntry>> getProjectsInProject() { return projectsInProject; }

    public LiveData<List<TaskEntry>> getAllShoppingItems() { return allShoppingItems; }

    public LiveData<List<NoteEntry>> getAllNotes() { return allNotes; }
    public LiveData<NoteEntry> getNoteById(String documentId) { return mRepository.getNoteById(documentId); }

    public LiveData<List<TaskEntry>> getAllTasks() {
        return allTasks;
    }

    // ----- CRUD (Create, Read, Update, Delete) -----

    public void insertNote(NoteEntry note) { mRepository.insertNote(note); }
    public void updateNote(NoteEntry note) { mRepository.updateNote(note); }
    public void deleteNote(NoteEntry note) { mRepository.deleteNote(note); }

    public void insertTask(TaskEntry task) { mRepository.insertTask(task); }
    public void updateTask(TaskEntry task) { mRepository.updateTask(task); }
    public void deleteTask(TaskEntry task) { mRepository.deleteTask(task); }

    public void insertProject(String name, String parentProjectId) {
        mRepository.insertProject(name, parentProjectId);
    }
    public void updateProjectName(String projectId, String newName) {
        mRepository.updateProjectName(projectId, newName);
    }
    public void deleteProject(ProjectEntry project) {
        mRepository.deleteProject(project);
    }
}