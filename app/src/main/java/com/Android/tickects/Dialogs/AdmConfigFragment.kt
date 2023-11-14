package com.Android.tickects.Dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.Android.tickects.R


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class AdmConfigFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_adm_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val edtNombreEvento = view.findViewById<EditText>(R.id.EDTnombreEvento)
        val edtDescripcion = view.findViewById<EditText>(R.id.EDTdescripcion)
        val edtUbicacion = view.findViewById<EditText>(R.id.EDTubicacion)
        val edtHora = view.findViewById<EditText>(R.id.EDThora)
        val edtDuracion = view.findViewById<EditText>(R.id.EDTduracion)
        val spinnerEventType = view.findViewById<Spinner>(R.id.spinnerEventType)
        val buttonLoadFlyer = view.findViewById<Button>(R.id.buttonLoadFlyer)
        val imageViewFlyerPreview = view.findViewById<ImageView>(R.id.imageViewFlyerPreview)
        val buttonSaveEvent = view.findViewById<Button>(R.id.buttonSaveEvent)

        val eventTypes = arrayOf("Concierto", "Deporte", "Cine", "Conferencia")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, eventTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEventType.adapter = adapter

        buttonLoadFlyer.setOnClickListener {
            cargarImagen()
        }

        buttonSaveEvent.setOnClickListener {
            val nombreEvento = edtNombreEvento.text.toString()
            val descripcion = edtDescripcion.text.toString()
            val ubicacion = edtUbicacion.text.toString()
            val hora = edtHora.text.toString()
            val duracion = edtDuracion.text.toString()
            val tipoEvento = spinnerEventType.selectedItem.toString()

            guardarImagenEnStorage(nombreEvento, descripcion, ubicacion, hora, duracion, tipoEvento)

            // Restablecer la vista previa de la imagen
            imageViewFlyerPreview.setImageResource(R.drawable.placeholder)
            imageViewFlyerPreview.visibility = View.GONE
        }
    }

    private fun cargarImagen() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selectedImageUri = data.data
                val imageViewFlyerPreview = view?.findViewById<ImageView>(R.id.imageViewFlyerPreview)
                imageViewFlyerPreview?.setImageURI(selectedImageUri)
                imageViewFlyerPreview?.visibility = View.VISIBLE
            }
        }
    }

    private fun guardarImagenEnStorage(
        nombreEvento: String,
        descripcion: String,
        ubicacion: String,
        hora: String,
        duracion: String,
        tipoEvento: String
    ) {
        if (selectedImageUri != null) {
            val storageRef: StorageReference = storage.reference
            val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        guardarEntradaEnFirestore(nombreEvento, descripcion, ubicacion, hora, duracion, tipoEvento, imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    // Manejar errores en caso de fallo al cargar la imagen
                }
        } else {
            // El usuario no seleccionó una imagen
            // Puedes mostrar un mensaje de error o realizar otra acción apropiada
        }
    }

    private fun guardarEntradaEnFirestore(
        nombreEvento: String,
        descripcion: String,
        ubicacion: String,
        hora: String,
        duracion: String,
        tipoEvento: String,
        imageUrl: String
    ) {
        val nuevaEntrada = hashMapOf(
            "nombreEvento" to nombreEvento,
            "descripcion" to descripcion,
            "ubicacion" to ubicacion,
            "hora" to hora,
            "duracion" to duracion,
            "tipoEvento" to tipoEvento,
            "imageUrl" to imageUrl
        )

        db.collection("entradas")
            .add(nuevaEntrada)
            .addOnSuccessListener { documentReference ->
                // La entrada se ha guardado con éxito en Firestore
            }
            .addOnFailureListener { e ->
                // Manejar errores en caso de fallo al guardar la entrada
            }
    }
}