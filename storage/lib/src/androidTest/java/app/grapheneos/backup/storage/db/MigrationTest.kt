/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.db

import androidx.room.Room.databaseBuilder
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.grapheneos.seedvault.core.toHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.random.Random

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
internal class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        Db::class.java,
        emptyList(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        val chunk1 = CachedChunk(Random.nextBytes(32).toHexString(), 1, 23)
        val chunk2 = CachedChunk(Random.nextBytes(32).toHexString(), 2, 42)
        val chunk3 = CachedChunk(Random.nextBytes(32).toHexString(), 3, 1337)
        val chunks = listOf(chunk1, chunk2, chunk3)
        helper.createDatabase(TEST_DB, 1).use { db ->
            // Database has schema version 1. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            chunks.forEach { c ->
                db.execSQL(
                    "INSERT INTO CachedChunk (id, ref_count, size, version) VALUES (?, ?, ?, ?)",
                    arrayOf(c.id, c.refCount, c.size, c.version),
                )
            }
        }
        // Re-open the database with version 2
        // MigrationTestHelper automatically verifies the schema changes.
        helper.runMigrationsAndValidate(TEST_DB, 2, true).close()

        val db: Db = databaseBuilder(getApplicationContext(), Db::class.java, TEST_DB)
            .allowMainThreadQueries()
            .build()
        chunks.forEach { c ->
            val cachedChunk = db.getChunksCache().get(c.id)
            assertEquals(c.id, cachedChunk?.id)
            assertEquals(c.refCount, cachedChunk?.refCount)
            assertEquals(c.size, cachedChunk?.size)
            assertEquals(c.version, cachedChunk?.version)
            assertFalse(cachedChunk!!.corrupted) // most important: not corrupted after migration
        }
        db.close()
    }
}
