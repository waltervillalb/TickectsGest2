package com.Android.tickects.eventoAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R

class ContactsAdapter(
    private val contactList: List<Contact>,
    private val onContactClicked: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout_contactos, parent, false)
        return ContactViewHolder(view)
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.bind(contact)
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactNameTextView: TextView = itemView.findViewById(R.id.nombreContacto)
        private val contactPhoneNumberTextView: TextView = itemView.findViewById(R.id.telefonoContacto)

        fun bind(contact: Contact) {
            contactNameTextView.text = contact.name
            contactPhoneNumberTextView.text = contact.phoneNumber

            itemView.setOnClickListener {
                onContactClicked(contact)
            }
        }
    }
}