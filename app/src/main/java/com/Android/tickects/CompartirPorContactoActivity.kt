package com.Android.tickects

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.eventoAdapter.Contact
import com.Android.tickects.eventoAdapter.ContactsAdapter
import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CompartirPorContactoActivity : AppCompatActivity() {
    private lateinit var entradaId: String
    private lateinit var recyclerView: RecyclerView
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadContacts()
            } else {
                // Manejo de caso donde el permiso no fue concedido
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compartir_por_contacto) // Corregido aquí

        entradaId = intent.getStringExtra("EXTRA_ENTRADA_ID") ?: ""
        recyclerView = findViewById(R.id.recyclerViewContactos) // Asegúrate de tener un RecyclerView en tu layout

        checkPermissionAndLoadContacts()
    }

    private fun checkPermissionAndLoadContacts() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadContacts()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun loadContacts() {
        val todosLosContactos = obtenerTodosLosContactosDelDispositivo()
        obtenerContactosRegistradosEnLaApp { contactosRegistrados ->
            val contactosParaMostrar = filtrarContactosRegistrados(todosLosContactos, contactosRegistrados)
            runOnUiThread {
                val adapter = ContactsAdapter(contactosParaMostrar) { contactoSeleccionado ->
                    AlertDialog.Builder(this)
                        .setTitle("Confirmar Transferencia")
                        .setMessage("¿Deseas transferir la entrada a ${contactoSeleccionado.name}?")
                        .setPositiveButton("Sí") { _, _ ->
                            // Asegúrate de pasar el número de teléfono correctamente
                            buscarUsuarioYCompartirEntrada(contactoSeleccionado.phoneNumber, entradaId)
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
                recyclerView.adapter = adapter
            }
        }
    }

    private fun obtenerTodosLosContactosDelDispositivo(): List<Contact> {
        val listaDeContactos = mutableListOf<Contact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                listaDeContactos.add(Contact("", name, number)) // ID no es relevante para este ejemplo
            }
        }

        return listaDeContactos
    }

    private fun obtenerContactosRegistradosEnLaApp(onResult: (List<Contact>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val contactosRegistrados = result.documents.mapNotNull { document ->
                    val phoneNumber = document.getString("telefono") // Asegúrate de que el campo se llame 'telefono' en Firestore
                    if (phoneNumber != null) {
                        Contact(document.id, document.getString("name") ?: "Nombre no disponible", phoneNumber)
                    } else {
                        null
                    }
                }
                onResult(contactosRegistrados)
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "Error al obtener documentos: ", exception)
                onResult(listOf()) // Llama al callback con una lista vacía en caso de error
            }
    }

    private fun filtrarContactosRegistrados(todosLosContactos: List<Contact>, contactosRegistrados: List<Contact>): List<Contact> {
        return todosLosContactos.filter { contactoDelDispositivo ->
            contactosRegistrados.any { it.phoneNumber == contactoDelDispositivo.phoneNumber }
        }
    }


    private fun buscarUsuarioYCompartirEntrada(phoneNumber: String, entradaId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("telefono", phoneNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) { // Esta es la forma correcta
                    val userId = documents.documents[0].id
                    compartirEntrada(userId, entradaId)
                } else {
                    Toast.makeText(this, "El contacto no está registrado en la app.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar el usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun compartirEntrada(idUsuarioDestinatario: String, entradaId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null) {
            // Registrar la acción de compartir en el historial del usuario remitente
            agregarAHistorialTransacciones(userId, entradaId, "compartido")
            val analytics = FirebaseAnalytics.getInstance(this)
            val bundle = Bundle()
            bundle.putString("entrada_id", entradaId)
            analytics.logEvent("entrada_compartida", bundle)
            // Eliminar la entrada y el token del usuario remitente en Realtime Database
            eliminarEntradaYTokenDeRealtimeDatabase(userId, entradaId)

            // Eliminar la entrada del usuario remitente en Firestore
            eliminarEntradaDeFirestore(userId, entradaId)

            // Agregar la entrada al usuario destinatario en Firestore
            agregarEntradaAUsuarioDestinatario(idUsuarioDestinatario, entradaId)
            // Cierra la actividad después de realizar la transferencia
            finish()
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