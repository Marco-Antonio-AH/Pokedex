package com.example.pokedexbueno

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class Pokemon(
    val id: Int,
    val name: String,
    val sprites: Sprites,
    val height: Int,
    val weight: Int,
    var description: String // Nueva propiedad para almacenar la descripción
)

data class Sprites(val front_default: String)
data class PokemonSpecies(val flavor_text_entries: List<FlavorTextEntry>)
data class FlavorTextEntry(val flavor_text: String, val language: Language)
data class Language(val name: String)

interface PokemonApiService {
    @GET("pokemon/{id}/")
    suspend fun getPokemon(@Path("id") id: Int): Pokemon

    @GET("pokemon-species/{id}/")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpecies
}

class PokemonViewModel : ViewModel() {

    private val _pokemonLiveData = MutableLiveData<Pokemon>()
    val pokemonLiveData: LiveData<Pokemon> get() = _pokemonLiveData

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val pokemonApiService = retrofit.create(PokemonApiService::class.java)

    fun fetchRandomPokemon() {
        viewModelScope.launch {
            try {
                val randomPokemonId = (1..898).random()
                val randomPokemon = withContext(Dispatchers.IO) {
                    pokemonApiService.getPokemon(randomPokemonId)
                }

                // Obtener la descripción del Pokémon
                val pokemonSpecies = withContext(Dispatchers.IO) {
                    pokemonApiService.getPokemonSpecies(randomPokemonId)
                }
                val description = extractEnglishDescription(pokemonSpecies)
                randomPokemon.description = description

                _pokemonLiveData.value = randomPokemon

            } catch (e: Exception) {
                _pokemonLiveData.value = null
            }
        }
    }

    private fun extractEnglishDescription(pokemonSpecies: PokemonSpecies): String {
        val englishFlavorTextEntry = pokemonSpecies.flavor_text_entries.find { entry ->
            entry.language.name == "es" // "en" representa el idioma inglés
        }
        return englishFlavorTextEntry?.flavor_text ?: "Descripción no disponible"
    }
}
