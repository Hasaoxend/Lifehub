package com.test.lifehub.features.two_productivity.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.features.two_productivity.data.NoteEntry;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;

import java.util.List;
import java.util.Objects; // Thêm import
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

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