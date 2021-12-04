package controller;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class Home implements Initializable {

    /**
     * 配置文件
     */
    private Document document;
    private File xmlFile;
    private Element jira_url;
    private Element jira_jql;
    private Element notion_token;
    private Element notion_databaseID;


    public TextField jiraUrl;
    public TextField jiraJql;
    public TextField notionToken;
    public TextField notionDatabaseId;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            document = new SAXReader().read(getClass().getClassLoader().getResourceAsStream("config.xml"));
            xmlFile = new File(getClass().getClassLoader().getResource("config.xml").toURI());
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        // 获取XML对象
        Element rootElement = document.getRootElement();
        jira_url = rootElement.element("jira").element("url");
        jira_jql = rootElement.element("jira").element("jql");
        notion_token = rootElement.element("notion").element("token");
        notion_databaseID = rootElement.element("notion").element("databaseID");

        // 界面设置值
        jiraUrl.setText(jira_url.getText());
        jiraJql.setText(jira_jql.getText());
        notionToken.setText(notion_token.getText());
        notionDatabaseId.setText(notion_databaseID.getText());
    }

    public void jira_save(ActionEvent actionEvent) {
        jira_url.setText(jiraUrl.getText());
        jira_jql.setText(jiraJql.getText());
        try {
            XMLWriter writer = new XMLWriter(new FileWriter(xmlFile));
            writer.write(document);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notion_save(ActionEvent actionEvent) {

    }
}
