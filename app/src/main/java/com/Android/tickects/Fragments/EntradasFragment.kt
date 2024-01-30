package com.Android.tickects.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R
import com.Android.tickects.eventoAdapter.Entradas
import com.Android.tickects.eventoAdapter.EntradasAdapter
import com.Android.tickects.qr_generator
import com.google.firebase.firestore.*
import java.lang.Math.abs
import java.lang.Math.floor
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class EntradasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var entradaArrayList: ArrayList<Entradas>
    private lateinit var entradasadapter: EntradasAdapter
    private lateinit var db: FirebaseFirestore
    val usuarioId = userId.iduser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entradas, container, false)
        eliminarEntradasAntiguas()
        recyclerView = view.findViewById(R.id.recyclerViewEntradas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        entradaArrayList = arrayListOf()
        // Inicializa el adaptador con el listener de clics
        entradasadapter = EntradasAdapter(entradaArrayList) { entradaId ->
            // Lógica a ejecutar cuando se presiona el botón "Ver"
            abrirDetalleEntrada(entradaId as String)
        }
        recyclerView.adapter = entradasadapter
        EventChangeListener()
        return view
    }


    private fun EventChangeListener() {
        db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(usuarioId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val entradasIds = document.get("entradasAdquiridas") as? List<String> ?: listOf()
                for (entradaId in entradasIds) {
                    db.collection("entradas").document(entradaId).get()
                        .addOnSuccessListener { entradaDoc ->
                            val entrada = entradaDoc.toObject(Entradas::class.java)
                            if (entrada != null) {
                                entrada.idEntrada = entradaDoc.id // Asignar el ID del documento
                                entradaArrayList.add(entrada)
                                entradasadapter.notifyDataSetChanged()
                            }
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore Error", e.message.toString())
        }
    }

    private fun abrirDetalleEntrada(entradaId: String) {
        val intent = Intent(context, qr_generator::class.java)
        intent.putExtra("EXTRA_ENTRADA_ID", entradaId)
        startActivity(intent)
    }

    private fun eliminarEntradasAntiguas() {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(usuarioId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val entradasIds = document.get("entradasAdquiridas") as? List<String> ?: listOf()

                for (entradaId in entradasIds) {
                    val entradaRef = db.collection("entradas").document(entradaId)
                    entradaRef.get().addOnSuccessListener { entradaDocument ->
                        if (entradaDocument.exists()) {
                            val fechaEventoStr = entradaDocument.getString("fecha")
                            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val fechaEvento: Date? = try {
                                formato.parse(fechaEventoStr) // Convierte el String a Date
                            } catch (e: ParseException) {
                                null // En caso de error en el parseo, asigna null
                            }

                            fechaEvento?.let {
                                val hoy = Date()
                                if (it.before(hoy)) { // Verifica que la fecha del evento sea anterior a hoy
                                    val diasTranscurridos = calcularDiferenciaEnDias(it, hoy)

                                    if (diasTranscurridos >= 15) {
                                        eliminarEntradaAntigua(usuarioId, entradaId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore Error", "Error al buscar entradas del usuario: ${e.message}")
        }
    }

    private fun calcularDiferenciaEnDias(fechaInicio: Date, fechaFin: Date): Long {
        val diferenciaMillis = abs(fechaFin.time - fechaInicio.time)
        val dias = floor(diferenciaMillis / (1000 * 60 * 60 * 24).toDouble()).toLong()
        return dias
    }

    private fun eliminarEntradaAntigua(usuarioId: String, entradaId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(usuarioId)
            .update("entradasAdquiridas", FieldValue.arrayRemove(entradaId))
            .addOnSuccessListener {
                Log.d("Firestore Success", "Entrada antigua eliminada para el usuario: $usuarioId")
            }
            .addOnFailureListener { e ->
                Log.e(
                    "Firestore Error",
                    "Error al eliminar entrada antigua para el usuario $usuarioId: ${e.message}"
                )
            }
    }
}