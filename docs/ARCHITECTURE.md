# Architecture Document — Phishing Simulation Training App

---

## 1. System Overview

This is a client-server Android application. The **client** is the Android app running
on a user's phone. The **server** is Google Firebase — a fully managed cloud platform
that handles authentication and the database. There is no custom backend server; Firebase
replaces it entirely.

```
┌──────────────────────┐          ┌─────────────────────────────┐
│   Android App        │  HTTPS   │         Firebase Cloud      │
│   (Kotlin/XML)       │ ◄──────► │  ┌─────────────────────┐   │
│                      │          │  │   Firebase Auth      │   │
│  Screens (Activities)│          │  │  (identity / login)  │   │
│  Repository Layer    │          │  ├─────────────────────┤   │
│  Auth Layer          │          │  │   Firestore DB       │   │
└──────────────────────┘          │  │  (all app data)      │   │
                                  │  └─────────────────────┘   │
                                  └─────────────────────────────┘

                                  ┌─────────────────────────────┐
                                  │  Firebase Hosting (Web)     │
                                  │  Fake phishing landing page │
                                  │  mobile-final-project-      │
                                  │  483a5.web.app              │
                                  └─────────────────────────────┘
```

---

## 2. Architecture Pattern

The app follows a **layered architecture** (a simplified form of MVVM —
Model-View-ViewModel):

```
┌─────────────────────────────────────────────────┐
│                  VIEW LAYER                      │
│  Activities (screens) + XML layouts              │
│  What the user sees and interacts with           │
└─────────────────────┬───────────────────────────┘
                      │ calls
┌─────────────────────▼───────────────────────────┐
│              BUSINESS LOGIC LAYER                │
│  AuthManager  — handles login/registration       │
│  FirebaseRepository — handles all data access    │
└─────────────────────┬───────────────────────────┘
                      │ reads/writes
┌─────────────────────▼───────────────────────────┐
│                  DATA LAYER                      │
│  Firebase Auth — who you are                     │
│  Firestore — what data you have                  │
└─────────────────────────────────────────────────┘
```

The key principle: **screens never talk to Firebase directly**. They always go through
`AuthManager` or `FirebaseRepository`. This keeps the code clean and testable.

---

## 3. Directory Structure

```
app/src/main/
│
├── AndroidManifest.xml            ← App configuration: activities, permissions
│
├── java/com/phishing/simulation/
│   │
│   ├── LOGIN & AUTH SCREENS
│   │   ├── LoginActivity.kt       ← Login form + role-based routing
│   │   └── RegisterActivity.kt    ← New account creation
│   │
│   ├── ROLE-BASED DASHBOARDS
│   │   ├── AdminMainActivity.kt   ← Admin: campaign list + create/edit/delete
│   │   └── UserMainActivity.kt    ← Viewer: campaign list + click simulation
│   │
│   ├── SECONDARY SCREENS
│   │   ├── StatisticsActivity.kt  ← Admin analytics: charts + map + list
│   │   ├── ProfileActivity.kt     ← User profile view/edit
│   │   └── CaughtActivity.kt      ← "You got phished!" reveal screen
│   │
│   ├── adapter/                   ← List rendering components
│   │   ├── CampaignAdapter.kt     ← Admin campaign cards
│   │   ├── UserCampaignAdapter.kt ← User campaign cards
│   │   └── DetectionAdapter.kt    ← Detection list items in Statistics
│   │
│   ├── auth/
│   │   └── AuthManager.kt         ← Login, register, logout, get current role
│   │
│   ├── model/
│   │   └── Models.kt              ← Data classes: User, Campaign, Detection
│   │
│   └── repository/
│       └── FirebaseRepository.kt  ← All Firestore CRUD operations
│
└── res/
    ├── layout/                    ← XML files: visual design for every screen
    ├── values/                    ← Colors, strings, themes
    ├── drawable/                  ← App icons and graphics
    └── menu/                      ← Toolbar menu items
```

---

## 4. Data Layer — Firestore Database

### Collections (like database tables)

```
Firestore
│
├── Users/
│   └── {uid}/                     ← Document ID = Firebase Auth UID
│       ├── Name: "John Smith"
│       ├── Email: "john@company.com"
│       ├── Role: "Admin" | "Viewer"
│       ├── Department: "IT"
│       ├── fcmToken: "..."
│       └── CreatedAt: Timestamp
│
├── Campaigns/
│   └── {auto-id}/
│       ├── id: "abc123"
│       ├── Title: "IT Security Update"
│       ├── Description: "Your password will expire..."
│       ├── LandingPageUrl: "https://..."
│       ├── Department: "All" | "IT" | "HR" | ...
│       ├── CreatedBy: "admin@company.com"
│       └── CreatedAt: Timestamp
│
└── Detections/
    └── {auto-id}/
        ├── CampaignId: "abc123"
        ├── UserId: "uid456"
        ├── Location: GeoPoint(lat, long)
        └── Timestamp: Timestamp
```

### Relationships

```
User ──── (creates) ────► Campaign
User ──── (triggers) ───► Detection ◄──── (references) ──── Campaign
```

---

## 5. Authentication Layer

`AuthManager` wraps Firebase Auth and Firestore to provide two main functions:

### Login Flow

```
loginUser(email, password)
    │
    ├─ Firebase Auth: signInWithEmailAndPassword()
    │       └─ Returns a UID on success
    │
    └─ Firestore: Users/{uid}.get()
            └─ Returns the "Role" field ("Admin" or "Viewer")
```

### Register Flow

```
registerUser(name, email, password, department)
    │
    ├─ Firebase Auth: createUserWithEmailAndPassword()
    │       └─ Returns a UID
    │
    └─ Firestore: Users/{uid}.set(User object)
            └─ Saves profile with Role = "Viewer"
```

Both functions return an `AuthResult` — either `Success(role)` or `Failure(message)`,
so screens only need to handle two cases.

---

## 6. Repository Layer

`FirebaseRepository` is the single source of truth for all data operations.
It wraps every operation in a `Result<T>` type:

```kotlin
Result.Success(data)   // operation worked, here is the data
Result.Failure(exception) // something went wrong
```

### Operations Provided

| Method                       | Direction     | Description                                         |
| ---------------------------- | ------------- | --------------------------------------------------- |
| `createCampaign()`           | Write         | Creates a new campaign document                     |
| `updateCampaign()`           | Write         | Updates an existing campaign                        |
| `deleteCampaign()`           | Write         | Deletes a campaign document                         |
| `getAllCampaigns()`          | Read (stream) | Real-time listener — emits updates automatically    |
| `getCampaignsByDepartment()` | Read (stream) | Filtered real-time stream for a specific department |
| `getCampaigns()`             | Read (paged)  | Paginated one-time fetch                            |
| `saveDetection()`            | Write         | Records a phishing click event                      |
| `getAllDetections()`         | Read          | Fetches all detections (admin only)                 |
| `getUserProfile()`           | Read          | Fetches a single user document by UID               |
| `updateUserProfile()`        | Write         | Partial update of user fields                       |

### Real-time Streaming

The `getAllCampaigns()` and `getCampaignsByDepartment()` methods return a **Kotlin Flow**
— a continuous stream of data. When Firestore data changes, the Flow automatically emits
the new data, and the UI re-renders without any manual refresh.

```
Firestore change → Flow emits → Activity receives → Adapter updates → List re-renders
```

---

## 7. View Layer

### Activities (Screens)

Each screen is an `AppCompatActivity` — the Android base class for a screen. Each:

1. Inflates its XML layout to render the UI.
2. Sets up event listeners (button clicks, etc.).
3. Calls the Repository or AuthManager for data.
4. Updates the UI based on the result.

### ViewBinding

Instead of calling `findViewById()` for every UI element, the app uses **View Binding**.
Each XML layout automatically generates a typed binding class:

```kotlin
binding = ActivityLoginBinding.inflate(layoutInflater)
setContentView(binding.root)

// Then access views directly:
binding.btnLogin.setOnClickListener { ... }
binding.etEmail.text
```

This is type-safe — the compiler catches typos in view names.

### RecyclerView + Adapters

Lists are rendered with `RecyclerView`, which only renders the visible items (efficient
for long lists). An **Adapter** is the bridge between the data list and the visual rows:

```
List<Campaign> ──► CampaignAdapter ──► RecyclerView (on screen)
```

Each Adapter has three responsibilities:

1. Create a view holder (the visual template for one row).
2. Bind data to a specific row (fill in title, description, etc.).
3. Use `DiffUtil` to efficiently update only the rows that changed.

---

## 8. Async / Threading

All network and database calls are **asynchronous** — they run on a background thread
so they don't freeze the UI. The app uses **Kotlin Coroutines**:

```
Activity (main thread)
    │
    └── lifecycleScope.launch { ... }    ← Opens a coroutine (background work block)
            │
            ├── repository.createCampaign(...)  ← runs on background thread, suspends
            │   (await)
            └── Update UI ← back on main thread when done
```

`repeatOnLifecycle(Lifecycle.State.STARTED)` ensures that real-time listeners are
automatically paused when the app goes to the background and resumed when the app
comes back — saving battery and preventing memory leaks.

---

## 9. Permissions

The `AndroidManifest.xml` declares three permissions:

| Permission               | Why                                                          |
| ------------------------ | ------------------------------------------------------------ |
| `INTERNET`               | To communicate with Firebase and open web links              |
| `ACCESS_FINE_LOCATION`   | To get precise GPS coordinates when a user clicks a campaign |
| `ACCESS_COARSE_LOCATION` | Fallback for approximate location                            |

Location permission is requested **at runtime** (when the user first taps "View Details"),
not at install time. This is required by Android for permissions that access sensitive data.

---

## 10. Security Rules (Firestore)

Access to the database is controlled by Firestore Security Rules (configured in Firebase Console):

| Collection   | Who can read                       | Who can write                       |
| ------------ | ---------------------------------- | ----------------------------------- |
| `Users`      | Authenticated users (own document) | Own document only                   |
| `Campaigns`  | Any authenticated user             | Admin role only                     |
| `Detections` | Admin role only                    | Any authenticated user (can create) |

This means even if someone reverse-engineers the APK, they cannot create campaigns or
read other users' detections without an Admin role in the database.

---

## 11. External Libraries

| Library                           | Purpose                                                      |
| --------------------------------- | ------------------------------------------------------------ |
| **Firebase Auth**                 | User login / session management                              |
| **Firebase Firestore**            | Cloud NoSQL database + real-time sync                        |
| **MPAndroidChart**                | Bar charts in the Statistics screen                          |
| **Google Maps SDK**               | Interactive map with detection pin markers                   |
| **Google Play Services Location** | GPS/location access via `FusedLocationProviderClient`        |
| **Material Design 3**             | Google's UI component system (buttons, cards, dialogs, etc.) |
| **Kotlin Coroutines**             | Async programming without callbacks                          |

---

## 12. Component Interaction Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                         APP FLOW                                │
│                                                                 │
│  LoginActivity                                                  │
│    └── AuthManager.loginUser()                                  │
│          └── Firebase Auth → Firestore (get Role)              │
│                └── "Admin"  → AdminMainActivity                 │
│                └── "Viewer" → UserMainActivity                  │
│                                                                 │
│  AdminMainActivity                                              │
│    └── FirebaseRepository.getAllCampaigns()  [real-time]        │
│    └── FirebaseRepository.createCampaign()                      │
│    └── FirebaseRepository.updateCampaign()                      │
│    └── FirebaseRepository.deleteCampaign()                      │
│    └── → StatisticsActivity                                     │
│              └── FirebaseRepository.getCampaigns()              │
│              └── FirebaseRepository.getAllDetections()           │
│              └── MPAndroidChart → renders bar chart             │
│              └── Google Maps → renders detection pins           │
│                                                                 │
│  UserMainActivity                                               │
│    └── FirebaseRepository.getUserProfile()  (get department)    │
│    └── FirebaseRepository.getCampaignsByDepartment() [stream]   │
│    └── [tap "View Details"]                                     │
│          └── GPS permission request                             │
│          └── FusedLocationProviderClient.getCurrentLocation()   │
│          └── FirebaseRepository.saveDetection()                 │
│          └── → CaughtActivity → browser opens phishing URL      │
└─────────────────────────────────────────────────────────────────┘
```
