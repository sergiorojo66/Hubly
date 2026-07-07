package com.example.rankup.ui.createEventScreen

import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateEventViewModelTest {

    private lateinit var viewModel: CreateEventViewModel

    // Simulamos las dependencias necesarias para que el ViewModel se cree sin fallar
    private val eventRepository: EventRepository = mockk(relaxed = true)
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)

    @Before
    fun setUp() {
        // Inicializamos el ViewModel antes de cada test con los mocks vacíos
        viewModel = CreateEventViewModel(eventRepository, firebaseAuth)
    }

    @Test
    fun `formulario invalido si el titulo es demasiado corto`() {
        // Configurando un escenario con título de solo 2 caracteres (el mínimo es 3)
        viewModel.onTitleChange("To")
        viewModel.onLocationChange("Polideportivo Pisuerga")
        viewModel.togglePrivacy(false)

        // Verificación: isFormValid debe devolver false
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun `formulario valido si todos los campos cumplen los requisitos obligatorios`() {
        // Configurando un escenario con datos correctos
        viewModel.onTitleChange("Torneo de Pádel Hubly")
        viewModel.onLocationChange("Pistas Municipales de Palencia")
        viewModel.togglePrivacy(false)

        // Verificación: isFormValid debe devolver true
        assertTrue(viewModel.isFormValid)
    }

    @Test
    fun `formulario invalido si el evento es privado y la contrasena es demasiado corta`() {
        // Configurando evento privado con contraseña de 2 dígitos (el mínimo son 4)
        viewModel.onTitleChange("Quedada Juegos de Mesa")
        viewModel.onLocationChange("Campus Universitario")
        viewModel.togglePrivacy(true)
        viewModel.onPasswordChange("12")

        // Verificación: isFormValid debe ser false debido a la contraseña insegura
        assertFalse(viewModel.isFormValid)
    }
}