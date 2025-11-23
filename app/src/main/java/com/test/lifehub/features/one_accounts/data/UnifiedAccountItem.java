package com.test.lifehub.features.one_accounts.data;

/**
 * Model thống nhất đại diện cho CẢ tài khoản mật khẩu VÀ tài khoản TOTP
 * 
 * Mục đích: Hiển thị cả 2 loại tài khoản trong cùng 1 RecyclerView như Microsoft Authenticator
 * 
 * 2 Loại tài khoản:
 * 1. PASSWORD: Tài khoản mật khẩu thông thường (lưu trên Firebase Firestore)
 * 2. TOTP: Tài khoản xác thực 2 yếu tố (lưu trong EncryptedSharedPreferences)
 */
public class UnifiedAccountItem {
    
    /**
     * Enum phân loại tài khoản
     */
    public enum AccountType {
        PASSWORD,  // Tài khoản mật khẩu (username, password, notes)
        TOTP       // Tài khoản TOTP (secret key, mã 6 số tự động đổi mỗi 30s)
    }
    
    private AccountType type;  // Loại tài khoản
    
    // ===== Trường chung cho cả 2 loại =====
    private String serviceName;  // Tên dịch vụ (Google, Facebook, GitHub...)
    private String username;     // Tên người dùng hoặc email
    private String id;           // ID duy nhất
    
    // ===== Chỉ dành cho PASSWORD =====
    private String password;  // Mật khẩu (chỉ có khi type = PASSWORD)
    private String notes;     // Ghi chú thêm
    
    // ===== Chỉ dành cho TOTP =====
    private String secret;  // Mã bí mật Base32 để tạo OTP (chỉ có khi type = TOTP)
    private String issuer;  // Nhà phát hành (thường giống serviceName)
    
    // ===== Để sắp xếp =====
    private long timestamp;  // Thời gian tạo (Unix timestamp)
    
    /**
     * Constructor cho TÀI KHOẢN MẬT KHẨU (từ AccountEntry - Firebase)
     * 
     * @param accountEntry Đối tượng AccountEntry từ Firebase Firestore
     */
    public UnifiedAccountItem(AccountEntry accountEntry) {
        this.type = AccountType.PASSWORD;
        this.id = accountEntry.documentId;
        this.serviceName = accountEntry.serviceName;
        this.username = accountEntry.username;
        this.password = accountEntry.password;
        this.notes = accountEntry.notes;
        this.timestamp = accountEntry.lastModified != null ? 
                        accountEntry.lastModified.getTime() : 
                        System.currentTimeMillis();
    }
    
    /**
     * Constructor cho TÀI KHOẢN MẬT KHẨU
     * 
     * @param id ID tài khoản (từ Firebase)
     * @param serviceName Tên dịch vụ (VD: "Gmail", "Facebook")
     * @param username Tên đăng nhập hoặc email
     * @param password Mật khẩu
     * @param notes Ghi chú thêm (có thể null)
     * @param timestamp Thời gian tạo
     */
    public UnifiedAccountItem(String id, String serviceName, String username, String password, String notes, long timestamp) {
        this.type = AccountType.PASSWORD;
        this.id = id;
        this.serviceName = serviceName;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.timestamp = timestamp;
    }
    
    /**
     * Constructor cho TÀI KHOẢN TOTP (Authenticator)
     * 
     * @param serviceName Tên dịch vụ (VD: "Google", "GitHub")
     * @param username Email hoặc tên người dùng
     * @param secret Mã bí mật Base32 (từ QR code hoặc nhập tay)
     * @param issuer Nhà phát hành (thường giống serviceName)
     */
    public UnifiedAccountItem(String serviceName, String username, String secret, String issuer) {
        this.type = AccountType.TOTP;
        this.id = serviceName + "_" + username; // Tạo ID từ serviceName + username
        this.serviceName = serviceName;
        this.username = username;
        this.secret = secret;
        this.issuer = issuer;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ===== Getters =====
    
    public AccountType getType() {
        return type;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getId() {
        return id;
    }
    
    // Chỉ dành cho PASSWORD
    public String getPassword() {
        return password;
    }
    
    public String getNotes() {
        return notes;
    }
    
    // Chỉ dành cho TOTP
    public String getSecret() {
        return secret;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
