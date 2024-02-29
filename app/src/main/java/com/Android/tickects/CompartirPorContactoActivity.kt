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
import com.google.firebase.firestore.FirebaseFirestore

class CompartirPorContactoActivity : AppCompatActivity() {

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
            // Este código se ejecuta después de obtener los contactos registrados de Firestore
            val contactosParaMostrar = filtrarContactosRegistrados(todosLosContactos, contactosRegistrados)
            runOnUiThread {
                // Asegúrate de actualizar la UI en el hilo principal
                val adapter = ContactsAdapter(contactosParaMostrar) { contacto ->
                    // Manejo del clic en el contacto aquí
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
}