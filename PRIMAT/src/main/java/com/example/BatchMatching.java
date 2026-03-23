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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.RandomHammingLshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.model.LinkageConstraint;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DoubleListAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.lsh.LshBlocker;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.ThresholdClassificator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.batch.BatchMatcher;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.NoPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxBothPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxRightPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.BitSetAttributeSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.BaseRecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.BatchSimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.RedundancyCheckStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.FlatSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.NoThresholdRefinement;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ThresholdClassificationRefinement;



/**
 *
 * @author mfranke
 *
 */
public class BatchMatching {

	public static void main(String[] args) throws IOException {

		final String abtFilePath = args[0];
        final String buyFilePath = args[1];
        final double threshold = Double.parseDouble(args[2]); // e.g., 0.80


		final int expectedMatches = Integer.parseInt(args[3]); // Total true matches for Recall calculation

        // ==========================================
        // 1. FIX THE SCHEMA & READ DATASETS
        // ==========================================
        // We removed the duplicate Index 1 bug from the original code!
        final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
                .add(0, NonQidAttributeType.ID)
				.add(1, NonQidAttributeType.GLOBAL_ID)
                .add(2, NonQidAttributeType.PARTY)
                // Note: Depending on your PRIMAT version, use BIT_SET or BITSET here
                .add(3, QidAttributeType.BITSET, "RBF")
                .build();

        final DatasetReader abtReader = new DatasetReader(abtFilePath, rsc);
        abtReader.setHasHeader(true);
        final List<Record> datasetAbt = abtReader.read();

        final DatasetReader buyReader = new DatasetReader(buyFilePath, rsc);
        buyReader.setHasHeader(true);
        final List<Record> datasetBuy = buyReader.read();

		// ==========================================
        // 2. THE GRAPH SAVER: FORCE UNIQUE IDs
        // ==========================================
        for (Record r : datasetAbt) {
            String oldId = r.getIdAttribute().getStringValue();
            r.getIdAttribute().setValueFromString("abt_" + oldId);
        }
        for (Record r : datasetBuy) {
            String oldId = r.getIdAttribute().getStringValue();
            r.getIdAttribute().setValueFromString("buy_" + oldId);
        }

        // Map datasets to their respective parties
        final Party partyAbt = new Party("abt");
        final Party partyBuy = new Party("buy");

        final Map<Party, Collection<Record>> input = new HashMap<>();
        input.put(partyAbt, datasetAbt);
        input.put(partyBuy, datasetBuy);

        System.out.println("Loaded Abt Records: " + datasetAbt.size());
        System.out.println("Loaded Buy Records: " + datasetBuy.size());

        // ==========================================
        // 3. BLOCKING STRATEGY (LSH)
        // ==========================================
        final LshKeyGenerator keyGen = new RandomHammingLshKeyGenerator(16, 30, 1024, 42L);
        final Blocker blocker = new LshBlocker(keyGen);

        // ==========================================
        // 4. SIMILARITY & CLASSIFICATION
        // ==========================================
        // Compare BitSets using Jaccard Similarity
        final RecordSimilarityCalculator simCalc = new BaseRecordSimilarityCalculator(
            List.of(new BitSetAttributeSimilarityCalculator(List.of(BinarySimilarity.JACCARD_SIMILARITY))));

        // Aggregate the similarities into a final score
        final SimilarityVectorFlattener flattener = new BaseSimilarityVectorFlattener(List.of(DoubleListAggregator.FIRST));
        final FlatSimilarityVectorAggregator aggregator = new BaseSimilarityVectorAggregator(DoubleListAggregator.FIRST);
        final SimilarityVectorAggregator agg = new SimilarityVectorAggregator(flattener, aggregator);

        final Classificator classifier = new ThresholdClassificator(threshold, agg);

        // Setup internal Batch definitions
        final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
        final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
        final LinkageResultPartitionFactory<Record> linkResFac = new LinkageResultPartitionFactory<>(matchFactory, nonMatchFactory);

        final SimilarityClassification simClass = new BatchSimilarityClassification(
            ComparisonStrategy.SOURCE_CONSISTENT, simCalc, classifier, RedundancyCheckStrategy.MATCH_TWICE, linkResFac);

        // ==========================================
        // 4. POST-PROCESSING (1:1 RESTRICTION)
        // ==========================================
        // This ensures the highest scoring match is kept if one Abt product matches multiple Buy products.
        final PostprocessingStrategy<Record> postprocessor = new PostprocessingStrategy<>();
        postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_ONE, new MaxBothPostprocessor<Record>());
        postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_ONE, new MaxRightPostprocessor<Record>());
        postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_MANY, new NoPostprocessor<Record>());
        postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_MANY, new NoPostprocessor<Record>());

        // ==========================================
        // 5. EXECUTE BATCH MATCHING
        // ==========================================
        System.out.println("Running Batch Matcher...");
        final Matcher<Record> matcher = new BatchMatcher(blocker, simClass, new NoThresholdRefinement(), postprocessor);
        final LinkageResult<Record> linkRes = matcher.match(input);

        // ==========================================
        // 6. EVALUATION
        // ==========================================
        // NOTE: IdEqualityTrueMatchChecker assumes an Abt item and Buy item have the exact same String ID.
        final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
        final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);

        final PartyPair partyPairAB = new PartyPair(partyAbt, partyBuy);
        final LinkageResultPartition<Record> part = linkRes.getPartition(partyPairAB);
        evaluator.addMatches(part.getMatchStrategy().getMatches());

        final long truePos = evaluator.getTruePositives();
        final long falsePos = evaluator.getFalsePositives();

        final double recall = QualityMetrics.getRecall(truePos, expectedMatches);
        final double precision = QualityMetrics.getPrecision(truePos, truePos + falsePos);
        final double fmeasure = QualityMetrics.getFMeasure(recall, precision);

        System.out.println("=========================================");
        System.out.println("Total Matches Found: " + part.getMatchStrategy().getMatches().size());
        System.out.println("Recall: " + recall);
        System.out.println("Precision: " + precision);
        System.out.println("F-Measure: " + fmeasure);
        System.out.println("=========================================");
    }
}