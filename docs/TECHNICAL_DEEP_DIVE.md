# KMP User Management — Technical Deep Dive

> **Purpose:** Understand every architectural, UX, and implementation decision for portfolio reviewers and technical discussions.
>
> **Repo:** [https://github.com/kanav22/kmp-user-management](https://github.com/kanav22/kmp-user-management)

---

# Part 1 — Project Overview

## What the application does

A cross-platform **User Management** app for **Android and iOS** that:

1. Fetches users from the **last page** of GoRest `/users` and displays them in a Material 3 list
2. Shows **name, email, relative timestamp** ("5 minutes ago") computed in shared Kotlin code
3. Supports **pull-to-refresh**, **shimmer loading**, and **graceful offline/error states**
4. Lets users **add** a user via a polished bottom sheet with real-time validation
5. Lets users **delete** via long-press → confirmation → animated removal → **Undo snackbar**
6. **Adapts layout** between portrait (single list) and landscape/tablet (list + detail pane)
7. Works **offline** for reading cached users; network failures degrade gracefully

## Project features satisfied


| Requirement                  | Status | Primary implementation                                      |
| ---------------------------- | ------ | ----------------------------------------------------------- |
| Smart User Feed (last page)  | ✅      | `UserApiService.fetchLastPage()`, `GetLastPageUsersUseCase` |
| Relative timestamps (shared) | ✅      | `RelativeTimeFormatter.kt`                                  |
| Shimmer loading              | ✅      | `ShimmerUserCard.kt`, `UserListScreen`                      |
| Error / No Internet UI       | ✅      | `ErrorStateContent`, `InlineErrorBanner`                    |
| Add User FAB + form          | ✅      | `AddUserBottomSheet`, `UserListViewModel.submitAddUser`     |
| Real-time validation         | ✅      | `updateFormName`, `updateFormEmail`                         |
| Immediate insert after 201   | ✅      | SQLDelight Flow + `insertOrIgnore`                          |
| Delete + confirmation        | ✅      | `RequestDelete`, `AlertDialog`                              |
| Delete animation + Undo      | ✅      | `AnimatedVisibility`, `ShowDeleteSnackbar` effect           |
| Adaptive layout              | ✅      | `AdaptiveUserListLayout`, `ListDetailPaneScaffold`          |
| 100% shared Compose UI       | ✅      | All screens in `commonMain`                                 |
| Clean Architecture + MVI     | ✅      | domain/data/presentation/ui layers                          |
| Ktor + SQLDelight            | ✅      | `UserApiService`, `User.sq`                                 |
| Offline support              | ✅      | DB as source of truth                                       |
| Koin DI                      | ✅      | `AppModules.kt`                                             |
| Unit tests                   | ✅      | ViewModel, formatter, SQLDelight JVM test                   |


## Optional / stretch (not required but implemented)

- GitHub Actions CI with APK artifact
- R8/ProGuard for release builds
- iOS Xcode project with Gradle framework integration
- Dynamic Material You colors on Android API 31+
- Extended FAB that collapses on scroll
- Collapsing top app bar
- Accessibility: semantics, 72dp min touch targets
- Token guard on startup (prevents silent 401 loops)
- 404-as-success on DELETE (idempotent deletes)

## Highest engineering effort

1. **Delete + Undo race condition** — deferred API call until snackbar dismissed
2. **Offline-first + INSERT OR IGNORE** — preserving `addedAt` across refreshes
3. **KMP dependency compatibility** — Koin 4.1.1 KLIB ABI, material3-adaptive-navigation artifact
4. **Adaptive layout without Parcelable crash** — untyped `rememberListDetailPaneScaffoldNavigator()`
5. **422 validation mapping** — API field errors → form field errors
6. **iOS build setup** — Xcode project, `-lsqlite3`, `CADisableMinimumFrameDurationOnPhone`

## Requirements that shaped architecture

- **Offline read** → SQLDelight + `Flow` observation drove Repository-as-cache pattern
- **Shared ViewModel** → MVI in `commonMain`, Koin `koinViewModel()`
- **Undo delete** → `pendingDeleteUser` state + `Channel` effects + deferred `FinalizeDelete`
- **Adaptive UI** → `selectedUser` in ViewModel, not in navigator generic type
- **Last page fetch** → two-step pagination probe in `UserApiService`

---

# Part 2 — Requirement Mapping

## 1. Smart User Feed

**Files:** `UserApiService.kt`, `UserRepositoryImpl.kt`, `GetLastPageUsersUseCase.kt`, `UserListViewModel.kt`, `UserListScreen.kt`, `UserCard.kt`, `RelativeTimeFormatter.kt`

**Classes:** `UserApiService`, `UserRepositoryImpl`, `UserListViewModel`, `UserCard`

**Flow:**

```
UserListScreen → LoadUsers intent → GetLastPageUsersUseCase → refreshLastPage()
→ fetchLastPage() [GET page 1, read X-Pagination-Pages, GET last page]
→ insertOrIgnore each UserDto → SQLDelight Flow emits → ViewModel state.users updates → UserCard renders
```

**Why:** GoRest paginates; project asks for **last page** users. Local DB is display source so UI survives rotation and offline.

**Alternatives:** Fetch all pages (slow, wasteful); fetch page 1 only (wrong requirement); Room instead of SQLDelight (valid but SQLDelight is more KMP-native).

**Limitations:** Only last page cached, not full directory. Refresh replaces/augments last page rows only.

---

## 2. Relative Timestamps

**Files:** `RelativeTimeFormatter.kt`, `UserCard.kt`, `UserDetailPane.kt`

**Function:** `Instant.toRelativeString()`

**Why shared:** Challenge requires shared logic; uses `kotlinx-datetime` in `commonMain`.

**Alternatives:** Platform formatters (violates KMP goal); `kotlinx-datetime` formatting API (less UX-friendly).

**Limitations:** `addedAt` is **local cache time**, not server creation time — documented in README. No months/years buckets.

---

## 3. Shimmer Loading

**Files:** `ShimmerUserCard.kt`, `UserListScreen.kt`

**Trigger:** `isLoading && users.isEmpty()` — shimmer only on cold start with empty cache.

**Why:** `isLoading` defaults `false` to avoid flash when ViewModel recreates with warm DB.

---

## 4. Error / No Internet

**Files:** `ErrorStateContent.kt`, `UserListViewModel.classifyError()`, `InlineErrorBanner`

**States:**

- Empty cache + error → full-screen `ErrorStateContent` with Retry
- Warm cache + error → list visible + `InlineErrorBanner`
- Missing token → dedicated copy, no Retry button

**Classification:** String matching on exception message (`Unable to resolve host`, `Network`, `timeout`).

**Limitation:** Fragile vs typed network exceptions; acceptable for project scope.

---

## 5. Add User Flow

**Files:** `AddUserBottomSheet.kt`, `UserListViewModel`, `AddUserUseCase`, `UserApiService.createUser`

**Flow:**

```
FAB tap → showAddUserSheet (rememberSaveable local state)
→ UpdateForm* intents → inline validation
→ SubmitAddUser → addUserUseCase → POST → insertOrIgnore → Flow updates list
→ UserAddedSuccess effect → close sheet + scroll to top
```

**Validation regex:**

- Name: `^[\p{L}\s'\-.]{2,100}$`
- Email: `^[^\s@]+@[^\s@]+\.[^\s@]{2,}$`

**422 handling:** `ValidationException` → `nameError` / `emailError` on form state.

**Offline:** Add fails with "No internet" message — no offline queue (documented tradeoff).

---

## 6. Delete + Undo

**Files:** `UserListScreen.kt`, `UserListViewModel.kt`, `AdaptiveUserListLayout.kt`, `DeleteUserUseCase`

**Flow:**

```
Long press → RequestDelete → AlertDialog
→ ConfirmDelete → pendingDeleteUser set + ShowDeleteSnackbar effect
→ UI hides row (AnimatedVisibility, id == pendingDeleteUserId)
→ Snackbar: Undo → UndoDelete (clear pending) | Dismissed → FinalizeDelete → deleteUser API + local delete
```

**Why deferred API:** Prevents race where Undo arrives after DELETE already sent.

**Limitation:** Undo is local-only until snackbar dismissed; if app killed during snackbar, delete may not finalize (acceptable).

---

## 7. Adaptive Layout

**Files:** `AdaptiveUserListLayout.kt`, `UserDetailPane.kt`, `PlatformBackHandler.kt`

**APIs:** `ListDetailPaneScaffold`, `rememberListDetailPaneScaffoldNavigator()`, `AnimatedPane`

**State:** `selectedUser` in ViewModel — navigator has **no type parameter** (avoids Android Parcelable crash).

**Portrait:** Single list pane. **Landscape/wide:** List + detail side by side.

---

## 8. Offline Support

**Read:** Always from SQLDelight. Launch with cache + no network → list + inline banner.

**Write:** Add/delete require network. No sync queue.

**Refresh failure with cache:** Users stay visible; error banner shown.

---

## 9. Unit Tests

**Files:** `UserListViewModelTest.kt`, `RelativeTimeFormatterTest.kt`, `SqlDelightInsertOrIgnoreTest.kt`, `UserRepositoryImplTest.kt` (tests fake)

38 test methods total. ViewModel is primary coverage target.

---

# Part 3 — Complete Architecture

## Overall architecture

```
UI (Compose, commonMain)
  ↕ intents / state / effects
Presentation (ViewModel, MVI, commonMain)
  ↕ use cases
Domain (models, repository interface, use cases, commonMain)
  ↕
Data (Repository impl, API, Local DB, commonMain)
  ↕ expect/actual
Platform (drivers, token, theme, back handler)
```

## Why each choice


| Choice                    | Why                                                                   |
| ------------------------- | --------------------------------------------------------------------- |
| **Clean Architecture**    | Testability, clear boundaries, interviewer expectation for HoE role   |
| **MVI**                   | Unidirectional flow; explicit intents; one-shot effects for snackbars |
| **Repository**            | Single abstraction over API + DB; offline-first                       |
| **Use Cases**             | Thin but keeps ViewModel from touching repository for everything      |
| **Koin**                  | KMP-friendly; simpler than manual DI for project timeline           |
| **Shared UI**             | Challenge requirement; one UX on both platforms                       |
| **Shared ViewModel**      | Business logic + state in one place                                   |
| **KMP**                   | Core challenge; maximize code sharing                                 |
| **Compose Multiplatform** | Shared declarative UI                                                 |
| **Ktor**                  | KMP HTTP client; kotlinx.serialization integration                    |
| **SQLDelight**            | Type-safe SQL; excellent KMP support; `INSERT OR IGNORE` control      |
| **Coroutines + Flow**     | Async network/DB; reactive user list                                  |
| **StateFlow**             | Hot state for UI; lifecycle-aware collection                          |
| **Material 3**            | Modern UX; adaptive APIs; challenge UX criteria                       |


## Layer communication

- **UI → ViewModel:** `process(UserListIntent)`
- **ViewModel → UI:** `state: StateFlow`, `effects: Flow` (Channel)
- **ViewModel → Domain:** use case `invoke()`
- **Use cases → Data:** `UserRepository` interface
- **Repository:** orchestrates `UserApiService` + `UserLocalDataSource`
- **Local:** SQLDelight `Flow` from queries
- **DI:** Koin wires all layers at startup

---

# Part 4 — Folder-by-Folder Explanation

## Root


| Path                        | Purpose                                                     |
| --------------------------- | ----------------------------------------------------------- |
| `composeApp/`               | Single KMP module (Android app + iOS framework + JVM tests) |
| `iosApp/`                   | Thin SwiftUI shell embedding Compose UIViewController       |
| `gradle/libs.versions.toml` | Centralized dependency versions                             |
| `docs/screenshots/`         | README screenshots                                          |
| `.github/workflows/ci.yml`  | CI pipeline                                                 |


## `composeApp/src/commonMain/`

### `config/`

- `AppConfig.kt` — `expect val goRestToken`

### `data/local/`

- `UserLocalDataSource.kt` — SQLDelight wrapper; `observeUsers`, `insertOrIgnore`, `deleteById`

### `data/remote/`

- `UserDto.kt` — API DTOs + mapping to domain
- `UserApiService.kt` — Ktor calls to GoRest

### `data/repository/`

- `UserRepositoryImpl.kt` — offline-first orchestration

### `domain/model/`

- `User.kt` — `User`, `Gender`, `UserStatus`

### `domain/repository/`

- `UserRepository.kt` — interface + error types

### `domain/usecase/`

- `ObserveUsersUseCase`, `GetLastPageUsersUseCase`, `AddUserUseCase`, `DeleteUserUseCase`

### `domain/util/`

- `RelativeTimeFormatter.kt`

### `presentation/userlist/`

- `UserListState.kt`, `UserListIntent.kt`, `UserListEffect.kt`, `UserListViewModel.kt`

### `ui/screens/`

- `UserListScreen.kt`, `AddUserBottomSheet.kt`

### `ui/components/`

- `UserCard`, `UserAvatar`, `UserDetailPane`, `ShimmerUserCard`, `ErrorStateContent`, `EmptyStateContent`

### `ui/theme/`

- `Color.kt`, `Type.kt`, `Theme.kt` (expect AppTheme)

### `ui/util/`

- `AdaptiveUserListLayout.kt`, `LazyListStateExt.kt`, `PlatformBackHandler.kt` (expect)

### `di/`

- `AppModules.kt` — Koin modules

### `sqldelight/`

- `User.sq` — schema + queries

## `composeApp/src/androidMain/`

- `MainActivity.kt`, `UserManagerApp.kt`
- `AppConfig.android.kt`, `PlatformModule.android.kt`, `Theme.android.kt`, `PlatformBackHandler.android.kt`

## `composeApp/src/iosMain/`

- `MainViewController.kt`, `initKoinIos`
- Platform actuals + gitignored `Secrets.ios.kt`

## `composeApp/src/commonTest/` + `jvmTest/`

- ViewModel tests, formatter tests, SQLDelight integration test

---

# Part 5 — Every Important Class

## `UserListViewModel`

- **Responsibilities:** MVI reducer; subscribes to user Flow; load/refresh/add/delete; form validation
- **Dependencies:** 4 use cases (not repository directly — after PR fix)
- **Lifecycle:** `viewModelScope`; `observeUsers` collection in `init`
- **Design:** Effects via `Channel` for one-shot UI events
- **Improvement:** Extract regex to shared validator; typed network errors

## `UserRepositoryImpl`

- **Responsibilities:** Sync API ↔ DB; `runCatching` for Result types
- **Design:** DB is source of truth for UI; network mutates DB
- **Improvement:** Real `UserRepositoryImpl` unit tests with mocked API/DB

## `UserApiService`

- **Responsibilities:** HTTP only; pagination probe; 422 → ValidationException
- **Improvement:** Extract pagination to separate function; check POST status explicitly

## `UserLocalDataSource`

- **Responsibilities:** SQLDelight access; entity→domain mapping
- **Design:** `Dispatchers.Default` for writes (IO is JVM-only in common code)

## `FakeUserRepository`

- **Test double:** Configurable Results + `MutableStateFlow` for users

---

# Part 6 — Data Flow (Method-by-Method)

## Opening app

1. `UserManagerApp.onCreate` / `MainViewController` configure → `initKoin()`
2. `UserListScreen` composes → `koinViewModel<UserListViewModel>()`
3. `UserListViewModel.init`:
  - `observeUsers().collect { update users }`
  - if `goRestToken.isBlank()` → `MissingToken` error
  - else `loadUsers()`

## Loading users

1. `loadUsers()` → if `users.isEmpty()` set `isLoading=true`
2. `getLastPageUsers()` → `refreshLastPage()`
3. `fetchLastPage()` → GET page 1 → read header → GET last page
4. foreach dto → `insertOrIgnore`
5. SQLDelight Flow emits → `state.users` updated
6. `isLoading=false`, clear error on success

## Offline mode (warm cache)

1. `refreshLastPage()` fails
2. `classifyError()` → `NoInternet`
3. `users` unchanged (Flow still emitting cached data)
4. `InlineErrorBanner` shown

## Adding user

1. `SubmitAddUser` → validate → `addUserUseCase`
2. `apiService.createUser` POST
3. On 201: `insertOrIgnore` → Flow emits → user at top (ORDER BY addedAt DESC)
4. `UserAddedSuccess` effect → close sheet, `animateScrollToItem(0)`

## Deleting user

1. Long press → `RequestDelete` → dialog
2. `ConfirmDelete` → `pendingDeleteUser` + snackbar effect
3. Row hidden via `AnimatedVisibility`
4. Undo → clear `pendingDeleteUser` (row reappears)
5. Dismiss snackbar → `FinalizeDelete` → `deleteUser` API + `deleteById`

## Refreshing

1. Pull-to-refresh → `RefreshUsers`
2. `isRefreshing=true` → `refreshLastPage()` → `isRefreshing=false`

---

# Part 7 — API Layer

**Base URL:** `https://gorest.co.in/public/v2/users`


| Operation    | Method         | Success    | Notes                            |
| ------------ | -------------- | ---------- | -------------------------------- |
| List (probe) | GET `?page=1`  | 200        | Read `X-Pagination-Pages` header |
| List (last)  | GET `?page=N`  | 200        | Returns `List<UserDto>`          |
| Create       | POST JSON body | 201        | 422 → `ValidationException`      |
| Delete       | DELETE `/{id}` | 204 or 404 | Both treated as success          |


**Auth:** `Authorization: Bearer {token}` via Koin `defaultRequest`

**Ktor config:**

- JSON: `ignoreUnknownKeys = true`
- Timeout: 10s connect + request
- Retry: 3x on server errors, exponential backoff

**DTO mapping:** Gender/status strings → enums; `addedAt` supplied at mapping time (not from API)

---

# Part 8 — Local Database

**Schema:** `UserEntity(id PK, name, email, gender, status, addedAt TEXT ISO-8601)`

**Queries:**

- `selectAllOrderedByAddedAt` — UI list order
- `insertOrIgnore` — cache without overwriting `addedAt`
- `deleteById`
- `selectById` — test only

**Strategy:** Offline-first read; network refresh writes; no cache invalidation TTL

**Sync:** Not bidirectional sync — one-way API→DB on refresh, API+DB on add/delete

---

# Part 9 — ViewModel Deep Dive

## State (`UserListState`)

- `users`, `isLoading`, `isRefreshing`, `error`
- `showDeleteDialogFor`, `pendingDeleteUser`, `selectedUser`
- `formState: AddUserFormState`

## Intents (17 types)

See `UserListIntent.kt` — all UI actions as sealed interface

## Effects (3 types)

- `ShowDeleteSnackbar(user)`
- `ShowError(message)`
- `UserAddedSuccess`

## Coroutine scope

- `viewModelScope` for all async work
- `Channel.BUFFERED` for effects

## Loading semantics

- `isLoading`: cold empty load only
- `isRefreshing`: pull-to-refresh indicator

---

# Part 10 — Compose UI

## `UserListScreen`

- **State hoisting:** ViewModel owns business state; `showAddUserSheet` is local `rememberSaveable` (UI-only)
- **LaunchedEffect:** collects effects once
- **Scaffold:** TopAppBar + FAB + SnackbarHost
- **Branches:** shimmer / full error / content with pull-refresh

## `AddUserBottomSheet`

- `ModalBottomSheet`; form state from parent
- `imePadding` for keyboard

## Animations

- Delete row: `slideOutHorizontally + shrinkVertically`
- Shimmer: infinite gradient animation
- FAB: `ExtendedFloatingActionButton` collapses when scrolling down

## Dark mode

- Android: dynamic color API 31+ or static schemes
- iOS/common: `AppThemeBase` light/dark from `isSystemInDarkTheme()`

## Adaptive

- `ListDetailPaneScaffold` — list pane + detail pane
- Back handler pops detail on Android

---

# Part 11 — Kotlin Multiplatform

## Shared (`commonMain`)

- All UI, ViewModel, domain, data orchestration, SQLDelight schema

## Platform-specific


| Concern     | Android               | iOS                  | JVM                  |
| ----------- | --------------------- | -------------------- | -------------------- |
| Token       | `BuildConfig`         | `Secrets.ios.kt`     | `System.getProperty` |
| SQL driver  | `AndroidSqliteDriver` | `NativeSqliteDriver` | JDBC (tests only)    |
| HTTP engine | OkHttp                | Darwin               | N/A                  |
| Theme       | Dynamic Material You  | `AppThemeBase`       | `AppThemeBase`       |
| Back        | `BackHandler`         | no-op                | no-op                |


## Limitations

- No offline add/delete queue
- JVM target is for tests, not shipped desktop app
- iOS requires manual `Secrets.ios.kt`
- IDE Kotlin/Native version warning possible (doesn't block Android)

---

# Part 12 — Design Decisions (Complete List)


| Decision                     | Why                         | Alternative                | Tradeoff                                      |
| ---------------------------- | --------------------------- | -------------------------- | --------------------------------------------- |
| MVI over MVVM                | Explicit intents/effects    | MVVM with ad-hoc callbacks | More boilerplate                              |
| SQLDelight over Room         | Better KMP story            | Room KMP (newer)           | Manual SQL                                    |
| INSERT OR IGNORE             | Preserve addedAt            | UPSERT                     | Stale name/email on refresh if server changes |
| Deferred delete API          | Undo safety                 | Immediate delete           | Slightly delayed server sync                  |
| Channel for effects          | One-shot events             | SharedFlow replay          | Slightly more complex                         |
| `rememberSaveable` for sheet | Avoids config change bugs   | Intent to open sheet       | Split state ownership                         |
| Koin 4.1.1                   | KLIB ABI with Kotlin 2.1.21 | Koin 4.2.1                 | Older Koin                                    |
| Last page only               | Challenge spec              | Full sync                  | Incomplete directory                          |
| String error classification  | Fast to implement           | Sealed network errors      | Fragile                                       |
| `Dispatchers.Default` for DB | KMP common compatible       | `Dispatchers.IO`           | IO doesn't exist in common                    |
| Untyped adaptive navigator   | No Parcelable crash         | Typed navigator            | Less type safety                              |
| No offline write queue       | 1-day scope                 | WorkManager queue          | No offline add                                |


---

# Part 13 — AI-Assisted Decisions


| Area               | Likely AI-assisted | Good?              | Keep in prod?      |
| ------------------ | ------------------ | ------------------ | ------------------ |
| MVI boilerplate    | Yes                | Yes                | Yes                |
| Regex patterns     | Yes                | Mostly             | Review edge cases  |
| ProGuard rules     | Yes                | Needs verification | Test release build |
| Xcode pbxproj      | Yes                | Works              | Yes but fragile    |
| Test enumeration   | Yes                | Yes                | Yes                |
| Architecture       | Manual             | Yes                | Yes                |
| Delete/undo timing | Manual             | Yes                | Yes                |
| INSERT OR IGNORE   | Manual             | Yes                | Yes                |


**Interview line:** "AI accelerated boilerplate and test case discovery; every architectural decision was reviewed and several were revised — Koin version, delete timing, navigator typing."

---

# Part 14 — Performance

- **LazyColumn** with `key = { it.id }` — stable identity, less recomposition
- **Shimmer** only on empty cold load — avoids unnecessary animation
- **Flow observation** — DB pushes updates; no polling
- **INSERT OR IGNORE** — single write per user on refresh, no read-before-write
- **FAB collapse** — `derivedStateOf` on scroll direction
- **No image loading** — letter avatars (no network images)
- **Static framework on iOS** — faster startup vs dynamic (tradeoff: larger binary)

**Not optimized (acceptable):** full pagination, image caching, diffutil beyond Compose keys, R8 not verified end-to-end

---

# Part 15 — Security

- **Token:** Android `local.properties` → BuildConfig; iOS gitignored secrets file
- **Token guard:** Blank token → no network calls, clear error
- **Validation:** Client-side regex; server 422 as source of truth for conflicts
- **HTTPS:** GoRest over TLS
- **iOS ATS:** `NSAllowsArbitraryLoads` true in Info.plist (dev convenience — **would remove for prod**)
- **No secrets in repo:** `.gitignore` for `Secrets.*.kt`, `local.properties`
- **ProGuard:** Release obfuscation enabled

---

# Part 16 — Testing

## `RelativeTimeFormatterTest` (12 tests)

Boundary testing for seconds/minutes/hours/days/weeks + singular/plural + future guard

## `UserListViewModelTest` (18 tests)

Load, refresh, delete dialog, undo, finalize, add success/failure, 422 field errors, form reset

## `SqlDelightInsertOrIgnoreTest` (2 tests)

**Critical:** proves `addedAt` preserved on duplicate ID — validates core cache strategy

## `UserRepositoryImplTest` (6 tests)

Actually tests `FakeUserRepository` — **gap:** no real impl tests with mocked dependencies

## Uncovered

- `UserApiService` pagination logic
- `UserLocalDataSource` mapping edge cases
- UI/Compose tests
- Release ProGuard verification
- End-to-end integration tests

---

# Part 17 — Interview Questions (150)

> Format: **Q** / **Ideal Answer** / **Why asked** / **Mistake** / **Follow-up**

## Project Overview (1–10)

**Q1. What does this app do?**
**A:** Cross-platform user directory app using GoRest API with offline cache, add/delete, undo delete, adaptive layouts.
**Why:** Baseline understanding.
**Mistake:** Only describing Android.
**Follow-up:** Which requirements were hardest?

**Q2. Why KMP for this challenge?**
**A:** Maximize shared business logic and UI; demonstrate platform expertise.
**Mistake:** "Because it was required" without technical reasoning.

**Q3. What would you demo first?**
**A:** User list with relative time → add user → delete with undo → rotate to landscape for adaptive layout.

**Q4. What's NOT implemented?**
**A:** Full pagination sync, offline write queue, login, multi-screen navigation.

**Q5. How long would this take without KMP?**
**A:** ~2x for duplicate UI + ViewModel + validation on each platform.

**Q6. Production readiness score?**
**A:** Strong prototype; needs analytics, crash reporting, release QA, offline queue for writes.

**Q7. Biggest technical risk?**
**A:** KMP dependency version matrix; iOS native klib ABI mismatches.

**Q8. Why GoRest?**
**A:** Challenge spec; free public API with pagination and CRUD.

**Q9. How do you handle config?**
**A:** expect/actual `goRestToken` per platform.

**Q10. What makes the UX stand out?**
**A:** Shimmer, undo delete, adaptive layout, collapsing FAB, Material 3 polish.

## Architecture (11–25)

**Q11. Explain Clean Architecture layers.**
**A:** UI → Presentation → Domain → Data; dependencies point inward.

**Q12. Why use cases if they're one-liners?**
**A:** ViewModel depends on operations not repository; room to grow; test seams.

**Q13. Why Repository pattern?**
**A:** Single API for UI layer; hides API+DB coordination.

**Q14. Could ViewModel call Repository directly?**
**A:** Yes for small apps; use cases preferred for consistency and HoE-scale codebases.

**Q15. Where is business logic?**
**A:** ViewModel (validation, error mapping, delete timing) + Repository (sync policy).

**Q16. Why sealed interfaces for Intent/Effect?**
**A:** Exhaustive when expressions; compile-time safety.

**Q17. Data flow direction?**
**A:** Unidirectional: Intent up, State down, Effect sideways.

**Q18. Why Channel not SharedFlow for effects?**
**A:** No replay; each effect consumed once.

**Q19. Single module vs multi-module?**
**A:** Single module for single-module layout; would split :core :feature :app for production.

**Q20. How would you scale to 10 features?**
**A:** Feature modules, navigation graph, per-feature ViewModels.

**Q21. CQRS applicable?**
**A:** Overkill here; refresh/add/delete are simple commands.

**Q22. Event sourcing?**
**A:** No; SQLDelight is CRUD cache not event log.

**Q23. Dependency rule violated anywhere?**
**A:** Domain defines ValidationException used by data layer — pragmatic coupling.

**Q24. Hexagonal architecture mapping?**
**A:** Ports: UserRepository; Adapters: UserApiService, UserLocalDataSource.

**Q25. Why not TCA/MVI framework library?**
**A:** Custom MVI is enough; avoid dependency weight.

## MVI (26–40)

**Q26. MVI vs MVVM here?**
**A:** MVI: explicit intents, immutable state, sealed effects. MVVM would use methods like `onDeleteClicked()`.

**Q27. Is this "pure" MVI?**
**A:** Pragmatic MVI; some UI state (sheet visibility) is local Compose state.

**Q28. What's in State vs Effect?**
**A:** State: renderable; Effect: one-shot (snackbar, scroll).

**Q29. Why is `showAddUserSheet` not in ViewModel?**
**A:** Pure UI concern; `rememberSaveable` survives rotation without ViewModel boilerplate.

**Q30. How does Undo work in MVI terms?**
**A:** ConfirmDelete → state `pendingDeleteUser` + effect snackbar; Undo → intent clears pending.

**Q31. Reducer pattern?**
**A:** `process()` is the reducer; `_state.update { }` applies transitions.

**Q32. Race condition in delete?**
**A:** API deferred until snackbar dismissed without Undo.

**Q33. Idempotent intents?**
**A:** `SubmitAddUser` guards `isSubmitting`; `FinalizeDelete` guards null pending.

**Q34. State restoration?**
**A:** `rememberSaveable` for sheet; ViewModel state lost on process death (would use SavedStateHandle in prod).

**Q35. Multiple collectors on state?**
**A:** StateFlow is safe; single collector in screen.

**Q36. Effect lost on rotation?**
**A:** Possible if rotation during snackbar; acceptable for challenge.

**Q37. Testing MVI?**
**A:** Assert state after intents; Turbine for effects.

**Q38. Intent naming convention?**
**A:** Verb-noun: `ConfirmDelete`, `UpdateFormEmail`.

**Q39. Could effects be intents?**
**A:** Anti-pattern; effects are outputs not inputs.

**Q40. MVI on iOS without ViewModel?**
**A:** Could use shared presenter; we use AndroidX ViewModel KMP artifact.

## Compose (41–55)

**Q41. Why collectAsState not collectAsStateWithLifecycle?**
**A:** Broader KMP compatibility across targets.

**Q42. State hoisting example?**
**A:** Form state in ViewModel; sheet visibility local.

**Q43. remember vs rememberSaveable?**
**A:** Saveable survives config change via Bundle.

**Q44. LaunchedEffect(Unit) for effects — why?**
**A:** Start collection once when screen enters composition.

**Q45. derivedStateOf in FAB?**
**A:** Recompute scroll direction only when scroll state changes.

**Q46. PullToRefreshBox?**
**A:** Material3 experimental pull-to-refresh wrapper.

**Q47. Why ExtendedFAB?**
**A:** UX polish; collapses to icon on scroll down.

**Q48. ModalBottomSheet back handling?**
**A:** Swipe down, tap outside, Android BackHandler.

**Q49. combinedClickable?**
**A:** Tap select, long-press delete on same row.

**Q50. AnimatedVisibility for delete?**
**A:** Hides row immediately while undo pending.

**Q51. Recomposition triggers on user list?**
**A:** `state.users` change, `pendingDeleteUserId`, selection.

**Q52. Stable keys in LazyColumn?**
**A:** `key = { it.id }` for animation and scroll position.

**Q53. Preview support?**
**A:** uiToolingPreview dependency; previews not heavily used.

**Q54. Theme architecture?**
**A:** expect AppTheme; Android dynamic color; common AppThemeBase.

**Q55. Accessibility?**
**A:** contentDescription on avatars/FAB; 72dp min row height.

## Compose Multiplatform (56–65)

**Q56. What's shared vs platform UI?**
**A:** 100% screens/components in commonMain; only theme/back/token differ.

**Q57. iOS embedding?**
**A:** ComposeUIViewController in SwiftUI UIViewControllerRepresentable.

**Q58. Main thread on iOS Compose?**
**A:** Compose MP handles; Koin init in configure block.

**Q59. CADisableMinimumFrameDurationOnPhone?**
**A:** Required plist key for ProMotion; app crashes without it.

**Q60. iOS SQLite linking?**
**A:** `-lsqlite3` in Xcode OTHER_LDFLAGS for SQLDelight native driver.

**Q61. Compose resources?**
**A:** Dependency present; icons use materialIconsExtended.

**Q62. Platform differences in UX?**
**A:** Android back button; iOS swipe back; same UI code.

**Q63. Desktop target?**
**A:** JVM for tests only; not productized.

**Q64. CMPS vs Flutter?**
**A:** Native UI feel, share with existing Kotlin team skills.

**Q65. Binary size concern?**
**A:** Static iOS framework; Compose runtime adds size.

## Kotlin / Coroutines / Flow (66–80)

**Q66. Why StateFlow not LiveData?**
**A:** KMP; coroutines-first.

**Q67. Cold vs hot Flow?**
**A:** DB observation is cold Flow converted hot via collection; StateFlow for UI state.

**Q68. viewModelScope behavior?**
**A:** Cancelled when ViewModel cleared.

**Q69. Dispatchers.Default for DB?**
**A:** Dispatchers.IO unavailable in commonMain.

**Q70. runCatching in Repository?**
**A:** Converts exceptions to Result for ViewModel.

**Q71. Instant vs Long for timestamps?**
**A:** kotlinx-datetime Instant is multiplatform type-safe.

**Q72. sealed interface vs sealed class?**
**A:** Intent/Effect as interfaces allow data objects/classes implementations.

**Q73. Result type usage?**
**A:** Explicit success/failure without try/catch in ViewModel.

**Q74. Turbine in tests?**
**A:** Asserts Flow emissions (effects) with timeouts.

**Q75. UnconfinedTestDispatcher why?**
**A:** Immediate execution for ViewModel tests.

**Q76. Structured concurrency violation risks?**
**A:** None obvious; all work in viewModelScope.

**Q77. Flow collection in init?**
**A:** Long-lived collection updates users reactively.

**Q78. Operator fun invoke on use cases?**
**A:** Callable syntax `getLastPageUsers()`.

**Q79. fun interface for use cases?**
**A:** SAM-like; easy mocking.

**Q80. Exception vs Result?**
**A:** Result at repository boundary; ValidationException for typed 422.

## Koin (81–90)

**Q81. Why Koin over Hilt?**
**A:** Hilt is Android-only; Koin works in commonMain.

**Q82. viewModel {} DSL?**
**A:** From koin-compose-viewmodel; integrates with Compose.

**Q83. Platform modules?**
**A:** androidModule/iosModule provide SqlDriver + HttpClientEngine.

**Q84. Single vs factory for use cases?**
**A:** single — stateless, one instance.

**Q85. Koin 4.1.1 downgrade reason?**
**A:** KLIB ABI mismatch with Kotlin 2.1.21 on iOS.

**Q86. HttpClientEngine injection?**
**A:** Platform provides OkHttp/Darwin; common builds HttpClient.

**Q87. Testing with Koin?**
**A:** Tests bypass Koin; construct ViewModel with fakes directly.

**Q88. initKoin appDeclaration lambda?**
**A:** Android passes androidContext and logger.

**Q89. startKoin twice risk?**
**A:** Guarded by single Application/ViewController init.

**Q90. Koin Compose koinViewModel() scope?**
**A:** Default ViewModelStore owner from LocalViewModelStoreOwner.

## Ktor (91–100)

**Q91. Why Ktor over Retrofit?**
**A:** Retrofit not KMP; Ktor is multiplatform.

**Q92. ContentNegotiation?**
**A:** kotlinx.serialization JSON codec.

**Q93. defaultRequest Bearer token?**
**A:** Centralized auth header.

**Q94. HttpRequestRetry config?**
**A:** 3 retries, exponential backoff, server errors only.

**Q95. How fetch last page?**
**A:** GET page 1 → read X-Pagination-Pages → GET page N.

**Q96. Double request cost?**
**A:** Acceptable; could cache page count.

**Q97. 422 handling?**
**A:** Parse body to ApiValidationError list → ValidationException.

**Q98. DELETE 404 success?**
**A:** Idempotent delete — already gone is fine.

**Q99. Timeout values?**
**A:** 10 seconds connect and request.

**Q100. Logging?**
**A:** Napier on Android; Ktor Logging plugin not added.

## Repository / Offline / Caching (101–115)

**Q101. Source of truth?**
**A:** SQLDelight local DB for UI.

**Q102. INSERT OR IGNORE why?**
**A:** Preserve original addedAt when same id re-fetched.

**Q103. UPSERT alternative?**
**A:** Would overwrite addedAt on every refresh.

**Q104. Offline read behavior?**
**A:** Cached users visible; error banner.

**Q105. Offline add?**
**A:** Blocked with error message — no queue.

**Q106. Stale data on refresh failure?**
**A:** By design — availability over freshness.

**Q107. Cache invalidation?**
**A:** None/TTL; manual refresh only.

**Q108. Ordering?**
**A:** addedAt DESC — new adds appear top.

**Q109. Sync strategy name?**
**A:** Network-first write, DB-first read.

**Q110. Conflict resolution?**
**A:** Server wins on add (201); local undo before delete finalized.

**Q111. Empty cache offline?**
**A:** Full-screen No Internet error.

**Q112. Token missing offline?**
**A:** MissingToken error; no API calls attempted.

**Q113. observeUsers vs refresh?**
**A:** Observe is passive reactive; refresh is active network fetch.

**Q114. delete local before or after API?**
**A:** After API success — if API fails, user stays.

**Q115. Optimistic delete UI?**
**A:** Row hidden while pendingDeleteUser set, before API.

## Database (116–120)

**Q116. Why SQLDelight over Room?**
**A:** Mature KMP path; explicit SQL for INSERT OR IGNORE.

**Q117. Schema migrations?**
**A:** Not implemented — v1 only.

**Q118. asFlow + mapToList?**
**A:** SQLDelight coroutines extension for reactive queries.

**Q119. JvmTest SQLDelight?**
**A:** JdbcSqliteDriver in-memory for real SQL semantics.

**Q120. Gender/status as TEXT?**
**A:** Simple; mapped to enums in Kotlin.

## Undo / Delete (121–130)

**Q121. Walk through delete flow.**
**A:** Long press → dialog → confirm → pending + snackbar → undo or finalize.

**Q122. Why not delete immediately?**
**A:** Undo requirement; race with API.

**Q123. Snackbar duration?**
**A:** SnackbarDuration.Long.

**Q124. What if user rotates during snackbar?**
**A:** pendingDeleteUser in ViewModel survives rotation.

**Q125. Double finalize guard?**
**A:** `pendingDeleteUser ?: return`.

**Q126. API failure on finalize?**
**A:** ShowError effect; pending cleared.

**Q127. Block delete during pending?**
**A:** Long press ignored if pendingDeleteUser != null.

**Q128. Animation on undo?**
**A:** AnimatedVisibility shows row again when pending cleared.

**Q129. Server 204 expectation?**
**A:** GoRest returns 204 No Content on delete.

**Q130. Local delete timing?**
**A:** After successful API in repository.

## Adaptive UI (131–135)

**Q131. Which API for adaptive?**
**A:** material3-adaptive 1.2.0: ListDetailPaneScaffold + navigation artifact.

**Q132. Why no type param on navigator?**
**A:** Android back stack requires Parcelable for typed navigator — User isn't Parcelable.

**Q133. selectedUser storage?**
**A:** ViewModel state, not navigator back stack.

**Q134. Portrait behavior?**
**A:** List pane only; detail on navigate.

**Q135. PlatformBackHandler role?**
**A:** Android back pops detail pane + clears selection.

## Testing / Performance / Security (136–150)

**Q136. Test pyramid here?**
**A:** Many unit tests; one SQL integration; no UI tests.

**Q137. Fake vs Mock?**
**A:** Hand-written FakeUserRepository for clarity.

**Q138. Missing test coverage?**
**A:** UserApiService pagination, real RepositoryImpl, Compose UI.

**Q139. R8 risk?**
**A:** Serialization/Ktor keep rules added; needs release smoke test.

**Q140. CI pipeline?**
**A:** jvmTest + testDebugUnitTest + assembleDebug on Ubuntu.

**Q141. Shimmer performance?**
**A:** Only 8 items on cold load; infinite animation but limited count.

**Q142. List performance 1000 users?**
**A:** LazyColumn handles; no pagination in UI.

**Q143. Token security?**
**A:** Not in git; BuildConfig/secrets file.

**Q144. ATS arbitrary loads?**
**A:** Dev convenience; remove for production.

**Q145. Input validation bypass?**
**A:** Server 422 catches bad data.

**Q146. ProGuard and Koin?**
**A:** keep rules for org.koin.**

**Q147. How increase coverage?**
**A:** MockEngine for Ktor; RepositoryImpl tests; Compose uiTest.

**Q148. Relative time test strategy?**
**A:** Fixed instants from Clock offset; boundary values.

**Q149. Flaky test risk?**
**A:** Low with UnconfinedTestDispatcher; time tests use relative offsets.

**Q150. What test proves cache strategy?**
**A:** SqlDelightInsertOrIgnoreTest — duplicate id preserves addedAt.

---

# Part 18 — Code Review (Production Lens)

## Strengths

- Clear layer separation
- Offline-first read path
- Delete/undo race handled correctly
- KMP expect/actual used minimally and correctly
- Real SQLDelight integration test
- CI pipeline
- README documents tradeoffs

## Weaknesses

- `UserRepositoryImplTest` tests fake not impl
- Network error classification via string matching
- No offline mutation queue
- No SavedStateHandle for process death
- ATS allows arbitrary loads on iOS
- Release build not verified with R8

## Potential bugs

- Process death during snackbar → delete may not finalize
- Refresh doesn't update name/email for existing IDs (INSERT OR IGNORE)
- Token check only at init — token added later needs rebuild

## Improvements

- Typed network error sealed class
- MockEngine API tests
- SavedStateHandle for pending delete
- Remove ATS arbitrary loads
- Pagination UI

---

# Part 19 — If I Had Another Week


| Priority | Item                                           | Impact                 |
| -------- | ---------------------------------------------- | ---------------------- |
| P0       | Release build QA with R8                       | Production credibility |
| P0       | Real UserRepositoryImpl tests                  | Test confidence        |
| P1       | Ktor MockEngine tests for pagination/422       | API correctness        |
| P1       | SavedStateHandle for process death             | UX reliability         |
| P2       | Offline add queue with WorkManager             | Offline writes         |
| P2       | Update stale fields on refresh (UPSERT policy) | Data accuracy          |
| P3       | Compose UI tests for delete flow               | Regression safety      |
| P3       | Analytics + crash reporting hooks              | Production ops         |
| P4       | Multi-module split                             | Scale                  |


---

# Part 20 — Elevator Pitch

"Let me walk you through the architecture.

I built a Kotlin Multiplatform user management app for Android and iOS with a single Compose Multiplatform UI. The goal was to demonstrate clean architecture, offline-first behavior, and UX polish within a project scope.

**Architecture:** I used MVI in the presentation layer. The UI sends sealed `UserListIntent`s to `UserListViewModel`, which exposes immutable `UserListState` via StateFlow and one-shot `UserListEffect`s through a Channel — things like snackbars shouldn't live in persistent state.

Below that, I have thin use cases — `ObserveUsers`, `GetLastPageUsers`, `AddUser`, `DeleteUser` — sitting on a `UserRepository` interface. The implementation coordinates Ktor API calls with a SQLDelight local cache. The database is the **source of truth for the UI**: the ViewModel subscribes to a Flow from SQLDelight, so the list updates reactively whenever the cache changes.

**Smart feed:** GoRest paginates users. The project for the **last page**, so `UserApiService` does a probe request to page 1, reads the `X-Pagination-Pages` header, then fetches that last page. On refresh, I insert each user with `INSERT OR IGNORE` — that's a deliberate choice. A normal upsert would overwrite `addedAt` every refresh, making relative timestamps meaningless. `INSERT OR IGNORE` means the first time we see a user locally, we record when we saw them, and that timestamp persists.

**Relative time** is computed in shared Kotlin via `kotlinx-datetime` — singular/plural grammar, negative duration guard, buckets up to weeks. It's honest that these are local cache times, not server creation times, because GoRest doesn't expose created-at.

**Offline:** If the network fails on launch with an empty cache, you get a full-screen error with retry. If you have cached data, you still see the list with an inline banner — availability over freshness. Add and delete require network by policy — I documented that tradeoff rather than building a half-baked offline queue in one day.

**Add user:** FAB opens a Material 3 bottom sheet. Validation runs on keystroke and on submit. Gender and status are required by GoRest — the form includes them. On 422, I parse field errors into `ValidationException` and map them back to individual field errors. On success, SQLDelight Flow pushes the new user to the top and we scroll there.

**Delete with undo:** This was the trickiest UX piece. On confirm, I set `pendingDeleteUser` and hide the row with animation, then show a snackbar. I intentionally **do not** call the DELETE API yet. Only when the snackbar is dismissed without tapping Undo do I fire `FinalizeDelete`, which calls the API and removes from DB. That eliminates the race where Undo arrives after the server already deleted the user.

**Adaptive layout:** I use Material3 Adaptive `ListDetailPaneScaffold`. In landscape or on wide screens, you get list plus detail. I keep `selectedUser` in ViewModel state and use an untyped navigator — typing it with `User` caused Android Parcelable crashes on back stack save.

**KMP specifics:** Platform code is minimal — SQL driver, HTTP engine, API token via expect/actual, and `PlatformBackHandler` on Android. iOS embeds `ComposeUIViewController` in SwiftUI. I hit real KMP issues: Koin 4.2.1 had KLIB ABI mismatch with Kotlin 2.1.21 on iOS, and `material3-adaptive-navigation` is a separate artifact from the layout library.

**Testing:** ViewModel tests with a hand-written fake repository and Turbine for effects. A JVM SQLDelight test proves `INSERT OR IGNORE` semantics with a real in-memory SQLite — that test exists because mocks wouldn't catch a wrong SQL policy.

**If I had more time:** real repository tests with MockEngine, SavedStateHandle for process death, release build verification with R8, and an offline write queue.

That's the system — happy to go deeper on any layer."

---

