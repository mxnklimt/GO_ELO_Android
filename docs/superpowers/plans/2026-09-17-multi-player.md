# Multi-player Implementation Plan
> For agentic workers: use superpowers:executing-plans to implement task by task in this session.
**Goal:** Add player management and atomic two-player matches, preserving existing data.
**Architecture:** One global event ledger, per-player ratings and UI projections; Room migration and versioned backup.
**Tech Stack:** Existing Kotlin, Compose, Room, JUnit. No new dependencies.
**Spec:** ../specs/2026-09-17-multi-player-design.md

## Global Constraints
K=20; no rounding in calculation; no smoothing for temporary opponents; old fixed deltas unchanged; offline; app id/signing unchanged.

### Task 1: Ledger and service behavior
Files: model/Models.kt, model/PlayerViews.kt, rating/ReplayEngine.kt, data/ProfileService.kt, data/MatchService.kt; test/data/MultiPlayerTest.kt.
Interfaces: AppState.allProfiles, forPlayer(id): AppState; ProfileService.add(id,name,elo,revision), select(id,revision); MatchService.recordKnown(id,opponentId,outcome,at,zone,playerId), editKnown(id,outcome,revision).
- [x] Write tests using real services and an atomic in-memory StateStore; assert A/B equal-rating game yields 2010/1990, mirrored view, temporary game isolation, cross-player cascade, duplicate retry and self-match rejection.
- [x] Run :app:testDebugUnitTest to observe missing new behavior.
- [x] Implement model extension, global replay and validator, profile switching, service mutations.
- [x] Run unit tests; commit verified domain changes with storage task.

### Task 2: Persistence and backup
Files: data/Entities.kt, StateDao.kt, GoEloDatabase.kt, RoomStateStore.kt; backup/BackupCodec.kt; AppContainer.kt; test/backup/MultiPlayerBackupTest.kt; androidTest/data/MultiPlayerMigrationTest.kt.
- [x] Add literal v1 archive fixture and v2 roundtrip tests; foreign-key and tamper rejection; Room v1 DB test fixture with original table/index DDL.
- [x] Add MIGRATION_1_2, registered on database builder, persist active ID/all profiles and match associations.
- [x] Accept v1 original strict shape, normalize to v2, encode only v2. Maintain indexed diagnostic failures and strict validation.
- [x] Run tests and compile device tests.

### Task 3: Player UX and two recording modes
Files: ui/players/PlayersSheet.kt, ui/record/RecordSheet.kt, ui/AppUiState.kt, AppViewModel.kt, GoEloApp.kt; history/HistoryScreen.kt and EditMatchSheet.kt; backup/RestorePreviewSheet.kt; components/VisualComponents.kt.
- [x] Expose a player selector above current screen with add/search/switch; persist selected player.
- [x] Feed forPlayer(activeId) only to growth/analysis/history/settings; global state to backups.
- [x] Add known-opponent choice with scores and explicit current-player result; maintain temporary mode and all existing record test tags.
- [x] Handle known match editing from either side, disable repeated save and invalid identity, refresh both projections.
- [x] Add UI device flow test for creation, switch, known match, mirrored history; compile and run when device exists.

### Task 4: Verification and delivery
- [x] Build assembleDebug, lintDebug, testDebugUnitTest, compileDebugAndroidTestKotlin offline with installed toolchain.
- [x] Review all mutations, projection usages and backup compatibility; fix findings with regression checks.
- [x] Compare previous/new signing certificates; update version to 0.3.0 and versionCode3.
- [x] Commit source + dist/go-elo-debug.apk; fast-forward original checkout; copy APK into outputs/go-elo-0.3.0-debug.apk and outputs/go-elo-debug.apk. Report actual tests and device limitations.
