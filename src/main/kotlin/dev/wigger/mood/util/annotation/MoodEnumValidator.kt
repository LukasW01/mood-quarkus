package dev.wigger.mood.util.annotation

import dev.wigger.mood.util.enums.Mood
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class MoodEnumValidator : ConstraintValidator<MoodEnum, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean = try {
        value?.let {
            Mood.valueOf(value)
        }
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}
