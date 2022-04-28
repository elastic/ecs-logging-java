package co.elastic.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(category = PatternConverter.CATEGORY, name = "CustomExceptionPatternConverter")
@ConverterKeys({"cEx"})
public class CustomExceptionPatternConverter extends LogEventPatternConverter {

    public CustomExceptionPatternConverter(final String[] options) {
        super("Custom", "custom");
    }

    public static CustomExceptionPatternConverter newInstance(final String[] options) {
        return new CustomExceptionPatternConverter(options);
    }


    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            String message = thrown.getMessage();
            if (message == null || message.isEmpty()) {
                toAppendTo.append(thrown.getClass().getName())
                        .append('\n');
            } else {
                toAppendTo.append(thrown.getClass().getName())
                        .append(": ")
                        .append(message)
                        .append('\n');
            }

            toAppendTo.append("STACK_TRACE!");
        }
    }

    @Override
    public boolean handlesThrowable() {
        return true;
    }
}
