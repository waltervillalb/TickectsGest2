package com.Android.tickects

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.Android.tickects.Dialogs.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {
    private lateinit var fab: FloatingActionButton
    private lateinit var frameLayout: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fab = findViewById(R.id.fab)
        frameLayout = findViewById(R.id.frame_layout)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)


        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.abrir_drawer, R.string.cerrar_drawer)
        drawerLayout.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView.setNavigationItemSelectedListener (this)




        bottomNavigationView.background = null
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.shorts -> replaceFragment(EntradasFragment())
                R.id.subscriptions -> replaceFragment(EventorFragment())
                R.id.library -> replaceFragment(ContactosFragment())

            }
            true
        }
        /*fab.setOnClickListener {
            Log.d("ClickEvent", "Floating Action Button Clicked!")
            val fragment = addEntradasFragment()
            supportFragmentManager.beginTransaction().replace(R.id.frame_layout, fragment).addToBackStack(null).commit()
        }*/
        replaceFragment(HomeFragment())


        val db = FirebaseFirestore.getInstance()

// Obtén el UID del usuario utilizando tu clase userId
        val uid = userId.iduser

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val nombre = documentSnapshot.getString("nombre")
                    val correo = documentSnapshot.getString("correo")

                    // Actualiza los TextViews con los datos obtenidos
                    val nombreTextView = findViewById<TextView>(R.id.nombre_Cabecera)
                    val correoTextView = findViewById<TextView>(R.id.correo_cabecera)

                    nombreTextView.text = nombre
                    correoTextView.text = correo
                } else {
                    // El documento no existe
                    // Manejar el caso en el que no se encuentre el usuario en la base de datos
                }
            }
            .addOnFailureListener { exception ->
                // Manejar errores si la consulta a la base de datos falla
                Log.e(TAG, "Error al obtener datos del usuario: $exception")
            }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    private fun showBottomDialog() {
        // Tu código para mostrar el diálogo flotante
    }

    private fun logoutUser() {
        // Realiza aquí las acciones necesarias para cerrar sesión, por ejemplo, en Firebase.
        // Ejemplo de cierre de sesión en Firebase:
        FirebaseAuth.getInstance().signOut()

        // Redirige al usuario a la pantalla de inicio de sesión o la pantalla principal.
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()  // Esto cierra la actividad actual, evitando que el usuario retroceda a la sesión cerrada.
    }
    override fun onBackPressed() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage("¿Desea salir de la aplicación?")
        alertDialogBuilder.setPositiveButton("Sí") { _, _ ->
            super.onBackPressed()
        }
        alertDialogBuilder.setNegativeButton("No") { _, _ -> }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cerrarSesion -> {
                logoutUser()
                drawerLayout.closeDrawer(GravityCompat.START) // Cierra el cajón después de la selección.
                return true
            }
            R.id.CONFentradas -> {
                replaceFragment(AdmConfigFragment())
                drawerLayout.closeDrawer(GravityCompat.START) // Cierra el cajón después de la selección.
                return true
            }
            else -> return false
        }
    }
}

