package com.Android.tickects.eventoAdapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EntradasAdapter(
    private val entradaList: ArrayList<Entradas>,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<EntradasAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntradasAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val entrada: Entradas = entradaList[position]

        // Asignar el nombre del evento al TextView en el holder
        holder.nombreEvento.text = entrada.nombreEvento ?: "Nombre no disponible"
        holder.fecha.text = entrada.fecha ?: "Fecha no disponible"

// Asignar la hora del evento al TextView en el holder
        holder.hora.text = entrada.hora ?: "Hora no disponible"

        Glide.with(holder.itemView.context)
            .load(entrada.imageUrl)
            .placeholder(R.drawable.placeholder) // Imagen de placeholder
            .error(R.drawable.placeholder)      // Imagen en caso de error
            .into(holder.flyerImagen)
        // Log para verificar los datos de la entrada
        Log.d("EntradasAdapter", "Entrada: ${entrada.nombreEvento}, Fecha del Evento: ${entrada.fecha}")

        // Obtener la fecha actual
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaActual = formatoFecha.format(Date())

        // Log para verificar la fecha actual
        Log.d("EntradasAdapter", "Fecha actual: $fechaActual")

        val btnVer: Button = holder.itemView.findViewById(R.id.btnVer)

        if (entrada.fecha >= fechaActual) {
            // Mostrar el botón si estamos en el día del evento
            btnVer.visibility = View.VISIBLE
            Log.d("EntradasAdapter", "Mostrando botón para ${entrada.nombreEvento}")
        } else {
            // Ocultar el botón si no estamos en el día del evento
            btnVer.visibility = View.INVISIBLE
            Log.d("EntradasAdapter", "Ocultando botón para ${entrada.nombreEvento}")
        }

        btnVer.setOnClickListener {
            Log.d("EntradasAdapter", "ID de Entrada al hacer clic: ${entrada.idEntrada}")
            onItemClicked(entrada.idEntrada)
        }

}

    override fun getItemCount(): Int {
        return entradaList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreEvento: TextView = itemView.findViewById(R.id.tvNombreEvento)
        val fecha: TextView = itemView.findViewById(R.id.tvFechaEvento)
        val hora: TextView = itemView.findViewById(R.id.tvHoraEvento)
        val flyerImagen: ImageView = itemView.findViewById(R.id.ivFlyer)
    }
}