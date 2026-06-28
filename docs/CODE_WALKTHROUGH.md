# KMP User Management — Complete Code Walkthrough

> **Audience:** Portfolio reviewers and engineers — understand every file, class, function, and execution path without opening the repo.
>
> **Repo:** [https://github.com/kanav22/kmp-user-management](https://github.com/kanav22/kmp-user-management)

---

## Table of Contents

1. [Root & Build Configuration](#1-root--build-configuration)
2. [Domain Layer](#2-domain-layer)
3. [Data Layer](#3-data-layer)
4. [Presentation Layer (MVI)](#4-presentation-layer-mvi)
5. [UI Layer (Compose)](#5-ui-layer-compose)
6. [Dependency Injection & Config](#6-dependency-injection--config)
7. [Platform Entry Points](#7-platform-entry-points)
8. [iOS Shell](#8-ios-shell)
9. [Tests](#9-tests)
10. [CI & Release](#10-ci--release)
11. [Overall Execution Walkthrough](#11-overall-execution-walkthrough)
12. [Dependency Graph](#12-dependency-graph)
13. [Call Flows](#13-call-flows)
14. [Architecture Validation](#14-architecture-validation)
15. [Learning Roadmap](#15-learning-roadmap)

---

# 1. Root & Build Configuration

---

## `settings.gradle.kts`

### Purpose

Gradle project bootstrap. Declares the root project name and which modules exist. This is a **single-module KMP** project — only `:composeApp` is included.

### Role in the Architecture

**Configuration** — not runtime code; defines module graph for Gradle.

### Dependencies

None at runtime. References `:composeApp` module.

### Key content

- `rootProject.name = "KmpUserManagement"`
- `include(":composeApp")`
- `pluginManagement` + `dependencyResolutionManagement` repositories (Google, Maven Central, Gradle Plugin Portal)

### Design Decisions

Single module keeps 1-day project scope manageable. Production would split `:core`, `:feature-users`, `:app`.

### Interview Questions

- **Q:** Why one module? **A:** Challenge timeline; all layers coexist in `composeApp` source sets.
- **Q:** Where is iOS app? **A:** Separate `iosApp/` Xcode project, not a Gradle module.

### File Summary

Entry point for Gradle. Everything builds through `:composeApp`.

---

## `build.gradle.kts` (root)

### Purpose

Declares plugins at root with `apply false` — versions come from `libs.versions.toml`, applied in `composeApp/build.gradle.kts`.

### Role

**Configuration**

### Plugins declared

`kotlin.multiplatform`, `kotlin.serialization`, `compose.multiplatform`, `compose.compiler`, `android.application`, `sqldelight`

### Design Decisions

Standard Gradle version catalog pattern — centralize plugin versions, apply per-module.

---

## `gradle/libs.versions.toml`

### Purpose

Version catalog — single source of truth for dependency and plugin versions.

### Role

**Configuration**

### Critical versions


| Library            | Version | Why pinned                            |
| ------------------ | ------- | ------------------------------------- |
| Kotlin             | 2.1.21  | Compose/Koin compatibility            |
| Koin               | 4.1.1   | 4.2.1 breaks iOS KLIB ABI             |
| AGP                | 8.10.1  | AGP 9 needs Kotlin 2.2+               |
| material3-adaptive | 1.2.0   | Includes separate navigation artifact |
| SQLDelight         | 2.3.2   | KMP drivers                           |
| Ktor               | 3.1.3   | KMP client                            |


### Design Decisions

Explicit version refs on Koin artifacts (not BOM) after Gradle sync removed implicit BOM.

---

## `composeApp/build.gradle.kts`

### Purpose

**The main build file.** Configures KMP targets, dependencies, Android app, SQLDelight codegen, release shrinking.

### Role

**Configuration** — defines entire compile graph.

### Kotlin targets


| Target                                    | Output                        |
| ----------------------------------------- | ----------------------------- |
| `androidTarget`                           | Android APK                   |
| `iosX64`, `iosArm64`, `iosSimulatorArm64` | Static framework `ComposeApp` |
| `jvm`                                     | Desktop compile + JVM tests   |


### Android block highlights

- `minSdk 26`, `compileSdk 35`
- `GOREST_TOKEN` from `local.properties` → `BuildConfig.GOREST_TOKEN`
- Release: `isMinifyEnabled = true`, ProGuard rules

### SQLDelight block

```kotlin
sqldelight {
    databases {
        create("UserDatabase") {
            packageName.set("com.kanav.usermanager.data.local.db")
        }
    }
}
```

Generates `UserDatabase`, query classes from `User.sq`.

### Execution Flow

Gradle sync → SQLDelight codegen → compile commonMain → compile platform source sets → link iOS framework / package APK.

### Design Decisions

- **Static iOS framework** — simpler Xcode integration
- **jvm() target** — enables `jvmTest` with real SQLite
- **compose.desktop.currentOs** on jvmMain — compiles Compose for JVM tests

### Improvements

Add `compileSdk` alignment checks in CI; verify release build with R8.

---

## `gradle.properties`

### Purpose

Gradle JVM settings (heap, parallel, Android flags). Standard KMP project properties.

### Role

**Configuration**

---

## `composeApp/proguard-rules.pro`

### Purpose

R8/ProGuard keep rules for release Android builds.

### Role

**Configuration / Security**

### Keeps

- kotlinx.serialization generated serializers
- Ktor, SQLDelight, Koin, Compose, OkHttp classes
- Line numbers for crash reports

### Design Decisions

Without these, R8 would strip reflection-used serialization classes → runtime crashes.

---

## `.github/workflows/ci.yml`

### Purpose

GitHub Actions CI on push/PR.

### Role

**Configuration**

### Steps

1. JDK 17
2. Create `local.properties` with `GOREST_TOKEN` secret
3. `./gradlew :composeApp:jvmTest`
4. `./gradlew :composeApp:testDebugUnitTest`
5. `./gradlew :composeApp:assembleDebug`
6. Upload APK artifact

---

# 2. Domain Layer

---

## `domain/model/User.kt`

### Purpose

Core domain entity — platform-agnostic user model used everywhere (UI, DB, API mapping).

### Role in Architecture

**Domain** — innermost layer; no dependencies on framework or IO.

### Classes

#### `data class User`


| Property  | Type         | Why                                |
| --------- | ------------ | ---------------------------------- |
| `id`      | Long         | GoRest primary key                 |
| `name`    | String       | Display                            |
| `email`   | String       | Display + unique constraint on API |
| `gender`  | `Gender`     | Typed enum, not raw string         |
| `status`  | `UserStatus` | Active/inactive chip               |
| `addedAt` | `Instant`    | Relative timestamp source          |


#### `enum class Gender` / `UserStatus`

- `apiValue` property returns lowercase string for REST API
- Avoids scattering string literals in POST body

### Design Decisions

- **data class** — immutable value semantics, copy for MVI state
- **Instant not String** — type safety in domain; persistence layer serializes
- **Enums** — exhaustive when/switch in UI

### Interview Questions

- **Beginner:** What is `addedAt`? **A:** Local first-seen timestamp, not from API.
- **Advanced:** Why enums with `apiValue`? **A:** Domain types at rest; API boundary converts.

### File Summary

Depends on: `kotlinx-datetime`. Used by: all layers.

---

## `domain/repository/UserRepository.kt`

### Purpose

Repository **port** — defines data operations the domain needs without knowing Ktor or SQLDelight.

### Role

**Domain** (interface) — implemented in **Data** layer.

### Interface methods


| Method              | Returns            | Semantics                   |
| ------------------- | ------------------ | --------------------------- |
| `observeUsers()`    | `Flow<List<User>>` | Reactive cache read         |
| `refreshLastPage()` | `Result<Unit>`     | Network fetch → cache write |
| `addUser(...)`      | `Result<User>`     | POST → cache write          |
| `deleteUser(id)`    | `Result<Unit>`     | DELETE → cache delete       |


### Error types

- `UserListError` sealed: `NoInternet`, `Generic`, `MissingToken`
- `ValidationException(errors)` — 422 from API
- `ValidationError(field, message)`

### Design Decisions

- **Flow for observe, Result for mutate** — read is stream; writes are one-shot with explicit failure
- **ValidationException in domain package** — pragmatic; purists would put in data layer

### File Summary

Contract between presentation (via use cases) and data implementation.

---

## `domain/usecase/ObserveUsersUseCase.kt`

### Purpose

Wraps `repository.observeUsers()` — ViewModel should not call repository directly.

### Role

**Domain**

### Classes

- `fun interface ObserveUsersUseCase` — `operator fun invoke(): Flow<List<User>>`
- `ObserveUsersUseCaseImpl(repository)` — delegates

### Design Decisions

Thin use case — adds test seam and keeps ViewModel depending on operations not repository.

### Functions

`**invoke()`** — returns cold/hot Flow from SQLDelight via repository chain.

---

## `domain/usecase/GetLastPageUsersUseCase.kt`

### Purpose

Triggers network refresh of last API page into local cache.

### Role

**Domain**

### Functions

`**suspend operator fun invoke(): Result<Unit>`** → `repository.refreshLastPage()`

### Naming note

"GetLastPage" sounds like a read, but implementation is **refresh** (network + write). Interview: explain the name maps to project requirement "fetch last page."

---

## `domain/usecase/AddUserUseCase.kt`

### Purpose

Create user through repository.

### Functions

`**suspend operator fun invoke(name, email, gender, status): Result<User>`**

---

## `domain/usecase/DeleteUserUseCase.kt`

### Purpose

Delete user by ID through repository.

### Functions

`**suspend operator fun invoke(userId: Long): Result<Unit>**`

---

## `domain/util/RelativeTimeFormatter.kt`

### Purpose

Shared relative time strings for UI — challenge requires shared logic.

### Role

**Domain / Utilities**

### Function: `fun Instant.toRelativeString(): String`

**Step-by-step:**

1. `diff = Clock.System.now() - this`
2. If `diff.isNegative()` → `"Just now"` (clock skew guard)
3. Bucket by seconds, minutes, hours, days, weeks
4. Singular/plural: `"1 minute ago"` vs `"5 minutes ago"`

### Edge cases

- Future timestamps → "Just now"
- No months/years bucket (caps at weeks)
- Uses wall clock — drifts if device time changes

### Alternatives

`kotlinx-datetime` format APIs; platform `RelativeDateTimeFormatter` (not KMP).

### Interview Questions

- **Q:** Why not use API created_at? **A:** GoRest doesn't provide it; we use local cache time.

### File Summary

Used by `UserCard`, `UserDetailPane`. Tested by 12 boundary tests.

---

# 3. Data Layer

---

## `sqldelight/.../User.sq`

### Purpose

SQL schema and queries — single source of truth for local cache structure.

### Role

**Database** — generates `UserDatabase`, `UserQueries`, `UserEntity`.

### Schema: `UserEntity`

```sql
id INTEGER PRIMARY KEY
name, email, gender, status, addedAt TEXT NOT NULL
```

### Queries


| Query                       | SQL                   | Purpose                           |
| --------------------------- | --------------------- | --------------------------------- |
| `selectAllOrderedByAddedAt` | ORDER BY addedAt DESC | Newest local inserts first        |
| `insertOrIgnore`            | INSERT OR IGNORE      | Cache without overwriting addedAt |
| `deleteById`                | DELETE WHERE id = ?   | Remove after API delete           |
| `selectById`                | SELECT WHERE id = ?   | JVM test helper                   |


### Design Decisions

**INSERT OR IGNORE** is the most important SQL decision in the project — preserves `addedAt` on refresh. Documented in code comment and tested in `SqlDelightInsertOrIgnoreTest`.

---

## `data/local/UserLocalDataSource.kt`

### Purpose

Kotlin wrapper around SQLDelight — maps `UserEntity` → domain `User`, provides suspend write methods.

### Role

**Data / Database**

### Dependencies

- `UserDatabase` (generated)
- `Dispatchers.Default` for writes

### Classes: `UserLocalDataSource(db: UserDatabase)`

### Functions

#### `observeUsers(): Flow<List<User>>`

1. `db.userQueries.selectAllOrderedByAddedAt()`
2. `.asFlow().mapToList(Dispatchers.Default)`
3. `.map { entities → entities.map { toDomain() } }`

**Why Flow:** UI subscribes once; any insert/delete triggers re-emit.

#### `suspend fun insertOrIgnore(...)`

- `withContext(Dispatchers.Default) { db.userQueries.insertOrIgnore(...) }`
- **Why Default not IO:** `Dispatchers.IO` is JVM-only; unavailable in `commonMain`

#### `suspend fun deleteById(id)`

- Same dispatcher pattern

#### `private fun UserEntity.toDomain(): User`

- Parses `addedAt` string → `Instant.parse`
- Maps gender/status strings → enums (case-insensitive)

### Execution Flow

Repository → UserLocalDataSource → UserQueries → SQLite file `usermanager.db`

### Improvements

Extract string→enum mapping to shared mapper (duplicated in UserDto).

---

## `data/remote/UserDto.kt`

### Purpose

API wire format — kotlinx.serialization models matching GoRest JSON.

### Role

**Data / Network**

### Classes

#### `@Serializable data class UserDto`

Fields match API response. No `addedAt` — supplied at mapping time.

#### `fun UserDto.toDomain(addedAt: Instant): User`

Converts API strings to domain enums.

#### `@Serializable data class CreateUserRequest`

POST body: name, email, gender, status (all strings).

#### `@Serializable data class ApiValidationError`

422 response item: `field`, `message`.

### Design Decisions

DTOs stay in data layer; domain never sees `UserDto`.

---

## `data/remote/UserApiService.kt`

### Purpose

All HTTP calls to GoRest — pagination, create, delete.

### Role

**Data / Network**

### Dependencies

`HttpClient` (Ktor) — auth header set in DI, not here.

### Companion

`USERS_URL = "https://gorest.co.in/public/v2/users"`

### Functions

#### `suspend fun fetchLastPage(): List<UserDto>`

1. `GET USERS_URL?page=1` — probe response
2. Read header `X-Pagination-Pages` → `lastPage` (default 1)
3. If `lastPage == 1`, return probe body (avoid double request)
4. Else `GET USERS_URL?page=lastPage` → return body

**Complexity:** O(2) HTTP requests worst case. **Edge case:** missing header → page 1.

#### `suspend fun createUser(...): User`

1. `POST USERS_URL` with `CreateUserRequest` JSON
2. If status `422 Unprocessable Entity` → parse `List<ApiValidationError>` → throw `ValidationException`
3. Else return `response.body<UserDto>().toDomain(addedAt = Clock.System.now())`

**Note:** Does not explicitly check for 201 — relies on Ktor throwing on other error codes.

#### `suspend fun deleteUser(id: Long)`

1. `DELETE USERS_URL/id`
2. If status NOT 204 and NOT 404 → `response.body<Unit>()` forces exception
3. 404 treated as success (idempotent delete)

### Design Decisions

- **404 on delete = success** — user already gone is OK
- **ValidationException** — typed error for form field mapping

---

## `data/repository/UserRepositoryImpl.kt`

### Purpose

**Orchestrator** — implements offline-first policy: API mutates DB, UI reads DB.

### Role

**Data**

### Dependencies

`UserApiService`, `UserLocalDataSource`

### Functions

#### `observeUsers(): Flow<List<User>>`

Pure delegation to local — no network.

#### `suspend fun refreshLastPage(): Result<Unit>`

```kotlin
runCatching {
    val users = apiService.fetchLastPage()
    val now = Clock.System.now()
    // INSERT OR IGNORE preserves original addedAt for rows already in cache
    users.forEach { dto → localDataSource.insertOrIgnore(..., addedAt = now.toString()) }
}
```

**Important:** Same `now` for entire batch on refresh — timestamps within a refresh batch are identical.

#### `suspend fun addUser(...): Result<User>`

1. `apiService.createUser(...)` — network first
2. `localDataSource.insertOrIgnore` with API-returned user fields and `addedAt`
3. Return `user`

#### `suspend fun deleteUser(id): Result<Unit>`

1. `apiService.deleteUser(id)` — network first
2. `localDataSource.deleteById(id)` — local only on API success

### Design Decisions

- **runCatching** — exceptions become `Result.failure`
- **Delete: API before local** — if API fails, user remains in list
- **Add: API before local** — no optimistic add (offline add blocked at VM)

### Offline behavior


| Operation       | Offline result                                 |
| --------------- | ---------------------------------------------- |
| observeUsers    | Works (cached)                                 |
| refreshLastPage | Result.failure                                 |
| addUser         | Result.failure                                 |
| deleteUser      | Result.failure (unless finalize never reached) |


---

# 4. Presentation Layer (MVI)

---

## `presentation/userlist/UserListState.kt`

### Purpose

Immutable snapshot of everything the user list screen needs to render.

### Role

**Presentation** — MVI State.

### Properties


| Property              | Default              | Why                                                                |
| --------------------- | -------------------- | ------------------------------------------------------------------ |
| `users`               | `emptyList()`        | From DB Flow                                                       |
| `isLoading`           | `false`              | **Not true** — avoids shimmer flash on VM recreate with warm cache |
| `isRefreshing`        | `false`              | Pull-to-refresh indicator                                          |
| `error`               | `null`               | `UserListError?`                                                   |
| `showDeleteDialogFor` | `null`               | Which user dialog is open                                          |
| `pendingDeleteUser`   | `null`               | Undo window — row hidden while set                                 |
| `selectedUser`        | `null`               | Adaptive detail pane                                               |
| `formState`           | `AddUserFormState()` | Add user form                                                      |


### `AddUserFormState`

Form fields + validation errors + `isSubmitting` guard.

### Design Decisions

- **data class** — copy() for immutable updates via `_state.update { it.copy(...) }`
- Dialog state in VM (not local) — survives rotation
- Sheet visibility in UI (`rememberSaveable`) — split ownership

---

## `presentation/userlist/UserListIntent.kt`

### Purpose

Sealed hierarchy of every user action — single entry point `process(intent)`.

### Role

**Presentation** — MVI Intent/Event.

### All intents (17)

`LoadUsers`, `RefreshUsers`, `RequestDelete`, `DismissDeleteDialog`, `ConfirmDelete`, `UndoDelete`, `FinalizeDelete`, `SelectUser`, `ClearSelectedUser`, `UpdateFormName/Email/Gender/Status`, `SubmitAddUser`, `DismissAddUserSheet`

### Design Decisions

- **sealed interface** — exhaustive `when` in ViewModel
- **data class** for payloads (User, String, Gender)
- **data object** for parameterless events

---

## `presentation/userlist/UserListEffect.kt`

### Purpose

One-shot side effects UI must handle — not stored in State.

### Role

**Presentation** — MVI Effect.

### Effects

1. `ShowDeleteSnackbar(user)` — triggers Undo/Finalize flow
2. `ShowError(message)` — generic error snackbar
3. `UserAddedSuccess` — close sheet, scroll to top

### Design Decisions

**Why not state?** Snackbar is ephemeral; storing "showSnackbar=true" requires clearing and causes rotation bugs. Channel + Flow = consume once.

---

## `presentation/userlist/UserListViewModel.kt`

### Purpose

**Brain of the app** — MVI reducer, coroutine orchestration, validation, error classification.

### Role

**Presentation**

### Dependencies

`ObserveUsersUseCase`, `GetLastPageUsersUseCase`, `AddUserUseCase`, `DeleteUserUseCase`

### Properties


| Property   | Type                              | Visibility | Why                             |
| ---------- | --------------------------------- | ---------- | ------------------------------- |
| `_state`   | `MutableStateFlow<UserListState>` | private    | Writable internally             |
| `state`    | `StateFlow<UserListState>`        | public     | Read-only via `asStateFlow()`   |
| `_effects` | `Channel<UserListEffect>`         | private    | BUFFERED — won't suspend sender |
| `effects`  | `Flow<UserListEffect>`            | public     | `receiveAsFlow()`               |


**Why StateFlow not MutableStateFlow public?** Encapsulation — UI only reads.

### Lifecycle

- Extends `androidx.lifecycle.ViewModel` (KMP artifact)
- `viewModelScope` — cancelled when VM cleared
- `init` block starts perpetual `observeUsers().collect`

### `init` block

```
1. launch { observeUsers().collect { users → update state.users } }
2. if goRestToken.isBlank() → error = MissingToken
   else loadUsers()
```

### `fun process(intent: UserListIntent)`

Central dispatcher — `when (intent)` routes to private handlers.

### `private fun loadUsers()`

1. `cacheIsEmpty = users.isEmpty()`
2. If empty → `isLoading = true` (shimmer)
3. `getLastPageUsers()` — suspend
4. `isLoading = false`, set `error` if failure

### `private fun refreshUsers()`

Sets `isRefreshing`, calls same use case, clears refreshing.

### `private fun confirmDelete(user)`

1. Clear dialog, set `pendingDeleteUser = user`
2. Emit `ShowDeleteSnackbar` effect
3. **Does NOT call delete API**

### `private fun undoDelete(user)`

Clear `pendingDeleteUser` — row reappears via AnimatedVisibility.

### `private fun finalizeDelete()`

1. Guard: `pendingDeleteUser ?: return`
2. `deleteUserUseCase(user.id)`
3. On success/failure: clear pending; on failure emit `ShowError`

### `private fun updateFormName/Email`

Real-time validation with regex; empty field → no error (non-blocking while typing).

### `private fun submitAddUser()`

1. Guard `isSubmitting`
2. Full validation (required fields)
3. Set `isSubmitting = true`
4. `addUserUseCase(...).fold(onSuccess, onFailure)`
5. Success → reset form, `UserAddedSuccess` effect
6. Failure → `ValidationException` → field errors; network → `submitError`

### `private fun classifyError(e): UserListError`

String matching on exception message — fragile but fast.

### Execution Flow (typical launch)

```
init → observeUsers collect (background)
     → loadUsers → GetLastPageUsersUseCase → Repository → API+DB
     → Flow emits → state.users updated → UI recomposes
```

### Interview Questions

- **Advanced:** Why Channel for effects? **A:** No replay; one consumer.
- **Architecture:** Why 4 use cases not repository? **A:** ViewModel shouldn't know data layer.
- **Bug:** Process death during snackbar? **A:** pendingDelete lost — would use SavedStateHandle.

### Improvements

SavedStateHandle, typed network errors, extract Validator class.

---

# 5. UI Layer (Compose)

---

## `ui/screens/UserListScreen.kt`

### Purpose

Root composable — wires ViewModel to Material3 scaffold, handles effects, branches UI by state.

### Role

**UI**

### Dependencies

`UserListViewModel` via `koinViewModel()`, child composables, Material3 APIs.

### Key composable: `UserListScreen(viewModel = koinViewModel())`

### Local state

- `showAddUserSheet` — `rememberSaveable { mutableStateOf(false) }` — survives rotation
- `snackbarHostState` — `remember { SnackbarHostState() }`
- `listState` — `rememberLazyListState()` — shared with adaptive layout for scroll-to-top

### `LaunchedEffect(Unit)` — effect collector

```
effects.collect { effect →
  ShowDeleteSnackbar → showSnackbar with Undo
    → ActionPerformed → UndoDelete intent
    → Dismissed → FinalizeDelete intent
  ShowError → snackbar
  UserAddedSuccess → close sheet, animateScrollToItem(0)
}
```

**Why LaunchedEffect(Unit):** Start once per composition entry; cancels on leave.

### UI branches (`when`)

1. `isLoading && users.isEmpty()` → 8× `ShimmerUserCard`
2. `error != null && users.isEmpty()` → `ErrorStateContent` + Retry
3. else → `PullToRefreshBox` + optional `InlineErrorBanner` + list or empty

### Scaffold features

- Collapsing `TopAppBar` with `exitUntilCollapsedScrollBehavior`
- `ExtendedFloatingActionButton` — `expanded = listState.isScrollingUp()`
- `SnackbarHost`

### Overlays

- `AlertDialog` when `showDeleteDialogFor != null`
- `AddUserBottomSheet` when `showAddUserSheet`

### Design Decisions

- **collectAsState()** not `collectAsStateWithLifecycle` — KMP compatibility
- **Long press blocked** when `pendingDeleteUser != null`
- **Pull-to-refresh** only in content branch (not during full-screen error)

---

## `ui/screens/AddUserBottomSheet.kt`

### Purpose

Modal form for creating users — gender/status required by GoRest API.

### Role

**UI**

### Parameters

- `formState: AddUserFormState` — hoisted from ViewModel
- `onIntent: (UserListIntent) -> Unit` — sends intents up
- `onDismiss: () -> Unit` — parent closes sheet + resets form

### UI structure

`ModalBottomSheet` → scrollable `Column` with:

- Name/email `OutlinedTextField` with inline errors
- `SingleChoiceSegmentedButtonRow` for Gender and Status
- Submit `Button` with loading spinner
- `submitError` text

### Design Decisions

- **imePadding()** — keyboard doesn't cover fields
- **navigationBarsPadding()** — safe area on gesture nav devices
- No local ViewModel — pure presentation component

---

## `ui/components/UserCard.kt`

### Purpose

Single list row — avatar, name, email, relative time, status chip.

### Role

**UI**

### `UserCard(user, onClick, onLongClick)`

- `Card` + `combinedClickable` — tap vs long-press
- `Modifier.heightIn(min = 72.dp)` — accessibility touch target
- `UserAvatar` at 44.dp
- `user.addedAt.toRelativeString()`
- `StatusChip` — green pill for Active

### `StatusChip(status)`

Visual indicator using `ActiveGreen` / `ActiveGreenContainer` from theme.

---

## `ui/components/UserAvatar.kt`

### Purpose

Letter avatar with deterministic color from name hash.

### `UserAvatar(name, modifier)`

- `remember(name)` — recompute only when name changes
- `initial` = first char uppercase
- `bg` = `avatarColors[name.hashCode().absoluteValue % size]`
- `contentDescription` for accessibility

### Design Decisions

No Coil/network images — challenge doesn't require; zero loading complexity.

---

## `ui/components/UserDetailPane.kt`

### Purpose

Detail pane in adaptive layout — large avatar, labeled rows.

### `UserDetailPane(user: User?)`

- If null → "Select a user" placeholder
- Else → 72.dp avatar, name, email, divider, `DetailRow` for status/gender/added date

---

## `ui/components/ShimmerUserCard.kt`

### Purpose

Loading skeleton matching UserCard layout.

### `shimmerBrush()` — infinite animation translating gradient

### `ShimmerUserCard` — grey rounded rects in Card shape

---

## `ui/components/ErrorStateContent.kt`

### Purpose

Full-screen and inline error UI.

### `ErrorStateContent(error, onRetry)`

Maps `UserListError` → icon, title, body. Retry hidden for `MissingToken`.

### `InlineErrorBanner(error)`

Thin banner above list when cache is warm but refresh failed.

---

## `ui/components/EmptyStateContent.kt`

### Purpose

Zero-state when list empty and not loading.

---

## `ui/util/AdaptiveUserListLayout.kt`

### Purpose

Material3 Adaptive list/detail for tablet and landscape.

### `AdaptiveUserListLayout(...)`

- `rememberListDetailPaneScaffoldNavigator()` — **no type parameter**
- `PlatformBackHandler` — Android back pops detail
- `ListDetailPaneScaffold` with `listPane` and `detailPane` as `AnimatedPane`

### `UserListPane`

- `LazyColumn` with `AnimatedVisibility` hiding `pendingDeleteUserId`
- Exit animation: `slideOutHorizontally + shrinkVertically`

### Design Decisions

`selectedUser` in ViewModel, not navigator back stack — avoids Parcelable crash.

---

## `ui/util/LazyListStateExt.kt`

### Purpose

FAB expand/collapse based on scroll direction.

### `LazyListState.isScrollingUp(): Boolean`

`derivedStateOf` — compares current vs previous `firstVisibleItemIndex` and `firstVisibleItemScrollOffset`.

**Why derivedStateOf:** Recompose only when scroll direction changes, not every pixel.

---

## `ui/util/PlatformBackHandler.kt` (+ platform actuals)

### Purpose

`expect fun PlatformBackHandler(enabled, onBack)`


| Platform | Implementation                      |
| -------- | ----------------------------------- |
| Android  | `BackHandler` from activity-compose |
| iOS      | no-op (swipe back native)           |
| JVM      | no-op                               |


---

## `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`

### Color.kt

Brand seed colors + `ActiveGreen` for status chips.

### Type.kt

`AppTypography` — Material3 Typography with custom title/body/label scales.

### Theme.kt

`expect fun AppTheme(darkTheme, content)`
`AppThemeBase` — static light/dark ColorScheme + Typography.

### Theme.android.kt

`actual AppTheme` — dynamic Material You on API 31+, else seed colors. Uses `isSystemInDarkTheme()`.

### Theme.ios.kt / Theme.jvm.kt

Delegate to `AppThemeBase`.

---

# 6. Dependency Injection & Config

---

## `di/AppModules.kt`

### Purpose

Koin module definitions — wires entire app graph.

### Modules

#### `networkModule`

- `single { HttpClient(get()) { ... } }` — JSON, timeout 10s, retry 3x, Bearer token
- `single { UserApiService(get()) }`

#### `databaseModule`

- `single { UserDatabase(get()) }`
- `single { UserLocalDataSource(get()) }`

#### `repositoryModule`

- `single<UserRepository> { UserRepositoryImpl(get(), get()) }`

#### `domainModule`

- All 4 use cases as `single`

#### `presentationModule`

- `viewModel { UserListViewModel(get(), get(), get(), get()) }`

### `fun initKoin(additionalModules, appDeclaration)`

`startKoin { appDeclaration(); modules(...); modules(additionalModules) }`

---

## `di/PlatformModule.android.kt`

### `androidModule`

- `SqlDriver` → `AndroidSqliteDriver(Schema, context, "usermanager.db")`
- `HttpClientEngine` → `OkHttp.create()`

---

## `di/PlatformModule.ios.kt`

### `iosModule` + `initKoinIos()`

- `NativeSqliteDriver(Schema, "usermanager.db")`
- `Darwin.create()` engine

---

## `config/AppConfig.kt` (+ actuals)

### `expect val goRestToken: String`


| Source set | Source                                   |
| ---------- | ---------------------------------------- |
| android    | `BuildConfig.GOREST_TOKEN`               |
| ios        | `Secrets.goRestToken` (gitignored)       |
| jvm        | `System.getProperty("GOREST_TOKEN", "")` |


---

# 7. Platform Entry Points

---

## `androidMain/.../UserManagerApp.kt`

### Purpose

`Application` subclass — Koin + Napier init before any Activity.

### `onCreate`

```kotlin
Napier.base(DebugAntilog())
initKoin(
    additionalModules = listOf(androidModule),
    appDeclaration = { androidContext(this); androidLogger() }
)
```

---

## `androidMain/.../MainActivity.kt`

### Purpose

Single Activity host for Compose.

### `onCreate`

`enableEdgeToEdge()` → `setContent { AppTheme { UserListScreen() } }`

---

## `androidMain/AndroidManifest.xml`

- `INTERNET` permission
- `application android:name=".UserManagerApp"`
- `MainActivity` exported with MAIN/LAUNCHER intent filter
- `configChanges` for orientation (Compose handles)
- `windowSoftInputMode=adjustResize`

---

## `androidMain/res/values/strings.xml`

`app_name` = "User Management" (or project name) — referenced by manifest label.

---

## `iosMain/.../MainViewController.kt`

### Purpose

Exports Compose UI as `UIViewController` for SwiftUI embedding.

### `MainViewController()`

```kotlin
ComposeUIViewController(configure = { initKoinIos() }) {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        UserListScreen()
    }
}
```

Koin initialized in `configure` block — runs before first composition.

---

# 8. iOS Shell

---

## `iosApp/iosApp/iOSApp.swift`

`@main struct iOSApp: App` → `WindowGroup { ContentView() }`

---

## `iosApp/iosApp/ContentView.swift`

`ComposeView: UIViewControllerRepresentable` → `MainViewControllerKt.MainViewController()`

`.ignoresSafeArea(.keyboard)` — keyboard inset fix.

---

## `iosApp/iosApp/Info.plist`

- `CADisableMinimumFrameDurationOnPhone` = true — **required** by Compose Multiplatform for ProMotion
- `NSAppTransportSecurity` allows arbitrary loads (dev convenience)
- Supported orientations include landscape (for adaptive demo)

---

## `iosApp/iosApp.xcodeproj/project.pbxproj`

- Build phase: `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
- `OTHER_LDFLAGS = -lsqlite3` — SQLDelight native driver needs system SQLite
- Framework search path → `composeApp/build/xcode-frameworks/...`

---

# 9. Tests

---

## `fakes/FakeUserRepository.kt`

Hand-written test double — `MutableStateFlow` for users, configurable `Result` for each operation, `emit()` helper.

---

## `UserListViewModelTest.kt` (18 tests)

Tests MVI flows with fake repo + real use case impls. Uses `UnconfinedTestDispatcher` for `Dispatchers.Main`. Turbine for effect assertions.

Key scenarios: load success/failure, cached+error, delete dialog, undo, finalize, add 422, form reset.

---

## `RelativeTimeFormatterTest.kt` (12 tests)

Boundary tests for every time bucket + singular/plural.

---

## `UserRepositoryImplTest.kt` (6 tests)

**Note:** Tests `FakeUserRepository`, not real impl — naming mismatch.

---

## `SqlDelightInsertOrIgnoreTest.kt` (2 tests)

JVM integration with `JdbcSqliteDriver(IN_MEMORY)` — proves `addedAt` preservation on duplicate ID.

---

# 10. CI & Release

Covered in root section — CI runs tests + debug APK. Release enables R8 with `proguard-rules.pro`.

---

# 11. Overall Execution Walkthrough

## Android cold start

```
1. Zygote forks app process
2. UserManagerApp.onCreate()
   → Napier init
   → initKoin(androidModule, androidContext)
   → SqlDriver bean registered (DB file created on first query)
   → HttpClient bean registered
3. MainActivity.onCreate()
   → setContent { AppTheme { UserListScreen() } }
4. UserListScreen composes
   → koinViewModel<UserListViewModel>() created/retrieved
5. UserListViewModel.init
   → observeUsers().collect { ... }  [Flow subscription starts]
   → goRestToken check
   → loadUsers() if token present
6. loadUsers → GetLastPageUsersUseCase → UserRepositoryImpl.refreshLastPage
   → UserApiService.fetchLastPage() [HTTP]
   → UserLocalDataSource.insertOrIgnore() [SQLite]
   → Flow emits new user list
7. state.users updated → collectAsState triggers recomposition
8. UserListScreen renders AdaptiveUserListLayout → UserCard for each user
```

## User taps FAB

```
FAB onClick → showAddUserSheet = true
→ AddUserBottomSheet composes
→ User types → UpdateFormName intent → ViewModel updates formState
→ Submit → addUserUseCase → API POST → insertOrIgnore → Flow emit
→ UserAddedSuccess effect → sheet closes, scroll to 0
```

---

# 12. Dependency Graph

```
UI (Compose screens/components)
  ↓ intents / reads state
Presentation (UserListViewModel)
  ↓ use cases
Domain (interfaces, models, use case interfaces)
  ↓
Data (UserRepositoryImpl, UserApiService, UserLocalDataSource)
  ↓                    ↓
Network (Ktor)      Database (SQLDelight)
  ↓                    ↓
Platform engines   Platform SqlDriver
(OkHttp/Darwin)    (Android/iOS)
```

**Must NOT communicate directly:**

- UI → Repository (goes through ViewModel)
- UI → UserApiService
- Domain → Ktor/SQLDelight
- UserApiService → ViewModel

---

# 13. Call Flows

## Opening app

`UserManagerApp` → `MainActivity` → `UserListScreen` → `UserListViewModel.init` → `observeUsers` + `loadUsers`

## Fetching users

`LoadUsers` → `getLastPageUsers()` → `refreshLastPage()` → `fetchLastPage()` → `insertOrIgnore` × N → Flow emit

## Displaying cached users

`observeUsers()` → Flow → `state.users` → `AdaptiveUserListLayout` → `UserCard`

## Adding user

`SubmitAddUser` → validate → `addUserUseCase` → `createUser` POST → `insertOrIgnore` → `UserAddedSuccess`

## Validating input

`UpdateFormName` → regex check → `formState.nameError`
`SubmitAddUser` → required + regex → field errors or proceed

## Deleting user

`RequestDelete` → dialog → `ConfirmDelete` → `pendingDeleteUser` + snackbar effect
→ `FinalizeDelete` → `deleteUserUseCase` → API DELETE → `deleteById`

## Undo delete

Snackbar Undo → `UndoDelete` → `pendingDeleteUser = null` → row visible again (no API call)

## Offline mode

`refreshLastPage` throws → `classifyError` → `NoInternet`
If cache warm: users visible + `InlineErrorBanner`
If cache empty: `ErrorStateContent`

## Error handling

Repository `runCatching` → ViewModel `classifyError` / `fold` → State error or Effect or form errors

---

# 14. Architecture Validation

## Good decisions

- Offline-first read path
- MVI with explicit intents/effects
- INSERT OR IGNORE cache semantics
- Deferred delete API
- Minimal expect/actual surface
- Real SQLDelight JVM test

## Weak decisions

- String-based error classification
- UserRepositoryImplTest tests fake
- ValidationException in domain package
- iOS ATS arbitrary loads

## Code smells

- Duplicate gender/status mapping (Dto + Entity)
- `GetLastPageUsersUseCase` name vs refresh behavior
- Regex duplicated in update vs submit

## Testing gaps

- No UserApiService tests
- No real RepositoryImpl tests
- No Compose UI tests
- Release R8 not verified in CI

## Security

- Token not in git ✅
- ATS wide open on iOS ⚠️
- Client validation only — server 422 is backstop ✅

---

# 15. Learning Roadmap

Read in this exact order:


| #   | File                                                           | Why now              | Concept                      |
| --- | -------------------------------------------------------------- | -------------------- | ---------------------------- |
| 1   | `settings.gradle.kts` + `composeApp/build.gradle.kts`          | Project shape        | KMP targets, dependencies    |
| 2   | `domain/model/User.kt`                                         | Core entity          | Domain modeling              |
| 3   | `domain/repository/UserRepository.kt`                          | Contracts            | Ports & adapters             |
| 4   | `User.sq`                                                      | Schema               | SQLDelight, INSERT OR IGNORE |
| 5   | `data/local/UserLocalDataSource.kt`                            | Cache                | Flow observation             |
| 6   | `data/remote/UserDto.kt` + `UserApiService.kt`                 | API                  | Ktor, pagination, 422        |
| 7   | `data/repository/UserRepositoryImpl.kt`                        | Orchestration        | Offline-first                |
| 8   | `domain/usecase/*.kt`                                          | Use cases            | Clean architecture           |
| 9   | `di/AppModules.kt` + platform modules                          | Wiring               | Koin graph                   |
| 10  | `config/AppConfig.kt`                                          | Secrets              | expect/actual                |
| 11  | `presentation/UserListState/Intent/Effect.kt`                  | MVI contracts        | State vs effect              |
| 12  | `presentation/UserListViewModel.kt`                            | Brain                | Reducer, coroutines          |
| 13  | `ui/theme/*`                                                   | Visual foundation    | Material3                    |
| 14  | `ui/components/UserCard.kt` + `UserAvatar.kt`                  | List building blocks | Compose patterns             |
| 15  | `ui/util/AdaptiveUserListLayout.kt`                            | Adaptive             | ListDetailPaneScaffold       |
| 16  | `ui/screens/UserListScreen.kt`                                 | Root UI              | Effect collection, branches  |
| 17  | `ui/screens/AddUserBottomSheet.kt`                             | Form UX              | State hoisting               |
| 18  | `UserManagerApp.kt` + `MainActivity.kt`                             | Android entry        | App startup                  |
| 19  | `MainViewController.kt` + iOS Swift files                      | iOS entry            | UIViewController bridge      |
| 20  | `UserListViewModelTest.kt` + `SqlDelightInsertOrIgnoreTest.kt` | Verification         | Testing strategy             |


After step 20 you understand: **startup → DI → VM init → API fetch → DB cache → Flow → Compose UI → user actions → state/effects → API mutations → UI update**.

---

# Appendix A — Per-File Deep Dives (Exact Format)

The sections above group related files. This appendix applies the **full template** to every source file.

---

## File: `composeApp/src/commonMain/kotlin/com/kanav/usermanager/domain/usecase/GetLastPageUsersUseCase.kt`

### Purpose

Defines the contract and implementation for refreshing users from the **last page** of the GoRest API into local cache.

### Role in the Architecture

**Domain** — application-specific operation; sits between ViewModel and Repository.

### Dependencies

- `UserRepository` — only dependency
- Returns `Result<Unit>` — success means cache updated, not "list of users" (UI reads Flow separately)

### Classes

- `**GetLastPageUsersUseCase`** (fun interface) — callable syntax `invoke()`
- `**GetLastPageUsersUseCaseImpl**` — constructor-injected repository

### Functions

`**suspend operator fun invoke(): Result<Unit>**`

1. Calls `repository.refreshLastPage()`
2. Returns Result as-is
3. No transformation — pure delegation

**Why suspend:** Repository refresh performs network I/O.

**Alternatives:** Inline in ViewModel (breaks layering); rename to `RefreshLastPageUseCase` (clearer).

### Design Decisions

- **fun interface** — SAM-like, one method, easy fake
- **Result not throw** — ViewModel handles failure without try/catch

### Potential Interview Questions


| Level        | Question                    | Ideal Answer                                              |
| ------------ | --------------------------- | --------------------------------------------------------- |
| Beginner     | What does this use case do? | Fetches last API page into SQLite                         |
| Intermediate | Why Result not List?        | UI observes DB Flow; refresh is write-only                |
| Advanced     | Is this CQRS?               | Light command/query split: observe=query, refresh=command |


### Improvements

Rename to `RefreshLastPageUseCase`; add interface method documenting two-step pagination.

### File Summary

**Depends on:** `UserRepository`. **Used by:** `UserListViewModel`, `AppModules`, tests.

---

## File: `composeApp/src/commonTest/kotlin/com/kanav/usermanager/UserListViewModelTest.kt`

### Purpose

Unit tests for all critical `UserListViewModel` MVI paths without real network or database.

### Role in the Architecture

**Testing** — validates presentation layer in isolation.

### Dependencies

- `FakeUserRepository` — controllable data + Results
- Real use case implementations (thin wrappers)
- `UnconfinedTestDispatcher` — replaces `Dispatchers.Main`
- `turbine` — effect Flow assertions
- `kotlinx-coroutines-test` — `runTest`

### Setup / Teardown

`**@BeforeTest setup()`**

- `Dispatchers.setMain(UnconfinedTestDispatcher())`
- Construct fake repo + ViewModel with 4 use case impls

`**@AfterTest teardown()**`

- `Dispatchers.resetMain()`

### Test cases (18)


| Test                       | Asserts                                |
| -------------------------- | -------------------------------------- |
| LoadUsers empty DB success | users non-empty, not loading, no error |
| LoadUsers empty DB failure | NoInternet error                       |
| LoadUsers cached + failure | users remain, error set                |
| RequestDelete              | showDeleteDialogFor set                |
| DismissDeleteDialog        | dialog cleared                         |
| ConfirmDelete              | pending set, dialog cleared            |
| UndoDelete                 | pending cleared, API not called        |
| FinalizeDelete             | pending cleared after success          |
| FinalizeDelete no pending  | no-op                                  |
| FinalizeDelete API failure | ShowError effect via Turbine           |
| SubmitAddUser success      | UserAddedSuccess effect                |
| SubmitAddUser idempotent   | (weak — no final assert)               |
| SubmitAddUser network fail | submitError set                        |
| SubmitAddUser 422 email    | emailError set                         |
| SubmitAddUser 422 name     | nameError set                          |
| SubmitAddUser 422 both     | both errors                            |
| DismissAddUserSheet        | form reset                             |
| RefreshUsers               | isRefreshing false after               |


### Design Decisions

- **Fake not Mock** — readable, no mock framework
- **Real use cases** — tests wiring + ViewModel logic together
- **Turbine for effects** — async snackbar events

### Improvements

Fix idempotent submit test; add test for MissingToken init path; use `StandardTestDispatcher` for timing-sensitive tests.

### File Summary

Highest-value test file in the project. Proves delete/undo/add/error paths.

---

## File: `composeApp/src/jvmTest/kotlin/com/kanav/usermanager/SqlDelightInsertOrIgnoreTest.kt`

### Purpose

**Integration test** — validates real SQLite `INSERT OR IGNORE` semantics, not mocked behavior.

### Role

**Testing / Database**

### Dependencies

- `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)`
- `UserDatabase.Schema.create(driver)`
- Generated `userQueries`

### Functions

`**@BeforeTest setup()`** — create in-memory DB

`**INSERT OR IGNORE preserves addedAt on duplicate id**`

1. Insert id=1, addedAt=T1
2. Insert id=1, addedAt=T2
3. Assert stored addedAt == T1

`**INSERT OR IGNORE inserts new row when id is unique**`

1. Insert id=2
2. Assert count == 1

### Why JVM test not commonTest

SQLDelight JDBC driver is JVM-only; validates SQL policy with real engine.

### Interview Questions

**Q:** Why not mock UserLocalDataSource? **A:** INSERT OR IGNORE behavior is SQL-level guarantee we must prove.

### File Summary

Defends the most subtle caching decision in the entire app.

---

## File: `iosApp/iosApp/ContentView.swift`

### Purpose

Bridge SwiftUI app lifecycle to Kotlin Compose runtime via `UIViewControllerRepresentable`.

### Role

**Platform / iOS shell** — thinnest possible native layer.

### Classes

- `**ContentView: View`** — SwiftUI root view
- `**ComposeView: UIViewControllerRepresentable**` — embeds Kotlin VC

### Functions

`**makeUIViewController(context:)**` → `MainViewControllerKt.MainViewController()`

- `MainViewControllerKt` is Kotlin export from `ComposeApp` framework

`**updateUIViewController(_:context:)**` — empty (static content)

### Execution Flow

`iOSApp` → `ContentView` → `ComposeView` → Kotlin `MainViewController` → `UserListScreen`

### Design Decisions

SwiftUI chosen over UIKit AppDelegate-only for modern iOS template; all UI still Compose.

### File Summary

Only Swift UI bridge; ~16 lines. No business logic.

---

# Appendix B — Complete File Index


| #   | File                                             | Layer               |
| --- | ------------------------------------------------ | ------------------- |
| 1   | `settings.gradle.kts`                            | Config              |
| 2   | `build.gradle.kts`                               | Config              |
| 3   | `gradle/libs.versions.toml`                      | Config              |
| 4   | `gradle.properties`                              | Config              |
| 5   | `composeApp/build.gradle.kts`                    | Config              |
| 6   | `composeApp/proguard-rules.pro`                  | Config              |
| 7   | `.github/workflows/ci.yml`                       | Config              |
| 8   | `domain/model/User.kt`                           | Domain              |
| 9   | `domain/repository/UserRepository.kt`            | Domain              |
| 10  | `domain/usecase/ObserveUsersUseCase.kt`          | Domain              |
| 11  | `domain/usecase/GetLastPageUsersUseCase.kt`      | Domain              |
| 12  | `domain/usecase/AddUserUseCase.kt`               | Domain              |
| 13  | `domain/usecase/DeleteUserUseCase.kt`            | Domain              |
| 14  | `domain/util/RelativeTimeFormatter.kt`           | Domain              |
| 15  | `sqldelight/.../User.sq`                         | Database            |
| 16  | `data/local/UserLocalDataSource.kt`              | Data                |
| 17  | `data/remote/UserDto.kt`                         | Data                |
| 18  | `data/remote/UserApiService.kt`                  | Data                |
| 19  | `data/repository/UserRepositoryImpl.kt`          | Data                |
| 20  | `presentation/userlist/UserListState.kt`         | Presentation        |
| 21  | `presentation/userlist/UserListIntent.kt`        | Presentation        |
| 22  | `presentation/userlist/UserListEffect.kt`        | Presentation        |
| 23  | `presentation/userlist/UserListViewModel.kt`     | Presentation        |
| 24  | `di/AppModules.kt`                               | DI                  |
| 25  | `config/AppConfig.kt`                            | Config              |
| 26  | `ui/screens/UserListScreen.kt`                   | UI                  |
| 27  | `ui/screens/AddUserBottomSheet.kt`               | UI                  |
| 28  | `ui/components/UserCard.kt`                      | UI                  |
| 29  | `ui/components/UserAvatar.kt`                    | UI                  |
| 30  | `ui/components/UserDetailPane.kt`                | UI                  |
| 31  | `ui/components/ShimmerUserCard.kt`               | UI                  |
| 32  | `ui/components/ErrorStateContent.kt`             | UI                  |
| 33  | `ui/components/EmptyStateContent.kt`             | UI                  |
| 34  | `ui/util/AdaptiveUserListLayout.kt`              | UI                  |
| 35  | `ui/util/LazyListStateExt.kt`                    | UI                  |
| 36  | `ui/util/PlatformBackHandler.kt`                 | UI                  |
| 37  | `ui/theme/Color.kt`                              | UI                  |
| 38  | `ui/theme/Type.kt`                               | UI                  |
| 39  | `ui/theme/Theme.kt`                              | UI                  |
| 40  | `androidMain/.../UserManagerApp.kt`                   | Platform            |
| 41  | `androidMain/.../MainActivity.kt`                | Platform            |
| 42  | `androidMain/AndroidManifest.xml`                | Platform            |
| 43  | `androidMain/res/values/strings.xml`             | Platform            |
| 44  | `androidMain/.../PlatformModule.android.kt`      | DI                  |
| 45  | `androidMain/.../AppConfig.android.kt`           | Config              |
| 46  | `androidMain/.../Theme.android.kt`               | UI                  |
| 47  | `androidMain/.../PlatformBackHandler.android.kt` | UI                  |
| 48  | `iosMain/.../MainViewController.kt`              | Platform            |
| 49  | `iosMain/.../PlatformModule.ios.kt`              | DI                  |
| 50  | `iosMain/.../AppConfig.ios.kt`                   | Config              |
| 51  | `iosMain/.../Secrets.ios.kt`                     | Config (gitignored) |
| 52  | `iosMain/.../Theme.ios.kt`                       | UI                  |
| 53  | `iosMain/.../PlatformBackHandler.ios.kt`         | UI                  |
| 54  | `jvmMain/.../AppConfig.jvm.kt`                   | Config              |
| 55  | `jvmMain/.../Theme.jvm.kt`                       | UI                  |
| 56  | `jvmMain/.../PlatformBackHandler.jvm.kt`         | UI                  |
| 57  | `commonTest/.../FakeUserRepository.kt`           | Testing             |
| 58  | `commonTest/.../UserListViewModelTest.kt`        | Testing             |
| 59  | `commonTest/.../RelativeTimeFormatterTest.kt`    | Testing             |
| 60  | `commonTest/.../UserRepositoryImplTest.kt`       | Testing             |
| 61  | `jvmTest/.../SqlDelightInsertOrIgnoreTest.kt`    | Testing             |
| 62  | `iosApp/iosApp/iOSApp.swift`                     | iOS shell           |
| 63  | `iosApp/iosApp/ContentView.swift`                | iOS shell           |
| 64  | `iosApp/iosApp/Info.plist`                       | iOS shell           |
| 65  | `iosApp/iosApp.xcodeproj/project.pbxproj`        | iOS shell           |


**Skipped (generated/build):** `.gradle/`, `build/`, `.kotlin/`, SQLDelight generated `UserDatabase.kt`, `BuildConfig`

---

# Appendix C — Part 20 Script (Extended)

Use this verbatim structure in a technical interview:

**Opening (30 sec):**
"I built a Kotlin Multiplatform user management app — one Compose UI codebase targeting Android and iOS, with shared ViewModel and business logic. It talks to the GoRest public API and caches locally in SQLDelight for offline reading."

**Architecture (2 min):**
"Clean Architecture with MVI. UI sends Intents to UserListViewModel. State flows down via StateFlow. One-shot events like snackbars go through a Channel as Effects. Below the ViewModel are use cases on a Repository interface. The implementation coordinates Ktor for network and SQLDelight for cache. The database is the UI's source of truth — the ViewModel subscribes to a Flow from SQLite."

**Hardest problem (1 min):**
"Delete with Undo. If you call DELETE immediately on confirm, Undo can't work if the network is fast. I defer the API call until the snackbar is dismissed without Undo. Meanwhile `pendingDeleteUser` hides the row in the UI."

**KMP lesson (1 min):**
"Dependency alignment matters. Koin 4.2.1 was compiled with a newer Kotlin and broke iOS klib linking. Material3 adaptive navigation is a separate artifact from the layout library. And INSERT OR IGNORE isn't an accident — it preserves local timestamps across API refresh."

**Close:**
"Tests focus on ViewModel behavior with a fake repository and a JVM SQLDelight test that proves our cache semantics. Given more time I'd add MockEngine API tests and SavedStateHandle for process death."

---

*End of Code Walkthrough. See also `docs/TECHNICAL_DEEP_DIVE.md` for 150 categorized interview Q&A.*