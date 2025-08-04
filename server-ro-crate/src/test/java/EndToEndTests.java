import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import io.quarkus.runtime.Quarkus;
import org.junit.Ignore;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class EndToEndTests extends AbstractTest
{

    private static String username = "system";
    private static String password = "changeit";

    public static final String roCrateMetadataJsonExample1 =
            "{\"@context\": [\"https://w3id.org/ro/crate/1.1/context\", {\"Organization\": \"http://schema.org/Organization\", \"owl\": \"http://www.w3.org/2002/07/owl#\", \"xsd\": \"http://www.w3.org/2001/XMLSchema#\", \"CreativeWork\": \"http://schema.org/CreativeWork\", \"about\": \"http://schema.org/about\", \"description\": \"http://schema.org/description\", \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\", \"title\": \"http://schema.org/title\", \"dateCreated\": \"http://schema.org/dateCreated\", \"rangeIncludes\": \"http://schema.org/rangeIncludes\", \"creativeWorkStatus\": \"http://schema.org/creativeWorkStatus\", \"identifier\": \"http://schema.org/identifier\", \"creator\": \"http://schema.org/creator\", \"hasPart\": \"http://schema.org/hasPart\", \"Dataset\": \"http://schema.org/Dataset\", \"dateModified\": \"http://schema.org/dateModified\", \"abstract\": \"http://schema.org/abstract\", \"sdDatePublished\": \"http://schema.org/sdDatePublished\", \"domainIncludes\": \"http://schema.org/domainIncludes\", \"datePublished\": \"http://schema.org/datePublished\", \"license\": \"http://schema.org/license\", \"additionalType\": \"http://schema.org/additionalType\", \"name\": \"http://schema.org/name\", \"publisher\": \"http://schema.org/publisher\", \"conformsTo\": \"http://purl.org/dc/terms/conformsTo\", \"scicat\": \"_:scicat\", \"Person\": \"http://schema.org/Person\"}], \"@graph\": [{\"@id\": \"./\", \"@type\": \"Dataset\", \"name\": \"Micrometer-resolution X-ray tomographic imaging of a complete intact post mortem juvenile rat lung\", \"description\": \"In the associate article to these data sets, we present an X-ray tomographic imaging method that is well suited for pulmonary disease studies in animal models, to resolve the full pathway from gas intake to gas exchange. Current state-of-the-art synchrotron-based tomographic phase-contrast imaging methods allow for three-dimensional microscopic imaging data to be acquired non-destructively in scan times of the order of seconds with good soft tissue contrast. However, when studying multi-scale hierarchically structured objects, such as the mammalian lung, the overall sample size typically exceeds the field of view illuminated by the X-rays in a single scan, and the necessity for achieving a high spatial resolution conflicts with the need to image the whole sample. Several image-stitching and calibration techniques to achieve extended high-resolution fields of view have been reported, but those approaches tend to fail when imaging non-stable samples, thus precluding tomographic measurements of large biological samples, which are prone to degradation and motion during extended scan times. In this work, we demonstrate a full-volume three-dimensional reconstruction of an intact rat lung under immediate post mortem conditions and at an isotropic voxel size of (2.75 µm)^3. We present the methodology for collecting multiple local tomographies with 360 degree extended field of view scans followed by locally non-rigid volumetric stitching. Applied to the lung, it allows to resolve the entire pulmonary structure from the trachea down to the parenchyma in a single dataset.\", \"license\": {\"@id\": \"https://creativecommons.org/licenses/by-sa/4.0/\"}, \"datePublished\": \"2020\", \"hasPart\": [{\"@id\": \"_:scicatUser\"}, {\"@id\": \"https://doi.org/10.16907/7eb141d3-11f1-47a6-9d0e-76f8832ed1b2\"}, {\"@id\": \"_:sizeOfArchive\"}, {\"@id\": \"_:relatedPublications\"}, {\"@id\": \"_:PublishedData\"}, {\"@id\": \"_:numberOfFiles\"}]}, {\"about\": {\"@id\": \"./\"}, \"conformsTo\": {\"@id\": \"https://w3id.org/ro/crate/1.1\"}, \"@id\": \"ro-crate-metadata.json\", \"@type\": \"CreativeWork\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F7f8adb54-6abe-4c02-a1ad-c9b4a6d60f97\", \"name\": \"PSI-2018-R2-6_segmented_cropped_volume\", \"description\": \"Full-volume high-resolution x-ray tomographic microscopy reconstruction of a juvenile rat lung in fresh post mortem conditions; cropped to the maximum extent of the lung structure and segmented to separate air spaces from lung tissue\", \"contentSize\": \"302324856686\", \"@type\": \"Dataset\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F07f633c1-7b3d-4e0e-bdce-491d57defce3\", \"name\": \"data_final_volume_fullresolution/PSI-2018-R2-6_full_volume\", \"description\": \"Full-volume high-resolution x-ray tomographic microscopy reconstruction of a juvenile rat lung in fresh post mortem conditions\", \"contentSize\": \"1173381773872\", \"@type\": \"Dataset\"}, {\"@id\": \"_:scicatUser\", \"schema:rangeIncludes\": {\"@id\": \"xsd:string\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"identifier\": \"10.16907/7eb141d3-11f1-47a6-9d0e-76f8832ed1b2\", \"creator\": [{\"@id\": \"6680a65e-8134-46f9-8d97-c32908ddc31f\"}, {\"@id\": \"f1e13e18-f6be-474c-9ef0-5c071086bf16\"}, {\"@id\": \"fc648549-17f1-4e13-ba4d-0ff4bf4a2ff4\"}, {\"@id\": \"3a1954ae-888d-4601-b55a-d813735fdcde\"}, {\"@id\": \"c8e49375-404d-4f28-8cd8-8c7fda5b629f\"}, {\"@id\": \"7fe66453-2890-4c04-b985-75aaca401d1d\"}, {\"@id\": \"2e19c57d-ca62-4fa2-b8b0-a0a4e73db299\"}, {\"@id\": \"6a42c7af-3b91-4aff-8e12-0ace8c1e50f9\"}, {\"@id\": \"65714c93-5430-4e54-bdf7-6d3f4c0957b1\"}], \"datePublished\": \"2020\", \"title\": \"Micrometer-resolution X-ray tomographic imaging of a complete intact post mortem juvenile rat lung\", \"abstract\": \"In the associate article to these data sets, we present an X-ray tomographic imaging method that is well suited for pulmonary disease studies in animal models, to resolve the full pathway from gas intake to gas exchange. Current state-of-the-art synchrotron-based tomographic phase-contrast imaging methods allow for three-dimensional microscopic imaging data to be acquired non-destructively in scan times of the order of seconds with good soft tissue contrast. However, when studying multi-scale hierarchically structured objects, such as the mammalian lung, the overall sample size typically exceeds the field of view illuminated by the X-rays in a single scan, and the necessity for achieving a high spatial resolution conflicts with the need to image the whole sample. Several image-stitching and calibration techniques to achieve extended high-resolution fields of view have been reported, but those approaches tend to fail when imaging non-stable samples, thus precluding tomographic measurements of large biological samples, which are prone to degradation and motion during extended scan times. In this work, we demonstrate a full-volume three-dimensional reconstruction of an intact rat lung under immediate post mortem conditions and at an isotropic voxel size of (2.75 µm)^3. We present the methodology for collecting multiple local tomographies with 360 degree extended field of view scans followed by locally non-rigid volumetric stitching. Applied to the lung, it allows to resolve the entire pulmonary structure from the trachea down to the parenchyma in a single dataset.\", \"additionalType\": \"derived\", \"sdDatePublished\": \"2020-02-03T08:44:00.000Z\", \"creativeWorkStatus\": \"registered\", \"dateCreated\": \"2025-03-05T14:54:24.470Z\", \"description\": \"This published data collection contains three large volume datasets obtained by X-ray tomographic microscopy of the full juvenile rat lung structure at micrometer resolution. Data were collected and processed at the TOMCAT beamline X02DA of the Swiss Light Source. The first dataset contains the full scanned volume reconstruction (ca. 1.2 Tb), while a second one contains the same data but cropped down in size to the smallest bounding box encompassing the entire lung structure. The third dataset is a binarized version of the second one after thresholding operations to segment out the air volume of the lung.\", \"hasPart\": [{\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F904aa555-ac63-488e-9250-0fbeb6d9267c\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F7f8adb54-6abe-4c02-a1ad-c9b4a6d60f97\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F07f633c1-7b3d-4e0e-bdce-491d57defce3\"}], \"scicat:relatedPublications\": \"E. Borisova, G. Lovrić, A. Miettinen, L. Fardin, S. Bayat, A. Larsson, M. Stampanoni, J. C. Schittny, and C. M. Schlepütz. Micrometer-resolution X-ray tomographic full-volume reconstruction of an intact post-mortem juvenile rat lung. Histochem Cell Biol (2020). https://doi.org/10.1007/s00418-020-01868-8\", \"scicat:numberOfFiles\": 0, \"scicat:sizeOfArchive\": 0, \"scicat:scicatUser\": \"schlepuetz_c\", \"@id\": \"https://doi.org/10.16907/7eb141d3-11f1-47a6-9d0e-76f8832ed1b2\", \"@type\": [\"CreativeWork\", \"scicat:PublishedData\"]}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F904aa555-ac63-488e-9250-0fbeb6d9267c\", \"name\": \"PSI-2018-R2-6_cropped_volume\", \"description\": \"Full-volume high-resolution x-ray tomographic microscopy reconstruction of a juvenile rat lung in fresh post mortem conditions; cropped to the maximum extent of the lung structure\", \"contentSize\": \"604648500482\", \"@type\": \"Dataset\"}, {\"@id\": \"_:sizeOfArchive\", \"schema:rangeIncludes\": {\"@id\": \"xsd:integer\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"@id\": \"_:relatedPublications\", \"schema:rangeIncludes\": {\"@id\": \"xsd:string\"}, \"owl:equivalentProperty\": {\"@id\": \"datacite:relatedIdentifier\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"@id\": \"_:PublishedData\", \"rdfs:subClassOf\": {\"@id\": \"Thing\"}, \"@type\": \"rdfs:Class\"}, {\"@id\": \"_:numberOfFiles\", \"schema:rangeIncludes\": {\"@id\": \"xsd:integer\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"name\": \"Goran Lovric\", \"@id\": \"f1e13e18-f6be-474c-9ef0-5c071086bf16\", \"@type\": \"Person\"}, {\"name\": \"Elena Borisova\", \"@id\": \"6680a65e-8134-46f9-8d97-c32908ddc31f\", \"@type\": \"Person\"}, {\"name\": \"Anders Larsson\", \"@id\": \"7fe66453-2890-4c04-b985-75aaca401d1d\", \"@type\": \"Person\"}, {\"name\": \"Sam Bayat\", \"@id\": \"c8e49375-404d-4f28-8cd8-8c7fda5b629f\", \"@type\": \"Person\"}, {\"name\": \"Marco Stampanoni\", \"@id\": \"2e19c57d-ca62-4fa2-b8b0-a0a4e73db299\", \"@type\": \"Person\"}, {\"name\": \"Luca Fardin\", \"@id\": \"3a1954ae-888d-4601-b55a-d813735fdcde\", \"@type\": \"Person\"}, {\"name\": \"Christian M. Schlepütz\", \"@id\": \"65714c93-5430-4e54-bdf7-6d3f4c0957b1\", \"@type\": \"Person\"}, {\"name\": \"Johannes C. Schittny\", \"@id\": \"6a42c7af-3b91-4aff-8e12-0ace8c1e50f9\", \"@type\": \"Person\"}, {\"name\": \"Arttu Mietinen\", \"@id\": \"fc648549-17f1-4e13-ba4d-0ff4bf4a2ff4\", \"@type\": \"Person\"}]}\n";
    public static final String roCrateMetadataJsonExampleWrongDataType =
            "{\"@context\": [\"https://w3id.org/ro/crate/1.1/context\", {\"owl\": \"https://www.w3.org/TR/owl-ref/#\", \"datacite\": \"http://datacite.org/schema/kernel-4#\", \"xsd\": \"http://www.w3.org/2001/XMLSchema#\", \"rdfs\": \"https://www.w3.org/2000/01/rdf-schema#\", \"scicat\": \"_:\"}], \"@graph\": [{\"@id\": \"./\", \"@type\": \"Dataset\", \"name\": \"Micrometer-resolution X-ray tomographic imaging of a complete intact post mortem juvenile rat lung\", \"description\": \"In the associate article to these data sets, we present an X-ray tomographic imaging method that is well suited for pulmonary disease studies in animal models, to resolve the full pathway from gas intake to gas exchange. Current state-of-the-art synchrotron-based tomographic phase-contrast imaging methods allow for three-dimensional microscopic imaging data to be acquired non-destructively in scan times of the order of seconds with good soft tissue contrast. However, when studying multi-scale hierarchically structured objects, such as the mammalian lung, the overall sample size typically exceeds the field of view illuminated by the X-rays in a single scan, and the necessity for achieving a high spatial resolution conflicts with the need to image the whole sample. Several image-stitching and calibration techniques to achieve extended high-resolution fields of view have been reported, but those approaches tend to fail when imaging non-stable samples, thus precluding tomographic measurements of large biological samples, which are prone to degradation and motion during extended scan times. In this work, we demonstrate a full-volume three-dimensional reconstruction of an intact rat lung under immediate post mortem conditions and at an isotropic voxel size of (2.75 µm)^3. We present the methodology for collecting multiple local tomographies with 360 degree extended field of view scans followed by locally non-rigid volumetric stitching. Applied to the lung, it allows to resolve the entire pulmonary structure from the trachea down to the parenchyma in a single dataset.\", \"license\": {\"@id\": \"https://creativecommons.org/licenses/by-sa/4.0/\"}, \"datePublished\": \"2020\", \"hasPart\": [{\"@id\": \"_:scicatUser\"}, {\"@id\": \"https://doi.org/10.16907/7eb141d3-11f1-47a6-9d0e-76f8832ed1b2\"}, {\"@id\": \"_:sizeOfArchive\"}, {\"@id\": \"_:relatedPublications\"}, {\"@id\": \"_:PublishedData\"}, {\"@id\": \"_:numberOfFiles\"}]}, {\"about\": {\"@id\": \"./\"}, \"conformsTo\": {\"@id\": \"https://w3id.org/ro/crate/1.1\"}, \"@id\": \"ro-crate-metadata.json\", \"@type\": \"CreativeWork\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F7f8adb54-6abe-4c02-a1ad-c9b4a6d60f97\", \"name\": \"PSI-2018-R2-6_segmented_cropped_volume\", \"description\": \"Full-volume high-resolution x-ray tomographic microscopy reconstruction of a juvenile rat lung in fresh post mortem conditions; cropped to the maximum extent of the lung structure and segmented to separate air spaces from lung tissue\", \"contentSize\": \"302324856686\", \"@type\": \"Dataset\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F07f633c1-7b3d-4e0e-bdce-491d57defce3\", \"name\": \"data_final_volume_fullresolution/PSI-2018-R2-6_full_volume\", \"description\": \"Full-volume high-resolution x-ray tomographic microscopy reconstruction of a juvenile rat lung in fresh post mortem conditions\", \"contentSize\": \"1173381773872\", \"@type\": \"Dataset\"}, {\"@id\": \"_:scicatUser\", \"schema:rangeIncludes\": {\"@id\": \"xsd:string\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"identifier\": \"10.16907/7eb141d3-11f1-47a6-9d0e-76f8832ed1b2\", \"creator\": [{\"@id\": \"6680a65e-8134-46f9-8d97-c32908ddc31f\"}, {\"@id\": \"f1e13e18-f6be-474c-9ef0-5c071086bf16\"}, {\"@id\": \"fc648549-17f1-4e13-ba4d-0ff4bf4a2ff4\"}, {\"@id\": \"3a1954ae-888d-4601-b55a-d813735fdcde\"}, {\"@id\": \"c8e49375-404d-4f28-8cd8-8c7fda5b629f\"}, {\"@id\": \"7fe66453-2890-4c04-b985-75aaca401d1d\"}, {\"@id\": \"2e19c57d-ca62-4fa2-b8b0-a0a4e73db299\"}, {\"@id\": \"6a42c7af-3b91-4aff-8e12-0ace8c1e50f9\"}, {\"@id\": \"65714c93-5430-4e54-bdf7-6d3f4c0957b1\"}], \"publisher\": {\"@id\": \"https://ror.org/03eh3y714\"}, \"datePublished\": \"2020\", \"title\": \"Micrometer-resolution X-ray tomographic imaging of a complete intact post mortem juvenile rat lung\", \"abstract\": \"In the associate article to these data sets, we present an X-ray tomographic imaging method that is well suited for pulmonary disease studies in animal models, to resolve the full pathway from gas intake to gas exchange. Current state-of-the-art synchrotron-based tomographic phase-contrast imaging methods allow for three-dimensional microscopic imaging data to be acquired non-destructively in scan times of the order of seconds with good soft tissue contrast. However, when studying multi-scale hierarchically structured objects, such as the mammalian lung, the overall sample size typically exceeds the field of view illuminated by the X-rays in a single scan, and the necessity for achieving a high spatial resolution conflicts with the need to image the whole sample. Several image-stitching and calibration techniques to achieve extended high-resolution fields of view have been reported, but those approaches tend to fail when imaging non-stable samples, thus precluding tomographic measurements of large biological samples, which are prone to degradation and motion during extended scan times. In this work, we demonstrate a full-volume three-dimensional reconstruction of an intact rat lung under immediate post mortem conditions and at an isotropic voxel size of (2.75 µm)^3. We present the methodology for collecting multiple local tomographies with 360 degree extended field of view scans followed by locally non-rigid volumetric stitching. Applied to the lung, it allows to resolve the entire pulmonary structure from the trachea down to the parenchyma in a single dataset.\", \"additionalType\": \"derived\", \"sdDatePublished\": \"2020-02-03T08:44:00.000Z\", \"creativeWorkStatus\": \"registered\", \"dateCreated\": \"2025-03-05T14:54:24.470Z\", \"description\": \"This published data collection contains three large volume datasets obtained by X-ray tomographic microscopy of the full juvenile rat lung structure at micrometer resolution. Data were collected and processed at the TOMCAT beamline X02DA of the Swiss Light Source. The first dataset contains the full scanned volume reconstruction (ca. 1.2 Tb), while a second one contains the same data but cropped down in size to the smallest bounding box encompassing the entire lung structure. The third dataset is a binarized version of the second one after thresholding operations to segment out the air volume of the lung.\", \"hasPart\": [{\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F904aa555-ac63-488e-9250-0fbeb6d9267c\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F7f8adb54-6abe-4c02-a1ad-c9b4a6d60f97\"}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F07f633c1-7b3d-4e0e-bdce-491d57defce3\"}], \"scicat:relatedPublications\": \"E. Borisova, G. Lovrić, A. Miettinen, L. Fardin, S. Bayat, A. Larsson, M. Stampanoni, J. C. Schittny, and C. M. Schlepütz. Micrometer-resolution X-ray tomographic full-volume reconstruction of an intact post-mortem juvenile rat lung. Histochem Cell Biol (2020). https://doi.org/10.1007/s00418-020-01868-8\", \"scicat:numberOfFiles\": \"aaaa\", \"scicat:sizeOfArchive\": 0, \"scicat:scicatUser\": 1, \"yesHahaha\": \"yes\", \"@id\": \"https://doi.org/10.16907/7eb141d3-11f1-47a6-9d0e-76f8832ed1b2\", \"@type\": [\"CreativeWork\", \"scicat:PublishedData\"]}, {\"@id\": \"https://dacat.psi.ch/api/v3/datasets/20.500.11935%2F904aa555-ac63-488e-9250-0fbeb6d9267c\", \"name\": \"PSI-2018-R2-6_cropped_volume\", \"description\": \"Full-volume high-resolution x-ray tomographic microscopy reconstruction of a juvenile rat lung in fresh post mortem conditions; cropped to the maximum extent of the lung structure\", \"contentSize\": \"604648500482\", \"@type\": \"Dataset\"}, {\"@id\": \"_:sizeOfArchive\", \"schema:rangeIncludes\": {\"@id\": \"xsd:integer\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"@id\": \"_:relatedPublications\", \"schema:rangeIncludes\": {\"@id\": \"xsd:string\"}, \"owl:equivalentProperty\": {\"@id\": \"datacite:relatedIdentifier\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"@id\": \"_:PublishedData\", \"rdfs:subClassOf\": {\"@id\": \"Thing\"}, \"@type\": \"rdfs:Class\"}, {\"@id\": \"_:numberOfFiles\", \"schema:rangeIncludes\": {\"@id\": \"xsd:integer\"}, \"schema:domainIncludes\": {\"@id\": \"scicat:PublishedData\"}, \"@type\": \"rdfs:Property\"}, {\"name\": \"Goran Lovric\", \"@id\": \"f1e13e18-f6be-474c-9ef0-5c071086bf16\", \"@type\": \"Person\"}, {\"name\": \"Elena Borisova\", \"@id\": \"6680a65e-8134-46f9-8d97-c32908ddc31f\", \"@type\": \"Person\"}, {\"name\": \"Anders Larsson\", \"@id\": \"7fe66453-2890-4c04-b985-75aaca401d1d\", \"@type\": \"Person\"}, {\"name\": \"Sam Bayat\", \"@id\": \"c8e49375-404d-4f28-8cd8-8c7fda5b629f\", \"@type\": \"Person\"}, {\"name\": \"Marco Stampanoni\", \"@id\": \"2e19c57d-ca62-4fa2-b8b0-a0a4e73db299\", \"@type\": \"Person\"}, {\"name\": \"Luca Fardin\", \"@id\": \"3a1954ae-888d-4601-b55a-d813735fdcde\", \"@type\": \"Person\"}, {\"name\": \"Christian M. Schlepütz\", \"@id\": \"65714c93-5430-4e54-bdf7-6d3f4c0957b1\", \"@type\": \"Person\"}, {\"name\": \"Johannes C. Schittny\", \"@id\": \"6a42c7af-3b91-4aff-8e12-0ace8c1e50f9\", \"@type\": \"Person\"}, {\"name\": \"Arttu Mietinen\", \"@id\": \"fc648549-17f1-4e13-ba4d-0ff4bf4a2ff4\", \"@type\": \"Person\"}]}\n";
    @BeforeClass
    public void startQuarkus() {
        Quarkus.run(StartupMain.class, new String[] { "src/main/resources/service.properties" });
    }

    @Test
    public void testTestEcho()
            throws Exception
    {
        getConfiguration();

        given()
                .param("message", "Hello World")
                .when().get("http://localhost:8085/openbis/open-api/ro-crate/test-echo")
                .then()
                .body(is("Hello World"))
                .statusCode(200);
    }

    @Test
    public void testTestOpenbisConnection()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .param("api-key", openBIS.getSessionToken())
                .when().get("http://localhost:8085/openbis/open-api/ro-crate/test-openbis-connection")
                .then()
                .body(is(username))
                .statusCode(200);
    }

    @Test
    public void testImport()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .body(roCrateMetadataJsonExample1.getBytes())
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/import")
                .then()
                .statusCode(200);
    }

    @Test
    public void testValidate()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        String expected = "{\"isValid\":true}";
        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .body(roCrateMetadataJsonExample1.getBytes())
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[]"),
                        containsString("\"isValid\":true")))
                .statusCode(200);
    }

    @Test
    public void testValidateUnknwon()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        String expected = "{\"isValid\":true}";
        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .body(roCrateMetadataJsonExampleWrongDataType.getBytes())
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[{"),
                        containsString("\"isValid\":true"), containsString("NUMBEROFFILES")))
                .statusCode(200);
    }


    @Test
    public void testExportDOI()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .body("[\"https://doi.org/10.1038/s41586-020-3010-5\"]")
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(200);
    }

    @Test
    public void testExportIdentifier()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .body("[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]")
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(200);
    }

    @Test
    @Ignore
    // PermIds depend on when the import was done. This can lead to false failure.
    // As long as we don't have a good solution for search in tests, this is disabled.
    public void testExportPermId()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .body("[\"20250728111931402-94\"]")
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(200);
    }

    @Test
    public void testExportEmptyResults()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .body("[\"NOT-AN-IDENTIFIER\"]")
                .when().post("http://localhost:8085/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(404);
    }
}
