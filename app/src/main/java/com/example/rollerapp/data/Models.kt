package com.example.rollerapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conveyors")
data class Conveyor(
    @PrimaryKey val name: String = "",
    val number: String = "",
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inspections",
    indices = [Index(value = ["conveyor"])],
    foreignKeys = [
        ForeignKey(
            entity = Conveyor::class,
            parentColumns = ["name"],
            childColumns = ["conveyor"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Inspection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conveyor") val conveyorId: String = "",
    val opora: Int = 0,
    val roller: String = "",
    val damage: String = "",
    val spare: Boolean = false,
    val nearestRoller: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "replacements",
    indices = [Index(value = ["conveyor"])],
    foreignKeys = [
        ForeignKey(
            entity = Conveyor::class,
            parentColumns = ["name"],
            childColumns = ["conveyor"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Replacement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conveyor") val conveyorId: String = "",
    val opora: Int = 0,
    val roller: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "belt_journal",
    indices = [Index(value = ["conveyor"])],
    foreignKeys = [
        ForeignKey(
            entity = Conveyor::class,
            parentColumns = ["name"],
            childColumns = ["conveyor"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BeltJournal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conveyor") val conveyorId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
