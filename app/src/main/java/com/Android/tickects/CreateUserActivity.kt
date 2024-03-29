package com.Android.tickects

import android.annotation.SuppressLint

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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
import com.google.firebase.auth.FirebaseAuth
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
    private var uidUsuario: String? = null


    @SuppressLint("MissingInflatedId", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        hayErrores = true
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
            if (existeAlgunError()) {
                Toast.makeText(this, "Por favor, ingrese información válida en los datos ingresados anteriormente", Toast.LENGTH_SHORT).show()
            } else {
                mostrarPopUpVerificacionTelefono()
            }
        }
        //validarTelefono()
        validarCorreo()
        validarNombre()
        validarFechaNacimiento()
        validarNumeroCI()
        validarGenero()
        validarContrasena()
        validarApellido()
    }
    private fun mostrarPopUpVerificacionEmail() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verificar Correo Electrónico")
        builder.setMessage("Por favor, verifica tu correo electrónico para continuar.")
        builder.setCancelable(false)

        // Agregar un botón para reintentar la verificación
        builder.setPositiveButton("Verificar") { _, _ ->
            refrescarEstadoUsuarioYVerificar()
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun refrescarEstadoUsuarioYVerificar() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    crearUsuarioEnFirestore()
                    finish() // Cierra la actividad actual
                    val intentLogin = Intent(this, LoginActivity::class.java)
                    startActivity(intentLogin) // Abre la actividad de login
                } else {
                    Toast.makeText(this, "Por favor, verifica nuevamente tu correo electrónico", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error al actualizar el estado del usuario.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun mostrarPopUpVerificacionTelefono() {
        enviarCodigo()
        val dialogView = layoutInflater.inflate(R.layout.validatephone_number, null)
        val codigoEditText = dialogView.findViewById<EditText>(R.id.edt_code)

        // Agrega un TextWatcher para validar en tiempo real
        codigoEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().length != 6) {
                    codigoEditText.error = "El código debe tener 6 dígitos"
                } else {
                    codigoEditText.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Introducir Código")
            .setView(dialogView)
            .create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        val btnValidate = dialogView.findViewById<Button>(R.id.btn_validar)
        btnValidate.setOnClickListener {
            val codigo = codigoEditText.text.toString()
            if (codigo.isEmpty()) { // Verifica si el campo está vacío
                Toast.makeText(this, "Por favor, introduce un código", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sal de la función si el campo está vacío
            }
            if (codigo.length == 6) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, codigo)
                signInWithPhoneAuthCredential(credential)
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun enviarCodigo() {
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

            // Inicia el proceso de verificación
            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            Toast.makeText(this, "Ingrese un número de teléfono válido", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // La validación del código SMS fue exitosa, proceder a crear el usuario en Firestore
                    val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
                    val password = findViewById<EditText>(R.id.etPasswordCreateUser).text.toString().trim()
                    createUserWithEmailAndPassword(email, password)
                } else {
                    // La verificación del código SMS falló
                    Toast.makeText(this, "La verificación del código SMS falló", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun crearUsuarioEnFirestore() {
        val user = FirebaseAuth.getInstance().currentUser
        val nombre = findViewById<EditText>(R.id.etNombre).text.toString().trim()
        val apellido = findViewById<EditText>(R.id.etApellido).text.toString().trim()
        val telefono = findViewById<EditText>(R.id.etNumeroCelular).text.toString().trim()
        val numeroCI = findViewById<EditText>(R.id.etNumeroCi).text.toString().trim()
        val genero = findViewById<Spinner>(R.id.spGenero).selectedItem.toString()
        val fechanac = FechaRegistro.text.toString().trim()
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()

        // Almacena el UID del usuario actual en la variable global
        uidUsuario = user?.uid
        uidUsuario?.let {
            saveUserData(nombre, apellido, telefono, numeroCI, genero, fechanac, email, it)
        } ?: run {
            Toast.makeText(this, "Error: No se pudo obtener el UID del usuario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserData(nombre: String, apellido: String, telefono: String, numeroCI: String, genero: String, fechanac: String, email: String, uid: String) {
        val userData = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "telefono" to telefono,
            "numeroci" to numeroCI,
            "genero" to genero,
            "fechanacimiento" to fechanac,
            "correo" to email,
            "entradasAdquiridas" to " ",
            "rol" to "Usuario"
        )
        FirebaseFirestore.getInstance().collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registro Exitoso de usuario", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_SHORT).show()
            }
    }
    private fun createUserWithEmailAndPassword(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // No es necesario almacenar el UID en una variable global, puedes obtenerlo directamente
                    val user = FirebaseAuth.getInstance().currentUser
                    uidUsuario = user?.uid
                    if (user != null) {
                        // Usuario creado exitosamente
                        sendEmailVerification(user)
                    } else {
                        Toast.makeText(this, "Error: El usuario o el UID son nulos", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleFirebaseAuthError(task.exception)
                }
            }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification().addOnCompleteListener { verificationTask ->
            if (verificationTask.isSuccessful) {
                Toast.makeText(this, "Se envió un correo de verificación a ${user.email}", Toast.LENGTH_SHORT).show()
                // Aquí puedes mostrar el pop-up para verificar el correo electrónico
                mostrarPopUpVerificacionEmail()
            } else {
                Toast.makeText(this, "Error al enviar el correo de verificación", Toast.LENGTH_SHORT).show()
            }
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


    //verificacion de errores en cada campo
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
            emailEditText.error = "Ingrese un correo"
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
        if (generoSeleccionado == "Seleccione su género") {
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
                if (genero == "Seleccione su género") {
                    generoError.error = "Seleccione su género"
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
                } else if (nombre.length < 3) {
                    nombreEditText.error = "El nombre debe tener al menos 3 letras"
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
                } else if (apellido.length < 3) {
                    apeEditText.error = "El apellido debe tener al menos 3 letras"
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
                verificarTelefono(telefonoModificado, telefonoEditText)
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
                    } else if (edad > 70) {
                        fechaNacimientoInputLayout.error =
                            "  Debe ser menor de 70 años para registrarte"
                        hayErrores = true
                    }else{
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
                } else if (numeroCI.length !in 6..7) {
                    ciEditText.error = "El número de cédula debe tener 6 o 7 caracteres"
                    hayErrores = true
                    return
                } else {
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
    private fun existeAlgunError(): Boolean {
        // Verificar si el campo de contraseña tiene un error
        var hayErrores = false

        // Verificar si el campo de contraseña tiene un error
        val passwordEditText = findViewById<EditText>(R.id.etPasswordCreateUser)
        if (passwordEditText.error != null) {
            hayErrores = true
        }

        // Verificar si el campo de email tiene un error
        val emailEditText = findViewById<EditText>(R.id.etEmail)
        if (emailEditText.error != null) {
            hayErrores = true
        }

        // Verificar si el campo de nombre tiene un error
        val nombreEditText = findViewById<EditText>(R.id.etNombre)
        if (nombreEditText.error != null) {
            hayErrores = true
        }

        // Verificar si el campo de apellido tiene un error
        val apellidoEditText = findViewById<EditText>(R.id.etApellido)
        if (apellidoEditText.error != null) {
            hayErrores = true
        }

        // Verificar si el campo de teléfono tiene un error
        val telefonoEditText = findViewById<EditText>(R.id.etNumeroCelular)
        if (telefonoEditText.error != null) {
            hayErrores = true
        }

        // Verificar si el campo de número de CI tiene un error
        val numeroCIEditText = findViewById<EditText>(R.id.etNumeroCi)
        if (numeroCIEditText.error != null) {
            hayErrores = true
        }
        if (validarFechaNacimient()) {
            hayErrores = true
        }
        // Verificar si el campo de fecha de nacimiento tiene un error
        val fechaNacEditText = findViewById<EditText>(R.id.etFechaNac)
        if (fechaNacEditText.error != null) {
            hayErrores = true
        }

        // Verificar si se ha seleccionado un género
        val generoSpinner = findViewById<Spinner>(R.id.spGenero)
        val generoSeleccionado = generoSpinner.selectedItem?.toString() ?: ""
        if (generoSeleccionado == "Seleccione su género") {
            val generoError = findViewById<TextInputLayout>(R.id.titulo_Genero)
            generoError.error = "Seleccione su género"
            hayErrores = true
        }

        return hayErrores
    }

    private fun validarFechaNacimient(): Boolean {
        val fechaNacEditText = findViewById<EditText>(R.id.etFechaNac)
        val fechaNacimiento = fechaNacEditText.text.toString().trim()

        if (fechaNacimiento.isEmpty()) {
            fechaNacEditText.error = "Ingrese su fecha de nacimiento"
            return true
        }

        val formatter = SimpleDateFormat("dd/MM/yyyy")
        val fechaNacimientoDate = try {
            formatter.parse(fechaNacimiento)
        } catch (e: Exception) {
            null
        }

        if (fechaNacimientoDate == null) {
            fechaNacEditText.error = "Formato de fecha inválido. Use dd/MM/yyyy"
            return true
        }

        val hoy = Calendar.getInstance().time
        val edad = calcularEdad(fechaNacimientoDate, hoy)
        if (edad < 18) {
            fechaNacEditText.error = "Debe ser mayor de edad para registrarse"
            return true
        } else if (edad > 70) {
            fechaNacEditText.error = "Debe ser menor de 70 años para registrarte"
            return true
        }

        // Si llega hasta aquí, la fecha de nacimiento es válida
        return false
    }
}



