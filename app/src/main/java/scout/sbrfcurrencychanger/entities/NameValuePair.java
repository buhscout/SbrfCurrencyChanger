package scout.sbrfcurrencychanger.entities;

/**
 * Created by goncharovka on 29.02.2016.
 */
public class NameValuePair {

    private String mName;
    private String mValue;

    public NameValuePair(String name, String value) {
        mName = name;
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public String getValue() {
        return mValue;
    }
}
