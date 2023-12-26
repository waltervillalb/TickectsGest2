package com.Android.tickects.Dialogs

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

    class AdapterDeEntradas(private val entradas: MutableList<DocumentSnapshot> = mutableListOf()) :
        RecyclerView.Adapter<AdapterDeEntradas.EntradaViewHolder>() {

        // Método para agregar una entrada a la lista
        fun addEntrada(entrada: DocumentSnapshot) {
            entradas.add(entrada)
            notifyItemInserted(entradas.size - 1)
        }

        // Método para limpiar todas las entradas de la lista
        fun clearEntradas() {
            entradas.clear()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntradaViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_layout, parent, false)
            return EntradaViewHolder(view)
        }

        override fun onBindViewHolder(holder: EntradaViewHolder, position: Int) {
            val entrada = entradas[position]
            holder.bind(entrada)
        }

        override fun getItemCount(): Int {
            return entradas.size
        }

        class EntradaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.image1)
            val eventoNombreTextView: TextView = itemView.findViewById(R.id.eventoNombre1)
            val horaFechaTextView: TextView = itemView.findViewById(R.id.horaFecha)
            val tipoEventoTextView: TextView = itemView.findViewById(R.id.tipoEvento)
            val ubicacion: TextView = itemView.findViewById(R.id.ubicacion)

            fun bind(entrada: DocumentSnapshot) {
                // Configurar las vistas con los datos de la entrada
                val eventoNombre = entrada.getString("nombre") ?: "Nombre no disponible"
                val horaFecha = entrada.getString("horaFecha") ?: "Fecha y hora no disponibles"
                val imagenUrl = entrada.getString("imagenUrl")
                val tipo = entrada.getString("tipoEvento") ?: "Tipo de evento no disponible"
                val ubi = entrada.getString("Ubicacion") ?: "Ubicación no disponible"


                eventoNombreTextView.text = eventoNombre
                horaFechaTextView.text = horaFecha
                tipoEventoTextView.text = tipo
                ubicacion.text = ubi

                Glide.with(itemView.context)
                    .load(imagenUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error_image)
                    .into(imageView)
            }
        }
    }