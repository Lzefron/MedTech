package com.medtrack.app.data.model

object MedicalDictionary {
    val tests = listOf(
        // Complete Blood Count (CBC)
        CanonicalTest(
            name = "Hemoglobin",
            category = "Hematology (CBC)",
            defaultUnit = "g/dL",
            normalLow = 13.0,
            normalHigh = 17.5,
            aliases = listOf("hb", "hgb", "total hemoglobin", "haemoglobin"),
            physiologicalMin = 3.0,
            physiologicalMax = 25.0,
            description = "Oxygen-carrying protein in red blood cells"
        ),
        CanonicalTest(
            name = "RBC Count",
            category = "Hematology (CBC)",
            defaultUnit = "million/mcL",
            normalLow = 4.5,
            normalHigh = 5.9,
            aliases = listOf("rbc", "red blood cell", "erythrocyte count", "total rbc"),
            physiologicalMin = 1.0,
            physiologicalMax = 10.0,
            description = "Total number of red blood cells in blood sample"
        ),
        CanonicalTest(
            name = "WBC Count",
            category = "Hematology (CBC)",
            defaultUnit = "cells/mcL",
            normalLow = 4000.0,
            normalHigh = 11000.0,
            aliases = listOf("wbc", "white blood cell", "total leucocyte count", "tlc", "leukocytes"),
            physiologicalMin = 500.0,
            physiologicalMax = 100000.0,
            description = "White blood cells fighting infections"
        ),
        CanonicalTest(
            name = "Platelet Count",
            category = "Hematology (CBC)",
            defaultUnit = "/mcL",
            normalLow = 150000.0,
            normalHigh = 450000.0,
            aliases = listOf("platelets", "plt", "thrombocyte count", "total platelets"),
            physiologicalMin = 5000.0,
            physiologicalMax = 2000000.0,
            description = "Essential for normal blood clotting"
        ),
        CanonicalTest(
            name = "Hematocrit (PCV)",
            category = "Hematology (CBC)",
            defaultUnit = "%",
            normalLow = 38.0,
            normalHigh = 50.0,
            aliases = listOf("pcv", "hct", "packed cell volume", "hematocrit"),
            physiologicalMin = 10.0,
            physiologicalMax = 75.0,
            description = "Proportion of red blood cells in your blood"
        ),
        CanonicalTest(
            name = "MCV",
            category = "Hematology (CBC)",
            defaultUnit = "fL",
            normalLow = 80.0,
            normalHigh = 100.0,
            aliases = listOf("mean corpuscular volume", "mcv"),
            physiologicalMin = 40.0,
            physiologicalMax = 150.0,
            description = "Average size of red blood cells"
        ),
        CanonicalTest(
            name = "MCH",
            category = "Hematology (CBC)",
            defaultUnit = "pg",
            normalLow = 27.0,
            normalHigh = 33.0,
            aliases = listOf("mean corpuscular hemoglobin", "mch"),
            physiologicalMin = 10.0,
            physiologicalMax = 50.0
        ),
        CanonicalTest(
            name = "MCHC",
            category = "Hematology (CBC)",
            defaultUnit = "g/dL",
            normalLow = 32.0,
            normalHigh = 36.0,
            aliases = listOf("mchc"),
            physiologicalMin = 20.0,
            physiologicalMax = 45.0
        ),
        CanonicalTest(
            name = "ESR",
            category = "Hematology (CBC)",
            defaultUnit = "mm/hr",
            normalLow = 0.0,
            normalHigh = 20.0,
            aliases = listOf("erythrocyte sedimentation rate", "esr-westergren", "sed rate"),
            physiologicalMin = 0.0,
            physiologicalMax = 200.0,
            description = "Nonspecific marker of inflammation"
        ),
        CanonicalTest(
            name = "Neutrophils",
            category = "Differential Count",
            defaultUnit = "%",
            normalLow = 40.0,
            normalHigh = 75.0,
            aliases = listOf("neutrophil", "polymorphs", "segs", "neut"),
            physiologicalMin = 0.0,
            physiologicalMax = 100.0
        ),
        CanonicalTest(
            name = "Lymphocytes",
            category = "Differential Count",
            defaultUnit = "%",
            normalLow = 20.0,
            normalHigh = 45.0,
            aliases = listOf("lymphocyte", "lymphs", "lymph"),
            physiologicalMin = 0.0,
            physiologicalMax = 100.0
        ),
        CanonicalTest(
            name = "Monocytes",
            category = "Differential Count",
            defaultUnit = "%",
            normalLow = 2.0,
            normalHigh = 10.0,
            aliases = listOf("monocyte", "mono"),
            physiologicalMin = 0.0,
            physiologicalMax = 50.0
        ),
        CanonicalTest(
            name = "Eosinophils",
            category = "Differential Count",
            defaultUnit = "%",
            normalLow = 1.0,
            normalHigh = 6.0,
            aliases = listOf("eosinophil", "eos"),
            physiologicalMin = 0.0,
            physiologicalMax = 60.0
        ),

        // Diabetes & Blood Glucose
        CanonicalTest(
            name = "Fasting Blood Glucose",
            category = "Diabetes / Glycemic",
            defaultUnit = "mg/dL",
            normalLow = 70.0,
            normalHigh = 99.0,
            aliases = listOf("fbs", "fbg", "fasting blood sugar", "glucose fasting", "sugar fasting"),
            physiologicalMin = 20.0,
            physiologicalMax = 1000.0,
            description = "Blood sugar level after an overnight fast"
        ),
        CanonicalTest(
            name = "Postprandial Blood Glucose (2h)",
            category = "Diabetes / Glycemic",
            defaultUnit = "mg/dL",
            normalLow = 70.0,
            normalHigh = 140.0,
            aliases = listOf("ppbs", "post prandial blood sugar", "2h ppbs", "glucose post prandial"),
            physiologicalMin = 30.0,
            physiologicalMax = 1000.0,
            description = "Blood sugar level 2 hours after a meal"
        ),
        CanonicalTest(
            name = "Random Blood Glucose",
            category = "Diabetes / Glycemic",
            defaultUnit = "mg/dL",
            normalLow = 70.0,
            normalHigh = 140.0,
            aliases = listOf("rbs", "random blood sugar", "glucose random"),
            physiologicalMin = 20.0,
            physiologicalMax = 1000.0
        ),
        CanonicalTest(
            name = "HbA1c",
            category = "Diabetes / Glycemic",
            defaultUnit = "%",
            normalLow = 4.0,
            normalHigh = 5.6,
            aliases = listOf("glycated hemoglobin", "glycosylated hemoglobin", "a1c", "hba1c test"),
            physiologicalMin = 3.0,
            physiologicalMax = 20.0,
            description = "Average blood sugar control over the past 3 months"
        ),

        // Renal / Kidney Function (KFT)
        CanonicalTest(
            name = "Serum Creatinine",
            category = "Renal Function (KFT)",
            defaultUnit = "mg/dL",
            normalLow = 0.7,
            normalHigh = 1.3,
            aliases = listOf("creatinine", "creat", "s. creatinine", "serum creat"),
            physiologicalMin = 0.1,
            physiologicalMax = 25.0,
            description = "Primary biomarker for glomerular filtration & kidney health"
        ),
        CanonicalTest(
            name = "Blood Urea Nitrogen (BUN)",
            category = "Renal Function (KFT)",
            defaultUnit = "mg/dL",
            normalLow = 7.0,
            normalHigh = 20.0,
            aliases = listOf("bun", "urea nitrogen"),
            physiologicalMin = 1.0,
            physiologicalMax = 200.0
        ),
        CanonicalTest(
            name = "Serum Urea",
            category = "Renal Function (KFT)",
            defaultUnit = "mg/dL",
            normalLow = 15.0,
            normalHigh = 45.0,
            aliases = listOf("urea", "blood urea", "s. urea"),
            physiologicalMin = 5.0,
            physiologicalMax = 350.0
        ),
        CanonicalTest(
            name = "Uric Acid",
            category = "Renal Function (KFT)",
            defaultUnit = "mg/dL",
            normalLow = 3.5,
            normalHigh = 7.2,
            aliases = listOf("serum uric acid", "s. uric acid"),
            physiologicalMin = 1.0,
            physiologicalMax = 25.0,
            description = "High levels associated with gout and kidney stones"
        ),
        CanonicalTest(
            name = "Serum Sodium",
            category = "Electrolytes",
            defaultUnit = "mmol/L",
            normalLow = 135.0,
            normalHigh = 145.0,
            aliases = listOf("na+", "sodium", "s. sodium"),
            physiologicalMin = 100.0,
            physiologicalMax = 180.0
        ),
        CanonicalTest(
            name = "Serum Potassium",
            category = "Electrolytes",
            defaultUnit = "mmol/L",
            normalLow = 3.5,
            normalHigh = 5.1,
            aliases = listOf("k+", "potassium", "s. potassium"),
            physiologicalMin = 1.5,
            physiologicalMax = 10.0
        ),

        // Lipid Profile
        CanonicalTest(
            name = "Total Cholesterol",
            category = "Lipid Profile",
            defaultUnit = "mg/dL",
            normalLow = 125.0,
            normalHigh = 200.0,
            aliases = listOf("cholesterol", "serum cholesterol", "s. cholesterol"),
            physiologicalMin = 50.0,
            physiologicalMax = 800.0,
            description = "Overall circulating blood cholesterol level"
        ),
        CanonicalTest(
            name = "Triglycerides",
            category = "Lipid Profile",
            defaultUnit = "mg/dL",
            normalLow = 50.0,
            normalHigh = 150.0,
            aliases = listOf("tg", "triglyceride", "s. triglycerides"),
            physiologicalMin = 20.0,
            physiologicalMax = 2000.0,
            description = "Type of blood fat stored in adipose tissue"
        ),
        CanonicalTest(
            name = "HDL Cholesterol",
            category = "Lipid Profile",
            defaultUnit = "mg/dL",
            normalLow = 40.0,
            normalHigh = 60.0,
            aliases = listOf("hdl", "hdl-c", "good cholesterol", "high density lipoprotein"),
            physiologicalMin = 10.0,
            physiologicalMax = 150.0,
            description = "Good cholesterol protecting against coronary disease"
        ),
        CanonicalTest(
            name = "LDL Cholesterol",
            category = "Lipid Profile",
            defaultUnit = "mg/dL",
            normalLow = 0.0,
            normalHigh = 100.0,
            aliases = listOf("ldl", "ldl-c", "bad cholesterol", "low density lipoprotein"),
            physiologicalMin = 10.0,
            physiologicalMax = 400.0,
            description = "Bad cholesterol contributing to arterial plaque"
        ),

        // Liver Function Tests (LFT)
        CanonicalTest(
            name = "SGPT (ALT)",
            category = "Liver Function (LFT)",
            defaultUnit = "U/L",
            normalLow = 7.0,
            normalHigh = 56.0,
            aliases = listOf("alt", "alanine aminotransferase", "sgpt", "s.g.p.t"),
            physiologicalMin = 1.0,
            physiologicalMax = 3000.0,
            description = "Enzyme found predominantly in liver cells"
        ),
        CanonicalTest(
            name = "SGOT (AST)",
            category = "Liver Function (LFT)",
            defaultUnit = "U/L",
            normalLow = 10.0,
            normalHigh = 40.0,
            aliases = listOf("ast", "aspartate aminotransferase", "sgot", "s.g.o.t"),
            physiologicalMin = 1.0,
            physiologicalMax = 3000.0
        ),
        CanonicalTest(
            name = "Total Bilirubin",
            category = "Liver Function (LFT)",
            defaultUnit = "mg/dL",
            normalLow = 0.2,
            normalHigh = 1.2,
            aliases = listOf("bilirubin total", "s. bilirubin total", "t. bilirubin"),
            physiologicalMin = 0.05,
            physiologicalMax = 40.0,
            description = "Breakdown product of hemoglobin processed by liver"
        ),
        CanonicalTest(
            name = "Alkaline Phosphatase (ALP)",
            category = "Liver Function (LFT)",
            defaultUnit = "U/L",
            normalLow = 44.0,
            normalHigh = 147.0,
            aliases = listOf("alp", "alk phos", "s. alkaline phosphatase"),
            physiologicalMin = 10.0,
            physiologicalMax = 1500.0
        ),

        // Thyroid Panel
        CanonicalTest(
            name = "TSH",
            category = "Thyroid Panel",
            defaultUnit = "mIU/L",
            normalLow = 0.4,
            normalHigh = 4.5,
            aliases = listOf("thyroid stimulating hormone", "s. tsh", "ultra tsh"),
            physiologicalMin = 0.01,
            physiologicalMax = 150.0,
            description = "Pituitary hormone controlling thyroid gland secretion"
        ),
        CanonicalTest(
            name = "Free T4",
            category = "Thyroid Panel",
            defaultUnit = "ng/dL",
            normalLow = 0.8,
            normalHigh = 1.8,
            aliases = listOf("ft4", "free thyroxine"),
            physiologicalMin = 0.1,
            physiologicalMax = 10.0
        ),
        CanonicalTest(
            name = "Free T3",
            category = "Thyroid Panel",
            defaultUnit = "pg/mL",
            normalLow = 2.3,
            normalHigh = 4.2,
            aliases = listOf("ft3", "free triiodothyronine"),
            physiologicalMin = 0.5,
            physiologicalMax = 25.0
        ),

        // Inflammatory & Vitamins
        CanonicalTest(
            name = "C-Reactive Protein (CRP)",
            category = "Inflammatory",
            defaultUnit = "mg/L",
            normalLow = 0.0,
            normalHigh = 5.0,
            aliases = listOf("crp", "s. crp", "c reactive protein"),
            physiologicalMin = 0.1,
            physiologicalMax = 400.0,
            description = "Acute phase inflammatory protein produced by liver"
        ),
        CanonicalTest(
            name = "Vitamin D (25-OH)",
            category = "Vitamins & Minerals",
            defaultUnit = "ng/mL",
            normalLow = 30.0,
            normalHigh = 100.0,
            aliases = listOf("vit d", "25-hydroxy vitamin d", "vitamin d3", "cholecalciferol"),
            physiologicalMin = 3.0,
            physiologicalMax = 200.0,
            description = "Essential for bone mineralization, calcium homeostasis, and immune health"
        ),
        CanonicalTest(
            name = "Vitamin B12",
            category = "Vitamins & Minerals",
            defaultUnit = "pg/mL",
            normalLow = 200.0,
            normalHigh = 900.0,
            aliases = listOf("vit b12", "b12", "cobalamin", "serum b12"),
            physiologicalMin = 20.0,
            physiologicalMax = 2500.0
        ),
        CanonicalTest(
            name = "Serum Ferritin",
            category = "Iron & Anemia",
            defaultUnit = "ng/mL",
            normalLow = 20.0,
            normalHigh = 250.0,
            aliases = listOf("ferritin", "s. ferritin"),
            physiologicalMin = 1.0,
            physiologicalMax = 5000.0,
            description = "Primary intracellular iron storage protein"
        )
    )

    fun findCanonical(query: String): CanonicalTest? {
        val clean = query.trim().lowercase()
        // Exact match on name
        tests.firstOrNull { it.name.lowercase() == clean }?.let { return it }
        // Alias exact match
        tests.firstOrNull { test -> test.aliases.any { it.lowercase() == clean } }?.let { return it }
        // Contains match
        tests.firstOrNull { test ->
            clean.contains(test.name.lowercase()) || test.aliases.any { clean.contains(it.lowercase()) }
        }?.let { return it }
        return null
    }

    fun search(query: String): List<CanonicalTest> {
        if (query.isBlank()) return tests
        val clean = query.trim().lowercase()
        return tests.filter { test ->
            test.name.lowercase().contains(clean) ||
            test.category.lowercase().contains(clean) ||
            test.aliases.any { it.lowercase().contains(clean) }
        }
    }
}
