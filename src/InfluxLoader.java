import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class InfluxLoader {
    private InfluxDBClient client;
    private final String org = "";
    private final String token = "";
    private final String bucket = "";
    private final String url = "";

    //Поля, выбранные для отчета, ключ - тип данных, значение -  HashMap, где ключ - поле, значение псевдоним поля
    private HashMap<String,HashMap<String, String>> selectedFieldsSet = new HashMap<String,HashMap<String, String>>();
    //Данные в отдельной мапе, чтобы удобнее было. Ключ - тип данных, значение HashMap, где
    //ключ - время, значение HashMap, где ключ - поле, значение- значение поля
    private HashMap<String, HashMap<String, HashMap<String, Double>>> dataMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>();

    Instant beginTime;
    Instant endTime;
    int timeZone;
    String imei;

    public void setTimeZone(int timeZone) {
        this.timeZone = timeZone;
    }
    public void setBeginTime(Instant beginTime) {
        this.beginTime = beginTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public void clearFieldsInQuery(){
        selectedFieldsSet.clear();
    }
    public void addFieldInQuery(String type, String field, String pseudo) {
        if (selectedFieldsSet.containsKey(type)) {
            selectedFieldsSet.get(type).put(field, pseudo);
        }
        else{
            HashMap<String, String> valuesList = new HashMap<String, String>();
            valuesList.put(field, pseudo);
            selectedFieldsSet.put(type, valuesList);
        }
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public InfluxLoader() {
        createConnection();
    }

    private void createConnection() {
        client = InfluxDBClientFactory.create(url, token.toCharArray(), this.org, bucket);
    }

    public void closeInfluxLoader() {
        this.client.close();
    }

    public void readData() {
        dataMap.clear();
        System.out.println("Begin readData");
        String query = "from(bucket: \"company\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"%s\") " +
                "|> filter(fn: (r) => %s) " +
                "|> filter(fn: (r) => %s)";

        String typesQuery = "";
        String fieldsQuery = "";
        Iterator itT = selectedFieldsSet.keySet().iterator();
        while (itT.hasNext()) {
            String type = (String) itT.next();
            //System.out.println("it = " + type);
            typesQuery += "r[\"type\"] == \"" + type + "\" or ";

            Iterator itF = selectedFieldsSet.get(type).keySet().iterator();
            while (itF.hasNext()) {
                String field = (String) itF.next();
                //System.out.println("itF = " + field);
                fieldsQuery += "r[\"_field\"] == \"" + field + "\" or ";
            }
        }


        typesQuery = typesQuery.substring(0, typesQuery.length() - 4);
        fieldsQuery = fieldsQuery.substring(0, fieldsQuery.length() - 4);

        System.out.println("typesQuery1 = " + typesQuery);
        System.out.println("fieldsQuery1 = " + fieldsQuery);

        int mod = Math.abs(timeZone);
        beginTime = timeZone < 0 ? beginTime.plus(Duration.ofHours(mod)) : beginTime.minus(Duration.ofHours(mod));
        endTime = timeZone < 0 ? endTime.plus(Duration.ofHours(mod)) : endTime.minus(Duration.ofHours(mod));
        String fluxQuery = String.format(query, beginTime, endTime, imei, typesQuery, fieldsQuery);
        System.out.println(fluxQuery);

        try {
            List<RecordQuery> parameterList = client.getQueryApi().query(fluxQuery, RecordQuery.class);
            System.out.println("Count RecordQuery = " + parameterList.size());
            Iterator itQuery = parameterList.iterator();
            while (itQuery.hasNext()) {
                RecordQuery record = (RecordQuery) itQuery.next();
                Instant t = record.time;
                t = timeZone > 0 ? t.plus(Duration.ofHours(mod)) : t.minus(Duration.ofHours(mod));
                System.out.println("End RecordQuery = " + record);
                if (!dataMap.containsKey(record.type)) {
                    HashMap<String, HashMap<String, Double>> timeMap = new HashMap<String, HashMap<String, Double>>();
                    HashMap<String, Double> valueMap = new HashMap<String, Double>();
                    valueMap.put(record.name, record.value);

                    timeMap.put(t.toString(), valueMap);
                    //timeMap.put(record.time.toString(), valueMap);

                    dataMap.put(record.type, timeMap);
                } else {
                    if (!dataMap.get(record.type).containsKey(t.toString())) {
                        HashMap<String, Double> valueMap = new HashMap<String, Double>();
                        valueMap.put(record.name, record.value);
                        dataMap.get(record.type).put(t.toString(), valueMap);
                    }
                    else{
                        dataMap.get(record.type).get(t.toString()).put(record.name, record.value);
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Method readData() throw an exception: " + e.getMessage());
        }
        System.out.println("End readData");
    }

    public void createFile() {
        System.out.println("Begin createFile");

        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        String var10000 = path.substring(0, path.length() - 1);
        LocalDateTime currentDateTime = LocalDateTime.now();
        String fn = currentDateTime.getYear() + "-" + currentDateTime.getMonthValue() + "-" + currentDateTime.getDayOfMonth() +
                "-" + currentDateTime.getHour() + "-" + currentDateTime.getMinute();
        String fileLocation = var10000 + "out_" + fn + ".xlsx";

        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            Row row;

            Sheet allSheet = workbook.createSheet("All Fields");
            allSheet.setColumnWidth(0, 6000);
            allSheet.setColumnWidth(1, 4000);

            Row allSheetHeader = allSheet.createRow(0);
            Cell cell = allSheetHeader.createCell(0);
            cell.setCellValue("Date Time");
            HashMap<String, Integer> cellAllTimes = new HashMap<String, Integer>();
            HashMap<String, Integer> cellAllNames = new HashMap<String, Integer>();

            Iterator itType = dataMap.keySet().iterator();
            while (itType.hasNext()) {
                String sheetName = (String) itType.next();
                System.out.println("Create sheet " + sheetName);

                Sheet sheet = workbook.createSheet(sheetName);
                sheet.setColumnWidth(0, 6000);
                sheet.setColumnWidth(1, 4000);

                Row header = sheet.createRow(0);
                Cell headerCell = header.createCell(0);
                headerCell.setCellValue("Date Time");

                Iterator itTime = dataMap.get(sheetName).keySet().iterator();
                int cntRow = 1;
                HashMap<String, Integer> cellNames = new HashMap<String, Integer>();
                while (itTime.hasNext()) {
                    String time = (String) itTime.next();

                    Row allRow;
                    Cell allCell;
                    if(cellAllTimes.containsKey(time)){
                        allRow = allSheet.getRow(cellAllTimes.get(time));
                    }
                    else{
                        cellAllTimes.put(time, cellAllTimes.size() + 1);
                        allRow = allSheet.createRow(cellAllTimes.size());
                        cell = allRow.createCell(0);
                        cell.setCellValue(time);
                    }

                    row = sheet.createRow(cntRow);
                    cell = row.createCell(0);
                    cell.setCellValue(time);
                    cntRow++;

                    Iterator itName = dataMap.get(sheetName).get(time).keySet().iterator();
                    int cntCell = 1;
                    while (itName.hasNext()) {
                        String name = (String) itName.next();

                        if(cellAllNames.containsKey(name)){
                            cell = allRow.createCell(cellAllNames.get(name));
                            cell.setCellValue(dataMap.get(sheetName).get(time).get(name));
                        }
                        else{
                            cellAllNames.put(name, cellAllNames.size() + 1);
                            headerCell = allSheetHeader.createCell(cellAllNames.size());
                            headerCell.setCellValue(selectedFieldsSet.get(sheetName).get(name));

                            cell = allRow.createCell(cellAllNames.size());
                            cell.setCellValue(dataMap.get(sheetName).get(time).get(name));
                        }

                        headerCell = header.createCell(cntCell);
                        headerCell.setCellValue(selectedFieldsSet.get(sheetName).get(name));

                        cell = row.createCell(cntCell);
                        cell.setCellValue(dataMap.get(sheetName).get(time).get(name));
                        cntCell++;
                    }
                }
            }

            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            System.out.println("Method createFile() throw an exception: " + e.getMessage());
        }

        System.out.println("End createFile");
    }

}
