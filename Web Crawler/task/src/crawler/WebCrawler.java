package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class WebCrawler extends JFrame {

    public WebCrawler() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setTitle("Web Crawler");
        setVisible(true);

        final var url_label = new JLabel("URL:  ");
        final var url_text = new JTextField("");
        url_text.setName("UrlTextField");
        final var parse_button = new JButton("Parse");
        parse_button.setName("RunButton");
        final var title_label = new JLabel("Title:  ");
        final var title_label_text = new JLabel();
        title_label_text.setName("TitleLabel");
        final String[] column_names = {"URL", "Title"};
        final var title_table_model = new DefaultTableModel(column_names, 0);
        final var title_table = new JTable(title_table_model);
        title_table.setName("TitlesTable");
        final var export_label = new JLabel("Export: ");
        final var export_text = new JTextField();
        export_text.setName("ExportUrlTextField");
        final var export_button = new JButton("Save");
        export_button.setName("ExportButton");

        initUI(url_label, url_text, parse_button, title_label, title_label_text, title_table, export_label, export_text, export_button);
        initParser(parse_button, url_text, title_label_text, title_table_model);
        initSave(export_text, export_button, title_table_model);

        pack();
        setSize(800, 600);

        title_table.disable();
    }

    private void initUI(JLabel url_label, JTextField url_text, JButton parse_button, JLabel title_label, JLabel title_label_text, JTable title_table, JLabel export_label, JTextField export_text, JButton export_button) {
        final var user_input_pane = new JPanel();
        final var user_input_layout = new GroupLayout(user_input_pane);
        user_input_pane.setLayout(user_input_layout);
        user_input_layout.setAutoCreateGaps(true);
        user_input_layout.setAutoCreateContainerGaps(true);
        user_input_layout.setHorizontalGroup(
                user_input_layout.createSequentialGroup()
                        .addGroup(user_input_layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(url_label)
                                .addComponent(title_label)
                        )
                        .addGroup(user_input_layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(url_text)
                                .addComponent(title_label_text)
                        )
                        .addComponent(parse_button)
        );
        user_input_layout.setVerticalGroup(
                user_input_layout.createSequentialGroup()
                        .addGroup(user_input_layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(url_label)
                                .addComponent(url_text)
                                .addComponent(parse_button)
                        )
                        .addGroup(user_input_layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(title_label)
                                .addComponent(title_label_text)
                        )
        );
        add(user_input_pane, BorderLayout.PAGE_START);

        final var title_table_pane = new JScrollPane(title_table);
        add(title_table_pane, BorderLayout.CENTER);

        final var export_input_pane = new JPanel();
        final var export_input_layout = new GroupLayout(export_input_pane);
        export_input_pane.setLayout(export_input_layout);
        export_input_layout.setAutoCreateGaps(true);
        export_input_layout.setAutoCreateContainerGaps(true);
        export_input_layout.setHorizontalGroup(
                export_input_layout.createSequentialGroup()
                        .addComponent(export_label)
                        .addComponent(export_text)
                        .addGroup(export_input_layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(export_button)
                        )
        );
        export_input_layout.setVerticalGroup(
                export_input_layout.createSequentialGroup()
                        .addGroup(export_input_layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(export_label)
                                .addComponent(export_text)
                                .addComponent(export_button)
                        )
        );
        add(export_input_pane, BorderLayout.PAGE_END);
    }

    private void initParser(JButton parse_button, JTextField url_text, JLabel title_label_text, DefaultTableModel title_table_model) {
        parse_button.addActionListener(e -> {
            try {
                while (title_table_model.getRowCount() > 0) {
                    title_table_model.removeRow(title_table_model.getRowCount() - 1);
                }

                final var url = url_text.getText();
                final var input = new URL(url);
                var protocol = input.getProtocol();
                final var input_stream = input.openConnection();
                input_stream.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
                final var reader = new BufferedReader(new InputStreamReader(input_stream.getInputStream(), StandardCharsets.UTF_8));

                StringBuilder nextLine;

                var title_pattern = Pattern.compile("(?Uis).*?<title[^>]*>(.*?)(?:</title>.*?)?");
                var href_pattern = Pattern.compile("(?Ui).*<a\\s(?:[^>]+//s)?href=([\"'])([^\"']+)\\1[^>]*>(?:.+?</a>)?.*");
                while ((nextLine = Optional.ofNullable(reader.readLine()).map(StringBuilder::new).orElse(null)) != null) {
                    var title_matcher = title_pattern.matcher(nextLine);
                    if (title_matcher.matches()) {
                        while (!nextLine.toString().contains("</title>")) {
                            var next = reader.readLine();
                            if (next != null) nextLine.append(next);
                            else throw new IOException("Invalid <title>: " + nextLine);
                        }
                        title_label_text.setText(title_matcher.group(1));
                    }
                    var href_matcher = href_pattern.matcher(nextLine);
                    if (href_matcher.matches()) {
                        var link = href_matcher.group(2);
                        var relative_pattern = Pattern.compile("(?Ui)^([^/]+)");
                        var relative_matcher = relative_pattern.matcher(link);
                        var no_protocol_pattern = Pattern.compile("(?Ui)^//.+");
                        var no_protocol_matcher = no_protocol_pattern.matcher(link);
                        var nosource_slash_pattern = Pattern.compile("(?Ui)^/[^/]+.*");
                        var nosource_slash_matcher = nosource_slash_pattern.matcher(link);
                        var nosource_noslash_pattern = Pattern.compile("(?Ui)^[^/]+.+/[^/]*");
                        var nosource_noslash_matcher = nosource_noslash_pattern.matcher(link);
                        URLConnection connection = null;
                        try {
                            connection = new URL(link).openConnection();
                        }
                        catch (IOException error) {
                            if (relative_matcher.matches() || nosource_noslash_matcher.matches()) {
                                var index = url.lastIndexOf('/');
                                if (url.charAt(index - 1) != '/') link = url.substring(0, index + 1) + link;
                                else link = url + '/' + link;
                            }
                            else if (no_protocol_matcher.matches()) link = protocol + ':' + link;
                            else if (nosource_slash_matcher.matches()) {
                                var index = url.lastIndexOf('/');
                                if (url.charAt(index - 1) != '/') link = url.substring(0, index) + link;
                                else link = url + link;
                            }
                        }
                        finally {
                            try {
                                if (connection == null) connection = new URL(link).openConnection();
                                if (connection.getContentType().contains("text/html")) {
                                    var title = getLinkTitle(link);
                                    String[] data = {link, title};
                                    title_table_model.addRow(data);
                                }
                            }
                            catch (IOException error) {
                                // TODO add error popup window
                                error.printStackTrace();
                            }
                        }
                    }
                }
            }
            catch (IOException error) {
                // TODO add error popup window
                error.printStackTrace();
            }
        });
    }

    private void initSave(JTextField export_text, JButton export_button, DefaultTableModel title_table_model) {
        export_button.addActionListener(e -> {
            final var row_count = title_table_model.getRowCount();
            try {
                final var output = new OutputStreamWriter(new FileOutputStream(export_text.getText()), StandardCharsets.UTF_8);
                final var row = new AtomicInteger();
                while (row.get() < row_count) {
                    output.write(title_table_model.getValueAt(row.get(), 0).toString());
                    output.write("\n");
                    output.write(title_table_model.getValueAt(row.get(), 1).toString());
                    if (row.get() + 1 < row_count) output.write("\n");
                    row.getAndIncrement();
                }
                output.close();
            }
            catch (IOException error) {
                // TODO add error popup window
                error.printStackTrace();
            }
        });
    }

    private String getLinkTitle(String url) {
        try {
            final var inputStream = new URL(url).openStream();
            final var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder nextLine;
            var title_pattern = Pattern.compile("(?Uis).*?<title[^>]*>(.*?)(?:</title>.*?)?");
            while ((nextLine = Optional.ofNullable(reader.readLine()).map(StringBuilder::new).orElse(null)) != null) {
                var title_matcher = title_pattern.matcher(nextLine);
                if (title_matcher.matches()) {
                    while (!nextLine.toString().contains("</title>")) {
                        var next = reader.readLine();
                        if (next != null) nextLine.append(next);
                        else throw new IOException("Invalid <title>: " + nextLine);
                    }
                    return title_matcher.group(1);
                }
            }
        }
        catch (IOException error) {
            // TODO add error popup window
            error.printStackTrace();
        }
        return "";
    }
}
