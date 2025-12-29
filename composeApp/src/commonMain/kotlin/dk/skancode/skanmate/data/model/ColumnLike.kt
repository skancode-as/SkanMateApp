package dk.skancode.skanmate.data.model
interface ColumnLike {
    val dbName: String
    val name: String
    val width: Float
    val type: ColumnType
    val constraints: List<ColumnConstraint>
    val rememberValue: Boolean
}

fun List<ColumnLike>.isAvailableOffline(): Boolean {
    return none { c ->
        c.constraints.any { cc ->
            cc.name == UniqueConstraintName
        }
    }
}
