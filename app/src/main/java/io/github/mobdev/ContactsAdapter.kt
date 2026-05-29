package io.github.mobdev

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.databinding.ItemContactBinding

class ContactsAdapter(
    private val onClick: (Contact) -> Unit,
) : ListAdapter<Contact, ContactsAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemContactBinding,
        private val onClick: (Contact) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.contactName.text = contact.name ?: binding.root.context.getString(R.string.value_missing)
            binding.root.setOnClickListener { onClick(contact) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem.name == newItem.name && oldItem.phoneNumber == newItem.phoneNumber

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean = oldItem == newItem
    }
}
