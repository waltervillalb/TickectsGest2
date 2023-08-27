package com.Android.tickects.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import com.Android.tickects.R
import com.google.firebase.auth.FirebaseAuth

class PhoneVerificationDialog(context: Context, telefono: String) : Dialog(context) {

    val auth = FirebaseAuth.getInstance()
    lateinit var etCodigoVerificacion: EditText

    init {
        // Obtener la referencia al objeto LayoutInflater
        val inflater = LayoutInflater.from(context)

        // Inflar el layout dentro del diálogo
        val dialogView = inflater.inflate(R.layout.validatephone_number, null)

        // Establecer la vista del diálogo en el layout inflado
        setContentView(dialogView)

// Asignar el listener de clics al botón de validación
        val btnValidate = dialogView.findViewById<Button>(R.id.btn_validar)
        btnValidate.setOnClickListener {
        }
    }

}