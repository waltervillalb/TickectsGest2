package com.Android.tickects

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Android.tickects.Fragments.userId

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupLoadingDialog()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        val txtEmail: TextView = findViewById(R.id.text_email)
        val txtPssw: TextView = findViewById(R.id.text_password)
        firebaseAuth = Firebase.auth

        //validar Boton de Olvidaste tu contraseña
        val recupBTN = findViewById<TextView>(R.id.tv_recup_password)
        recupBTN.setOnClickListener {
            val lanzar2 = Intent(this, ContraRecuperar::class.java)
            startActivity(lanzar2)
        }
        //validar Boton de registrarse


        val btnLogin: Button = findViewById(R.id.btn_login)
        btnLogin.setOnClickListener {
            val email = txtEmail.text.toString().trim()
            val password = txtPssw.text.toString().trim()

            // Validar campos vacíos
            if (email.isNullOrEmpty()) {
                txtEmail.error = "Correo electrónico requerido"
                return@setOnClickListener
            }

            if (password.isNullOrEmpty()) {
                txtPssw.error = "Contraseña requerida"
                return@setOnClickListener
            }

            // Validar formato de correo electrónico
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                txtEmail.error = "Formato de correo electrónico inválido"
                return@setOnClickListener
            }

            signIn(txtEmail.text.toString(), txtPssw.text.toString())
        }

    }
    //traer datos de la autenticacion de firebase
    private fun signIn(email: String, password: String) {
        showLoadingDialog()
        val databaseReference = FirebaseDatabase.getInstance().getReference("sesion")

        firebaseAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener { fetchTask ->
            if (fetchTask.isSuccessful) {
                val signInMethods = fetchTask.result?.signInMethods ?: emptyList()
                if (signInMethods.isNotEmpty()) {
                    // El usuario ya está registrado, verificar el estado de la sesión
                    firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            val id = user?.uid ?: ""
                            userId.iduser=id
                            if (id.isNotEmpty()) {
                                databaseReference.child(id).get().addOnSuccessListener { dataSnapshot ->
                                    val isActiveSession = dataSnapshot.getValue(Boolean::class.java) ?: false
                                    if (!isActiveSession) {
                                        // No hay sesión activa, proceder a marcar la sesión como activa
                                        databaseReference.child(id).setValue(true).addOnCompleteListener { sessionTask ->
                                            hideLoadingDialog()
                                            if (sessionTask.isSuccessful) {
                                                Toast.makeText(baseContext, "Operación exitosa", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, HomeActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(baseContext, "Error al actualizar el estado de la sesión", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        // Ya existe una sesión activa
                                        hideLoadingDialog()
                                        Toast.makeText(baseContext, "Ya hay una sesión activa en otro dispositivo", Toast.LENGTH_SHORT).show()
                                    }
                                }.addOnFailureListener {
                                    hideLoadingDialog()
                                    Toast.makeText(baseContext, "Error al verificar el estado de la sesión", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                hideLoadingDialog()
                                Toast.makeText(baseContext, "Error obteniendo información del usuario", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            hideLoadingDialog()
                            Toast.makeText(baseContext, "Error en los datos ingresados", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // El usuario no está registrado, manejar según corresponda
                    hideLoadingDialog()
                    Toast.makeText(baseContext, "No existe una cuenta con este correo electrónico", Toast.LENGTH_SHORT).show()
                }
            } else {
                hideLoadingDialog()
                Toast.makeText(baseContext, "Error al verificar el correo electrónico", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupLoadingDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomProgressDialog)
        val progressBar = ProgressBar(this)
        builder.setView(progressBar)
        builder.setCancelable(false) // Opcional: hace que el diálogo no se pueda cancelar

        loadingDialog = builder.create()
    }

    private fun showLoadingDialog() {
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }
}


