package jp.buzza.androidgde

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import jp.buzza.androidgde.extension.startActivity
import jp.buzza.androidgde.extension.startActivityWithoutReified
import jp.buzza.androidgde.widget.FloatingWidgetService

class MainActivity : AppCompatActivity() {

    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // startActivityWithoutReified(this, MainActivity::class.java)
        // startActivity<MainActivity>(context = this)
        //serviceIntent = Intent(this, FloatingWidgetService::class.java)
        //startService(serviceIntent)
    }

    override fun onBackPressed() {
        //stopService(serviceIntent)
        super.onBackPressed()
    }
}
