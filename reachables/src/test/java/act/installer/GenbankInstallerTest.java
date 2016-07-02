package act.installer;

import act.server.MongoDB;
import act.shared.Reaction;
import act.shared.Seq;
import act.shared.helpers.MongoDBToJSON;
import com.act.biointerpretation.test.util.MockedMongoDBAPI;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Before;
import org.junit.Test;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class GenbankInstallerTest {

  MockedMongoDBAPI mockAPI;

  String seq = "MMTNLQKEFFKRLKIPAKEITFNDLDEILLKMGLTLPYENLDIMAGTIKDISKNNLVEKILIQKRGGLCYELNSLLYYFLMDCGFQVYK" +
      "VAGTVYDLYDNKWKPDDGHVIIVLTHNNKDYVIDAGFASHLPLHPVPFNGEVISSQTGEYRIRKRTTRKGTHILEMRKGANGESTNFLQSEPSHEWKV" +
      "GYAFTLDPIDEKKVNNIQKVIVEHKESPFNKGAITCKLTDYGHVSLTNKNYTETFKGTKNKRPIESKDYAHILRESFGITQVKYVGKTLERG";

  String seq2 = "MELIQDTSRPPLEYVKGVPLIKYFAEALGPLQSFQARPDDLLISTYPKSGTTWVSQILDMIYQGGDLEKCHRAPIFMRVPFLEFKAPG" +
      "IPSGMETLKDTPAPRLLKTHLPLALLPQTLLDQKVKVVYVARNAKDVAVSYYHFYHMAKVHPEPGTWDSFLEKFMVGEVSYGSWYQHVQEWWELSRTH" +
      "PVLYLFYEDMKENPKREIQKILEFVGRSLPEETVDFVVQHTSFKEMKKNPMTNYTTVPQEFMDHSISPFMRKGMAGDWKTTFTVAQNERFDADYAEKM" +
      "AGCSLSFRSEL";

  String seq3 = "MMTNLQKEFFKRLKIPAKEITFNDLDEILLKMGLTLPYENLDIMAGTIKDISKNNLVEKILIQKRGGLCYELNSLLYYFLMDCGFQVYK" +
      "VAGTVYDLYDNKWKPDDGHVIIVLTHNNKDYVIDAGFASHLPLHPVPFNGEVISSQTGEYRIRKRTTRKGT";

  @Before
  public void setUp() throws Exception {

    JSONObject metadata = new JSONObject();
    metadata.put("accession", Arrays.asList("CUB13083"));
    metadata.put("accession_sources", Arrays.asList("genbank"));

    Seq emptyTestSeq = new Seq(91973L, "2.3.1.5", 4000000648L, "Bacillus cereus", seq, new ArrayList<>(), MongoDBToJSON.conv(metadata), Seq.AccDB.genbank);

    metadata.remove("accession");
    metadata.put("accession", Arrays.asList("P50225"));

    Seq emptyTestSeq2 = new Seq(29034L, "2.8.2.1", 4000002681L, "Homo sapiens", seq2, new ArrayList<>(), MongoDBToJSON.conv(metadata), Seq.AccDB.genbank);

    metadata = new JSONObject();
    metadata.put("accession", Arrays.asList("NUR84963"));
    metadata.put("accession_sources", Arrays.asList("genbank"));
    metadata.put("synonyms", Arrays.asList("STP", "STP1", "ST1A1"));
    metadata.put("product_names", Arrays.asList("Sulfotransferase 1A1"));
    metadata.put("name", "SULT1A1");

    List<JSONObject> references = new ArrayList<>();

    List<String> pmids = Arrays.asList("8363592", "8484775", "8423770", "8033246", "7864863", "7695643", "7581483", "8912648", "8924211", "9855620");

    for (String pmid : pmids) {
      JSONObject obj = new JSONObject();
      obj.put("src", "PMID");
      obj.put("val", pmid);
      references.add(obj);
    }

    JSONObject ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "JP");
    ref_obj.put("patent_number", "2008518610");
    ref_obj.put("patent_year", "2008");
    references.add(ref_obj);

    ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "EP");
    ref_obj.put("patent_number", "2904117");
    ref_obj.put("patent_year", "2015");
    references.add(ref_obj);

    ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "EP");
    ref_obj.put("patent_number", "1731531");
    ref_obj.put("patent_year", "2006");
    references.add(ref_obj);

    Seq fullTestSeq = new Seq(2893740L, "2.4.1.8", 4000006340L, "Thermus sp.", seq3, references, MongoDBToJSON.conv(metadata), Seq.AccDB.genbank);


    mockAPI = new MockedMongoDBAPI();

    mockAPI.installMocks(new ArrayList<Reaction>(), Arrays.asList(emptyTestSeq, emptyTestSeq2, fullTestSeq), new HashMap<>(), new HashMap<>());

    MongoDB mockDb = mockAPI.getMockMongoDB();

    GenbankInstaller genbankInstaller = new GenbankInstaller(new File(this.getClass().getResource("genbank_installer_test_null_protein.gb").getFile()), "Protein", mockDb);
    genbankInstaller.init();

    GenbankInstaller genbankInstaller2 = new GenbankInstaller(new File(this.getClass().getResource("genbank_installer_test_full_protein.gb").getFile()), "Protein", mockDb);
    genbankInstaller2.init();

    GenbankInstaller genbankInstaller3 = new GenbankInstaller(new File(this.getClass().getResource("genbank_installer_test_null_protein_2.gb").getFile()), "Protein", mockDb);
    genbankInstaller3.init();
    
  }

  @Test
  public void testNullNull() {

    JSONObject metadata = new JSONObject();
    metadata.put("accession", Arrays.asList("CUB13083"));
    metadata.put("accession_sources", Arrays.asList("genbank"));

    Map<Long, Seq> seqs = mockAPI.getSeqMap();
    Seq emptyTestSeq = new Seq(91973L, "2.3.1.5", 4000000648L, "Bacillus cereus", seq, new ArrayList<>(), MongoDBToJSON.conv(metadata), Seq.AccDB.genbank);

    compareSeqs(emptyTestSeq, seqs.get(91973L));

  }

  @Test
  public void testNullFull() {

    JSONObject metadata = new JSONObject();
    metadata.put("accession", Arrays.asList("P50225"));
    metadata.put("accession_sources", Arrays.asList("genbank"));
    metadata.put("synonyms", Arrays.asList("STP", "STP1", "ST1A1"));
    metadata.put("product_names", Arrays.asList("Sulfotransferase 1A1"));
    metadata.put("name", "SULT1A1");

    Map<Long, Seq> seqs = mockAPI.getSeqMap();

    List<JSONObject> references = new ArrayList<>();

    List<String> pmids = Arrays.asList("8363592", "8484775", "8423770", "8033246", "7864863", "7695643", "7581483", "8912648", "8924211", "9855620");

    for (String pmid : pmids) {
      JSONObject obj = new JSONObject();
      obj.put("src", "PMID");
      obj.put("val", pmid);
      references.add(obj);
    }

    JSONObject ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "JP");
    ref_obj.put("patent_number", "2008518610");
    ref_obj.put("patent_year", "2008");
    references.add(ref_obj);

    ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "EP");
    ref_obj.put("patent_number", "2904117");
    ref_obj.put("patent_year", "2015");
    references.add(ref_obj);

    ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "EP");
    ref_obj.put("patent_number", "1731531");
    ref_obj.put("patent_year", "2006");
    references.add(ref_obj);

    Seq testSeq = new Seq(29034L, "2.8.2.1", 4000002681L, "Homo sapiens", seq2, references, MongoDBToJSON.conv(metadata), Seq.AccDB.genbank);

    compareSeqs(testSeq, seqs.get(29034L));

  }

  @Test
  public void testFullNull() {
    JSONObject metadata = new JSONObject();
    metadata.put("accession", Arrays.asList("NUR84963"));
    metadata.put("accession_sources", Arrays.asList("genbank"));
    metadata.put("synonyms", Arrays.asList("STP", "STP1", "ST1A1"));
    metadata.put("product_names", Arrays.asList("Sulfotransferase 1A1"));
    metadata.put("name", "SULT1A1");

    Map<Long, Seq> seqs = mockAPI.getSeqMap();

    List<JSONObject> references = new ArrayList<>();

    List<String> pmids = Arrays.asList("8363592", "8484775", "8423770", "8033246", "7864863", "7695643", "7581483", "8912648", "8924211", "9855620");

    for (String pmid : pmids) {
      JSONObject obj = new JSONObject();
      obj.put("src", "PMID");
      obj.put("val", pmid);
      references.add(obj);
    }

    JSONObject ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "JP");
    ref_obj.put("patent_number", "2008518610");
    ref_obj.put("patent_year", "2008");
    references.add(ref_obj);

    ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "EP");
    ref_obj.put("patent_number", "2904117");
    ref_obj.put("patent_year", "2015");
    references.add(ref_obj);

    ref_obj = new JSONObject();
    ref_obj.put("src", "Patent");
    ref_obj.put("country_code", "EP");
    ref_obj.put("patent_number", "1731531");
    ref_obj.put("patent_year", "2006");
    references.add(ref_obj);

    Seq fullTestSeq = new Seq(2893740L, "2.4.1.8", 4000006340L, "Thermus sp.", seq3, references, MongoDBToJSON.conv(metadata), Seq.AccDB.genbank);

    compareSeqs(fullTestSeq, seqs.get(2893740L));
  }

  public void compareSeqs(Seq expectedSeq, Seq testSeq) {
    assertEquals("comparing id", expectedSeq.getUUID(), testSeq.getUUID());
    assertEquals("comparing ec", expectedSeq.get_ec(), testSeq.get_ec());
    assertEquals("comparing org_id", expectedSeq.getOrgId(), testSeq.getOrgId());
    assertEquals("comparing organism", expectedSeq.get_org_name(), testSeq.get_org_name());
    assertEquals("comparing sequence", expectedSeq.get_sequence(), testSeq.get_sequence());
    assertEquals("comparing references", expectedSeq.get_references().toString(), testSeq.get_references().toString());
    assertEquals("comparing metadata", expectedSeq.get_metadata().toString(), testSeq.get_metadata().toString());
    assertEquals("comapring src db", expectedSeq.get_srcdb(), testSeq.get_srcdb());
  }

}
