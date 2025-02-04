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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.util.SequenceCodec;
import opennlp.tools.util.Span;

/**
 * The default {@link SequenceCodec} implementation according to the {@code BIO} scheme:
 * <ul>
 *   <li>B: 'beginning' of a NE</li>
 *   <li>I: 'inside', the word is inside a NE</li>
 *   <li>O: 'outside', the word is a regular word outside a NE</li>
 * </ul>
 *
 * See also the paper by Roth D. and Ratinov L.:
 * <a href="https://cogcomp.seas.upenn.edu/page/publication_view/199">
 *  Design Challenges and Misconceptions in Named Entity Recognition</a>.
 *
 * @see SequenceCodec
 * @see BilouCodec
 */
public class BioCodec implements SequenceCodec<String> {

  public static final String START = "start";
  public static final String CONTINUE = "cont";
  public static final String OTHER = "other";

  private static final Pattern TYPED_OUTCOME_PATTERN = Pattern.compile("(.+)-\\w+");

  static String extractNameType(String outcome) {
    Matcher matcher = TYPED_OUTCOME_PATTERN.matcher(outcome);
    if (matcher.matches()) {
      return matcher.group(1);
    }

    return null;
  }

  @Override
  public Span[] decode(List<String> c) {
    int start = -1;
    int end = -1;
    List<Span> spans = new ArrayList<>(c.size());
    for (int li = 0; li < c.size(); li++) {
      String chunkTag = c.get(li);
      if (chunkTag.endsWith(BioCodec.START)) {
        if (start != -1) {
          spans.add(new Span(start, end, extractNameType(c.get(li - 1))));
        }

        start = li;
        end = li + 1;

      }
      else if (chunkTag.endsWith(BioCodec.CONTINUE)) {
        end = li + 1;
      }
      else if (chunkTag.endsWith(BioCodec.OTHER)) {
        if (start != -1) {
          spans.add(new Span(start, end, extractNameType(c.get(li - 1))));
          start = -1;
          end = -1;
        }
      }
    }

    if (start != -1) {
      spans.add(new Span(start, end, extractNameType(c.get(c.size() - 1))));
    }

    return spans.toArray(new Span[spans.size()]);
  }

  @Override
  public String[] encode(Span[] names, int length) {
    String[] outcomes = new String[length];
    Arrays.fill(outcomes, BioCodec.OTHER);
    for (Span name : names) {
      if (name.getType() == null) {
        outcomes[name.getStart()] = "default" + "-" + BioCodec.START;
      }
      else {
        outcomes[name.getStart()] = name.getType() + "-" + BioCodec.START;
      }
      // now iterate from begin + 1 till end
      for (int i = name.getStart() + 1; i < name.getEnd(); i++) {
        if (name.getType() == null) {
          outcomes[i] = "default" + "-" + BioCodec.CONTINUE;
        }
        else {
          outcomes[i] = name.getType() + "-" + BioCodec.CONTINUE;
        }
      }
    }

    return outcomes;
  }

  @Override
  public NameFinderSequenceValidator createSequenceValidator() {
    return new NameFinderSequenceValidator();
  }

  @Override
  public boolean areOutcomesCompatible(String[] outcomes) {
    // We should have *optionally* one outcome named "other", some named xyz-start and sometimes
    // they have a pair xyz-cont. We should not have any other outcome
    // To validate the model we check if we have one outcome named "other", at least
    // one outcome with suffix start. After that we check if all outcomes that ends with
    // "cont" have a pair that ends with "start".
    List<String> start = new ArrayList<>();
    List<String> cont = new ArrayList<>();

    for (String outcome : outcomes) {
      if (outcome.endsWith(BioCodec.START)) {
        start.add(outcome.substring(0, outcome.length()
                - BioCodec.START.length()));
      } else if (outcome.endsWith(BioCodec.CONTINUE)) {
        cont.add(outcome.substring(0, outcome.length()
                - BioCodec.CONTINUE.length()));
      } else if (!outcome.equals(BioCodec.OTHER)) {
        // got unexpected outcome
        return false;
      }
    }

    if (start.size() == 0) {
      return false;
    } else {
      for (String contPreffix : cont) {
        if (!start.contains(contPreffix)) {
          return false;
        }
      }
    }

    return true;
  }
}
