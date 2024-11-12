import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;

class MainWindow extends JFrame{
    private String fileNameForFields = "db_fields_pseudo.txt";
    private HashMap<String, String> fieldsPseudo = new HashMap<String, String>();
    private JDatePickerImpl datePickerBegin, datePickerEnd;
    private JTextField textFieldIMEI, textFieldTimeZone;
    private JTextField timeBeginField, timeEndField;

    private InfluxLoader influxLoader;

    private ArrayList<String> arrayTypes;
    private ArrayList<ArrayList<String>> arrayFieldsLists;
    private ArrayList<JList> arrayLists = new ArrayList<JList>();
    public MainWindow() {
        createWindow();
    }

    public MainWindow(String name) {
        super(name);
        influxLoader = new InfluxLoader();
        arrayTypes = new ArrayList<String>();
        arrayFieldsLists = new ArrayList<ArrayList<String>>();

        createWindow();
    }

    private void createWindow(){
        loadFieldsFromFile(fileNameForFields);
        createGUI();

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                influxLoader.closeInfluxLoader();
            }
            public void windowClosing(WindowEvent e) {
                influxLoader.closeInfluxLoader();
            }
        });
    }

    private void createGUI(){
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 10, 10, 10);

        JPanel mainPanel = new JPanel();

        constraints.gridx = 0;
        constraints.gridy = 0;
        mainPanel.add(createDatesPanel(), constraints);

        constraints.gridx = 1;
        mainPanel.add(createImeiPanel(), constraints);

        constraints.gridy = 1;
        mainPanel.add(createTimezonePanel(), constraints);

        constraints.gridy = 2;
        mainPanel.add(createFieldsListPanel(), constraints);

        constraints.gridy = 3;
        mainPanel.add(createButtonsPanel(), constraints);

        getContentPane().add(mainPanel);
    }

    private JPanel createImeiPanel() {
        JPanel imeiPanel = new JPanel();
        imeiPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "IMEI"));

        textFieldIMEI = new JTextField("");
        textFieldIMEI.setColumns(15);//15
        imeiPanel.add(textFieldIMEI);
        return imeiPanel;
    }

    private JPanel createTimezonePanel() {
        JPanel zonePanel = new JPanel();
        zonePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Часовой пояс"));

        textFieldTimeZone = new JTextField("+5");
        textFieldTimeZone.setColumns(15);//15
        zonePanel.add(textFieldTimeZone);
        return zonePanel;
    }

    private JPanel createFieldsListPanel() {
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Выбор данных"));

        int i = 0;
        for(String type : arrayTypes){
            DefaultListModel<String> listModel = new DefaultListModel<>();
            listModel.addAll(arrayFieldsLists.get(i));

            JList<String> fieldsList = new JList<>(listModel);
            fieldsList.setName(type);
            fieldsList.setFixedCellWidth(110);
            fieldsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            fieldsPanel.add(new JScrollPane(fieldsList));
            arrayLists.add(fieldsList);
            /*
            fieldsList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    //influxLoader.addTypeInQuery(fieldsList.getName());
                    System.out.println("Select type = " + fieldsList.getSelectedValuesList());
                    System.out.println("Select = " + e.getLastIndex() + "   :" + fieldsList.getModel().getElementAt(e.getLastIndex()));
                    //influxLoader.addFieldInQuery(fieldsList.getModel().getElementAt(e.getLastIndex()));
                    influxLoader.addFieldInQuery(fieldsList.getName(), fieldsPseudo.get(fieldsList.getModel().getElementAt(e.getLastIndex())), fieldsList.getModel().getElementAt(e.getLastIndex()));
                }
            });

             */
            i++;
        }
        return fieldsPanel;
    }

    private JPanel createDatesPanel(){
        JLabel labelDateBegin = new JLabel("Дата/Время начала: ");
        JLabel labelDateEnd = new JLabel("Дата/Время окончания: ");

        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");

        LocalDate currentDate = LocalDate.now().minusDays(1);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.gridx = 0;
        constraints.gridy = 0;

        JPanel selectDatePanel = new JPanel(new GridBagLayout());
        selectDatePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Выбор периода"));

        constraints.gridx = 0;
        constraints.gridy = 0;
        selectDatePanel.add(labelDateBegin, constraints);

        UtilDateModel modelBegin = new UtilDateModel();
        JDatePanelImpl datePanelBegin = new JDatePanelImpl(modelBegin, p);
        datePanelBegin.getModel().setDate(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth());
        datePanelBegin.getModel().setSelected(true);
        datePickerBegin = new JDatePickerImpl(datePanelBegin, new DateLabelFormatter());
        constraints.gridx = 1;
        selectDatePanel.add(datePickerBegin, constraints);
        timeBeginField = new JTextField("09:00");
        timeBeginField.setPreferredSize(new Dimension(100, 27));
        constraints.gridx = 2;
        selectDatePanel.add(timeBeginField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        selectDatePanel.add(labelDateEnd, constraints);

        currentDate = LocalDate.now();
        UtilDateModel modelEnd = new UtilDateModel();
        JDatePanelImpl datePanelEnd = new JDatePanelImpl(modelEnd, p);
        datePanelEnd.getModel().setDate(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth());
        datePanelEnd.getModel().setSelected(true);

        datePickerEnd = new JDatePickerImpl(datePanelEnd, new DateLabelFormatter());
        constraints.gridx = 1;
        selectDatePanel.add(datePickerEnd, constraints);
        timeEndField = new JTextField("09:10");
        timeEndField.setPreferredSize(new Dimension(100, 27));
        constraints.gridx = 2;
        selectDatePanel.add(timeEndField, constraints);

        return selectDatePanel;
    }

    private JPanel createButtonsPanel(){
        JPanel buttonPanel = new JPanel();
        JButton readBtn = new JButton("Получить данные из базы");

        buttonPanel.add(readBtn);
        readBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                onBtnReadData();
            }
        });

        JButton createBtn = new JButton("Сформировать отчет (.xlsx)");

        buttonPanel.add(createBtn);
        createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                onBtnCreateFile();
            }
        });

        return buttonPanel;
    }

    private void onBtnReadData(){
        if(influxLoader == null)
            return;

        influxLoader.clearFieldsInQuery();
        for(JList fList : arrayLists){
            for(int ind : fList.getSelectedIndices()){
                String str = (String) fList.getModel().getElementAt(ind);
                System.out.println("Select = " + fList.getName() + "  " + ind + "  " + str);
                influxLoader.addFieldInQuery(fList.getName(), fieldsPseudo.get(str), str);
            }
        }

        //String formatDateTime = "%s-%s-%sT%s:00Z";
        String formatDateTime = "%02d-%s-%02dT%s:00Z";
        int m = datePickerBegin.getModel().getMonth() + 1;
        String mS = (m > 9) ? "" + m : "0" + m;
        String dateTime = String.format(formatDateTime, datePickerBegin.getModel().getYear(),  mS, datePickerBegin.getModel().getDay(), timeBeginField.getText());
        System.out.println("dateTime: " + dateTime);
        Instant instant = Instant.parse(dateTime);
        influxLoader.setBeginTime(instant);

        try
        {
            int tZ = Integer.parseInt(textFieldTimeZone.getText());
            influxLoader.setTimeZone(tZ);
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }

        m = datePickerEnd.getModel().getMonth() + 1;
        mS = (m > 9) ? "" + m : "0" + m;
        dateTime = String.format(formatDateTime, datePickerEnd.getModel().getYear(),  mS, datePickerEnd.getModel().getDay(), timeEndField.getText());
        instant = Instant.parse(dateTime);
        influxLoader.setEndTime(instant);

        influxLoader.setImei(textFieldIMEI.getText());
        influxLoader.readData();
    }

    private void onBtnCreateFile(){
        if(influxLoader == null)
            return;

        influxLoader.createFile();
    }

    private void loadFieldsFromFile(String fileName){
        try {
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int i = -1;
            while((line = br.readLine()) != null){
                if(!line.trim().isEmpty()) {
                    if(line.startsWith("\t")){
                        System.out.println("str = " + line.trim());
                        String arr[] = line.trim().split("\\*");
                        System.out.println("str = " + line.trim());
                        arrayFieldsLists.get(i).add(arr[1]);
                        fieldsPseudo.put(arr[1], arr[0]);
                    }
                    else{
                        arrayTypes.add(line);
                        ArrayList<String> fields = new ArrayList<String>();
                        arrayFieldsLists.add(fields);
                        i++;
                    }
                }
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            System.out.println("Method loadFieldsFromFile() throw an exception: " + e.getMessage());
        }
    }
}


