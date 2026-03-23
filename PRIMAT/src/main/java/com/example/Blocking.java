/*******************************************************************************
 *  Copyright © 2017 - 2022 Leipzig University (Database Research Group)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

import java.io.IOException;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.common.blocking.BlockingKeyDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVWriter;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.SubstringByPositionExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.phonetic.PhoneticCodeExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.common.utils.StringListAggregator;


public class Blocking {

	public static void main(String[] args) throws IOException {

		final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
				.add(0, NonQidAttributeType.ID)
				.add(1, NonQidAttributeType.GLOBAL_ID)
				.add(2, NonQidAttributeType.PARTY)
				.add(3, QidAttributeType.STRING, "name")
				.build();

		final String inputPath = args[0];
		final String outputPath = args[1];

		final DatasetReader reader = new DatasetReader(inputPath, rsc);
		final List<Record> records = reader.read();

		final ExtractorDefinition exDef1 = new ExtractorDefinition();
		exDef1.setColumnsByName("name");
		exDef1.setExtractors(new SubstringByPositionExtractor(0, 2));


		final BlockingKeyDefinition bk1 = new BlockingKeyDefinition(List.of(exDef1),
			StringListAggregator.CONCAT);

		final Blocker blocker = new Blocker();
		blocker.addBlockingKeyDefinition(bk1);
		blocker.addBlockingKeys(records);

		final CSVWriter csvWriter = new CSVWriter(outputPath);
		csvWriter.writeRecords(records);
	}
}
