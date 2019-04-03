package com.example.planner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.view.MenuItem
import android.widget.Toast
import com.example.planner.fragments.AddTaskFragment
import com.example.planner.fragments.MainContentFragment
import com.example.planner.fragments.SettingsFragment
import com.example.planner.observer.FragmentListener
import com.example.planner.storages.STORAGE_TYPE_EXTERNAL
import com.example.planner.storages.STORAGE_TYPE_SHARED
import kotlinx.android.synthetic.main.activity_main.*

const val CHECK_REQUEST = 3
const val FRAGMENT_TAG_ADD_TASK = "FragmentAdd"

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, FragmentListener {
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        supportFragmentManager.beginTransaction()
            .add(R.id.content_fragments, MainContentFragment())
            .commit()

        init()
    }

    private fun init() {
        drawerLayout = findViewById(R.id.drawer_layout)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        if(fragment is SettingsFragment) {
            fragment.setFragmentListener(this)
        } else if (fragment is AddTaskFragment) {
            fragment.setFragmentListener(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val addFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_ADD_TASK)
            if (addFragment != null && addFragment.isVisible) {
                supportFragmentManager.popBackStack()
                return true
            }

            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> supportFragmentManager.beginTransaction()
                .replace(R.id.content_fragments, SettingsFragment())
                .addToBackStack(null)
                .commit()
            R.id.nav_tasks -> supportFragmentManager.beginTransaction()
                .replace(R.id.content_fragments, MainContentFragment())
                .addToBackStack(null)
                .commit()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun checkPermissions() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
        if (pref.getBoolean(STORAGE_TYPE_EXTERNAL, false)) {
            val permissionWrite = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
                makeRequest()
            } else {
                //presenter.getTasksList()
            }
        } else {
            //presenter.getTasksList()
        }
    }

    override fun setupActionBar(title: String, iconId: Int) {
        supportActionBar?.apply {
            this.title = title
            setHomeAsUpIndicator(iconId)
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            CHECK_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == CHECK_REQUEST) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val pref = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                val editor = pref.edit()
                editor.putBoolean(STORAGE_TYPE_SHARED, true)
                editor.putBoolean(STORAGE_TYPE_EXTERNAL, false)
                editor.apply()
                Toast.makeText(this@MainActivity, R.string.error_external_permission, Toast.LENGTH_SHORT).show()
                //presenter.getTasksList()
            } else {
                //presenter.getTasksList()
            }
        }
    }
}
