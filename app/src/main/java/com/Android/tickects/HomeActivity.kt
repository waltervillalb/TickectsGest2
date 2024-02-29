package com.Android.tickects

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.Android.tickects.Fragments.*
import com.Android.tickects.Fragments.userId.Companion.iduser
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {
    private lateinit var fab: FloatingActionButton
    private lateinit var frameLayout: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    @SuppressLint("MissingInflatedId", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

/*
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

 */

        val analytics: FirebaseAnalytics
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fab = findViewById(R.id.fab)
        frameLayout = findViewById(R.id.frame_layout)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val btnCopyInfo = headerView.findViewById<ImageView>(R.id.btnCopyInfo)
        btnCopyInfo.setOnClickListener {
            copyUserInfoToClipboard()
        }

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.abrir_drawer, R.string.cerrar_drawer)
        drawerLayout.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView.setNavigationItemSelectedListener (this)


        bottomNavigationView.background = null
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.shorts -> replaceFragment(EntradasFragment())
                R.id.Historial -> replaceFragment(HistorialFragment())
                R.id.library -> replaceFragment(ContactosFragment())

            }
            true
        }

        replaceFragment(HomeFragment())


        val db = FirebaseFirestore.getInstance()
        val menu = navigationView.menu
        val itemConfigEntradas = menu.findItem(R.id.CONFentradas)
        //verifica el rol para ocultar el item de configuracion entradas
        val id = userId.iduser
        db.collection("users").document(id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val rol = documentSnapshot.getString("rol")
                    if (rol == "Administrador") {
                        // Si el usuario es administrador, muestra el ítem
                        itemConfigEntradas.isVisible = true
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener datos del usuario: $exception")
            }

        //oculta o muestra el boton mas de acuerdo al rol del usuario
        val uid = userId.iduser

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val rol = documentSnapshot.getString("rol")

                    // Verifica si el rol es igual a "Administrador"
                    if (rol == "Administrador") {
                        // Si el rol es Administrador, muestra el FAB
                        fab.visibility = View.VISIBLE
                        fab.setOnClickListener {
                            val intent = Intent(this, Escaneo_de_qr::class.java)
                            startActivity(intent)
                        }
                    } else {
                        // Si el rol no es Administrador, oculta el boton +
                        fab.visibility = View.GONE
                    }
                } else {
                    // El documento no existe
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener datos del usuario: $exception")
            }

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
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener datos del usuario: $exception")
            }
    }
    private fun copyUserInfoToClipboard() {
        val nombre = findViewById<TextView>(R.id.nombre_Cabecera).text.toString()
        val correo = findViewById<TextView>(R.id.correo_cabecera).text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "ID no disponible"

        val infoToCopy = "Mi nombre es: $nombre, mi correo es: $correo y mi ID para compartir entradas es: $userId"

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UserInfo", infoToCopy)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "Información copiada al portapapeles", Toast.LENGTH_SHORT).show()
    }
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    private fun showBottomDialog() {
    }

    private fun logoutUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("sesion")

            // Actualiza el estado de la sesión en Firebase Realtime Database a false
            databaseReference.child(userId).setValue(false)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Una vez actualizado el estado de la sesión, cierra la sesión en Firebase Auth
                        FirebaseAuth.getInstance().signOut()

                        // Redirige al usuario a la pantalla de inicio de sesión
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Esto cierra la actividad actual
                    } else {
                        // Manejo de errores, si falla la actualización en la base de datos
                        Toast.makeText(
                            baseContext,
                            "Error al actualizar el estado de la sesión.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
    override fun onBackPressed() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage("¿Desea salir de la aplicación?")
        alertDialogBuilder.setPositiveButton("Sí") { _, _ ->
             logoutUser()
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

