package cn.smilefamily.moduleexample.common;

public interface Datasource {
    public default Connection getConnection(){
        return new Connection();
    }
}
