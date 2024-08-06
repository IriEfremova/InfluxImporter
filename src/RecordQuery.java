import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

import java.time.Instant;

@Measurement(
        name = "record_query"
)
public class RecordQuery {
    @Column(
            name = "_table"
    )
    public String table;
    @Column(
            name = "_volume"
    )
    public String volume;
    @Column(
            name = "_timeBegin"
    )
    public Instant timeBegin;
    @Column(
            name = "_timeEnd"
    )
    public Instant timeEnd;
    @Column(
            name = "_time"
    )
    public Instant time;
    @Column(
            name = "_value"
    )
    public Double value;
    @Column(
            name = "_field"
    )
    public String name;
    @Column
    public Double imei;
    @Column(
            name = "type"
    )
    public String type;

    public RecordQuery() {
    }
}
