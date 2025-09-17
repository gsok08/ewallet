package com.example.cash_goblin.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cash_goblin.MembersAdapter
import com.example.cash_goblin.MessagesAdapter
import com.example.cash_goblin.R
import com.example.cash_goblin.data.models.Group
import com.example.cash_goblin.data.models.GroupMessage
import com.example.cash_goblin.data.models.MemberContribution
import com.example.cash_goblin.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupDetailFragment : Fragment() {

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        fun newInstance(groupId: String) = GroupDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_GROUP_ID, groupId) }
        }
    }

    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var groupId: String


    private lateinit var tvGroupName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var rvMembers: RecyclerView
    private lateinit var rvMessages: RecyclerView


    private lateinit var membersAdapter: MembersAdapter
    private val members = mutableListOf<Pair<String, MemberContribution>>()

    private lateinit var messagesAdapter: MessagesAdapter
    private val messages = mutableListOf<GroupMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = requireArguments().getString(ARG_GROUP_ID) ?: ""
        db = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_groupdetail, container, false)

        tvGroupName = v.findViewById(R.id.tvGroupName)
        progressBar = v.findViewById(R.id.progressBar)
        tvProgressText = v.findViewById(R.id.tvProgressText)

        val etMessage = v.findViewById<EditText>(R.id.etMessage)
        val btnSend = v.findViewById<Button>(R.id.btnSend)

        val etPhone = v.findViewById<EditText>(R.id.etPhone)
        val btnAddMember = v.findViewById<Button>(R.id.btnAddMember)

        val etAmount = v.findViewById<EditText>(R.id.etMemberAmount)
        val btnAddContribution = v.findViewById<Button>(R.id.btnAddContribution)

        rvMembers = v.findViewById(R.id.rvMembers)
        membersAdapter = MembersAdapter(members) { _, _ -> }
        rvMembers.layoutManager = LinearLayoutManager(requireContext())
        rvMembers.adapter = membersAdapter

        rvMessages = v.findViewById(R.id.rvMessages)
        messagesAdapter = MessagesAdapter(messages, auth.currentUser?.uid ?: "anon")
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
        rvMessages.adapter = messagesAdapter

        btnSend.setOnClickListener {
            val txt = etMessage.text.toString().trim()
            if (txt.isNotEmpty()) {
                sendMessage(groupId, txt)
                etMessage.text.clear()
            }
        }

        btnAddMember.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                addMemberByPhone(groupId, phone)
                etPhone.text.clear()
            } else {
                Toast.makeText(requireContext(), "Enter phone number", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddContribution.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (amount <= 0.0) {
                Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addContribution(amount)
        }

        listenGroup()
        listenMessages()

        return v
    }

    private fun listenGroup() {
        val ref = db.child("groups").child(groupId)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val g = snapshot.getValue(Group::class.java) ?: return
                tvGroupName.text = g.name

                var total = 0.0
                g.members?.forEach { (_, contrib) -> total += contrib.amount }
                progressBar.max = g.targetAmount.toInt()
                progressBar.progress = total.toInt()
                tvProgressText.text = "RM %.2f / RM %.2f".format(total, g.targetAmount)

                members.clear()
                g.members?.forEach { (uid, contrib) -> members.add(uid to contrib) }
                membersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenMessages() {
        val ref = db.child("groups").child(groupId).child("messages")
        ref.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val m = snapshot.getValue(GroupMessage::class.java) ?: return
                messages.add(m)
                messagesAdapter.notifyItemInserted(messages.size - 1)
                rvMessages.scrollToPosition(messages.size - 1)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendMessage(groupId: String, text: String) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val userRef = db.child("users")

        userRef.orderByChild("authUid").equalTo(userId).get()
            .addOnSuccessListener { snapshot ->
                var senderName = "Anonymous"
                for (child in snapshot.children) {
                    senderName = child.child("userId").getValue(String::class.java) ?: "Anonymous"
                }

                val messageRef = db.child("groups").child(groupId).child("messages").push()
                val message = GroupMessage(
                    senderId = userId,
                    senderName = senderName,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )

                messageRef.setValue(message)
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to get name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMemberByPhone(groupId: String, phone: String) {
        val usersRef = db.child("users")
        usersRef.orderByChild("phone").equalTo(phone).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No user found with that phone", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val user = snapshot.children.first().getValue(User::class.java) ?: return@addOnSuccessListener
                val memberRef = db.child("groups").child(groupId).child("members").child(user.userId)
                val member = MemberContribution(phone = user.phone, amount = 0.0)
                memberRef.setValue(member)
                    .addOnSuccessListener { Toast.makeText(requireContext(), "Member added", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireContext(), "Failed to add member", Toast.LENGTH_SHORT).show() }
            }
            .addOnFailureListener { Toast.makeText(requireContext(), "Error finding user", Toast.LENGTH_SHORT).show() }
    }

    private fun addContribution(amount: Double) {
        val authUid = auth.currentUser?.uid ?: return
        db.child("users").child(authUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) ?: return
                    val userId = if (user.userId.isNotEmpty()) user.userId else snapshot.key ?: return

                    val contribRef = db.child("groups").child(groupId).child("members").child(userId)
                    contribRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val member = currentData.getValue(MemberContribution::class.java)
                                ?: MemberContribution(phone = user.phone, amount = 0.0)
                            currentData.value = member.copy(amount = member.amount + amount)
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            if (committed) {
                                Toast.makeText(requireContext(), "Contribution added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed: ${error?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
