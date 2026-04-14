# Project Explainer — Key Concepts & Insights

This document is designed to help you explain this project to your teacher,
even if you did not write it yourself. It covers the "why" behind every major
technology and design decision in plain language.

---

## What Does This App Do? (The Elevator Pitch)

This is a **security training tool for companies**. Security teams know that employees
are the weakest link in cybersecurity — people fall for fake emails all the time.
This app lets a security admin create fake phishing emails (called "campaigns"), push
them to employees' phones, and record who clicked the link and where they were
geographically when they did it. The goal is awareness: employees learn to be more
careful, and the security team gets data on vulnerability.

---

## The Technologies — Explained Simply

### Kotlin

- Kotlin is the **programming language** the entire app is written in.
- It replaced Java as Google's preferred language for Android in 2017.
- Think of it as a more concise, safer version of Java.
- Key feature used here: **Coroutines** — Kotlin's way of doing background tasks
  (like network calls) without freezing the user interface.
- **Why Kotlin over Java?** Less code, fewer bugs, better Android integration.

Here is a real example from `Models.kt`. A **data class** is Kotlin's way of defining
a plain object that holds data — one line replaces dozens of lines in Java:

```kotlin
// A Campaign object — all fields, typed, with default values
data class Campaign(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var landingPageUrl: String = "",
    var department: String = "All",
    var createdBy: String = "",
    var createdAt: Timestamp = Timestamp.now()
)
```

Kotlin also makes null safety explicit. If a variable can be null, it must be declared
with `?`. The `?:` operator (called "Elvis") provides a fallback:

```kotlin
// If getCurrentUser() returns null, uid stays null too
val uid = FirebaseAuth.getInstance().currentUser?.uid

// If snapshot.getString("Role") returns null, use ROLE_VIEWER as the default
val role = snapshot.getString("Role") ?: ROLE_VIEWER
```

### XML (Extensible Markup Language)

- XML files define the **visual layout** of every screen in the app.
- They are like blueprints: "put a button here, a text field there, make this blue."
- Android reads these files and renders them into actual pixels on the screen.
- Every `.xml` file in `res/layout/` corresponds to a screen or a list item template.
- **Why XML and not code?** XML layouts are visual and declarative — you describe
  _what_ the screen looks like, not the step-by-step instructions to draw it.
  Android Studio can also preview XML layouts visually.

Here is a snippet from `item_campaign.xml` — the card layout for one campaign row:

```xml
<!-- A rounded card container for the whole row -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="16dp"
    android:layout_marginVertical="10dp"
    app:cardCornerRadius="24dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <!-- The campaign title text -->
        <TextView
            android:id="@+id/tvCampaignTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textStyle="bold" />

        <!-- The delete button, shown as an icon -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnDelete"
            style="@style/Widget.Material3.Button.IconButton"
            app:icon="@android:drawable/ic_menu_delete"
            app:iconTint="?attr/colorError" />

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

Every XML attribute (`android:layout_width`, `app:cardCornerRadius`, etc.) is a
property that controls one aspect of the visual appearance or behavior.
`match_parent` means "take all available width". `wrap_content` means "just big enough
to fit the content inside."

### Android Activities

- An **Activity** is the Android concept for "one screen."
- Every screen in this app has its own Activity (a Kotlin class) and its own layout (an XML file).
- The Activity contains all the logic for that screen: what happens when you press a button,
  how to fetch data, how to navigate to the next screen.
- Example: `LoginActivity.kt` = the logic. `activity_login.xml` = the visual design.
  They are paired together.

Here is how `LoginActivity` navigates to the correct dashboard after login.
`Intent` is Android's way of saying "open this screen":

```kotlin
// After login, decide which screen to open based on role
private fun navigateByRole(role: String) {
    val destination = if (role == AuthManager.ROLE_ADMIN) {
        AdminMainActivity::class.java   // open this if Admin
    } else {
        UserMainActivity::class.java    // open this if Viewer
    }
    startActivity(Intent(this, destination).apply {
        // Clear the navigation stack — pressing Back will not return to Login
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
}
```

### Firebase Authentication

- Firebase Auth is Google's ready-made **login system**.
- Instead of building a login server from scratch (which would be complex and risky),
  the app uses Firebase, which handles passwords, sessions, and security.
- When a user logs in, Firebase gives back a unique ID (called a **UID**) that
  identifies that user permanently.
- The app also stores a `Role` field alongside the UID in the database to know if
  this person is an Admin or a Viewer.

### Firestore (Cloud Database)

- Firestore is a **cloud NoSQL database** — it stores data as documents (like JSON files),
  not as traditional rows and columns.
- Data is organized into **Collections** (like folders) containing **Documents** (like files).
- The app has three collections: `Users`, `Campaigns`, `Detections`.
- **Key feature: real-time listeners.** When an admin creates a campaign, all Viewer
  phones that are open get the update _immediately_ without refreshing — Firestore
  "pushes" the change to them.

### RecyclerView and Adapters

- A `RecyclerView` is the Android component for showing a **scrollable list**.
- It is "recycling" because it only renders the items currently visible on screen —
  if there are 1000 items, it doesn't draw 1000 rows, only the ~10 you can see.
  As you scroll, it recycles off-screen views.
- An **Adapter** connects the data (list of campaigns) to the visual rows.
  Think of it as a translator: "given this Campaign object, draw this card."

Here is `CampaignAdapter.kt` — the full adapter (simplified) showing how a data object
becomes a visible card:

```kotlin
class CampaignAdapter(
    private val onDeleteClick: (Campaign) -> Unit,  // callback: "tell me when Delete is tapped"
    private val onEditClick: (Campaign) -> Unit
) : ListAdapter<Campaign, CampaignAdapter.CampaignViewHolder>(DiffCallback) {

    // Called when a new empty row view needs to be created (on first load or scroll)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemCampaignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CampaignViewHolder(binding)
    }

    // Called to fill a row with real data at a given position
    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CampaignViewHolder(private val binding: ItemCampaignBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: Campaign) {
            binding.tvCampaignTitle.text = campaign.title       // fill in the title TextView
            binding.tvCampaignBody.text = campaign.description  // fill in the description
            binding.btnDelete.setOnClickListener { onDeleteClick(campaign) } // wire up Delete
            binding.btnEdit.setOnClickListener { onEditClick(campaign) }     // wire up Edit
        }
    }

    // DiffUtil: compare two Campaign objects — if the ID matches, it's the same item
    private companion object DiffCallback : DiffUtil.ItemCallback<Campaign>() {
        override fun areItemsTheSame(old: Campaign, new: Campaign) = old.id == new.id
        override fun areContentsTheSame(old: Campaign, new: Campaign) = old == new
    }
}
```

### ViewBinding

- A code generation feature that makes accessing XML views from Kotlin easy and safe.
- Without it, you would write `val button = findViewById<Button>(R.id.btnLogin)` for
  every single UI element — verbose and error-prone.
- With ViewBinding, you just write `binding.btnLogin` — shorter and the compiler
  catches typos at build time.

Here is how every Activity starts up — inflate the XML into the real view, then use it:

```kotlin
class LoginActivity : AppCompatActivity() {

    // Declares the binding — gives us typed access to every view in activity_login.xml
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // "Inflate" = read the XML and build the real view objects from it
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)   // attach it to the screen

        // Now we can access any view by name — no casting, no typos
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
```

### Kotlin Coroutines and Flow

- **Coroutines** are Kotlin's way of running tasks in the background without blocking
  the main thread (which would make the UI freeze).
- Any network call or database operation in this app is inside a coroutine:
  it runs in the background, and when done, reports back to the UI.
- **Flow** is like a coroutine that can emit multiple values over time — perfect for
  real-time database listeners that keep sending updates.
- `lifecycleScope.launch` means "run this background task for as long as this screen
  is alive, then cancel it automatically."

Here is the real code from `AdminMainActivity` that listens to all campaigns in real time:

```kotlin
private fun observeCampaigns() {
    setLoading(true)
    lifecycleScope.launch {                          // start a background coroutine
        repeatOnLifecycle(Lifecycle.State.STARTED) { // auto-pause when app goes to background
            repository.getAllCampaigns().collect { result ->  // receive each database update
                setLoading(false)
                when (result) {
                    is Result.Success -> {
                        adapter.submitList(result.data)    // update the visible list
                        binding.tvEmpty.visibility =
                            if (result.data.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is Result.Failure -> {
                        Toast.makeText(this@AdminMainActivity,
                            "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
```

And here is the detection flow in `UserMainActivity` — saving a phishing click event
to the database:

```kotlin
private suspend fun saveDetection(location: GeoPoint) {
    val campaign = selectedCampaign ?: return  // bail out if no campaign was selected
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    // Build the detection record
    val detection = Detection(
        campaignId = campaign.id,
        userId = userId,
        location = location,        // GPS coordinates captured just before this call
        timestamp = Timestamp.now()
    )

    // Save it and handle both outcomes
    when (val result = repository.saveDetection(detection)) {
        is Result.Success -> {
            clickedCampaigns.add(campaign.id)  // mark locally so the UI reflects the click
            openPhishingLink()                 // now open the fake page in the browser
        }
        is Result.Failure -> {
            Toast.makeText(this, "Detection failed to save", Toast.LENGTH_LONG).show()
        }
    }
}
```

### MVP & Architecture Pattern

- The app uses a pattern called the **Repository Pattern**:
  - Screens (Activities) only know about high-level operations: "load campaigns," "save detection."
  - `FirebaseRepository` knows _how_ to talk to Firebase — the screens don't.
  - This separation means if Firebase were ever replaced with a different database,
    only the repository file would need to change — not every screen.
- `Result<T>` is a wrapper returned by every repository function. It is either
  `Success(data)` or `Failure(exception)` — screens handle both cases cleanly.

Here is the `Result` type definition and how the repository uses it, from
`FirebaseRepository.kt`:

```kotlin
// A sealed class is a type that can only be one of a fixed set of subtypes
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()    // worked — here is the data
    data class Failure(val exception: Exception) : Result<Nothing>() // something went wrong
}

// Example: saving a new campaign
suspend fun createCampaign(campaign: Campaign): Result<String> {
    return try {
        val docRef = campaignsCol.document()            // get a new auto-ID document ref
        val campaignWithId = campaign.copy(id = docRef.id)  // save the ID back on the object
        docRef.set(campaignWithId).await()              // write to Firestore, wait for it
        Result.Success(docRef.id)                       // return the new ID on success
    } catch (e: Exception) {
        Result.Failure(e)                               // anything goes wrong → Failure
    }
}
```

---

## Key Design Decisions — The "Why"

### Why Firebase instead of a custom server?

Building a backend server (REST API, database, auth system) is a major undertaking for
a mobile project. Firebase provides all of it — authentication, real-time database, hosting
— as a managed service. This lets the project focus on the mobile client, not infrastructure.

### Why two separate dashboards (AdminMainActivity vs UserMainActivity)?

Role separation is fundamental to this app's security model. If a Viewer somehow got
access to the admin dashboard, they could delete campaigns or see who else was tracked.
By routing based on role immediately after login, admin-only screens are never accessible
to regular users.

### Why does the detection get saved before the user sees the "Caught" screen?

The moment a user taps "View Details," the intent to interact with the phishing campaign
is clear. The detection is saved _before_ opening the browser because the user might
close the browser immediately — but the click still happened. This mirrors real phishing
attacks where the damage (credential theft) happens at the moment of clicking, not later.

### Why GPS location permission is requested at runtime (not at install)?

Android 6.0+ requires that apps ask for "dangerous" permissions (like location and camera)
at the moment they are first needed, not at app install. This gives users more control.
The app gracefully handles denial — it still records the detection, just with
coordinates `(0.0, 0.0)` since no location was available.

### Why is department filtering important?

In a real company, phishing campaigns are often targeted. An IT department might receive
a fake "VPN credentials expired" email, while HR receives a fake "employment contract"
email. The department field on campaigns allows the admin to target specific groups,
making the simulation more realistic.

### Why use ListAdapter with DiffUtil instead of a plain list?

`ListAdapter` + `DiffUtil` is the modern, efficient way to update RecyclerView lists.
`DiffUtil` compares the old list vs the new list and calculates the minimal set of changes
(which items were added, removed, or changed). This produces smooth animations and avoids
re-rendering unchanged items — important for real-time lists that update frequently.

---

## The Data Flow in One Sentence Per Feature

| Feature          | What happens end-to-end                                                                                                              |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Login            | User types email/password → Firebase validates → app reads Role from Firestore → routes to the right screen                          |
| Create Campaign  | Admin fills form → app writes document to Firestore `Campaigns` collection → real-time listener fires → all open screens update      |
| Click Simulation | Viewer taps "View Details" → GPS captured → `Detection` document written to Firestore → "Caught" screen shown → URL opens in browser |
| Statistics       | Activity fetches campaigns + detections in parallel → joins data in memory → renders bar chart + map pins + detection list           |
| Profile Edit     | User changes name/department → app writes only the changed fields to Firestore `Users` document                                      |

---

## How to Explain the Codebase to Your Teacher

### The big picture

> "The app is split into three layers. The screens (Activities) handle what the user sees
> and does. The Repository handles all communication with the cloud database. The data
> models define what a User, Campaign, and Detection look like. These layers only talk
> to each other, never directly to Firebase."

### On Kotlin

> "Kotlin is a modern programming language for Android. Its biggest advantage here is
> Coroutines, which let us call Firebase (network operations) without freezing the app.
> Instead of callbacks — 'when this finishes, call this function' — we write code that
> reads top-to-bottom, like synchronous code."

### On XML

> "XML files describe the visual structure of each screen. They work like HTML for a
> website — elements, positions, sizes, colors. The Kotlin code binds to these elements
> at runtime using ViewBinding, which auto-generates a typed class from each XML file."

### On Firebase

> "Firebase is our entire backend. Firebase Auth handles login and identity. Firestore
> is the database — three collections: Users, Campaigns, Detections. The app uses
> real-time Firestore listeners, so whenever an admin writes data, all connected devices
> receive the update instantly through Kotlin Flow."

### On RecyclerView

> "RecyclerView is the Android component for scrollable lists. It is paired with an
> Adapter, which knows how to convert a Campaign data object into a visible card on
> screen. DiffUtil makes updates efficient by only re-rendering rows that actually changed."

### On security

> "Role-based access is enforced in two places: in the app (routing to different screens
> based on the Role field) and in Firestore security rules on the server (Admin-only
> collections reject writes from Viewers even at the database level)."

---

## Quick Reference: File → Responsibility

| File                     | One-sentence purpose                                                       |
| ------------------------ | -------------------------------------------------------------------------- |
| `LoginActivity.kt`       | Login form + role-based navigation after auth                              |
| `RegisterActivity.kt`    | New account form, always creates a Viewer role                             |
| `AdminMainActivity.kt`   | Admin dashboard: list + create/edit/delete campaigns                       |
| `UserMainActivity.kt`    | Viewer dashboard: list campaigns filtered by department, trigger detection |
| `StatisticsActivity.kt`  | Analytics screen: bar chart + Google Map + detection history               |
| `ProfileActivity.kt`     | View and update own name and department                                    |
| `CaughtActivity.kt`      | "You got phished!" reveal screen after clicking a campaign                 |
| `AuthManager.kt`         | Wraps Firebase Auth + role fetching into clean login/register functions    |
| `FirebaseRepository.kt`  | All Firestore reads/writes behind a clean `Result<T>` interface            |
| `Models.kt`              | Data classes: User, Campaign, Detection                                    |
| `CampaignAdapter.kt`     | Renders campaign cards in the admin list                                   |
| `UserCampaignAdapter.kt` | Renders campaign cards in the user list                                    |
| `DetectionAdapter.kt`    | Renders detection rows in the statistics list                              |
| `AndroidManifest.xml`    | Declares all screens and OS permissions to the Android system              |
| `activity_*.xml`         | Visual layout for each screen                                              |
| `item_*.xml`             | Visual layout for each list row type                                       |
| `google-services.json`   | Firebase project credentials (auto-generated, not written by hand)         |
