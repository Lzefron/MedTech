# Reference ranges and units

Reviewed on 2026-09-22. ReferenceCatalog.kt contains a versioned, offline set of general adult examples researched from MedlinePlus (US National Library of Medicine). Opening a source link is optional; entering results does not upload patient data.

## How suggestions work

Every built-in numeric test starts with its common unit. Users can choose another listed unit or type one. Unit changes never convert the measured value. A range typed from a report is cleared when its unit changes, because the app cannot establish its meaning in another unit.

Choose **General reference**, then the adult group applicable at the time of the test. Male/female groups are required for RBC count, hemoglobin and hematocrit. The selected group is remembered within the current entry screen, so supported subsequent tests receive a default range. Pregnancy, children and unknown patient groups require the reporting lab's range or **Skip range**.

Suggested ranges carry their source, patient group and retrieval date into saved results, charts and PDFs. Typing over a range removes the general-source attribution. Skipping a range leaves the result unassessed. No existing result is rewritten during upgrade.

## Sources

| Tests | Source |
| --- | --- |
| Hemoglobin, RBC, WBC, hematocrit, MCV, MCH, MCHC | [CBC](https://medlineplus.gov/ency/article/003642.htm) |
| Platelets | [Platelet count](https://medlineplus.gov/ency/article/003647.htm) |
| Neutrophils, lymphocytes, monocytes, eosinophils (percentages only) | [Blood differential](https://medlineplus.gov/ency/article/003657.htm) |
| Fasting glucose, BUN, creatinine, sodium, potassium, ALT, AST, ALP, bilirubin | [Comprehensive metabolic panel](https://medlineplus.gov/ency/article/003468.htm) |
| TSH | [TSH](https://medlineplus.gov/ency/article/003684.htm) |

The dedicated platelet article is used for platelet units; the CBC summary displays an inconsistent denominator. Direct SI values from the chemistry source are retained rather than inventing molecular conversions. Additional exact conversions use metric scaling: g/dL to g/L (×10), percent to L/L (÷100), cells/µL to 10^9/L (÷1000), million/µL to 10^12/L (same number). Sodium and potassium are monovalent, so mmol/L and mEq/L have the same numerical value. TSH mIU/L and µIU/mL are equivalent. Unit aliases normalize µ, μ, u and mcL.

## Deliberate limits

There are no automatic generic ranges for custom tests, random or post-meal glucose, HbA1c, lipid targets, ESR, CRP, vitamins, ferritin, uric acid, serum urea or free thyroid hormones in this reviewed catalog. Interpretation can depend on testing method, treatment, fasting, age or other context. These tests still receive unit choices and accept the lab's printed range. Absolute differential counts cannot borrow percentage ranges.

General intervals are not diagnostic thresholds or treatment targets, and a result inside an interval does not guarantee health. Prefer the range printed by the reporting laboratory. [MedlinePlus explains why ranges differ](https://medlineplus.gov/lab-tests/how-to-understand-your-lab-results/).
