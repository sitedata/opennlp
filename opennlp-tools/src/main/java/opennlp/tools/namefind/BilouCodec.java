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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.util.SequenceCodec;
import opennlp.tools.util.SequenceValidator;
import opennlp.tools.util.Span;

/**
 * The default {@link SequenceCodec} implementation according to the {@code BILOU} scheme.
 * <ul>
 *   <li>B: 'beginning' of a NE</li>
 *   <li>I: 'inside', the word is inside a NE</li>
 *   <li>L: 'last', the last (I) word inside a NE</li>
 *   <li>O: 'outside', the word is a regular word outside a NE</li>
 *   <li>U: 'unit', any standalone token following words outside of NE</li>
 * </ul>
 *
 * See paper by Roth D. and Ratinov L. (2009):
 * <a href="https://cogcomp.seas.upenn.edu/page/publication_view/199">
 *  Design Challenges and Misconceptions in Named Entity Recognition</a>.
 *
 * @see SequenceCodec
 * @see BioCodec
 */
public class BilouCodec implements SequenceCodec<String> {

  public static final String START = "start";
  public static final String CONTINUE = "cont";
  public static final String LAST = "last";
  public static final String UNIT = "unit";
  public static final String OTHER = "other";

  @Override
  public Span[] decode(List<String> c) {
    int start = -1;
    int end = -1;
    List<Span> spans = new ArrayList<>(c.size());
    for (int li = 0; li < c.size(); li++) {
      String chunkTag = c.get(li);
      if (chunkTag.endsWith(BilouCodec.START)) {
        start = li;
        end = li + 1;
      }
      else if (chunkTag.endsWith(BilouCodec.CONTINUE)) {
        end = li + 1;
      }
      else if (chunkTag.endsWith(LAST)) {
        if (start != -1) {
          spans.add(new Span(start, end + 1, BioCodec.extractNameType(c.get(li - 1))));
          start = -1;
          end = -1;
        }
      }
      else if (chunkTag.endsWith(UNIT)) {
        spans.add(new Span(li, li + 1, BioCodec.extractNameType(c.get(li))));
      }
    }

    return spans.toArray(new Span[spans.size()]);
  }

  @Override
  public String[] encode(Span[] names, int length) {
    String[] outcomes = new String[length];
    Arrays.fill(outcomes, BilouCodec.OTHER);

    for (Span name : names) {

      if (name.length() > 1) {
        if (name.getType() == null) {
          outcomes[name.getStart()] = "default" + "-" + BilouCodec.START;
        }
        else {
          outcomes[name.getStart()] = name.getType() + "-" + BilouCodec.START;
        }
        // now iterate from begin + 1 till end
        for (int i = name.getStart() + 1; i < name.getEnd() - 1; i++) {
          if (name.getType() == null) {
            outcomes[i] = "default" + "-" + BilouCodec.CONTINUE;
          }
          else {
            outcomes[i] = name.getType() + "-" + BilouCodec.CONTINUE;
          }
        }

        if (name.getType() == null) {
          outcomes[name.getEnd() - 1] = "default" + "-" + BilouCodec.LAST;
        }
        else {
          outcomes[name.getEnd() - 1] = name.getType() + "-" + BilouCodec.LAST;
        }
      }
      else {
        if (name.getType() == null) {
          outcomes[name.getEnd() - 1] = "default" + "-" + BilouCodec.UNIT;
        }
        else {
          outcomes[name.getEnd() - 1] = name.getType() + "-" + BilouCodec.UNIT;
        }
      }
    }

    return outcomes;
  }

  @Override
  public SequenceValidator<String> createSequenceValidator() {
    return new BilouNameFinderSequenceValidator();
  }

  /**
   * {@code
   * B requires CL or L,
   * C requires BL,
   * L requires B,
   * O requires any valid combo/unit,
   * U requires none.
   * }
   * 
   * @param outcomes All potential model outcomes check.
   *
   * @return {@code true}, if model outcomes are compatible, {@code false} otherwise.
   */
  @Override
  public boolean areOutcomesCompatible(String[] outcomes) {
    Set<String> start = new HashSet<>();
    Set<String> cont = new HashSet<>();
    Set<String> last = new HashSet<>();
    Set<String> unit = new HashSet<>();

    for (String outcome : outcomes) {
      if (outcome.endsWith(BilouCodec.START)) {
        start.add(outcome.substring(0, outcome.length()
                - BilouCodec.START.length()));
      } else if (outcome.endsWith(BilouCodec.CONTINUE)) {
        cont.add(outcome.substring(0, outcome.length()
                - BilouCodec.CONTINUE.length()));
      } else if (outcome.endsWith(BilouCodec.LAST)) {
        last.add(outcome.substring(0, outcome.length()
                - BilouCodec.LAST.length()));
      } else if (outcome.endsWith(BilouCodec.UNIT)) {
        unit.add(outcome.substring(0, outcome.length()
                - BilouCodec.UNIT.length()));
      } else if (!outcome.equals(BilouCodec.OTHER)) {
        return false;
      }
    }

    if (start.size() == 0 && unit.size() == 0) {
      return false;
    } else {
      // Start, must have matching Last
      for (String startPrefix : start) {
        if (!last.contains(startPrefix)) {
          return false;
        }
      }
      // Cont, must have matching Start and Last
      for (String contPrefix : cont) {
        if (!start.contains(contPrefix) && !last.contains(contPrefix)) {
          return false;
        }
      }
      // Last, must have matching Start
      for (String lastPrefix : last) {
        if (!start.contains(lastPrefix)) {
          return false;
        }
      }

    }

    return true;
  }
}
