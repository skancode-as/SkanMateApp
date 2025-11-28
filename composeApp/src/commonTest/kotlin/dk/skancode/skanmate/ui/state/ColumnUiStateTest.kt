package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.data.model.ColumnConstraint
import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.ColumnValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ColumnUiStateTest {
    @Test
    fun prepareFileColumnNotUploaded() {
        var column: ColumnUiState = ColumnModel(
            id = "1",
            name = "Name",
            dbName = "dbName",
            width = 1f,
            type = ColumnType.File,
            constraints = listOf(),
            listOptions = emptyList(),
            rememberValue = false,
        ).toUiState().copy(
            value = ColumnValue.File(fileName = "Image", localUrl = "file://local/path/to/Image", bytes = ByteArray(1))
        )

        assertEquals(
            expected = ColumnValue.File(fileName = "Image", objectUrl = null, localUrl = "file://local/path/to/Image", bytes = ByteArray(1), isUploaded = false),
            actual = column.value,
        )
        val imageQueue = mutableListOf<String>()
        column = column.prepare(
            uploadImage = {_, _ -> "https://object.url"},
            queueImageDeletion = {
                imageQueue.add(it)
            },
        )
        assertEquals(
            expected = 1,
            actual = imageQueue.size,
            )
        assertEquals(
            expected = ColumnValue.File(fileName = "Image", objectUrl = "https://object.url", localUrl = "file://local/path/to/Image", bytes = ByteArray(1), isUploaded = true),
            actual = column.value,
        )
    }

    @Test
    fun prepareTextColumnWithPrefix() {
        var column: ColumnUiState = ColumnModel(
            id = "1",
            name = "Name",
            dbName = "dbName",
            width = 1f,
            type = ColumnType.Text,
            constraints = listOf(
                ColumnConstraint.Prefix("SC-"),
            ),
            listOptions = emptyList(),
            rememberValue = false,
        ).toUiState().copy(
            value = ColumnValue.Text("123456")
        )

        assertEquals(expected = column.value, actual = ColumnValue.Text(text = "123456"))
        column = column.prepare({_, _ -> ""}, {})
        assertEquals(expected = column.value, actual = ColumnValue.Text(text = "SC-123456"))
    }

    @Test
    fun prepareTextColumnWithSuffix() {
        var column: ColumnUiState = ColumnModel(
            id = "1",
            name = "Name",
            dbName = "dbName",
            width = 1f,
            type = ColumnType.Text,
            constraints = listOf(
                ColumnConstraint.Suffix("-SC"),
            ),
            listOptions = emptyList(),
            rememberValue = false,
        ).toUiState().copy(
            value = ColumnValue.Text("123456")
        )

        assertEquals(expected = column.value, actual = ColumnValue.Text(text = "123456"))
        column = column.prepare({_, _ -> ""}, {})
        assertEquals(expected = column.value, actual = ColumnValue.Text(text = "123456-SC"))
    }
}