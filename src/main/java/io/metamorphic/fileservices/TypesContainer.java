package io.metamorphic.fileservices;

/**
 * Created by markmo on 11/07/2015.
 */
public class TypesContainer {

    public TypeInfo[] types;
    public DataTypes[] sqlTypes;
    public int[] lengths;

    public TypesContainer(TypeInfo[] types, DataTypes[] sqlTypes, int[] lengths) {
        this.types = types;
        this.sqlTypes = sqlTypes;
        this.lengths = lengths;
    }
}
