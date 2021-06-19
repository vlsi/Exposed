@file:Suppress("internal")
package org.jetbrains.exposed.sql.statements

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import java.util.*

/**
 * @author max
 */

abstract class UpdateBuilder<out T>(type: StatementType, targets: List<Table>) : Statement<T>(type, targets) {
    protected val values: MutableMap<Column<*>, Any?> = LinkedHashMap()

    open operator fun <S> set(column: Column<S>, value: S) {
        when {
            values.containsKey(column) -> error("$column is already initialized")
            !column.columnType.nullable && value == null -> error("Trying to set null to not nullable column $column")
            else -> {
                column.columnType.validateValueBeforeUpdate(value)
                values[column] = value
            }
        }
    }

    @JvmName("setWithEntityIdExpression")
    operator fun <S : Comparable<S>> set(column: Column<out EntityID<S>?>, value: Expression<out S?>) {
        require(!values.containsKey(column)) { "$column is already initialized" }
        column.columnType.validateValueBeforeUpdate(value)
        values[column] = value
    }

    @JvmName("setWithEntityIdValue")
    operator fun <S : Comparable<S>> set(column: Column<out EntityID<S>?>, value: S?) {
        require(!values.containsKey(column)) { "$column is already initialized" }
        column.columnType.validateValueBeforeUpdate(value)
        values[column] = value
    }

    /**
     * Sets column value to null.
     * This method is helpful for "optional references" since compiler can't decide between
     * "null as T?" and "null as EntityID<T>?".
     */
    fun <T> setNull(column: Column<T?>) = set(column, null as T?)

    open operator fun <S> set(column: Column<S>, value: Expression<out S>) = update(column, value)

    open operator fun <S> set(column: CompositeColumn<S>, value: S) {
        column.getRealColumnsWithValues(value).forEach { (realColumn, itsValue) -> set(realColumn as Column<Any?>, itsValue) }
    }

    open fun <S> update(column: Column<S>, value: Expression<out S>) {
        require(!values.containsKey(column)) { "$column is already initialized" }
        column.columnType.validateValueBeforeUpdate(value)
        values[column] = value
    }

    open fun <S> update(column: Column<S>, value: SqlExpressionBuilder.() -> Expression<out S>) {
        require(!values.containsKey(column)) { "$column is already initialized" }
        val expression = SqlExpressionBuilder.value()
        column.columnType.validateValueBeforeUpdate(expression)
        values[column] = expression
    }
}
