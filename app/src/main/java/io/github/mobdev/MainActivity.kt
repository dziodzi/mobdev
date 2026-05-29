package io.github.mobdev

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ContactsAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            loadContacts()
        } else {
            showPermissionRequired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = ContactsAdapter { contact ->
            val intent = Intent(this, ContactDetailActivity::class.java).apply {
                putExtra(ContactDetailActivity.EXTRA_CONTACT, contact)
            }
            startActivity(intent)
        }

        binding.contactsList.layoutManager = LinearLayoutManager(this)
        binding.contactsList.adapter = adapter

        binding.btnGrant.setOnClickListener { requestContactsPermission() }

        binding.swipeRefresh.setOnRefreshListener {
            if (hasContactsPermission()) {
                loadContacts()
            } else {
                binding.swipeRefresh.isRefreshing = false
                showPermissionRequired()
            }
        }

        updateScreen()
    }

    override fun onResume() {
        super.onResume()
        updateScreen()
    }

    private fun updateScreen() {
        if (hasContactsPermission()) {
            loadContacts()
        } else {
            showPermissionRequired()
        }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestContactsPermission() {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun showPermissionRequired() {
        binding.swipeRefresh.isRefreshing = false
        binding.permissionBlock.visibility = android.view.View.VISIBLE
        binding.contactsList.visibility = android.view.View.GONE
        binding.emptyView.visibility = android.view.View.GONE
    }

    private fun loadContacts() {
        binding.permissionBlock.visibility = android.view.View.GONE
        binding.contactsList.visibility = android.view.View.VISIBLE

        val contacts = fetchAllContacts()
        adapter.submitList(contacts)
        binding.swipeRefresh.isRefreshing = false

        val isEmpty = contacts.isEmpty()
        binding.contactsList.visibility =
            if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
        binding.emptyView.visibility =
            if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
    }
}
