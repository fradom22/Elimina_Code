package com.example.elimina_code

import ImageAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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

class MainActivity : AppCompatActivity() {

    private var counter = 0
    private lateinit var printerSocket: Socket
    private lateinit var outputStream: OutputStream
    private lateinit var viewPager: ViewPager2
    private lateinit var imageAdapter: ImageAdapter
    private var isVisualMode: Boolean = false // Variabile per tracciare la modalità corrente

    private val images = listOf(
        R.drawable.image1,
    )
    private val handler = Handler(Looper.getMainLooper())
    private val slideRunnable = Runnable { moveToNextSlide() }

    private var touchCount = 0 // Per la sequenza di tocchi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Carica i reparti salvati
        RepartiManager.loadReparti(this)

        // Recupera la modalità corrente
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        isVisualMode = sharedPreferences.getBoolean("isVisualMode", false)

        updateRepartiUI(isVisualMode)
        initSlideshow()
        setupTouchSequence()
        connectToPrinter("192.168.1.168", 9100)
    }

    private fun connectToPrinter(ip: String, port: Int) {
        Thread {
            try {
                printerSocket = Socket(ip, port)
                outputStream = printerSocket.getOutputStream()
                runOnUiThread {
                    Toast.makeText(this, "Stampante connessa", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Errore di connessione: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendToPrinter(repartoName: String, number: Int) {
        Thread {
            try {
                // Comandi ESC/POS per stampare
                val escPosCommand = "\u001B" + "@\n"  // Reset della stampante
                val textToPrint = "Reparto: $repartoName\nNumero: $number\n"  // Nome del reparto e numero

                // Invio dei comandi alla stampante
                outputStream.write(escPosCommand.toByteArray())  // Reset
                outputStream.write(textToPrint.toByteArray())    // Stampa il reparto e numero
                outputStream.flush()

                runOnUiThread {
                    Toast.makeText(this, "Stampa completata", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Errore di stampa: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    private fun updateRepartiUI(isVisualMode: Boolean) {
        val container = findViewById<LinearLayout>(R.id.repartiContainer)
        val noRepartiText = findViewById<TextView>(R.id.noRepartiText)

        container.removeAllViews()

        if (RepartiManager.counterMap.isEmpty()) {
            noRepartiText.visibility = View.VISIBLE
        } else {
            noRepartiText.visibility = View.GONE

            for ((repartoName, count) in RepartiManager.counterMap) {
                if (isVisualMode) {
                    // Modalità visiva
                    val buttonLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1f
                            setMargins(16, 16, 16, 16)
                        }
                        background = resources.getDrawable(R.drawable.rounded_button, null)
                        setPadding(40, 40, 40, 40)
                    }

                    // Nome del reparto a sinistra
                    val repartoNameView = TextView(this).apply {
                        text = repartoName
                        textSize = 32f
                        setTextColor(android.graphics.Color.WHITE)
                        gravity = Gravity.START
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }

                    // Numero incrementale a destra
                    val countView = TextView(this).apply {
                        text = count.toString()
                        textSize = 70f  // Numero grande
                        setTextColor(android.graphics.Color.WHITE)
                        gravity = Gravity.END
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }

                    buttonLayout.addView(repartoNameView)
                    buttonLayout.addView(countView)

                    buttonLayout.setOnClickListener {
                        val newCount = RepartiManager.counterMap[repartoName]?.plus(1) ?: 1
                        RepartiManager.counterMap[repartoName] = newCount
                        countView.text = newCount.toString()
                    }

                    container.addView(buttonLayout)
                } else {
                    // Modalità Stampa
                    val button = Button(this).apply {
                        text = "$repartoName ($count)"
                        textSize = 32f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0
                        ).apply {
                            weight = 1f
                            setMargins(16, 16, 16, 16)
                        }
                        background = resources.getDrawable(R.drawable.rounded_button, null)
                        setTextColor(android.graphics.Color.WHITE)
                        setPadding(40, 30, 40, 30)

                        setOnClickListener {
                            val newCount = RepartiManager.counterMap[repartoName]?.plus(1) ?: 1
                            RepartiManager.counterMap[repartoName] = newCount
                            text = "$repartoName ($newCount)"
                            sendToPrinter(repartoName, newCount)
                        }
                    }

                    container.addView(button)
                }
            }
        }
    }



    private fun setupTouchSequence() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isInTopLeftCorner(event.x, event.y)) {
                    touchCount++
                    if (touchCount == 3) {
                        showPasswordDialog()
                        touchCount = 0
                    }
                } else {
                    touchCount = 0
                }
            }
            true
        }
    }

    private fun isInTopLeftCorner(x: Float, y: Float): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        return x < screenWidth * 0.1 && y < screenHeight * 0.1
    }

    private fun showPasswordDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Accesso riservato")

        val passwordInput = EditText(this).apply {
            hint = "Inserisci la password"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(40, 30, 40, 30)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(passwordInput)
        }

        dialogBuilder.setView(layout)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            if (passwordInput.text.toString() == "1234") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                Toast.makeText(this, "Password errata!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Annulla", null)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog)
        dialog.show()
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
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        isVisualMode = sharedPreferences.getBoolean("isVisualMode", false)
        updateRepartiUI(isVisualMode)
    }
}
