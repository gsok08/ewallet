package com.example.cash_goblin

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cash_goblin.ui.group.GroupsFragment
import com.example.cash_goblin.HomeFragment
import com.example.cash_goblin.ui.auth.ProfileFragment
import com.example.cash_goblin.ui.auth.TransferFragment
import com.example.cash_goblin.data.models.Transaction
import com.example.cash_goblin.databinding.HomeBinding
import com.example.cash_goblin.ui.history.TransactionHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Home : AppCompatActivity() {

    private lateinit var binding: HomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> replaceFragment(HomeFragment())
                R.id.menu_transfer -> replaceFragment(TransferFragment())
                R.id.menu_history -> replaceFragment(TransactionHistory())
                R.id.menu_groups -> replaceFragment(GroupsFragment())
                R.id.menu_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
