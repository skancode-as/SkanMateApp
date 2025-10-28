package dk.skancode.skanmate.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColumnValidationTest {
    @Test
    fun deserializeEmail() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "email"}""")

        assertEquals(ColumnValidation.Email, validation)
    }
    @Test
    fun deserializeEmailAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "email"}""")

        assertEquals(ColumnValidation.Email, validation)

        val correctEmail = ColumnValue.Text("test@skancode.dk")
        assertTrue(validation.validate(correctEmail))

        val invalidEmail = ColumnValue.Text("test@skancode")
        assertFalse(validation.validate(invalidEmail))
    }
    @Test
    fun deserializeUnique() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "unique"}""")

        assertEquals(ColumnValidation.Unique, validation)
    }
    @Test
    fun deserializeRequired() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "required"}""")

        assertEquals(ColumnValidation.Required, validation)
    }
    @Test
    fun deserializeRequiredAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "required"}""")

        assertEquals(ColumnValidation.Required, validation)

        val validValue = ColumnValue.Text("Not empty")
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Text("   ")
        assertFalse(validation.validate(invalidValue))
    }

    @Test
    fun deserializeMinLength() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "minLength", "value": 2}""")

        assertEquals(ColumnValidation.MinLength(2), validation)
    }
    @Test
    fun deserializeMinLengthAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "minLength", "value": 19}""")

        assertEquals(ColumnValidation.MinLength(19), validation)

        val validValue = ColumnValue.Text("This is long enough")
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Text("NOT long enough")
        assertFalse(validation.validate(invalidValue))
    }

    @Test
    fun deserializeMaxLength() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "maxLength", "value": 5}""")

        assertEquals(ColumnValidation.MaxLength(5), validation)
    }
    @Test
    fun deserializeMaxLengthAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "maxLength", "value": 20}""")

        assertEquals(ColumnValidation.MaxLength(20), validation)

        val validValue = ColumnValue.Text("This is not too long")
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Text("This is waaay too long")
        assertFalse(validation.validate(invalidValue))
    }
    @Test
    fun deserializeMinValue() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "minValue", "value": 2}""")

        assertEquals(ColumnValidation.MinValue(2f), validation)
    }
    @Test
    fun deserializeMinValueAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "minValue", "value": 2}""")

        assertEquals(ColumnValidation.MinValue(2f), validation)

        val validValue = ColumnValue.Numeric(3f)
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Numeric(1f)
        assertFalse(validation.validate(invalidValue))
    }

    @Test
    fun deserializeMaxValue() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "maxValue", "value": 5}""")

        assertEquals(ColumnValidation.MaxValue(5f), validation)
    }
    @Test
    fun deserializeMaxValueAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "maxValue", "value": 5}""")
        assertEquals(ColumnValidation.MaxValue(5f), validation)

        val validValue = ColumnValue.Numeric(3f)
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Numeric(6f)
        assertFalse(validation.validate(invalidValue))
    }

    @Test
    fun deserializePattern() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "pattern", "value": "[A-Z]{3}"}""")

        assertEquals(ColumnValidation.Pattern("[A-Z]{3}"), validation)
    }
    @Test
    fun deserializePatternAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "pattern", "value": "[A-Z]{3}"}""")

        assertEquals(ColumnValidation.Pattern("[A-Z]{3}"), validation)
        val validValue = ColumnValue.Text("ABC")
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Text("abc")
        assertFalse(validation.validate(invalidValue))
    }
    @Test
    fun deserializeDefaultValueString() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "defaultValue", "value": "15"}""")

        assertEquals(ColumnValidation.DefaultValue("15"), validation)
    }
    @Test
    fun deserializeDefaultValueNumber() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "defaultValue", "value": 15}""")

        assertEquals(ColumnValidation.DefaultValue("15"), validation)
    }

    @Test
    fun deserializeConstantValueString() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "constantValue", "value": "1234"}""")

        assertEquals(ColumnValidation.ConstantValue("1234"), validation)
    }
    @Test
    fun deserializeConstantValueStringAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "constantValue", "value": "1234"}""")
        assertEquals(ColumnValidation.ConstantValue("1234"), validation)

        val validValue = ColumnValue.Text("1234")
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Text("abc")
        assertFalse(validation.validate(invalidValue))
    }
    @Test
    fun deserializeConstantValueNumber() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "constantValue", "value": 1234}""")

        assertEquals(ColumnValidation.ConstantValue("1234"), validation)
    }
    @Test
    fun deserializeConstantValueNumberAndValidate() {
        val validation: ColumnValidation = Json.Default.decodeFromString("""{"name": "constantValue", "value": 1234}""")
        assertEquals(ColumnValidation.ConstantValue("1234"), validation)

        val validValue = ColumnValue.Numeric(1234)
        assertTrue(validation.validate(validValue))

        val invalidValue = ColumnValue.Numeric(123)
        assertFalse(validation.validate(invalidValue))
    }
}