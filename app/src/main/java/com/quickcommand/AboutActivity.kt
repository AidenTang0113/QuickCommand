package com.quickcommand

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.quickcommand.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.selectedItemId = R.id.nav_about
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    finish()
                    true
                }
                R.id.nav_about -> {
                    true
                }
                else -> false
            }
        }
    }
}
