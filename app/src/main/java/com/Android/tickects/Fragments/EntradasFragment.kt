package com.Android.tickects.Fragments

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*


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

            recyclerView = view.findViewById(R.id.recyclerViewEntradas)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.setHasFixedSize(true)

            entradaArrayList = arrayListOf()

            entradasadapter = EntradasAdapter(entradaArrayList)
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
    }