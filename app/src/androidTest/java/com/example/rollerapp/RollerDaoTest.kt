package com.example.rollerapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rollerapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RollerDaoTest {
    private lateinit var rollerDao: RollerDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        rollerDao = db.rollerDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeConveyorAndReadInList() = runBlocking {
        val conveyor = Conveyor("Conv1")
        rollerDao.insertConveyor(conveyor)
        val allConveyors = rollerDao.getAllConveyors().first()
        assertEquals(allConveyors[0].name, "Conv1")
    }

    @Test
    fun insertAndGetInspection() = runBlocking {
        rollerDao.insertConveyor(Conveyor("Conv1"))
        val inspection = Inspection(
            conveyorId = "Conv1",
            opora = 100,
            roller = "Center",
            damage = "Noise",
            spare = false
        )
        rollerDao.insertInspection(inspection)
        val inspections = rollerDao.getInspectionsByConveyorSync("Conv1")
        assertEquals(inspections.size, 1)
        assertEquals(inspections[0].opora, 100)
    }
}
