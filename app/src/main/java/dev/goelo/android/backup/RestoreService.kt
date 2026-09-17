package dev.goelo.android.backup

import dev.goelo.android.data.StateStore
import dev.goelo.android.data.StoreSnapshot
import dev.goelo.android.model.AppState
import dev.goelo.android.rating.validateState
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PreparedRestore(
    val envelope: BackupEnvelope,
    val expectedRevision: Long,
    val sourceName: String,
)

class RestoreService(
    private val store: StateStore,
    private val snapshots: SnapshotStore,
    private val codec: BackupCodec,
    private val clock: Clock,
    private val appVersion: String,
) {
    suspend fun prepare(bytes: ByteArray, sourceName: String): PreparedRestore {
        val envelope = withContext(Dispatchers.IO) { codec.decode(bytes) }
        return prepareEnvelope(envelope, sourceName)
    }

    suspend fun prepareState(state: AppState, sourceName: String): PreparedRestore = prepareEnvelope(
        BackupEnvelope(exportedAtEpochMs = clock.millis(), appVersion = appVersion, state = state),
        sourceName,
    )

    suspend fun apply(prepared: PreparedRestore): StoreSnapshot {
        require(prepared.sourceName.isNotBlank()) { "恢复来源不能为空" }
        validateEnvelopeForRestore(prepared.envelope)
        val current = store.read()
        check(current.revision == prepared.expectedRevision) { "数据已变化，请重新预览" }
        snapshots.save(codec.encode(
            BackupEnvelope(
                exportedAtEpochMs = clock.millis(),
                appVersion = appVersion,
                state = current.state,
            ),
        ))
        return store.update(prepared.expectedRevision) { prepared.envelope.state }
    }

    private suspend fun prepareEnvelope(envelope: BackupEnvelope, sourceName: String): PreparedRestore {
        require(sourceName.isNotBlank()) { "恢复来源不能为空" }
        validateEnvelopeForRestore(envelope)
        return PreparedRestore(envelope, store.read().revision, sourceName)
    }

    private fun validateEnvelopeForRestore(envelope: BackupEnvelope) {
        // encode performs the canonical format, metadata, and deep state validation without mutating storage.
        codec.encode(envelope)
        validateState(envelope.state).getOrThrow()
    }
}
