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

@Singleton
public class CalendarRepository {

    private static final String TAG = "CalendarRepository";
    private static final String COLLECTION_EVENTS = "calendar_events";

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private CollectionReference mEventsCollection;

    private final MutableLiveData<List<CalendarEvent>> mAllEvents = new MutableLiveData<>();
    
    // ✅ THÊM: Track listener để quản lý lifecycle
    private boolean isListening = false;
    private String currentUserId = null;
    private ListenerRegistration listenerRegistration = null;

    @Inject
    public CalendarRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        startListening();
    }

    private CollectionReference getEventsCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users").document(user.getUid()).collection(COLLECTION_EVENTS);
        }
        return null;
    }

    /**
     * ✅ SỬA LỖI: Bắt đầu lắng nghe thay đổi từ Firestore
     * Tương tự AccountRepository - kiểm tra user thay đổi và reset listener
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
        
        // ✅ SỬA LỖI: Chỉ dùng whereEqualTo, không orderBy để tránh cần composite index
        // Sẽ sắp xếp ở client-side (trong Fragment/ViewModel)
        listenerRegistration = mEventsCollection
                .whereEqualTo("userOwnerId", currentUserId)
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
                        mAllEvents.setValue(events);
                        Log.d(TAG, "✅ Events updated: " + events.size() + " items");
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

        // ✅ SỬA LỖI: Chỉ filter theo userOwnerId, loại bỏ orderBy để tránh cần index
        // Filter theo date range sẽ làm ở client-side
        mEventsCollection
                .whereEqualTo("userOwnerId", currentUserId)
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
                        
                        // ✅ Filter theo date range ở client-side
                        List<CalendarEvent> filteredEvents = new java.util.ArrayList<>();
                        for (CalendarEvent event : events) {
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
        if (mEventsCollection == null || mAuth.getCurrentUser() == null) return;
        event.setUserOwnerId(mAuth.getCurrentUser().getUid());
        mEventsCollection.add(event)
                .addOnSuccessListener(ref -> Log.d(TAG, "✅ Event added: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Error adding event", e));
    }

    public void updateEvent(CalendarEvent event) {
        if (mEventsCollection == null || event.documentId == null) return;
        mEventsCollection.document(event.documentId).set(event)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Event updated"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Error updating event", e));
    }

    public void deleteEvent(CalendarEvent event) {
        if (mEventsCollection == null || event.documentId == null) return;
        mEventsCollection.document(event.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Event deleted"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Error deleting event", e));
    }
}