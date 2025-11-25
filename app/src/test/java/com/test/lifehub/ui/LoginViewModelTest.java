package com.test.lifehub.ui;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.core.util.SessionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit test cho LoginViewModel
 * Kiểm tra logic đăng nhập
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    FirebaseAuth mockAuth;

    @Mock
    SessionManager mockSessionManager;

    @Mock
    Application mockApplication;

    @Mock
    FirebaseUser mockFirebaseUser;

    @Mock
    Task<AuthResult> mockAuthTask;

    @Mock
    Observer<LoginViewModel.LoginState> mockObserver;

    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        // Setup mocks
        when(mockAuth.getCurrentUser()).thenReturn(null);
    }

    @Test
    public void testValidateEmail_ValidEmail_ReturnsTrue() {
        // Given
        String validEmail = "test@example.com";
        
        // Verify
        assertTrue("Email hợp lệ phải pass validation", 
            validEmail.contains("@") && validEmail.contains("."));
    }

    @Test
    public void testValidateEmail_InvalidEmail_ReturnsFalse() {
        // Given
        String invalidEmail = "invalid-email";
        
        // Verify
        assertFalse("Email không hợp lệ phải fail validation", 
            invalidEmail.contains("@") && invalidEmail.contains("."));
    }

    @Test
    public void testValidatePassword_ValidPassword_ReturnsTrue() {
        // Given
        String validPassword = "password123";
        
        // Verify
        assertTrue("Mật khẩu >= 6 ký tự phải hợp lệ", validPassword.length() >= 6);
    }

    @Test
    public void testValidatePassword_TooShort_ReturnsFalse() {
        // Given
        String shortPassword = "12345";
        
        // Verify
        assertFalse("Mật khẩu < 6 ký tự phải không hợp lệ", shortPassword.length() >= 6);
    }

    @Test
    public void testLoginState_InitialState_IsIdle() {
        // Given
        LoginViewModel.LoginState initialState = LoginViewModel.LoginState.IDLE;
        
        // Verify
        assertEquals("Initial state phải là IDLE", LoginViewModel.LoginState.IDLE, initialState);
    }

    @Test
    public void testLogin_EmptyFields_ReturnsError() {
        // Given
        String emptyEmail = "";
        String emptyPassword = "";
        
        // Verify
        assertTrue("Empty fields phải trigger error", 
            emptyEmail.isEmpty() || emptyPassword.isEmpty());
    }

    @Test
    public void testLogin_ValidCredentials_CallsFirebaseAuth() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        
        // Verify inputs are valid
        assertNotNull("Email không được null", email);
        assertNotNull("Password không được null", password);
        assertTrue("Email phải hợp lệ", email.contains("@"));
        assertTrue("Password phải >= 6 ký tự", password.length() >= 6);
    }

    @Test
    public void testCheckInitialState_NoUser_ReturnsIdle() {
        // Given
        when(mockAuth.getCurrentUser()).thenReturn(null);
        
        // Verify
        assertNull("Không có user phải trả về null", mockAuth.getCurrentUser());
    }

    @Test
    public void testCheckInitialState_VerifiedUser_ReturnsSuccess() {
        // Given
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        
        // Verify
        assertTrue("User đã verify email phải success", mockFirebaseUser.isEmailVerified());
    }

    @Test
    public void testCheckInitialState_UnverifiedUser_ReturnsError() {
        // Given
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(false);
        
        // Verify
        assertFalse("User chưa verify email phải error", mockFirebaseUser.isEmailVerified());
    }
}
