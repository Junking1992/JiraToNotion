package controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import service.JiraToNotion;
import service.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.*;

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
    public TextField jiraUsername;
    public PasswordField jiraPassword;
    public TextArea jiraJql;
    public TextField notionToken;
    public TextField notionDatabaseId;
    public Slider slider;
    public Label time;
    public TextArea textArea;

    /**
     * 界面按钮
     */
    public Label msg;
    public Button jira_save_but;
    public Button notion_save_but;
    public Button start;

    /**
     * 创建线程池
      */
    private ExecutorService threadPool = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private JiraToNotion jiraToNotion = new JiraToNotion();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Log.init(textArea);
        // 用户本地配置文件路径
        configLocation = Paths.get(System.getProperty("user.home"), ".jiraToNotion", "config.xml");
        // 本地配置文件不存在
        if (!Files.exists(configLocation)) {
            // 配置文件父目录不存在
            if (!Files.exists(configLocation.getParent())) {
                // 创建父目录
                try {
                    Files.createDirectory(configLocation.getParent());
                } catch (IOException e) {
                    Log.error("创建父目录异常:", e);
                }
            }

            // 从jar包里将默认的配置文件复制到本地
            try (BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/config" +
                    ".xml"))); BufferedWriter out = Files.newBufferedWriter(configLocation)) {
                in.lines().forEach(line -> {
                    try {
                        out.append(line);
                        out.newLine();
                    } catch (IOException e) {
                        Log.error("配置文件复制到本地异常:", e);
                    }
                });
            } catch (IOException e) {
                Log.error("配置文件复制到本地异常:", e);
            }
        }

        // 读取本地配置文件
        try (BufferedReader bufferedReader = Files.newBufferedReader(configLocation)) {
            document = new SAXReader().read(bufferedReader);
        } catch (Exception e) {
            Log.error("读取本地配置文件异常:", e);
        }

        // 界面设置值
        Element rootElement = document.getRootElement();
        jiraUrl.setText(rootElement.element("jira").element("url").getText());
        jiraJql.setText(rootElement.element("jira").element("jql").getText());
        jiraUsername.setText(rootElement.element("jira").element("username").getText());
        jiraPassword.setText(rootElement.element("jira").element("password").getText());
        notionToken.setText(rootElement.element("notion").element("token").getText());
        notionDatabaseId.setText(rootElement.element("notion").element("databaseID").getText());
        String interval = rootElement.element("run").element("interval").getText();
        time.setText("更新数据频率:" + interval + "分钟");
        slider.setValue(Double.valueOf(interval));

        // 滑块滑动取整
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                int interval = newValue.intValue();
                if (interval % 10 < 5) {
                    interval = (interval / 10) * 10;
                } else {
                    interval = (interval / 10) * 10 + 10;
                }
                time.setText("更新数据频率:" + interval + "分钟");
                slider.setValue(interval);
            }
        });

        // 保存滑块的值到配置文件
        slider.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Double interval = slider.getValue();
                document.getRootElement().element("run").element("interval").setText(String.valueOf(interval.intValue()));
                // 保存
                saveConfig();
                showMsg("保存成功!");
            }
        });
    }

    /**
     * jira保存按钮触发
     */
    public void jira_save(ActionEvent actionEvent) {
        // 设置修改后的值
        document.getRootElement().element("jira").element("url").setText(jiraUrl.getText());
        document.getRootElement().element("jira").element("jql").setText(jiraJql.getText());
        document.getRootElement().element("jira").element("username").setText(jiraUsername.getText());
        document.getRootElement().element("jira").element("password").setText(jiraPassword.getText());
        // 保存
        saveConfig();
        showMsg("保存成功!");
    }

    /**
     * notion保存按钮触发
     */
    public void notion_save(ActionEvent actionEvent) {
        // 设置修改后的值
        document.getRootElement().element("notion").element("token").setText(notionToken.getText());
        document.getRootElement().element("notion").element("databaseID").setText(notionDatabaseId.getText());
        // 保存
        saveConfig();
        showMsg("保存成功!");
    }

    /**
     * 开始按钮触发
     */
    public void start(ActionEvent actionEvent) {
        if (jiraToNotion.running()) {
            // 停止更新数据
            jiraToNotion.stop();
            start.setText("启动");
            start.setTextFill(Color.rgb(0, 0, 0));
            showMsg("正在停止...");
        } else {
            // 启动更新数据
            jiraToNotion.run();
            start.setText("停止");
            start.setTextFill(Color.rgb(141, 70, 71));
            showMsg("正在启动...");
        }
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
            Log.error("保存到本地配置文件异常:", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.error("保存到本地配置文件关闭资源异常:", e);
                }
            }
        }
    }

    /**
     * 提示信息
     *
     * @param text 提示内容
     */
    private void showMsg(String text) {
        msg.setText(text);
        msg.setVisible(true);
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    Thread.sleep(1200L);
                } catch (InterruptedException e) {
                    Log.error("提示信息等待异常:", e);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                msg.setVisible(false);
            }
        };
        threadPool.submit(task);
    }
}
