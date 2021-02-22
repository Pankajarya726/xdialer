package com.xdialer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.xdialer.databinding.RowContactBinding

class ContactAdapter(
    context: Context,
    contactList: ArrayList<Contact>,
    listener: CustomClickListener
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private var listener = listener

    private var ctx = context
    private var contactList = contactList
    lateinit var contactBinding: RowContactBinding

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactAdapter.ViewHolder {
        contactBinding =
            DataBindingUtil.inflate(LayoutInflater.from(ctx), R.layout.row_contact, parent, false)
        return ViewHolder(contactBinding)

    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    override fun onBindViewHolder(holder: ContactAdapter.ViewHolder, position: Int) {
        val contact = contactList[position]
        holder.binding.txtName.text = contact.name
        holder.binding.txtNumber.text = contact.number

        holder.binding.imgCall.setOnClickListener(View.OnClickListener {

            listener.onCall(contact.id)
        })


    }

    fun filterList(filterdContact: ArrayList<Contact>) {
        this.contactList = filterdContact
        notifyDataSetChanged()
    }


    class ViewHolder(binding: RowContactBinding) : RecyclerView.ViewHolder(binding.root) {
        var binding = binding
    }


    interface CustomClickListener {
        fun onCall(id: String)
    }


}