package controller;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class Home implements Initializable {

    /**
     * 本地配置文件路径
     */
    private Path configLocation;

    /**
     * 配置文件
     */
    private Document document;

    /**
     * 界面显示字段
     */
    public TextField jiraUrl;
    public TextField jiraJql;
    public TextField notionToken;
    public TextField notionDatabaseId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 用户本地配置文件路径
        configLocation = Paths.get(System.getProperty("user.home"), ".jiraToNotion", "config.xml");

        // 本地配置文件是否存在
        if (Files.exists(configLocation)) {
            // 读取本地配置文件
            try (BufferedReader bufferedReader = Files.newBufferedReader(configLocation)) {
                document = new SAXReader().read(bufferedReader);
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 界面设置值
            Element rootElement = document.getRootElement();
            jiraUrl.setText(rootElement.element("jira").element("url").getText());
            jiraJql.setText(rootElement.element("jira").element("jql").getText());
            notionToken.setText(rootElement.element("notion").element("token").getText());
            notionDatabaseId.setText(rootElement.element("notion").element("databaseID").getText());
        } else {
            // 配置文件父目录不存在
            if (!Files.exists(configLocation.getParent())) {
                // 创建父目录
                try {
                    Files.createDirectory(configLocation.getParent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 文件不存在就从jar包里将默认的配置文件复制到本地
            try (BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/config.xml"))); BufferedWriter out = Files.newBufferedWriter(configLocation)) {
                in.lines().forEach(line -> {
                    try {
                        out.append(line);
                        out.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void jira_save(ActionEvent actionEvent) {
        // 设置修改后的值
        document.getRootElement().element("jira").element("url").setText(jiraUrl.getText());
        document.getRootElement().element("jira").element("jql").setText(jiraJql.getText());

        saveConfig();
    }

    public void notion_save(ActionEvent actionEvent) {
        // 设置修改后的值
        document.getRootElement().element("notion").element("token").setText(notionToken.getText());
        document.getRootElement().element("notion").element("databaseID").setText(notionDatabaseId.getText());

        saveConfig();
    }

    /**
     * 保存到本地配置文件
     */
    private void saveConfig() {
        XMLWriter writer = null;
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(configLocation)) {
            writer = new XMLWriter(bufferedWriter);
            writer.write(document);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
