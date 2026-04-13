package com.phishing.simulation.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

/**
 * Represents an authenticated user in the system.
 *
 * Firestore collection: "Users"
 * Document ID = Firebase Auth UID
 *
 * Role values: "Admin" | "Viewer"
 */
data class User(
    @get:PropertyName("Name")
    @set:PropertyName("Name")
    var name: String = "",

    @get:PropertyName("Email")
    @set:PropertyName("Email")
    var email: String = "",

    @get:PropertyName("Role")
    @set:PropertyName("Role")
    var role: String = "",

    @get:PropertyName("Department")
    @set:PropertyName("Department")
    var department: String = "",

    @get:PropertyName("fcmToken")
    @set:PropertyName("fcmToken")
    var fcmToken: String = "",

    @get:PropertyName("CreatedAt")
    @set:PropertyName("CreatedAt")
    var createdAt: Timestamp = Timestamp.now()
)

/**
 * Represents a phishing simulation campaign.
 *
 * Firestore collection: "Campaigns"
 */
data class Campaign(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("Title")
    @set:PropertyName("Title")
    var title: String = "",

    @get:PropertyName("Description")
    @set:PropertyName("Description")
    var description: String = "",

    @get:PropertyName("LandingPageUrl")
    @set:PropertyName("LandingPageUrl")
    var landingPageUrl: String = "",

    @get:PropertyName("Department")
    @set:PropertyName("Department")
    var department: String = "All",

    @get:PropertyName("CreatedBy")
    @set:PropertyName("CreatedBy")
    var createdBy: String = "",

    @get:PropertyName("CreatedAt")
    @set:PropertyName("CreatedAt")
    var createdAt: Timestamp = Timestamp.now()
)

/**
 * Represents a recorded phishing click / detection event.
 *
 * Firestore collection: "Detections"
 */
data class Detection(
    @get:PropertyName("CampaignId")
    @set:PropertyName("CampaignId")
    var campaignId: String = "",

    @get:PropertyName("UserId")
    @set:PropertyName("UserId")
    var userId: String = "",

    @get:PropertyName("Location")
    @set:PropertyName("Location")
    var location: GeoPoint = GeoPoint(0.0, 0.0),

    @get:PropertyName("Timestamp")
    @set:PropertyName("Timestamp")
    var timestamp: Timestamp = Timestamp.now()
)

/**
 * Represents a direct message between two users.
 *
 * Firestore collection: "Messages"
 */
data class Message(
    @get:PropertyName("SenderId")
    @set:PropertyName("SenderId")
    var senderId: String = "",

    @get:PropertyName("ReceiverId")
    @set:PropertyName("ReceiverId")
    var receiverId: String = "",

    @get:PropertyName("Text")
    @set:PropertyName("Text")
    var text: String = "",

    @get:PropertyName("Timestamp")
    @set:PropertyName("Timestamp")
    var timestamp: Timestamp = Timestamp.now()
)
