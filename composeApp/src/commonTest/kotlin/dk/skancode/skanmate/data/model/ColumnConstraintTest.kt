package dk.skancode.skanmate.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ColumnConstraintTest {
    @Test
    fun deserializeEmail() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "email"}""")

        assertEquals(ColumnConstraint.Email, constraint)
    }
    @Test
    fun deserializeEmailAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "email"}""")

        assertEquals(ColumnConstraint.Email, constraint)

        val correctEmail = ColumnValue.Text("test@skancode.dk")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(correctEmail))

        val invalidEmail = ColumnValue.Text("test@skancode")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidEmail))
    }
    @Test
    fun deserializeUnique() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "unique"}""")

        assertEquals(ColumnConstraint.Unique, constraint)
    }
    @Test
    fun deserializeRequired() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "required"}""")

        assertEquals(ColumnConstraint.Required, constraint)
    }
    @Test
    fun deserializeRequiredAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "required"}""")

        assertEquals(ColumnConstraint.Required, constraint)

        val validValue = ColumnValue.Text("Not empty")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("   ")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }

    @Test
    fun deserializeMinLength() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "minLength", "value": 2}""")

        assertEquals(ColumnConstraint.MinLength(2), constraint)
    }
    @Test
    fun deserializeMinLengthAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "minLength", "value": 19}""")

        assertEquals(ColumnConstraint.MinLength(19), constraint)

        val validValue = ColumnValue.Text("This is long enough")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("NOT long enough")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }

    @Test
    fun deserializeMaxLength() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "maxLength", "value": 5}""")

        assertEquals(ColumnConstraint.MaxLength(5), constraint)
    }
    @Test
    fun deserializeMaxLengthAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "maxLength", "value": 20}""")

        assertEquals(ColumnConstraint.MaxLength(20), constraint)

        val validValue = ColumnValue.Text("This is not too long")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("This is waaay too long")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }
    @Test
    fun deserializeMinValue() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "minValue", "value": 2}""")

        assertEquals(ColumnConstraint.MinValue(2f), constraint)
    }
    @Test
    fun deserializeMinValueAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "minValue", "value": 2}""")

        assertEquals(ColumnConstraint.MinValue(2f), constraint)

        val validValue = ColumnValue.Numeric(3f)
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Numeric(1f)
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }

    @Test
    fun deserializeMaxValue() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "maxValue", "value": 5}""")

        assertEquals(ColumnConstraint.MaxValue(5f), constraint)
    }
    @Test
    fun deserializeMaxValueAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "maxValue", "value": 5}""")
        assertEquals(ColumnConstraint.MaxValue(5f), constraint)

        val validValue = ColumnValue.Numeric(3f)
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Numeric(6f)
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }

    @Test
    fun deserializePattern() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "pattern", "value": "[A-Z]{3}"}""")

        assertEquals(ColumnConstraint.Pattern("[A-Z]{3}"), constraint)
    }
    @Test
    fun deserializePatternAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "pattern", "value": "[A-Z]{3}"}""")

        assertEquals(ColumnConstraint.Pattern("[A-Z]{3}"), constraint)
        val validValue = ColumnValue.Text("ABC")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("abc")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }
    @Test
    fun deserializeDefaultValueString() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "defaultValue", "value": "15"}""")

        assertEquals(ColumnConstraint.DefaultValue("15"), constraint)
    }
    @Test
    fun deserializeDefaultValueNumber() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "defaultValue", "value": 15}""")

        assertEquals(ColumnConstraint.DefaultValue("15"), constraint)
    }

    @Test
    fun deserializeConstantValueString() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "constantValue", "value": "1234"}""")

        assertEquals(ColumnConstraint.ConstantValue("1234"), constraint)
    }
    @Test
    fun deserializeConstantValueStringAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "constantValue", "value": "1234"}""")
        assertEquals(ColumnConstraint.ConstantValue("1234"), constraint)

        val validValue = ColumnValue.Text("1234")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("abc")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }
    @Test
    fun deserializeConstantValueNumber() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "constantValue", "value": 1234}""")

        assertEquals(ColumnConstraint.ConstantValue("1234"), constraint)
    }
    @Test
    fun deserializeConstantValueNumberAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "constantValue", "value": 1234}""")
        assertEquals(ColumnConstraint.ConstantValue("1234"), constraint)

        val validValue = ColumnValue.Numeric(1234)
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Numeric(123)
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }

    @Test
    fun deserializePrefix() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "prefix", "value": "1234"}""")

        assertEquals(ColumnConstraint.Prefix("1234"), constraint)
    }

    @Test
    fun deserializeSuffix() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "suffix", "value": "1234"}""")

        assertEquals(ColumnConstraint.Suffix("1234"), constraint)
    }

    @Test
    fun deserializeStartsWith() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "startsWith", "value": "1234"}""")

        assertEquals(ColumnConstraint.StartsWith("1234"), constraint)
    }

    @Test
    fun deserializeStartsWithAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "startsWith", "value": "1234"}""")
        assertEquals(ColumnConstraint.StartsWith("1234"), constraint)

        val validValue = ColumnValue.Text("1234-5678")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("123-5678")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }


    @Test
    fun deserializeEndsWith() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "endsWith", "value": "1234"}""")

        assertEquals(ColumnConstraint.EndsWith("1234"), constraint)
    }

    @Test
    fun deserializeEndsWithAndValidate() {
        val constraint: ColumnConstraint = Json.Default.decodeFromString("""{"name": "endsWith", "value": "4321"}""")
        assertEquals(ColumnConstraint.EndsWith("4321"), constraint)

        val validValue = ColumnValue.Text("5678-4321")
        assertEquals(ConstraintCheckResult.Ok, constraint.check(validValue))

        val invalidValue = ColumnValue.Text("5678-321")
        assertIs<ConstraintCheckResult.Error>(constraint.check(invalidValue))
    }
}