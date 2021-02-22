package com.xdialer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdialer.Constants.Companion.PERMISSIONS_REQUEST_PHONE_CALL
import com.xdialer.Constants.Companion.PERMISSIONS_REQUEST_READ_CONTACTS
import im.dlg.dialer.DialpadActivity
import im.dlg.dialer.DialpadFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//8602119024
class MainActivity : AppCompatActivity(), ContactAdapter.CustomClickListener,
    DialpadFragment.Callback {

    val mimeString: String = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call";
    private var rvContact: RecyclerView? = null
    private var searchEditText: EditText? = null
    private var edtPhoneNumber: EditText? = null
    private var lblinfo: TextView? = null
    private var contactAdapter: ContactAdapter? = null
    private var contactList: ArrayList<Contact>? = ArrayList()
    private var imgSearch: ImageView? = null
    private var searching: Boolean = false

    private var cId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvContact = findViewById<RecyclerView>(R.id.rv_contact)
        searchEditText = findViewById<EditText>(R.id.editTextSearch)
        lblinfo = findViewById<TextView>(R.id.lblinfo)
        edtPhoneNumber = findViewById<EditText>(R.id.edtPhoneNumber)
        edtPhoneNumber!!.isFocusable = false
        edtPhoneNumber!!.isEnabled = false
        imgSearch = findViewById<ImageView>(R.id.imgSearch)
        searchEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable) {
                //after the change calling the method and passing the search input
                filter(editable.toString())
            }
        })



        imgSearch!!.setOnClickListener(View.OnClickListener {


            Log.e(javaClass.simpleName, "imagesearch click")

            if (searching) {
                imgSearch!!.setImageDrawable(this.getDrawable(R.drawable.search))
                searchEditText!!.setText("")

                val imm =
                    applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                searchEditText!!.visibility = View.GONE

                searching = false
            } else {
                imgSearch!!.setImageDrawable(this.getDrawable(R.drawable.close))
                searchEditText!!.visibility = View.VISIBLE

                val imm =
                    applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                searching = true
            }
        })

        checkPermission()


        findViewById<ImageView>(R.id.fab_dial).setOnClickListener(View.OnClickListener {

            val intent = Intent(this, DialpadActivity::class.java)
            intent.putExtra(DialpadActivity.EXTRA_FORMAT_AS_YOU_TYPE, true)
            startActivityForResult(intent, 100)
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                var formatted = data!!.getStringExtra(DialpadActivity.EXTRA_RESULT_FORMATTED);
                var raw = data!!.getStringExtra(DialpadActivity.EXTRA_RESULT_RAW);

                Log.e(javaClass.simpleName, "formatted->$formatted")
                Log.e(javaClass.simpleName, "raw->$raw")

                if (raw != null && raw.isNotEmpty()) {
                    makeWhatsappCall(raw)
                }


            }
        }

    }

    private fun makeWhatsappCall(raw: String) {

        if (checkPhoneNumberExist(raw, applicationContext)!!) {
            val name = getContactNameByPhoneNumber(raw);
            val id = getContactIdForWhatsAppCall(name, applicationContext)
            if (id!!.toInt() != 0) {
                onCall(id.toString())
            } else {
                Log.e("Error--->", "error")
            }
        }else{
            val name = getContactNameByPhoneNumber(raw);
            val id = getContactIdForWhatsAppCall(name, applicationContext)
            if (id!!.toInt() != 0) {
                onCall(id.toString())
            } else {
                Log.e("Error--->", "error")
            }
        }

    }




    private fun insertContactDisplayName(
        addContactsUri: Uri,
        rawContactId: Long,
        displayName: String,
        context: Context
    ) {


        Log.e(javaClass.simpleName, "insertContactDisplayName-->");

        val contentValues = ContentValues();

        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

        // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
        contentValues.put(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        );

        // Put contact display name value.
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName);

        var result = context.contentResolver.insert(addContactsUri, contentValues);
        Log.e(javaClass.simpleName, "result-->$result");

    }

    private fun insertContactPhoneNumber(
        addContactsUri: Uri,
        rawContactId: Long,
        phoneNumber: String,
        phoneTypeStr: String,
        context: Context
    ): Boolean {

        Log.e(javaClass.simpleName, "insertContactPhoneNumber-->");
        // Create a ContentValues object.
        val contentValues = ContentValues()

        // Each contact must has an id to avoid java.lang.IllegalArgumentException: raw_contact_id is required error.
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)

        // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
        contentValues.put(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
        )

        // Put phone number value.
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)

        // Calculate phone type by user selection.
        var phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        if ("home".equals(phoneTypeStr, ignoreCase = true)) {
            phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        } else if ("mobile".equals(phoneTypeStr, ignoreCase = true)) {
            phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        } else if ("work".equals(phoneTypeStr, ignoreCase = true)) {
            phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        }
        // Put phone type value.
        contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, phoneContactType)

        // Insert new contact data into phone contact list.
        var v = context.contentResolver.insert(addContactsUri, contentValues)
        Log.e(javaClass.simpleName, "insertContactPhoneNumber-->$v");

        return v != null


    }


    private fun filter(text: String) {

        try {
            val filterdConact: ArrayList<Contact> = ArrayList()


            for (c in contactList!!) {
                //if the existing elements contains the search input
                if (c.name.toLowerCase().contains(text.toLowerCase())) {
                    //adding the element to filtered list
                    filterdConact.add(c)
                }
            }


            contactAdapter!!.filterList(filterdConact!!)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.toString())
        }


    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {


            loadContacts()
        }
    }

    private fun loadContacts() {

        Log.e(javaClass.simpleName, "loadContacts")
        val databaseHandler: SqliteDb = SqliteDb(this)
        contactList = databaseHandler.getContact()
        if (contactList != null && contactList!!.size > 0) {
            Log.e(javaClass.simpleName, "Total contact" + contactList!!.size.toString());
            contactAdapter = ContactAdapter(this!!, contactList!!, this)
            rvContact!!.layoutManager = LinearLayoutManager(this)
            rvContact!!.hasFixedSize()
            rvContact!!.adapter = contactAdapter!!

        } else {
            getContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                if (requestCode == Constants.PERMISSIONS_REQUEST_PHONE_CALL) {
                    if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            applicationContext,
                            "Permission granted please try again!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "could not make call due to permission denied",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                Toast.makeText(
                    applicationContext,
                    "Permission must be granted in order to display contacts information",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (requestCode == PERMISSIONS_REQUEST_PHONE_CALL) {
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {


                if (cId.isNotEmpty()) {
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$cId"),
                        mimeString
                    )
                    intent.setPackage("com.whatsapp")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Permission granted.",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                Toast.makeText(
                    applicationContext,
                    "Could not make call due to Permission Denied",
                    Toast.LENGTH_LONG
                ).show()
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

    }

    override fun onCall(id: String) {

        Log.e("contact id", id)

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW

        intent.setDataAndType(
            Uri.parse("content://com.android.contacts/data/$id"),
            mimeString
        )
        intent.setPackage("com.whatsapp")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                cId = id
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    PERMISSIONS_REQUEST_PHONE_CALL
                )
            } else {
                startActivity(intent);
            }
        } else {
            startActivity(intent);
        }
    }


    @SuppressLint("LongLogTag")
    private fun checkPhoneNumberExist(phoneNumber: String, context: Context): Boolean? {
        var inserted: Boolean? = false
        val nameOfContactNumber = getContactNameByPhoneNumber(phoneNumber)
        if (nameOfContactNumber.isEmpty()) {
            val addContactsUri: Uri = ContactsContract.Data.CONTENT_URI
            val rowContactId: Long = getRawContactId()
            insertContactDisplayName(addContactsUri, rowContactId, phoneNumber, context)
            inserted = insertContactPhoneNumber(
                addContactsUri,
                rowContactId,
                phoneNumber,
                "mobile",
                context
            )
        }
        Log.e("checkPhoneNumberExist--->",inserted.toString())
        return inserted
    }


    @SuppressLint("LongLogTag")
    fun getContactNameByPhoneNumber(phoneNumber: String): String {
        var contactName: String = ""
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME);
        val cursor: Cursor? = this.contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Log.e(javaClass.simpleName, "cursor -->1")
                contactName = cursor.getString(0);
            }
        }
        Log.e("getContactNameByPhoneNumber--->",contactName.toString())
        return contactName
    }

    private fun addToContact(phoneNumber: String, context: Context) {
        var addContactsUri: Uri = ContactsContract.Data.CONTENT_URI;
        var rowContactId: Long = getRawContactId();
        insertContactDisplayName(addContactsUri, rowContactId, phoneNumber, context)
        var inserted: Boolean = insertContactPhoneNumber(
            addContactsUri,
            rowContactId,
            phoneNumber,
            "mobile",
            context
        )
    }

    @SuppressLint("LongLogTag")
    private fun getContactIdForWhatsAppCall(name: String, context: Context): String? {
        var phoneContactID: String? = ""
        if (name.isNotEmpty()) {
            phoneContactID =  getInfo(name)
        }
        Log.e("getContactIdForWhatsAppCall--->",phoneContactID.toString())
        return phoneContactID
    }

    private fun getInfo(name: String): String? {
        var phoneContactID: String? = ""
        val cursor: Cursor? = this.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            ContactsContract.Data.DISPLAY_NAME + "=? and " + ContactsContract.Data.MIMETYPE + "=?",
            arrayOf(name, "$mimeString"),
            ContactsContract.Contacts.DISPLAY_NAME
        )
        Log.e(javaClass.simpleName, "cursor")
        if (cursor!!.moveToFirst()) {
            phoneContactID =
                cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID))
            Log.e("name--->\t$name", " \nid------>\t$phoneContactID")
        }
        cursor.close()
        Log.e("ngetInfo" ,phoneContactID.toString())
        return phoneContactID
    }

//    public int getContactIdForWhatsAppVideoCall(String name,Context context)
//    {
//        Cursor cursor = getContentResolver ().query(
//            ContactsContract.Data.CONTENT_URI,
//            new String []{ ContactsContract.Data._ID },
//            ContactsContract.Data.DISPLAY_NAME + "=? and " + ContactsContract.Data.MIMETYPE + "=?",
//            new String [] { name, "vnd.android.cursor.item/vnd.com.whatsapp.video.call" },
//            ContactsContract.Contacts.DISPLAY_NAME
//        );
//
//        if (cursor.getCount() > 0) {
//            cursor.moveToFirst();
//            int phoneContactID = cursor . getInt (cursor.getColumnIndex(ContactsContract.Data._ID));
//            return phoneContactID;
//        } else {
//            System.out.println("8888888888888888888          ");
//            return 0;
//        }
//    }

    private fun getRawContactId(): Long {
        // Inser an empty contact.
        val contentValues: ContentValues = ContentValues();
        val rawContactUri: Uri? = contentResolver.insert(
            ContactsContract.RawContacts.CONTENT_URI,
            contentValues
        );
        // Get the newly created contact raw id.
        val ret: Long = ContentUris.parseId(rawContactUri!!);
        return ret;
    }

    override fun ok(formatted: String?, raw: String?) {

        Log.e(javaClass.simpleName, formatted!!)
        Log.e(javaClass.simpleName, raw!!)
    }

    fun buttonClickEvent(v: View) {
        var phoneNo: String = edtPhoneNumber!!.getText().toString()
        try {


            when (v.id) {
                R.id.btnAterisk -> {
                    lblinfo!!.setText("")
                    phoneNo += "*"
                    edtPhoneNumber!!.setText(phoneNo)


                }
                R.id.btnHash -> {
                    lblinfo!!.setText("")
                    phoneNo += "#"
                    edtPhoneNumber!!.setText(phoneNo)


                }
                R.id.btnZero -> {
                    lblinfo!!.setText("")
                    phoneNo += "0"
                    edtPhoneNumber!!.setText(phoneNo)

//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }
                }
                R.id.btnOne -> {
                    lblinfo!!.setText("")
                    phoneNo += "1"
                    edtPhoneNumber!!.setText(phoneNo)
//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }

                }
                R.id.btnTwo -> {
                    lblinfo!!.setText("")
                    phoneNo += "2"
                    edtPhoneNumber!!.setText(phoneNo)

//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }
                }
                R.id.btnThree -> {
                    lblinfo!!.setText("")
                    phoneNo += "3"
                    edtPhoneNumber!!.setText(phoneNo)
//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }

                }
                R.id.btnFour -> {
                    lblinfo!!.setText("")
                    phoneNo += "4"
                    edtPhoneNumber!!.setText(phoneNo)

//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }
                }
                R.id.btnFive -> {
                    lblinfo!!.setText("")
                    phoneNo += "5"
                    edtPhoneNumber!!.setText(phoneNo)
//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }

                }
                R.id.btnSix -> {
                    lblinfo!!.setText("")
                    phoneNo += "6"
                    edtPhoneNumber!!.setText(phoneNo)
//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }

                }
                R.id.btnSeven -> {
                    lblinfo!!.setText("")
                    phoneNo += "7"
                    edtPhoneNumber!!.setText(phoneNo)
//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }

                }
                R.id.btnEight -> {
                    lblinfo!!.setText("")
                    phoneNo += "8"
                    edtPhoneNumber!!.setText(phoneNo)
//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }

                }
                R.id.btnNine -> {
                    lblinfo!!.setText("")
                    phoneNo += "9"
                    edtPhoneNumber!!.setText(phoneNo)

//                    if(phoneNo.length==10){
//                        addToContact(phoneNo,this)
//                    }
                }
                R.id.btndel -> {

                    Log.e(javaClass.simpleName, "btndel--> ")
                    lblinfo!!.setText("")
                    if (phoneNo != null && phoneNo.length > 0) {
                        phoneNo = phoneNo.substring(0, phoneNo.length - 1)

                    }
                    edtPhoneNumber!!.setText(phoneNo)


                }
                R.id.btnClearAll -> {
                    lblinfo!!.setText("")
                    edtPhoneNumber!!.setText("")
                }
                R.id.btnCall -> if (phoneNo.trim { it <= ' ' } == "") {
                    lblinfo!!.setText("Please enter a number to call on!")
                } else {
                    val isHash = false
                    if (phoneNo.subSequence(phoneNo.length - 1, phoneNo.length) == "#") {
                        phoneNo = phoneNo.substring(0, phoneNo.length - 1)
                        val callInfo = "tel:" + phoneNo + Uri.encode("#")
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse(callInfo)
                        startActivity(callIntent)
                    } else {

//                        getContactName(phoneNo, this);

                        makeWhatsappCall(phoneNo)
                    }
                }
            }


        } catch (ex: Exception) {
        }
    }
}

