package com.example.elimina_code

import ImageAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.io.OutputStream
import java.net.Socket

//ciao bello mio
class MainActivity : AppCompatActivity() {

    private var counter = 0
    private lateinit var printerSocket: Socket
    private lateinit var outputStream: OutputStream
    private lateinit var viewPager: ViewPager2
    private lateinit var imageAdapter: ImageAdapter

    private val images = listOf(
        R.drawable.image1,
        R.drawable.image2,
        R.drawable.image3
    )
    private val handler = Handler(Looper.getMainLooper())
    private val slideRunnable = Runnable { moveToNextSlide() }

    // Variabile per la sequenza di tocchi
    private var touchCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateRepartiUI()
        initSlideshow()

        // Aggiungi il rilevamento dei 3 tocchi nell'angolo in alto a destra
        setupTouchSequence()

        // Inizializza la connessione alla stampante
        connectToPrinter("192.168.1.30", 9100)
    }

    private fun connectToPrinter(ip: String, port: Int) {
        try {
            printerSocket = Socket(ip, port)
            outputStream = printerSocket.getOutputStream()
            Toast.makeText(this, "Stampante connessa", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore di connessione: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendToPrinter(number: Int) {
        try {
            val escPosCommand = "\u001B" + "@\n"
            val textToPrint = "Numero: $number\n"

            outputStream.write(escPosCommand.toByteArray())
            outputStream.write(textToPrint.toByteArray())
            outputStream.flush()

            outputStream.close()
            printerSocket.close()

            connectToPrinter("192.168.50.30", 9101)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore di stampa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRepartiUI() {
        val container = findViewById<LinearLayout>(R.id.repartiContainer)
        val noRepartiText = findViewById<TextView>(R.id.noRepartiText)

        // Svuota il contenitore dei reparti
        container.removeAllViews()

        if (RepartiManager.counterMap.isEmpty()) {
            noRepartiText.visibility = View.VISIBLE
        } else {
            noRepartiText.visibility = View.GONE

            // Conta quanti reparti ci sono per il layout dinamico
            val repartoCount = RepartiManager.counterMap.size

            // Loop per creare i pulsanti dei reparti
            for ((repartoName, count) in RepartiManager.counterMap) {
                val button = Button(this).apply {
                    text = "$repartoName ($count)"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT // Altezza variabile in base al contenuto
                    ).apply {
                        weight = 1f  // Distribuzione uniforme
                        setMargins(16, 16, 16, 16) // Margini tra i pulsanti
                    }

                    // Imposta lo sfondo smussato
                    background = resources.getDrawable(R.drawable.rounded_button, null)

                    // Testo bianco sui pulsanti
                    setTextColor(android.graphics.Color.WHITE)

                    // Aggiungere padding interno per "ciccionare" i pulsanti
                    setPadding(40, 30, 40, 30) // Imposta padding per aumentarne la "pienezza"

                    setOnClickListener {
                        val newCount = RepartiManager.counterMap[repartoName]?.plus(1) ?: 1
                        RepartiManager.counterMap[repartoName] = newCount
                        text = "$repartoName ($newCount)"

                        // Invia il numero alla stampante
                        counter++
                        sendToPrinter(counter)
                    }
                }
                container.addView(button)
            }
        }
    }


    private fun setupTouchSequence() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isInTopRightCorner(event.x, event.y)) {
                    touchCount++
                    if (touchCount == 3) {
                        showPasswordDialog()
                        touchCount = 0 // Resetta il contatore
                    }
                } else {
                    touchCount = 0 // Resetta il contatore se il tocco non è nell'angolo
                }
            }
            true
        }
    }

    private fun isInTopRightCorner(x: Float, y: Float): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        return x > screenWidth * 0.9 && y < screenHeight * 0.1
    }

    private fun showPasswordDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Accesso riservato")

        val passwordInput = EditText(this).apply {
            hint = "Inserisci la password"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        dialogBuilder.setView(passwordInput)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            if (passwordInput.text.toString() == "1234") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                Toast.makeText(this, "Password errata!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Annulla", null)
        dialogBuilder.show()
    }

    private fun initSlideshow() {
        viewPager = findViewById(R.id.slideshow)
        imageAdapter = ImageAdapter(images)
        viewPager.adapter = imageAdapter
        startSlideshow()
    }

    private fun startSlideshow() {
        handler.postDelayed(slideRunnable, 3000)
    }

    private fun moveToNextSlide() {
        viewPager.currentItem = (viewPager.currentItem + 1) % images.size
        handler.postDelayed(slideRunnable, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(slideRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateRepartiUI() // Aggiorna l'interfaccia dei reparti ogni volta che torniamo alla MainActivity
    }
}
