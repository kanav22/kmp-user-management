package com.kanav.usermanager

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kanav.usermanager.data.local.db.UserDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightInsertOrIgnoreTest {

    private lateinit var db: UserDatabase

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        UserDatabase.Schema.create(driver)
        db = UserDatabase(driver)
    }

    @Test
    fun `INSERT OR IGNORE preserves addedAt on duplicate id`() {
        val originalTime = "2026-01-01T10:00:00Z"
        val laterTime    = "2026-06-01T10:00:00Z"

        db.userQueries.insertOrIgnore(1L, "Alice", "alice@example.com", "female", "active", originalTime)
        db.userQueries.insertOrIgnore(1L, "Alice", "alice@example.com", "female", "active", laterTime)

        val stored = db.userQueries.selectById(1L).executeAsOne()
        assertEquals(originalTime, stored.addedAt)
    }

    @Test
    fun `INSERT OR IGNORE inserts new row when id is unique`() {
        db.userQueries.insertOrIgnore(2L, "Bob", "bob@example.com", "male", "active", "2026-01-01T10:00:00Z")
        val count = db.userQueries.selectAllOrderedByAddedAt().executeAsList().size
        assertEquals(1, count)
    }
}
