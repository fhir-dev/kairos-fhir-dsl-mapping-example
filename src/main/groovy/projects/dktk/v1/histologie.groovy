package projects.dktk.v1

import org.hl7.fhir.r4.model.Observation

/**
 * Represented by a CXX Histology
 * Specified by https://simplifier.net/oncology/histologie
 *
 * hints:
 * Reference to a single specimen is not clearly determinable, because in CXX the reference might be histology 1->n diagnosis/tumor 1->n sample.
 * Reference to hasMember is not available. There is no parent child hierarchy of histologies in CXX yet.
 *
 * @author Mike Wähnert
 * @since CXX v.3.17.0.11, v.3.17.1
 */
observation {
  id = "Observation/Histology-" + context.source["id"]

  meta {
    profile "http://dktk.dkfz.de/fhir/StructureDefinition/onco-core-Observation-Histologie"
  }

  status = Observation.ObservationStatus.UNKNOWN
  category {
    coding {
      system = "http://hl7.org/fhir/observation-category"
      code = "laboratory"
    }
  }
  code {
    coding {
      system = "http://loinc.org"
      code = "59847-4"
    }
  }

  subject {
    reference = "Patient/" + context.source["patientcontainer.id"]
  }

  if (context.source["episode"]) {
    encounter {
      reference = "Encounter/" + context.source["episode.id"]
    }
  }

  effectiveDateTime {
    date = normalizeDate(context.source["date"] as String)
  }

  if (context.source["icdEntry"]) {
    valueCodeableConcept {
      coding {
        system = "urn:oid:2.16.840.1.113883.6.43.1"
        version = "32"
        code = context.source["icdEntry.code"] as String
      }
    }
  }
}

/**
 * removes milli seconds and time zone.
 * @param dateTimeString the date time string
 * @return the result might be something like "1989-01-15T00:00:00"
 */
static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}
