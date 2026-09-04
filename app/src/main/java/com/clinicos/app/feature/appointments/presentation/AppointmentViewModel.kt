package com.clinicos.app.feature.appointments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.CreateAppointmentRequest
import com.clinicos.app.core.network.dto.UpdateAppointmentRequest
import com.clinicos.app.feature.appointments.data.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AppointmentViewModel(
    private val repository: AppointmentRepository
) : ViewModel() {

    private val _appointmentsState = MutableStateFlow<AppointmentsListState>(AppointmentsListState.Loading)
    val appointmentsState: StateFlow<AppointmentsListState> = _appointmentsState.asStateFlow()

    private val _actionState = MutableStateFlow<AppointmentActionState>(AppointmentActionState.Idle)
    val actionState: StateFlow<AppointmentActionState> = _actionState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _doctorFilter = MutableStateFlow<String?>(null)
    val doctorFilter: StateFlow<String?> = _doctorFilter.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadAppointmentsForSelectedDate()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadAppointmentsForSelectedDate()
    }

    fun selectPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        loadAppointmentsForSelectedDate()
    }

    fun selectNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
        loadAppointmentsForSelectedDate()
    }

    fun selectToday() {
        _selectedDate.value = LocalDate.now()
        loadAppointmentsForSelectedDate()
    }

    fun filterByDoctor(doctorId: String?) {
        _doctorFilter.value = doctorId
        loadAppointmentsForSelectedDate()
    }

    fun loadAppointmentsForSelectedDate() {
        viewModelScope.launch {
            _appointmentsState.value = AppointmentsListState.Loading
            val dateStr = _selectedDate.value.format(dateFormatter)
            repository.getAppointments(
                page = 1,
                pageSize = 100,
                startDate = dateStr,
                endDate = dateStr,
                doctorId = _doctorFilter.value
            )
                .onSuccess { res -> _appointmentsState.value = AppointmentsListState.Success(res.items, res.total) }
                .onFailure { err -> _appointmentsState.value = AppointmentsListState.Error(err.message ?: "Failed to load appointments.") }
        }
    }

    fun bookAppointment(
        patientId: String,
        doctorId: String,
        scheduledAtIso: String,
        durationMinutes: Int = 30,
        notes: String?
    ) {
        viewModelScope.launch {
            _actionState.value = AppointmentActionState.Loading
            val req = CreateAppointmentRequest(
                patientId = patientId,
                doctorId = doctorId,
                scheduledAt = scheduledAtIso,
                durationMinutes = durationMinutes,
                notes = notes?.trim()?.ifEmpty { null }
            )

            repository.createAppointment(req)
                .onSuccess {
                    _actionState.value = AppointmentActionState.Success("Appointment booked successfully!")
                    loadAppointmentsForSelectedDate()
                }
                .onFailure { err ->
                    _actionState.value = AppointmentActionState.Error(err.message ?: "Failed to book appointment.")
                }
        }
    }

    fun updateAppointment(
        appointmentId: String,
        patientId: String?,
        doctorId: String?,
        scheduledAtIso: String?,
        durationMinutes: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            _actionState.value = AppointmentActionState.Loading
            val req = UpdateAppointmentRequest(
                patientId = patientId,
                doctorId = doctorId,
                scheduledAt = scheduledAtIso,
                durationMinutes = durationMinutes,
                notes = notes?.trim()?.ifEmpty { null }
            )

            repository.updateAppointment(appointmentId, req)
                .onSuccess {
                    _actionState.value = AppointmentActionState.Success("Appointment updated!")
                    loadAppointmentsForSelectedDate()
                }
                .onFailure { err ->
                    _actionState.value = AppointmentActionState.Error(err.message ?: "Failed to update appointment.")
                }
        }
    }

    fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        viewModelScope.launch {
            _actionState.value = AppointmentActionState.Loading
            repository.updateAppointmentStatus(appointmentId, newStatus)
                .onSuccess {
                    _actionState.value = AppointmentActionState.Success("Appointment status updated to $newStatus!")
                    loadAppointmentsForSelectedDate()
                }
                .onFailure { err ->
                    _actionState.value = AppointmentActionState.Error(err.message ?: "Failed to update status.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = AppointmentActionState.Idle
    }

    class Factory(private val repository: AppointmentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppointmentViewModel(repository) as T
        }
    }
}
