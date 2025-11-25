package com.test.lifehub.features.authenticator.data;

/**
 * Model cho tài khoản TOTP/2FA trên Firestore
 */
public class TotpAccount {
    
    public String documentId;        // ID document trên Firestore
    public String userOwnerId;       // UID của user sở hữu
    public String accountName;       // Tên tài khoản (email, username)
    public String issuer;            // Tên service (Google, Facebook, GitHub...)
    public String secretKey;         // Secret key đã mã hóa
    public long createdAt;           // Timestamp tạo
    public long updatedAt;           // Timestamp cập nhật
    
    // Constructor không tham số (bắt buộc cho Firestore)
    public TotpAccount() {
    }
    
    // Constructor đầy đủ
    public TotpAccount(String accountName, String issuer, String secretKey) {
        this.accountName = accountName;
        this.issuer = issuer;
        this.secretKey = secretKey;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters và Setters
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getUserOwnerId() {
        return userOwnerId;
    }
    
    public void setUserOwnerId(String userOwnerId) {
        this.userOwnerId = userOwnerId;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
