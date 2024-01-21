package com.Android.tickects.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R
import com.Android.tickects.eventoAdapter.Historial
import com.Android.tickects.eventoAdapter.HistorialAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HistorialFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historialAdapter: HistorialAdapter
    private var historialList = ArrayList<Historial>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_historial, container, false)
        Log.d("HistorialFragment", "onCreateView called")

        historialList = ArrayList()

        recyclerView = view.findViewById(R.id.recyclerViewHistorial)
        recyclerView.layoutManager = LinearLayoutManager(context)

        historialAdapter = HistorialAdapter(historialList)
        recyclerView.adapter = historialAdapter

        setupEventListener()
        return view
    }

    private fun setupEventListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("HistorialFragment", "UserID: $userId")
        val dbFirestore = FirebaseFirestore.getInstance()

        dbFirestore.collection("historialTransacciones").document(userId)
            .collection("transacciones")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HistorialFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                Log.d(
                    "HistorialFragment",
                    "SnapshotListener activated, document count: ${snapshot?.documents?.size ?: "null"}"
                )
                historialList.clear()

                snapshot?.documents?.forEach { document ->
                    val historial = document.toObject(Historial::class.java)
                    historial?.let { transaccion ->
                        historial.idTransaccion = document.id
                        historial.entradaID?.let { idEntrada ->
                            dbFirestore.collection("entradas").document(idEntrada).get()
                                .addOnSuccessListener { entradaDoc ->
                                    Log.d("HistorialFragment", "EntradaDoc: ${entradaDoc.data}")
                                    transaccion.nombreEvento = entradaDoc.getString("nombreEvento")
                                    Log.d(
                                        "HistorialFragment",
                                        "Nombre del evento: ${transaccion.nombreEvento}"
                                    )
                                    historialList.add(transaccion)
                                    historialAdapter.notifyDataSetChanged()
                                }.addOnFailureListener { e ->
                                    Log.w(
                                        "HistorialFragment",
                                        "Error al obtener el nombre del evento",
                                        e
                                    )
                                }
                        }
                    }
                }
            }
    }
}