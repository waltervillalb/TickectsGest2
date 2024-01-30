package com.Android.tickects.Fragments

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
        etCodigoVerificacion = dialogView.findViewById(R.id.edt_code)
        etCodigoVerificacion.inputType = InputType.TYPE_CLASS_NUMBER

        // Agregar TextWatcher para validar en tiempo real
        etCodigoVerificacion.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length != 6) {
                    // Mostrar error si el código no tiene exactamente 6 dígitos
                    etCodigoVerificacion.error = "El código debe tener 6 dígitos"
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Aquí puedes implementar alguna lógica antes de que el texto cambie
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Aquí puedes implementar alguna lógica mientras el texto está cambiando
            }
        })
// Asignar el listener de clics al botón de validación
        val btnValidate = dialogView.findViewById<Button>(R.id.btn_validar)
        btnValidate.setOnClickListener {

        }
    }

    private fun validarCodigo() {
        val codigo = etCodigoVerificacion.text.toString()
        if (codigo.length == 6) {
            // Lógica para validar el código
        } else {
            etCodigoVerificacion.error = "El código debe tener 6 dígitos"
        }
    }
}