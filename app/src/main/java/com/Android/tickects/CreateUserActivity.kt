package com.Android.tickects

import android.annotation.SuppressLint

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CreateUserActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var datePickerDialog: DatePickerDialog
    private lateinit var FechaRegistro: EditText
    var hayErrores = true
    private var verificationId: String? = null


    @SuppressLint("MissingInflatedId", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)
        auth = FirebaseAuth.getInstance()
        FechaRegistro = findViewById(R.id.etFechaNac)
        FechaRegistro.setOnClickListener {
            val c: Calendar = Calendar.getInstance()
            val mYear: Int = c.get(Calendar.YEAR)
            val mMonth: Int = c.get(Calendar.MONTH)
            val mDay: Int = c.get(Calendar.DAY_OF_MONTH)
            datePickerDialog = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    FechaRegistro.setText(dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)
                }, mYear, mMonth, mDay
            )
            datePickerDialog.show()
        }
        val etpassword = findViewById<EditText>(R.id.etPasswordCreateUser)
        val cbMostrarContrasena = findViewById<CheckBox>(R.id.cbMostrarContrasena)

// Configurar el CheckBox para mostrar/ocultar la contraseña
        cbMostrarContrasena.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Mostrar la contraseña
                etpassword.transformationMethod = null
            } else {
                // Ocultar la contraseña

                etpassword.transformationMethod = PasswordTransformationMethod()
            }
        }





        val btnEnviarRegistro = findViewById<Button>(R.id.btnContinuarRegistro)
        btnEnviarRegistro.setOnClickListener {
            //if (hayErrores) {
                // hay errores, mostrar mensaje y no hacer nada
              //  Toast.makeText(
               //     this,
              //      "Por favor, ingrese información válida en los datos ingresados anteriormente",
              //      Toast.LENGTH_SHORT
              //  ).show()
          //  } else {
                enviarCodigo()
                val dialogView = layoutInflater.inflate(R.layout.validatephone_number, null)
                val codigoEditText = dialogView.findViewById<EditText>(R.id.edt_code)
                val validarButton = dialogView.findViewById<Button>(R.id.btn_validar)
                val crearUsuarioButton = dialogView.findViewById<Button>(R.id.btn_CrearUsuario)

            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Introducir Código")
                .setView(dialogView)
                .create()

            val dialog = alertDialog.show()

            dialogView.findViewById<Button>(R.id.btn_validar).setOnClickListener {
                val codigo = codigoEditText.text.toString()
                val credential = PhoneAuthProvider.getCredential(verificationId!!, codigo)
                signInWithPhoneAuthCredential(credential, validarButton, crearUsuarioButton)
                // Cerrar el diálogo después de validar el código
            }

                dialogView.findViewById<Button>(R.id.btn_CrearUsuario).setOnClickListener {
                    // Verificar si hay errores

                    val nombre = findViewById<EditText>(R.id.etNombre).text.toString().trim()
                    val apellido = findViewById<EditText>(R.id.etApellido).text.toString().trim()
                    val telefono =
                        findViewById<EditText>(R.id.etNumeroCelular).text.toString().trim()
                    val numeroCI = findViewById<EditText>(R.id.etNumeroCi).text.toString().trim()
                    val genero = findViewById<Spinner>(R.id.spGenero).selectedItem.toString()
                    val fechanac = FechaRegistro.text.toString().trim()
                    val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
                    saveUserData(nombre, apellido, telefono, genero, fechanac, email, numeroCI)
                    // Cerrar el diálogo después de crear el usuario
         //       }
            }
        }


        validarTelefono()
        validarCorreo()
        validarNombre()
        validarFechaNacimiento()
        validarNumeroCI()
        validarGenero()
        validarContrasena()
        validarApellido()
    }


    private fun enviarCodigo() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPasswordCreateUser).text.toString().trim()
        createUserWithEmailAndPassword(email, password)
        val phoneNumber = findViewById<EditText>(R.id.etNumeroCelular).text.toString().trim()
        if (phoneNumber.isNotEmpty()) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(80L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // No se requiere ninguna acción aquí si solo se desea enviar el código de verificación
                    }

                    override fun onVerificationFailed(exception: FirebaseException) {
                        // Error al enviar el código de seguridad
                        Toast.makeText(
                            this@CreateUserActivity, "Error: $exception", Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("Verification", "Verification ID: $verificationId")
                        if (verificationId != null) {
                            this@CreateUserActivity.verificationId = verificationId
                        } else {
                            Toast.makeText(
                                this@CreateUserActivity,
                                "No se pudo enviar el código de verificación",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
                .build()

            // Iniciar el proceso de verificación
            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            Toast.makeText(this, "Ingrese un número de teléfono válido", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun validarContrasena() {
        val passwordEditText = findViewById<EditText>(R.id.etPasswordCreateUser)

        if (passwordEditText.text.toString().isEmpty()) {
            passwordEditText.error = "Ingrese una contraseña"
            hayErrores = true
        }

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val passwordText = s.toString()

                // Validar que tenga más de 6 caracteres o que esté vacío
                if (s.toString().isEmpty()) {
                    passwordEditText.error = "Ingrese una contraseña"
                    hayErrores = true
                    return
                }
                // Validar que tenga más de 6 caracteres
                if (s.toString().length < 6) {
                    passwordEditText.error = "La contraseña debe tener al menos 6 caracteres"
                    hayErrores = true
                } else {
                    passwordEditText.error = null
                    hayErrores = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validarCorreo() {
        val emailEditText = findViewById<EditText>(R.id.etEmail)

        if (emailEditText.text.toString().isEmpty()) {
            emailEditText.error = "Ingrese una contraseña"
            hayErrores = true
        }

        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val correo = s.toString().trim()

                // Validar que no esté vacío
                if (correo.isEmpty()) {
                    emailEditText.error = "Ingrese un correo electrónico"
                    hayErrores = true
                    return
                }

                // Verificar en tiempo real si el correo electrónico ya existe en Firebase
                verificarCorreoEnFirebase(correo)

                // Validar el formato del correo electrónico
                if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                    emailEditText.error = "Ingrese un correo electrónico válido"
                    hayErrores = true
                    return
                }

                // Verificar si hay un punto antes del "@"
                val indexOfAt = correo.indexOf("@")
                if (indexOfAt > 0 && correo.substring(indexOfAt - 1, indexOfAt) == ".") {
                    emailEditText.error =
                        "El correo electrónico no puede contener un punto antes del @"
                    hayErrores = true
                    return
                }

                // Si llegamos aquí, no hay errores
                emailEditText.error = null
                hayErrores = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun verificarCorreoEnFirebase(correo: String) {
        val auth = FirebaseAuth.getInstance()

        auth.fetchSignInMethodsForEmail(correo)
            .addOnCompleteListener { task: Task<SignInMethodQueryResult> ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods

                    val emailEditText = findViewById<EditText>(R.id.etEmail)

                    if (signInMethods != null && signInMethods.isNotEmpty()) {
                        // El correo electrónico ya está registrado
                        emailEditText.error = "Este correo electrónico ya está registrado"
                        hayErrores = true
                    } else {
                        // El correo electrónico no está registrado
                        emailEditText.error = null
                        hayErrores = false
                    }
                }
            }
    }


    private fun validarGenero() {
        val generoSpinner = findViewById<Spinner>(R.id.spGenero)
        val generoError = findViewById<TextInputLayout>(R.id.titulo_Genero)

        // Obtener el valor seleccionado en el Spinner
        val generoSeleccionado = generoSpinner.selectedItem?.toString() ?: ""

        // Validar que se haya seleccionado un género distinto de "Selecciona una opción"
        if (generoSeleccionado == "Selecciona una opción") {
            generoError.error = "  Seleccione su género"
            hayErrores = true
        } else {
            generoError.error = null
            hayErrores = false
        }

        generoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val genero = parent?.getItemAtPosition(position)?.toString() ?: ""
                if (genero == "Selecciona una opción") {
                    generoError.error = "  Seleccione su género"
                    hayErrores = true
                } else {
                    generoError.error = null
                    hayErrores = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun validarNombre() {
        val nombreEditText = findViewById<EditText>(R.id.etNombre)

        if (nombreEditText.text.toString().isEmpty()) {
            nombreEditText.error = "Ingrese un nombre válido"
            hayErrores = true
        }

        nombreEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nombre = s.toString().trim()
                if (nombre.isEmpty()) {
                    nombreEditText.error = "El nombre no puede estar vacío"
                    hayErrores = true
                    return
                } else if (nombre.length < 5) {
                    nombreEditText.error = "El nombre debe tener al menos 5 letras"
                    hayErrores = true
                } else {
                    nombreEditText.error = null
                    hayErrores = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Validar que no esté vacío
                if (s.toString().isEmpty()) {
                    nombreEditText.error = "El nombre no puede estar vacío"
                    hayErrores = true
                    return
                }
            }
        })
    }

    private fun validarApellido() {
        val apeEditText = findViewById<EditText>(R.id.etApellido)

        if (apeEditText.text.toString().isEmpty()) {
            apeEditText.error = "Ingrese un apellido valido"
            hayErrores = true
        }
        apeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val apellido = s.toString().trim()
                if (apellido.isEmpty()) {
                    apeEditText.error = "El apellido no puede estar vacío"
                    hayErrores = true
                    return
                } else if (apellido.length < 5) {
                    apeEditText.error = "El apellido debe tener al menos 5 letras"
                    hayErrores = true
                } else {
                    apeEditText.error = null
                    hayErrores = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Validar que no esté vacío
                if (s.toString().isEmpty()) {
                    apeEditText.error = "El apellido no puede estar vacío"
                    hayErrores = true
                    return
                }
            }
        })
    }

    private fun validarTelefono() {
        val telefonoEditText = findViewById<EditText>(R.id.etNumeroCelular)
        val telefonoRegex = Regex("^\\+595\\d{9}\$")

        if (telefonoEditText.text.toString().isEmpty()) {
            telefonoEditText.error = "Ingrese un numero de telefono valido"
            hayErrores = true
        }

        telefonoEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val telefono = s.toString().trim()
                val telefonoModificado = when {
                    telefono.startsWith("0") -> "+595" + telefono.substring(1)
                    telefono.startsWith("9") -> "+595" + telefono
                    else -> telefono
                }

                // Validar que no esté vacío
                if (telefonoModificado.isEmpty()) {
                    telefonoEditText.error = "El número de teléfono no puede estar vacío"
                    hayErrores = true
                    return
                }

                // Validar el formato del número de teléfono
                if (!telefonoModificado.matches(telefonoRegex)) {
                    telefonoEditText.error = "El número de teléfono no es válido"
                    hayErrores = true
                    return
                }

                telefonoEditText.error = null
                hayErrores = false

                if (telefonoModificado != telefono) {
                    telefonoEditText.setText(telefonoModificado)
                }

                // Aquí pasamos telefonoEditText como argumento
                //verificarTelefono(telefonoModificado, telefonoEditText)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se utiliza
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Validar que no esté vacío
                if (s.toString().isEmpty()) {
                    telefonoEditText.error = "El número de teléfono no puede estar vacío"
                    hayErrores = true
                    return
                }
            }
        })
    }

    private fun verificarTelefono(telefono: String, telefonoEditText: EditText) {
        val db = FirebaseFirestore.getInstance()
        val usuariosRef = db.collection("users")

        // Escucha cambios en la base de datos Firestore en tiempo real
        usuariosRef.whereEqualTo("telefono", telefono)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    // Maneja los errores aquí, si es necesario
                    hayErrores = true
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    // El número de teléfono ya está registrado
                    telefonoEditText.error = "Este número de teléfono ya está registrado"
                    hayErrores = true
                } else {
                    // El número de teléfono no está registrado
                    telefonoEditText.error = null
                    hayErrores = false
                }
            }
    }

    private fun validarFechaNacimiento() {
        val fechaNacimientoEditText = findViewById<EditText>(R.id.etFechaNac)
        val fechaNacimientoInputLayout = findViewById<TextInputLayout>(R.id.titulo_fechaNac)

        if (fechaNacimientoEditText.text.toString().isEmpty()) {
            fechaNacimientoInputLayout.error = "  Ingrese su fecha de nacimiento"
            hayErrores = true
        }

        fechaNacimientoEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val fechaNacimiento = fechaNacimientoEditText.text.toString().trim()
                if (fechaNacimiento.isEmpty()) {
                    fechaNacimientoInputLayout.error = "  Ingrese su fecha de nacimiento"
                    hayErrores = true
                    return
                }
                val formatter = SimpleDateFormat("dd/MM/yyyy")
                val fechaNacimientoDate = try {
                    formatter.parse(fechaNacimiento)
                } catch (e: Exception) {
                    null
                }
                if (fechaNacimientoDate == null) {
                    fechaNacimientoInputLayout.error = "Formato de fecha inválido. Use dd/MM/yyyy"
                    hayErrores = true
                } else {
                    val hoy = Calendar.getInstance().time
                    val edad = calcularEdad(fechaNacimientoDate, hoy)
                    if (edad < 18) {
                        fechaNacimientoInputLayout.error = "  Debe ser mayor de edad para registrarse"
                        hayErrores = true
                    } else {
                        fechaNacimientoInputLayout.error = null
                        hayErrores = false
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    private fun calcularEdad(fechaNacimiento: Date, hoy: Date): Int {
        val diffInMillis = hoy.time - fechaNacimiento.time
        val edadInMillis = diffInMillis / 31557600000L //milisegundos en un año
        return edadInMillis.toInt()
    }


    private fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        validarButton: Button,
        crearUsuarioButton: Button
    ) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    validarButton.visibility = View.GONE // Ocultar el botón de Validar
                    crearUsuarioButton.visibility = View.VISIBLE
                    Toast.makeText(this, "Verificacion Exitosa!", Toast.LENGTH_SHORT).show()
                } else {
                    // La verificación del código SMS falló
                    Toast.makeText(
                        this, "La verificación del código SMS falló", Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    private fun createUserWithEmailAndPassword(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    sendEmailVerification(user)
                } else {
                    handleFirebaseAuthError(task.exception)
                }
            }
    }


    private fun sendEmailVerification(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener { verificationTask ->
                if (verificationTask.isSuccessful) {
                    // El correo de verificación se envió exitosamente
                    Toast.makeText(
                        this,
                        "Se envió un correo de verificación a ${user.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Error al enviar el correo de verificación",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    private fun saveUserData(
        nombre: String,
        apellido: String,
        telefono: String,
        genero: String,
        numeroCI: String,
        fechanac: String,
        email: String
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user!!.uid
        val userData = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "telefono" to telefono,
            "numeroci" to numeroCI,
            "genero" to genero,
            "fechanacimiento" to fechanac,
            "correo" to email
        )
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                // Los datos se guardaron exitosamente en Firestore
                Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Ocurrió un error al guardar los datos en Firestore
                Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_SHORT).show()
            }
    }


    private fun handleFirebaseAuthError(exception: Exception?) {
        Toast.makeText(this, "Error al crear el usuario", Toast.LENGTH_SHORT).show()

        when (exception) {
            is FirebaseAuthUserCollisionException -> {
                // El correo electrónico ya está registrado
                Toast.makeText(
                    this,
                    "Este correo electrónico ya está registrado. Por favor, utiliza otro correo electrónico",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is FirebaseAuthWeakPasswordException -> {
                // La contraseña es demasiado débil
                Toast.makeText(
                    this,
                    "La contraseña debe tener al menos 6 caracteres. Por favor, intenta con otra contraseña",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                // Otros errores
                Toast.makeText(
                    this,
                    "Ocurrió un error. Por favor, intenta más tarde",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("FirebaseAuthError", exception?.message ?: "")
            }
        }
    }

    private fun validarNumeroCI() {
        val ciEditText = findViewById<EditText>(R.id.etNumeroCi)

        if (ciEditText.text.toString().isEmpty()) {
            ciEditText.error = "Ingrese un número de cédula"
            hayErrores = true
        }

        ciEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val numeroCI = s.toString().trim()

                // Validar que no esté vacío
                if (numeroCI.isEmpty()) {
                    ciEditText.error = "Ingrese un número de cédula"
                    hayErrores = true
                    return
                } else if (numeroCI.length < 6) {
                    ciEditText.error = "El número de cédula debe tener al menos 6 caracteres"
                    hayErrores = true
                    return
                } else {
                    ciEditText.error = null
                    hayErrores = false
                    // Llamar a la función que verifica si el número de cédula ya existe
                    verificarNumeroCI(numeroCI, ciEditText)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Validar que no esté vacío antes de cambiar el texto
                if (s.toString().isEmpty()) {
                    ciEditText.error = "Ingrese un número de cédula"
                    hayErrores = true
                    return
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Este método se ejecuta cuando el texto cambia, pero no es necesario validar aquí
            }
        })
    }

    private fun verificarNumeroCI(numeroCI: String, ciEditText: EditText) {
        val db = FirebaseFirestore.getInstance()
        val usuariosRef = db.collection("users")

        // Escucha cambios en la base de datos Firestore en tiempo real
        usuariosRef.whereEqualTo("numeroci", numeroCI)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    // Maneja los errores aquí, si es necesario
                    hayErrores = true
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    // El número de cédula ya está registrado
                    ciEditText.error = "Este número de cédula ya está registrado"
                    hayErrores = true
                } else {
                    // El número de cédula no está registrado
                    ciEditText.error = null
                    hayErrores = false
                }
            }
    }
    private fun validarcodigo() {
        val codigoEditText = findViewById<EditText>(R.id.edt_code)

        if (codigoEditText.text.toString().isEmpty()) {
            codigoEditText.error = "Ingrese un nombre válido"
            hayErrores = true
        }

        codigoEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nombre = s.toString().trim()
                if (nombre.isEmpty()) {
                    codigoEditText.error = "El nombre no puede estar vacío"
                    hayErrores = true
                    return
                } else if (nombre.length < 5) {
                    codigoEditText.error = "El nombre debe tener al menos 5 letras"
                    hayErrores = true
                } else {
                    codigoEditText.error = null
                    hayErrores = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Validar que no esté vacío
                if (s.toString().isEmpty()) {
                    codigoEditText.error = ""
                    hayErrores = true
                    return
                }
            }
        })
    }
}



