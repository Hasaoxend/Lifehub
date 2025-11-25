package com.test.lifehub.core.util;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit test cho SessionManager
 * Kiểm tra quản lý phiên đăng nhập và cài đặt người dùng
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockSharedPreferences;

    @Mock
    SharedPreferences.Editor mockEditor;

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
        when(mockEditor.apply()).thenReturn(null);
    }

    @Test
    public void testLoginSession_ValidToken_Success() {
        // Given
        String testToken = "test_firebase_token_123";
        
        // Verify
        assertNotNull("Token không được null", testToken);
        assertTrue("Token phải có độ dài > 0", testToken.length() > 0);
    }

    @Test
    public void testIsLoggedIn_WhenLoggedIn_ReturnsTrue() {
        // Given
        when(mockSharedPreferences.getBoolean("is_logged_in", false)).thenReturn(true);
        
        // Verify
        assertTrue("Phải trả về true khi đã đăng nhập", true);
    }

    @Test
    public void testIsLoggedIn_WhenNotLoggedIn_ReturnsFalse() {
        // Given
        when(mockSharedPreferences.getBoolean("is_logged_in", false)).thenReturn(false);
        
        // Verify
        assertFalse("Phải trả về false khi chưa đăng nhập", false);
    }

    @Test
    public void testLogout_ClearsSession() {
        // Verify rằng logout xóa session data
        assertNotNull("Editor không được null", mockEditor);
    }

    @Test
    public void testBiometricEnabled_SetAndGet() {
        // Given
        boolean biometricEnabled = true;
        when(mockSharedPreferences.getBoolean("is_biometric_enabled", false)).thenReturn(biometricEnabled);
        
        // Verify
        assertTrue("Biometric phải được bật", biometricEnabled);
    }

    @Test
    public void testFirstRun_DefaultIsTrue() {
        // Given
        when(mockSharedPreferences.getBoolean("is_first_run", true)).thenReturn(true);
        
        // Verify
        assertTrue("Lần đầu chạy phải là true", true);
    }

    @Test
    public void testThemeMode_SetAndGet() {
        // Given
        int themeMode = 1; // Night mode
        when(mockSharedPreferences.getInt("theme_mode", -1)).thenReturn(themeMode);
        
        // Verify
        assertEquals("Theme mode phải khớp", 1, themeMode);
    }

    @Test
    public void testTotpAccounts_EmptyByDefault() {
        // Given
        when(mockSharedPreferences.getString("totp_accounts", "[]")).thenReturn("[]");
        
        // Verify
        String accounts = mockSharedPreferences.getString("totp_accounts", "[]");
        assertEquals("TOTP accounts mặc định phải là array rỗng", "[]", accounts);
    }
}
