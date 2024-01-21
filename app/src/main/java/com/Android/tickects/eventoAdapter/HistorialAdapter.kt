package com.Android.tickects.eventoAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class HistorialAdapter(private val historialList: ArrayList<Historial>) :
    RecyclerView.Adapter<HistorialAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_layout_historial, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val historial: Historial = historialList[position]
        holder.nombreEvento.text = historial.nombreEvento
        holder.fecha.text = formatDate(historial.fecha)
        holder.idTransacc.text = historial.idTransaccion
        holder.accion.text = historial.accion
    }

    private fun formatDate(timestamp: Timestamp?): String {
        return timestamp?.toDate()?.let { date ->
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            formatter.format(date)
        } ?: "Fecha desconocida"
    }

    override fun getItemCount(): Int {
        return historialList.size
    }

    fun updateData(newData: List<Historial>) {
        historialList.clear()
        historialList.addAll(newData)
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreEvento: TextView = itemView.findViewById(R.id.tvNombreEventohistorial)
        val fecha: TextView = itemView.findViewById(R.id.tvFechahistorial)
        val idTransacc: TextView = itemView.findViewById(R.id.tvIdTransaccion)
        val accion: TextView = itemView.findViewById(R.id.accion)
    }
}