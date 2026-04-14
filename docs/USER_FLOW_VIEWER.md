# User Flow — Viewer (Regular Employee)

A "Viewer" is a regular employee of the organization. They do not have any management
powers. They simply receive phishing simulation campaigns and interact with them —
unknowingly or knowingly — as part of security awareness training.

---

## Entry Point

The app always opens at the **Login screen** (`LoginActivity`).

---

## Full Flow

### 1. Launch the App

- The app starts on the **Login screen**.
- If the user already has an active session (was logged in before and did not sign out),
  the app skips the login form and goes directly to the dashboard.

---

### 2. Registration (First Time Only)

If the user does not have an account:

1. Tap **"Don't have an account? Register"** on the login screen.
2. The **Register screen** (`RegisterActivity`) opens.
3. Fill in:
   - **Full Name**
   - **Email address**
   - **Department** (dropdown: IT / HR / Finance / Marketing / Sales / Operations / Engineering)
   - **Password** + **Confirm Password**
4. Tap **Register**.
5. The app creates a Firebase Auth account and saves the user profile to the database
   with role = `"Viewer"` (default for all new users).
6. On success, the app shows a toast: _"Account created! Please log in."_
   and returns to the Login screen.

> **Note:** The role `Viewer` is set automatically. A user cannot self-promote to Admin.

---

### 3. Login

1. Enter **email** and **password**.
2. Tap **Login**.
3. The app authenticates with Firebase and reads the `Role` field from the database.
4. Since the role is `"Viewer"`, the app navigates to the **User Dashboard** (`UserMainActivity`).

---

### 4. User Dashboard

This is the main screen for a Viewer.

**What they see:**

- A scrollable list of **phishing campaigns** assigned to their department (or campaigns
  marked as "All" departments).
- Each campaign card shows:
  - **Title** (e.g., "IT Security Update Required")
  - **Description** — the body of the fake phishing email
  - A **"View Details"** button

**Behind the scenes:**

- The app fetches the user's department from the database first.
- Then it loads only campaigns that match the user's department OR are set to "All".
- The list updates in real time — if an admin creates a new campaign while the user
  has the app open, it appears instantly without a manual refresh.

---

### 5. Clicking "View Details" (The Simulation Trigger)

This is the core interaction — the moment the employee "falls" for the phishing simulation.

**Step-by-step:**

1. User taps **"View Details"** on a campaign card.
2. The app checks whether **location permission** has been granted:
   - **If not granted:** A system dialog pops up asking:
     _"Allow this app to access your location?"_
     - If **Allow**: Proceed with location capture.
     - If **Deny**: The app records the detection with `(0, 0)` coordinates and still
       proceeds.
3. The app captures the device's **GPS coordinates** (latitude & longitude) using
   Google's location services.
4. A **detection record** is saved to the database with:
   - The campaign ID
   - The user's ID
   - The GPS location
   - The current timestamp
5. The **"You Got Caught"** screen (`CaughtActivity`) appears — this reveals to the
   employee that they just clicked a simulated phishing link.
6. The screen has two buttons:
   - **"Continue"** — opens the actual fake phishing landing page in the phone's browser
     (hosted at `https://mobile-final-project-483a5.web.app`).
   - **"Back"** — returns to the dashboard without opening the browser.

> **The detection is recorded regardless of whether the user taps "Continue" or "Back".**
> The act of tapping "View Details" is what triggers recording.

---

### 6. Profile Screen

The user can view and edit their profile at any time:

1. Tap the **Profile** button in the toolbar (top of the screen).
2. The **Profile screen** (`ProfileActivity`) opens.
3. The user can update their:
   - **Name**
   - **Department**
4. Tap **Save** to write changes to the database.
5. The **Email** and **Role** fields are shown as read-only.

> Changing the department affects which campaigns the user sees going forward.

---

### 7. Sign Out

1. Tap the **Sign Out** button in the toolbar.
2. The Firebase session is cleared.
3. The app navigates back to the **Login screen** and clears the navigation history
   (so pressing "Back" does not return to the dashboard).

---

## State Diagram

```
[App Launch]
     │
     ▼
[Already logged in?] ── Yes ──► [User Dashboard]
     │
    No
     │
     ▼
[Login Screen]
     │
     ├── "Register" ──► [Register Screen] ──► [Login Screen]
     │
     └── Login success ──► [User Dashboard]
                                │
                ┌───────────────┼───────────────┐
                │               │               │
                ▼               ▼               ▼
         [View Campaign]   [Profile]      [Sign Out]
                │                              │
                ▼                              ▼
       [Location Permission]           [Login Screen]
                │
                ▼
       [Detection Saved to DB]
                │
                ▼
       [CaughtActivity Screen]
                │
       ┌────────┴────────┐
       ▼                 ▼
  [Open Browser]    [Back to Dashboard]
```

---

## Key Constraints

| Constraint                | Detail                                                                                                        |
| ------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Campaigns shown           | Only campaigns targeting the user's department, or "All" departments                                          |
| Detection trigger         | Tapping "View Details" — not just viewing the campaign card                                                   |
| Location fallback         | If GPS permission is denied, coordinates are saved as (0.0, 0.0)                                              |
| Role                      | Always `Viewer` — cannot be changed from within the app                                                       |
| Already-clicked indicator | Campaigns the user has already clicked are visually marked (tracked in memory, not persisted across sessions) |
