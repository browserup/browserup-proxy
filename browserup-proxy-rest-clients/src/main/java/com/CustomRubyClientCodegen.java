package com;

import org.openapitools.codegen.languages.RubyClientCodegen;

public class CustomRubyClientCodegen extends RubyClientCodegen {
    public CustomRubyClientCodegen() {
        reservedWords.add("send");
    }
}
