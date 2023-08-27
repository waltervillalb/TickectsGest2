package com.Android.tickects

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory


class MainActivity : AppCompatActivity() {


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        // Obtener la instancia de FirebaseAppCheck
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Instalar el proveedor de Play Integrity
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
        val iniciosesionBTN = findViewById<Button>(R.id.IniciarSesión)

        iniciosesionBTN.setOnClickListener {
            val lanzar = Intent(this, LoginActivity::class.java)
            startActivity(lanzar)
        }

        val createUserBTN = findViewById<Button>(R.id.CrearUsuario)
        createUserBTN.setOnClickListener {
            val lanzar2 = Intent(this, CreateUserActivity::class.java)
            startActivity(lanzar2)
        }


        }
    }



