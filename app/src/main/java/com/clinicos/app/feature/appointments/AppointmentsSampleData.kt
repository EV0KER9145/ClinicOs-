package com.clinicos.app.feature.appointments

data class AppointmentSample(
    val id: String,
    val time: String,
    val patientName: String,
    val doctorName: String,
    val status: String,
    val dateLabel: String
)

object AppointmentsSampleData {
    val sampleAppointments = listOf(
        AppointmentSample(
            id = "1",
            time = "09:30 AM",
            patientName = "Rahul Sharma",
            doctorName = "Dr. Priya Mehta",
            status = "Confirmed",
            dateLabel = "Today, Sep 3"
        ),
        AppointmentSample(
            id = "2",
            time = "10:15 AM",
            patientName = "Anita Kapoor",
            doctorName = "Dr. Raj Singh",
            status = "Scheduled",
            dateLabel = "Today, Sep 3"
        ),
        AppointmentSample(
            id = "3",
            time = "11:00 AM",
            patientName = "Vikram Patel",
            doctorName = "Dr. Priya Mehta",
            status = "Confirmed",
            dateLabel = "Today, Sep 3"
        ),
        AppointmentSample(
            id = "4",
            time = "02:00 PM",
            patientName = "Sunita Rao",
            doctorName = "Dr. Raj Singh",
            status = "Scheduled",
            dateLabel = "Today, Sep 3"
        ),
        AppointmentSample(
            id = "5",
            time = "03:30 PM",
            patientName = "Amit Kumar",
            doctorName = "Dr. Priya Mehta",
            status = "Completed",
            dateLabel = "Today, Sep 3"
        )
    )
}
