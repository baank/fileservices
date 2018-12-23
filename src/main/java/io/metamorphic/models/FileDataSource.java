package io.metamorphic.models;

/**
 * Created by markmo on 28/03/2015.
 */
public class FileDataSource extends DataSource {

    private String network;

    private String filepath;

    private String filenamePattern;

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getFilenamePattern() {
        return filenamePattern;
    }

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }
}
