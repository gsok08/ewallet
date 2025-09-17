package com.example.cash_goblin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cash_goblin.data.models.User
import com.example.cash_goblin.ui.auth.RequestMoney
import com.example.cash_goblin.ui.auth.SendMoney
import com.example.cash_goblin.ui.auth.TopUpFragment
import com.example.cash_goblin.ui.auth.TransferFragment
import com.example.cash_goblin.ui.group.GroupsFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment() {

    private lateinit var tvBalance: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var btnTopUp: Button
    private lateinit var btnSend: Button
    private lateinit var btnScanPay: Button
    private lateinit var btnRequest: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvBalance = view.findViewById(R.id.tv_balance)
        tvWelcome = view.findViewById(R.id.tv_welcome)
        btnTopUp = view.findViewById(R.id.btn_top_up)
        btnSend = view.findViewById(R.id.btn_send)
        btnScanPay = view.findViewById(R.id.btn_scan_pay)
        btnRequest = view.findViewById(R.id.btn_request)

        loadUserData()

        setupQuickActions()

        return view
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            tvBalance.text = "Not signed in"
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("users")
        tvBalance.text = "Loading..."

        ref.orderByChild("authUid").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    tvBalance.text = "No match for UID: $uid"
                    return@addOnSuccessListener
                }

                var found = false
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null) {
                        tvBalance.text = "RM %.2f".format(user.balance)
                        tvWelcome.text = "Welcome, ${user.userId}"
                        found = true
                        break
                    }
                }

                if (!found) {
                    tvBalance.text = "User data structure mismatch"
                }
            }
            .addOnFailureListener { e ->
                tvBalance.text = "Error: ${e.message}"
            }
    }



    private fun setupQuickActions() {
        btnTopUp.setOnClickListener {
            navigateTo(TopUpFragment())
        }

        btnSend.setOnClickListener {
            navigateTo(SendMoney())
        }

        btnScanPay.setOnClickListener {
            navigateTo(GroupsFragment())
        }

        btnRequest.setOnClickListener {
            navigateTo(RequestMoney())
        }
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
