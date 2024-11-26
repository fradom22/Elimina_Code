package com.example.elimina_code

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        setupAddRepartoButton()
        setupResetButton()
        setupDeleteRepartoButton()
        setupExitButton() // Configura il pulsante "Torna alla pagina principale"
    }

    private fun setupAddRepartoButton() {
        val addRepartoButton = findViewById<Button>(R.id.addRepartoButton)
        addRepartoButton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle("Aggiungi Reparto")

            // Layout personalizzato per il dialogo
            val dialogLayout = layoutInflater.inflate(R.layout.dialog_add_reparto, null)
            val inputName = dialogLayout.findViewById<EditText>(R.id.inputRepartoName)
            val colorSpinner = dialogLayout.findViewById<Spinner>(R.id.colorSpinner)

            // Configura lo spinner con i colori
            val colors = arrayOf("Verde", "Blu", "Rosso")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            colorSpinner.adapter = adapter

            dialogBuilder.setView(dialogLayout)

            dialogBuilder.setPositiveButton("OK") { _, _ ->
                val repartoName = inputName.text.toString().trim()
                val selectedColor = colorSpinner.selectedItem.toString()

                if (repartoName.isNotEmpty()) {
                    // Converti il colore selezionato in codice colore
                    val colorCode = when (selectedColor) {
                        "Verde" -> "#4CAF50"
                        "Blu" -> "#2196F3"
                        "Rosso" -> "#F44336"
                        else -> "#4CAF50" // Default verde
                    }

                    // Aggiungi il reparto con il colore selezionato
                    RepartiManager.counterMap[repartoName] = 1
                    RepartiManager.colorMap[repartoName] = colorCode

                    Toast.makeText(this, "Reparto aggiunto: $repartoName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Il nome del reparto non può essere vuoto", Toast.LENGTH_SHORT).show()
                }
            }

            dialogBuilder.setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }

            dialogBuilder.show()
        }
    }



    private fun setupResetButton() {
        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.setOnClickListener {
            for (key in RepartiManager.counterMap.keys) {
                RepartiManager.counterMap[key] = 1
            }
            Toast.makeText(this, "Contatori azzerati", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDeleteRepartoButton() {
        val deleteRepartoButton = findViewById<Button>(R.id.deleteRepartoButton)
        deleteRepartoButton.setOnClickListener {
            val repartoNames = RepartiManager.counterMap.keys.toList()
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle("Seleziona il reparto da eliminare")

            dialogBuilder.setItems(repartoNames.toTypedArray()) { dialog, which ->
                val repartoName = repartoNames[which]
                RepartiManager.counterMap.remove(repartoName)
                RepartiManager.colorMap.remove(repartoName)
                Toast.makeText(this, "Reparto $repartoName eliminato", Toast.LENGTH_SHORT).show()
            }

            dialogBuilder.setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }

            dialogBuilder.show()
        }
    }

    private fun setupExitButton() {
        val exitButton = findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            finish() // Torna alla MainActivity
        }
    }
}