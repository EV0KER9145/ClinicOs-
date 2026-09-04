package com.clinicos.app.feature.patients

data class PatientSample(
    val id: String,
    val name: String,
    val phone: String,
    val lastVisit: String,
    val genderAge: String
)

object PatientsSampleData {
    val samplePatients = listOf(
        PatientSample(
            id = "1",
            name = "Rahul Sharma",
            phone = "+91 98765 43210",
            lastVisit = "Last visit: 2 days ago",
            genderAge = "Male, 34 yrs"
        ),
        PatientSample(
            id = "2",
            name = "Anita Kapoor",
            phone = "+91 99887 77665",
            lastVisit = "Last visit: Yesterday",
            genderAge = "Female, 28 yrs"
        ),
        PatientSample(
            id = "3",
            name = "Vikram Patel",
            phone = "+91 91234 56789",
            lastVisit = "Last visit: 1 week ago",
            genderAge = "Male, 45 yrs"
        ),
        PatientSample(
            id = "4",
            name = "Priya Verma",
            phone = "+91 98112 23344",
            lastVisit = "Last visit: 3 weeks ago",
            genderAge = "Female, 31 yrs"
        ),
        PatientSample(
            id = "5",
            name = "Suresh Kumar",
            phone = "+91 97654 32109",
            lastVisit = "Last visit: Today",
            genderAge = "Male, 52 yrs"
        ),
        PatientSample(
            id = "6",
            name = "Sunita Rao",
            phone = "+91 94321 09876",
            lastVisit = "Last visit: 1 month ago",
            genderAge = "Female, 39 yrs"
        )
    )
}
