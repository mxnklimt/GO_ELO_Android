# Analysis Prediction and Player Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add player-library history filtering, read-only win-rate prediction, and head-to-head statistics to the analysis experience without changing stored data formats.

**Architecture:** Keep all new behavior as pure statistics and filter functions over the existing `AppState` and current-player projection. Add local Compose state for the prediction form, split new analysis cards into focused files, and pass both the full ledger and current-player projection into the analysis screen. Do not add Room columns, backup fields, or persisted analysis snapshots.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing Room/StateStore model, JUnit 4 JVM tests, Compose Android tests, Gradle offline toolchain.

**Spec:** `docs/superpowers/specs/2026-09-21-analysis-prediction-and-player-filter-design.md`

## Global Constraints

- History filtering is read-only and must preserve the existing `kind == NATIVE` distinction; legacy records are never classified as temporary or player-library matches.
- Temporary prediction uses the current `elo-v3` opponent estimate; player-library prediction uses `AppState.ratingOf(opponentId)`.
- Prediction uses `P = 1 / (1 + 10 ^ ((Ropp - Rself) / 400))` and the existing K=20 `scoreChange` behavior.
- No Room schema change, no `.goelo.json` schema change, no network, and no persisted analysis snapshot.
- Existing `elo-v1`, `elo-v2`, and `elo-v3` replay behavior remains unchanged.
- Use TDD for each domain change: write a failing test, run it, implement the smallest change, run the focused test, then run the broader suite.

## Review Focus

- A legacy record with no `opponentPlayerId` must not appear under either new native-game filter; pin this in `HistoryFilterTest`.
- A match viewed from its second player must reverse outcome, delta, and opponent identity; pin this in head-to-head statistics tests.
- A temporary prediction with `0-0`, `20-0`, `0-20`, and invalid totals must use existing validation and boundary rules; pin this in prediction tests.
- A player-library prediction must use the opponent’s current replayed ELO, not the opponent’s stored initial ELO; pin this in prediction tests.
- Empty player libraries and empty head-to-head history must render stable empty states rather than throw; pin this in Compose analysis tests.

---

### Task 1: Add reusable prediction math

**Files:**
- Modify: `app/src/main/java/dev/goelo/android/rating/EloCalculator.kt`
- Test: `app/src/test/java/dev/goelo/android/rating/EloCalculatorTest.kt`

**Interfaces:**
- Produces `fun winProbability(selfElo: Double, opponentElo: Double): Double`.
- Existing `scoreChange(selfElo, opponentElo, outcome)` continues to return the same `ScoreChange` type and delegates to the shared probability calculation.

- [ ] **Step 1: Write failing tests**

Add tests for equal ratings (`0.5`), a stronger opponent (probability below `0.5`), probability plus loss probability equaling `1.0`, and finite/positive input validation. Add a regression asserting `scoreChange(2000.0, 2100.0, Outcome.WIN)` retains its current delta.

```kotlin
@Test fun expectedWinProbabilityUsesTheExistingEloFormula() {
    assertEquals(0.5, winProbability(2000.0, 2000.0), 1e-12)
    assertTrue(winProbability(2000.0, 2100.0) < 0.5)
    assertEquals(1.0, winProbability(2000.0, 2100.0) + winProbability(2100.0, 2000.0), 1e-12)
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```powershell
. ./scripts/Use-Toolchain.ps1
$env:GRADLE_USER_HOME = 'C:/Users/mxnkilmt/Documents/Codex/Projects/GO_ELO_Android/.local/gradle-cache'
./gradlew.bat :app:testDebugUnitTest --tests dev.goelo.android.rating.EloCalculatorTest --offline --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

Expected: compilation fails because `winProbability` does not exist.

- [ ] **Step 3: Implement the shared probability function**

Move the logistic expression currently inside `scoreChange` into `winProbability`, validate both inputs with `require`, and calculate:

```kotlin
fun winProbability(selfElo: Double, opponentElo: Double): Double {
    require(selfElo.isFinite() && opponentElo.isFinite())
    return 1.0 / (1.0 + 10.0.pow((opponentElo - selfElo) / 400.0))
}
```

Update `scoreChange` to use `winProbability` without changing K=20 or the returned values.

- [ ] **Step 4: Run the focused test and verify it passes**

Run the same Gradle command; expected result is all `EloCalculatorTest` tests passing.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/dev/goelo/android/rating/EloCalculator.kt app/src/test/java/dev/goelo/android/rating/EloCalculatorTest.kt
git commit -m "refactor: expose reusable elo win probability"
```

### Task 2: Add native match-type history filtering

**Files:**
- Modify: `app/src/main/java/dev/goelo/android/stats/HistoryFilter.kt`
- Modify: `app/src/main/java/dev/goelo/android/ui/history/HistoryScreen.kt`
- Test: `app/src/test/java/dev/goelo/android/stats/HistoryFilterTest.kt`

**Interfaces:**
- Produces `enum class MatchScope { ALL, TEMPORARY, PLAYER_LIBRARY }`.
- `HistoryFilter` gains `scope: MatchScope = MatchScope.ALL`.
- `filterMatches` applies scope only to `MatchKind.NATIVE`, while preserving existing rank/result/date/undated behavior.

- [ ] **Step 1: Write failing filter tests**

Extend the fixture list with one native temporary match, one native player-library match, and one legacy match. Assert `PLAYER_LIBRARY` returns only the native match with a non-null `opponentPlayerId`, `TEMPORARY` returns only the native match with a null `opponentPlayerId`, and `ALL` still returns all existing records. Add one combined scope-plus-outcome assertion.

- [ ] **Step 2: Run `HistoryFilterTest` and verify it fails**

```powershell
./gradlew.bat :app:testDebugUnitTest --tests dev.goelo.android.stats.HistoryFilterTest --offline --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

Expected: compilation fails because `MatchScope` and `HistoryFilter.scope` do not exist.

- [ ] **Step 3: Implement the scope and Compose filter control**

Add the enum and filter condition:

```kotlin
private fun Match.matchesScope(scope: MatchScope): Boolean = when (scope) {
    MatchScope.ALL -> true
    MatchScope.TEMPORARY -> kind == MatchKind.NATIVE && opponentPlayerId == null
    MatchScope.PLAYER_LIBRARY -> kind == MatchKind.NATIVE && opponentPlayerId != null
}
```

Add a third filter-chip row in `HistoryScreen` labelled “全部对局 / 临时对手 / 棋手库对局”, pass `scope` into `HistoryFilter`, and keep the existing error/result behavior.

- [ ] **Step 4: Run focused JVM tests**

Run `HistoryFilterTest`; expected result is PASS with all combinations covered.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/dev/goelo/android/stats/HistoryFilter.kt app/src/main/java/dev/goelo/android/ui/history/HistoryScreen.kt app/src/test/java/dev/goelo/android/stats/HistoryFilterTest.kt
git commit -m "feat: filter history by native match type"
```

### Task 3: Add prediction and head-to-head statistics

**Files:**
- Create: `app/src/main/java/dev/goelo/android/stats/AnalysisInsights.kt`
- Test: `app/src/test/java/dev/goelo/android/stats/AnalysisInsightsTest.kt`

**Interfaces:**
- Produces `data class PredictionResult(val opponentElo: Double, val winProbability: Double, val winDelta: Double, val lossDelta: Double)`.
- Produces `fun predictAgainst(selfElo: Double, opponentElo: Double): PredictionResult`.
- Produces `data class HeadToHeadMatch(val matchId: String, val order: Long, val outcome: Outcome, val playedAtEpochMs: Long?, val playedZoneId: String?)`.
- Produces `data class HeadToHeadStat(val opponentId: String, val opponentName: String, val wins: Int, val losses: Int, val matches: List<HeadToHeadMatch>)`.
- Produces `fun headToHead(state: AppState, currentPlayerId: String): List<HeadToHeadStat>`.

- [ ] **Step 1: Write failing tests**

Create a shared-ledger fixture with two known-player matches, one temporary match, and one match where the selected player is the second side. Assert prediction values use the supplied ELO and `scoreChange`; assert head-to-head excludes temporary and legacy records, groups by opponent, reverses the second-side outcome, preserves chronological order, and calculates wins/losses correctly. Add a test proving `headToHead` reads the opponent’s display name from `allProfiles`.

- [ ] **Step 2: Run `AnalysisInsightsTest` and verify it fails**

```powershell
./gradlew.bat :app:testDebugUnitTest --tests dev.goelo.android.stats.AnalysisInsightsTest --offline --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

Expected: compilation fails because the new result types and functions do not exist.

- [ ] **Step 3: Implement pure insight functions**

Use `winProbability` for the prediction result and calculate `winDelta`/`lossDelta` through `scoreChange`. For head-to-head, obtain `state.forPlayer(currentPlayerId)`, retain only native matches with `opponentPlayerId != null`, group by the projected `opponentPlayerId`, and map outcomes into `HeadToHeadMatch` values. Sort each group and the returned groups by stable opponent name then ID.

- [ ] **Step 4: Run focused tests**

Run `AnalysisInsightsTest` and `EloCalculatorTest`; expected result is PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/dev/goelo/android/stats/AnalysisInsights.kt app/src/test/java/dev/goelo/android/stats/AnalysisInsightsTest.kt
git commit -m "feat: calculate prediction and head to head insights"
```

### Task 4: Build the analysis UI and wire the full ledger

**Files:**
- Create: `app/src/main/java/dev/goelo/android/ui/analysis/PredictionCard.kt`
- Create: `app/src/main/java/dev/goelo/android/ui/analysis/HeadToHeadCard.kt`
- Modify: `app/src/main/java/dev/goelo/android/ui/analysis/AnalysisScreen.kt`
- Modify: `app/src/main/java/dev/goelo/android/ui/GoEloApp.kt`
- Create: `app/src/androidTest/java/dev/goelo/android/ui/AnalysisInsightsTest.kt`
- Create: `app/src/androidTest/java/dev/goelo/android/ui/HistoryScreenTest.kt`

**Interfaces:**
- `AnalysisScreen` consumes `ledger: AppState`, `personal: AppState`, `range: HistoryRange`, and `onRange`.
- `PredictionCard` consumes `selfElo`, `profiles`, `currentPlayerId`, and displays local form state only.
- `HeadToHeadCard` consumes `List<HeadToHeadStat>` and manages expanded opponent IDs locally.

- [ ] **Step 1: Add failing Compose tests**

Create a Compose test with a current player and one known opponent. Assert the analysis screen shows “胜率预测”, can select the player-library opponent, shows a percentage, and renders “棋手对局” with the correct W-L summary. Add a second test with no other profiles that shows the empty state. Create `HistoryScreenTest` with a native temporary match, a native player-library match, and a legacy match; assert tapping the “棋手库对局” filter leaves only the player-library row visible.

- [ ] **Step 2: Run the new Android tests and verify they fail**

```powershell
./gradlew.bat :app:compileDebugAndroidTestKotlin --offline --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

Expected: compilation or assertion failures because the new cards and screen inputs do not exist.

- [ ] **Step 3: Wire the complete ledger into `GoEloApp`**

Change the analysis call from `AnalysisScreen(personal, ...)` to `AnalysisScreen(ledger = snapshot.state, personal = personal, ...)`. Keep growth and history on their existing current-player projections.

- [ ] **Step 4: Implement `PredictionCard`**

Use local `remember` state for mode, rank, record text, and selected player ID. In temporary mode parse with `parseRecord`; in player-library mode use only profiles whose ID differs from the current player. Render an input prompt or error until valid, then render opponent ELO, win/loss percentages, and `+/-` projected deltas. Do not call `MatchService` or mutate `AppViewModel`.

- [ ] **Step 5: Implement `HeadToHeadCard` and place both cards**

Render the prediction card after the main interval summary and the head-to-head card after the opponent-rank distribution. Use existing `PanelCard`, `SectionTitle`, `FilterChip`, `EmptyPanel`, Gold/Positive/Negative colors, and the current scroll layout. Expand a row to show each `HeadToHeadMatch` date and result.

- [ ] **Step 6: Run Compose tests and focused JVM tests**

Run `AnalysisInsightsTest`, the new Android analysis tests, and existing `RecordFlowTest`/history tests. Expected result is PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/dev/goelo/android/ui/analysis app/src/main/java/dev/goelo/android/ui/GoEloApp.kt app/src/androidTest/java/dev/goelo/android/ui/AnalysisInsightsTest.kt
git commit -m "feat: add analysis prediction and head to head cards"
```

### Task 5: Update product documentation and regression coverage

**Files:**
- Modify: `README.md`
- Modify: `docs/multi-player-0.3.0.md`
- Modify: `docs/2026-09-21-analysis-prediction-and-player-filter-design.md` only if implementation wording needs correction

- [ ] **Step 1: Update user-facing documentation**

Document the history match-type filter, read-only win-rate prediction, and player-library head-to-head summary. State that prediction uses the current player’s live ELO and that no backup schema change is required.

- [ ] **Step 2: Run documentation consistency checks**

```powershell
rg -n "棋手库对局|胜率预测|对手段位|schema|备份" README.md docs
git diff --check
```

Expected: the new feature descriptions appear without stale statements that analysis is limited to temporary opponents.

- [ ] **Step 3: Commit**

```powershell
git add README.md docs/multi-player-0.3.0.md
git commit -m "docs: describe analysis insights and history filters"
```

### Task 6: Full verification, APK refresh, and final review

**Files:**
- Modify: `dist/go-elo-debug.apk`

- [ ] **Step 1: Run the complete verification command**

```powershell
. ./scripts/Use-Toolchain.ps1
$env:GRADLE_USER_HOME = 'C:/Users/mxnkilmt/Documents/Codex/Projects/GO_ELO_Android/.local/gradle-cache'
./gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --offline --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

Expected: `BUILD SUCCESSFUL`, zero JVM test failures/errors, zero Lint errors, and successful Android test compilation.

- [ ] **Step 2: Refresh the repository APK**

Copy `app/build/outputs/apk/debug/app-debug.apk` to `dist/go-elo-debug.apk` and the existing output aliases under `C:/Users/mxnkilmt/Documents/Codex/2026-09-17/wo/outputs/`. Verify the SHA-256 hash of the repository APK after copying.

- [ ] **Step 3: Perform final consistency checks**

Run `git diff --check`, `git status --short`, and inspect the final diff for accidental schema or dependency changes. Confirm the APK, docs, tests, and source are included.

- [ ] **Step 4: Commit the final artifact**

```powershell
git add dist/go-elo-debug.apk
git commit -m "build: refresh apk with analysis insights"
```
