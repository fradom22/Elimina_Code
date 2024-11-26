package com.example.elimina_code

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            // Aggiungi la logica per verificare username e password
            if (username == "admin" && password == "1234") {
                Toast.makeText(this, "Login effettuato con successo!", Toast.LENGTH_SHORT).show()
                // Successo del login, puoi navigare altrove se necessario
            } else {
                Toast.makeText(this, "Username o password errati!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
