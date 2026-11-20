package com.altea.salud

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class LocalStore(context: Context) : SQLiteOpenHelper(context, "altea_local.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE readings(id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER, bpm INTEGER, movement REAL, tilt REAL, fall INTEGER)")
        db.execSQL("CREATE TABLE symptoms(id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER, text TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addReading(r: SensorReading) {
        val v = ContentValues().apply {
            put("time", System.currentTimeMillis()); put("bpm", r.bpm)
            put("movement", r.movement); put("tilt", r.tilt); put("fall", if (r.fall) 1 else 0)
        }
        writableDatabase.insert("readings", null, v)
    }
    fun addSymptom(text: String) {
        val v = ContentValues().apply { put("time", System.currentTimeMillis()); put("text", text) }
        writableDatabase.insert("symptoms", null, v)
    }
    fun latestReadings(limit: Int = 100): List<SensorReading> {
        val out = mutableListOf<SensorReading>()
        readableDatabase.rawQuery("SELECT bpm,movement,tilt,fall FROM readings ORDER BY time DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) out.add(SensorReading(c.getInt(0), c.getFloat(1), c.getFloat(2), c.getInt(3) == 1))
        }
        return out.reversed()
    }
    fun latestSymptoms(): List<String> {
        val out = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT text FROM symptoms ORDER BY time DESC LIMIT 30", null).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }
}
