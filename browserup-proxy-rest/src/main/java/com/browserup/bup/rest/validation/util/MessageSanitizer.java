package com.browserup.bup.rest.validation.util;
/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 * Original from:
 * https://github.com/hibernate/hibernate-validator/blob/master/engine/src/main/java/org/hibernate/validator/internal/engine/messageinterpolation/util/InterpolationHelper.java
 */
/*
 * License: Apache License, Version 2.0
 * See the license file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageSanitizer {

  public static final char BEGIN_CHAR = '{';
  public static final char END_CHAR = '}';
  public static final char EL_DESIGNATOR = '$';
  public static final char ESCAPE_CHARACTER = '\\';

  private static final Pattern ESCAPE_PATTERN = Pattern.compile( "([\\" + ESCAPE_CHARACTER + BEGIN_CHAR + END_CHAR + EL_DESIGNATOR + "])" );

  private MessageSanitizer() {
  }

  public static String escape(String message) {
    if ( message == null ) {
      return null;
    }
    return ESCAPE_PATTERN.matcher( message ).replaceAll( Matcher.quoteReplacement( String.valueOf( ESCAPE_CHARACTER ) ) + "$1" );
  }
}
