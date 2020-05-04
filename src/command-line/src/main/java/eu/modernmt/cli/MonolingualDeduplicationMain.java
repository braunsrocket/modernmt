package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.List;

public class MonolingualDeduplicationMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option language = Option.builder("s").hasArg().required().build();
            Option lengthThreshold = Option.builder("l").hasArg().required(false).build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(language);
            cliOptions.addOption(lengthThreshold);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
        }

        public final Language language;
        public final int lengthThreshold;
        public final File[] inputRoots;
        public final File outputRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            language = Language.fromString(cli.getOptionValue('s'));
            lengthThreshold = cli.hasOption("l") ? Integer.parseInt(cli.getOptionValue("l")) : 0;

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        List<Corpus> corpora = Corpora.list(args.language, args.inputRoots);
        if (corpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        ModernMT.training.deduplicateMonolingual(corpora, args.outputRoot, args.lengthThreshold);
    }
}
