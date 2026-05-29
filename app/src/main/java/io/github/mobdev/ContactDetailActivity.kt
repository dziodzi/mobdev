package io.github.mobdev

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.databinding.ActivityContactDetailBinding

class ContactDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val contact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CONTACT, Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CONTACT) as? Contact
        }
        if (contact == null) {
            finish()
            return
        }

        val missing = getString(R.string.value_missing)
        binding.detailName.text = contact.name ?: missing
        binding.detailPhone.text = contact.phoneNumber ?: missing
        binding.detailEmail.text = contact.email ?: missing
    }

    companion object {
        const val EXTRA_CONTACT = "extra_contact"
    }
}
