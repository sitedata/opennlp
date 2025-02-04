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

package opennlp.tools.namefind;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorUtil;
import opennlp.tools.util.featuregen.OutcomePriorFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

/**
 * A {@link NameContextGenerator} implementation for determining contextual features
 * for a {@code tag-chunk} style named-entity recognizer.
 */
public class DefaultNameContextGenerator implements NameContextGenerator {

  protected AdaptiveFeatureGenerator[] featureGenerators;

  @Deprecated
  private static final AdaptiveFeatureGenerator WINDOW_FEATURES = new CachedFeatureGenerator(
      new WindowFeatureGenerator(new TokenFeatureGenerator(), 2, 2),
      new WindowFeatureGenerator(new TokenClassFeatureGenerator(true), 2, 2),
      new OutcomePriorFeatureGenerator(),
      new PreviousMapFeatureGenerator(),
      new BigramNameFeatureGenerator());

  /**
   * Creates a name context generator.
   * 
   * @deprecated use {@link #DefaultNameContextGenerator(AdaptiveFeatureGenerator...)} and
   *             always provide one or more {@link AdaptiveFeatureGenerator feature generators}.
   */
  @Deprecated
  public DefaultNameContextGenerator() {
    this((AdaptiveFeatureGenerator[]) null);
  }

  /**
   * Creates a name context generator with the specified
   * {@link AdaptiveFeatureGenerator feature generators}.
   *
   * @param featureGenerators One or more {@link AdaptiveFeatureGenerator feature generators}.
   *                          If none are provided, a default config ({@link #WINDOW_FEATURES})
   *                          will be used.
   */
  public DefaultNameContextGenerator(AdaptiveFeatureGenerator... featureGenerators) {

    if (featureGenerators != null) {
      this.featureGenerators = featureGenerators;
    }
    else { // use defaults
      this.featureGenerators =
          new AdaptiveFeatureGenerator[]{WINDOW_FEATURES, new PreviousMapFeatureGenerator()};
    }
  }

  @Override
  public void addFeatureGenerator(AdaptiveFeatureGenerator generator) {
    AdaptiveFeatureGenerator[] generators = featureGenerators;

    featureGenerators = new AdaptiveFeatureGenerator[featureGenerators.length + 1];

    System.arraycopy(generators, 0, featureGenerators, 0, generators.length);

    featureGenerators[featureGenerators.length - 1] = generator;
  }

  @Override
  public void updateAdaptiveData(String[] tokens, String[] outcomes) {

    if (tokens != null && outcomes != null && tokens.length != outcomes.length) {
      throw new IllegalArgumentException(
          "The tokens and outcome arrays MUST have the same size!");
    }

    for (AdaptiveFeatureGenerator featureGenerator : featureGenerators) {
      featureGenerator.updateAdaptiveData(tokens, outcomes);
    }
  }

  @Override
  public void clearAdaptiveData() {
    for (AdaptiveFeatureGenerator featureGenerator : featureGenerators) {
      featureGenerator.clearAdaptiveData();
    }
  }

  /**
   * Finds the context for finding names at the specified index.
   *
   * @param index The index of the token in the specified {@code tokens} for which the
   *              context should be constructed.
   * @param tokens The tokens of the sentence. The {@code toString()} methods
   *               of these objects should return the token text.
   * @param preds The previous decisions made in the tagging of this sequence.
   *              Only indices less than {@code index} will be examined.
   * @param additionalContext Addition features which may be based on a context outside the sentence.
   *
   * @return The context for finding names at the specified {@code index}.
   */
  @Override
  public String[] getContext(int index, String[] tokens, String[] preds, Object[] additionalContext) {
    List<String> features = new ArrayList<>();

    for (AdaptiveFeatureGenerator featureGenerator : featureGenerators) {
      featureGenerator.createFeatures(features, tokens, index, preds);
    }

    //previous outcome features
    String po = NameFinderME.OTHER;
    String ppo = NameFinderME.OTHER;

    // TODO: These should be moved out here in its own feature generator!
    if (preds != null) {
      if (index > 1) {
        ppo = preds[index - 2];
      }

      if (index > 0) {
        po = preds[index - 1];
      }
      features.add("po=" + po);
      features.add("pow=" + po + "," + tokens[index]);
      features.add("powf=" + po + "," + FeatureGeneratorUtil.tokenFeature(tokens[index]));
      features.add("ppo=" + ppo);
    }

    return features.toArray(new String[features.size()]);
  }
}
