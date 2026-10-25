package ru.familycinema.parental
import org.junit.Assert.*
import org.junit.Test
class PinHasherTest {
 @Test fun correctAndWrongPin() { val salt=PinHasher.salt(); val hash=PinHasher.hash("1234",salt); assertTrue(PinHasher.verify("1234",salt,hash));assertFalse(PinHasher.verify("4321",salt,hash)) }
 @Test fun uniqueSalts() { val a=PinHasher.salt();val b=PinHasher.salt();assertFalse(a.contentEquals(b));assertFalse(PinHasher.hash("1234",a).contentEquals(PinHasher.hash("1234",b))) }
}
