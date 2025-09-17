package com.example.cash_goblin.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cash_goblin.R
import com.example.cash_goblin.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnSignOut: Button

    private val auth = FirebaseAuth.getInstance()
    private val usersDb = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvName = view.findViewById(R.id.tvUserId)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvBalance = view.findViewById(R.id.tvBalance)
        btnSignOut = view.findViewById(R.id.btnSignOut)

        loadProfile()

        btnSignOut.setOnClickListener {
            auth.signOut()
            requireActivity().finish()
        }

        return view
    }

    private fun loadProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            tvName.text = "Not logged in"
            return
        }

        val uid = currentUser.uid
        tvEmail.text = "Email: ${currentUser.email ?: "Not linked"}"

        usersDb.orderByChild("authUid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val user = child.getValue(User::class.java)
                            tvName.text = "User ID: ${user?.userId ?: "-"}"
                            tvPhone.text = "Phone: ${user?.phone ?: "-"}"
                            tvBalance.text = "Balance: RM %.2f".format(user?.balance ?: 0.0)
                            return
                        }
                    } else {
                        tvName.text = "User not found"
                        tvPhone.text = "-"
                        tvBalance.text = "Balance: RM 0.00"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    tvName.text = "Error loading profile"
                }
            })
    }
}
