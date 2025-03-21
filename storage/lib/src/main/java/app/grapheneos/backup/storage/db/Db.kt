/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.db

import android.content.Context
import android.net.Uri
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

internal const val DB_MAX_OP = 750

@Database(
    version = 2,
    exportSchema = true,
    entities = [StoredUri::class, CachedFile::class, CachedChunk::class],
    autoMigrations = [
        AutoMigration(1, 2),
    ],
)
@TypeConverters(Converters::class)
internal abstract class Db : RoomDatabase() {
    abstract fun getUriStore(): UriStore
    abstract fun getFilesCache(): FilesCache
    abstract fun getChunksCache(): ChunksCache

    internal companion object {
        fun build(context: Context): Db {
            return Room.databaseBuilder(context, Db::class.java, "seedvault-storage-local-cache")
                .build()
        }
    }

    fun <T> applyInParts(list: Collection<T>, apply: (list: Collection<T>) -> Unit) =
        runInTransaction {
            list.chunked(DB_MAX_OP).forEach {
                apply(it)
            }
        }
}

internal class Converters {
    @TypeConverter
    fun uriToString(value: Uri?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun stringToUri(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    @TypeConverter
    fun chunkIdsToString(value: List<String>?): String? {
        return value?.joinToString("#")
    }

    @TypeConverter
    fun stringToChunkIds(value: String?): List<String>? {
        return value?.split("#")
    }
}
