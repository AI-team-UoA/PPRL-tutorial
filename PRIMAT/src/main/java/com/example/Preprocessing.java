
import java.io.IOException;
import java.util.List;
import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVWriter;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.FieldNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.FieldSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.NormalizeDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.PartySupplier;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.SplitDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.AccentRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.LetterLowerCaseToNumberNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.LetterUpperCaseToNumberNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.LowerCaseNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.NormalizerChain;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SpecialCharacterRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SubstringNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.TrimNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.UmlautNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.BlankSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.DotSplitter;


/**
 *
 * @author mfranke
 *
 */

import java.io.IOException;
import java.util.List;


public class Preprocessing {

    public static void main(String[] args) throws IOException {

        // ==========================================
        // 1. SCHEMA DEFINITION
        // ==========================================
        final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
                .add(0, NonQidAttributeType.ID)
				.add(2, NonQidAttributeType.PARTY)
                .add(3, NonQidAttributeType.GLOBAL_ID)
                .add(1, QidAttributeType.STRING, "name")
                .build();

        final String inputPath = args[0];
        final String outputPath = args[1];

        // ==========================================
        // 2. READ DATA & SET DELIMITER
        // ==========================================
        final DatasetReader reader = new DatasetReader(inputPath, rsc);


        // Optional: If your file has a header row, uncomment the line below to skip it
        reader.setHasHeader(true);

        final List<Record> records = reader.read();



		// final PartySupplier partySupp = new PartySupplier();
		// partySupp.preprocess(records);

        // ==========================================
        // 3. NORMALIZATION
        // ==========================================
        // Clean the 'name' column for accurate Bloom Filter encoding.
        // We trim spaces, convert to lowercase, and remove accents/special characters.
        final NormalizerChain nameNormalizer = new NormalizerChain(
            List.of(
                new TrimNormalizer(),
                new LowerCaseNormalizer(),
                new AccentRemover(),
                new SpecialCharacterRemover()
            )
        );

        final NormalizeDefinition normDef = new NormalizeDefinition();

        // Apply the normalizer to the 'name' column (which is index 0 in our schema above)
        normDef.setNormalizer(0, nameNormalizer);

        final FieldNormalizer fn = new FieldNormalizer(normDef);
        fn.preprocess(records);

        // ==========================================
        // 4. WRITE CLEANED DATA
        // ==========================================
        final CSVWriter csvWriter = new CSVWriter(outputPath);

        // Ensure the writer also outputs using the pipe delimiter
        csvWriter.writeRecords(records);

        System.out.println("Preprocessing complete. Cleaned and saved " + records.size() + " records.");
    }
}