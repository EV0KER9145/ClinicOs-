package com.clinicos.app.feature.dashboard

data class UpcomingAppointmentSample(
    val id: String,
    val time: String,
    val patientName: String,
    val doctorName: String,
    val status: String
)

object DashboardSampleData {
    const val clinicName = "SmileCare Clinic"
    const val greeting = "Good morning"
    const val appointmentsToday = 12
    const val newPatientsToday = 3
    const val overdueFollowups = 7
    const val noShows = 2

    val upcomingAppointments = listOf(
        UpcomingAppointmentSample(
            id = "1",
            time = "09:30 AM",
            patientName = "Rahul Sharma",
            doctorName = "Dr. Priya Mehta",
            status = "Confirmed"
        ),
        UpcomingAppointmentSample(
            id = "2",
            time = "10:15 AM",
            patientName = "Anita Kapoor",
            doctorName = "Dr. Raj Singh",
            status = "Scheduled"
        ),
        UpcomingAppointmentSample(
            id = "3",
            time = "11:00 AM",
            patientName = "Vikram Patel",
            doctorName = "Dr. Priya Mehta",
            status = "Confirmed"
        )
    )
}
