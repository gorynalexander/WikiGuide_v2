package com.santo.wikiguide.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.santo.wikiguide.R
import com.santo.wikiguide.presentation.places.PlacesFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().replace(R.id.container, PlacesFragment()).commit()
    }
}