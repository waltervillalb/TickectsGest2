package com.Android.tickects.eventoAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Android.tickects.R

class EntradasAdapter(private val entradaList : ArrayList<Entradas>):
    RecyclerView.Adapter<EntradasAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntradasAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_layout,
        parent,false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EntradasAdapter.MyViewHolder, position: Int) {

        val entrada : Entradas = entradaList [position]
        holder.nombreEvento.text = entrada.nombreEvento

    }

    override fun getItemCount(): Int {
        return entradaList.size
    }

    public class MyViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val nombreEvento : TextView = itemView.findViewById(R.id.tvNombreEvento)
    }
}