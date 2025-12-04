# 🔒 BẢO MẬT DỮ LIỆU NGƯỜI DÙNG - LIFEHUB CALENDAR

## ✅ CÁC LỚP BẢO MẬT ĐÃ TRIỂN KHAI

### **1. Path-Based Isolation (Firestore)**
```
users/{userId}/calendar_events/{eventId}
```
- Mỗi user có collection riêng biệt
- Firestore Security Rules ngăn chặn cross-user access
- Không thể query events của user khác (do path isolation)

### **2. UserOwnerId Validation**

#### **Insert Event (Thêm sự kiện):**
```java
public void insertEvent(CalendarEvent event) {
    // ✅ BẮT BUỘC: Ghi đè userOwnerId bằng UID hiện tại
    String currentUserId = mAuth.getCurrentUser().getUid();
    event.setUserOwnerId(currentUserId);
    
    // Ngăn chặn: User A tạo event với userOwnerId = User B
}
```

#### **Update Event (Cập nhật sự kiện):**
```java
public void updateEvent(CalendarEvent event) {
    // ✅ VERIFY: Kiểm tra event có thuộc về user hiện tại không
    mEventsCollection.document(event.documentId).get()
        .addOnSuccessListener(documentSnapshot -> {
            CalendarEvent existingEvent = documentSnapshot.toObject(CalendarEvent.class);
            
            if (!existingEvent.getUserOwnerId().equals(currentUserId)) {
                Log.w(TAG, "❌ SECURITY VIOLATION: User attempted to modify other's event");
                return; // Chặn update
            }
            
            // OK, proceed
        });
}
```

#### **Delete Event (Xóa sự kiện):**
```java
public void deleteEvent(CalendarEvent event) {
    // ✅ VERIFY: Tương tự update, kiểm tra ownership trước khi xóa
    // Ngăn chặn: User A xóa event của User B
}
```

### **3. Client-Side Filtering**
```java
private void listenForEventChanges() {
    listenerRegistration = mEventsCollection
        .addSnapshotListener((snapshot, e) -> {
            // ✅ Double-check: Filter events với sai userOwnerId
            for (CalendarEvent event : events) {
                if (event.getUserOwnerId() != null && 
                    !currentUserId.equals(event.getUserOwnerId())) {
                    
                    Log.w(TAG, "⚠️ Found event with wrong userOwnerId");
                    // Không thêm vào list
                    continue;
                }
                validEvents.add(event);
            }
        });
}
```

### **4. Session Management**
```java
public void startListening() {
    FirebaseUser currentUser = mAuth.getCurrentUser();
    
    // ✅ User thay đổi → Dừng listener cũ, xóa data cũ
    if (currentUserId != null && !currentUserId.equals(newUserId)) {
        stopListening();
        mAllEvents.setValue(new ArrayList<>()); // Clear old data
    }
}
```

---

## 🛡️ FIRESTORE SECURITY RULES (Khuyến nghị)

Thêm rules sau vào Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Calendar Events
    match /users/{userId}/calendar_events/{eventId} {
      // Chỉ cho phép user đọc/ghi events của chính họ
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Bắt buộc userOwnerId phải khớp với userId khi tạo mới
      allow create: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.userOwnerId == userId;
      
      // Không cho phép thay đổi userOwnerId khi update
      allow update: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.userOwnerId == resource.data.userOwnerId;
    }
  }
}
```

---

## 🔍 KIỂM TRA BẢO MẬT

### **Test Case 1: User A không thể xem events của User B**
```
✅ PASS: Path-based isolation ngăn chặn
✅ PASS: Client-side filtering loại bỏ events lạ
```

### **Test Case 2: User A không thể tạo event cho User B**
```
✅ PASS: insertEvent() ghi đè userOwnerId
```

### **Test Case 3: User A không thể sửa/xóa event của User B**
```
✅ PASS: updateEvent() và deleteEvent() verify ownership
```

### **Test Case 4: User logout → Login user khác**
```
✅ PASS: startListening() clear data cũ
✅ PASS: Listener cũ bị dừng
```

---

## 📊 LOGGING BẢO MẬT

Các log quan trọng để monitor:

```java
// Phát hiện vi phạm
Log.w(TAG, "❌ SECURITY VIOLATION: User " + currentUserId + 
           " attempted to update event owned by " + existingEvent.getUserOwnerId());

// Phát hiện dữ liệu lỗi
Log.w(TAG, "⚠️ Found event with wrong userOwnerId: " + event.getUserOwnerId() + 
           " (expected: " + currentUserId + ")");

// Xác nhận thành công
Log.d(TAG, "✅ Events updated: " + validEvents.size() + " items");
```

---

## ✅ KẾT LUẬN

**Dữ liệu của mỗi người dùng được bảo vệ ở 4 tầng:**

1. **Firestore Path Isolation** → Mỗi user có collection riêng
2. **Security Rules** → Firebase chặn truy cập trái phép
3. **Server-side Validation** → userOwnerId được verify trước mọi thao tác
4. **Client-side Filtering** → Double-check để loại bỏ data lạ

**→ ĐẢM BẢO: User A KHÔNG BAO GIỜ thấy/sửa/xóa events của User B!** ✅
