package com.example.planner.storages

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import com.example.planner.R
import com.example.planner.observer.ErrorObserver
import com.example.planner.observer.StorageObserver
import com.example.planner.task.Task
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import java.lang.ref.WeakReference
import java.util.*


internal object FirebaseStorage : Storage {
    private var taskMap = sortedMapOf<Int, Task>()
    private val observers: MutableList<StorageObserver> = ArrayList()
    private var actualObservers: MutableList<StorageObserver> = ArrayList()
    private val errorObservers: MutableList<ErrorObserver> = ArrayList()

    private val database = FirebaseDatabase.getInstance()
    private val dbReference = database.reference.child("tasks")
    private lateinit var connectManager: ConnectivityManager
    private lateinit var res: Resources

    fun init(context: WeakReference<Context>): FirebaseStorage {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
            })
        res = context.get()?.resources!!
        connectManager = context.get()?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        dbReference.keepSynced(true)

        FirebaseFunctions.getInstance()
        FirebaseMessaging.getInstance().subscribeToTopic("tasks-notifications")
        return this
    }

    override fun addTask(task: Task) {
        checkConnection(res.getString(R.string.networkErrorBack), false)

        var lastId = -1

        val lastQuery = dbReference.orderByKey().limitToLast(1)
        lastQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    lastId = child.getValue(Task::class.java)?.id ?: -1
                }

                task.id = ++lastId

                dbReference.child(task.id.toString()).setValue(
                    task
                ) { databaseError, _ ->
                    taskMap[task.id] = task
                    notifyObservers(taskMap)

                    if (databaseError != null) {

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun removeTask(task: Task) {
        checkConnection(res.getString(R.string.networkErrorSimple), false)

        dbReference.child(task.id.toString())
            .removeValue { _, _ ->
                taskMap.remove(task.id)
                notifyObservers(taskMap)
            }
    }

    override fun editTask(task: Task) {

        checkConnection(res.getString(R.string.networkErrorBack), false)

        val updateMap = mutableMapOf<String, Any?>()
        updateMap["title"] = task.title
        updateMap["description"] = task.description
        updateMap["favorite"] = task.favorite
        updateMap["done"] = task.done

        dbReference.child(task.id.toString())
            .updateChildren(updateMap) { _, _ ->
                taskMap[task.id] = task
                notifyObservers(taskMap)
            }
    }

    override fun getList() {
        checkConnection(res.getString(R.string.networkErrorReload), true)

        actualObservers = observers

        dbReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    val task = postSnapshot.getValue(Task::class.java)
                    taskMap[task?.id] = task
                }
                notifyObservers(taskMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                throw databaseError.toException()
            }
        })
    }

    override fun addObserver(observer: StorageObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: StorageObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers(tasks: Map<Int, Task>) {
        observers.forEach { it.onUpdateMap(tasks) }
    }

    override fun addErrorObserver(observer: ErrorObserver) {
        errorObservers.add(observer)
    }

    override fun removeErrorObserver(observer: ErrorObserver) {
        errorObservers.remove(observer)
    }

    private fun notifyErrorObservers(message: String, reload: Boolean) {
        errorObservers.forEach { it.showError(message, reload) }
    }

    private fun checkConnection(message: String, reload: Boolean) {
        val activeNetwork = connectManager.activeNetworkInfo
        if (activeNetwork == null || !activeNetwork.isConnected) {
            notifyErrorObservers(message, reload)
        }
    }
}