package com.example.cash_goblin.ui.group

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.cash_goblin.ui.group.GroupDetailFragment
import com.example.cash_goblin.data.models.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.cash_goblin.R
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.cash_goblin.data.models.MemberContribution
import com.example.cash_goblin.data.models.User

class GroupsFragment : Fragment() {

    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var lvGroups: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val groupItems = mutableListOf<String>()
    private val groupsMap = mutableMapOf<String, Group>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_grouplist, container, false)
        db = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        lvGroups = v.findViewById(R.id.lvGroups)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, groupItems)
        lvGroups.adapter = adapter

        val etGroupName = v.findViewById<EditText>(R.id.etGroupName)
        val etTarget = v.findViewById<EditText>(R.id.etTarget)
        val btnCreate = v.findViewById<Button>(R.id.btnCreateGroup)

        btnCreate.setOnClickListener {
            val name = etGroupName.text.toString().trim()
            val target = etTarget.text.toString().toDoubleOrNull() ?: 0.0
            if (name.isEmpty() || target <= 0.0) {
                Toast.makeText(requireContext(), "Enter valid name and target", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createGroup(name, target)
            etGroupName.text.clear()
            etTarget.text.clear()
        }

        lvGroups.setOnItemClickListener { _, _, pos, _ ->
            val groupId = groupsMap.keys.toList()[pos]
            openGroup(groupId)
        }
        Log.d("GroupsFragment", "Fragment created, calling listenForGroups()")


        listenForGroups()
        return v
    }

    private fun listenForGroups() {
        val authUid = auth.currentUser?.uid ?: return

        db.child("users").orderByChild("authUid").equalTo(authUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.e("GroupsFragment", "No user record found for authUid=$authUid")
                        return
                    }

                    val user = snapshot.children.first().getValue(User::class.java) ?: return
                    val userId = user.userId

                    val ref = db.child("groups")
                    ref.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            groupItems.clear()
                            groupsMap.clear()

                            snapshot.children.forEach { child ->
                                val g = child.getValue(Group::class.java) ?: return@forEach

                                if (g.members.containsKey(userId)) {
                                    groupsMap[g.groupId] = g
                                    groupItems.add("${g.name} — target: ${g.targetAmount}")
                                }
                            }

                            adapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("GroupsFragment", "DB error: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("GroupsFragment", "User lookup failed: ${error.message}")
                }
            })
    }





    private fun createGroup(name: String, target: Double) {
        val authUid = auth.currentUser?.uid ?: return

        db.child("users").orderByChild("authUid").equalTo(authUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val user = snapshot.children.first().getValue(User::class.java) ?: return
                    val userId = user.userId
                    val phone = user.phone

                    val id = db.child("groups").push().key ?: return

                    val member = MemberContribution(phone = phone, amount = 0.0)

                    val group = Group(
                        groupId = id,
                        name = name,
                        targetAmount = target,
                        createdBy = userId,
                        members = mapOf(userId to member)
                    )

                    db.child("groups").child(id).setValue(group)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }



    private fun openGroup(groupId: String) {
        parentFragmentManager.commit {
            replace<GroupDetailFragment>(R.id.fragment_container, args = Bundle().apply {
                putString("group_id", groupId)
            })
            addToBackStack(null)
        }
    }
}
