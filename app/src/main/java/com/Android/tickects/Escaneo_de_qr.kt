package com.Android.tickects

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class Escaneo_de_qr : AppCompatActivity() {
    private val REQUEST_CAMERA_PERMISSION = 1001
    private lateinit var resultEscaneo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo_de_qr)
        resultEscaneo = findViewById(R.id.tv_ScaneoResult)



        val btnStartScan = findViewById<Button>(R.id.btn_StartScaneo)
        btnStartScan.setOnClickListener {
            if (checkCameraPermission()) {
                startQRScanning()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun startQRScanning() {
        IntentIntegrator(this).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                resultEscaneo.text = result.contents
                procesarTokenEscaneado(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun procesarTokenEscaneado(tokenEscaneado: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var uidEncontrado: String? = null
                var entradaIDEncontrada: String? = null

                for (uidSnapshot in dataSnapshot.children) {
                    val uid = uidSnapshot.key ?: continue

                    for (entradaSnapshot in uidSnapshot.children) {
                        val entradaID = entradaSnapshot.key
                        val token = entradaSnapshot.child("token").getValue(String::class.java)

                        if (token == tokenEscaneado) {
                            uidEncontrado = uid
                            entradaIDEncontrada = entradaID
                            break
                        }
                    }
                    if (uidEncontrado != null) break
                }

                if (uidEncontrado != null && entradaIDEncontrada != null) {
                    actualizarHistorial(uidEncontrado, entradaIDEncontrada)
                    eliminarEntradaAdquirida(uidEncontrado, entradaIDEncontrada)
                    eliminarEntradaYTokenDeRealtimeDatabase(uidEncontrado, entradaIDEncontrada)
                } else {
                    Toast.makeText(applicationContext, "Token no encontrado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, "Error en la base de datos: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun actualizarHistorial(uid: String, entradaID: String) {
        val dbFirestore = FirebaseFirestore.getInstance()
        val transaccion = hashMapOf(
            "entradaID" to entradaID,
            "accion" to "canjeado",
            "fecha" to FieldValue.serverTimestamp()
        )

        dbFirestore.collection("historialTransacciones").document(uid)
            .collection("transacciones").add(transaccion)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(applicationContext, "Transacción registrada con ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al registrar la transacción: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarEntradaAdquirida(uidEncontrado: String, entradaIDEncontrada: String) {
        val dbFirestore = FirebaseFirestore.getInstance()
        val entradaRef = dbFirestore.collection("users").document(uidEncontrado)

        // Suponiendo que 'entradasAdquiridas' es un array en Firestore
        entradaRef.update("entradasAdquiridas", FieldValue.arrayRemove(entradaIDEncontrada))
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Entrada eliminada de Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al eliminar la entrada de Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanning()
            } else {
                Toast.makeText(this, "Permiso de Cámara Denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun eliminarEntradaYTokenDeRealtimeDatabase(uidEncontrado: String, entradaIDEncontrada: String) {
        val dbRealtime = FirebaseDatabase.getInstance()
        val entradaRef = dbRealtime.getReference("users/$uidEncontrado/$entradaIDEncontrada")

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
}