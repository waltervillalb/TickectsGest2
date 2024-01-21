package com.Android.tickects

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.os.CountDownTimer
import android.text.Html
import android.text.InputType
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

class qr_generator : AppCompatActivity() {
    private lateinit var imgQRCode: ImageView
    private lateinit var database: FirebaseDatabase
    private lateinit var userTokenRef: DatabaseReference
    private val handler = Handler()  // el Handler se utiliza para programar actualizaciones en un periodo de tiempo deseado

    private var countdownTimer: CountDownTimer? = null
    private lateinit var tv_cronometro: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvQrId: TextView
    private lateinit var entradaId: String

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_generator)

        //concatena los datos del usuario y de la entrada
        entradaId = intent.getStringExtra("EXTRA_ENTRADA_ID") ?: ""

        // Verifica si entradaId está vacío y maneja ese caso
        if (entradaId.isEmpty()) {
            Toast.makeText(this, "ID de entrada no proporcionado", Toast.LENGTH_SHORT).show()
            finish() // Finaliza la actividad si no hay ID de entrada
            return
        }

        // Ahora que entradaId está inicializado, llama a cargarDatosUsuarioYEvento
        cargarDatosUsuarioYEvento(entradaId)

        // condicion para pedir Permisos para acceder a los contenidos del telefono movil
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
        }

        val btnCompartir = findViewById<Button>(R.id.btnCompartir)
        btnCompartir.setOnClickListener {
            mostrarPopupCompartir()
        }
        //obtener el ID del usuario actual
        val user = FirebaseAuth.getInstance().currentUser
        val user_id = user?.uid

        //se accede a la ubicación correcta de la estructura en la BD
        val database = FirebaseDatabase.getInstance()
        val userTokenRef = database.getReference("users/$user_id/$entradaId/tokens")


        //declaracion para la imagen del QR
        imgQRCode = findViewById(R.id.img_QRCode)
        this.progressBar = findViewById<ProgressBar>(R.id.progressBar_time)
        progressBar.max = 2000


        // Inicialización de SharedPreferences
        //en donde recuperamos el token de SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        // trae el id del token
        val idQr: String? = sharedPreferences.getString("tokens", null)

        // Mostrar el ID (token) del QR generado en el TextView
        tvQrId = findViewById<TextView>(R.id.tv_id_qr)


        // Recupera información desde el fragmento fragment_entradas
        val extras = intent.extras
        if (extras != null) {
            // Genera y actualiza un nuevo token
            val nuevoToken = generarNuevoToken()
            actualizarTokenEnFirebase(nuevoToken)
            // Iniciar actualizaciones programadas automáticamente, indicando el progreso del ProgressBar
            //iniciarActualizacionesProgramadas(progressBar, tvQrId)
        }

        //  Escuchar cambios del token
        userTokenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) { //  El token se almacena en Firebase y se recupera mediante el evento onDataChange
                val tokens = dataSnapshot.getValue(String::class.java)
                if (!TextUtils.isEmpty(tokens)) {
                    // Actualiza el código QR con el nuevo token
                    if (tokens != null) {
                        generarQRCode(tokens)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejo de errores
            }
        })

        // Encuentra la TextView del contador
        tv_cronometro = findViewById(R.id.tv_cronometro)

        // Inicia el cronómetro automáticamente al crear la actividad
        iniciarCronometro()


    }
    override fun onResume() {
        super.onResume()
        val window = window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f // 1.0f es 100% de brillo
        window.attributes = layoutParams
    }

    override fun onPause() {
        super.onPause()
        val window = window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }
    private fun iniciarCronometro() {
        countdownTimer?.cancel() // Detén el contador si está en marcha

        val tiempoTotal = 50000 // Duración total en milisegundos (ejemplo: 15 segundos)
        progressBar.max = tiempoTotal / 1000 // Configura el máximo del ProgressBar

        // Crea un nuevo cronometro de 15 segundos con actualizaciones cada segundo
        countdownTimer = object : CountDownTimer(tiempoTotal.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000 // Calcula los segundos restantes
                tv_cronometro.text = secondsRemaining.toString() // Muestra los segundos restantes
                progressBar.progress = secondsRemaining.toInt() // Actualiza el progreso del ProgressBar
            }

            override fun onFinish() {
                // Cuando llega a 0 se reinicia el cronómetro a 15 segundos automáticamente
                val nuevoToken = generarNuevoToken()
                tvQrId.text = nuevoToken // Actualiza el token en el TextView
                actualizarTokenEnFirebase(nuevoToken) // Actualiza el token en Firebase
                iniciarCronometro() // Reinicia el cronómetro
            }
        }
        countdownTimer?.start() // Inicia el contador
    }

    //Generar nuevo Token
    private fun generarNuevoToken(): String {
        // Se implementa la lógica de Utilizar un UUID (Identificador Único Universal) generado aleatoriamente.
        // apartir de este ID generado automaticamente se generará el QR.
        //val nuevo_token_generado = UUID.randomUUID().toString()
        //Log.d("TOKEN_GENERADO", nuevo_token_generado)
        return UUID.randomUUID().toString()
    }




    //Actualizar el Token en Firebase

    private fun actualizarTokenEnFirebase(nuevoToken: String) {
        // Actualiza el token utilizando updateChildren. El updateChildren actualiza los token en una ruta específica.
        // es decir; se ocupa de actualizar el valor en la ubicación definida por tokenPath.
        val updates = HashMap<String, Any>()
        val user = FirebaseAuth.getInstance().currentUser
        val user_id = user?.uid

        // Construye la ruta en la base de datos
        val tokenPath = "users/$user_id/$entradaId/token"
        updates[tokenPath] = nuevoToken

        // Obtiene la referencia a la base de datos
        val database = FirebaseDatabase.getInstance()
        val userTokenRef = database.getReference("/")

        // Ejecuta la actualización
        userTokenRef.updateChildren(updates)
            .addOnSuccessListener {
                generarQRCode(nuevoToken)  // Generar el QR al actualizar el token
            }
            .addOnFailureListener { error ->
                // Manejo de errores
            }

    }

    //Generar codigo QR de acuerdo al token generado.
    private fun generarQRCode(token: String) {
        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(token, BarcodeFormat.QR_CODE, 300, 300)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

            imgQRCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar el QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatosUsuarioYEvento(entradaId: String) {
        // Suponiendo que tienes el ID del usuario y el ID del evento
        val user = FirebaseAuth.getInstance().currentUser
        val userID = user?.uid ?: return // Retorna si el usuario no está autenticado

        val tvNombre: TextView = findViewById(R.id.tvNombre)
        val tvApellido: TextView = findViewById(R.id.tvApellido)
        val tvCi: TextView = findViewById(R.id.tvCi)
        val tvNombreEntrada: TextView = findViewById(R.id.tvNombreEntrada)
        val tvFecha: TextView = findViewById(R.id.tvFecha)
        val tvHora: TextView = findViewById(R.id.tvFecha)
        val tvUbicacion: TextView = findViewById(R.id.tvUbicacion)

        // Obtener datos del usuario
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userID)
        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val nombre = document.getString("nombre") ?: ""
                val apellido = document.getString("apellido") ?: ""
                val ci = document.getString("numeroci") ?: ""
                val genero = document.getString("genero") ?: ""
                val numero = document.getString("telefono") ?: ""

                tvNombre.text = "Nombre: $nombre"
                tvApellido.text = "Apellido: $apellido"
                tvCi.text = "Número de CI: $ci"
            }
        }

        // Obtener datos del evento
        val eventoRef = FirebaseFirestore.getInstance().collection("entradas").document(entradaId)
        eventoRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val nombreEvento = document.getString("nombreEvento") ?: ""
                val fecha = document.getString("fecha") ?: ""
                val hora = document.getString("hora") ?: ""
                val ubicacion = document.getString("ubicacion") ?: ""

                tvNombreEntrada.text = "Evento: $nombreEvento"
                tvFecha.text = "fecha: $fecha"
                tvHora.text = "fecha: $hora"
                tvUbicacion.text = "Ubicación: $ubicacion"
            }
        }
    }

    private fun mostrarPopupCompartir() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Compartir Entrada")

        // Configurar el input para el ID del usuario
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        // Configurar botones OK y Cancelar
        builder.setPositiveButton("OK") { dialog, _ ->
            val idUsuarioACompartir = input.text.toString()
            confirmarCompartir(idUsuarioACompartir)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
    private fun confirmarCompartir(idUsuarioACompartir: String) {
        FirebaseFirestore.getInstance().collection("users").document(idUsuarioACompartir).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    val apellido = document.getString("apellido") ?: ""
                    val mensajeConfirmacion = "¿Estás seguro de que quieres compartir con <b>$nombre</b> $apellido?"

                    AlertDialog.Builder(this)
                        .setTitle("Confirmar Compartir")
                        .setMessage(Html.fromHtml(mensajeConfirmacion, Html.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton("Sí") { _, _ ->
                            compartirEntrada(idUsuarioACompartir, entradaId)
                        }
                        .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar el usuario", Toast.LENGTH_SHORT).show()
            }
    }
    private fun compartirEntrada(idUsuarioDestinatario: String, entradaId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null) {
            // Registrar la acción de compartir en el historial del usuario remitente
            agregarAHistorialTransacciones(userId, entradaId, "compartido")

            // Eliminar la entrada y el token del usuario remitente en Realtime Database
            eliminarEntradaYTokenDeRealtimeDatabase(userId, entradaId)

            // Eliminar la entrada del usuario remitente en Firestore
            eliminarEntradaDeFirestore(userId, entradaId)

            // Agregar la entrada al usuario destinatario en Firestore
            agregarEntradaAUsuarioDestinatario(idUsuarioDestinatario, entradaId)

            finish()
            onBackPressed()
        } else {
            Toast.makeText(this, "Error al obtener el ID del usuario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun agregarAHistorialTransacciones(userId: String, entradaId: String, accion: String) {
        val dbFirestore = FirebaseFirestore.getInstance()
        val transaccion = hashMapOf(
            "entradaID" to entradaId,
            "accion" to accion,
            "fecha" to FieldValue.serverTimestamp()
        )
        dbFirestore.collection("historialTransacciones").document(userId)
            .collection("transacciones").add(transaccion)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(applicationContext, "Transacción agregada al historial", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al agregar a historialTransacciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun eliminarEntradaYTokenDeRealtimeDatabase(userId: String, entradaId: String) {
        val dbRealtime = FirebaseDatabase.getInstance()
        val entradaRef = dbRealtime.getReference("users/$userId/$entradaId")

        entradaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Eliminar el token y el ID de la entrada del usuario remitente en Realtime Database
                entradaRef.removeValue().addOnSuccessListener { Toast.makeText(applicationContext, "Entrada y token eliminados de Realtime Database", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(applicationContext, "Error al eliminar entrada y token: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar errores aquí
            }
        })
    }
    private fun eliminarEntradaDeFirestore(userId: String, entradaId: String) {
        val dbFirestore = FirebaseFirestore.getInstance()
        val entradaRef = dbFirestore.collection("users").document(userId)

        // Suponiendo que 'entradasAdquiridas' es un array en Firestore
        entradaRef.update("entradasAdquiridas", FieldValue.arrayRemove(entradaId))
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Entrada eliminada de Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al eliminar la entrada de Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun agregarEntradaAUsuarioDestinatario(idUsuarioDestinatario: String, entradaId: String) {
        val dbFirestore = FirebaseFirestore.getInstance()
        val entradasAdquiridasRef = dbFirestore.collection("users").document(idUsuarioDestinatario)

        // Asumo que 'entradasAdquiridas' es un array en Firestore
        entradasAdquiridasRef.update("entradasAdquiridas", FieldValue.arrayUnion(entradaId))
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Entrada agregada a las adquiridas del destinatario", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al agregar la entrada al destinatario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
