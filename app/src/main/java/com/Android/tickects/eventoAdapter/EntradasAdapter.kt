package com.Android.tickects.eventoAdapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R

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
        holder.nombreEvento.text = entrada.nombreEvento

        // Asume que tienes un botón en tu item_layout con ID btnVer
        val btnVer: Button = holder.itemView.findViewById(R.id.btnVer)
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
        // Puedes también inicializar el botón aquí si lo prefieres
    }
}