package io.metamorphic.fileservices;

import io.metamorphic.models.DatasetInfo;

import java.io.IOException;
import java.util.List;

/**
 * Created by markmo on 18/05/15.
 */
public interface FileService {

    void setDateFormats(String[] dateFormats);

    FileParameters sniff(String data, String lineEnding);

    TypeInfo deduceDataType(String value);

    DatasetInfo extractMetadata(String datasetName, String data) throws ExtractionException, IOException;

    FileParameters findMultiCharSequences(String data, String lineEnding);

    void generateDataFromDDL(String ddl, int numRows) throws IOException;

    FileParameters guessDelimiter(String data, String lineEnding);

    FileParameters guessQuoteAndDelimiter(String data, String lineEnding);

    boolean hasHeader(String[][] data);

    boolean hasHeader(List<List<String>> sample);

    LinesContainer readLines(String data);

    String[] getHeader(String[][] rows, TypeInfo[] types, boolean hasHeader);

    String[] getHeader(List<List<String>> rows, TypeInfo[] types, boolean hasHeader);

    String[] makeHeaderNames(TypeInfo[] types);

    TypesContainer getTypes(String[][] rows, int sampleSize, int maxNumberColumns, boolean hasHeader);

    TypesContainer getTypes(List<List<String>> rows, int sampleSize, int maxNumberColumns, boolean hasHeader);

    DataTypes getSqlType(ValueTypes type);

    ParsedDate parseDate(String value);

    boolean parseBoolean(String value);

    boolean isDefaultName(String name);
}
