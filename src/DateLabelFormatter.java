import javax.swing.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
    private final String datePattern = "dd-MM-YYYY";
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-YYYY");

    public DateLabelFormatter() {
    }

    public Object stringToValue(String text) throws ParseException {
        return this.dateFormatter.parseObject(text);
    }

    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar)value;
            return this.dateFormatter.format(cal.getTime());
        } else {
            return "";
        }
    }
}