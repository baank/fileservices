package io.metamorphic.models;

/**
 * Created by markmo on 29/03/2015.
 */
public class FileColumn extends DataColumn {

    private FileDataset dataset;

    private Record record;


    @Override
    public FileDataset getDataset() {
        return dataset;
    }

    public void setDataset(FileDataset dataset) {
        this.dataset = dataset;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }
}
