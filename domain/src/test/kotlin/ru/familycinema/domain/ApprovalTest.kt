package ru.familycinema.domain
import org.junit.Assert.*
import org.junit.Test
class ApprovalTest {
 private val f=Film("one","История","",0,"CC0",VideoVersion("v1","asset:///a","abc",1))
 private val approvals=setOf(Approval(f.id,f.version.fingerprint))
 @Test fun exactVersionIsApproved() { assertTrue(isApproved(f,approvals)) }
 @Test fun changedSourceIsNotApproved() { assertFalse(isApproved(f.copy(version=f.version.copy(uri="https://other")),approvals)) }
 @Test fun changedDigestIsNotApproved() { assertFalse(isApproved(f.copy(version=f.version.copy(sha256="changed")),approvals)) }
 @Test fun otherEpisodeNeedsApproval() { assertFalse(isApproved(f.copy(version=f.version.copy(episode=2)),approvals)) }
 @Test fun emptyCatalogFailsClosed() { assertFalse(isApproved(f,emptySet())) }
}
