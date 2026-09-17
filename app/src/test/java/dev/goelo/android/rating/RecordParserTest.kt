package dev.goelo.android.rating
import dev.goelo.android.model.RecordInput
import org.junit.Assert.*
import org.junit.Test
class RecordParserTest {
 @Test fun validAndInvalidRecords() { assertEquals(RecordInput(7,11,8),parseRecord(7,"11 － 8").getOrThrow()); assertTrue(parseRecord(7,"20-1").isFailure); assertTrue(parseRecord(7,"-1-3").isFailure); assertTrue(parseRecord(7,"").isFailure) }
}
