package com.Android.tickects.Dialogs

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.Android.tickects.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EntradasFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entradas, container, false)
        val eventoImageView: ImageView = view.findViewById(R.id.image1)
        val nombreEventoTextView: TextView = view.findViewById(R.id.eventoNombre1)
        val tipoEventoTextView: TextView = view.findViewById(R.id.tipoEvento)
        val ubicacionTextView: TextView = view.findViewById(R.id.ubicacion)
        val horaFechaTextView: TextView = view.findViewById(R.id.horaFecha)

        // Recupera el UID del usuario desde la clase UserId
        val usuarioId = userId.iduser

        // Referencia a la ubicación del usuario en la base de datos
        val userRef: DocumentReference = firestore.collection("users").document(usuarioId)

        // Agregar un listener para obtener los datos del usuario
        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Recupera las entradas adquiridas (asumo que es un mapa)
                val entradasAdquiridaMap =
                    documentSnapshot.get("entradasAdquirida") as? Map<String, Boolean>

                // Verifica si hay entradas adquiridas (IDs en "true")
                if (entradasAdquiridaMap != null) {
                    for ((entradaId, adquirida) in entradasAdquiridaMap) {
                        if (adquirida) {
                            // Si la entrada está marcada como adquirida, busca los detalles en la colección "entradas"
                            val entradasCollectionRef = firestore.collection("entradas")
                            entradasCollectionRef.document(entradaId).get()
                                .addOnSuccessListener { entradaSnapshot ->
                                    if (entradaSnapshot.exists()) {
                                        // Recupera los datos de la entrada
                                        val nombreEvento =
                                            entradaSnapshot.getString("nombreEvento")
                                        val tipoEvento =
                                            entradaSnapshot.getString("tipoEvento")
                                        val ubicacion =
                                            entradaSnapshot.getString("ubicacion")
                                        val horaFecha =
                                            entradaSnapshot.getString("hora") + " - " + entradaSnapshot.getString(
                                                "fecha"
                                            )
                                        val imageUrl =
                                            entradaSnapshot.getString("imageUrl")

                                        // Utiliza bibliotecas como Picasso o Glide para cargar la imagen desde la URL
                                        // Aquí se utiliza un marcador de posición (placeholder) en caso de que la URL de la imagen sea nula
                                        if (imageUrl != null) {
                                            Picasso.get().load(imageUrl)
                                                .placeholder(R.drawable.placeholder)
                                                .into(eventoImageView)
                                        }

                                        nombreEventoTextView.text = nombreEvento
                                        tipoEventoTextView.text = tipoEvento
                                        ubicacionTextView.text = ubicacion
                                        horaFechaTextView.text = horaFecha
                                    } else {
                                        // La entrada no existe en la colección "entradas"
                                        // Manejar el caso en el que no se encuentre la entrada
                                    }
                                }.addOnFailureListener { exception ->
                                    // Manejar errores si la consulta a la base de datos de "entradas" falla
                                    Log.e(TAG, "Error al obtener datos de la entrada: $exception")
                                }
                        }
                    }
                }
            } else {
                // El usuario no existe en la base de datos
                Toast.makeText(
                    requireContext(),
                    "Usuario no encontrado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener { exception ->
            // Manejar errores de lectura de la base de datos de usuario
            Toast.makeText(
                requireContext(),
                "Error al obtener datos del usuario: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }

        return view
    }
}