package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.lucene.LuceneTokenAnnotator;

import java.io.Reader;

public class HebrewTokenizer extends BaseTokenizer {

    public HebrewTokenizer() {
        super.annotators.add(LuceneTokenAnnotator.forLanguage(Language.HEBREW));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
