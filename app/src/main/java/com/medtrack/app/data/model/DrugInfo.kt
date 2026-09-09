package com.medtrack.app.data.model

data class DrugReference(
    val genericName: String,
    val brandNames: List<String>,
    val indication: String,
    val standardDose: String,
    val mealAdvice: String,
    val precautions: String
)

object DrugDatabase {
    val commonDrugs = listOf(
        DrugReference(
            genericName = "Paracetamol (Acetaminophen)",
            brandNames = listOf("Napa", "Ace", "Tylenol", "Panadol", "Renova"),
            indication = "Fever and mild to moderate pain relief",
            standardDose = "500 mg - 1000 mg every 4-6 hours (max 4g/day)",
            mealAdvice = "With or without food",
            precautions = "Avoid taking multiple medications containing paracetamol to prevent liver injury."
        ),
        DrugReference(
            genericName = "Montelukast",
            brandNames = listOf("Monas", "Montene", "Singulair", "M-Kast", "Odmon"),
            indication = "Bronchial asthma and allergic rhinitis prophylaxis",
            standardDose = "10 mg once daily at bedtime",
            mealAdvice = "Take at night, with or without food",
            precautions = "Not intended to treat sudden severe asthma attacks; take regularly as prescribed."
        ),
        DrugReference(
            genericName = "Metformin",
            brandNames = listOf("Glucophage", "Comet", "Daomet", "Glumet"),
            indication = "Type 2 Diabetes Mellitus glycemic control",
            standardDose = "500 mg - 1000 mg twice daily",
            mealAdvice = "Take with or immediately after meals",
            precautions = "Taking with food reduces stomach upset and gastrointestinal discomfort."
        ),
        DrugReference(
            genericName = "Amlodipine",
            brandNames = listOf("Camlodin", "Amdocal", "Norvasc", "Amlopin"),
            indication = "Hypertension (high blood pressure) and angina",
            standardDose = "5 mg - 10 mg once daily",
            mealAdvice = "Same time each day, before or after food",
            precautions = "Watch for pedal/ankle swelling or dizziness when standing up quickly."
        ),
        DrugReference(
            genericName = "Losartan Potassium",
            brandNames = listOf("Angilock", "Osartil", "Cozaar", "Prosan"),
            indication = "Hypertension and kidney protection in diabetics",
            standardDose = "25 mg - 50 mg once daily",
            mealAdvice = "Consistent time daily, with or without food",
            precautions = "Periodically monitor serum potassium and renal function."
        ),
        DrugReference(
            genericName = "Omeprazole / Esomeprazole",
            brandNames = listOf("Seclo", "Maxpro", "Nexum", "Prilosec", "Sergel"),
            indication = "Gastric acidity, GERD, and peptic ulcer disease",
            standardDose = "20 mg - 40 mg once daily",
            mealAdvice = "30 to 60 minutes before breakfast on an empty stomach",
            precautions = "Swallow whole; do not crush or chew delayed-release capsules."
        ),
        DrugReference(
            genericName = "Atorvastatin",
            brandNames = listOf("Lipitor", "Atova", "Tiginor", "Storvas"),
            indication = "Hypercholesterolemia and cardiovascular risk reduction",
            standardDose = "10 mg - 40 mg once daily at night",
            mealAdvice = "Preferably in the evening, with or without food",
            precautions = "Report unexplained muscle pain or weakness to your physician promptly."
        ),
        DrugReference(
            genericName = "Azithromycin",
            brandNames = listOf("Zithrin", "Tridosil", "Zithromax", "Azyth"),
            indication = "Bacterial respiratory, skin, and soft tissue infections",
            standardDose = "500 mg once daily for 3 to 5 days",
            mealAdvice = "1 hour before or 2 hours after food for optimal absorption",
            precautions = "Always complete the full prescribed antibiotic course even if feeling better."
        ),
        DrugReference(
            genericName = "Cholecalciferol (Vitamin D3)",
            brandNames = listOf("D-Rise", "Bio-D", "Calbo-D", "Ostocal"),
            indication = "Vitamin D deficiency and bone mineral density support",
            standardDose = "2000 IU daily or 40000 IU weekly",
            mealAdvice = "Take with a fat-containing meal for maximum absorption",
            precautions = "Fat-soluble vitamin; do not exceed recommended dosage without lab testing."
        )
    )

    fun searchDrug(query: String): List<DrugReference> {
        if (query.isBlank()) return commonDrugs
        val clean = query.trim().lowercase()
        return commonDrugs.filter { drug ->
            drug.genericName.lowercase().contains(clean) ||
            drug.brandNames.any { it.lowercase().contains(clean) } ||
            drug.indication.lowercase().contains(clean)
        }
    }
}
