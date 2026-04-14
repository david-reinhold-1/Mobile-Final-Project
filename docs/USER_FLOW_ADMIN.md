# User Flow — Admin

An "Admin" is a system administrator or security team member. They control the entire
simulation platform: creating phishing campaigns, managing them, and viewing analytics
on how employees interacted with them.

---

## Entry Point

The app always opens at the **Login screen** (`LoginActivity`).

---

## How to Become an Admin

Admin accounts are **not** created through the app's registration screen. The registration
screen always creates a regular `Viewer` account. To make someone an admin:

1. Register normally to create an account.
2. Open the **Firebase Console** (the cloud database web interface).
3. Navigate to the `Users` collection.
4. Find the user's document.
5. Manually change the `Role` field from `"Viewer"` to `"Admin"`.

> There is no in-app promotion mechanism — this is intentional for security.

---

## Full Flow

### 1. Launch the App

- The app starts on the **Login screen**.
- If the admin already has an active session, the app skips the login form and goes
  directly to the Admin Dashboard.

---

### 2. Login

1. Enter **email** and **password**.
2. Tap **Login**.
3. The app authenticates with Firebase and reads the `Role` field from the database.
4. Since the role is `"Admin"`, the app navigates to the **Admin Dashboard** (`AdminMainActivity`).

---

### 3. Admin Dashboard

This is the command center for the admin.

**What they see:**

- A scrollable list of **all campaigns** in the system (not filtered by department).
- Each campaign card shows:
  - **Title**
  - **Description** (email body)
  - **Landing page URL** (the fake phishing website)
  - **Department** it targets
  - **Created by** (admin email)
  - **Creation date**
  - An **Edit** button (pencil icon)
  - A **Delete** button (trash icon)
- A **floating action button** (`+`) in the bottom-right corner to create a new campaign.
- Toolbar buttons: **Statistics** and **Sign Out**.

**Behind the scenes:**

- The dashboard uses a real-time Firestore listener — any campaign added, edited, or
  deleted by any admin updates the list instantly for all admins.

---

### 4. Create a New Campaign

1. Tap the **`+` (FAB) button** on the Admin Dashboard.
2. A dialog popup appears with the form:
   - **Title** — the name/subject of the phishing campaign (e.g., "Your password will expire")
   - **Email Body** — the description/body text users will see
   - **Phishing URL** — the fake landing page link (e.g., `https://mobile-final-project-483a5.web.app`)
   - **Department** — dropdown to target a specific department, or "All"
3. Tap **Create**.
4. The campaign is saved to the `Campaigns` collection in Firestore.
5. The dialog closes and the new campaign appears immediately in the list.

**Validation:**

- Title and URL are required fields.
- If the URL does not look like a valid URL, the field shows an error.

---

### 5. Edit an Existing Campaign

1. Tap the **Edit** button on a campaign card.
2. The same dialog popup opens, pre-filled with the existing campaign's data.
3. Modify any fields as needed.
4. Tap **Update**.
5. The changes are written to Firestore and reflected in the list immediately.

---

### 6. Delete a Campaign

1. Tap the **Delete** button on a campaign card.
2. A **confirmation dialog** appears: _"Are you sure you want to delete this campaign?"_
3. Tap **Delete** to confirm, or **Cancel** to abort.
4. On confirmation, the campaign document is removed from Firestore.
5. The campaign disappears from the list immediately.

> **Note:** Deleting a campaign does NOT delete the detection records associated with it.
> Those remain in the `Detections` collection and are still visible in Statistics.

---

### 7. Statistics Screen

Accessible by tapping **Statistics** in the toolbar.

**What the admin sees:**

#### Summary Cards

- **Total Campaigns** — the total number of campaigns in the system
- **Total Detections** — the total number of times employees clicked a phishing link

#### Bar Chart

- An interactive bar chart showing the **top 10 campaigns** by detection count.
- The X-axis shows campaign titles (truncated if long).
- The Y-axis shows number of detections.
- Bars are animated on load.

#### Google Map

- A map showing **pins at each GPS location** where a detection was recorded.
- Different colored pins for different campaigns.
- The map auto-zooms to fit all detection pins.
- Tapping a pin shows the campaign name and detection timestamp.

#### Recent Detections List

- A scrollable list of all detections ordered by most recent first.
- Each item shows:
  - The **user's email** who clicked
  - The **campaign title** they clicked
  - The **relative timestamp** (e.g., "2 hours ago", "3 days ago")

**Behind the scenes:**

- The screen fetches all campaigns and all detections in parallel (simultaneously) for speed.
- It then joins the two datasets in memory to display the enriched list.

---

### 8. Back Navigation from Statistics

- Tap the **Back** button in the toolbar (top-left) to return to the Admin Dashboard.
- The Android system back button also works.

---

### 9. Sign Out

1. Tap the **Sign Out** button in the Admin Dashboard toolbar.
2. The Firebase session is cleared.
3. The app navigates to the **Login screen** and clears the navigation history.

---

## State Diagram

```
[App Launch]
     │
     ▼
[Already logged in?] ── Yes ──► [Admin Dashboard]
     │
    No
     │
     ▼
[Login Screen]
     │
     └── Login success (role = Admin) ──► [Admin Dashboard]
                                               │
                         ┌─────────────────────┼────────────────────┐
                         │                     │                    │
                         ▼                     ▼                    ▼
                  [Create Campaign]    [Statistics Screen]     [Sign Out]
                         │                     │                    │
                  [Edit Campaign]       [Bar Chart +           [Login Screen]
                         │              Google Map +
                  [Delete Campaign]     Detection List]
                  (with confirmation)
```

---

## Campaign Lifecycle

```
Admin creates campaign
        │
        ▼
Campaign appears in Admin Dashboard list
        │
        ▼
Employees (Viewers) see it in their User Dashboard
        │
        ▼
Employee clicks "View Details"
        │
        ▼
Detection record created in Firestore (with GPS + timestamp)
        │
        ▼
Admin sees detection in Statistics screen (bar chart + map + list)
        │
        ▼
Admin can edit or delete the campaign at any time
```

---

## Admin Capabilities Summary

| Action                        | Where                                                       |
| ----------------------------- | ----------------------------------------------------------- |
| View all campaigns            | Admin Dashboard                                             |
| Create new campaign           | Admin Dashboard → `+` button                                |
| Edit a campaign               | Admin Dashboard → Edit button on card                       |
| Delete a campaign             | Admin Dashboard → Delete button on card (with confirmation) |
| View total campaign count     | Statistics screen                                           |
| View total detection count    | Statistics screen                                           |
| View detection bar chart      | Statistics screen                                           |
| View detection map (GPS pins) | Statistics screen                                           |
| View recent detection list    | Statistics screen                                           |
| Sign out                      | Admin Dashboard toolbar                                     |
