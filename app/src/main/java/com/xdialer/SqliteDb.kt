package com.xdialer

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class SqliteDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val tag: String = this.javaClass.simpleName
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ContactDatabase"
        private const val TABLE_CONTACTS = "Contacts"
        private const val CONTACT_ID = "CONTACT_ID"
        private const val C_ID = "C_ID"
        private const val CONTACT_NAME = "CONTACT_NAME"
        private const val CONTACT_NUMBER = "CONTACT_NUMBER"
        private const val CONTACT_FAVORITE = "CONTACT_FAVORITE"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_CONTACTS_TABLE = ("CREATE TABLE " + TABLE_CONTACTS + "("
                + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + CONTACT_ID + " TEXT," + CONTACT_NAME + " TEXT," + CONTACT_FAVORITE + " INTEGER,"
                + CONTACT_NUMBER + " TEXT" + ")")
        db?.execSQL(CREATE_CONTACTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    fun addContact(contact: Contact): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(CONTACT_ID, contact.id)
        contentValues.put(CONTACT_NAME, contact.name) // EmpModelClass Name
        contentValues.put(CONTACT_NUMBER, contact.number) // EmpModelClass Phone
        contentValues.put(CONTACT_FAVORITE, contact.favorite) // EmpModelClass Phone
        val success = db.insert(TABLE_CONTACTS, null, contentValues)
        Log.e(tag, success.toString())
        db.close()

        if (success > -1) {
            return true
        }
        return false

    }

    fun getContact(): ArrayList<Contact> {
        val empList: ArrayList<Contact> = ArrayList<Contact>()
        val selectQuery = "SELECT  * FROM $TABLE_CONTACTS"
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        var id: Int
        var favorite: Int
        var name: String
        var number: String
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(CONTACT_ID))
                name = cursor.getString(cursor.getColumnIndex(CONTACT_NAME))
                number = cursor.getString(cursor.getColumnIndex(CONTACT_NUMBER))
                favorite = cursor.getInt(cursor.getColumnIndex(CONTACT_FAVORITE))
                val emp =
                    Contact(id = id.toString(), name = name, number = number, favorite = favorite)
                empList.add(emp)
            } while (cursor.moveToNext())
        }
        return empList
    }

    fun updateContact(contact: Contact): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(CONTACT_FAVORITE, contact.favorite)

        val success =
            db.update(TABLE_CONTACTS, contentValues, "$C_ID=" + Integer.parseInt(contact.id), null)
        db.close()
        return success
    }
    fun deleteAllContact(): Int {
        val db = this.writableDatabase
                val success =
            db.delete(TABLE_CONTACTS, null, null)
        db.close()
        return success
    }

}