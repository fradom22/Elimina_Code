package com.example.elimina_code

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class RepartiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Questo risolve anche l'ambiguità di inflate
        return inflater.inflate(R.layout.fragment_reparti, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAdd = view.findViewById<Button>(R.id.btnAddReparto)

        btnAdd.setOnClickListener {
            setupAddRepartoButton()
        }
    }

    private fun setupAddRepartoButton() {
        // TODO: metti qui la logica vera (dialog, aggiunta reparto, ecc.)
        // Per ora è vuota così compila
    }
}