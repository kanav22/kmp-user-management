# MVI, Offline-First, and KMP: Architecture Decisions That Survive Production

**Kanav Wadhawan** · Principal Android Engineer  
*Companion to [kmp-user-management](https://github.com/kanav22/kmp-user-management)*

---

## The problem

User-management flows look simple in a demo: fetch a list, show details, handle errors. In production they fail in predictable ways:

- Network drops mid-scroll while the user still expects cached data
- Token misconfiguration causes silent 401 loops
- iOS and Android teams diverge on state handling
- UI tests pass while integration paths regress

This article documents the decisions in a Kotlin Multiplatform reference app built to survive those failures—not win a hackathon.

---

## Decision 1: MVI over ad-hoc MVVM

**Choice:** Unidirectional data flow with explicit `UiState`, `UiEvent`, and `UiEffect`.

**Why:** MVVM without discipline becomes "state everywhere." MVI forces every user action through a single reducer path, which makes:

- Compose recomposition predictable
- ViewModel tests deterministic (event in → state out)
- Code review faster ("where does this state change?")

**Tradeoff:** More boilerplate for trivial screens. For a list+detail flow with pagination and error recovery, the upfront cost pays back in testability.

```kotlin
// Pattern: events are nouns, state is immutable, effects are one-shot
data class UserListUiState(
    val items: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

---

## Decision 2: SQLDelight for shared offline storage

**Choice:** SQLDelight in `commonMain` instead of Room (Android-only).

**Why:**

- **Single schema** across Android and iOS
- **Type-safe queries** generated at compile time
- **`INSERT OR IGNORE`** semantics for idempotent sync without duplicate rows

**Tradeoff:** SQLDelight's KMP story requires JVM tests for query verification (see `jvmTest` in the repo). Room would be faster to scaffold on Android-only projects.

**Production lesson:** Define your sync contract at the SQL layer first. UI state should reflect *what's in the database*, not what's in memory.

---

## Decision 3: Cache-first, not network-first

**Choice:** Repository returns local data immediately; network refresh updates the cache in the background.

**Why:** Users on mobile networks experience latency spikes. Fintech and consumer apps that blank the screen on every refresh feel broken.

**Implementation signals in the repo:**

- Local data source owns the truth for UI
- Network errors surface as banners, not full-screen wipes
- Missing API token fails loudly with a README-linked message—no silent retry loops

---

## Decision 4: Ktor in commonMain

**Choice:** Ktor client with platform engines (`Android`, `Darwin`) instead of Retrofit in shared code.

**Why:** Retrofit is JVM/Android-centric. Ktor keeps HTTP in `commonMain` with expect/actual only where needed (token storage).

**Tradeoff:** Smaller ecosystem than Retrofit + OkHttp interceptors on Android. For KMP, portability wins.

---

## Decision 5: CI as an architecture gate

**Choice:** GitHub Actions runs JVM tests, Android unit tests, and debug builds on every PR.

**Why:** Principal engineers don't just write architecture—they **enforce** it. CI catches:

- Broken SQLDelight migrations
- ViewModel regressions
- Gradle configuration drift

If it's not in CI, it's not a standard.

---

## What I'd do differently at scale

| At startup scale | At org scale |
|----------------|--------------|
| Single `composeApp` module | Extract `core:network`, `core:database` libraries |
| Fake repositories | Contract tests against API schemas |
| Manual token in `local.properties` | Secure enclave + remote config |
| JVM + Android tests | Add screenshot/golden tests per platform |

---

## Summary

Production mobile architecture is about **boundaries**:

- **MVI** for UI state discipline
- **SQLDelight** for shared offline truth
- **Cache-first** for resilient UX
- **CI** for enforcement

The code is in [kmp-user-management](https://github.com/kanav22/kmp-user-management). The patterns transfer directly to fintech Android monorepos—replace the user API with your domain, keep the boundaries.

**Publish-ready copy:** [Medium/Dev.to draft](https://github.com/kanav22/kanav22/blob/main/docs/publishing/kmp-mvi-offline-first-medium.md)

---

*Questions or improvements? Open a PR or reach out on [LinkedIn](https://www.linkedin.com/in/kanav-wadhawan/).*
