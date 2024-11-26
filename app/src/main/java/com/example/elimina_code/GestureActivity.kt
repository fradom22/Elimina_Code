package com.example.elimina_code

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GestureActivity : AppCompatActivity() {

    // Contatore per il numero di tocchi
    private var touchCount = 0
    // Tempo massimo tra tocchi per considerare 3 tocchi consecutivi (in millisecondi)
    private val maxTouchInterval = 500L // 500 ms
    private var lastTouchTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    // Questo metodo intercetta il tocco dell'utente e lo passa al GestureDetector
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // Se il tempo tra due tocchi consecutivi è troppo lungo, resetta il contatore
        if (currentTime - lastTouchTime > maxTouchInterval) {
            touchCount = 0
        }

        // Incrementa il contatore dei tocchi
        touchCount++
        lastTouchTime = currentTime

        // Se l'utente ha toccato tre volte consecutivamente, avvia l'AdminActivity
        if (touchCount == 3) {
            val intent = Intent(this, AdminActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Accesso amministratore consentito!", Toast.LENGTH_SHORT).show()
            touchCount = 0 // Reset del contatore per evitare accessi ripetuti
        } else {
            // Mostra il numero di tocchi rimanenti
            Toast.makeText(this, "Tocca ancora ${3 - touchCount} volte", Toast.LENGTH_SHORT).show()
        }

        return super.onTouchEvent(event)
    }
}
