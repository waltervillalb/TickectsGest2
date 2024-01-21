package com.Android.tickects.eventoAdapter

import com.google.firebase.Timestamp

data class Historial(
    var idTransaccion: String? = "",
    var entradaID: String? = null,
    var accion: String? = null,
    var fecha: Timestamp? = null,
    var nombreEvento: String? = null // Este campo se llenará después de consultar la colección 'entradas'
)
