package ru.familycinema.parental

import android.content.Context
import android.util.Base64
import ru.familycinema.domain.*

class FamilyStore(context: Context) {
    private val prefs = context.getSharedPreferences("family_private", Context.MODE_PRIVATE)
    val hasPin
        get() = prefs.contains("pin")

    private fun encode(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)

    private fun decode(s: String) = Base64.decode(s, Base64.NO_WRAP)

    fun setPin(pin: String) {
        require(pin.matches(Regex("[0-9]{4,8}")))
        val salt = PinHasher.salt()
        prefs
            .edit()
            .putString("salt", encode(salt))
            .putString("pin", encode(PinHasher.hash(pin, salt)))
            .commit()
    }

    fun verify(pin: String): Boolean =
        hasPin &&
            PinHasher.verify(
                pin,
                decode(prefs.getString("salt", "")!!),
                decode(prefs.getString("pin", "")!!),
            )

    fun approve(film: Film, allowed: Boolean) {
        // A manual revoke must not be silently undone by the auto-approval policy.
        prefs
            .edit()
            .putString("approval_${film.id}", if (allowed) film.version.fingerprint else "")
            .apply {
                if (allowed) remove("denied_${film.id}")
                else putString("denied_${film.id}", film.version.fingerprint)
            }
            .commit()
    }

    fun approved(film: Film) =
        prefs.getString("approval_${film.id}", null) == film.version.fingerprint

    fun position(id: String) = prefs.getLong("position_$id", 0)

    fun savePosition(id: String, value: Long) {
        prefs.edit().putLong("position_$id", value.coerceAtLeast(0)).apply()
    }

    var failedAttempts: Int
        get() = prefs.getInt("failures", 0)
        set(v) {
            prefs.edit().putInt("failures", v).commit()
        }

    var lockedUntil: Long
        get() = prefs.getLong("locked_until", 0)
        set(v) {
            prefs.edit().putLong("locked_until", v).commit()
        }
}
