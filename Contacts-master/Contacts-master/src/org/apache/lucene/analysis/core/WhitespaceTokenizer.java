package org.apache.lucene.analysis.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * A WhitespaceTokenizer is a tokenizer that divides text at whitespace.
 * Adjacent sequences of non-Whitespace characters form tokens. <a
 * name="version"/>
 * <p>
 * You must specify the required {@link org.apache.lucene.util.Version} compatibility when creating
 * {@link WhitespaceTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link org.apache.lucene.analysis.util.CharTokenizer} uses an int based API to normalize and
 * detect token characters. See {@link org.apache.lucene.analysis.util.CharTokenizer#isTokenChar(int)} and
 * {@link org.apache.lucene.analysis.util.CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 */
public final class WhitespaceTokenizer extends CharTokenizer {

  /**
   * Construct a new WhitespaceTokenizer. * @param matchVersion Lucene version
   * to match See {@link <a href="#version">above</a>}
   *
   * @param in
   *          the input to split up into tokens
   */
  public WhitespaceTokenizer(Version matchVersion, Reader in) {
    super(matchVersion, in);
  }

  /**
   * Construct a new WhitespaceTokenizer using a given {@link org.apache.lucene.util.AttributeSource}.
   *
   * @param matchVersion
   *          Lucene version to match See {@link <a href="#version">above</a>}
   * @param source
   *          the attribute source to use for this {@link org.apache.lucene.analysis.Tokenizer}
   * @param in
   *          the input to split up into tokens
   */
  public WhitespaceTokenizer(Version matchVersion, AttributeSource source, Reader in) {
    super(matchVersion, source, in);
  }

  /**
   * Construct a new WhitespaceTokenizer using a given
   * {@link org.apache.lucene.util.AttributeSource.AttributeFactory}.
   *
   * @param
   *          matchVersion Lucene version to match See
   *          {@link <a href="#version">above</a>}
   * @param factory
   *          the attribute factory to use for this {@link org.apache.lucene.analysis.Tokenizer}
   * @param in
   *          the input to split up into tokens
   */
  public WhitespaceTokenizer(Version matchVersion, AttributeFactory factory, Reader in) {
    super(matchVersion, factory, in);
  }
  
  /** Collects only characters which do not satisfy
   * {@link Character#isWhitespace(int)}.*/
  @Override
  protected boolean isTokenChar(int c) {
    return !Character.isWhitespace(c);
  }
}
