# Phishing Simulation Training App

A comprehensive Android application designed to train employees in recognizing and avoiding phishing attacks through simulated campaigns.

## Overview

This application is a security awareness training platform that allows administrators to create phishing simulation campaigns and track how users interact with them. The goal is to educate users about phishing threats in a controlled environment.

## Features

### Admin Features
- **Campaign Management**: Create, view, and delete phishing simulation campaigns
- **Real-time Dashboard**: View all active campaigns with real-time updates
- **Statistics & Analytics**: 
  - View total campaigns and detections
  - Interactive bar charts showing campaign performance
  - Recent detection history with user details and timestamps
  - Location tracking for detections

### User Features
- **Campaign Viewing**: Users see assigned phishing campaigns in their dashboard
- **Interactive Simulation**: Click on campaigns to open the phishing landing page
- **Location Tracking**: Automatically captures device location when user clicks (with permission)
- **Real-time Updates**: Campaign list updates automatically when new campaigns are created

### Authentication
- Firebase Authentication for secure user login
- Role-based access control (Admin vs Regular User)
- User registration with email/password
- Automatic role-based navigation

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM-like pattern with Repository
- **UI**: Material Design 3 (Material Components)
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **Location Services**: Google Play Services Location API
- **Charts**: MPAndroidChart library
- **Async**: Kotlin Coroutines with Flow

## Project Structure

```
app/src/main/java/com/phishing/simulation/
├── AdminMainActivity.kt         # Admin dashboard
├── UserMainActivity.kt          # User dashboard (phishing target)
├── StatisticsActivity.kt        # Analytics and graphs
├── LoginActivity.kt             # Login screen
├── RegisterActivity.kt          # Registration screen
├── adapter/
│   ├── CampaignAdapter.kt      # Campaign list adapter
│   └── DetectionAdapter.kt      # Detection list adapter
├── auth/
│   └── AuthManager.kt           # Authentication logic
├── model/
│   └── Models.kt                # Data models (User, Campaign, Detection)
└── repository/
    └── FirebaseRepository.kt    # Firebase data operations
```

## Data Models

### User
- Name, Email, Role (Admin/Viewer)
- Department
- FCM Token (for future push notifications)
- Creation timestamp

### Campaign
- Title, Description
- Landing Page URL (phishing page)
- Created By, Creation timestamp

### Detection
- Campaign ID, User ID
- Location (GeoPoint with lat/long)
- Timestamp

## User Flow

### Admin Flow
1. Admin logs in → Directed to Admin Dashboard
2. Creates new campaigns with:
   - Title
   - Description/Email body
   - Phishing URL (e.g., the fake Google login page)
3. Views all campaigns in real-time list
4. Clicks Statistics to view:
   - Total campaigns created
   - Total detections (users who clicked)
   - Bar chart of performance per campaign
   - List of recent detections with user info

### Regular User Flow
1. User logs in → Directed to User Dashboard
2. Sees the most recent campaign assigned to them
3. Views campaign details (title and description)
4. Clicks "View Details" button
5. System requests location permission (if not granted)
6. System captures location and saves detection to database
7. Phishing page opens in browser
8. Detection is recorded with user ID, campaign ID, location, and timestamp

## Security & Permissions

### Android Permissions Required
- `INTERNET` - For Firebase and web access
- `ACCESS_FINE_LOCATION` - For precise location tracking
- `ACCESS_COARSE_LOCATION` - For approximate location tracking

### Firebase Security
- Authentication required for all operations
- Role-based access control
- Firestore security rules should be configured to:
  - Allow admins to create/delete campaigns
  - Allow all authenticated users to read campaigns
  - Allow all authenticated users to write detections

## Setup Instructions

### Prerequisites
1. Android Studio Hedgehog or newer
2. Firebase project with:
   - Authentication enabled (Email/Password)
   - Firestore Database
3. Google Play Services

### Configuration
1. Clone the repository
2. Add your `google-services.json` to `app/` directory
3. Sync Gradle
4. Configure Firebase Security Rules (see below)
5. Run the app

### Firebase Security Rules (Recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /Users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null;
    }
    
    // Campaigns collection
    match /Campaigns/{campaignId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/Users/$(request.auth.uid)).data.Role == "Admin";
    }
    
    // Detections collection
    match /Detections/{detectionId} {
      allow read: if request.auth != null && 
                    get(/databases/$(database)/documents/Users/$(request.auth.uid)).data.Role == "Admin";
      allow create: if request.auth != null;
    }
  }
}
```

## Testing Accounts

Create test accounts with different roles:
- Admin: Set `Role` field to `"Admin"` in Firestore Users collection
- Regular User: Default `Role` is `"Viewer"`

## Phishing Landing Page

The fake phishing page is deployed at:
- URL: https://mobile-final-project-483a5.web.app

This can be used as the landing page URL when creating campaigns.

## Implementation Details

### Location Tracking
- Uses FusedLocationProviderClient for efficient location access
- Requests permissions at runtime using ActivityResultContracts
- Falls back gracefully if permission denied (records with 0,0 coordinates)
- Uses high-accuracy priority for location requests

### Real-time Updates
- Uses Firestore snapshot listeners with Kotlin Flow
- Lifecycle-aware collection (automatically unsubscribes when app is in background)
- Admin dashboard updates automatically when campaigns change

### Charts and Analytics
- MPAndroidChart library for bar charts
- Shows top 10 campaigns by detection count
- Interactive charts with animations
- Displays recent detections with user email and relative timestamps

## Known Limitations & Future Enhancements

### Current Limitations
- All users see the same campaign (first/newest campaign)
- No user-campaign assignment system
- Location always uses GeoPoint(0,0) if permission denied

### Planned Enhancements
- Campaign assignment system (assign specific campaigns to specific users)
- Push notifications when new campaigns are assigned
- More detailed analytics (click-through rates, time-to-click metrics)
- Export detection data as CSV
- User training materials and phishing awareness content
- Multiple phishing templates

## Credits

Developed as a final project for Mobile Application Development course.

## License

This is an educational project. Not for commercial use.
