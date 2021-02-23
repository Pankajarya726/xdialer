package com.xdialer

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    val mimeString: String = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call";
    private var mDelayHandler: Handler? = null
    private val SPLASH_DELAY: Long = 300 //3 mili seconds
    private val mRunnable: Runnable = Runnable {
        if (!isFinishing) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        checkPermission()

    }
    public override fun onDestroy() {

        if (mDelayHandler != null) {
            mDelayHandler!!.removeCallbacks(mRunnable)
        }
        super.onDestroy()
    }
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                Constants.PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {


            loadContacts()
        }
    }
    private fun loadContacts() {

        Log.e(javaClass.simpleName, "loadContacts")
        val databaseHandler: SqliteDb = SqliteDb(this)
        databaseHandler.deleteAllContact()
        val contactList = databaseHandler.getContact()
        if (contactList != null && contactList!!.size > 0) {
        mDelayHandler = Handler()
        mDelayHandler!!.postDelayed(mRunnable, SPLASH_DELAY)
//            val intent = Intent(applicationContext, MainActivity::class.java)
//            startActivity(intent)
//            finish()
        } else {
            getContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Permission must be granted in order to display contacts information",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)

            }


        }


    }

    private fun getContacts() {
        Log.e(javaClass.simpleName, "getContacts")
        val resolver: ContentResolver = contentResolver;
        val cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI, null, null, null,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        if (cursor!!.count > 0) {

//            val mimeString = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call"
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID))
                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val phoneNumber = (cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                ))

                val mimeType: String =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))

                if (mimeType.equals(mimeString)) {
                    val _id = id.toString()
                    val name = name.toString()
                    val number =
                        phoneNumber.toString().replaceAfter(delimiter = "@", replacement = "")
                            .replace(
                                "@",
                                ""
                            )
                    val favorite = 0
                    val databaseHandler: SqliteDb = SqliteDb(this)

                    if (_id.trim() != "" && name.trim() != "" && number.trim() != "") {
                        val status = databaseHandler.addContact(
                            Contact(
                                _id,
                                name,
                                number,
                                favorite
                            )
                        )
                        if (status) {

                            Log.e("tag", "Data inserted");
                            Log.e(
                                "data :",
                                "id : $id\t name : $name\t number : $phoneNumber\t mimeType : $mimeType"
                            );

                        }
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "id or name or email cannot be blank",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
//                    Log.e(
//                        "data :",
//                        "id : $id\t name : $name\t number : $phoneNumber\t mimeType : $mimeType"
//                    );
                }


            }
        }
        cursor.close()

        mDelayHandler = Handler()
        mDelayHandler!!.postDelayed(mRunnable, SPLASH_DELAY)
    }

}