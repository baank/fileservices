package io.metamorphic.fileservices;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.moilioncircle.ddl.parser.ColumnElement;
import com.moilioncircle.ddl.parser.MysqlDDLParser;
import com.moilioncircle.ddl.parser.TableElement;
import io.metamorphic.commons.Pair;
import io.metamorphic.models.*;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.metamorphic.commons.utils.ArrayUtils.*;
import static io.metamorphic.commons.utils.StringUtils.*;

/**
 * Created by markmo on 18/05/15.
 */
public class FileServiceImpl implements FileService {

    private static final Logger log = LogManager.getLogger(FileServiceImpl.class);

    private static final char[] preferredColumnDelimiters = new char[] { ',', '\t', ';', ' ', '|' };

    // row delimiters tested for
    private static final String[] lineEndings = new String[] { "\n", "\r\n", "\r", "<ret>" };

    // to resolve ambiguity when a value qualifies for more than one type
    private static final List<ValueTypes> typeHierarchy = Arrays.asList(
        ValueTypes.NONE,
        ValueTypes.BIT,
        ValueTypes.BOOLEAN,
        ValueTypes.INTEGER,
        ValueTypes.NUMERIC,
        ValueTypes.DATE,
        ValueTypes.STRING
    );

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\s*[+-]?(0(?=\\.)|[1-9])[0-9]*(\\.[0-9]+)?\\s*$");

    private TypeParser typeParser;

    private static final int MAX_SAMPLE_SIZE = 20;

    public FileServiceImpl() {
        typeParser = new TypeParser();
        typeParser.registerTypeParser(Boolean.class, new BooleanParser());

        // Set date formats
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream resource = classLoader.getResourceAsStream("date_formats.txt");
        if (resource != null) {
            try {
                String[] dateFormats = readDateFormats(resource);
                typeParser.registerTypeParser(ParsedDate.class, new DateParser(dateFormats));
            } catch (IOException e) {
                log.warn("Error reading 'date_formats.txt' file: " + e.getMessage());
                typeParser.registerTypeParser(ParsedDate.class, new DateParser());
            }
        } else {
            log.warn("Could not find 'date_formats.txt' file.");
            typeParser.registerTypeParser(ParsedDate.class, new DateParser());
        }
    }

    public void setDateFormats(String[] dateFormats) {
        typeParser.registerTypeParser(ParsedDate.class, new DateParser(dateFormats));
    }

    public TypeInfo deduceDataType(String value) {
        if (value == null) return new TypeInfo(ValueTypes.NONE);
        String v = value.trim();
        if (v.isEmpty()) return new TypeInfo(ValueTypes.NONE);
        Matcher m = NUMERIC_PATTERN.matcher(value);
        try {
            Integer i = Integer.parseInt(v, 10);
            // check that the decimal place is not truncated
            if (i.toString().equals(v)) {
                if (i == 0 || i == 1) {
                    return new TypeInfo(ValueTypes.BIT);
                } else {
                    return new TypeInfo(ValueTypes.INTEGER);
                }
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        if (m.matches()) {
            try {
                Double.parseDouble(v);
                return new TypeInfo(ValueTypes.NUMERIC);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        ParsedDate dt = typeParser.parse(v, ParsedDate.class);
        if (dt != null) return new TypeInfo(ValueTypes.DATE, "format", dt.getFormat());
        Boolean bool = typeParser.parse(v, Boolean.class);
        if (bool != null) return new TypeInfo(ValueTypes.BOOLEAN);
        if (v.length() > 128) return new TypeInfo(ValueTypes.TEXT);
        return new TypeInfo(ValueTypes.STRING);
    }

    public DatasetInfo extractMetadata(String dataSourceName, String datasetName, String data)
            throws ExtractionException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Extracting metadata");
        }
        Pattern startJsonFilePattern = Pattern.compile("^\\s*[\\[\\{]", Pattern.MULTILINE);
        Matcher matcher = startJsonFilePattern.matcher(data);
        if (matcher.find()) {
            if (log.isDebugEnabled()) {
                log.debug("Reading JSON");
            }
            return readJsonSample(datasetName, data);
        }
        if (log.isDebugEnabled()) {
            log.debug("Reading delimited");
        }

        // strip blank lines at the start of the file
        data = data.replaceAll("^\\s+", "");

        LinesContainer lc = readLines(data);
        String[] lines = lc.lines;
        String lineEnding = lc.lineEnding;

        if (log.isDebugEnabled()) {
            log.debug("line ending [" + StringEscapeUtils.escapeJava(lineEnding) + "]");
        }

        FileParameters fileParameters = sniff(data, lineEnding);

        if (fileParameters == null) {
            throw new ExtractionException("Could not determine file parameters");
        }

        int sampleSize = Math.min(MAX_SAMPLE_SIZE, lines.length);

        if (log.isDebugEnabled()) {
            log.debug("sample size: " + sampleSize);
        }

        char quotechar = getQuoteChar(fileParameters.getTextQualifier());

        if (!memberOf(new String[]{"\n", "\r\n", "\r"}, lineEnding)) {
            data = data.replace("\n", "\\n").replace("\r", "\\r").replace(lineEnding, "\n");
        }

        CSVParser parser = new CSVParser(fileParameters.getColumnDelimiter(), quotechar);
        CSVReader reader = new CSVReader(new StringReader(data), 0, parser);

        RowsContainer rc = readRows(reader, sampleSize);
        String[][] rows = rc.rows;
        int maxNumberColumns = rc.maxNumberColumns;

        boolean hasHeader = hasHeader(rows);
        fileParameters.setHeader(hasHeader);

        if (log.isDebugEnabled()) {
            log.debug("header? " + (hasHeader ? "YES" : "NO"));
        }

        TypesContainer tc = getTypes(rows, sampleSize, maxNumberColumns, hasHeader);
        TypeInfo[] types = tc.types;
        DataTypes[] sqlTypes = tc.sqlTypes;
        int[] lengths = tc.lengths;

        Pair<String, String> dateFormats = getFirstDateFormat(tc);
        fileParameters.setFirstDateFormat(dateFormats.l);
        fileParameters.setFirstDateTimeFormat(dateFormats.r);

        String[] header = getHeader(rows, types, hasHeader);

        DatasetInfo datasetInfo = new DatasetInfo();
        datasetInfo.setSsuDesignation("nonsp");
        datasetInfo.setDataSourceName(dataSourceName);
        datasetInfo.setName(datasetName);
        datasetInfo.setFileType(FileType.DELIMITED.toString());
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        for (int i = 0; i < header.length; i++) {
            columns.add(new ColumnInfo(header[i], i + 1, types[i].toString(),
                    sqlTypes[i].toString(), lengths[i]));
        }
        datasetInfo.setColumns(columns);
        fileParameters.setSrcFormats(getSrcFormats(columns, tc));
        datasetInfo.setFileParameters(fileParameters);

        return datasetInfo;
    }

    private Map<String, String> getSrcFormats(List<ColumnInfo> columns, TypesContainer tc) {
        Map<String, String> srcFormats = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo c = columns.get(i);
            TypeInfo t = tc.types[i];
            if (ValueTypes.DATE.equals(t.getType())) {
                if (t.getInfo().containsKey("format")) {
                    String format = (String)t.getInfo().get("format");
                    srcFormats.put(c.getName(), format);
                }
            }
        }
        return srcFormats;
    }

    private Pair<String, String> getFirstDateFormat(TypesContainer tc) {
        String dateFormat = null;
        String dateTimeFormat = null;
        for (TypeInfo t : tc.types) {
            if (ValueTypes.DATE.equals(t.getType())) {
                if (t.getInfo().containsKey("format")) {
                    String format = (String)t.getInfo().get("format");
                    if (dateTimeFormat == null && format.contains("HH")) {
                        dateTimeFormat = format;
                    } else if (dateFormat == null) {
                        dateFormat = format;
                    }
                    if (dateFormat != null && dateTimeFormat != null) {
                        break;
                    }
                }
            }
        }
        return new Pair<>(dateFormat, dateTimeFormat);
    }

    /**
     * urrgh.. the following is dense. I'd use Java 8 if I could be sure of
     * compatibility, or Scala if I could.
     *
     * @param data (str) sample file as string
     * @return (DatasetInfo)
     */
    public FileParameters findMultiCharSequences(String data, String lineEnding) {
        String[] rows = data.split(lineEnding);
        int chunkLength = Math.min(10, rows.length);
        int iteration = 0;
        Map<String, Map<Integer, Integer>> strFrequency = new HashMap<>();
        Map<String, Integer[]> modes = new HashMap<>();
        Map<String, Integer[]> delims = new HashMap<>();
        String delim;
        boolean skipInitialSpace;
        int start = 0;
        int end = Math.min(chunkLength, rows.length);
        int slidingWindow = 5;
        ObjectMapper mapper = new ObjectMapper();
        while (start < rows.length) {
            iteration += 1;
            for (String line : Arrays.asList(rows).subList(start, end)) {
                Map<String, Integer> counts = new HashMap<>();
                for (int w = 2; w <= slidingWindow; w++) {
                    for (int i = 0; i <= line.length() - w; i++) {
                        String str = line.substring(i, i + w);
                        if (counts.containsKey(str)) {
                            counts.put(str, counts.get(str) + 1);
                        } else {
                            counts.put(str, 1);
                        }
                    }
                }
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() > 1) {
                        Map<Integer, Integer> metaFrequency;
                        if (strFrequency.containsKey(entry.getKey())) {
                            metaFrequency = strFrequency.get(entry.getKey());
                        } else {
                            metaFrequency = new HashMap<>();
                        }
                        int value = 0;
                        if (metaFrequency.containsKey(entry.getValue())) {
                            value = metaFrequency.get(entry.getValue());
                        }
                        metaFrequency.put(entry.getValue(), value + 1);
                        //Map<Integer, Integer> metaFrequency = strFrequency.getOrDefault(entry.getKey(), new HashMap<Integer, Integer>());
                        //metaFrequency.put(entry.getValue(), metaFrequency.getOrDefault(entry.getValue(), 0) + 1);
                        strFrequency.put(entry.getKey(), metaFrequency);
                    }
                }
            }
            for (Map.Entry<String, Map<Integer, Integer>> entry : strFrequency.entrySet()) {
                Map<Integer, Integer> metaFrequency = entry.getValue();
                if (!(metaFrequency.size() == 1 && metaFrequency.containsKey(0))) {
                    if (metaFrequency.size() > 0) {
                        Map.Entry<Integer, Integer> maxItem = null;
                        for (Map.Entry<Integer, Integer> item : metaFrequency.entrySet()) {
                            if (maxItem == null || item.getValue() > maxItem.getValue()) {
                                maxItem = item;
                            }
                        }
                        int sumOtherFreqs = 0;
                        Integer maxFreq = (maxItem == null ? null : maxItem.getKey());
                        Integer maxFreqVal = (maxItem == null ? 0 : maxItem.getValue());
                        for (Map.Entry<Integer, Integer> item : metaFrequency.entrySet()) {
                            if (!item.getKey().equals(maxFreq)) {
                                sumOtherFreqs += item.getValue();
                            }
                        }
                        modes.put(entry.getKey(), new Integer[] { maxFreq, maxFreqVal - sumOtherFreqs });
                    } else {
                        Map.Entry<Integer, Integer> it = metaFrequency.entrySet().iterator().next();
                        modes.put(entry.getKey(), new Integer[] { it.getKey(), it.getValue() });
                    }
                }
            }
            int total = chunkLength * iteration;
            if (log.isDebugEnabled()) {
                try {
                    log.debug(mapper.writeValueAsString(modes));
                } catch (JsonProcessingException e) {
                    // ignore
                }
            }
            // (rows of consistent data) / (number of rows) = 100%
            double consistency = 1.0;

            // minimum consistency threshold
            double threshold = 0.9;

            while (delims.isEmpty() && consistency >= threshold) {
                for (Map.Entry<String, Integer[]> entry : modes.entrySet()) {
                    Integer[] v = entry.getValue();
                    if (v[0] > 0 && v[1] > 0) {
                        if ((v[1] / total) >= consistency) {
                            delims.put(entry.getKey(), v);
                        }
                    }
                }
                consistency -= 0.01;
            }
            if (delims.size() == 1) {
                delim = delims.keySet().iterator().next();
                String firstLine = rows[0];
                int delimCount = countSubstring(firstLine, delim);
                Pattern p = Pattern.compile(delim + " ");
                Matcher m = p.matcher(firstLine);
                int delimWithSpaceCount = 0;
                while (m.find()) delimWithSpaceCount++;
                skipInitialSpace = (delimCount == delimWithSpaceCount);
                return new FileParameters(delim, skipInitialSpace);
            }

            // analyze another chunkLength lines
            start = end;
            end += chunkLength;
            end = Math.min(end, rows.length);
        }
        if (delims.isEmpty()) {
            return new FileParameters();
        }

        // if there's more than one, fall back to a 'preferred' list
        for (Character ch : preferredColumnDelimiters) {
            String del = ch.toString();
            if (delims.keySet().contains(del)) {
                String firstLine = rows[0];
                int delimCount = countSubstring(firstLine, del);
                Pattern p = Pattern.compile(del + " ");
                Matcher m = p.matcher(firstLine);
                int delimWithSpaceCount = 0;
                while (m.find()) delimWithSpaceCount++;
                skipInitialSpace = (delimCount == delimWithSpaceCount);
                return new FileParameters(del, skipInitialSpace);
            }
        }
        if (log.isDebugEnabled()) {
            try {
                log.debug(mapper.writeValueAsString(delims));
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        // nothing else indicates a preference, pick the character that
        // dominates(?)
        Map.Entry<String, Integer[]> maxEntry = null;
        for (Map.Entry<String, Integer[]> entry : delims.entrySet()) {
            if (maxEntry == null || entry.getValue()[0] > maxEntry.getValue()[0]) {
                maxEntry = entry;
            }
        }
        delim = maxEntry.getKey();
        String firstLine = rows[0];
        int delimCount = countSubstring(firstLine, delim);
        Pattern p = Pattern.compile(delim + " ");
        Matcher m = p.matcher(firstLine);
        int delimWithSpaceCount = 0;
        while (m.find()) delimWithSpaceCount++;
        skipInitialSpace = (delimCount == delimWithSpaceCount);
        return new FileParameters(delim, skipInitialSpace);
    }

    public static String[] STRING_TYPES = {"STRING", "VARCHAR", "NVARCHAR", "TEXT"};
    public static String[] INT_TYPES = {"TINYINT", "SMALLINT", "MEDIUMINT", "INTEGER", "INT", "BIGINT"};
    public static String[] FLOAT_TYPES = {"REAL", "DOUBLE", "FLOAT", "DECIMAL", "NUMERIC"};
    public static String[] FIRST_NAMES = {"first_name", "firstname", "given_name", "given_names"};
    public static String[] LAST_NAMES = {"last_name", "lastname", "surname", "family_name"};
    public static String[] FULL_NAMES = {"name", "full_name", "fullname"};
    public static String[] DATE_TYPES = {"DATE", "DATETIME"};
    public static String[] IP_ADDRESS = {"ip_address", "ip_addr", "ip", "ipv4", "ipv6"};

    public void generateDataFromDDL(String ddl, int numRows) throws IOException {
        Faker faker = new Faker();
        List<TableElement> tables = new MysqlDDLParser().parse(ddl);
        TableElement table = tables.get(0);
        String datasetName = table.getTableName().toString();
        int numCols = table.getColumns().size();
        String[][] columns = new String[numCols][numRows];
        String[] header = new String[numCols];
        int j = 0;
        for (ColumnElement column : table.getColumns()) {
            String columnName = column.getColumnName().toString().toLowerCase();
            header[j] = columnName;
            String columnType = column.getType().name();
            String[] values = new String[numRows];
            for (int i = 0; i < numRows; i++) {
                if (memberOf(INT_TYPES, columnType)) {
                    values[i] = String.valueOf(faker.number().numberBetween(0, 1000));
                } else if (memberOf(FLOAT_TYPES, columnType)) {
                    values[i] = String.valueOf(faker.number().randomDouble(2, 0, 1000));
                } else if (memberOf(DATE_TYPES, columnType)) {
                    values[i] = faker.date().toString();
                } else {
                    if (columnName.endsWith("id")) {
                        values[i] = faker.crypto().md5();
                    } else if (memberOf(FIRST_NAMES, columnName)) {
                        values[i] = faker.name().firstName();
                    } else if (memberOf(LAST_NAMES, columnName)) {
                        values[i] = faker.name().lastName();
                    } else if (memberOf(FULL_NAMES, columnName)) {
                        values[i] = faker.name().fullName();
                    } else if (columnName.contains("email")) {
                        values[i] = faker.internet().emailAddress();
                    } else if (columnName.contains("url")) {
                        values[i] = faker.internet().url();
                    } else if (memberOf(IP_ADDRESS, columnName)) {
                        values[i] = faker.internet().ipV4Address();
                    } else {
                        values[i] = faker.lorem().characters(5, 10);
                    }
                }
            }
            columns[j] = values;
            j += 1;
        }
        String[][] rows = transposeColumnArray(columns);
        CSVWriter writer = new CSVWriter(new FileWriter(datasetName + ".csv"));
        writer.writeNext(header);
        for (String[] row : rows) {
            writer.writeNext(row);
        }
        writer.close();
    }

    private String[][] transposeColumnArray(String[][] columns) {
        int numCols = columns.length;
        int numRows = columns[0].length;
        String[][] rows = new String[numRows][numCols];
        for (int i = 0; i < numCols; i++) {
            for (int j = 0; j < numRows; j++) {
                rows[j][i] = columns[i][j];
            }
        }
        return rows;
    }

    private Map<String, DataType> getDataTypeMap() {
        Map<String, DataType> dataTypeMap = new HashMap<>();
        int i = 0;
        for (DataTypes type : DataTypes.values()) {
            dataTypeMap.put(type.name(), new DataType(i, type.name()));
            i += 1;
        }
        return dataTypeMap;
    }

    public String[] getHeader(String[][] rows, TypeInfo[] types, boolean hasHeader) {
        String[] header;
        if (hasHeader) {
            header = rows[0];
            for (int i = 0; i < header.length; i++) {
                if (header[i] != null) {
                    String columnName = header[i].trim();
                    if (!columnName.isEmpty()) {
                        header[i] = columnName;
                        continue;
                    }
                }
                header[i] = types[i].toString().toLowerCase() + "_" + (i + 1);
            }
        } else {
            header = makeHeaderNames(types);
        }
        return header;
    }

    public String[] getHeader(List<List<String>> rows, TypeInfo[] types, boolean hasHeader) {
        int n = rows.size();
        String[][] data = new String[n][];
        for (int i = 0; i < n; i++) {
            List<String> row = rows.get(i);
            data[i] = row.toArray(new String[row.size()]);
        }
        return getHeader(data, types, hasHeader);
    }

    private double getLineLengthVariance(String[] lines, double mean) {
        if (lines.length == 0) return 0;
        double temp = 0.0;
        for (String line : lines) {
            temp += (mean - line.length()) * (mean - line.length());
        }
        return temp / lines.length;
    }

    private double getMeanLineLength(String[] lines) {
        if (lines.length == 0) return 0;
        double sum = 0.0;
        for (String line : lines) {
            sum += line.length();
        }
        return sum / lines.length;
    }

    private double getModeLineLength(String[] lines) {
        if (lines.length == 0) return 0;
        int modeCount = 0;
        int mode = 0;
        int currCount = 0;

        for (String line : lines) {
            // Reset the number of times we have seen the current value
            currCount = 0;

            // Iterate through the array counting the number of times we see the current candidate mode
            for (String ln : lines) {
                // If they match, increment the current count
                if (line.length() == ln.length()) {
                    currCount++;
                }
            }
            // We only save this candidate mode, if its count is greater than the current mode
            // we have stored in the "mode" variable
            if (currCount > modeCount) {
                modeCount = currCount;
                mode = line.length();
            }
        }
        return mode;
    }

    private static char getQuoteChar(String textQualifier) {
        return textQualifier == null || textQualifier.trim().isEmpty() ?
                CSVParser.DEFAULT_QUOTE_CHARACTER : textQualifier.charAt(0);
    }

    public DataTypes getSqlType(ValueTypes type) {
        switch (type) {
            case INTEGER:
                return DataTypes.INTEGER;
            case NUMERIC:
                return DataTypes.NUMERIC;
            case BOOLEAN:
                return DataTypes.BOOLEAN;
            case BIT:
                return DataTypes.BOOLEAN;
            case DATE:
                return DataTypes.TIMESTAMP;
            case TEXT:
                return DataTypes.TEXT;
            default:
                return DataTypes.NVARCHAR;
        }
    }

    private Pair<ValueTypes, Integer> getType(String str) {
        if (str == null) return new Pair<>(ValueTypes.NONE, 0);
        Matcher m = NUMERIC_PATTERN.matcher(str);
        if (m.matches()) {
            try {
                Integer.parseInt(str);
                return new Pair<>(ValueTypes.INTEGER, str.length());
            } catch (NumberFormatException e) {
                // do nothing
            }
            try {
                Double.parseDouble(str);
                return new Pair<>(ValueTypes.NUMERIC, str.length());
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        ParsedDate dt = typeParser.parse(str, ParsedDate.class);
        if (dt != null) return new Pair<>(ValueTypes.DATE, str.length());

        Boolean bool = typeParser.parse(str, Boolean.class);
        if (bool != null) return new Pair<>(ValueTypes.BOOLEAN, str.length());

        return new Pair<>(ValueTypes.STRING, str.length());
    }

    public TypesContainer getTypes(String[][] rows, int sampleSize, int maxNumberColumns, boolean hasHeader) {
        TypeInfo[] types = new TypeInfo[maxNumberColumns];
        DataTypes[] sqlTypes = new DataTypes[maxNumberColumns];
        int[] lengths = new int[maxNumberColumns];

        int start = hasHeader ? 1 : 0;
        for (int i = start; i < sampleSize; i++) {
            String[] sampleRow = rows[i];
            for (int j = 0; j < maxNumberColumns; j++) {
                TypeInfo type = deduceDataType(sampleRow[j]);
                if (types[j] == null) {
                    types[j] = type;
                    sqlTypes[j] = getSqlType(type.getType());
                } else {
                    if (types[j].equals(type)) {
                        if (typeHierarchy.indexOf(type.getType()) > typeHierarchy.indexOf(types[j].getType())) {
                            types[j] = type;
                            sqlTypes[j] = getSqlType(type.getType());
                        }
                    }
                }
                int len = sampleRow[j].length();
                if (len > lengths[j]) {
                    lengths[j] = len;
                }
            }
        }
        return new TypesContainer(types, sqlTypes, lengths);
    }

    public TypesContainer getTypes(List<List<String>> rows, int sampleSize, int maxNumberColumns, boolean hasHeader) {
        int n = Math.min(rows.size(), sampleSize);
        String[][] data = new String[n][];
        for (int i = 0; i < n; i++) {
            List<String> row = rows.get(i);
            data[i] = row.toArray(new String[row.size()]);
        }
        return getTypes(data, sampleSize, maxNumberColumns, hasHeader);
    }

    private Map<String, ValueType> getValueTypeMap() {
        Map<String, ValueType> valueTypeMap = new HashMap<>();
        int i = 0;
        for (ValueTypes type : ValueTypes.values()) {
            valueTypeMap.put(type.name(), new ValueType(i, type.name()));
            i += 1;
        }
        return valueTypeMap;
    }

    /**
     * The delimiter /should/ occur the same number of times on each row.
     * However, due to malformed data, it may not. We don't want an all
     * or nothing approach, so we allow for small variations in this number.
     *
     *   1) build a table of the frequency of each character on every line.
     *   2) build a table of frequencies of this frequency (meta-frequency?),
     *      e.g. 'x occurred 5 times in 10 rows, 6 times in 1000 rows,
     *      7 times in 2 rows'
     *   3) use the mode of the meta-frequency to determine the /expected/
     *      frequency for that character
     *   4) find out how often the character actually meets that goal
     *   5) the character that best meets its goal is the delimiter
     *      For performance reasons, the data is evaluated in chunks, so it can
     *      try and evaluate the smallest portion of the data possible, evaluating
     *      additional chunks as necessary.
     *
     * @param data File data
     * @return metastore.models.FileParameters
     */
    public FileParameters guessDelimiter(String data, String lineEnding) {
        ObjectMapper mapper = new ObjectMapper();
        String[] rows = data.split(lineEnding);
        // Check in the two-byte UTF8 range
        Character[] cs = new Character[2048];
        for (int i = 0; i < 2048; i++) {
            cs[i] = (char)i;
        }

        int chunkLength = Math.min(10, rows.length);
        if (log.isDebugEnabled()) {
            log.debug("rows.length " + rows.length);
            log.debug("chunkLength " + chunkLength);
        }
        int iteration = 0;
        Map<Character, Map<Integer, Integer>> charFrequency = new HashMap<>();
        Map<Character, Integer[]> modes = new HashMap<>();
        Map<Character, Integer[]> delims = new HashMap<>();
        Character delim;
        boolean skipInitialSpace;
        int start = 0;
        int end = Math.min(chunkLength, rows.length);
        while (start < rows.length) {
            iteration += 1;
            for (String line : Arrays.asList(rows).subList(start, end)) {
                //for (Character ch : ascii) {
                for (Character ch : cs) {
                    Map<Integer, Integer> metaFrequency;
                    if (charFrequency.containsKey(ch)) {
                        metaFrequency = charFrequency.get(ch);
                    } else {
                        metaFrequency = new HashMap<>();
                    }
                    int freq = 0;
                    for (int i = 0; i < line.length(); i++) {
                        if (line.charAt(i) == ch) freq++;
                    }
                    if (metaFrequency.containsKey(freq)) {
                        metaFrequency.put(freq, metaFrequency.get(freq) + 1);
                    } else {
                        metaFrequency.put(freq, 1);
                    }
                    charFrequency.put(ch, metaFrequency);
                }
            }
            for (Map.Entry<Character, Map<Integer, Integer>> entry : charFrequency.entrySet()) {
                Map<Integer, Integer> metaFrequency = entry.getValue();
                if (!(metaFrequency.size() == 1 && metaFrequency.containsKey(0))) {
                    if (metaFrequency.size() > 0) {
                        Map.Entry<Integer, Integer> maxItem = null;
                        for (Map.Entry<Integer, Integer> item : metaFrequency.entrySet()) {
                            if (maxItem == null || item.getValue() > maxItem.getValue()) {
                                maxItem = item;
                            }
                        }
                        int sumOtherFreqs = 0;
                        Integer maxFreq = (maxItem == null ? null : maxItem.getKey());
                        Integer maxFreqVal = (maxItem == null ? 0 : maxItem.getValue());
                        for (Map.Entry<Integer, Integer> item : metaFrequency.entrySet()) {
                            if (!item.getKey().equals(maxFreq)) {
                                sumOtherFreqs += item.getValue();
                            }
                        }
                        modes.put(entry.getKey(), new Integer[] { maxFreq, maxFreqVal - sumOtherFreqs });
                    } else {
                        Map.Entry<Integer, Integer> it = metaFrequency.entrySet().iterator().next();
                        modes.put(entry.getKey(), new Integer[] { it.getKey(), it.getValue() });
                    }
                }
            }
            //int total = chunkLength * iteration;
            double total = end * iteration;

            if (log.isDebugEnabled()) {
                log.debug("modes:");
                try {
                    log.debug(mapper.writeValueAsString(modes));
                } catch (JsonProcessingException e) {
                    // ignore
                }
            }

            // (rows of consistent data) / (number of rows) = 100%
            double consistency = 1.0;

            // minimum consistency threshold
            double threshold = 0.9;

            while (delims.isEmpty() && consistency >= threshold) {
                for (Map.Entry<Character, Integer[]> entry : modes.entrySet()) {
                    Integer[] v = entry.getValue();
                    if (v[0] > 0 && v[1] > 0) {
                        if (log.isDebugEnabled()) {
                            log.debug(entry.getKey() + " " + v[1] + " / " + total + " = " + (v[1] / total) + " ~ " + consistency);
                        }
                        if ((v[1] / total) >= consistency) {
                            delims.put(entry.getKey(), v);
                        }
                    }
                }
                consistency -= 0.01;
            }
            if (delims.size() == 1) {
                delim = delims.keySet().iterator().next();
                String firstLine = rows[0];
                int delimCount = 0;
                for (int i = 0; i < firstLine.length(); i++) {
                    if (firstLine.charAt(i) == delim) {
                        delimCount += 1;
                    }
                }
                Pattern p = Pattern.compile(delim + " ");
                Matcher m = p.matcher(firstLine);
                int delimWithSpaceCount = 0;
                while (m.find()) delimWithSpaceCount++;
                skipInitialSpace = (delimCount == delimWithSpaceCount);
                return new FileParameters(delim.toString(), skipInitialSpace);
            }

            // analyze another chunkLength lines
            start = end;
            end += chunkLength;
            end = Math.min(end, rows.length);
        }
        if (delims.isEmpty()) {
            return new FileParameters();
        }

        // if there's more than one, fall back to a 'preferred' list
        for (Character ch : preferredColumnDelimiters) {
            if (delims.keySet().contains(ch)) {
                String firstLine = rows[0];
                int delimCount = 0;
                for (int i = 0; i < firstLine.length(); i++) {
                    if (firstLine.charAt(i) == ch) {
                        delimCount += 1;
                    }
                }
                Pattern p = Pattern.compile(ch + " ");
                Matcher m = p.matcher(firstLine);
                int delimWithSpaceCount = 0;
                while (m.find()) delimWithSpaceCount++;
                skipInitialSpace = (delimCount == delimWithSpaceCount);
                return new FileParameters(ch.toString(), skipInitialSpace);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("delims:");
            try {
                log.debug(mapper.writeValueAsString(delims));
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        // nothing else indicates a preference, pick the character that
        // dominates(?)
        Map.Entry<Character, Integer[]> maxEntry = null;
        for (Map.Entry<Character, Integer[]> entry : delims.entrySet()) {
            if (maxEntry == null || entry.getValue()[0] > maxEntry.getValue()[0]) {
                maxEntry = entry;
            }
        }
        delim = maxEntry.getKey();
        String firstLine = rows[0];
        int delimCount = 0;
        for (int i = 0; i < firstLine.length(); i++) {
            if (firstLine.charAt(i) == delim) {
                delimCount += 1;
            }
        }
        Pattern p = Pattern.compile(delim + " ");
        Matcher m = p.matcher(firstLine);
        int delimWithSpaceCount = 0;
        while (m.find()) delimWithSpaceCount++;
        skipInitialSpace = (delimCount == delimWithSpaceCount);
        return new FileParameters(delim.toString(), skipInitialSpace);
    }

    /**
     * Looks for text enclosed between two identical quotes (the probable
     * textQualifier) which are preceded and followed by the same character
     * (the probable delimiter).
     * For example:
     *                  ,'some text',
     *
     * The quote with the most wins, same with the delimiter. If there is
     * no textQualifier then the delimiter can't be determined this way.
     *
     * @param data File data
     * @return metastore.models.FileParameters
     */
    public FileParameters guessQuoteAndDelimiter(String data, String lineEnding) {
        String[] regexes = new String[] {
                "(?<delim>[^\\w" + lineEnding + "\"']+)(?<space> ?)(?<quote>[\"']).*?(\\k<quote>)(\\k<delim>)",
                "(?:^|" + lineEnding + ")(?<quote>[\"']).*?(\\k<quote>)(?<delim>[^\\w" + lineEnding + "\"']+)(?<space> ?)",
                "(?:^|" + lineEnding + ")(?<quote>[\"']).*?(\\k<quote>)(?:$|" + lineEnding + ")"
        };

        // embedded construction flags        meanings
        // flags
        // (?i)     Pattern.CASE_INSENSITIVE  Enables case-insensitive matching.
        // (?d)     Pattern.UNIX_LINES        Enables Unix lines mode.
        // (?m)     Pattern.MULTILINE         Enables multi line mode.
        // (?s)     Pattern.DOTALL            Enables "." to match line terminators.
        // (?u)     Pattern.UNICODE_CASE      Enables Unicode-aware case folding.
        // (?x)     Pattern.COMMENTS          Permits white space and comments in the pattern.
        // ---      Pattern.CANON_EQ          Enables canonical equivalence.
        //
        Matcher matches = null;
        boolean matchNotFound = true;
        if (log.isDebugEnabled()) {
            log.debug("Matching quote patterns");
        }
        for (String regex : regexes) {
            Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
            matches = p.matcher(data);
            if (matches.find(0)) {
                matchNotFound = false;
                break;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Match " + (matchNotFound ? "not found" : "found"));
        }
        if (matchNotFound) {
            return new FileParameters();
        }
        Map<String, Integer> quotes = new HashMap<>();
        Map<String, Integer> delims = new HashMap<>();
        int spaces = 0;
        String quote = matches.group("quote");
        if (quote != null) {
            if (quotes.containsKey(quote)) {
                quotes.put(quote, quotes.get(quote) + 1);
            } else {
                quotes.put(quote, 1);
            }
        }
        String delim = matches.group("delim");
        if (delim != null) {
            if (delims.containsKey(delim)) {
                delims.put(delim, delims.get(delim) + 1);
            } else {
                delims.put(delim, 1);
            }
            if (matches.group("space") != null) {
                spaces += 1;
            }
        }
        Map.Entry<String, Integer> maxEntry = null;
        for (Map.Entry<String, Integer> entry : quotes.entrySet()) {
            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            }
        }
        String textQualifier = (maxEntry == null ? "" : maxEntry.getKey());
        String columnDelimiter = "";
        boolean skipInitialSpace = false;
        if (!delims.isEmpty()) {
            maxEntry = null;
            for (Map.Entry<String, Integer> entry : delims.entrySet()) {
                if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                    maxEntry = entry;
                }
            }
            columnDelimiter = (maxEntry == null ? "" : maxEntry.getKey());
            skipInitialSpace = (delims.get(columnDelimiter) == spaces);
            if (lineEnding.equals(columnDelimiter)) { // most likely a file with a single column
                columnDelimiter = "";

            }
        }
        // if we see an extra quote between delimiters, we've got a
        // double quoted format
        String del = Pattern.quote(columnDelimiter);
        String delFirstChar = Pattern.quote(columnDelimiter.charAt(0) + "");
        String qot = Pattern.quote(textQualifier);
        String dqr = "(?m)((" + del + ")|^)\\W*" + qot + "[^" + delFirstChar + lineEnding + "]*" + qot + "[^" + delFirstChar + lineEnding + "]*" + qot + "\\W*((" + del + ")|$$)";
        Pattern p = Pattern.compile(dqr);
        Matcher m = p.matcher(data);
        boolean doubleQuoted = m.find(0) && (m.group(1) != null);
        return new FileParameters(textQualifier, doubleQuoted, columnDelimiter, skipInitialSpace);
    }

    /**
     * Creates a dictionary of types of data in each column. If any
     * column is of a single type (say, integers), *except* for the first
     * row, then the first row is presumed to be labels. If the type
     * can't be determined, it is assumed to be a string in which case
     * the length of the string is the determining factor: if all of the
     * rows except for the first are the same length, it's a header.
     * Finally, a 'vote' is taken at the end for each column, adding or
     * subtracting from the likelihood of the first row being a header.
     *
     * @param data File data
     * @return boolean
     */
    public boolean hasHeader(String[][] data) {
        String[] header = data[0];
        int lenColumns = header.length;
        Map<Integer, Pair<ValueTypes, Integer>> columnTypes = new HashMap<>();
        for (int i = 0; i < lenColumns; i++) {
            columnTypes.put(i, new Pair<>(ValueTypes.NONE, 0));
        }
        for (String[] row : data) {
            if (row.length == lenColumns) {
                for (int i = 0; i < lenColumns; i++) {
                    Pair<ValueTypes, Integer> thisType = getType(row[i]);
                    if (!thisType.equals(columnTypes.get(i))) {
                        columnTypes.put(i, thisType);
                    } else {
                        columnTypes.put(i, new Pair<>(ValueTypes.NONE, 0));
                    }
                }
            }
        }
        int hasHeaderVote = 0;
        for (Map.Entry<Integer, Pair<ValueTypes, Integer>> entry : columnTypes.entrySet()) {
            hasHeaderVote += testHeaderType(entry.getValue(), header[entry.getKey()]);
        }
        return hasHeaderVote > 0;
    }

    public boolean hasHeader(List<List<String>> sample) {
        int sampleSize = sample.size();
        String[][] data = new String[sampleSize][];
        for (int i = 0; i < sampleSize; i++) {
            List<String> row = sample.get(i);
            data[i] = row.toArray(new String[row.size()]);
        }
        return hasHeader(data);
    }

    public boolean isDefaultName(String name) {
        if (hasValue(name)) {
            for (ValueTypes type : typeHierarchy) {
                if (name.startsWith(type.toString().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String[] makeHeaderNames(TypeInfo[] types) {
        String[] header = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            header[i] = types[i].toString().toLowerCase() + "_" + (i + 1);
        }
        return header;
    }

    public boolean parseBoolean(String value) {
        return typeParser.parse(value, Boolean.class);
    }

    public ParsedDate parseDate(String value) {
        return typeParser.parse(value, ParsedDate.class);
    }

    private String[] readDateFormats(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> formats = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            formats.add(line);
        }
        String[] dateFormats = new String[formats.size()];
        dateFormats = formats.toArray(dateFormats);
        return dateFormats;
    }

    public static String readFileAsString(File file) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Reading file: " + file.getAbsolutePath());
        }
        StringBuilder sb = new StringBuilder();
        // Detect the annoying Byte Order Mark (BOM) at the beginning of files saved from
        // Microsoft documents such as Excel
        BOMInputStream in = new BOMInputStream(new FileInputStream(file), false);
        if (in.hasBOM()) {
            log.info("File " + file.getName() + " has Byte Order Mark");
        }
        int read;
        final byte[] bytes = new byte[1024];
        try {
            while ((read = in.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, read));
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }

    private DatasetInfo readJsonSample(String datasetName, String data) throws ExtractionException {
        try {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(data);
            Stack<JsonToken> stack = new Stack<>();
            List<Map<String, MyJsonValue>> objects = new ArrayList<>();
            Map<String, MyJsonValue> currentObject = new HashMap<>();
            Map<String, Integer> lengths = new HashMap<>();
            JsonToken token;
            while ((token = parser.nextToken()) != null && objects.isEmpty()) {
                switch (token) {
                    case START_OBJECT:
                        stack.push(token);
                        if (stack.size() == 1) {
                            currentObject = new HashMap<>();
                        }
                        break;

                    case END_OBJECT:
                        stack.pop();
                        if (stack.empty()) {
                            objects.add(currentObject);
                        }
                        break;

                    case FIELD_NAME:
                        if (stack.size() == 1) {
                            String key = parser.getCurrentName();
                            JsonToken t = parser.nextToken();
                            switch (t) {
                                case START_OBJECT:
                                    currentObject.put(key, new MyJsonValue(null, ValueTypes.OBJECT));
                                    break;

                                case START_ARRAY:
                                    currentObject.put(key, new MyJsonValue(null, ValueTypes.ARRAY));
                                    break;

                                case VALUE_NUMBER_FLOAT:
                                    currentObject.put(key, new MyJsonValue(parser.getFloatValue(), ValueTypes.NUMERIC));
                                    break;

                                case VALUE_NUMBER_INT:
                                    currentObject.put(key, new MyJsonValue(parser.getIntValue(), ValueTypes.INTEGER));
                                    break;

                                case VALUE_NULL:
                                    currentObject.put(key, new MyJsonValue(null, ValueTypes.NONE));
                                    break;

                                case VALUE_FALSE:
                                    currentObject.put(key, new MyJsonValue(false, ValueTypes.BOOLEAN));
                                    break;

                                case VALUE_TRUE:
                                    currentObject.put(key, new MyJsonValue(true, ValueTypes.BOOLEAN));
                                    break;

                                case VALUE_STRING:
                                    String value = parser.getValueAsString();
                                    if (hasValue(value)) {
                                        ParsedDate dt = parseDate(value);
                                        if (dt != null) {
                                            currentObject.put(key, new MyJsonValue(dt, ValueTypes.DATE));
                                            break;
                                        }
                                    }
                                    currentObject.put(key, new MyJsonValue(value, ValueTypes.STRING));
                                    int len = value.length();
                                    int maxLen = lengths.getOrDefault(key, 0);
                                    if (len > maxLen) {
                                        lengths.put(key, len);
                                    }
                                    break;
                            }
                        }
                }
            }
            FileDataSource fileDataSource = new FileDataSource();
            fileDataSource.setName(datasetName);
            fileDataSource.setFilepath(datasetName);
            fileDataSource.setFilenamePattern(datasetName);
            FileDataset fileDataset = new FileDataset();
            fileDataset.setName(datasetName);
            fileDataset.setDataSource(fileDataSource);
            fileDataset.setFileType(FileType.JSON);
            fileDataset.setBatch(true);

            Map<String, ValueType> valueTypeMap = getValueTypeMap();
            Map<String, DataType> dataTypeMap = getDataTypeMap();

            int i = 1;
            for (Map.Entry<String, MyJsonValue> entry : currentObject.entrySet()) {
                String columnName = entry.getKey();
                ValueTypes type = entry.getValue().getType();
                DataTypes sqlType = getSqlType(type);
                ValueType valueType = valueTypeMap.get(type.toString());

                FileColumn fileColumn = new FileColumn();
                fileColumn.setName(columnName);
                fileColumn.setDescription("Automatically created from import of " + datasetName);
                fileColumn.setDataset(fileDataset);
                fileColumn.setColumnIndex(i++);
                fileColumn.setValueType(valueType);
                fileColumn.setDataType(dataTypeMap.get(sqlType.toString()));
                fileColumn.setLength(lengths.getOrDefault(columnName, 0));
            }

            DatasetInfo datasetInfo = new DatasetInfo();
            List<ColumnInfo> columns = new ArrayList<>();
            i = 1;
            for (Map.Entry<String, MyJsonValue> entry : currentObject.entrySet()) {
                columns.add(new ColumnInfo(entry.getKey(), i++, entry.getValue().getType().toString(),
                        getSqlType(entry.getValue().getType()).toString(),
                        lengths.getOrDefault(entry.getKey(), 0)));
            }
            datasetInfo.setColumns(columns);
            datasetInfo.setFileType(FileType.JSON.toString());

            return datasetInfo;

        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            throw new ExtractionException(e.getMessage());
        }
    }

    public LinesContainer readLines(String data) {
        String[] lines = null;
        String lineEnding = null;
        double minVariance = Double.MAX_VALUE;
        for (String ending : lineEndings) {
            if (log.isDebugEnabled()) {
                log.debug("try ending [" + StringEscapeUtils.escapeJava(ending) + "]");
            }
            Pattern p = Pattern.compile(ending);
            String[] ls = p.split(data);
            double meanLength = getMeanLineLength(ls);
            double sd = Math.sqrt(getLineLengthVariance(ls, meanLength));
            String[] filtered = removeOutliers(ls, meanLength, sd);
            double newMeanLength = getMeanLineLength(filtered);
            double newVariance = getLineLengthVariance(filtered, newMeanLength);
            if (log.isDebugEnabled()) {
                log.debug("length=" + ls.length + ", var=" + newVariance);
            }
            if (ls.length > 1 && newVariance < minVariance) {
                minVariance = newVariance;
                lines = ls;
                lineEnding = ending;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("selected ending [" + StringEscapeUtils.escapeJava(lineEnding) + "]");
        }
        // Test line ending for files with a single line
        if (lineEnding == null) {
            for (String ending : lineEndings) {
                if (data.replaceAll("[^(" + ending + ")]+", "").length() > 0) {
                    lineEnding = ending;
                    break;
                }
            }
        }
        return new LinesContainer(lines, lineEnding);
    }

    private RowsContainer readRows(CSVReader reader, int maxSampleSize) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("reading rows");
        }
        List<String[]> rowList = new ArrayList<>();
        int maxNumberColumns = 0;
        int k = 0;
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            rowList.add(nextLine);
            maxNumberColumns = Math.max(maxNumberColumns, nextLine.length);
            k += 1;
            if (k == maxSampleSize) break;
        }
        int n = Math.min(maxSampleSize, rowList.size());
        if (log.isDebugEnabled()) {
            log.debug("rows " + n);
            log.debug("cols " + maxNumberColumns);
        }
        String[][] rows = new String[n][maxNumberColumns];
        for (int i = 0; i < n; i++) {
            String[] row = rowList.get(i);
            System.arraycopy(row, 0, rows[i], 0, row.length);
        }
        return new RowsContainer(rows, maxNumberColumns);
    }

    private String[] removeOutliers(String[] lines, double mean, double sd) {
        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            // approximate by excluding lines with lengths greater than or equal to
            // 2 standard deviations from the mean
            // Chauvenet's criterion is a common method but requires a normal distribution function
            if (Math.sqrt((mean - line.length()) * (mean - line.length())) / sd < 2) {
                filtered.add(line);
            }
        }
        return filtered.toArray(new String[filtered.size()]);
    }

    public FileParameters sniff(String data, String lineEnding) {
        if (log.isDebugEnabled()) {
            log.debug("Guessing text qualifier and delimiter");
        }
        FileParameters params1 = guessQuoteAndDelimiter(data, lineEnding);
        String guessedDelimiter = params1.getColumnDelimiter();
        if (log.isDebugEnabled() && !guessedDelimiter.isEmpty()) {
            log.debug("delimiter is [" + StringEscapeUtils.escapeJava(guessedDelimiter) + "](" +
                    (int) guessedDelimiter.charAt(0) + ") (length=" +
                    guessedDelimiter.length() + ")");
        }
        if (guessedDelimiter.isEmpty() || guessedDelimiter.charAt(0) == 0) {
            FileParameters params2 = guessDelimiter(data, lineEnding);
            if (params2.getColumnDelimiter().isEmpty()) {
                // TODO
                // limit to 20 lines
                FileParameters params3 = findMultiCharSequences(data, lineEnding);
                if (params3.getColumnDelimiter().isEmpty()) {
                    log.warn("Could not determine delimiter - returning null");
                    return null;
                }
                params1.setColumnDelimiter(params3.getColumnDelimiter());
                params1.setSkipInitialSpace(params3.isSkipInitialSpace());
            }
            params1.setColumnDelimiter(params2.getColumnDelimiter());
            params1.setSkipInitialSpace(params2.isSkipInitialSpace());
        }
        return params1;
    }

    private int testHeaderType(Pair<ValueTypes, Integer> type, String cell) {
        if (cell == null) return 0;
        if (type.l == ValueTypes.STRING) {
            return (cell.length() == type.r) ? -1 : 1;
        }
        if (type.l == ValueTypes.INTEGER) {
            try {
                Integer i = Integer.parseInt(cell);
                // check that the decimal place is not truncated
                if (i.toString().equals(cell)) {
                    return -1;
                }
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        if (type.l == ValueTypes.NUMERIC) {
            try {
                Double.parseDouble(cell);
                return -1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        if (type.l == ValueTypes.DATE) {
            ParsedDate dt = typeParser.parse(cell, ParsedDate.class);
            return (dt == null) ? 1 : -1;
        }
        if (type.l == ValueTypes.BOOLEAN) {
            Boolean bool = typeParser.parse(cell, Boolean.class);
            return (bool == null) ? 1 : -1;
        }
        return 0;
    }

    private static class MyJsonValue {

        private Object value;
        private ValueTypes type;

        MyJsonValue(Object value, ValueTypes type) {
            this.value = value;
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public ValueTypes getType() {
            return type;
        }
    }

    private static class RowsContainer {
        String[][] rows;
        int maxNumberColumns;

        RowsContainer(String[][] rows, int maxNumberColumns) {
            this.rows = rows;
            this.maxNumberColumns = maxNumberColumns;
        }
    }
}
