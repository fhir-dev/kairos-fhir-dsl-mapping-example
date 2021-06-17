package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue
import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/diabetesmellitus
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 *
 */


condition {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "SARS-Cov-2"){
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus == "OPEN") {
    return //no export
  }
  final def crfItemDiab = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_DIABETES" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemDiab){
    return //no export
  }
  if (crfItemDiab[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "DiabetesMellitus/" + context.source[studyVisitItem().crf().id()]

    meta {
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/diabetes-mellitus"
    }

    crfItemDiab[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def VERcode = matchResponseToVerificationStatus(item[CatalogEntry.CODE] as String)
      if (VERcode == "261665006") {
        extension {
          url = "https://simplifier.net/forschungsnetzcovid-19/uncertaintyofpresence"
          valueCodeableConcept {
            coding {
              system = "http://snomed.info/sct"
              code = "261665006"
            }
          }
        }
      }
      else if (["410594000","410605003"].contains(VERcode)){
        verificationStatus {
          coding {
            system = "http://snomed.info/sct"
            code = VERcode
          }
        }
      }
    }
    category {
      coding {
        system = "http://snomed.info/sct"
        code = "408475000"
      }
    }

    subject {
      reference = "Patient/" + context.source[studyVisitItem().studyMember().patientContainer().id()]
    }

    code {
      crfItemDiab[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
        final def ICDcode = matchResponseToICD(item[CatalogEntry.CODE] as String)
        if (ICDcode) {
          coding {
            system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
            version = "2020"
            code = ICDcode
          }
        }
      }
      crfItemDiab[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
        final def SNOMEDcode =  matchResponseToSNOMED(item[CatalogEntry.CODE] as String)
        if (SNOMEDcode) {
          coding {
            system = "http://snomed.info/sct"
            code = SNOMEDcode
          }
        }
      }
    }

    recordedDate {
      date = normalizeDate(crfItemDiab[CrfItem.CREATIONDATE] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }
  }
}


static String matchResponseToICD(final String resp) {
  switch (resp) {
    case ("COV_TYP1"):
      return "E10.9"
    default: null
  }
}

static String matchResponseToSNOMED(final String resp) {
  switch (resp) {
    case ("COV_TYP1"):
      return "46635009"
    case ("COV_TYP2_INSULIN"):
      return "237599002"
    case ("COV_TYP2_O_INSULIN"):
      return "44054006"
    case ("COV_TYP3"):
      return "8801005"
    case ("COV_UNBEKANNT"):
      return "73211009" //generic code for diabetes
    case ("COV_NEIN"):
      return "73211009" //generic code for diabetes
    default: null
  }
}

static String matchResponseToVerificationStatus(final String resp) {
  switch (resp) {
    case null:
      return null
    case ("COV_UNBEKANNT"):
      return "261665006"
    case ("COV_NEIN"):
      return "410594000"
    default: "410605003"
  }
}


static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}