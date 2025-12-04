package com.test.lifehub.features.four_calendar.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Quản lý Dữ liệu Sự kiện Lịch từ Firestore
 * 
 * Repository này thực hiện:
 * - Lắng nghe realtime các thay đổi sự kiện trên Firestore
 * - Thêm, sửa, xóa sự kiện lịch
 * - Đảm bảo cách ly dữ liệu giữa các user (mỗi user chỉ xem sự kiện của mình)
 * - Quản lý lifecycle của Firestore listener để tránh memory leak
 * 
 * Cấu trúc Firestore:
 * users/{userId}/calendar_events/{eventId}
 * 
 * Lưu ý quan trọng:
 * - @Singleton: Đảm bảo chỉ có 1 instance trong toàn app
 * - Khi user đổi: phải dừng listener cũ và xóa dữ liệu cũ
 * - Không dùng orderBy() trong Firestore query để tránh phải tạo composite index
 */
@Singleton
public class CalendarRepository {

    private static final String TAG = "CalendarRepository";
    private static final String COLLECTION_EVENTS = "calendar_events"; // Tên collection trong Firestore

    private final FirebaseAuth mAuth;           // Quản lý xác thực user
    private final FirebaseFirestore mDb;        // Database Firestore
    private CollectionReference mEventsCollection; // Tham chiếu đến collection events

    private final MutableLiveData<List<CalendarEvent>> mAllEvents = new MutableLiveData<>();
    
    // Quản lý lifecycle của Firestore listener
    private boolean isListening = false;          // Đang lắng nghe Firestore?
    private String currentUserId = null;          // User ID hiện tại đang lắng nghe
    private ListenerRegistration listenerRegistration = null; // Để hủy listener khi cần

    @Inject
    public CalendarRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        startListening();
    }

    /**
     * Lấy tham chiếu đến collection chứa sự kiện của user hiện tại
     * Đường dẫn: users/{userId}/calendar_events
     * 
     * @return CollectionReference, hoặc null nếu user chưa đăng nhập
     */
    private CollectionReference getEventsCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users").document(user.getUid()).collection(COLLECTION_EVENTS);
        }
        return null;
    }

    /**
     * Bắt đầu lắng nghe thay đổi sự kiện realtime từ Firestore
     * 
     * Quy trình:
     * 1. Kiểm tra user hiện tại có đăng nhập không
     * 2. Nếu user thay đổi: dừng listener cũ và xóa dữ liệu cũ
     * 3. Nếu đã lắng nghe cho cùng user: không làm gì (tránh duplicate)
     * 4. Tạo listener mới với filter theo userOwnerId
     * 
     * Lưu ý: KHÔNG dùng .orderBy() để tránh phải tạo composite index.
     *         Sắp xếp sẽ được thực hiện phía client trong Fragment.
     */
    public void startListening() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot listen to events");
            stopListening();
            mAllEvents.setValue(new ArrayList<>());
            return;
        }
        
        String newUserId = currentUser.getUid();
        
        // Nếu user thay đổi, dừng listener cũ và xóa dữ liệu
        if (currentUserId != null && !currentUserId.equals(newUserId)) {
            Log.d(TAG, "User changed from " + currentUserId + " to " + newUserId + ", stopping old listener");
            stopListening();
            mAllEvents.setValue(new ArrayList<>());
        }
        
        // Nếu đã đang lắng nghe cho cùng user, không làm gì
        if (isListening && newUserId.equals(currentUserId)) {
            Log.d(TAG, "Already listening to Firestore for user: " + newUserId);
            return;
        }
        
        currentUserId = newUserId;
        mEventsCollection = getEventsCollection();
        
        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting Firestore listener for calendar events");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "========================================");
        
        if (mEventsCollection == null) {
            Log.w(TAG, "CollectionReference is null");
            return;
        }
        
        listenForEventChanges();
    }
    
    /**
     * ✅ THÊM: Dừng lắng nghe Firestore
     */
    public void stopListening() {
        if (listenerRegistration != null) {
            Log.d(TAG, "Removing Firestore listener for user: " + currentUserId);
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        isListening = false;
        currentUserId = null;
        mAllEvents.setValue(new ArrayList<>());
    }

    private void listenForEventChanges() {
        if (mEventsCollection == null || mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();
        
        // Query tất cả events trong collection của user (đã được cách ly bởi path users/{userId}/calendar_events)
        // KHÔNG dùng whereEqualTo() để tránh vấn đề với dữ liệu cũ không có field userOwnerId
        // Path-based security đã đủ để cách ly dữ liệu giữa các user
        listenerRegistration = mEventsCollection
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "❌ Error listening to events", e);
                        Log.w(TAG, "Error details: " + e.getMessage());
                        return;
                    }
                    if (snapshot != null) {
                        List<CalendarEvent> events = snapshot.toObjects(CalendarEvent.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            events.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        
                        // ✅ Client-side validation: log warning nếu phát hiện event với userOwnerId sai
                        List<CalendarEvent> validEvents = new ArrayList<>();
                        for (CalendarEvent event : events) {
                            if (event.getUserOwnerId() == null || currentUserId.equals(event.getUserOwnerId())) {
                                validEvents.add(event);
                            } else {
                                Log.w(TAG, "⚠️ Found event with wrong userOwnerId: " + event.getUserOwnerId() + " (expected: " + currentUserId + ")");
                            }
                        }
                        
                        mAllEvents.setValue(validEvents);
                        Log.d(TAG, "✅ Events updated: " + validEvents.size() + " items (from " + events.size() + " total)");
                    }
                });
        
        isListening = true;
        Log.d(TAG, "Firestore listener started successfully");
    }

    public LiveData<List<CalendarEvent>> getAllEvents() {
        return mAllEvents;
    }

    public LiveData<List<CalendarEvent>> getEventsForDateRange(Date startDate, Date endDate) {
        MutableLiveData<List<CalendarEvent>> eventsData = new MutableLiveData<>();
        if (mEventsCollection == null || mAuth.getCurrentUser() == null) return eventsData;
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Query tất cả events, filter theo date range ở client-side
        mEventsCollection
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "❌ Error getting events for range", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<CalendarEvent> events = snapshot.toObjects(CalendarEvent.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            events.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        
                        // ✅ Filter theo date range và validate userOwnerId ở client-side
                        List<CalendarEvent> filteredEvents = new java.util.ArrayList<>();
                        for (CalendarEvent event : events) {
                            // Validate userOwnerId
                            if (event.getUserOwnerId() != null && !currentUserId.equals(event.getUserOwnerId())) {
                                Log.w(TAG, "⚠️ Skipping event with wrong userOwnerId in date range query");
                                continue;
                            }
                            
                            if (event.getStartTime() != null) {
                                long eventTime = event.getStartTime().getTime();
                                if (eventTime >= startDate.getTime() && eventTime < endDate.getTime()) {
                                    filteredEvents.add(event);
                                }
                            }
                        }
                        
                        eventsData.setValue(filteredEvents);
                        Log.d(TAG, "✅ Events for range: " + filteredEvents.size() + " items");
                    }
                });
        return eventsData;
    }

    public LiveData<CalendarEvent> getEventById(String documentId) {
        MutableLiveData<CalendarEvent> eventData = new MutableLiveData<>();
        if (mEventsCollection == null) return eventData;

        mEventsCollection.document(documentId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                CalendarEvent event = snapshot.toObject(CalendarEvent.class);
                if (event != null) {
                    event.documentId = snapshot.getId();
                    eventData.setValue(event);
                }
            }
        });
        return eventData;
    }

    public void insertEvent(CalendarEvent event) {
        if (mEventsCollection == null || mAuth.getCurrentUser() == null) {
            Log.w(TAG, "❌ Cannot insert event: user not logged in");
            return;
        }
        
        // ✅ BẢO MẬT: Luôn ghi đè userOwnerId bằng UID của user hiện tại
        // Ngăn chặn việc user A tạo event với userOwnerId của user B
        String currentUserId = mAuth.getCurrentUser().getUid();
        event.setUserOwnerId(currentUserId);
        
        Log.d(TAG, "Inserting event for user: " + currentUserId);
        mEventsCollection.add(event)
                .addOnSuccessListener(ref -> Log.d(TAG, "✅ Event added: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Error adding event", e));
    }

    public void updateEvent(CalendarEvent event) {
        if (mEventsCollection == null || event.documentId == null) {
            Log.w(TAG, "❌ Cannot update event: collection or documentId is null");
            return;
        }
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "❌ Cannot update event: user not logged in");
            return;
        }
        
        // ✅ BẢO MẬT: Luôn ghi đè userOwnerId bằng UID của user hiện tại
        // Ngăn chặn việc user A update event với userOwnerId của user B
        String currentUserId = currentUser.getUid();
        event.setUserOwnerId(currentUserId);
        
        // ✅ BẢO MẬT: Verify event thuộc về user hiện tại trước khi update
        mEventsCollection.document(event.documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CalendarEvent existingEvent = documentSnapshot.toObject(CalendarEvent.class);
                        if (existingEvent != null && existingEvent.getUserOwnerId() != null) {
                            if (!existingEvent.getUserOwnerId().equals(currentUserId)) {
                                Log.w(TAG, "❌ SECURITY VIOLATION: User " + currentUserId + " attempted to update event owned by " + existingEvent.getUserOwnerId());
                                return;
                            }
                        }
                        
                        // OK, user owns this event, proceed with update
                        mEventsCollection.document(event.documentId).set(event)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Event updated"))
                                .addOnFailureListener(e -> Log.w(TAG, "❌ Error updating event", e));
                    } else {
                        Log.w(TAG, "❌ Event not found: " + event.documentId);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "❌ Error verifying event ownership", e));
    }

    public void deleteEvent(CalendarEvent event) {
        if (mEventsCollection == null || event.documentId == null) {
            Log.w(TAG, "❌ Cannot delete event: collection or documentId is null");
            return;
        }
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "❌ Cannot delete event: user not logged in");
            return;
        }
        
        String currentUserId = currentUser.getUid();
        
        // ✅ BẢO MẬT: Verify event thuộc về user hiện tại trước khi delete
        mEventsCollection.document(event.documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CalendarEvent existingEvent = documentSnapshot.toObject(CalendarEvent.class);
                        if (existingEvent != null && existingEvent.getUserOwnerId() != null) {
                            if (!existingEvent.getUserOwnerId().equals(currentUserId)) {
                                Log.w(TAG, "❌ SECURITY VIOLATION: User " + currentUserId + " attempted to delete event owned by " + existingEvent.getUserOwnerId());
                                return;
                            }
                        }
                        
                        // OK, user owns this event, proceed with delete
                        mEventsCollection.document(event.documentId).delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Event deleted"))
                                .addOnFailureListener(e -> Log.w(TAG, "❌ Error deleting event", e));
                    } else {
                        Log.w(TAG, "❌ Event not found: " + event.documentId);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "❌ Error verifying event ownership", e));
    }
}