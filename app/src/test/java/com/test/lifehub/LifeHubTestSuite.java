package com.test.lifehub;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.test.lifehub.core.security.EncryptionHelperTest;
import com.test.lifehub.core.util.SessionManagerTest;
import com.test.lifehub.features.authenticator.ui.AuthenticatorTest;
import com.test.lifehub.features.four_calendar.ui.CalendarViewModelTest;
import com.test.lifehub.features.one_accounts.ui.AccountViewModelTest;
import com.test.lifehub.features.two_productivity.ui.ProductivityViewModelTest;
import com.test.lifehub.integration.IntegrationTest;
import com.test.lifehub.ui.LoginViewModelTest;

/**
 * Test Suite cho toàn bộ ứng dụng LifeHub
 * Chạy tất cả unit tests và integration tests
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // Core Tests
    EncryptionHelperTest.class,
    SessionManagerTest.class,
    
    // Feature Tests - Authentication
    LoginViewModelTest.class,
    
    // Feature Tests - Accounts
    AccountViewModelTest.class,
    
    // Feature Tests - Productivity
    ProductivityViewModelTest.class,
    
    // Feature Tests - Calendar
    CalendarViewModelTest.class,
    
    // Feature Tests - Authenticator
    AuthenticatorTest.class,
    
    // Integration Tests
    IntegrationTest.class
})
public class LifeHubTestSuite {
    // Test suite sẽ chạy tất cả các test classes được liệt kê
}
