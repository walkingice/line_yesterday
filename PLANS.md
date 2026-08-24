# Implementation Plan

This document breaks down the features specified in the root-level `SPEC.md` into
work that can be implemented, tested, and reviewed incrementally. Start each Phase
only after the previous Phase has passed its tests to avoid changing the UI, state
management, and data sources at the same time.

## Implementation Principles

- `SPEC.md` is the single source of truth for product behavior. If this document
  conflicts with the SPEC, update and confirm the SPEC before changing the
  implementation plan.
- Use the names `FeedActivity`, `FeedFragment`, `FeedViewModel`,
  `FavoritesActivity`, `FavoritesFragment`, `FavoritesViewModel`,
  `DetailActivity`, `DetailFragment`, and `DetailViewModel`.
- Use `FavoriteStore`, `FavoritesRepository`, and `FreshnessValidator`. Do not use
  the incorrect names from the draft, such as `FavoritesStore`,
  `FavoritestRepository`, or `FreshnessValidatoer`.
- Use English for code, comments, and commit messages. Use Taiwan Traditional
  Chinese for documentation and explanations during development, while keeping
  technical terms in English.
- Each commit should complete only one short goal. If a change exceeds 100 lines
  and spans multiple files, split it into steps such as model, storage, behavior,
  and test.
- Every production code change must have corresponding local JVM tests. The SPEC
  does not require a complete UI test suite, but Activity, Fragment, and navigation
  still need focused Robolectric local JVM tests. Do not add emulator/instrumentation
  tests.
- Before each change is committed, have a sub-agent perform code review and run the
  applicable tests. Fix any findings, then run review again.
- Do not call `System.currentTimeMillis()` directly, hard-code network state, use
  real delays, or depend on real time in tests.
- Functions should have a single responsibility. When a function exceeds 50 lines,
  first consider extracting a pure helper or a smaller collaborator.
- Do not run `git push` or `git branch -D`.
- Begin commit subjects with a type such as `feat:`, `fix:`, `test:`, `docs:`, or
  `refactor:`. Limit subjects to 50 characters and body lines to 72 characters.
- The commit body must explain the intent, changes, reasons, and behavior changes,
  followed by a `Coding-Assistant: <Agent-Tool-Name> <version> (<Model name>)`
  trailer.

## Confirmed Design Decisions

### Feed Merge Order

Keep the complete sequence for each source separately, then merge them in this
fixed order:

```text
DummyJson[0], SpaceFlight[0], DummyJson[1], SpaceFlight[1], ...
```

If one source is exhausted first, append the remaining items from the other source
in their original order. Use `(FeedSource, id)` as the identity for deduplication;
do not compare only `id`.

### Initial Load

When `FeedFragment` opens for the first time, it automatically requests the first
page. Show a full-screen Loading state when no content exists yet. Once content
exists, show the load-more state through the state button at the bottom of the
list.

### Cursor

Repository and ViewModel do not assume that every source uses integer page
numbers. Both sources use an opaque `PageCursor`, which can initially be
implemented with a `String`. DummyJson Client may interpret the cursor as `skip`,
while SpaceFlight Client may interpret it as `offset` or a fixture identifier.

Define the initial cursor for both sources as `PageCursor("0")`; use the raw string
inside the cursor as the cache key. Always use the `nextCursor` produced by the
response. ViewModel must not increment it on its own.

`FeedPageResult` must return items, `nextCursor`, and `isExhausted`. Mark a source
as exhausted only when the response explicitly indicates that no next page exists.

### Cache Write Failure

New JSON returned by Client may be written to cache only after it has been parsed
and converted successfully. Cache updates must use atomic upsert and must not
delete old data first.

If the response can be converted to domain data but the cache write fails, the new
data can still be displayed for this request and the cursor may advance. The old
cache must remain available. Repository records the cache write failure as
`cacheWarning`; it does not turn this content load into Error or include the
warning in the Feed footer failure matrix.

If the first-page cache transaction during refresh fails, the corresponding source
Repository internally sets `requiresNetworkRecovery`. While the flag exists,
subsequent pages use `NETWORK_ONLY_RECOVERY` mode: do not read, fall back to, or
write that source's Feed page cache. Use only Client responses to update the
current in-memory sequence. Clear the flag only after the first-page transaction
of a later refresh succeeds. This prevents a failed transaction from mixing page
generations before and after refresh in persistent cache or on screen.

### Feed Cache After Refresh

When a source is refreshed successfully, JsonCacheStore writes the new first page
and then deletes other Feed cache cursors for that source in the same transaction.
Commit only if the entire transaction succeeds. If it fails, preserve all old
cache for that source and apply the non-fatal warning and
`requiresNetworkRecovery` rules described above. Never clear cache before refresh
begins.

### Unparseable Cache

Regardless of whether cache is still within the freshness window, JSON that cannot
be parsed cannot be displayed or treated as a successful result. Repository must
preserve that cache, attempt Client, and replace the old data only after the new
response is parsed successfully and written to cache successfully. If Client also
fails, return Offline or Error according to the Client result.

### Cache Read Failure

A Store read failure means there is no usable cache, but it does not prevent
Repository from trying Client. If Client succeeds and the response can be parsed,
display the new data. If the subsequent cache write still fails, record only a
`cacheWarning`. If Client also fails, the load failure includes both Storage and
Client errors, so use `Error` overall. Use `Offline` only when every actual load
failure is Offline.

### Stale Feed Cache

When Repository reads parseable but expired Feed cache, it attempts Client first:

- Client succeeds: return and cache the new data.
- Client fails, but the stale page is not yet in the ViewModel's source sequence:
  return stale items with failure metadata so ViewModel can display the content,
  but do not advance the cursor.
- Client fails, and the stale page is already in the current sequence: preserve the
  existing sequence and cursor.

For load more, if at least one new identity is actually added, the overall footer
state returns to `Ready`. Only when no new identity is added and at least one
non-exhausted source fails should the failure matrix select `Offline` or `Error`.

### Detail Stale-While-Revalidate

Detail Repository uses `Flow<DetailLoadEvent>` to express the two-stage result:

1. When parseable cache exists, first emit `Cached(detail, isStale)`.
2. Stop after fresh cache and do not call Client.
3. When stale cache exists, continue by calling Client. On success, emit
   `Updated(detail)`; on failure, emit `RefreshFailed(error)` while the screen keeps
   displaying the stale detail.
4. On cache miss or unparseable cache, call Client. On success, emit
   `Updated(detail)`; on failure, emit `LoadFailed(error)` and show a retryable
   Offline or Error state.

## Data Layer Responsibilities

Data is obtained from Client or JsonCacheStore as raw JSON. Only Repository may
parse the API schema and convert it to a domain model:

```text
Client ─────────────┐
                    ├─ raw JSON → Repository → API DTO → Domain Model
JsonCacheStore ─────┘
```

### Client

- Both the Client interface and mock implementation return only raw JSON, not API
  DTOs or domain models.
- Client encapsulates endpoint/asset selection, network state checks, and simulated
  latency.
- Every call in mock client first uses coroutine `delay(1000)` to simulate latency, then checks
  `NetworkStatusProvider`. Therefore, offline calls also have simulated latency but
  do not read assets. Production uses coroutine delay; unit tests use an
  immediately completing or controllable fake.
- Client does not use Room, read cache, perform Gson parsing, or decide freshness.
- Client uses a sealed result that distinguishes at least `Success(rawJson)`,
  `Offline`, and `Failure(cause)`.

Suggested interfaces:

```kotlin
interface DummyJsonClient {
    suspend fun getProducts(cursor: PageCursor): ClientResult
    suspend fun getProduct(id: String): ClientResult
}

interface SpaceFlightClient {
    suspend fun getArticles(cursor: PageCursor): ClientResult
    suspend fun getArticle(id: String): ClientResult
}
```

### JsonCacheStore

- Store saves and returns only raw JSON, timestamp, cache type, and cache key.
- Store treats JSON as an opaque `String`; it does not use Gson or know the API
  schema.
- `(type, key)` has a unique index so only one valid cache entry exists at each
  location.
- `put` uses atomic upsert. If the write fails, the existing cache remains readable.
- A successful refresh uses a transaction to write the first page and delete other
  Feed pages for the same source. If the transaction fails, preserve every old
  page.
- Feed pages use the source cursor as the key; Detail uses the item id as the key.
- API cache and the Favorite table are completely independent. Clearing API cache
  must not affect Favorite.

Suggested interface:

```kotlin
data class JsonCacheEntry(
    val rawJson: String,
    val timestamp: Long,
)

interface JsonCacheStore {
    suspend fun get(type: CacheType, key: String): JsonCacheEntry?
    suspend fun put(
        type: CacheType,
        key: String,
        rawJson: String,
        timestamp: Long,
    )
    suspend fun delete(type: CacheType, key: String)
    suspend fun replaceFeedPages(
        type: CacheType,
        firstPageKey: String,
        rawJson: String,
        timestamp: Long,
    )
}
```

### Repository

- Repository is the only layer that uses Gson API DTOs and mappers.
- Repository first selects fresh cache, stale cache, forced refresh, or Client,
  then parses the raw JSON into a DTO, validates required fields, and finally
  converts it to a domain model.
- Repository exposes only domain models, cursors, exhausted state, and defined
  errors to ViewModel. It does not expose raw JSON, Gson DTOs, Room Entities, or
  DAOs.
- Repository handles cache freshness, parse failure, cache write failure, and stale
  fallback. ViewModel does not operate cache directly.
- DummyJsonRepository and SpaceFlightRepository each handle their own Feed and
  Detail schemas. DetailRepository routes requests to the correct source according
  to `FeedSource`.
- FavoritesRepository depends only on `FavoriteStore`, not API cache. It handles
  toggle, querying, pagination, and domain mapping of Favorite snapshots.

### DTOs, Entities, and Domain Models

- API DTOs fully represent the fixture schema and exist only in the data layer.
- Room Entities represent only the database schema and are not used directly by
  the UI.
- Domain models are the only data types used by ViewModel and UI.
- DTO mappers and Entity mappers are pure functions. Test valid data, missing
  required fields, empty lists, and unsupported values separately.

### ViewModel

- ViewModel calls only Repository, not Client, Store, DAO, or Gson.
- ViewModel keeps each Feed source's items, cursor, and exhausted state separately,
  then produces the merged UI list.
- ViewModel exposes screen state as an immutable `StateFlow` and prevents refresh
  and load more from running concurrently.
- The coroutine dispatcher must be injectable so concurrency and duplicate-action
  tests remain deterministic.

### UI

- Fragment only sends user actions to ViewModel and renders the UI according to
  `StateFlow`.
- Activities pass only `FeedSource` and item id between one another, not complete
  items, DTOs, or JSON.
- Fragment uses lifecycle-aware collection. Recreating the screen must not produce
  duplicate data requests.

## Shared State and Error Contracts

Define the following types before starting Repository so the two sources do not
invent different semantics:

- `FeedSource`: `DUMMY_JSON`, `SPACE_FLIGHT`.
- `PageCursor`: a value class or data class that wraps an opaque `String`.
- `DataError`: includes at least `Offline`, `Client`, `Parse`, and `Storage`.
- `FeedPageResult`: contains domain items, `nextCursor`, `isExhausted`, `isStale`,
  optional `loadFailure`, and optional `cacheWarning`. Only `loadFailure`
  participates in the footer failure matrix.
- `FeedFooterState`: `Ready`, `Loading`, `NoMoreItems`, `Error`, `Offline`.
- `FeedUiState`: contains at least items, initial loading, refreshing, and footer
  state.
- `DetailUiState`: contains optional detail, loading/refreshing, favorite state, and
  an optional retryable error.
- `FavoritesUiState`: contains currently visible items, total count, and footer
  state.

Evaluate load more from top to bottom and use the footer state from the first
matching row:

| Result | Footer state |
| --- | --- |
| Any source adds a new identity | `Ready` |
| No data is added, and both sources are exhausted | `NoMoreItems` |
| No data is added, at least one load failure occurs, and all failures are offline | `Offline` |
| No data is added, and at least one load failure is not offline | `Error` |
| Any other case | `Ready` |

The last row covers a successful response that contains only duplicate items but
still provides the next cursor. The cursor may advance so the user can continue to
load more; the UI must not incorrectly show Error.

Evaluate refresh from top to bottom in the same way:

| Result | Footer state |
| --- | --- |
| Any source successfully loads a non-empty first page | `Ready` |
| No non-empty page succeeds, and both sources are exhausted | `NoMoreItems` |
| No non-empty page succeeds, at least one load failure occurs, and all failures are offline | `Offline` |
| No non-empty page succeeds, and at least one load failure is not offline | `Error` |
| Any other case | `Ready` |

Even if the first page returned by refresh contains only identities already on
screen, it is still `Ready` as long as the response succeeds and is non-empty.
Therefore, when the last page returns new data, first show `Ready`; show
`NoMoreItems` only when the next operation adds no new data and both sources are
exhausted. A failed source keeps its pre-refresh sequence and cursor.

## Phase 0: Set Up the Project and Fixtures

### Implementation

- [x] Create a single Android application module and a minimal Hello World app.
- [x] Configure Kotlin, Android Jetpack, coroutines, Flow, Room, RecyclerView,
  SwipeRefreshLayout, Gson, Coil, Lich Component, Mockito, kotlin-test, and
  Robolectric. If Robolectric tests need to replace the Lich component, also add
  the corresponding Lich component test helper. Do not add other unused Lich
  modules.
- [x] Configure the local JVM test source set and Robolectric resources.
- [x] Decide minSdk, targetSdk, application id, Java/Kotlin toolchain, and dependency
  versions. Keep them centralized in a version catalog or consistent Gradle
  configuration.
- [x] Add fixtures for four endpoints under `assets`: DummyJson Feed, DummyJson Detail,
  SpaceFlight Feed, and SpaceFlight Detail.

### Verification

- Run the Gradle local unit test task and verify that the test framework starts.
- Run debug assemble and verify that the empty project compiles.
- Manually verify that asset paths and names communicate the source, endpoint,
  cursor, and scenario.

### Completion Criteria

- The empty app builds successfully.
- The test task runs without an emulator.
- Later Client tests do not need to add happy-path fixtures at the last minute.

## Phase 1: Create the Three Screen Shells and Navigation

### Implementation

- [x] Create three sets of Activities, Fragments, layouts, and ViewModels.
- [x] Temporarily show only the screen name in each Fragment.
- [x] Use `FeedActivity` as the launcher Activity.
- [x] The temporary Feed item action may use a fixed source/id to open Detail.
- [x] The Feed option menu can open Favorites.
- [x] Favorites has no option menu. Back finishes FavoritesActivity and returns to
  Feed.
- [x] Activity intents contain only source and id. If Detail receives missing or
  invalid arguments, show an error and let the user go Back without crashing.

### Tests and Verification

- Add local JVM smoke tests for the three ViewModels to verify that their initial
  state is readable.
- Use Robolectric local JVM tests to verify creation of all three Activities,
  Fragment attachment, Feed-to-Detail/Favorites navigation, intent extras, and Back
  behavior.
- Also verify navigation and Back behavior for all three Activities manually.
- Run unit tests and debug assemble.

### Completion Criteria

- All three screens can open.
- Navigation does not pass DTOs, domain objects, or raw JSON.

## Phase 2: Create Domain Contracts, DTOs, Parsers, and Clients

### Step 2.1: Shared Domain and Result Contracts

- [x] Create `FeedSource`, `FeedItem`, `DummyJsonItem`, `SpaceFlightItem`, `Detail`,
  `PageCursor`, `DataError`, and shared result types.
- [x] Create `NetworkStatusProvider`, `TimeProvider`, and production/fake
  implementations.
- [x] Create an injectable coroutine dispatcher provider.
- [x] Add unit tests for identity, cursor, and fake providers.

### Step 2.2: DTOs, Parsers, and Mappers

- [x] Create API DTOs from the four fixture types.
- [x] Create Gson parsers and DTO-to-domain mappers. Do not put Gson annotations in
  domain models.
- [x] The Feed parser produces items together with the next cursor/exhausted
  information.
- [x] Test valid JSON, empty pages, last pages, malformed JSON, missing required fields,
  the same id from different sources, and duplicate ids.

### Step 2.3: Mock Clients

- [x] Create `DummyJsonClient`, `DummyJsonClientMock`, `SpaceFlightClient`, and
  `SpaceFlightClientMock`.
- [x] Client selects assets by cursor/id and returns only raw JSON.
- [x] Every Client call first runs a controllable delay, then performs the network
  check. When offline, it does not read assets.
- [x] Return `Failure` for a missing matching fixture, asset read failure, and other I/O
  errors.
- [x] Test complete raw JSON return values, every cursor/id mapping, offline calls not
  reading assets, controllable delay, and missing assets.

### Completion Criteria

- Client tests do not inspect domain models at all.
- Parser/mapper tests do not require Android UI.
- ViewModel and Fragment do not import the DTO package.

## Phase 3: Implement the Room Database and Stores

### Step 3.1: JSON Cache Persistence

- [x] Create `JsonCacheEntity` with an auto-generated `_id`, timestamp, type, key, and
  `jsonString`. The Store interface exposes `jsonString` using the clearer semantic
  name `rawJson`.
- [x] Always use the following `CacheType` mapping. Do not derive database values
  automatically from enum ordinal: `1 = DummyJsonFeed`,
  `2 = SpaceFlightFeed`, `3 = DummyJsonDetail`, and
  `4 = SpaceFlightDetail`.
- [x] Create a unique index on `(type, key)`.
- [x] Create `JsonCacheDao` with query, atomic upsert, targeted deletion, and clear-all
  API cache operations.
- [x] Create the `JsonCacheStore` interface and Room-backed implementation.
- [x] `replaceFeedPages` uses a Room transaction to upsert the new first page, then
  delete other keys for the same Feed type. Roll back if any step fails.
- [x] Store does not import Gson DTOs or domain models.

### Step 3.2: Favorite Persistence

- [x] Create `FavoriteEntity` with `(sourceType, itemId)` as a composite primary key.
- [x] Save `addedAt`, title, imgUrl, description, and an extraInformation snapshot.
- [x] `FavoriteDao` queries by `addedAt DESC`. For identical timestamps, sort by
  sourceType and then itemId to keep tests deterministic.
- [x] Create the `FavoriteStore` interface and Room-backed implementation.
- [x] When updating an existing Favorite snapshot, preserve its original `addedAt`.

### Step 3.3: AppDatabase

- [x] Create the application's only `AppDatabase`, providing both JsonCacheDao and
  FavoriteDao.
- [x] Repository depends only on Store interfaces, not directly on AppDatabase or DAO.
- [x] Use database version 1 for this demo's initial schema. Do not create unused
  migrations in advance.

### Tests

Use Robolectric local JVM tests to verify:

- cache get/put/delete/clear and `(type, key)` uniqueness behavior;
- only one valid cache entry remains after atomic upsert;
- simulated atomic upsert and `replaceFeedPages` failures leave old cache readable;
- Store returns raw JSON byte-for-byte without parsing it;
- Favorite composite identity, operations needed for toggle, and newest-first
  ordering;
- snapshot updates do not change `addedAt`;
- clearing API cache does not delete Favorite;
- after excluding Room internal metadata tables, AppDatabase has only two
  application tables.

### Completion Criteria

- All DAO and Store tests pass on the local JVM.
- Database closing and test isolation are correct, and tests do not share state.

## Phase 4: Implement Repositories

### Step 4.1: Freshness and Cache Policy

- [x] Implement `FreshnessValidator` with injected freshness duration and
  `TimeProvider`.
- [x] Select the actual freshness duration and store it as a constant.
- [x] Use `age = maxOf(0, now - timestamp)`. Data is fresh only when
  `age < duration`; it is expired at exactly the duration boundary, and future
  timestamps have age 0.
- [x] Explicitly test timestamps before expiration, exactly at the boundary, after
  expiration, and in the future.
- [x] Encapsulate fresh hit, miss, stale hit, forced refresh, parse failure, and write
  failure flows in small helpers so the two sources do not duplicate the entire
  flow.
- [x] The public Feed Repository API accepts only cursor and `forceRefresh`. Internally,
  it uses the three cache modes `NORMAL`, `FORCE_REFRESH_FIRST_PAGE`, and
  `NETWORK_ONLY_RECOVERY`; ViewModel does not know about the modes or recovery flag.

### Step 4.2: DummyJsonRepository

- [x] Implement Feed page and Detail loading.
- [x] Use the DummyJson parser/mapper without exposing DTOs.
- [x] Support `forceRefresh` to obtain the first page, but do not delete cache first.
- [x] A successful refresh uses `replaceFeedPages`. If the transaction fails, return
  new data with `cacheWarning` and enter `NETWORK_ONLY_RECOVERY` inside Repository.
- [x] In recovery mode, a Client failure does not fall back to stale cache, and Client
  success does not write page cache. Return to NORMAL only after a later first-page
  refresh transaction succeeds.
- [x] Still try Client after a cache read failure. If Client succeeds, display the
  data. If Client also fails, return Error through a `loadFailure` that includes
  Storage.
- [x] Return results according to this document's stale and cache-write-failure
  decisions.
- [x] Add tests for fresh, miss, stale success, stale offline, stale failure, forced
  refresh, malformed cache, malformed Client JSON, cache read failure, and cache
  write failure.
- [x] Test that recovery mode never reads/writes page cache, a Client failure does not
  fall back to stale data, recreating the Repository reads only the complete old
  generation, and a successful transaction on the next refresh restores NORMAL.

### Step 4.3: SpaceFlightRepository

- [x] Implement the same policy as DummyJson, using the SpaceFlight schema and cursor.
- [x] Do not duplicate DummyJson-specific DTOs or mappers.
- [x] Run the test matrix symmetric with Step 4.2, plus test the exhausted decision when
  `next` is null.

### Step 4.4: DetailRepository Router

- [x] Route only by `FeedSource` and id to the corresponding source repository.
- [x] Use `Flow<DetailLoadEvent>` to represent cached, updated, refresh-failed, and
  load-failed events.
- [x] Test routing for each source, the single emission from fresh cache, two-stage
  emissions from stale cache, no-cache offline behavior, and retry.

### Step 4.5: FavoritesRepository

- [x] Use `FavoriteStore` to implement `isFavorite`, add, remove, toggle, query, and
  snapshot update.
- [x] Use `TimeProvider` to produce `addedAt` when adding an item.
- [x] Toggle is completely independent of API network state.
- [x] Support five-item list pages through limit/offset or an equivalent interface.
- [x] Test add, remove, duplicate add, sorting, five-item pagination, last page,
  snapshot updates preserving `addedAt`, and local operations remaining unaffected
  by every network state.

### Completion Criteria

- Repository tests use fake Clients and fake Stores without starting Room.
- Each source passes the same cache policy test matrix.
- All public results contain only domain models and defined error/result types.

## Phase 5: Implement FeedViewModel

### Implementation

- [x] Create immutable `FeedUiState` and private mutable state.
- [x] On initialization, fetch the first page of both sources concurrently.
- [x] Keep the complete sequence, cursor, and exhausted state for each source
  separately.
- [x] Use two child coroutines to read non-exhausted sources concurrently, then
  evaluate the results together.
- [x] A successful source can update and advance independently. A failed source keeps
  its cursor and retries the same page next time.
- [x] Do not create requests for an exhausted source.
- [x] Implement deduplication and deterministic alternating merge with pure functions.
- [x] `loadMoreItems()` returns immediately while Loading, refreshing, or
  NoMoreItems.
- [x] `refresh()` returns immediately while load more or refresh is running.
- [x] Refresh forces both sources to read their first page. A successful source
  replaces its entire sequence and cursor; a failed source preserves its original
  sequence, cursor, and exhausted state.
- [x] Calculate footer state with the separate load-more and refresh failure matrices.

### Tests

- initial load success, partial success, both sources offline, and mixed failure;
- both sources actually start concurrently;
- fixed alternation, different source lengths, an empty source, and
  `(source, id)` deduplication;
- source cursors advance independently, and a failed source retries the same
  cursor;
- an exhausted source is not requested again, and only two exhausted sources yield
  NoMoreItems;
- the difference between a stale page that adds data and a stale page already in
  the sequence;
- a duplicate-only page can advance and remain Ready;
- every important load-more partial-success combination;
- refresh partial success replaces only the successful source;
- refresh remains Ready when it returns non-empty data whose identities are all
  unchanged;
- repeated load, repeated refresh, and cross-operation actions do not create
  duplicate requests;
- dispatcher-controlled tests do not use real delays.

### Completion Criteria

- ViewModel tests cover every Feed state transition in the SPEC.
- Merge and failure evaluation are small pure functions with separate tests.

## Phase 6: Implement DetailViewModel and FavoritesViewModel

### Step 6.1: DetailViewModel

- [x] Obtain `FeedSource` and id from saved state/creation arguments and validate the
  input.
- [x] Start Detail loading only when the screen opens for the first time.
- [x] Collect `DetailLoadEvent` while preserving any stale detail already displayed.
- [x] When no detail exists, Offline/Error states show a retry action. When stale
  detail exists, a refresh failure does not clear the content.
- [x] Query Favorite state at the same time. Toggle writes the current Detail snapshot
  locally.
- [x] When a newer Detail is loaded successfully, the ViewModel may update the snapshot
  if the item is already a Favorite, but it must preserve `addedAt`.
- [x] Prevent duplicate load and duplicate toggle operations.

### Step 6.2: FavoritesViewModel

- [x] Initially show five items. Every `loadMoreItems()` reveals five more.
- [x] Enter NoMoreItems after every item is visible. An empty list also has no more
  items.
- [x] Every time the Fragment returns to resumed state, call refresh and reread data
  through FavoritesRepository so toggles on the Detail screen are reflected.
- [x] Preserve a reasonable visible limit after refresh, but do not exceed the current
  total count.
- [x] Favorite is a local operation and does not show Offline due to network state.

### Tests

- Detail fresh cache, stale-first then updated, stale refresh failure, no-cache
  Offline/Error, retry, and invalid arguments;
- initial Favorite state, add, remove, toggle failure, and snapshot update;
- pagination states for 0, 1, 5, 6, 10, and 11 Favorites;
- additions/removals reflected after refreshing on return, and their ordering;
- repeated actions do not create duplicate work.

### Completion Criteria

- ViewModel tests do not depend on Android Activity/Fragment.
- When state already contains content, a background update failure does not clear
  displayable data.

## Phase 7: Assemble Application Dependencies with Lich

### Implementation

- [x] Create an application-level dependency component and use Lich
  `ComponentFactory` to manage the singleton dependency graph. Lich is responsible
  only for retrieving the component; objects inside it still use constructor
  injection.
- [x] Create and retain a single AppDatabase through that component.
- [x] Create production TimeProvider, NetworkStatusProvider, Clients,
  Stores, and Repositories.
- [x] Create three ViewModel factories. Fragment retrieves a factory only through the
  Lich component and does not manually instantiate Repository or Database.
- [x] The lifetimes of database, Store, and Repository must not be shorter than the
  ViewModels that use them.
- [x] The mock network state must have a source controllable by the demo app while
  preserving an interface boundary for a future real implementation.

### Tests and Verification

- Use the Lich test helper or a separate test component to replace dependencies,
  and add Robolectric local JVM tests for important factory/composition mappings.
- Run all local tests and debug assemble.
- Verify that there is no second RoomDatabase subclass.

### Completion Criteria

- All three ViewModels can obtain their complete dependency graphs through their
  factories.
- Each application context receives the same singleton components, while tests do
  not share component state.
- The UI layer does not know about Room, Gson DTOs, or Client implementations.

## Phase 8: Implement FeedFragment

### Implementation

- [x] Create SwipeRefreshLayout, RecyclerView, empty-content/initial-loading, and error
  views.
- [x] Use one adapter supporting three view types: DummyJson item, SpaceFlight item,
  and footer state button.
- [x] Item layouts show title, source-specific information, and a Coil image.
- [x] Use stable `(source, id)` identity and DiffUtil.
- [ ] Collect `FeedUiState` in a lifecycle-aware manner. Rendering must not modify
  ViewModel state directly.
- [ ] Pull-to-refresh calls `refresh()`. Depending on state, the footer button calls
  load more or retry.
- [ ] `Ready`, `Loading`, `NoMoreItems`, `Error`, and `Offline` each have clear text and
  enabled state.
- [ ] Clicking an item passes only source and id to DetailActivity.
- [ ] The option menu opens FavoritesActivity.

### Verification

- Use Robolectric local JVM tests to verify view visibility/enabled state for key
  states, pull-to-refresh and footer action forwarding, item click extras, and that
  Fragment recreation does not repeat initial loading.
- Run all local tests, lint, and debug assemble.
- Manually verify initial load, load more, refresh, rapid repeated taps, partial
  failure, Offline, Error, NoMoreItems, sources with different lengths, and the
  image-failure placeholder.

### Completion Criteria

- The UI order exactly matches the FeedViewModel state.
- Rotating or recreating Fragment does not create duplicate data requests.

## Phase 9: Implement DetailFragment

### Implementation

- [ ] The layout shows image, title, description, extra information, loading, and
  error.
- [ ] Keep stale detail visible after it appears. Update it when background refresh
  succeeds; provide a non-destructive error indication when refresh fails.
- [ ] No-cache Offline/Error states provide a retry action.
- [ ] The favorite icon renders the current state. Prevent repeated taps while an
  operation is running, then update the icon after success.
- [ ] An invalid source/id shows an error and allows Back without crashing.

### Verification

- Use Robolectric local JVM tests to verify loading, content, stale content,
  Offline/Error, retry, favorite action forwarding, and invalid arguments.
- Run all local tests, lint, and debug assemble.
- Manually verify both sources, fresh cache, stale update, stale failure, no-cache
  offline, retry, and offline Favorite toggle.

### Completion Criteria

- Activity can reconstruct the complete Detail screen from source/id alone.
- No network failure deletes existing detail or favorite data.

## Phase 10: Implement FavoritesFragment

### Implementation

- [ ] Reuse Feed item adapter/model capabilities. If necessary, add only
  Favorite-specific binding; do not create a duplicate Favorites domain model.
- [ ] Initially show five items. Each footer button action reveals the next five, and
  the footer becomes NoMoreItems after all items are visible.
- [ ] Pull-to-refresh rereads only local Favorites and does not clear data.
- [ ] `onResume` tells ViewModel to refresh so changes made in Detail are reflected.
- [ ] Render in newest-first order. Clicking an item passes only source/id to Detail.
- [ ] Do not provide an option menu. System Back returns to Feed.

### Verification

- Use Robolectric local JVM tests to verify the empty list, content, footer states,
  refresh, item click extras, absence of an option menu, and resumed-refresh
  forwarding.
- Run all local tests, lint, and debug assemble.
- Manually verify an empty list, fewer than five items, exactly five items, multiple
  pages, returning after Detail add/remove, app restart, offline state, and
  Favorites still displaying after API cache is cleared.

### Completion Criteria

- Favorite visibility is independent of API cache and network state.
- Pagination and refresh-on-return do not create duplicates or incorrect ordering.

## Phase 11: Overall Acceptance

### Automated Checks

- Run all local JVM unit tests.
- Run all Robolectric local JVM UI tests.
- Run lint.
- Run debug assemble.
- Verify that tests use no real network, real delay, or wall clock.
- Have a sub-agent review architecture boundaries, the error matrix, cache policy,
  state transitions, test gaps, and unnecessary dependencies.

### Manual Smoke Test

- Walk through Feed → Detail → Favorite toggle → Back → Favorites.
- Verify load-more partial success and independent cursors for both sources.
- Verify that refresh partial success preserves content from the failed source.
- Verify fresh, stale, offline, generic failure, and retry behavior.
- Verify that app restart and API cache clearing do not remove Favorites.
- Verify that repeated taps do not create duplicate requests.

### Final Completion Criteria

- Every observable behavior in `SPEC.md` has a production implementation and a
  corresponding local JVM test. Visual layout quality is additionally covered by
  manual verification.
- All automated checks pass with no unresolved review findings.
- Git history consists of small, single-purpose commits with Coding-Assistant
  trailers.
