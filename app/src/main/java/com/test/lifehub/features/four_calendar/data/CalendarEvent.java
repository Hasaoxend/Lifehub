package com.test.lifehub.features.four_calendar.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO cho Calendar Event (Sự kiện Lịch)
 */
public class CalendarEvent implements Serializable {

    @Exclude
    public String documentId;

    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private String color; // Hex color code
    private String userOwnerId;

    @ServerTimestamp
    private Date createdDate;

    @ServerTimestamp
    private Date lastModified;

    // Reminder
    private Date reminderTime;
    private int alarmRequestCode;

    // Repeat options
    private String repeatType; // NONE, DAILY, WEEKLY, MONTHLY
    private Date repeatUntil;

    public CalendarEvent() {
        this.color = "#2196F3"; // Default blue
        this.repeatType = "NONE";
    }

    public CalendarEvent(String title, Date startTime, Date endTime) {
        this();
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters & Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getUserOwnerId() { return userOwnerId; }
    public void setUserOwnerId(String userOwnerId) { this.userOwnerId = userOwnerId; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }

    public Date getReminderTime() { return reminderTime; }
    public void setReminderTime(Date reminderTime) { this.reminderTime = reminderTime; }

    public int getAlarmRequestCode() { return alarmRequestCode; }
    public void setAlarmRequestCode(int alarmRequestCode) { this.alarmRequestCode = alarmRequestCode; }

    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }

    public Date getRepeatUntil() { return repeatUntil; }
    public void setRepeatUntil(Date repeatUntil) { this.repeatUntil = repeatUntil; }

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}