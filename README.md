# fileservices

Extract structure from flat files

(Note: The test data is bogus. https://support.spatialkey.com/spatialkey-sample-csv-data/)

Example usage - extract metadata as DDL:

    String dataSourceName = "test";
    String datasetName = "sales";
    String sampleFilePath = "/Users/mark/src/fileservices/src/test/resources/SalesJan2009.csv";
    String data = FileServiceImpl.readFileAsString(new File(sampleFilePath));
    DatasetInfo info = svc.extractMetadata(dataSourceName, datasetName, data);
    
    // output DDL file to stdout
    System.out.println(info.toDDL());

 
Example usage (continued) - extract configuration properties files:
    
    Properties props = new Properties();
    // common platform properties
    InputStream propsFile = FileUtil.class.getResourceAsStream("/.env");
    props.load(propsFile);
    
    // output ingestion properties file to stdout
    System.out.println(info.toIngestionProperties(props));
    
    // output curation properties file to stdout
    System.out.println(info.toCurationProperties(props));
    
    // output file parsing properties file to stdout
    System.out.println(info.toFileProperties());


Example usage - extract metadata as DDL from command line:

    cd <fileservices_root_dir>
    ./gradlew clean build
    java -cp build/libs/fileservices-1.0.jar io.metamorphic.FileUtil test sales ~/src/fileservices/src/test/resources/SalesJan2009.csv curate
    
Arguments:

1. datasource name (no spaces, illegal chars)
2. dataset name (no spaces, illegal chars)
3. sample file path
4. output: one of ["ingest", "curate", "file", "DDL"] (case insensitive)

Note: an environment properties file is required on the classloader path,
containing common platform properties for inclusion in output files.
See 'src/main/resources/template.env' for an example. The file must be
renamed to '.env'.


Example usage - determine file characteristics:

    FileService fs = new FileServiceImpl();

    String data = readFileAsString(file);

    // check that we're not handling a JSON file instead
    Pattern startJsonFilePattern = Pattern.compile("^\\s*[\\[\\{]", Pattern.MULTILINE);
    Matcher matcher = startJsonFilePattern.matcher(data);

    if (matcher.find()) {
        if (log.isDebugEnabled()) {
            log.debug("Reading JSON");
        }
        return readJsonSample(name, data);
    }
    if (log.isDebugEnabled()) {
        log.debug("Reading Delimited");
    }
    
    // strip blank lines at the start of the file
    data = data.replaceAll("^\\s+", "");

    // infer the line ending
    LinesContainer lc = fs.readLines(data);
    String[] lines = lc.lines;
    String lineEnding = lc.lineEnding;

    if (log.isDebugEnabled()) {
        log.debug("\nline ending [" + StringEscapeUtils.escapeJava(lineEnding) + "]");
    }

    // infer file parameters
    FileParameters fileParameters = fs.sniff(data, lineEnding);

    if (fileParameters == null) {
        return new DatasetInfo("Could not determine file parameters");
    }
    

Returns a `FileParameters` object containing:
* Text Qualifier (quoting schema)
* Is Double Quoting used?
* Column Delimiter
* Has Header?
* Line Terminator
 
 
## Building the project

To build the project:

    ./gradlew clean build
    
Assumes use of Artifactory, so Artifactory host and user variables must be set in gradle config.


## Dependencies

* https://github.com/metamorphic/metamorphic-commons
* Jackson
* commons-lang
* commons-logging


## Setting up IntelliJ

1. Enable the Gradle plugin and import the project
2. Right-click the `fileservices` module, select "Open Module Settings" from the context menu,
   click on the plus sign above the left-hand module list to add a module, select "Import Module",
   select the root of the _metamorphic-commons_ project, keep the default settings in the setup 
   wizard, give it the name "io.metamorphic.commons".
3. Right-click the `fileservices` module again. This time, select the "main" sub-module, click on 
   the "Dependencies" tab, click on the plus sign at the bottom, select "3 Module Dependency...", 
   then select the commons module added above.


## Setting up Gradle

### Setup Artifactory

1. Download Artifactory from [JFrog website](https://jfrog.com/open-source/). Unzip to a directory.
2. Change to the Artifactory directory, and run `bin/artifactory.sh`
3. Open the [Artifactory Console](http://localhost:8081/artifactory). It will start with a wizard
   to setup. On one of the setup pages, choose Maven as a repository to setup.
   
### Setup Artifactory Gradle Plugin

1. Add the following properties to `~/.gradle/gradle.properties`:

    artifactory_user=admin
    artifactory_password=<password>  # the password you created in the Artifactory setup
    artifactory_url=http://localhost:8081/artifactory
    org.gradle.caching=true
    gradle.cache.push=false
    artifactory_contextUrl=http://localhost:8081/artifactory
    artifactory_pluginsUrl=http://localhost:8081/artifactory/jcenter
