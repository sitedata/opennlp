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

package opennlp.tools.cmdline.lemmatizer;

import java.io.OutputStream;

import opennlp.tools.cmdline.EvaluationErrorPrinter;
import opennlp.tools.lemmatizer.LemmaSample;
import opennlp.tools.lemmatizer.LemmatizerEvaluationMonitor;
import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * A default implementation of {@link EvaluationMonitor} that prints to an
 * output stream.
 */
public class LemmaEvaluationErrorListener extends
    EvaluationErrorPrinter<LemmaSample> implements LemmatizerEvaluationMonitor {

  /**
   * Creates a listener that will print to {@code System.err}.
   */
  public LemmaEvaluationErrorListener() {
    super(System.err);
  }

  /**
   * Creates a listener that will print to a given {@link OutputStream}
   */
  public LemmaEvaluationErrorListener(OutputStream outputStream) {
    super(outputStream);
  }

  @Override
  public void misclassified(LemmaSample reference, LemmaSample prediction) {
    printError(reference.getLemmas(), prediction.getLemmas(), reference,
        prediction, reference.getTokens());
  }

}
