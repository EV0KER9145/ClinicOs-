package com.clinicos.app.feature.leads

data class LeadSample(
    val id: String,
    val name: String,
    val source: String,
    val status: String,
    val assignedTo: String,
    val recentActivity: String
)

object LeadsSampleData {
    val sampleLeads = listOf(
        LeadSample(
            id = "1",
            name = "Meera Reddy",
            source = "Google Search",
            status = "New",
            assignedTo = "Reception - Sunita",
            recentActivity = "10 mins ago"
        ),
        LeadSample(
            id = "2",
            name = "Karan Malhotra",
            source = "Walk-in Enquiry",
            status = "Contacted",
            assignedTo = "Dr. Priya Mehta",
            recentActivity = "1 hour ago"
        ),
        LeadSample(
            id = "3",
            name = "Deepak Joshi",
            source = "Instagram Ad",
            status = "Interested",
            assignedTo = "Reception - Sunita",
            recentActivity = "3 hours ago"
        ),
        LeadSample(
            id = "4",
            name = "Pooja Hegde",
            source = "Patient Referral",
            status = "New",
            assignedTo = "Dr. Raj Singh",
            recentActivity = "Yesterday"
        ),
        LeadSample(
            id = "5",
            name = "Amitabh Sen",
            source = "Website Form",
            status = "Contacted",
            assignedTo = "Reception - Sunita",
            recentActivity = "2 days ago"
        )
    )
}
