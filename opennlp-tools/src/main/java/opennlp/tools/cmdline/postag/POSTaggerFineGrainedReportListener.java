/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.cmdline.postag;

import java.io.OutputStream;

import opennlp.tools.cmdline.FineGrainedReportListener;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerEvaluationMonitor;

/**
 * Generates a detailed report for the POS Tagger.
 * <p>
 * It is possible to use it from an API and access the statistics using the
 * provided getters
 *
 */
public class POSTaggerFineGrainedReportListener
    extends FineGrainedReportListener implements POSTaggerEvaluationMonitor {

  /**
   * Creates a listener that will print to {@code System#err}.
   */
  public POSTaggerFineGrainedReportListener() {
    this(System.err);
  }

  /**
   * Creates a listener that prints to a given {@link OutputStream}.
   */
  public POSTaggerFineGrainedReportListener(OutputStream outputStream) {
    super(outputStream);

  }

  // methods inherited from EvaluationMonitor
  @Override
  public void misclassified(POSSample reference, POSSample prediction) {
    statsAdd(reference, prediction);
  }

  @Override
  public void correctlyClassified(POSSample reference, POSSample prediction) {
    statsAdd(reference, prediction);
  }

  private void statsAdd(POSSample reference, POSSample prediction) {
    getStats().add(reference.getSentence(), reference.getTags(), prediction.getTags());
  }

  @Override
  public void writeReport() {
    printGeneralStatistics();
    // token stats
    printTokenErrorRank();
    printTokenOccurrencesRank();
    // tag stats
    printTagsErrorRank();
    // confusion tables
    printGeneralConfusionTable();
    printDetailedConfusionMatrix();
  }

}
