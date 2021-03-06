package picard.fingerprint;

import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.IOUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picard.util.TabbedTextFileWithHeaderParser;
import picard.vcf.SamTestUtils;
import picard.vcf.VcfTestUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tests for CrosscheckFingerprints
 */
public class CrosscheckFingerprintsTest {

    private final File TEST_DIR = new File("testdata/picard/fingerprint/");
    private final File HAPLOTYPE_MAP = new File(TEST_DIR, "Homo_sapiens_assembly19.haplotype_database.subset.txt");

    private final File NA12891_r1_sam = new File(TEST_DIR, "NA12891.over.fingerprints.r1.sam");
    private final File NA12891_r2_sam = new File(TEST_DIR, "NA12891.over.fingerprints.r2.sam");

    //this is a copy of a previous one, but with a different sample name
    private final File NA12891_named_NA12892_r1_sam = new File(TEST_DIR, "NA12891_named_NA12892.over.fingerprints.r1.sam");

    private final File NA12892_r1_sam = new File(TEST_DIR, "NA12892.over.fingerprints.r1.sam");
    private final File NA12892_r2_sam = new File(TEST_DIR, "NA12892.over.fingerprints.r2.sam");

    private File NA12891_r1, NA12891_r2, NA12891_named_NA12892_r1, NA12892_r1, NA12892_r2;

    private final int NA12891_r1_RGs = 27;
    private final int NA12891_r2_RGs = 26;
    private final int NA12892_r1_RGs = 25;
    private final int NA12892_r2_RGs = 26;

    private static File  NA12891_1_vcf;
    private static File  NA12891_2_vcf;
    private static File  NA12892_1_vcf;
    private static File  NA12891_swapped_nonref_g_vcf;
    private static File  NA12892_2_vcf;
    private static File  NA12891_named_NA12892_vcf;
    private static File  NA12891_g_vcf;
    private static File  NA12892_g_vcf;
    private static File  NA12892_and_NA123891_vcf;
    private static File  NA12892_and_NA123891_part1_vcf;
    private static File  NA12892_and_NA123891_part2_vcf;
    private static File  NA12892_and_NA123891_part3_vcf;

    private static final Map<CrosscheckMetric.DataType, List<String>> lookupMap = new HashMap<>(4);
    
    @BeforeClass
    public void setup() throws IOException {
        NA12891_r1 = SamTestUtils.createIndexedBam(NA12891_r1_sam, NA12891_r1_sam);
        NA12891_r2 = SamTestUtils.createIndexedBam(NA12891_r2_sam, NA12891_r2_sam);
        NA12891_named_NA12892_r1 = SamTestUtils.createIndexedBam(NA12891_named_NA12892_r1_sam, NA12891_named_NA12892_r1_sam);
        NA12892_r1 = SamTestUtils.createIndexedBam(NA12892_r1_sam, NA12892_r1_sam);
        NA12892_r2 = SamTestUtils.createIndexedBam(NA12892_r2_sam, NA12892_r2_sam);

        lookupMap.put(CrosscheckMetric.DataType.FILE, new ArrayList<>());
        lookupMap.get(CrosscheckMetric.DataType.FILE).addAll(Arrays.asList("LEFT_FILE", "RIGHT_FILE"));

        lookupMap.put(CrosscheckMetric.DataType.SAMPLE, new ArrayList<>());
        lookupMap.get(CrosscheckMetric.DataType.SAMPLE).addAll(Arrays.asList("LEFT_SAMPLE", "RIGHT_SAMPLE"));
        lookupMap.get(CrosscheckMetric.DataType.SAMPLE).addAll(lookupMap.get(CrosscheckMetric.DataType.FILE));

        lookupMap.put(CrosscheckMetric.DataType.LIBRARY, new ArrayList<>());
        lookupMap.get(CrosscheckMetric.DataType.LIBRARY).addAll(Arrays.asList("LEFT_LIBRARY", "RIGHT_LIBRARY"));
        lookupMap.get(CrosscheckMetric.DataType.LIBRARY).addAll(lookupMap.get(CrosscheckMetric.DataType.SAMPLE));

        lookupMap.put(CrosscheckMetric.DataType.READGROUP, new ArrayList<>());
        lookupMap.get(CrosscheckMetric.DataType.READGROUP).addAll(Arrays.asList("LEFT_RUN_BARCODE", "LEFT_LANE",
                "LEFT_MOLECULAR_BARCODE_SEQUENCE","RIGHT_RUN_BARCODE",
                "RIGHT_LANE", "RIGHT_MOLECULAR_BARCODE_SEQUENCE"));
        lookupMap.get(CrosscheckMetric.DataType.READGROUP).addAll(lookupMap.get(CrosscheckMetric.DataType.LIBRARY));


        NA12891_1_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891.vcf"), "fingerprint");
        NA12891_2_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891.fp.vcf"), "fingerprint");
        NA12891_named_NA12892_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891_named_NA12892.vcf"), "fingerprint");
        NA12892_1_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12892.vcf"), "fingerprint");
        NA12892_2_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12892.fp.vcf"), "fingerprint");
        NA12891_g_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891.g.vcf"), "fingerprint");
        NA12891_swapped_nonref_g_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891.with_swapped_NON_REF.g.vcf"), "fingerprint");

        NA12892_g_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12892.g.vcf"), "fingerprint");
        NA12892_and_NA123891_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891andNA12892.vcf"), "fingerprint");
        NA12892_and_NA123891_part1_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891andNA12892_part1.vcf"), "fingerprint");
        NA12892_and_NA123891_part2_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891andNA12892_part2.vcf"), "fingerprint");
        NA12892_and_NA123891_part3_vcf = VcfTestUtils.createTemporaryIndexedVcfFromInput(new File(TEST_DIR, "NA12891andNA12892_part3.vcf"), "fingerprint");

    }

    @DataProvider(name = "bamFilesRGs")
    public Object[][] bamFilesRGs() {
        return new Object[][] {
                {NA12891_r1, NA12891_r2, false, 0, (NA12891_r1_RGs + NA12891_r2_RGs) * (NA12891_r1_RGs + NA12891_r2_RGs + 1) / 2},
                {NA12891_r1, NA12892_r1, false, 0, (NA12891_r1_RGs + NA12892_r1_RGs) * (NA12891_r1_RGs + NA12892_r1_RGs + 1) / 2},
                {NA12891_r1, NA12892_r2, false, 0, (NA12891_r1_RGs + NA12892_r2_RGs) * (NA12891_r1_RGs + NA12892_r2_RGs + 1) / 2},
                {NA12892_r1, NA12892_r2, false, 0, (NA12892_r1_RGs + NA12892_r2_RGs) * (NA12892_r1_RGs + NA12892_r2_RGs + 1) / 2},
                {NA12892_r2, NA12891_r2, false, 0, (NA12892_r2_RGs + NA12891_r2_RGs) * (NA12892_r2_RGs + NA12891_r2_RGs + 1) / 2},
                {NA12892_r2, NA12891_r1, false, 0, (NA12892_r2_RGs + NA12891_r1_RGs) * (NA12892_r2_RGs + NA12891_r1_RGs + 1) / 2},
                {NA12891_r1, NA12891_r2, true,  0, (NA12891_r1_RGs + NA12891_r2_RGs) * (NA12891_r1_RGs + NA12891_r2_RGs + 1) / 2},
                {NA12891_r1, NA12892_r1, true,  1, (NA12891_r1_RGs + NA12892_r1_RGs) * (NA12891_r1_RGs + NA12892_r1_RGs + 1) / 2},
                {NA12891_r1, NA12892_r2, true,  1, (NA12891_r1_RGs + NA12892_r2_RGs) * (NA12891_r1_RGs + NA12892_r2_RGs + 1) / 2},
                {NA12892_r1, NA12892_r2, true,  0, (NA12892_r1_RGs + NA12892_r2_RGs) * (NA12892_r1_RGs + NA12892_r2_RGs + 1) / 2},
                {NA12892_r2, NA12891_r2, true,  1, (NA12892_r2_RGs + NA12891_r2_RGs) * (NA12892_r2_RGs + NA12891_r2_RGs + 1) / 2},
                {NA12892_r2, NA12891_r1, true,  1, (NA12892_r2_RGs + NA12891_r1_RGs) * (NA12892_r2_RGs + NA12891_r1_RGs + 1) / 2}
        };
    }

    @Test(dataProvider = "bamFilesRGs")
    public void testCrossCheckRGs(final File file1, final File file2, final boolean expectAllMatch, final int expectedRetVal, final int expectedNMetrics) throws IOException {

        File metrics = File.createTempFile("Fingerprinting", "NA1291.RG.crosscheck_metrics");
        metrics.deleteOnExit();

        final String[] args = new String[]{
                "INPUT=" + file1.getAbsolutePath(),
                "INPUT=" + file2.getAbsolutePath(),
                "OUTPUT=" + metrics.getAbsolutePath(),
                "HAPLOTYPE_MAP=" + HAPLOTYPE_MAP,
                "LOD_THRESHOLD=" + -2.0,
                "EXPECT_ALL_GROUPS_TO_MATCH=" + expectAllMatch
        };

        doTest(args, metrics, expectedRetVal, expectedNMetrics, CrosscheckMetric.DataType.READGROUP, expectAllMatch);
    }

    @DataProvider(name = "bamFilesLBs")
    public Object[][] bamFilesLBs() {

        return new Object[][]{
               {NA12891_r1, NA12891_r2,                   0},
               {NA12891_r1, NA12892_r1,                   0},
               {NA12892_r2, NA12891_r2,                   0},
               {NA12892_r2, NA12891_r1,                   0},
               {NA12891_r1, NA12891_named_NA12892_r1_sam, 1}, //error since expected match but found a mismatch
               {NA12892_r1, NA12891_named_NA12892_r1_sam, 1}, //error since expected mismatch but found a match
               {NA12891_r2, NA12891_named_NA12892_r1_sam, 1}, //error since expected match but found a mismatch
               {NA12892_r2, NA12891_named_NA12892_r1_sam, 1}, //error since expected mismatch but found a match
        };
    }

    @Test
    public void testCrossCheckLBsWithClustering() throws IOException {
        File metrics = File.createTempFile("Fingerprinting", "NA1291.LB.crosscheck_metrics");
        metrics.deleteOnExit();

        {
            final String[] args = new String[]{
                    "INPUT=" + NA12891_r1.getAbsolutePath(),
                    "INPUT=" + NA12891_r2.getAbsolutePath(),
                    "INPUT=" + NA12892_r1.getAbsolutePath(),
                    "INPUT=" + NA12892_r2.getAbsolutePath(),
                    "INPUT=" + NA12891_named_NA12892_r1_sam.getAbsolutePath(),
                    "OUTPUT=" + metrics.getAbsolutePath(),
                    "HAPLOTYPE_MAP=" + HAPLOTYPE_MAP,
                    "LOD_THRESHOLD=" + -1.0,
                    "CROSSCHECK_BY=LIBRARY"
            };
            final int nLibs = 5;
            doTest(args, metrics, 1, nLibs * (nLibs + 1) / 2, CrosscheckMetric.DataType.LIBRARY);
        }

        File cluster = File.createTempFile("Fingerprinting", "NA1291.LB.crosscheck_cluster_metrics");
        cluster.deleteOnExit();

        {
            final String[] args = new String[]{
                    "INPUT=" + metrics.getAbsolutePath(),
                    "OUTPUT=" + cluster.getAbsolutePath(),
                    "LOD_THRESHOLD=" + 1.0,
            };

            final ClusterCrosscheckMetrics clusterer = new ClusterCrosscheckMetrics();
            Assert.assertEquals(clusterer.instanceMain(args), 0);
        }

        IOUtil.assertFileIsReadable(cluster);

        final MetricsFile<ClusteredCrosscheckMetric, Comparable<?>> metricsOutput = new MetricsFile<>();
        metricsOutput.read(new FileReader(cluster));

        final List<Long> collect = metricsOutput
                .getMetrics()
                .stream()
                .map(m -> m.CLUSTER)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(collect.get(0), (Long)3L); // 2*3/2
        Assert.assertEquals(collect.get(1), (Long)6L); // 3*4/2
    }

    @Test(dataProvider = "bamFilesLBs")
    public void testCrossCheckLBs(final File file1, final File file2, final int expectedRetVal) throws IOException {
        File metrics = File.createTempFile("Fingerprinting", "NA1291.LB.crosscheck_metrics");
        metrics.deleteOnExit();

        final String[] args = new String[]{
                "INPUT=" + file1.getAbsolutePath(),
                "INPUT=" + file2.getAbsolutePath(),
                "OUTPUT=" + metrics.getAbsolutePath(),
                "HAPLOTYPE_MAP=" + HAPLOTYPE_MAP,
                "LOD_THRESHOLD=" + -1.0,
                "CROSSCHECK_BY=LIBRARY"
        };
        final int numLibs=2;
        doTest(args, metrics, expectedRetVal, numLibs * (numLibs + 1) / 2, CrosscheckMetric.DataType.LIBRARY);
    }


    @DataProvider(name = "bamFilesSources")
    public Object[][] bamFilesSources() {

        return new Object[][]{
                {NA12891_r1, NA12891_r2,               0},
                {NA12892_r1, NA12892_r2,               0},
                {NA12891_r1, NA12892_r1,               0},
                {NA12892_r2, NA12891_r2,               0},
                {NA12892_r2, NA12891_named_NA12892_r1, 1}, // the two files contain different samples one has wrong name
                {NA12891_r2, NA12891_named_NA12892_r1, 1}, // unexpected match
                {NA12892_r1, NA12891_named_NA12892_r1, 1}, // the two files contain different samples one has wrong name
                {NA12891_r1, NA12891_named_NA12892_r1, 1}, // unexpected match
        };
    }

    @Test(dataProvider = "bamFilesSources")
    public void testCrossCheckSources(final File file1, final File file2, final int expectedRetVal) throws IOException {
        File metrics = File.createTempFile("Fingerprinting", "NA1291.Sources.crosscheck_metrics");
        metrics.deleteOnExit();

        final String[] args = new String[]{
                "INPUT=" + file1.getAbsolutePath(),
                "INPUT=" + file2.getAbsolutePath(),
                "OUTPUT=" + metrics.getAbsolutePath(),
                "HAPLOTYPE_MAP=" + HAPLOTYPE_MAP,
                "LOD_THRESHOLD=" + -1.0,
                "CROSSCHECK_BY=FILE"
        };

        doTest(args, metrics, expectedRetVal, 2 * 3 / 2, CrosscheckMetric.DataType.FILE);
    }

    @DataProvider(name = "bamFilesSMs")
    public Object[][] bamFilesSMs() {

        return new Object[][] {
                {NA12891_r1, NA12891_r2,               0, 1},
                {NA12891_r1, NA12892_r1,               0, 2},
                {NA12892_r2, NA12891_r2,               0, 2},
                {NA12892_r2, NA12891_named_NA12892_r1, 0, 1}, // no error since only one sample in aggregate
                {NA12891_r2, NA12891_named_NA12892_r1, 1, 2}, // unexpected match
                {NA12892_r1, NA12891_named_NA12892_r1, 0, 1}, // no error since only one sample in aggregate
                {NA12891_r1, NA12891_named_NA12892_r1, 1, 2}, // unexpected match
        };
    }

    @Test(dataProvider = "bamFilesSMs")
    public void testCrossCheckSMs(final File file1, final File file2, final int expectedRetVal, final int numberOfSamples) throws IOException {
        File metrics = File.createTempFile("Fingerprinting", "NA1291.SM.crosscheck_metrics");
        metrics.deleteOnExit();

        File matrix = File.createTempFile("Fingerprinting", "NA1291.SM.matrix");
        matrix.deleteOnExit();

        final String[] args = new String[]{
                "INPUT=" + file1.getAbsolutePath(),
                "INPUT=" + file2.getAbsolutePath(),
                "OUTPUT=" + metrics.getAbsolutePath(),
                "MATRIX_OUTPUT=" + matrix.getAbsolutePath(),
                "HAPLOTYPE_MAP=" + HAPLOTYPE_MAP,
                "LOD_THRESHOLD=" + -1.0,
                "CROSSCHECK_BY=SAMPLE"
        };
        doTest(args, metrics, expectedRetVal, numberOfSamples * (numberOfSamples + 1) / 2, CrosscheckMetric.DataType.SAMPLE);

        TabbedTextFileWithHeaderParser matrixParser = new TabbedTextFileWithHeaderParser(matrix);
        Assert.assertEquals(matrixParser.columnLabelsList().size(), numberOfSamples + 1);
    }

    @DataProvider(name = "checkSamplesData")
    public Iterator<Object[]> checkSamplesData() {
        List<Object[]> tests = new ArrayList<>();

        // VCF tests
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_1_vcf), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_part1_vcf, NA12892_and_NA123891_part2_vcf, NA12892_and_NA123891_part3_vcf), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12891_named_NA12892_vcf,NA12891_1_vcf), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 1, 2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_1_vcf), Arrays.asList(NA12891_2_vcf, NA12892_1_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf), Arrays.asList(NA12891_2_vcf, NA12892_1_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf), Arrays.asList(NA12891_g_vcf, NA12891_named_NA12892_vcf), 1, 2, false});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_part1_vcf, NA12892_and_NA123891_part2_vcf, NA12892_and_NA123891_part3_vcf), Arrays.asList(NA12891_2_vcf, NA12892_r1), 0, 2, true});

        // SAM vs. VCF
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1, NA12891_r2, NA12892_r2), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12891_named_NA12892_r1, NA12891_r1), Arrays.asList(NA12891_g_vcf, NA12892_g_vcf), 1, 2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_r1), Arrays.asList(NA12891_r2, NA12892_1_vcf), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12891_named_NA12892_r1), Arrays.asList(NA12891_r2, NA12892_2_vcf), 1, 2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_1_vcf), Arrays.asList(NA12891_2_vcf, NA12891_named_NA12892_r1), 1, 2, false});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf), Arrays.asList(NA12891_2_vcf, NA12891_named_NA12892_r1), 1, 2, false});

        // SAM tests
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1), Arrays.asList(NA12891_r2, NA12892_r2), 0, 2, true});
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1), Arrays.asList(NA12891_r2), 1, 1, true});
        tests.add(new Object[]{Arrays.asList(NA12891_r1), Arrays.asList(NA12891_r2, NA12892_r2), 1, 1, true});
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12891_named_NA12892_r1), Arrays.asList(NA12891_r2, NA12892_r2), 1, 2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1), Arrays.asList(NA12891_r1, NA12891_named_NA12892_r1), 1, 2, false});

        // Swapped Non-ref gvcf
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_1_vcf), Arrays.asList(NA12891_swapped_nonref_g_vcf, NA12892_g_vcf), 1, 2, false});

        return tests.iterator();
    }

    @Test(dataProvider = "checkSamplesData")
    public void testCheckSamples(final List<File> files1, final List<File> files2, final int expectedRetVal, final int numberOfSamples, boolean ExpectAllMatch) throws IOException {
        File metrics = File.createTempFile("Fingerprinting", "test.crosscheck_metrics");
        metrics.deleteOnExit();

        final List<String> args = new ArrayList<>();
        files1.forEach(f->args.add("INPUT="+f.getAbsolutePath()));
        files2.forEach(f->args.add("SECOND_INPUT="+f.getAbsolutePath()));

                args.add("OUTPUT=" + metrics.getAbsolutePath());
                args.add("HAPLOTYPE_MAP=" + HAPLOTYPE_MAP);
                args.add("LOD_THRESHOLD=" + -1.0);
                args.add("CROSSCHECK_BY=SAMPLE");

        doTest(args.toArray(new String[args.size()]), metrics, expectedRetVal, numberOfSamples , CrosscheckMetric.DataType.SAMPLE, ExpectAllMatch);
    }

    @DataProvider(name = "checkFilesData")
    public Iterator<Object[]> checkFilesData() {
        List<Object[]> tests = new ArrayList<>();

        // VCF tests
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_1_vcf, NA12891_g_vcf, NA12892_g_vcf), 0, 4*5/2, false});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf, NA12891_g_vcf, NA12892_g_vcf),0,  4*5/2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_named_NA12892_vcf,NA12891_1_vcf, NA12891_g_vcf, NA12892_g_vcf), 1, 4*5/2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_1_vcf, NA12891_2_vcf, NA12892_1_vcf),  0,4*3/2, false});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf, NA12891_2_vcf, NA12892_1_vcf), 0, 4*5/2, false});
        tests.add(new Object[]{Arrays.asList(NA12892_and_NA123891_vcf, NA12891_g_vcf, NA12891_named_NA12892_vcf), 1, 4*5/2, false});

        // SAM vs. VCF
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1, NA12891_r2, NA12892_r2, NA12891_g_vcf, NA12892_g_vcf), 0, 6 * 7/2, false});
        tests.add(new Object[]{Arrays.asList(NA12891_named_NA12892_r1, NA12891_r1, NA12891_g_vcf, NA12891_1_vcf), 1, 4*5/2, false});

        tests.add(new Object[]{Arrays.asList(NA12891_1_vcf, NA12892_r1, NA12891_r2, NA12892_1_vcf), 0, 4*5/2, true});

        // SAM tests
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1, NA12891_r2, NA12892_r2), 0, 4*5/2, true});
        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12892_r1, NA12891_r2), 0, 3*4/2, true});

        tests.add(new Object[]{Arrays.asList(NA12891_r1, NA12891_named_NA12892_r1, NA12891_r2, NA12892_r2), 1, 4*5/2, false});

        return tests.iterator();
    }


    @Test(dataProvider = "checkFilesData")
    public void testCheckFiles(final List<File> files, final int expectedRetVal, final int numberOfSamples, boolean ExpectAllMatch) throws IOException {
        File metrics = File.createTempFile("Fingerprinting", "test.crosscheck_metrics");
        metrics.deleteOnExit();

        final List<String> args = new ArrayList<>();

        files.forEach(f->args.add("INPUT="+f.getAbsolutePath()));

        args.add("OUTPUT=" + metrics.getAbsolutePath());
        args.add("HAPLOTYPE_MAP=" + HAPLOTYPE_MAP);
        args.add("LOD_THRESHOLD=" + -1.0);
        args.add("CROSSCHECK_BY=FILE");

        doTest(args.toArray(new String[args.size()]), metrics, expectedRetVal, numberOfSamples , CrosscheckMetric.DataType.FILE, ExpectAllMatch);
    }



    private void doTest(final String[] args, final File metrics, final int expectedRetVal, final int expectedNMetrics, final CrosscheckMetric.DataType expectedType) throws IOException {
        doTest(args, metrics, expectedRetVal, expectedNMetrics, expectedType, false);
    }

    private void doTest(final String[] args, final File metrics, final int expectedRetVal, final int expectedNMetrics, final CrosscheckMetric.DataType expectedType, final boolean expectAllMatch) throws IOException {
       
        final CrosscheckFingerprints crossChecker = new CrosscheckFingerprints();
        Assert.assertEquals(crossChecker.instanceMain(args), expectedRetVal);

        final MetricsFile<CrosscheckMetric, Comparable<?>> metricsOutput = new MetricsFile<>();
        metricsOutput.read(new FileReader(metrics));

        Assert.assertFalse(metricsOutput.getMetrics().stream()
                .anyMatch(m -> m.DATA_TYPE != expectedType));

        Assert.assertFalse(metricsOutput.getMetrics().stream()
                .anyMatch(m-> m.LOD_SCORE_NORMAL_TUMOR == null));
        Assert.assertFalse(metricsOutput.getMetrics().stream()
                .anyMatch(m-> m.LOD_SCORE == null));
        Assert.assertFalse(metricsOutput.getMetrics().stream()
                .anyMatch(m-> m.LOD_SCORE_TUMOR_NORMAL == null));

        if (expectAllMatch) {
            Assert.assertTrue(metricsOutput.getMetrics().stream()
                    .allMatch(m -> m.RESULT == CrosscheckMetric.FingerprintResult.INCONCLUSIVE ||
                            m.RESULT.isMatch() == m.LEFT_SAMPLE.equals(m.RIGHT_SAMPLE)));
        } else if (expectedRetVal == 0) {
            Assert.assertTrue(metricsOutput.getMetrics().stream()
                    .allMatch(m -> m.RESULT == CrosscheckMetric.FingerprintResult.INCONCLUSIVE ||
                            m.RESULT.isExpected()));
        } else {
            Assert.assertTrue(metricsOutput.getMetrics().stream()
                    .anyMatch(m -> !m.RESULT.isExpected()));
        }

        Assert.assertEquals(metricsOutput.getMetrics().size(), expectedNMetrics);

        // at the readgroup level things aren't always conclusive...
        if (!metricsOutput.getMetrics().isEmpty() && expectedType != CrosscheckMetric.DataType.READGROUP) {
            Assert.assertTrue(metricsOutput.getMetrics().stream()
                    .anyMatch(m -> m.RESULT != CrosscheckMetric.FingerprintResult.INCONCLUSIVE));
        }

        //check that fields that should have an actual value, indeed do
        for(final String fieldName : lookupMap.get(expectedType)) {
            try {
                final Field field = CrosscheckMetric.class.getField(fieldName);
                Assert.assertTrue(metricsOutput.getMetrics().stream().allMatch(m -> {
                    try {
                        return field.get(m) != FingerprintIdDetails.multipleValuesString && field.get(m) != null;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return false;
                    }
                }));

            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                assert false;
            }
        }
    }

    @Test
    public void canWriteToDevNull() throws IOException {
        File f = new File("/dev/null");
        Assert.assertTrue(f.canRead());

        final OutputStream stream = new FileOutputStream(f);
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        writer.write("Just a test");
        writer.close();
    }
}