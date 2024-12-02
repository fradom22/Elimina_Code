package com.example.elimina_code

import ImageAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
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
import java.text.SimpleDateFormat
import java.util.Date
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import java.util.*

class MainActivity : AppCompatActivity() {


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


        // Imposta il layout landscape o portrait
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main) // Questo carica il layout landscape
            initLandscapeLayout()
        }else{
            setContentView(R.layout.activity_main) // Layout portrait
            initPortaitLayout()
        }

        hideSystemUI()

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

    private fun initPortaitLayout() {
        val timeTextView = findViewById<TextView>(R.id.timeTextView) // Assicurati che l'ID sia corretto
        val handler = Handler(Looper.getMainLooper())

        // Aggiorna data e ora ogni secondo
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val currentTime = Calendar.getInstance().time
                val sdf = SimpleDateFormat("dd MMM yyyy   HH:mm", Locale.getDefault())
                timeTextView.text = sdf.format(currentTime)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)
    }


    private fun initLandscapeLayout() {
        val timeTextView = findViewById<TextView>(R.id.timeTextView) // Assicurati che l'ID sia corretto
        val handler = Handler(Looper.getMainLooper())

        // Aggiorna data e ora ogni secondo
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val currentTime = Calendar.getInstance().time
                val sdf = SimpleDateFormat("dd MMM yyyy   HH:mm", Locale.getDefault())
                timeTextView.text = sdf.format(currentTime)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)
    }


    @SuppressLint("SetTextI18n")
    private fun updateNewsTicker() {
        val newsTicker = findViewById<TextView>(R.id.newsTicker)
        val feedUrl = "https://www.ansa.it/sito/ansait_rss.xml"

        // Recupera le notizie in un thread separato
        Thread {
            val news = RSSParser.getNewsFromFeed(feedUrl)
            runOnUiThread {
                if (news.isNotEmpty()) {
                    // Combina le notizie in una stringa unica
                    val newsText = news.joinToString(" • ")
                    newsTicker.text = newsText
                    newsTicker.isSelected = true // Necessario per il marquee
                } else {
                    newsTicker.text = "Nessuna notizia disponibile"
                }
            }
        }.start()
    }

    private val updateNewsRunnable = object : Runnable {
        override fun run() {
            updateNewsTicker()
            handler.postDelayed(this, 130000)
        }
    }






    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Se il dispositivo ha Android 11 (API livello 30) o superiore
            val windowInsetsController = window.insetsController
            windowInsetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()) // Nascondi barra di stato e navigazione
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // Mostra le barre se l'utente scorre verso il basso
            }
        } else {
            // Per dispositivi con versioni di Android inferiori a 11 (API livello 30)
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or // Nascondi la barra di stato
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or // Nascondi la barra di navigazione
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) // Rendere il comportamento immersivo (l'utente deve fare swipe per mostrare le barre)
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Riferimenti alle barre
        val topBar = findViewById<LinearLayout>(R.id.topBar)
        val bottomBar = findViewById<LinearLayout>(R.id.bottomBar)

        // Verifica l'orientamento
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape, mostra le barre
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
        } else {
            // In portrait, nascondi le barre
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
        }
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

    @SuppressLint("SimpleDateFormat")
    private fun sendToPrinter(repartoName: String, number: Int) {
        // Se siamo in modalità visione, non fare nulla
        if (isVisualMode) {
            return
        }

        // Procedi con la stampa solo se NON siamo in modalità visione
        Thread {
            try {
                // Ottieni la data e l'ora attuali
                val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date())

                // Comando di reset della stampante
                val escPosCommand = "\u001B@"  // Reset della stampante (default)

                // Comando per centratura del testo
                val centeringCommand = "\u001B\u0061\u0001"  // ESC a 1: Testo centrato

                // Comandi per diverse dimensioni del testo
                val largeTextCommand = "\u001D!\u0033"  // GS ! 51: Testo extra grande e spesso
                val extraLargeTextCommand = "\u001D!\u0011"  // GS ! 17: Testo grande senza grassetto
                val boldTextOnCommand = "\u001B\u0045\u0001"  // ESC E 1: Grassetto ON
                val boldTextOffCommand = "\u001B\u0045\u0000"  // ESC E 0: Grassetto OFF
                val normalTextCommand = "\u001B!\u0000"  // ESC ! 0: Testo normale

                // Comando per il taglio della carta
                val cutPaperCommand = "\u001D\u0056\u0041"  // GS V 65: Taglia carta

                // Formattazione per "ESYTECH" (testo grande e in grassetto, centrato)
                val formattedHeader = "$centeringCommand$boldTextOnCommand$largeTextCommand" +
                        "ESYTECH\n\n$boldTextOffCommand"

                // Formattazione per il nome del reparto (testo grande senza grassetto, centrato)
                val formattedRepartoName = "$centeringCommand$extraLargeTextCommand$repartoName\n\n"

                // Formattazione per il numero (grande e grassetto)
                val formattedNumber = "$centeringCommand$boldTextOnCommand$largeTextCommand$number\n\n$boldTextOffCommand"

                // Formattazione per "GRAZIE PER L'ATTESA" (piccolo e in grassetto)
                val formattedThanksMessage = "$centeringCommand$boldTextOnCommand$normalTextCommand" +
                        "GRAZIE PER L'ATTESA\n\n$boldTextOffCommand"

                // Formattazione per la data e l'ora
                val formattedDateTime = "$centeringCommand$normalTextCommand$currentDate\n\n"

                // Righe vuote per spazio extra tra le stampe
                val extraSpacing = "\n\n\n\n\n\n"

                // Invio dei comandi alla stampante
                outputStream.write(escPosCommand.toByteArray())  // Reset stampante
                outputStream.write(formattedHeader.toByteArray())  // Intestazione "ESYTECH"
                outputStream.write(formattedRepartoName.toByteArray())  // Nome del reparto
                outputStream.write(formattedNumber.toByteArray())  // Numero del reparto
                outputStream.write(formattedThanksMessage.toByteArray())  // "Grazie per l'attesa"
                outputStream.write(formattedDateTime.toByteArray())  // Data e ora
                outputStream.write(extraSpacing.toByteArray())  // Spazio tra le stampe
                outputStream.write(cutPaperCommand.toByteArray())  // Taglio della carta
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

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
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

                    // Numero a destra (visualizzato e incrementato in modalità visiva)
                    val countView = TextView(this).apply {
                        text = count.toString()  // Mostra il numero
                        textSize = 70f  // Numero grande
                        setTextColor(android.graphics.Color.WHITE)
                        gravity = Gravity.END
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }

                    buttonLayout.addView(repartoNameView)
                    buttonLayout.addView(countView)

                    // In modalità visiva, quando clicchi sul pulsante si incrementa il numero
                    buttonLayout.setOnClickListener {
                        // Incremento del contatore
                        val newCount = (RepartiManager.counterMap[repartoName] ?: -1) + 1
                        RepartiManager.counterMap[repartoName] = newCount
                        countView.text = newCount.toString()  // Aggiorna il numero visualizzato

                        showServingNumberDialog(repartoName,newCount)

                        // Salvataggio del contatore aggiornato
                        saveCounters()  // Aggiorna la memoria persistente
                    }

                    container.addView(buttonLayout)
                } else {
                    // Modalità Stampa
                    val button = Button(this).apply {
                        text = repartoName  // Mostra solo il nome del reparto, senza il numero
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

                        // In modalità stampa, quando clicchi sul pulsante si incrementa il numero e si stampa
                        setOnClickListener {
                            // Incremento del contatore
                            val newCount = (RepartiManager.counterMap[repartoName] ?: -1) + 1
                            RepartiManager.counterMap[repartoName] = newCount
                            sendToPrinter(repartoName, newCount)  // Invia alla stampante con il numero incrementato

                            showTakeNumberDialog()

                            // Salvataggio del contatore aggiornato
                            saveCounters()  // Salva il contatore dopo l'aggiornamento
                        }
                    }

                    container.addView(button)
                }
            }
        }
    }

    // Funzione per salvare i contatori aggiornati
    private fun saveCounters() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Salva il contatore nella memoria persistente
        RepartiManager.counterMap.forEach { (repartoName, count) ->
            editor.putInt(repartoName, count)
        }

        editor.apply()  // Salva le modifiche
    }


    @SuppressLint("SetTextI18n")
    fun showServingNumberDialog(repartoName: String, number: Int) {
        // Gonfia il layout personalizzato
        val dialogView = layoutInflater.inflate(R.layout.dialog_serving_number, null)

        // Trova i TextView e imposta il contenuto
        val servingTitle = dialogView.findViewById<TextView>(R.id.servingTitle)
        val servingLabel = dialogView.findViewById<TextView>(R.id.servingLabel)
        val servingNumber = dialogView.findViewById<TextView>(R.id.servingNumber)

        // Imposta il nome del reparto in grande
        servingTitle.text = repartoName  // Nome del reparto


        servingLabel.text = "Serviamo il numero:"

        // Imposta il numero in grassetto
        servingNumber.text = number.toString()  // Numero associato

        // Costruisci il dialog con il layout personalizzato
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)

        // Crea e mostra il dialog
        val dialog = dialogBuilder.create()
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog)

        // Usa un Handler per chiudere il dialog dopo 2 secondi
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 3000) // 2000 millisecondi = 2 secondi
    }


    private fun showTakeNumberDialog() {
        // Gonfia il layout del dialogo
        val dialogView = layoutInflater.inflate(R.layout.dialog_take_number, null)
        val dialogImageView = dialogView.findViewById<ImageView>(R.id.dialogImage)

        dialogImageView.setImageResource(R.drawable.scontr)

        // Costruisci il dialogo con il layout personalizzato
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true) // Permette di chiudere il dialogo cliccando fuori

        // Crea e mostra il dialogo
        val dialog = dialogBuilder.create()
        dialog.show()

        // Imposta uno sfondo arrotondato (opzionale)
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog)

        // Usa un Handler per chiudere il dialogo automaticamente dopo 2 secondi
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 5000) // Chiudi dopo 3 secondi
    }


    @SuppressLint("ClickableViewAccessibility")
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
        updateNewsTicker()
        handler.post(updateNewsRunnable)
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)


            // Recupera la modalità corrente dalle preferenze
            isVisualMode = sharedPreferences.getBoolean("isVisualMode", false)

            // Recupera l'orientamento corrente
            val orientation = resources.configuration.orientation
        orientation == Configuration.ORIENTATION_LANDSCAPE

            // Aggiorna l'interfaccia utente in base alla modalità e all'orientamento
            updateRepartiUI(isVisualMode)

            // Gestisci la visibilità delle barre
            val topBar = findViewById<LinearLayout>(R.id.topBar)
            val bottomBar = findViewById<LinearLayout>(R.id.bottomBar)

            if (isVisualMode) {
                // In modalità landscape e in modalità visual, mostra le barre
                topBar.visibility = View.VISIBLE
                bottomBar.visibility = View.VISIBLE
            } else if (!isVisualMode) {
                // In modalità portrait e stampa, mostra solo la barra superiore
                topBar.visibility = View.VISIBLE
                bottomBar.visibility = View.GONE
            }
    }
}
