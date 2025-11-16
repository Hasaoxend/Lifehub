package com.test.lifehub.features.four_calendar.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

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

    @Inject
    public CalendarRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            mEventsCollection = mDb.collection("users").document(uid).collection(COLLECTION_EVENTS);
            listenForEventChanges();
        }
    }

    private void listenForEventChanges() {
        if (mEventsCollection == null) return;
        mEventsCollection.orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Error listening to events", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<CalendarEvent> events = snapshot.toObjects(CalendarEvent.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            events.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        mAllEvents.setValue(events);
                        Log.d(TAG, "Events updated: " + events.size() + " items");
                    }
                });
    }

    public LiveData<List<CalendarEvent>> getAllEvents() {
        return mAllEvents;
    }

    public LiveData<List<CalendarEvent>> getEventsForDateRange(Date startDate, Date endDate) {
        MutableLiveData<List<CalendarEvent>> eventsData = new MutableLiveData<>();
        if (mEventsCollection == null) return eventsData;

        mEventsCollection
                .whereGreaterThanOrEqualTo("startTime", startDate)
                .whereLessThan("startTime", endDate)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Error getting events for range", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<CalendarEvent> events = snapshot.toObjects(CalendarEvent.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            events.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        eventsData.setValue(events);
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
                .addOnSuccessListener(ref -> Log.d(TAG, "Event added: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding event", e));
    }

    public void updateEvent(CalendarEvent event) {
        if (mEventsCollection == null || event.documentId == null) return;
        mEventsCollection.document(event.documentId).set(event)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Event updated"))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating event", e));
    }

    public void deleteEvent(CalendarEvent event) {
        if (mEventsCollection == null || event.documentId == null) return;
        mEventsCollection.document(event.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Event deleted"))
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting event", e));
    }
}