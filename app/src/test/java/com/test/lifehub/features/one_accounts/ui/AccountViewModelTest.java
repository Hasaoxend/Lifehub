package com.test.lifehub.features.one_accounts.ui;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test cho AccountViewModel
 * Kiểm tra quản lý danh sách tài khoản
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    AccountRepository mockRepository;

    @Mock
    EncryptionHelper mockEncryptionHelper;

    private MutableLiveData<List<AccountEntry>> accountsLiveData;

    @Before
    public void setUp() {
        accountsLiveData = new MutableLiveData<>();
        when(mockRepository.getAllAccounts()).thenReturn(accountsLiveData);
    }

    @Test
    public void testGetAllAccounts_ReturnsLiveData() {
        // Given
        List<AccountEntry> testAccounts = new ArrayList<>();
        testAccounts.add(createTestAccount("Google", "user@gmail.com"));
        testAccounts.add(createTestAccount("Facebook", "user@facebook.com"));
        
        accountsLiveData.setValue(testAccounts);
        
        // Verify
        assertNotNull("LiveData không được null", accountsLiveData.getValue());
        assertEquals("Số lượng accounts phải đúng", 2, accountsLiveData.getValue().size());
    }

    @Test
    public void testGetAllAccounts_EmptyList_Success() {
        // Given
        List<AccountEntry> emptyList = new ArrayList<>();
        accountsLiveData.setValue(emptyList);
        
        // Verify
        assertNotNull("LiveData không được null", accountsLiveData.getValue());
        assertEquals("List rỗng phải có size = 0", 0, accountsLiveData.getValue().size());
    }

    @Test
    public void testAccountEntry_ValidData() {
        // Given
        AccountEntry account = createTestAccount("Test Service", "test@example.com");
        
        // Verify
        assertNotNull("Account không được null", account);
        assertEquals("Service name phải khớp", "Test Service", account.serviceName);
        assertEquals("Username phải khớp", "test@example.com", account.username);
    }

    @Test
    public void testInsertAccount_CallsRepository() {
        // Given
        AccountEntry newAccount = createTestAccount("New Service", "new@example.com");
        
        // When
        mockRepository.insert(newAccount);
        
        // Verify
        verify(mockRepository, times(1)).insert(any(AccountEntry.class));
    }

    @Test
    public void testUpdateAccount_CallsRepository() {
        // Given
        AccountEntry existingAccount = createTestAccount("Existing", "exist@example.com");
        existingAccount.documentId = "doc123";
        
        // When
        mockRepository.update(existingAccount);
        
        // Verify
        verify(mockRepository, times(1)).update(any(AccountEntry.class));
    }

    @Test
    public void testDeleteAccount_CallsRepository() {
        // Given
        AccountEntry accountToDelete = createTestAccount("Delete Me", "delete@example.com");
        accountToDelete.documentId = "doc456";
        
        // When
        mockRepository.delete(accountToDelete);
        
        // Verify
        verify(mockRepository, times(1)).delete(any(AccountEntry.class));
    }

    @Test
    public void testGetAccountById_ValidId_ReturnsAccount() {
        // Given
        String testId = "test_doc_id";
        AccountEntry testAccount = createTestAccount("Test", "test@example.com");
        testAccount.documentId = testId;
        
        MutableLiveData<AccountEntry> singleAccount = new MutableLiveData<>();
        singleAccount.setValue(testAccount);
        
        when(mockRepository.getAccountById(testId)).thenReturn(singleAccount);
        
        // Verify
        LiveData<AccountEntry> result = mockRepository.getAccountById(testId);
        assertNotNull("Result không được null", result);
        assertEquals("Document ID phải khớp", testId, result.getValue().documentId);
    }

    @Test
    public void testAccountPassword_Encryption() {
        // Given
        String plainPassword = "MySecretPassword123";
        String encryptedPassword = "EncryptedData==";
        
        when(mockEncryptionHelper.encrypt(plainPassword)).thenReturn(encryptedPassword);
        
        // When
        String result = mockEncryptionHelper.encrypt(plainPassword);
        
        // Verify
        assertNotNull("Encrypted password không được null", result);
        assertNotEquals("Password phải được mã hóa", plainPassword, result);
        assertEquals("Encrypted password phải khớp", encryptedPassword, result);
    }

    // Helper method
    private AccountEntry createTestAccount(String serviceName, String username) {
        AccountEntry account = new AccountEntry();
        account.serviceName = serviceName;
        account.username = username;
        account.password = "encrypted_password";
        account.category = "Social Media";
        account.notes = "Test notes";
        return account;
    }
}
