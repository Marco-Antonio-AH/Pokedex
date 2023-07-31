package com.example.pokedexbueno

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var btnShowPokemon: Button
    private lateinit var imgPokemon: ImageView
    private lateinit var tvPokemonName: TextView
    private lateinit var tvPokemonDetails: TextView // TextView para mostrar más detalles del Pokémon
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: android.location.Location? = null
    private lateinit var vibrator: Vibrator
    private lateinit var tvPokemonInfo: TextView
    private val viewModel: PokemonViewModel by viewModels()

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnShowPokemon = findViewById(R.id.btnShowPokemon)
        imgPokemon = findViewById(R.id.imgPokemon)
        tvPokemonName = findViewById(R.id.tvPokemonName)
        tvPokemonDetails = findViewById(R.id.tvPokemonDetails) // Inicializar el TextView de detalles del Pokémon
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        tvPokemonInfo = findViewById(R.id.tvPokemonInfo)

        fusedLocationClient = FusedLocationProviderClient(this)

        btnShowPokemon.setOnClickListener {
            viewModel.fetchRandomPokemon()
        }

        viewModel.pokemonLiveData.observe(this, { pokemon ->
            if (pokemon != null) {
                displayPokemon(pokemon)
                showToast("Pokemon encontrado")
            } else {
                showToast("Error al obtener el Pokémon")
            }
        })

        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    if (lastLocation == null || location.distanceTo(lastLocation!!) >= 10) {
                        lastLocation = location
                        viewModel.fetchRandomPokemon()
                        vibrateDevice()
                    }
                }
            }
        }

        val locationRequest = createLocationRequest()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000 // Actualizar la ubicación cada 10 segundos
            fastestInterval = 5000 // La frecuencia más rápida para las actualizaciones de ubicación
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun vibrateDevice() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun displayPokemon(pokemon: Pokemon) {
        tvPokemonName.text = pokemon.name.capitalize()
        Picasso.get().load(pokemon.sprites.front_default).into(imgPokemon)

        // Actualizar el TextView con detalles del Pokémon (Justificar la descripción)
        tvPokemonDetails.apply {
            text = pokemon.description
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START // Justificar el texto
        }

        // Mostrar ID, altura y peso en el nuevo TextView (tvPokemonInfo)
        val pokemonInfo = "ID: ${pokemon.id}\nAltura: ${pokemon.height}\nPeso: ${pokemon.weight}"
        tvPokemonInfo.text = pokemonInfo
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.duration = Toast.LENGTH_SHORT
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 165)
        toast.show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}
