package com.Android.tickects

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ContraRecuperar : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contra_recuperar)
        val enviarBtn = findViewById<Button>(R.id.btn_recuContra)
        val recuEmailText = findViewById<EditText>(R.id.recu_emailText)

        enviarBtn.setOnClickListener {
            val emailAddress = recuEmailText.text.toString().trim()
            if (emailAddress.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor, ingrese su dirección de correo electrónico",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(emailAddress)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Se ha enviado un correo electrónico de restablecimiento de contraseña a $emailAddress",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "No se pudo enviar el correo electrónico de restablecimiento de contraseña",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

    }
}