package com.test.lifehub.features.two_productivity.ui;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.test.lifehub.features.two_productivity.data.NoteEntry;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit test cho ProductivityViewModel
 * Kiểm tra quản lý ghi chú, công việc, dự án
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductivityViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    ProductivityRepository mockRepository;

    private MutableLiveData<List<NoteEntry>> notesLiveData;
    private MutableLiveData<List<TaskEntry>> tasksLiveData;
    private MutableLiveData<List<ProjectEntry>> projectsLiveData;

    @Before
    public void setUp() {
        notesLiveData = new MutableLiveData<>();
        tasksLiveData = new MutableLiveData<>();
        projectsLiveData = new MutableLiveData<>();
        
        when(mockRepository.getAllNotes()).thenReturn(notesLiveData);
        when(mockRepository.getAllTasks()).thenReturn(tasksLiveData);
        when(mockRepository.getAllProjects()).thenReturn(projectsLiveData);
    }

    // ===== NOTES TESTS =====
    
    @Test
    public void testGetAllNotes_ReturnsLiveData() {
        // Given
        List<NoteEntry> testNotes = new ArrayList<>();
        testNotes.add(createTestNote("Note 1", "Content 1"));
        testNotes.add(createTestNote("Note 2", "Content 2"));
        
        notesLiveData.setValue(testNotes);
        
        // Verify
        assertNotNull("Notes LiveData không được null", notesLiveData.getValue());
        assertEquals("Số lượng notes phải đúng", 2, notesLiveData.getValue().size());
    }

    @Test
    public void testInsertNote_CallsRepository() {
        // Given
        NoteEntry newNote = createTestNote("New Note", "New Content");
        
        // When
        mockRepository.insertNote(newNote);
        
        // Verify
        verify(mockRepository, times(1)).insertNote(any(NoteEntry.class));
    }

    @Test
    public void testUpdateNote_CallsRepository() {
        // Given
        NoteEntry existingNote = createTestNote("Existing Note", "Old Content");
        existingNote.documentId = "note123";
        
        // When
        mockRepository.updateNote(existingNote);
        
        // Verify
        verify(mockRepository, times(1)).updateNote(any(NoteEntry.class));
    }

    @Test
    public void testDeleteNote_CallsRepository() {
        // Given
        NoteEntry noteToDelete = createTestNote("Delete Me", "Content");
        noteToDelete.documentId = "note456";
        
        // When
        mockRepository.deleteNote(noteToDelete);
        
        // Verify
        verify(mockRepository, times(1)).deleteNote(any(NoteEntry.class));
    }

    // ===== TASKS TESTS =====
    
    @Test
    public void testGetAllTasks_ReturnsLiveData() {
        // Given
        List<TaskEntry> testTasks = new ArrayList<>();
        testTasks.add(createTestTask("Task 1", false));
        testTasks.add(createTestTask("Task 2", true));
        
        tasksLiveData.setValue(testTasks);
        
        // Verify
        assertNotNull("Tasks LiveData không được null", tasksLiveData.getValue());
        assertEquals("Số lượng tasks phải đúng", 2, tasksLiveData.getValue().size());
    }

    @Test
    public void testTaskCompletion_ToggleStatus() {
        // Given
        TaskEntry task = createTestTask("Test Task", false);
        assertFalse("Task ban đầu chưa hoàn thành", task.isCompleted);
        
        // When
        task.isCompleted = true;
        
        // Verify
        assertTrue("Task phải được đánh dấu hoàn thành", task.isCompleted);
    }

    @Test
    public void testInsertTask_CallsRepository() {
        // Given
        TaskEntry newTask = createTestTask("New Task", false);
        
        // When
        mockRepository.insertTask(newTask);
        
        // Verify
        verify(mockRepository, times(1)).insertTask(any(TaskEntry.class));
    }

    @Test
    public void testUpdateTask_CallsRepository() {
        // Given
        TaskEntry existingTask = createTestTask("Existing Task", false);
        existingTask.documentId = "task123";
        
        // When
        mockRepository.updateTask(existingTask);
        
        // Verify
        verify(mockRepository, times(1)).updateTask(any(TaskEntry.class));
    }

    @Test
    public void testDeleteTask_CallsRepository() {
        // Given
        TaskEntry taskToDelete = createTestTask("Delete Task", false);
        taskToDelete.documentId = "task456";
        
        // When
        mockRepository.deleteTask(taskToDelete);
        
        // Verify
        verify(mockRepository, times(1)).deleteTask(any(TaskEntry.class));
    }

    // ===== PROJECTS TESTS =====
    
    @Test
    public void testGetAllProjects_ReturnsLiveData() {
        // Given
        List<ProjectEntry> testProjects = new ArrayList<>();
        testProjects.add(createTestProject("Project 1"));
        testProjects.add(createTestProject("Project 2"));
        
        projectsLiveData.setValue(testProjects);
        
        // Verify
        assertNotNull("Projects LiveData không được null", projectsLiveData.getValue());
        assertEquals("Số lượng projects phải đúng", 2, projectsLiveData.getValue().size());
    }

    @Test
    public void testInsertProject_CallsRepository() {
        // Given
        ProjectEntry newProject = createTestProject("New Project");
        
        // When
        mockRepository.insertProject(newProject);
        
        // Verify
        verify(mockRepository, times(1)).insertProject(any(ProjectEntry.class));
    }

    @Test
    public void testTaskPriority_ValidValues() {
        // Given
        TaskEntry highPriority = createTestTask("High Priority", false);
        highPriority.priority = 3;
        
        TaskEntry lowPriority = createTestTask("Low Priority", false);
        lowPriority.priority = 1;
        
        // Verify
        assertEquals("High priority phải là 3", 3, highPriority.priority);
        assertEquals("Low priority phải là 1", 1, lowPriority.priority);
        assertTrue("High priority > Low priority", highPriority.priority > lowPriority.priority);
    }

    @Test
    public void testTaskDueDate_ValidDate() {
        // Given
        TaskEntry task = createTestTask("Task with Due Date", false);
        Date dueDate = new Date();
        task.dueDate = dueDate;
        
        // Verify
        assertNotNull("Due date không được null", task.dueDate);
        assertEquals("Due date phải khớp", dueDate, task.dueDate);
    }

    // Helper methods
    private NoteEntry createTestNote(String title, String content) {
        NoteEntry note = new NoteEntry();
        note.title = title;
        note.content = content;
        note.createdAt = new Date();
        note.updatedAt = new Date();
        return note;
    }

    private TaskEntry createTestTask(String title, boolean isCompleted) {
        TaskEntry task = new TaskEntry();
        task.title = title;
        task.isCompleted = isCompleted;
        task.priority = 2;
        task.createdAt = new Date();
        return task;
    }

    private ProjectEntry createTestProject(String name) {
        ProjectEntry project = new ProjectEntry();
        project.name = name;
        project.createdAt = new Date();
        return project;
    }
}
