package com.Android.tickects

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Android.tickects.Fragments.userId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var loadingDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupLoadingDialog()

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
    private fun signIn(email: String, password: String){
        showLoadingDialog()
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener (this){
             task ->
            hideLoadingDialog()
            if(task.isSuccessful) {
                val user = firebaseAuth.currentUser
                val id= user?.uid ?: ""
                userId.iduser=id
                Toast.makeText(baseContext, "operación exitosa", Toast.LENGTH_SHORT).show()
                //aqui vamos a ir a la pantalla Home
                val i = Intent(this, HomeActivity::class.java)
                startActivity(i)
                finish()
            } else{
                    Toast.makeText(baseContext,"Error en los datos en los datos ingresados", Toast.LENGTH_SHORT).show()
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


